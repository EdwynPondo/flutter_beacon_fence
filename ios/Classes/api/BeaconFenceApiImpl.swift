import CoreLocation
import Flutter
import OSLog
import UIKit

public class BeaconFenceApiImpl: NSObject, FlutterBeaconFenceApi {
    private let log = Logger(subsystem: Constants.PACKAGE_NAME, category: "BeaconFenceApiImpl")
    
    private let locationManagerDelegate: LocationManagerDelegate
    
    init(registerPlugins: FlutterPluginRegistrantCallback) {
        self.locationManagerDelegate = LocationManagerDelegate(flutterPluginRegistrantCallback: registerPlugins)
    }
        
    func initialize(callbackDispatcherHandle: Int64) throws {
        FlutterBeaconFencePersistence.setCallbackDispatcherHandle(callbackDispatcherHandle)
    }

    func reCreateAfterReboot() throws {
        log.info("Re-create after reboot called. iOS handles this automatically, nothing for us to do here.")
    }
        
    func createBeacon(beacon: BeaconWire, completion: @escaping (Result<Void, any Error>) -> Void) {
        guard let beaconUUID = UUID(uuidString: beacon.uuid) else {
            log.error("Invalid UUID format: \(beacon.uuid)")
            completion(.failure(NSError(domain: "FlutterBeaconFenceApi", code: -1, userInfo: [NSLocalizedDescriptionKey: "Invalid UUID format"])))
            return
        }
        
        let constraint: CLBeaconIdentityConstraint
        
        if let major = beacon.major, let minor = beacon.minor {
            // Monitor specific beacon with UUID, major, and minor
            constraint = CLBeaconIdentityConstraint(
                uuid: beaconUUID,
                major: CLBeaconMajorValue(major),
                minor: CLBeaconMinorValue(minor)
            )
        } else if let major = beacon.major {
            // Monitor beacons with UUID and major
            constraint = CLBeaconIdentityConstraint(
                uuid: beaconUUID,
                major: CLBeaconMajorValue(major)
            )
        } else {
            // Monitor all beacons with UUID
            constraint = CLBeaconIdentityConstraint(uuid: beaconUUID)
        }
        
        let beaconRegion = CLBeaconRegion(beaconIdentityConstraint: constraint, identifier: beacon.id)
        
        beaconRegion.notifyOnEntry = beacon.triggers.contains(.enter)
        beaconRegion.notifyOnExit = beacon.triggers.contains(.exit)
        beaconRegion.notifyEntryStateOnDisplay = beacon.iosSettings.notifyEntryStateOnDisplay
        
        FlutterBeaconFencePersistence.setRegionCallbackHandle(id: beacon.id, handle: beacon.callbackHandle)
        
        locationManagerDelegate.locationManager.startMonitoring(for: beaconRegion)
        if beacon.iosSettings.initialTrigger {
            locationManagerDelegate.locationManager.requestState(for: beaconRegion)
        }
        
        log.debug("Created beacon ID=\(beacon.id) with UUID=\(beacon.uuid).")
        
        completion(.success(()))
    }
    
    func getBeaconIds() throws -> [String] {
        var beaconIds: [String] = []
        for region in locationManagerDelegate.locationManager.monitoredRegions {
            if region is CLBeaconRegion {
                beaconIds.append(region.identifier)
            }
        }
        log.debug("getBeaconIds() found \(beaconIds.count) beacon(s).")
        return beaconIds
    }
    
    func getBeacons() throws -> [ActiveBeaconWire] {
        var beacons: [ActiveBeaconWire] = []
        for region in locationManagerDelegate.locationManager.monitoredRegions {
            if let activeBeacon = ActiveBeaconWires.fromRegion(region) {
                beacons.append(activeBeacon)
            }
        }
        log.debug("getBeacons() found \(beacons.count) beacon(s).")
        return beacons
    }
    
    func removeBeaconById(id: String, completion: @escaping (Result<Void, any Error>) -> Void) {
        var removedCount = 0
        for region in locationManagerDelegate.locationManager.monitoredRegions {
            if region.identifier == id && region is CLBeaconRegion {
                locationManagerDelegate.locationManager.stopMonitoring(for: region)
                FlutterBeaconFencePersistence.removeRegionCallbackHandle(id: region.identifier)
                removedCount += 1
            }
        }
        log.debug("Removed \(removedCount) beacon(s) with ID=\(id).")
        completion(.success(()))
    }
    
    func removeAllBeacons(completion: @escaping (Result<Void, any Error>) -> Void) {
        var removedCount = 0
        for region in locationManagerDelegate.locationManager.monitoredRegions {
            if region is CLBeaconRegion {
                locationManagerDelegate.locationManager.stopMonitoring(for: region)
                FlutterBeaconFencePersistence.removeRegionCallbackHandle(id: region.identifier)
                removedCount += 1
            }
        }
        log.debug("Removed \(removedCount) beacon(s).")
        completion(.success(()))
    }
    
    func configureAndroidMonitor(settings: AndroidScannerSettingsWire, completion: @escaping (Result<Void, any Error>) -> Void) {
        // This method is Android specific and does nothing on iOS.
        completion(.success(()))
    }
}
