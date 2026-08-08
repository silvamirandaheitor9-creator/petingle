package br.com.petingle.ui.screen.main.pets

import android.content.Context
import android.graphics.Bitmap
import br.com.petingle.ui.screen.main.diary.flattenOnWhiteBackground
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Salva a foto de perfil final do pet em armazenamento interno (SPEC §11 —
 * parte 2; mesma regra de fundo branco antes do JPEG do SPEC 17.3 usada no
 * editor do Diário, via [flattenOnWhiteBackground]).
 */
fun savePetPhotoJpeg(context: Context, bitmap: Bitmap): String {
    val dir = File(context.filesDir, "pet_photos").apply { if (!exists()) mkdirs() }
    val file = File(dir, "pet_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")
    val flattened = flattenOnWhiteBackground(bitmap)
    FileOutputStream(file).use { out ->
        flattened.compress(Bitmap.CompressFormat.JPEG, 92, out)
    }
    return file.absolutePath
}
