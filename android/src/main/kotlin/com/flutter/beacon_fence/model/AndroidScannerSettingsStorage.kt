package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.AndroidNotificationsSettingsWire
import com.flutter.beacon_fence.generated.AndroidScanStrategy
import com.flutter.beacon_fence.generated.AndroidScannerSettingsWire
import kotlinx.serialization.Serializable

@Serializable
class AndroidScannerSettingsStorage(
    private val foregroundScanPeriodMillis: Long,
    private val foregroundBetweenScanPeriodMillis: Long,
    private val backgroundScanPeriodMillis: Long,
    private val backgroundBetweenScanPeriodMillis: Long,
    // Raw value of AndroidScanStrategy. Null for settings persisted before scan strategies existed.
    private val scanStrategy: Int? = null,
    private val regionExitPeriodMillis: Long = DEFAULT_REGION_EXIT_PERIOD_MILLIS,
    // Legacy field, only read to migrate settings persisted before scan strategies existed.
    private val useForegroundService: Boolean? = null,
    private val notificationsSettings: AndroidNotificationSettingStore = AndroidNotificationSettingStore.DEFAULT
) {
    @Serializable
    data class AndroidNotificationSettingStore(
        val title: String,
        val content: String
    ) {
        companion object {
            private const val DEFAULT_TITLE = "Beacon scanning active"
            private const val DEFAULT_CONTENT = "Detecting nearby beacons"

            val DEFAULT = AndroidNotificationSettingStore(DEFAULT_TITLE, DEFAULT_CONTENT)

            val DEFAULT_WIRE = AndroidNotificationsSettingsWire(DEFAULT_TITLE, DEFAULT_CONTENT)

            fun fromWire(e: AndroidNotificationsSettingsWire): AndroidNotificationSettingStore {
                return AndroidNotificationSettingStore(
                    e.title,
                    e.content
                )
            }
        }

        fun toWire(): AndroidNotificationsSettingsWire {
            return AndroidNotificationsSettingsWire(
                title,
                content
            )
        }
    }

    companion object {
        const val DEFAULT_REGION_EXIT_PERIOD_MILLIS = 10_000L

        /** Must match the defaults of the Dart `AndroidScannerSettings`. */
        val DEFAULT_WIRE = AndroidScannerSettingsWire(
            foregroundScanPeriodMillis = 1100,
            foregroundBetweenScanPeriodMillis = 0,
            backgroundScanPeriodMillis = 1100,
            backgroundBetweenScanPeriodMillis = 0,
            scanStrategy = AndroidScanStrategy.FOREGROUND_SERVICE,
            regionExitPeriodMillis = DEFAULT_REGION_EXIT_PERIOD_MILLIS,
            notificationsSettings = AndroidNotificationSettingStore.DEFAULT_WIRE
        )

        fun fromWire(e: AndroidScannerSettingsWire): AndroidScannerSettingsStorage {
            val notificationsSettings = (e.notificationsSettings)?.let {
                AndroidNotificationSettingStore.fromWire(it)
            } ?: AndroidNotificationSettingStore.DEFAULT
            return AndroidScannerSettingsStorage(
                foregroundScanPeriodMillis = e.foregroundScanPeriodMillis,
                foregroundBetweenScanPeriodMillis = e.foregroundBetweenScanPeriodMillis,
                backgroundScanPeriodMillis = e.backgroundScanPeriodMillis,
                backgroundBetweenScanPeriodMillis = e.backgroundBetweenScanPeriodMillis,
                scanStrategy = e.scanStrategy.raw,
                regionExitPeriodMillis = e.regionExitPeriodMillis,
                notificationsSettings = notificationsSettings
            )
        }
    }

    fun toWire(): AndroidScannerSettingsWire {
        val strategy = scanStrategy?.let { AndroidScanStrategy.ofRaw(it) }
            ?: if (useForegroundService == true) {
                AndroidScanStrategy.FOREGROUND_SERVICE
            } else {
                AndroidScanStrategy.JOB_SCHEDULER
            }
        return AndroidScannerSettingsWire(
            foregroundScanPeriodMillis,
            foregroundBetweenScanPeriodMillis,
            backgroundScanPeriodMillis,
            backgroundBetweenScanPeriodMillis,
            strategy,
            regionExitPeriodMillis,
            notificationsSettings.toWire()
        )
    }
}
