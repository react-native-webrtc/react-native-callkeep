import Foundation
import CallKit
import AVKit

@objc(EYRCallKeep)
class EYRCallKeep: NSObject {
    
    var cxCallController: CXCallController?
    var callKeepProvider: CXProvider?
    static var sharedProvider: CXProvider?
    
    // MARK: - Private params
    private var _hasListener = false
    private var _version: OperatingSystemVersion?
    static private let _settingsKey = "RNCallKeepSettings"
    
    // MARK: - Singleton init
    static let sharedInstance = EYRCallKeep()
    
    static func createCallKitProvider() {
        
        let settings = UserDefaults.standard.object(forKey: _settingsKey) as! [String: Any]
        
        guard let config = EYRCallKeep.getProviderConfiguration(settings: settings) else {
            
            print("[RNCallKeep][createCallKitProvider] Fails to retrieve config")
            return
        }
        
        sharedProvider = CXProvider(configuration: config)
    }
    
    
    // MARK: - Exported methods
    /// Activating a mute call action
    /// - Parameter uuidString: Device's uuid
    /// - Parameter muted: Mute or unmute the recipient
    @objc(setMutedCall:muted:)
    func setMutedCall(_ uuidString: String, muted: Bool) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("[RNCallKeep][setMutedCall] Cant find uuid")
            return
        }
        let mutedAction = CXSetMutedCallAction(call: uuid, muted: muted)
        let transaction = CXTransaction()
        transaction.addAction(mutedAction)
        
        // Request transaction
        self.requestTransaction(transaction)
    }
    
    @objc(endCall:)
    func endCall(_ uuidString: String) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("[RNCallKeep][setMutedCall] Cant find uuid")
            return
        }
        let endCallAction = CXEndCallAction(call: uuid)
        let transaction = CXTransaction()
        transaction.addAction(endCallAction)
        
        // Request transaction
        self.requestTransaction(transaction)
    }
    
    @objc(reportEndCall:reason:)
    func reportEndCall(_ uuidString: String, reason: Int) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("[RNCallKeep][endCall] Cant find uuid")
            return
        }
        
        switch reason {
        case 1:
            self.callKeepProvider?.reportCall(with: uuid, endedAt: Date(), reason: .failed)
            break
        case 2, 6:
            self.callKeepProvider?.reportCall(with: uuid, endedAt: Date(), reason: .remoteEnded)
            break
        case 3:
            self.callKeepProvider?.reportCall(with: uuid, endedAt: Date(), reason: .unanswered)
            break
        case 4:
            self.callKeepProvider?.reportCall(with: uuid, endedAt: Date(), reason: .answeredElsewhere)
            break
        case 5:
            self.callKeepProvider?.reportCall(with: uuid, endedAt: Date(), reason: .declinedElsewhere)
            break
        default:
            break
        }
    }
    
    @objc
    func getAudioRoutes(_ resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        
        
        let inputs = EYRCallKeep.getAudioInputs()
        
        if let inputs = inputs {
            let formatedInputs = EYRCallKeep.formatAudioInputs(inputs: inputs)
            resolve(formatedInputs)
        } else {
            
            reject("Fail to get audio routes", nil, nil)
        }
    }
    
    // MARK: - Class func
    @objc(options:)
    class func setup(options: [String: Any]) {
        
        sharedInstance.setup(options: options)        
    }
    
    /// Return provider config
    /// - Parameter settings: List of available settings
    @objc(getProviderConfiguration:)
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
        
        config.supportedHandleTypes = [.generic]
        return config
    }
    
    // IMPORTANT
    class func reportIncomingCall(uuidString: String,
                                  handle: String,
                                  handleType: CXHandle.HandleType,
                                  hasVideo: Bool,
                                  localizedCallerName: String?,
                                  supportsHolding: Bool,
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
        cxCallUpdate.hasVideo = hasVideo;
        
        if let name = localizedCallerName {
            cxCallUpdate.localizedCallerName = localizedCallerName;
        }
        
        EYRCallKeep.createCallKitProvider()
        
        if let sharedProvider = sharedProvider {
            sharedProvider.reportNewIncomingCall(with: uuid,
                                                 update: cxCallUpdate,
                                                 completion: {err in
                
                                                    //sendEventWithNameWrapper
                
            })
        } else {
            
            
            print("[RNCallKeep][reportIncomingCall] Error: Shared provider is nil")
        }
        
    }
    
    class func formatAudioInputs(inputs: [AVAudioSessionPortDescription]) -> [[String: Any]] {
    
        let speakerDict = [
            "name": "Speaker",
            "type": AVAudioSession.Port.builtInSpeaker
        ] as [String : Any]
        var newInputs = [speakerDict]
        
        for input in inputs {
            
            print("PORT: \(input.portName). UID: \(input.uid)")
            
            let type = EYRCallKeep.getAudioInputType(type: input.portType.rawValue)
            
            if let type = type {
                
                let dict = [
                    "name": input.portName,
                    "type": type
                ] as [String : Any]
                newInputs.append(dict)
            }
        }
        
        return newInputs
    }
    
    /// Returns current audio inputs
    class func getAudioInputs() -> [AVAudioSessionPortDescription]? {
        
        let audioSession = AVAudioSession.sharedInstance()
        
        do {
        
            try audioSession.setCategory(.playAndRecord, options: [.allowBluetooth, .defaultToSpeaker])
        } catch {
            
            print("[RNCallKeep][getAudioInputs] Audio session setCategory error: ", error)
            NSException(name: NSExceptionName(rawValue: "RNCallKeep: Get audio inputs"), reason: error.localizedDescription, userInfo: nil).raise()
        }
        
        do {
            
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            
            print("[RNCallKeep][getAudioInputs] Audio session setActive error: ", error)
            NSException(name: NSExceptionName(rawValue: "RNCallKeep: Get audio inputs"), reason: error.localizedDescription, userInfo: nil).raise()
        }
        
        return audioSession.availableInputs
        
    }
    
    /// Returns audio type. Used to be ported to JS file
    class func getAudioInputType(type: String) -> String? {
        
        if type == AVAudioSession.Port.builtInMic.rawValue {
            
            return "Phone"
        }
        
        if type == AVAudioSession.Port.headphones.rawValue || type == AVAudioSession.Port.headsetMic.rawValue {
            
            return "Phone"
        }
        
        if type == AVAudioSession.Port.bluetoothHFP.rawValue
            || type == AVAudioSession.Port.bluetoothLE.rawValue
            || type == AVAudioSession.Port.bluetoothA2DP.rawValue {
            
            return "Bluetooth"
        }
        
        if type == AVAudioSession.Port.builtInSpeaker.rawValue {
            
            return "Speaker"
        }
        
        print("[RNCallKeep][getAudioInputType] Error: Can't identify audio input type")
        return nil
    }
    
    /// Returns true if the associated with uuidString is active
    class func isCallActive(uuidString: String) -> Bool {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("[RNCallKeep][isCallActive] Cant find uuid")
            return false
        }
        
        let callObserver = CXCallObserver()
        for call in callObserver.calls {
            print("[RNCallKeep][isCallActive] \(call.uuid) \(call.uuid == uuid)?")
            if call.uuid == uuid {
                
                return call.hasConnected
            }
        }
        
        return false
    }
    
    /// Retrieves all current calls
    class func getCalls() -> [[String : Any]] {
        
        let callObserver = CXCallObserver()
        var currentCalls = [[String : Any]]()
        for call in callObserver.calls {
            
            let uuidString = call.uuid.uuidString
            let requestedCall = [
                "callUUID": uuidString,
                "outgoing": call.isOutgoing,
                "onHold": call.isOnHold,
                "hasConnected": call.hasConnected,
                "hasEnded": call.hasEnded
            ] as [String : Any]
            
            currentCalls.append(requestedCall)
        }
        
        return currentCalls
    }
    
    // END OF CLASS FUNC
    
    /// Setup CXProvider and CXCallController
    /// - Parameter options: List of options
    @objc(options:)
    func setup(options: [String: Any]) {
        
        _version = ProcessInfo().operatingSystemVersion
        self.cxCallController = CXCallController()
        
        // Save default settings
        let standard = UserDefaults.standard
        standard.set(options, forKey: EYRCallKeep._settingsKey)
        
        EYRCallKeep.createCallKitProvider()
        
        self.callKeepProvider = EYRCallKeep.sharedProvider;
        self.callKeepProvider?.setDelegate(self, queue: nil)
    }
    
    // MARK: - Private func
    fileprivate func requestTransaction(_ transaction: CXTransaction) {
        
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
                    
                    let cxCallUpdate = CXCallUpdate()
                    cxCallUpdate.remoteHandle = startAction.handle
                    cxCallUpdate.hasVideo = startAction.isVideo
                    cxCallUpdate.localizedCallerName = startAction.contactIdentifier
                    cxCallUpdate.supportsDTMF = true;
                    cxCallUpdate.supportsHolding = true;
                    
                    // reportCallWithUUID
                    self.callKeepProvider?.reportCall(with: startAction.callUUID, updated: cxCallUpdate)
                }
            }
        })
    }
    
    fileprivate func configureAudioSession() {
        
        let audioSession = AVAudioSession.sharedInstance()
        
        // All the calls below are throwable, so enclose them in try catch block
        do {
        
            try audioSession.setCategory(.playAndRecord, mode: .voiceChat, options: [.allowBluetooth, .defaultToSpeaker])
        } catch {
            print("[RNCallKeep][requestTransaction] Audio session setCategory error: ", error)
        }
        
        do {
            
            let sampleRate = 44100.0
            try audioSession.setPreferredSampleRate(sampleRate)
        } catch {
            
            print("[RNCallKeep][requestTransaction] Audio session setPreferredSampleRate error: ", error)
        }
        
        do {
            
            let bufferDuration: TimeInterval = 0.005
            try audioSession.setPreferredIOBufferDuration(bufferDuration)
        } catch {
            
            print("[RNCallKeep][requestTransaction] Audio session setPreferredIOBufferDuration error: ", error)
        }
        
        do {
            
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            
            print("[RNCallKeep][requestTransaction] Audio session setActive error: ", error)
        }
    }
    
    fileprivate func sendEvent(name: String, body: Any) {
        
    }
}

// MARK: - CXProvider delegate
extension EYRCallKeep: CXProviderDelegate {
    
    func providerDidReset(_ provider: CXProvider) {
        
    }
    
    /// Answer incoming call
    func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        
        self.configureAudioSession()
        
        // Send event with name wrapper
    }
    
    /// End ongoing call
    func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        
        // Send event with name wrapper
    }
    
    /// Muted ongoing call
    func provider(_ provider: CXProvider, perform action: CXSetMutedCallAction) {
        
        // Send event with name wrapper
    }
    
    /// Held ongoing call
    func provider(_ provider: CXProvider, perform action: CXSetHeldCallAction) {
        
        // Send event with name wrapper
    }
    
    func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        
        let userInfo: [String: Any] = [
            AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.ended,
            AVAudioSessionInterruptionOptionKey: AVAudioSession.InterruptionOptions.shouldResume
        ]
        
        NotificationCenter.default.post(name: AVAudioSession.interruptionNotification,
                                        object: nil,
                                        userInfo: userInfo)
        self.configureAudioSession()
        
        // Send event with name wrapper
    }
    
    func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
        
        // Send event with name wrapper
    }
}

