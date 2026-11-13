#!/usr/bin/env bash
# Builds the debug APK signed with the keystore from scripts/.env, installs it on the connected
# device and starts it. See BUILDING.md.
#
#   scripts/run-debug.sh              build, install, launch
#   scripts/run-debug.sh --no-launch  build and install only
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
APP_ID="pl.cukrzycowy.labelbite"
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"

launch=1
for arg in "$@"; do
  case "$arg" in
    --no-launch) launch=0 ;;
    -h|--help) sed -n '2,7p' "$0"; exit 0 ;;
    *) echo "Unknown option: $arg" >&2; exit 2 ;;
  esac
done

die() { echo "error: $*" >&2; exit 1; }

# --- read scripts/.env without executing it -------------------------------------------------
[ -f "$ENV_FILE" ] || die "$ENV_FILE not found. Copy scripts/.env.example to scripts/.env and fill it in."

KEYSTORE_PATH="" KEYSTORE_PASSWORD="" KEY_ALIAS="" KEY_PASSWORD="" ENV_JAVA_HOME=""
while IFS= read -r line || [ -n "$line" ]; do
  line="${line%$'\r'}"
  [[ "$line" =~ ^[[:space:]]*(#|$) ]] && continue
  [[ "$line" == *=* ]] || continue
  key="${line%%=*}"; key="${key//[[:space:]]/}"
  value="${line#*=}"
  value="${value#"${value%%[![:space:]]*}"}"; value="${value%"${value##*[![:space:]]}"}"
  if [[ "$value" =~ ^\"(.*)\"$ || "$value" =~ ^\'(.*)\'$ ]]; then value="${BASH_REMATCH[1]}"; fi
  case "$key" in
    KEYSTORE_PATH|KEYSTORE_PASSWORD|KEY_ALIAS|KEY_PASSWORD|ANDROID_SERIAL) printf -v "$key" '%s' "$value" ;;
    JAVA_HOME) [ -n "$value" ] && ENV_JAVA_HOME="$value" ;;
  esac
done < "$ENV_FILE"

for var in KEYSTORE_PATH KEYSTORE_PASSWORD KEY_ALIAS KEY_PASSWORD; do
  [ -n "${!var}" ] || die "$var is not set in $ENV_FILE"
done

# Relative keystore paths are relative to scripts/.
case "$KEYSTORE_PATH" in
  /*|[A-Za-z]:[\\/]*) ;;
  *) KEYSTORE_PATH="$SCRIPT_DIR/$KEYSTORE_PATH" ;;
esac
[ -f "$KEYSTORE_PATH" ] || die "keystore not found: $KEYSTORE_PATH"
# Gradle on Windows needs a Windows path rather than /c/... from Git Bash.
if command -v cygpath >/dev/null 2>&1; then KEYSTORE_PATH="$(cygpath -m "$KEYSTORE_PATH")"; fi

# --- find adb ---------------------------------------------------------------------------------
find_adb() {
  if [ -n "${ADB:-}" ]; then echo "$ADB"; return; fi
  if command -v adb >/dev/null 2>&1; then command -v adb; return; fi
  local sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
  if [ -z "$sdk" ] && [ -f "$ROOT/local.properties" ]; then
    sdk="$(sed -n 's/^sdk\.dir=//p' "$ROOT/local.properties" | sed 's/\\:/:/g; s/\\\\/\//g')"
  fi
  for candidate in "$sdk/platform-tools/adb" "$sdk/platform-tools/adb.exe"; do
    [ -n "$sdk" ] && [ -x "$candidate" ] && { echo "$candidate"; return; }
  done
  return 1
}
ADB_BIN="$(find_adb)" || die "adb not found. Put platform-tools on PATH, or set ANDROID_HOME or ADB."

[ -n "${ANDROID_SERIAL:-}" ] && export ANDROID_SERIAL
devices="$("$ADB_BIN" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "${ANDROID_SERIAL:-}" ]; then
  count="$(printf '%s\n' "$devices" | grep -c . || true)"
  [ "$count" -ge 1 ] || die "no device connected (or it is unauthorised). Check 'adb devices'."
  [ "$count" -eq 1 ] || die "$count devices connected; set ANDROID_SERIAL in scripts/.env to pick one."
fi

# --- pick a JDK 17+ ---------------------------------------------------------------------------
java_major() { # prints the major version of the java in JDK home $1, or 0
  local bin="$1/bin/java"; [ -x "$bin" ] || bin="$1/bin/java.exe"; [ -x "$bin" ] || { echo 0; return; }
  "$bin" -version 2>&1 | sed -nE '1s/.*version "(1\.)?([0-9]+).*/\2/p' | grep . || echo 0
}
if [ -n "$ENV_JAVA_HOME" ]; then
  export JAVA_HOME="$ENV_JAVA_HOME"
elif [ -z "${JAVA_HOME:-}" ] || [ "$(java_major "$JAVA_HOME")" -lt 17 ]; then
  # Fall back to the JDK bundled with Android Studio.
  for jbr in "/c/Program Files/Android/Android Studio/jbr" \
             "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
             "/opt/android-studio/jbr" "$HOME/android-studio/jbr"; do
    if [ "$(java_major "$jbr")" -ge 17 ]; then export JAVA_HOME="$jbr"; break; fi
  done
fi
if [ -n "${JAVA_HOME:-}" ] && [ "$(java_major "$JAVA_HOME")" -lt 17 ]; then
  die "JDK 17 or newer is needed; JAVA_HOME is $JAVA_HOME. Set JAVA_HOME in scripts/.env."
fi

# --- build ------------------------------------------------------------------------------------
export DEBUG_SIGNING_KEYSTORE_PATH="$KEYSTORE_PATH"
export DEBUG_SIGNING_KEYSTORE_PASSWORD="$KEYSTORE_PASSWORD"
export DEBUG_SIGNING_KEY_ALIAS="$KEY_ALIAS"
export DEBUG_SIGNING_KEY_PASSWORD="$KEY_PASSWORD"

echo "==> Building debug APK"
(cd "$ROOT" && ./gradlew assembleDebug)
[ -f "$APK" ] || die "APK not found at $APK"

# --- install and run --------------------------------------------------------------------------
echo "==> Installing on ${ANDROID_SERIAL:-$devices}"
if ! out="$("$ADB_BIN" install -r "$APK" 2>&1)"; then
  echo "$out" >&2
  if [[ "$out" == *INSTALL_FAILED_UPDATE_INCOMPATIBLE* ]]; then
    die "the installed app is signed with a different key. Uninstall it first (this deletes its data): $ADB_BIN uninstall $APP_ID"
  fi
  die "install failed"
fi
echo "$out"

if [ "$launch" -eq 1 ]; then
  echo "==> Starting $APP_ID"
  "$ADB_BIN" shell am start -n "$APP_ID/.MainActivity" >/dev/null
fi
echo "Done."
