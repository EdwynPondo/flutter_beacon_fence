package com.flutter.beacon_fence.util

import com.flutter.beacon_fence.generated.ActiveBeaconWire
import com.flutter.beacon_fence.generated.BeaconWire

class ActiveBeaconWires {
    companion object {
        fun fromBeaconWire(e: BeaconWire): ActiveBeaconWire {
            return ActiveBeaconWire(
                e.id,
                e.uuid,
                e.major,
                e.minor,
                null,
                e.triggers,
                e.androidSettings
            )
        }
    }
}
