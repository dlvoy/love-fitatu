package pl.cukrzycowy.labelbite.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.cukrzycowy.labelbite.R
import pl.cukrzycowy.labelbite.ocr.ImagePrep
import java.io.File

private const val MAX_ZOOM = 8f

/** Keeps a panned image from being dragged further than its zoomed edges. */
fun clampPan(offset: Offset, scale: Float, size: IntSize): Offset {
    val maxX = size.width * (scale - 1f) / 2f
    val maxY = size.height * (scale - 1f) / 2f
    return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
}

/** Full-screen photo: pinch to zoom, drag to pan, double tap to zoom in or reset. */
@Composable
fun ZoomableImageDialog(bitmap: Bitmap?, onDismiss: () -> Unit) {
    Dialog(onDismiss, DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var size by remember { mutableStateOf(IntSize.Zero) }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { size = it }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                        // Zoom about the fingers, not the screen centre.
                        val centre = Offset(size.width / 2f, size.height / 2f)
                        val focus = centroid - centre
                        val ratio = newScale / scale
                        offset = clampPan(focus + (offset - focus) * ratio + pan, newScale, size)
                        scale = newScale
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 3f
                            offset = clampPan((Offset(size.width / 2f, size.height / 2f) - tap) * 2f, 3f, size)
                        }
                    })
                },
        ) {
            if (bitmap == null) CircularProgressIndicator()
            else Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Zdjęcie etykiety",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .systemBarsPadding()
                    .padding(4.dp),
            ) {
                IconButton(onDismiss) { Icon(painterResource(R.drawable.ic_arrow_back), "Wróć", tint = Color.White) }
                Text("Zdjęcie etykiety", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

/** Same viewer for a stored photo, loaded at full stored resolution. */
@Composable
fun ZoomablePhotoDialog(photo: File, onDismiss: () -> Unit) {
    val bitmap by produceState<Bitmap?>(null, photo) {
        value = withContext(Dispatchers.IO) { ImagePrep.loadFile(photo, ImagePrep.STORED_EDGE) }
    }
    ZoomableImageDialog(bitmap, onDismiss)
}
