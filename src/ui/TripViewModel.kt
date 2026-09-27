package com.nebulousprime26.mileage_tracker.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nebulousprime26.mileage_tracker.data.SettingsRepository
import com.nebulousprime26.mileage_tracker.data.ThemeMode
import com.nebulousprime26.mileage_tracker.data.Trip
import com.nebulousprime26.mileage_tracker.data.TripBackup
import com.nebulousprime26.mileage_tracker.data.TripCrypto
import com.nebulousprime26.mileage_tracker.data.TripCsv
import com.nebulousprime26.mileage_tracker.data.TripDao
import com.nebulousprime26.mileage_tracker.data.getDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TripViewModel(app: Application) : AndroidViewModel(app) {

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
    private class PendingExport(
        val key: ByteArray,
        val filename: String,
        val encryptedBytes: ByteArray,
    )

    private var pendingExport: PendingExport? = null

    private val dao: TripDao = getDatabase(app).tripDao()
    private val settingsRepo = SettingsRepository(app)

    // ── Trip data ────────────────────────────────────────────────────

    val trips: StateFlow<List<Trip>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val privateTrips: StateFlow<List<Trip>> = dao.getByPrivateUse(true)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val maxEndMileage: StateFlow<Double?> = dao.getMaxEndMileage()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val lastEndPostalCode: StateFlow<String?> = dao.getLastEndPostalCode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val lastStartPostalCode: StateFlow<String?> = dao.getLastStartPostalCode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    val lastLicensePlate: StateFlow<String?> = dao.getLastLicensePlate()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    // ── User settings ────────────────────────────────────────────────

    val fabOnRight: StateFlow<Boolean> = settingsRepo.fabOnRight
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setFabOnRight(onRight: Boolean) {
        viewModelScope.launch { settingsRepo.setFabOnRight(onRight) }
    }

    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ThemeMode.SYSTEM,
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    }

    val postalFirst: StateFlow<Boolean> = settingsRepo.postalFirst
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setPostalFirst(value: Boolean) {
        viewModelScope.launch { settingsRepo.setPostalFirst(value) }
    }

    val draftLeft: StateFlow<Boolean> = settingsRepo.draftLeft
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setDraftLeft(value: Boolean) {
        viewModelScope.launch { settingsRepo.setDraftLeft(value) }
    }

    val roundTripAssumption: StateFlow<Boolean> = settingsRepo.roundTripAssumption
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setRoundTripAssumption(value: Boolean) {
        viewModelScope.launch { settingsRepo.setRoundTripAssumption(value) }
    }

    val autoFillEndTime: StateFlow<Boolean> = settingsRepo.autoFillEndTime
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true,
        )

    fun setAutoFillEndTime(value: Boolean) {
        viewModelScope.launch { settingsRepo.setAutoFillEndTime(value) }
    }

    val allowSpacesInPostal: StateFlow<Boolean> = settingsRepo.allowSpacesInPostal
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    fun setAllowSpacesInPostal(value: Boolean) {
        viewModelScope.launch { settingsRepo.setAllowSpacesInPostal(value) }
    }

    // ── Statistics filters ───────────────────────────────────────────

    private val _filterStart = MutableStateFlow<Long?>(null)
    private val _filterEnd = MutableStateFlow<Long?>(null)

    val filterStart: StateFlow<Long?> = _filterStart
    val filterEnd: StateFlow<Long?> = _filterEnd

    fun setFilterStart(millis: Long?) { _filterStart.value = millis }
    fun setFilterEnd(millis: Long?) { _filterEnd.value = millis }
    fun clearFilters() {
        _filterStart.value = null
        _filterEnd.value = null
    }

    val yearlyStats: StateFlow<List<YearlyStats>> = combine(
        dao.getAll(),
        _filterStart,
        _filterEnd,
    ) { trips, start, end ->
        trips
            .filter { trip ->
                (start == null || trip.startDate >= start) &&
                (end == null || trip.startDate <= end)
            }
            .groupBy { trip ->
                Calendar.getInstance().apply { timeInMillis = trip.startDate }
                    .get(Calendar.YEAR)
            }
            .map { (year, yearTrips) ->
                val privateTrips = yearTrips.filter { it.privateUse }
                val businessTrips = yearTrips.filter { !it.privateUse }
                YearlyStats(
                    year = year,
                    privateMileage = privateTrips.sumOf { it.distanceMileage },
                    businessMileage = businessTrips.sumOf { it.distanceMileage },
                    privateDurationMillis = privateTrips.sumOf { it.durationMillis },
                    businessDurationMillis = businessTrips.sumOf { it.durationMillis },
                    privateTripCount = privateTrips.size,
                    businessTripCount = businessTrips.size,
                )
            }
            .sortedBy { it.year }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    val monthlyStats: StateFlow<List<MonthlyStats>> = combine(
        dao.getAll(),
        _filterStart,
        _filterEnd,
    ) { trips, start, end ->
        trips
            .filter { trip ->
                (start == null || trip.startDate >= start) &&
                (end == null || trip.startDate <= end)
            }
            .groupBy { trip ->
                val cal = Calendar.getInstance().apply { timeInMillis = trip.startDate }
                cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
            }
            .map { (key, monthTrips) ->
                val privateTrips = monthTrips.filter { it.privateUse }
                val businessTrips = monthTrips.filter { !it.privateUse }
                MonthlyStats(
                    year = key.first,
                    month = key.second,
                    privateMileage = privateTrips.sumOf { it.distanceMileage },
                    businessMileage = businessTrips.sumOf { it.distanceMileage },
                    privateDurationMillis = privateTrips.sumOf { it.durationMillis },
                    businessDurationMillis = businessTrips.sumOf { it.durationMillis },
                    privateTripCount = privateTrips.size,
                    businessTripCount = businessTrips.size,
                )
            }
            .sortedWith(compareBy({ it.year }, { it.month }))
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    // ── Backup (export / import) ─────────────────────────────────────

    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    /**
     * Prepares the backup: reads all trips, generates a key, and
     * encrypts everything in memory. Nothing is written to disk until
     * [confirmExport] is called.
     */
    fun exportTrips() {
        viewModelScope.launch {
            _backupState.value = BackupState.Working
            try {
                val allTrips = dao.getAll().first()
                val json = TripBackup.serializeTrips(allTrips)
                val key = TripCrypto.generateKey()
                val encrypted = TripCrypto.encrypt(json, key)
                val filename = TripBackup.defaultFilename()

                pendingExport = PendingExport(
                    key = key,
                    filename = filename,
                    encryptedBytes = encrypted,
                )
                _backupState.value = BackupState.ExportPreview(
                    key = TripCrypto.formatKey(key),
                    filename = filename,
                )
            } catch (t: Throwable) {
                pendingExport = null
                _backupState.value = BackupState.Failed(
                    t.message ?: "Export failed",
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
            _backupState.value = BackupState.Failed("Nothing to export.")
            return
        }
        viewModelScope.launch {
            _backupState.value = BackupState.Working
            try {
                TripBackup.writeToDownloads(
                    context = getApplication(),
                    filename = pending.filename,
                    bytes = pending.encryptedBytes,
                )
                pendingExport = null
                _backupState.value = BackupState.Exported(pending.filename)
            } catch (t: Throwable) {
                pendingExport = null
                _backupState.value = BackupState.Failed(
                    t.message ?: "Export failed",
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
            try {
                val key = TripCrypto.parseKey(pending.key)
                    ?: throw IllegalArgumentException(
                        "The key isn't a valid 64-character hex string."
                    )
                val bytes = getApplication<Application>().contentResolver
                    .openInputStream(pending.uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException(
                        "Could not read the selected file."
                    )
                val json = TripCrypto.decrypt(bytes, key)
                    ?: throw IllegalStateException(
                        "Could not decrypt — the key is wrong or the file is damaged."
                    )
                val importedTrips = TripBackup.deserializeTrips(json)
                dao.replaceAll(importedTrips)
                _backupState.value = BackupState.Imported(importedTrips.size)
            } catch (t: Throwable) {
                _backupState.value = BackupState.Failed(
                    t.message ?: "Import failed",
                )
            }
        }
    }

    /** Clears any pending export/import dialog state. */
    fun dismissBackupState() {
        _backupState.value = BackupState.Idle
    }

    // ── CSV share ────────────────────────────────────────────────────

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
            try {
                val allTrips = dao.getAll().first()
                if (allTrips.isEmpty()) {
                    _backupState.value = BackupState.Failed("No trips to share.")
                    return@launch
                }
                val csv = TripCsv.build(allTrips)
                val filename = TripCsv.defaultFilename()
                val uri = TripCsv.writeToCache(getApplication(), filename, csv)
                _shareUri.value = uri
            } catch (t: Throwable) {
                _backupState.value = BackupState.Failed(
                    t.message ?: "Could not prepare the CSV."
                )
            }
        }
    }

    /** Clears the pending share URI once the share sheet has been launched. */
    fun clearShareUri() {
        _shareUri.value = null
    }

    // ── Mutations ────────────────────────────────────────────────────

    fun addTrip(trip: Trip) {
        viewModelScope.launch { dao.insert(trip) }
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch { dao.update(trip) }
    }

    fun deleteTrip(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }
}