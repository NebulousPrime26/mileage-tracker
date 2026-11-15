package com.nebulousprime26.mileage_tracker.ui.features.landing

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.nebulousprime26.mileage_tracker.R
import com.nebulousprime26.mileage_tracker.db.cipher.EncryptedBackup
import com.nebulousprime26.mileage_tracker.db.cipher.Encryption
import com.nebulousprime26.mileage_tracker.db.export.BackupException
import com.nebulousprime26.mileage_tracker.db.export.TripBackup
import com.nebulousprime26.mileage_tracker.db.export.exportDb
import com.nebulousprime26.mileage_tracker.db.export.shareCsv
import com.nebulousprime26.mileage_tracker.db.local.Trip
import com.nebulousprime26.mileage_tracker.db.local.TripDao
import com.nebulousprime26.mileage_tracker.db.DatabaseProvider

class LandingViewModel(app: Application) : AndroidViewModel(app) {

    /**
     * UI state for the export/import flow. The export produces the
     * key and encrypts the data in memory first, then writes to disk
     * only after the user has confirmed they've saved the key.
     */
    sealed interface BackupState {
        /** No backup operation in progress. */
        object Idle : BackupState

        /** An export or import is running. */
        object Working : BackupState

        /** Key generated, waiting for the user to confirm before writing. */
        data class ExportPreview(val key: String, val filename: String) : BackupState

        /** Backup file written successfully. */
        data class Exported(val filename: String) : BackupState

        /** The user picked a file and needs to supply the key. */
        data class ImportAwaitingKey(val uri: Uri) : BackupState

        /** The key is entered; awaiting confirmation of the destructive replace. */
        data class ImportConfirming(val uri: Uri, val key: String) : BackupState

        /** Import succeeded, with the number of trips inserted. */
        data class Imported(val count: Int) : BackupState

        /** Something went wrong in either direction. */
        data class Failed(val message: String) : BackupState
    }

    /**
     * Holds the encrypted backup in memory between the preview dialog
     * and the confirmation. Cleared on confirm, cancel, or failure.
     */
    private var pendingExport: EncryptedBackup? = null
    private val dao: TripDao = DatabaseProvider.getInstance(app).tripDao()

    /**
     * Resolves a throwable into a user-facing message in the active
     * locale. [BackupException] carries a resource ID and arguments;
     * other throwables fall back to their own message, and if even that
     * is missing a generic failure string is used.
     */
    private fun resolveMessage(t: Throwable, fallbackRes: Int): String {
        val app = getApplication<Application>()
        return when (t) {
            is BackupException -> app.getString(t.messageRes, *t.args)
            else -> t.message ?: app.getString(fallbackRes)
        }
    }

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    /**
     * Prepares the backup: reads all trips, generates a key, and
     * encrypts everything in memory. Nothing is written to disk until
     * [confirmExport] is called.
     */
    fun exportTrips() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            _backupState.value = BackupState.Working
            try {
                val backup = exportDb(app)
                pendingExport = backup
                _backupState.value = BackupState.ExportPreview(
                    key = Encryption.formatKey(backup.key),
                    filename = backup.filename,
                )
            } catch (t: Throwable) {
                pendingExport = null
                _backupState.value = BackupState.Failed(
                    resolveMessage(t, R.string.backup_error_export_failed)
                )
            }
        }
    }

    /**
     * Writes the prepared backup to Downloads. Called only after the
     * user has confirmed they saved the key.
     */
    fun confirmExport() {
        val pending = pendingExport ?: run {
            _backupState.value = BackupState.Failed(
                getApplication<Application>()
                    .getString(R.string.backup_error_nothing_to_export)
            )
            return
        }
        viewModelScope.launch {
            _backupState.value = BackupState.Working
            try {
                TripBackup.writeToDownloads(
                    context = getApplication(),
                    filename = pending.filename,
                    bytes = pending.blob,
                )
                pendingExport = null
                _backupState.value = BackupState.Exported(pending.filename)
            } catch (t: Throwable) {
                pendingExport = null
                _backupState.value = BackupState.Failed(
                    resolveMessage(t, R.string.backup_error_export_failed)
                )
            }
        }
    }

    /** Discards a prepared export without writing anything. */
    fun cancelExport() {
        pendingExport = null
        _backupState.value = BackupState.Idle
    }

    /**
     * Called when the user has picked a file to import. Records the URI
     * so the UI can prompt for the key.
     */
    fun beginImport(uri: Uri) {
        _backupState.value = BackupState.ImportAwaitingKey(uri)
    }

    /**
     * Called once the user has entered a key. Doesn't touch the
     * database yet — it moves to a confirmation step, since the import
     * wipes all existing trips.
     */
    fun submitImportKey(uri: Uri, keyInput: String) {
        _backupState.value = BackupState.ImportConfirming(uri, keyInput)
    }

    /**
     * Runs the destructive import: decrypts the backup, replaces every
     * row in the database in a single transaction, and reports the
     * number of trips restored.
     */
    fun confirmImport() {
        val pending = _backupState.value as? BackupState.ImportConfirming ?: return
        viewModelScope.launch {
            _backupState.value = BackupState.Working
            val app = getApplication<Application>()
            try {
                val key = Encryption.parseKey(pending.key)
                    ?: throw BackupException(R.string.backup_error_invalid_key)
                val bytes = app.contentResolver
                    .openInputStream(pending.uri)?.use { it.readBytes() }
                    ?: throw BackupException(R.string.backup_error_read_file)
                val json = Encryption.decryptBlob(bytes, key)
                    ?: throw BackupException(R.string.backup_error_decrypt)
                val importedTrips = TripBackup.deserializeTrips(json)
                dao.replaceAll(importedTrips)
                _backupState.value = BackupState.Imported(importedTrips.size)
            } catch (t: Throwable) {
                _backupState.value = BackupState.Failed(
                    resolveMessage(t, R.string.backup_error_import_failed)
                )
            }
        }
    }

    /** Clears any pending export/import dialog state. */
    fun dismissBackupState() {
        _backupState.value = BackupState.Idle
    }

    /**
     * Set to a content:// URI when a CSV has been prepared for sharing.
     * The UI observes this, launches the share sheet, and then clears
     * it via [clearShareUri].
     */
    private val _shareUri = MutableStateFlow<Uri?>(null)
    val shareUri: StateFlow<Uri?> = _shareUri

    /**
     * Builds a CSV of all trips and writes it to the cache, exposing
     * the resulting URI via [shareUri].
     */
    fun shareTripsAsCsv() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            try {
                val allTrips = dao.getAll().first()
                if (allTrips.isEmpty()) {
                    _backupState.value = BackupState.Failed(
                        app.getString(R.string.backup_error_no_trips)
                    )
                    return@launch
                }
                _shareUri.value = shareCsv(app, allTrips)
            } catch (t: Throwable) {
                _backupState.value = BackupState.Failed(
                    resolveMessage(t, R.string.backup_error_prepare_csv)
                )
            }
        }
    }

    /** Clears the pending share URI once the share sheet has been launched. */
    fun clearShareUri() {
        _shareUri.value = null
    }
}