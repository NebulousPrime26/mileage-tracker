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
     * UI state for the export/import flow. Import replaces the entire
     * database, so the flow includes an explicit confirmation step
     * before anything is deleted.
     */
    sealed interface BackupState {
        /** No backup operation in progress. */
        object Idle : BackupState

        /** An export or import is running. */
        object Working : BackupState

        /** Export finished; the key must be shown to the user exactly once. */
        data class Exported(val key: String, val filename: String) : BackupState

        /** The user picked a file and needs to supply the key. */
        data class ImportAwaitingKey(val uri: Uri) : BackupState

        /** The key is entered; awaiting confirmation of the destructive replace. */
        data class ImportConfirming(val uri: Uri, val key: String) : BackupState

        /** Import succeeded, with the number of trips inserted. */
        data class Imported(val count: Int) : BackupState

        /** Something went wrong in either direction. */
        data class Failed(val message: String) : BackupState
    }

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
     * Encrypts all trips and writes them to Downloads. On success,
     * exposes the encryption key via [backupState] so the UI can show
     * it to the user exactly once.
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
                TripBackup.writeToDownloads(
                    context = getApplication(),
                    filename = filename,
                    bytes = encrypted,
                )
                _backupState.value = BackupState.Exported(
                    key = TripCrypto.formatKey(key),
                    filename = filename,
                )
            } catch (t: Throwable) {
                _backupState.value = BackupState.Failed(
                    t.message ?: "Export failed",
                )
            }
        }
    }

    /**
     * Called when the user has picked a file to import. Records the URI
     * so the UI can prompt for the key.
     */
    fun beginImport(uri: Uri) {
        _backupState.value = BackupState.ImportAwaitingKey(uri)
    }

    /**
     * Called once the user has entered a key. Doesn't touch the database
     * yet — it moves to a confirmation step, since the actual import
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