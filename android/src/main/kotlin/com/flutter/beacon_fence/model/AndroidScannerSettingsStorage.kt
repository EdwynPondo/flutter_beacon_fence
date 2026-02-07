package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.AndroidScannerSettingsWire
import kotlinx.serialization.Serializable

@Serializable
class AndroidScannerSettingsStorage(
    private val foregroundScanPeriodMillis: Long,
    private val foregroundBetweenScanPeriodMillis: Long,
    private val backgroundScanPeriodMillis: Long,
    private val backgroundBetweenScanPeriodMillis: Long
) {
    companion object {
        fun fromWire(e: AndroidScannerSettingsWire): AndroidScannerSettingsStorage {
            return AndroidScannerSettingsStorage(
                e.foregroundScanPeriodMillis,
                e.foregroundBetweenScanPeriodMillis,
                e.backgroundScanPeriodMillis,
                e.backgroundBetweenScanPeriodMillis
            )
        }
    }

    fun toWire(): AndroidScannerSettingsWire {
        return AndroidScannerSettingsWire(
            foregroundScanPeriodMillis,
            foregroundBetweenScanPeriodMillis,
            backgroundScanPeriodMillis,
            backgroundBetweenScanPeriodMillis
        )
    }
}
