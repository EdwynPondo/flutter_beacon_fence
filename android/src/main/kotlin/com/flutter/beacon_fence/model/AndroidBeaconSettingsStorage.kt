package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.AndroidBeaconSettingsWire
import com.flutter.beacon_fence.generated.BeaconEvent
import kotlinx.serialization.Serializable

@Serializable
class AndroidBeaconSettingsStorage(
    private val initialTriggers: List<Int>
) {
    companion object {
        fun fromWire(e: AndroidBeaconSettingsWire): AndroidBeaconSettingsStorage {
            return AndroidBeaconSettingsStorage(
                e.initialTriggers.map { it.raw }
            )
        }
    }

    fun toWire(): AndroidBeaconSettingsWire {
        return AndroidBeaconSettingsWire(
            initialTriggers.map { BeaconEvent.ofRaw(it)!! }
        )
    }
}
