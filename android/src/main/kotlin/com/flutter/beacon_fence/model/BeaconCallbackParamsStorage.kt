package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.BeaconCallbackParamsWire
import com.flutter.beacon_fence.generated.BeaconEvent
import kotlinx.serialization.Serializable

@Serializable
class BeaconCallbackParamsStorage(
    private val beacons: List<ActiveBeaconStorage>,
    private val event: Int,
    private val callbackHandle: Long
) {
    companion object {
        fun fromWire(e: BeaconCallbackParamsWire): BeaconCallbackParamsStorage {
            return BeaconCallbackParamsStorage(
                e.beacons.map { ActiveBeaconStorage.fromWire(it) },
                e.event.raw,
                e.callbackHandle
            )
        }
    }

    fun toWire(): BeaconCallbackParamsWire {
        return BeaconCallbackParamsWire(
            beacons.map { it.toWire() },
            BeaconEvent.ofRaw(event)!!,
            callbackHandle
        )
    }
}
