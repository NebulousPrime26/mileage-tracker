package com.nebulousprime26.mileage_tracker.settings

/**
 * Which language the app should display. SYSTEM follows the device
 * setting, ENGLISH and DUTCH force a specific locale.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    DUTCH("nl");

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}