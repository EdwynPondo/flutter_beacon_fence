import Flutter
import OSLog
import UIKit

public class FlutterBeaconFencePlugin: NSObject, FlutterPlugin {
    private static let log = Logger(subsystem: Constants.PACKAGE_NAME, category: "FlutterBeaconFencePlugin")
    
    private static var registerPlugins: FlutterPluginRegistrantCallback? = nil
    private static var instance: FlutterBeaconFencePlugin? = nil
    
    private var locationManagerDelegate: LocationManagerDelegate? = nil
    private var beaconFenceApi: BeaconFenceApiImpl? = nil
    
    init(registrar: FlutterPluginRegistrar, registerPlugins: FlutterPluginRegistrantCallback) {
        // Create single API instance that implements BeaconFenceApi
        beaconFenceApi = BeaconFenceApiImpl(registerPlugins: registerPlugins)
        
        FlutterBeaconFenceApiSetup.setUp(binaryMessenger: registrar.messenger(), api: beaconFenceApi)
        FlutterBeaconFencePlugin.log.debug("BeaconFenceApi initialized.")
    }
    
    /// Called from the Flutter plugins AppDelegate.swift.
    public static func setPluginRegistrantCallback(_ callback: FlutterPluginRegistrantCallback) {
        registerPlugins = callback
        log.debug("registerPlugins updated.")
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        objc_sync_enter(self)
        defer { objc_sync_exit(self) }
        
        if instance != nil { return }
        
        guard let registerPlugins else {
            log.error("registerPlugins was nil at application launch.")
            fatalError("Please ensure you have updated your ios/Runner/AppDelegate to call setPluginRegistrantCallback. See the plugin documentation for more information.")
        }
        
        let plugin = FlutterBeaconFencePlugin(registrar: registrar, registerPlugins: registerPlugins)
        registrar.addApplicationDelegate(plugin)
        instance = plugin
        
        log.debug("FlutterBeaconFencePlugin registered.")
    }
    
    public func detachFromEngine(for registrar: any FlutterPluginRegistrar) {
        beaconFenceApi = nil
        FlutterBeaconFencePlugin.instance = nil
        FlutterBeaconFencePlugin.log.debug("FlutterBeaconFencePlugin detached.")
    }
}
