import CoreLocation
import Flutter
import OSLog

class BeaconFenceBackgroundApiImpl: FlutterBeaconFenceBackgroundApi {
    private let log = Logger(subsystem: Constants.PACKAGE_NAME, category: "BeaconFenceBackgroundApiImpl")
    
    private let binaryMessenger: FlutterBinaryMessenger
    
    private var beaconEventQueue: [BeaconCallbackParamsWire] = .init()
    private var isClosed: Bool = false
    private var flutterBeaconTriggerApi: FlutterBeaconFenceTriggerApi? = nil
    private var cleanup: (() -> Void)? = nil
    
    init(binaryMessenger: FlutterBinaryMessenger) {
        self.binaryMessenger = binaryMessenger
    }

    func triggerApiInitialized() throws {
        objc_sync_enter(self)
        
        if (flutterBeaconTriggerApi == nil) {
            flutterBeaconTriggerApi = FlutterBeaconFenceTriggerApi(binaryMessenger: binaryMessenger)
            log.debug("FlutterBeaconFenceTriggerApi setup complete.")
        }
        objc_sync_exit(self)
        
       if beaconEventQueue.isEmpty {
            log.debug("Waiting for geofence or beacon event...")
            return
        }
        processQueues()
    }

    func promoteToForeground() throws {
        log.info("promoteToForeground called. iOS does not distinguish between foreground and background, nothing to do here.")
    }
    
    func demoteToBackground() throws {
        log.info("demoteToBackground called. iOS does not distinguish between foreground and background, nothing to do here.")
    }
    
    func beaconTriggered(params: BeaconCallbackParamsWire, cleanup: @escaping () -> Void) {
        objc_sync_enter(self)
        
        beaconEventQueue.append(params)
        self.cleanup = cleanup
        
        objc_sync_exit(self)
        
        guard let flutterBeaconTriggerApi else {
            log.debug("Waiting for FlutterBeaconFenceTriggerApi to become available...")
            return
        }
        processQueues()
    }
    
    // MARK: - Private Methods
    
    private func processQueues() {
        objc_sync_enter(self)
        defer { objc_sync_exit(self) }
        
        if isClosed {
            log.error("BeaconFenceBackgroundApi already closed, ignoring additional events.")
            return
        }
        
        // Then process beacon events
        if !beaconEventQueue.isEmpty {
            let params = beaconEventQueue.removeFirst()
            log.debug("Queue dispatch: sending beacon trigger event for IDs=[\(BeaconFenceBackgroundApiImpl.beaconIds(params))].")
            callBeaconTriggerApi(params: params)
            return
        }
        
        // Now that event queue is empty we can cleanup and de-allocate this class.
        cleanup?()
        isClosed = true
    }

    private func callBeaconTriggerApi(params: BeaconCallbackParamsWire) {
        guard let api = flutterBeaconTriggerApi else {
            log.error("FlutterBeaconFenceTriggerApi was nil, this should not happen.")
            return
        }
        log.debug("Calling Dart callback to process beacon trigger for IDs=[\(BeaconFenceBackgroundApiImpl.beaconIds(params))] event=\(String(describing: params.event)).")
        api.beaconTriggered(params: params, completion: { result in
            if case .success = result {
                self.log.debug("Beacon trigger event for IDs=[\(BeaconFenceBackgroundApiImpl.beaconIds(params))] processed successfully.")
            } else {
                self.log.error("Beacon trigger event for IDs=[\(BeaconFenceBackgroundApiImpl.beaconIds(params))] failed.")
            }
            // Now that the callback is complete we can process the next item in the queue, if any.
            self.processQueues()
        })
    }
    
    private static func beaconIds(_ params: BeaconCallbackParamsWire) -> String {
        let ids: [String] = params.beacons.map(\.id)
        return ids.joined(separator: ",")
    }
}
