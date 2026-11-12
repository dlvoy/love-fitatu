package pl.lovefitatu.bridge.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

object ImagePrep {
    /** Long edge of the copy kept in history: enough to zoom into small print. */
    const val STORED_EDGE = 2560

    /** Long edge of what is sent to the model. */
    const val UPLOAD_EDGE = 1600

    /** Loads the image with its long edge at most [maxEdge] px and EXIF rotation applied. */
    fun load(context: Context, uri: Uri, maxEdge: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }

        val decoded = resolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, sampleOptions(bounds, maxEdge))
        } ?: error("Nie można odczytać zdjęcia")

        val orientation = resolver.openInputStream(uri)!!.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        return transform(decoded, maxEdge, degrees)
    }

    /** Loads a history photo (already upright) with its long edge at most [maxEdge] px. */
    fun loadFile(file: File, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        return BitmapFactory.decodeFile(file.path, sampleOptions(bounds, maxEdge))?.let { transform(it, maxEdge, 0f) }
    }

    fun scaled(bitmap: Bitmap, maxEdge: Int): Bitmap = transform(bitmap, maxEdge, 0f)

    fun toDataUrl(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    private fun sampleOptions(bounds: BitmapFactory.Options, maxEdge: Int): BitmapFactory.Options {
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        return BitmapFactory.Options().apply { inSampleSize = sample }
    }

    private fun transform(bitmap: Bitmap, maxEdge: Int, degrees: Float): Bitmap {
        val scale = maxEdge.toFloat() / maxOf(bitmap.width, bitmap.height)
        if (degrees == 0f && scale >= 1f) return bitmap
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
