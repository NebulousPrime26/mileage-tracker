package com.nebulousprime26.mileage_tracker.settings.model

enum class Theme (val darkScheme: Boolean?){
    SYSTEM(null),
    LIGHT(false),
    DARK(true);

    companion object {
        fun fromStorage(value: String?): Theme =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
