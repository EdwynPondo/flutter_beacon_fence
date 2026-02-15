package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.AndroidNotificationsSettingsWire
import com.flutter.beacon_fence.generated.AndroidScannerSettingsWire
import kotlinx.serialization.Serializable

@Serializable
class AndroidScannerSettingsStorage(
    private val foregroundScanPeriodMillis: Long,
    private val foregroundBetweenScanPeriodMillis: Long,
    private val backgroundScanPeriodMillis: Long,
    private val backgroundBetweenScanPeriodMillis: Long,
    private val useForegroundService: Boolean,
    private val notificationsSettings: AndroidNotificationSettingStore = AndroidNotificationSettingStore.DEFAULT
) {
    @Serializable
    data class AndroidNotificationSettingStore(
        val title: String,
        val content: String
    ) {
        companion object {
            val DEFAULT = AndroidNotificationSettingStore(
                "Listening for sessions",
                "We will keep you updated"
            )

            val DEFAULT_WIRE = AndroidNotificationsSettingsWire(
                "Listening for sessions",
                "We will keep you updated"
            )

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
        fun fromWire(e: AndroidScannerSettingsWire): AndroidScannerSettingsStorage {
            val notificationsSettings = (e.notificationsSettings)?.let {
                AndroidNotificationSettingStore.fromWire(it)
            } ?: AndroidNotificationSettingStore.DEFAULT
            return AndroidScannerSettingsStorage(
                e.foregroundScanPeriodMillis,
                e.foregroundBetweenScanPeriodMillis,
                e.backgroundScanPeriodMillis,
                e.backgroundBetweenScanPeriodMillis,
                e.useForegroundService,
                notificationsSettings
            )
        }
    }

    fun toWire(): AndroidScannerSettingsWire {
        return AndroidScannerSettingsWire(
            foregroundScanPeriodMillis,
            foregroundBetweenScanPeriodMillis,
            backgroundScanPeriodMillis,
            backgroundBetweenScanPeriodMillis,
            useForegroundService,
            notificationsSettings.toWire()
        )
    }
}
