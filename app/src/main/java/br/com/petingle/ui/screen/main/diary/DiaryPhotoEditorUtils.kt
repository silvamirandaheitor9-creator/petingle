package br.com.petingle.ui.screen.main.diary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Carregamento da foto de origem (galeria), respeitando a orientação EXIF
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Decodifica a imagem do [uri] e aplica a rotação indicada pelos metadados
 * EXIF (fotos de câmera costumam vir "de lado" sem essa correção).
 *
 * Usa apenas 2 aberturas de stream:
 *   1ª — ExifInterface lê dimensões (TAG_IMAGE_WIDTH/LENGTH) + orientação.
 *   2ª — BitmapFactory decodifica com o inSampleSize calculado.
 */
fun loadBitmapRespectingExif(context: Context, uri: Uri, maxDimension: Int = 1920): Bitmap? {
    val resolver = context.contentResolver

    // ── 1ª abertura: dimensões + orientação EXIF numa única passagem ──────────
    var rawWidth = 0
    var rawHeight = 0
    var rotationDegrees = 0f
    resolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        rawWidth  = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH,  0)
        rawHeight = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90  -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else                                 -> 0f
        }
    }

    // Calcula inSampleSize: potência de 2 que mantém o lado maior ≤ maxDimension.
    // Se EXIF não reportou dimensões (rawWidth == 0), sampleSize fica em 1 —
    // seguro: o decode carrega a imagem sem downsampling.
    var sampleSize = 1
    val larger = maxOf(rawWidth, rawHeight)
    if (larger > maxDimension) {
        val half = larger / 2
        while (half / sampleSize > maxDimension) sampleSize *= 2
    }

    // ── 2ª abertura: decodifica com downsampling ──────────────────────────────
    val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize       = sampleSize
        inPreferredConfig  = Bitmap.Config.ARGB_8888
    }
    val original = resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, decodeOpts)
    } ?: return null

    return if (rotationDegrees != 0f) rotateBitmap(original, rotationDegrees) else original
}

// ─────────────────────────────────────────────────────────────────────────────
// Transformações geométricas básicas (usadas pelo editor de foto do pet)
// ─────────────────────────────────────────────────────────────────────────────

fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

/**
 * Recorta um sub-quadrado de [bitmap] a partir do canto ([left], [top]) com
 * lado [size], garantindo que a região fique dentro dos limites do bitmap.
 */
fun cropBitmapToSquareRegion(bitmap: Bitmap, left: Int, top: Int, size: Int): Bitmap {
    val safeSize = size.coerceIn(1, minOf(bitmap.width, bitmap.height))
    val safeLeft = left.coerceIn(0, bitmap.width - safeSize)
    val safeTop  = top.coerceIn(0, bitmap.height - safeSize)
    return Bitmap.createBitmap(bitmap, safeLeft, safeTop, safeSize, safeSize)
}

// ─────────────────────────────────────────────────────────────────────────────
// Fundo branco + salvar em JPEG (SPEC 17.3)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Regra da SPEC 17.3: desenha a imagem sobre um fundo branco sólido antes de
 * comprimir em JPEG, para nunca deixar uma borda preta onde havia
 * transparência (o formato JPEG não tem canal alfa).
 */
fun flattenOnWhiteBackground(bitmap: Bitmap): Bitmap {
    val flattened = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(flattened)
    canvas.drawColor(Color.WHITE)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    return flattened
}

/** Salva a foto final do Diário em armazenamento interno (SPEC 17.1) como JPEG. */
fun saveDiaryPhotoJpeg(context: Context, bitmap: Bitmap): String {
    val dir = File(context.filesDir, "diary_photos").apply { if (!exists()) mkdirs() }
    val file = File(dir, "diary_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    val flattened = flattenOnWhiteBackground(bitmap)
    FileOutputStream(file).use { out ->
        flattened.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return file.absolutePath
}
