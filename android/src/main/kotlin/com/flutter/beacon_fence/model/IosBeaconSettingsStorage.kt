package com.flutter.beacon_fence.model

import com.flutter.beacon_fence.generated.IosBeaconSettingsWire
import kotlinx.serialization.Serializable

@Serializable
class IosBeaconSettingsStorage(
    private val initialTrigger: Boolean,
    private val notifyEntryStateOnDisplay: Boolean
) {
    companion object {
        fun fromWire(e: IosBeaconSettingsWire): IosBeaconSettingsStorage {
            return IosBeaconSettingsStorage(
                e.initialTrigger,
                e.notifyEntryStateOnDisplay
            )
        }
    }

    fun toWire(): IosBeaconSettingsWire {
        return IosBeaconSettingsWire(
            initialTrigger,
            notifyEntryStateOnDisplay
        )
    }
}
