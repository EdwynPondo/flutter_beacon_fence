package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.ActiveBeaconWire
import com.flutter.beacon_fence.generated.BeaconEvent
import kotlinx.serialization.Serializable

@Serializable
class ActiveBeaconStorage(
    private val id: String,
    private val uuid: String,
    private val major: Long? = null,
    private val minor: Long? = null,
    private val rssi: Long? = null,
    private val triggers: List<Int>,
    private val androidSettings: AndroidBeaconSettingsStorage? = null
) {
    companion object {
        fun fromWire(e: ActiveBeaconWire): ActiveBeaconStorage {
            return ActiveBeaconStorage(
                e.id,
                e.uuid,
                e.major,
                e.minor,
                e.rssi,
                e.triggers.map { it.raw },
                e.androidSettings?.let { AndroidBeaconSettingsStorage.fromWire(it) }
            )
        }
    }

    fun toWire(): ActiveBeaconWire {
        return ActiveBeaconWire(
            id,
            uuid,
            major,
            minor,
            rssi,
            triggers.map { BeaconEvent.ofRaw(it)!! },
            androidSettings?.toWire()
        )
    }
}
