package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.LocationWire
import kotlinx.serialization.Serializable

@Serializable
class LocationStorage(
    private val latitude: Double,
    private val longitude: Double
) {
    companion object {
        fun fromWire(e: LocationWire): LocationStorage {
            return LocationStorage(e.latitude, e.longitude)
        }
    }

    fun toWire(): LocationWire {
        return LocationWire(
            latitude,
            longitude,
        )
    }
}
