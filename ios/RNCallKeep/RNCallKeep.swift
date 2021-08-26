import Foundation
import CallKit

@objc(RNCallKeep)

class RNCallKeep: NSObject {
    
    var cxCallController: CXCallController?
    var callKeepProvider: CXProvider?
    
    // MARK: - Private params
    private var _version: OperatingSystemVersion?
    static private let _settingsKey = "RNCallKeepSettings"
    static var sharedProvider: CXProvider?
    
    // MARK: - Singleton init
    static let sharedInstance: RNCallKeep = {
        let callkeep = RNCallKeep()
        return callkeep
    }()
    
    static func createCallKitProvider() {
        
        let settings = UserDefaults.standard.object(forKey: _settingsKey) as! [String: Any]
        
        guard let config = RNCallKeep.getProviderConfiguration(settings: settings) else {
            
            print("[RNCallKeep][createCallKitProvider] Fails to retrieve config")
            return
        }
        
        sharedProvider = CXProvider(configuration: config)
    }
    
    // MARK: - Class func
    @objc(options:)
    class func setup(options: [String: Any]) {
        
        sharedInstance.setup(options: options)        
    }
    
    /// Return provider config
    /// - Parameter settings: List of available settings
    @objc(settings:)
    class func getProviderConfiguration(settings: [String: Any]) -> CXProviderConfiguration? {
    
        guard let appName = settings["appName"] as? String else {
            print("[RNCallKeep][getProviderConfiguration] Missing key: appName")
            return nil
        }
        
        let config = CXProviderConfiguration(localizedName: appName)
        
        if let supportsVideo = settings["supportsVideo"] as? Bool {
            config.supportsVideo = supportsVideo
        }
        
        if let ringtoneSound = settings["ringtoneSound"] as? String {
            config.ringtoneSound = ringtoneSound
        }
        
        return config
    }
    
    // IMPORTANT
    class func reportIncomingCall(uuidString: String,
                                  handle: String,
                                  handleType: CXHandle.HandleType,
                                  hasVideo: Bool,
                                  localizedCallerName: String?,
                                  supportsHolding: Bool,
                                  supportsDTMF: Bool,
                                  fromPushKit: Bool,
                                  payload: [String: Any]?,
                                  completionHandler: @escaping () -> Void ) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            
            print("[RNCallKeep][reportIncomingCall] Error: Cant create uuid from string")
            return
        }
            
        let cxCallUpdate = CXCallUpdate()
        cxCallUpdate.remoteHandle = CXHandle(type: handleType, value: handle)
        cxCallUpdate.supportsHolding = supportsHolding;
        cxCallUpdate.supportsDTMF = supportsDTMF;
        cxCallUpdate.hasVideo = hasVideo;
        
        if let name = localizedCallerName {
            cxCallUpdate.localizedCallerName = localizedCallerName;
        }
        
        RNCallKeep.createCallKitProvider()
        
        if let sharedProvider = sharedProvider {
            sharedProvider.reportNewIncomingCall(with: uuid, update: cxCallUpdate, completion: { err in
                
            })
        } else {
            
            print("[RNCallKeep][reportIncomingCall] Error: Shared provider is nil")
        }
        
    }
    
    /// Setup CXProvider and CXCallController
    /// - Parameter options: List of options
    @objc(options:)
    func setup(options: [String: Any]) {
        
        _version = ProcessInfo().operatingSystemVersion
        self.cxCallController = CXCallController()
        
        // Save default settings
        let standard = UserDefaults.standard
        standard.set(options, forKey: RNCallKeep._settingsKey)
        
        RNCallKeep.createCallKitProvider()
        
        self.callKeepProvider = RNCallKeep.sharedProvider;
        self.callKeepProvider?.setDelegate(self, queue: nil)
    }
    
    
    /// Activating a mute call action
    /// - Parameter uuidString: Device's uuid
    /// - Parameter muted: Mute or unmute the recipient
    @objc(uuid:muted:)
    func setMutedCall(uuidString: String, muted: Bool) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("Cant find uuid")
            return
        }
        let mutedAction = CXSetMutedCallAction(call: uuid, muted: muted)
        let transaction = CXTransaction()
        transaction.addAction(mutedAction)
        
        // Request transaction
        self.requestTransaction(transaction)
    }
    
    // MARK: - Private func
    private func requestTransaction(_ transaction: CXTransaction) {
        
        if self.cxCallController == nil {
            self.cxCallController = CXCallController()
        }
        
        self.cxCallController?.request(transaction, completion: { err in
            
            if let err = err {
                print("[RNCallKeep][requestTransaction] Error request transaction \(transaction.actions): \(err)")
                return
            }
            
            print("[RNCallKeep][requestTransaction] Requested transaction successfully")
            
            // Handle start call transaction
            if let action = transaction.actions.first {
                if let startAction = action as? CXStartCallAction {
                    
                    let callUpdate = CXCallUpdate()
                    callUpdate.remoteHandle = startAction.handle
                    callUpdate.hasVideo = startAction.isVideo
                    callUpdate.localizedCallerName = startAction.contactIdentifier
                    callUpdate.supportsDTMF = true;
                    callUpdate.supportsHolding = true;
                    
                    // reportCallWithUUID
                    
                }
            }
        })
    }
}

// MARK: - CXProvider delegate
extension RNCallKeep: CXProviderDelegate {
    
    func providerDidReset(_ provider: CXProvider) {
        
    }
    
    /// Answer incoming call
    func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        
    }
    
    /// End ongoing call
    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        
    }
}
