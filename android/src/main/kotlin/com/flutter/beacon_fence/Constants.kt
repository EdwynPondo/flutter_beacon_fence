package com.flutter.beacon_fence

import org.altbeacon.beacon.BeaconParser

class Constants {
    companion object {
        private const val PACKAGE_NAME = "com.flutter.beacon_fence"

        const val SHARED_PREFERENCES_KEY = "beacon_fence_plugin_cache"
        const val PERSISTENT_BEACONS_IDS_KEY = "persistent_beacons_ids"
        const val PERSISTENT_BEACON_KEY_PREFIX = "persistent_beacon/"
        const val PERSISTENT_SCANNER_SETTINGS_KEY = "persistent_scanner_settings"

        const val BEACON_CALLBACK_DISPATCHER_HANDLE_KEY = "beacon_callback_dispatch_handler"

        const val WORKER_PAYLOAD_KEY = "$PACKAGE_NAME.worker_payload"
        const val BEACON_CALLBACK_WORK_GROUP = "beacon_callback_work_group"

        val IBEACON_PARSER = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
    }
}
