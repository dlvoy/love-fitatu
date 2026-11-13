package pl.cukrzycowy.labelbite.data

typealias TomlDoc = Map<String, Map<String, Any>>

/**
 * The small TOML subset the history files use: `[section]` tables holding `key = value` pairs where
 * a value is a basic string, an integer, a float or a boolean.
 */
object Toml {
    fun write(doc: TomlDoc): String = buildString {
        for ((section, entries) in doc) {
            if (entries.isEmpty()) continue
            if (isNotEmpty()) append('\n')
            append('[').append(section).append("]\n")
            for ((key, value) in entries) append(key).append(" = ").append(encode(value)).append('\n')
        }
    }

    fun parse(text: String): LinkedHashMap<String, LinkedHashMap<String, Any>> {
        val doc = LinkedHashMap<String, LinkedHashMap<String, Any>>()
        var section = doc.getOrPut("") { LinkedHashMap() }
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                section = doc.getOrPut(line.substring(1, line.length - 1).trim()) { LinkedHashMap() }
                continue
            }
            val eq = line.indexOf('=')
            if (eq <= 0) continue
            decode(line.substring(eq + 1).trim())?.let { section[line.substring(0, eq).trim()] = it }
        }
        if (doc[""]?.isEmpty() == true) doc.remove("")
        return doc
    }

    private fun encode(value: Any): String = when (value) {
        is Boolean, is Int, is Long -> value.toString()
        is Number -> value.toDouble().let { if (it.isFinite()) it.toString() else "0.0" }
        else -> buildString {
            append('"')
            for (c in value.toString()) when (c) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(c)
            }
            append('"')
        }
    }

    private fun decode(token: String): Any? {
        if (token.startsWith("\"")) {
            val out = StringBuilder()
            var i = 1
            while (i < token.length) {
                val c = token[i]
                when {
                    c == '"' -> return out.toString()
                    c == '\\' && i + 1 < token.length -> {
                        i++
                        out.append(
                            when (token[i]) {
                                'n' -> '\n'
                                'r' -> '\r'
                                't' -> '\t'
                                else -> token[i]
                            }
                        )
                    }
                    else -> out.append(c)
                }
                i++
            }
            return out.toString()
        }
        val bare = token.substringBefore('#').trim()
        return when (bare) {
            "true" -> true
            "false" -> false
            else -> bare.toLongOrNull() ?: bare.toDoubleOrNull()
        }
    }
}
