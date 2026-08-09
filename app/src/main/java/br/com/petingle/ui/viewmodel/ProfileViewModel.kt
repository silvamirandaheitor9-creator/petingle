package br.com.petingle.ui.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.petingle.data.datastore.UserPreferencesRepository
import br.com.petingle.data.db.PetIngleDatabase
import br.com.petingle.data.db.dao.DiaryDao
import br.com.petingle.data.db.dao.HealthRecordDao
import br.com.petingle.data.db.dao.PetDao
import br.com.petingle.data.db.dao.ReminderDao
import br.com.petingle.data.db.entity.DiaryEntry
import br.com.petingle.data.db.entity.HealthRecord
import br.com.petingle.data.db.entity.Pet
import br.com.petingle.data.db.entity.Reminder
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

// ── Eventos de UI para o ProfileScreen ────────────────────────────────────────
sealed class ProfileUiEvent {
    object ExportSuccess                          : ProfileUiEvent()
    data class ExportError(val msg: String)       : ProfileUiEvent()
    object ImportSuccess                          : ProfileUiEvent()
    data class ImportError(val msg: String)       : ProfileUiEvent()
    object DeleteSuccess                          : ProfileUiEvent()
}

private data class PackagedBackup(
    val directory: File,
    val database: File,
    val profileProperties: File?,
    val profilePhoto: File?,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val prefs            : UserPreferencesRepository,
    private val db               : PetIngleDatabase,
    private val petDao           : PetDao,
    private val reminderDao      : ReminderDao,
    private val diaryDao         : DiaryDao,
    private val healthRecordDao  : HealthRecordDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // ── Nome do usuário ───────────────────────────────────────────────────────
    val userName = prefs.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    // ── Foto de perfil ────────────────────────────────────────────────────────
    val profilePhotoPath = prefs.profilePhotoPath
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /**
     * Copia a imagem selecionada pelo usuário para o armazenamento interno do app
     * e persiste o caminho absoluto no DataStore.
     */
    fun saveProfilePhoto(uri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = java.io.File(context.filesDir, "profile")
                dir.mkdirs()
                val dest = java.io.File(dir, "profile_photo.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { input.copyTo(it) }
                }
                prefs.setProfilePhotoPath(dest.absolutePath)
            } catch (_: Exception) { /* erro silencioso */ }
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { prefs.setUserName(name.trim()) }
    }

    // ── Contadores de estatísticas para o header ──────────────────────────────
    val petCount: kotlinx.coroutines.flow.StateFlow<Int> = petDao.getPetCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val diaryCount: kotlinx.coroutines.flow.StateFlow<Int> = diaryDao.getAllEntries()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val reminderCount: kotlinx.coroutines.flow.StateFlow<Int> = reminderDao.getAllReminders()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // ── Eventos one-shot para a UI ────────────────────────────────────────────
    private val _events = MutableSharedFlow<ProfileUiEvent>()
    val events: SharedFlow<ProfileUiEvent> = _events.asSharedFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Exportar backup via SAF
    // ─────────────────────────────────────────────────────────────────────────
    fun exportBackup(contentResolver: ContentResolver, treeUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val srcDb  = context.getDatabasePath("petingle.db")
                val srcWal = File(srcDb.parent!!, "petingle.db-wal")

                if (!srcDb.exists()) {
                    _events.emit(ProfileUiEvent.ExportError(
                        "Banco de dados não encontrado. Adicione um pet antes de exportar."))
                    return@launch
                }

                val tempDir = File(context.cacheDir, "petingle_export_tmp")
                tempDir.deleteRecursively()
                tempDir.mkdirs()
                val tempDb  = File(tempDir, "petingle.db")
                val tempWal = File(tempDir, "petingle.db-wal")

                srcDb.copyTo(tempDb, overwrite = true)
                if (srcWal.exists()) srcWal.copyTo(tempWal, overwrite = true)

                val exportDb = SQLiteDatabase.openDatabase(
                    tempDb.absolutePath, null, SQLiteDatabase.OPEN_READWRITE,
                )
                exportDb.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).close()
                exportDb.close()

                val profilePhoto = File(prefs.profilePhotoPath.first())
                val profileProperties = Properties().apply {
                    setProperty(
                        "user_name_base64",
                        Base64.encodeToString(
                            prefs.userName.first().toByteArray(Charsets.UTF_8),
                            Base64.NO_WRAP,
                        ),
                    )
                    setProperty("has_profile_photo", profilePhoto.isFile.toString())
                }

                val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
                val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
                val docUri = DocumentsContract.createDocument(
                    contentResolver, parentUri,
                    "application/zip", "petingle_backup.petingle",
                ) ?: run {
                    tempDir.deleteRecursively()
                    _events.emit(ProfileUiEvent.ExportError(
                        "Não foi possível criar o arquivo na pasta selecionada."))
                    return@launch
                }

                contentResolver.openOutputStream(docUri)?.use { out ->
                    ZipOutputStream(BufferedOutputStream(out)).use { zip ->
                        zip.putNextEntry(ZipEntry("petingle.db"))
                        tempDb.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()

                        zip.putNextEntry(ZipEntry("profile.properties"))
                        profileProperties.store(zip, "PetIngle profile backup")
                        zip.closeEntry()

                        if (profilePhoto.isFile) {
                            zip.putNextEntry(ZipEntry("profile_photo.jpg"))
                            profilePhoto.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }

                tempDir.deleteRecursively()
                _events.emit(ProfileUiEvent.ExportSuccess)
            } catch (e: Exception) {
                _events.emit(ProfileUiEvent.ExportError(e.localizedMessage ?: "Erro ao exportar."))
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Importar backup via SAF
    // ─────────────────────────────────────────────────────────────────────────
    fun importBackup(contentResolver: ContentResolver, fileUri: Uri, merge: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(context.cacheDir, "petingle_import_temp.db")
                contentResolver.openInputStream(fileUri)?.use { input ->
                    tempFile.outputStream().use { input.copyTo(it) }
                } ?: run {
                    _events.emit(ProfileUiEvent.ImportError("Não foi possível ler o arquivo."))
                    return@launch
                }

                val packagedBackup = if (isPackagedBackup(tempFile)) {
                    extractPackagedBackup(tempFile)
                } else {
                    null
                }
                val sourceDatabase = packagedBackup?.database ?: tempFile

                val src = SQLiteDatabase.openDatabase(
                    sourceDatabase.absolutePath, null, SQLiteDatabase.OPEN_READONLY,
                )

                if (!merge) db.clearAllTables()

                val petIdMap = mutableMapOf<Long, Long>()
                src.rawQuery("SELECT * FROM pets", null).use { c ->
                    while (c.moveToNext()) {
                        val oldId = c.getLong(c.getColumnIndexOrThrow("id"))
                        val newId = petDao.insertPet(Pet(
                            id                = 0,
                            name              = c.getString(c.getColumnIndexOrThrow("name")),
                            species           = c.getString(c.getColumnIndexOrThrow("species")),
                            breed             = c.getString(c.getColumnIndexOrThrow("breed")),
                            sex               = c.getString(c.getColumnIndexOrThrow("sex")),
                            isCastrated       = c.getInt(c.getColumnIndexOrThrow("isCastrated")) != 0,
                            birthDate         = c.getString(c.getColumnIndexOrThrow("birthDate")),
                            approximateAge    = c.getString(c.getColumnIndexOrThrow("approximateAge")),
                            weightKg          = c.getDouble(c.getColumnIndexOrThrow("weightKg")),
                            bloodType         = c.getString(c.getColumnIndexOrThrow("bloodType")),
                            allergies         = c.getString(c.getColumnIndexOrThrow("allergies")),
                            chronicConditions = c.getString(c.getColumnIndexOrThrow("chronicConditions")),
                            microchip         = c.getString(c.getColumnIndexOrThrow("microchip")),
                            notes             = c.getString(c.getColumnIndexOrThrow("notes")),
                            vetName           = c.getString(c.getColumnIndexOrThrow("vetName")),
                            vetPhone          = c.getString(c.getColumnIndexOrThrow("vetPhone")),
                            photoPath         = c.getString(c.getColumnIndexOrThrow("photoPath")),
                            createdAt         = c.getLong(c.getColumnIndexOrThrow("createdAt")),
                        ))
                        petIdMap[oldId] = newId
                    }
                }

                src.rawQuery("SELECT * FROM reminders", null).use { c ->
                    while (c.moveToNext()) {
                        val newPetId = petIdMap[c.getLong(c.getColumnIndexOrThrow("petId"))]
                            ?: continue
                        reminderDao.insertReminder(Reminder(
                            id             = 0,
                            petId          = newPetId,
                            title          = c.getString(c.getColumnIndexOrThrow("title")),
                            category       = c.getString(c.getColumnIndexOrThrow("category")),
                            dateTimeMillis = c.getLong(c.getColumnIndexOrThrow("dateTimeMillis")),
                            recurrence     = c.getString(c.getColumnIndexOrThrow("recurrence")),
                            isCompleted    = c.getInt(c.getColumnIndexOrThrow("isCompleted")) != 0,
                            notes          = c.getString(c.getColumnIndexOrThrow("notes")),
                            notificationId = c.getInt(c.getColumnIndexOrThrow("notificationId")),
                        ))
                    }
                }

                src.rawQuery("SELECT * FROM diary_entries", null).use { c ->
                    while (c.moveToNext()) {
                        val newPetId = petIdMap[c.getLong(c.getColumnIndexOrThrow("petId"))]
                            ?: continue
                        diaryDao.insertEntry(DiaryEntry(
                            id         = 0,
                            petId      = newPetId,
                            photoPath  = c.getString(c.getColumnIndexOrThrow("photoPath")),
                            caption    = c.getString(c.getColumnIndexOrThrow("caption")),
                            dateMillis = c.getLong(c.getColumnIndexOrThrow("dateMillis")),
                        ))
                    }
                }

                src.rawQuery("SELECT * FROM health_records", null).use { c ->
                    while (c.moveToNext()) {
                        val newPetId = petIdMap[c.getLong(c.getColumnIndexOrThrow("petId"))]
                            ?: continue
                        healthRecordDao.insertRecord(HealthRecord(
                            id                     = 0,
                            petId                  = newPetId,
                            type                   = c.getString(c.getColumnIndexOrThrow("type")),
                            vaccineName            = c.getString(c.getColumnIndexOrThrow("vaccineName")),
                            vaccineLot             = c.getString(c.getColumnIndexOrThrow("vaccineLot")),
                            nextDoseDate           = c.getString(c.getColumnIndexOrThrow("nextDoseDate")),
                            consultationReason     = c.getString(c.getColumnIndexOrThrow("consultationReason")),
                            diagnosis              = c.getString(c.getColumnIndexOrThrow("diagnosis")),
                            vetInstructions        = c.getString(c.getColumnIndexOrThrow("vetInstructions")),
                            weightKg               = c.getDouble(c.getColumnIndexOrThrow("weightKg")),
                            feedingType            = c.getString(c.getColumnIndexOrThrow("feedingType")),
                            feedingAmountGrams     = c.getDouble(c.getColumnIndexOrThrow("feedingAmountGrams")),
                            feedingSchedule        = c.getString(c.getColumnIndexOrThrow("feedingSchedule")),
                            medicationName         = c.getString(c.getColumnIndexOrThrow("medicationName")),
                            medicationDosage       = c.getString(c.getColumnIndexOrThrow("medicationDosage")),
                            medicationFrequency    = c.getString(c.getColumnIndexOrThrow("medicationFrequency")),
                            medicationDurationDays = c.getInt(c.getColumnIndexOrThrow("medicationDurationDays")),
                            dateMillis             = c.getLong(c.getColumnIndexOrThrow("dateMillis")),
                            notes                  = c.getString(c.getColumnIndexOrThrow("notes")),
                        ))
                    }
                }

                src.close()

                packagedBackup?.let { restoreProfileData(it) }

                packagedBackup?.directory?.deleteRecursively()
                tempFile.delete()
                _events.emit(ProfileUiEvent.ImportSuccess)
            } catch (e: Exception) {
                _events.emit(ProfileUiEvent.ImportError(
                    e.localizedMessage ?: "Erro ao importar backup."))
            }
        }
    }

    private fun isPackagedBackup(file: File): Boolean =
        FileInputStream(file).use { input ->
            input.read() == 'P'.code &&
                input.read() == 'K'.code &&
                input.read() == 3 &&
                input.read() == 4
        }

    private fun extractPackagedBackup(archive: File): PackagedBackup {
        val directory = File(context.cacheDir, "petingle_packaged_import")
        directory.deleteRecursively()
        directory.mkdirs()

        var database: File? = null
        var profileProperties: File? = null
        var profilePhoto: File? = null

        ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val destination = when (entry.name) {
                        "petingle.db" -> File(directory, "petingle.db").also { database = it }
                        "profile.properties" -> File(directory, "profile.properties").also { profileProperties = it }
                        "profile_photo.jpg" -> File(directory, "profile_photo.jpg").also { profilePhoto = it }
                        else -> null
                    }
                    destination?.let { target ->
                        FileOutputStream(target).use { output -> zip.copyTo(output) }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return PackagedBackup(
            directory = directory,
            database = requireNotNull(database) { "Backup PetIngle sem banco de dados." },
            profileProperties = profileProperties,
            profilePhoto = profilePhoto,
        )
    }

    private suspend fun restoreProfileData(backup: PackagedBackup) {
        val properties = backup.profileProperties?.let { file ->
            Properties().also { props ->
                FileInputStream(file).use { props.load(it) }
            }
        }

        val encodedName = properties?.getProperty("user_name_base64")
        if (encodedName != null) {
            val name = String(Base64.decode(encodedName, Base64.DEFAULT), Charsets.UTF_8)
            prefs.setUserName(name)
        }

        val hasPhoto = properties?.getProperty("has_profile_photo") == "true"
        if (hasPhoto && backup.profilePhoto?.isFile == true) {
            val profileDirectory = File(context.filesDir, "profile").apply { mkdirs() }
            val destination = File(profileDirectory, "profile_photo.jpg")
            backup.profilePhoto.inputStream().use { input ->
                destination.outputStream().use { output -> input.copyTo(output) }
            }
            prefs.setProfilePhotoPath(destination.absolutePath)
        } else if (!hasPhoto) {
            File(context.filesDir, "profile/profile_photo.jpg").delete()
            prefs.setProfilePhotoPath("")
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Apagar todos os dados (confirmação dupla na UI)
    // ─────────────────────────────────────────────────────────────────────────
    fun deleteAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                db.clearAllTables()
                prefs.setUserName("")
                prefs.setProfilePhotoPath("")
                _events.emit(ProfileUiEvent.DeleteSuccess)
            } catch (_: Exception) { /* raramente falha */ }
        }
    }
}
