import Foundation
import CallKit
import AVKit

let EYRCallKeepDidLoadWithEvents = "EYRCallKeepDidLoadWithEvents"
let EYRCallKeepPerformEndCallAction = "EYRCallKeepPerformEndCallAction"
let EYRCallKeepAnswerCallAction = "EYRCallKeepPerformAnswerCallAction"
let EYRCallKeepDidPerformSetMutedCallAction = "EYRCallKeepDidPerformSetMutedCallAction"

@objc(EYRCallKeep)
public class EYRCallKeep: RCTEventEmitter {
    
    @objc static var options: [String: Any]?
    
    var cxCallController: CXCallController = CXCallController()
    var callKeepProvider: CXProvider?
    
    // MARK: - Private params
    private var _hasListener = false
    private var _delayedEvents = [[String: Any]]()
    
    fileprivate var _answerCallAction: CXAnswerCallAction?
    fileprivate var _endCallAction: CXEndCallAction?
    
    private let _settingsKey = "EYRCallKeepSettings"
    
    // MARK: - Init
    public override init() {
        super.init()
        
        setup()
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(reportIncomingCall(noti:)),
                                               name: Notification.Name("EYRCallKeep.reportIncomingCall"),
                                               object: nil)
    }
    
    @objc public class func setOptions(options: [String: Any]) {
        
        EYRCallKeep.options = options
    }
    
    // MARK: - Override methods
    public override func startObserving() {
        _hasListener = true
        if _delayedEvents.count > 0 {
            self.sendEvent(withName: EYRCallKeepDidLoadWithEvents, body: _delayedEvents)
        }
    }

    public override func stopObserving() {
        _hasListener = false
    }
    
    public override func supportedEvents() -> [String]! {
        return [
            EYRCallKeepDidLoadWithEvents,
            EYRCallKeepAnswerCallAction,
            EYRCallKeepPerformEndCallAction,
            EYRCallKeepDidPerformSetMutedCallAction
        ]
    }
    
    func sendEventWithNameWrapper(_ name: String, body: Any) {
        
        if _hasListener {
            
            self.sendEvent(withName: name, body: body)
        } else {
            
            let dict: [String: Any] = ["name": name, "body": body]
            _delayedEvents.append(dict)
        }
    }
    
    // MARK: - Exported methods
    /// Activating a mute call action
    /// - Parameter uuidString: Device's uuid
    /// - Parameter muted: Mute or unmute the recipient
    @objc(setMutedCall:muted:)
    func setMutedCall(_ uuidString: String, muted: Bool) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("[EYRCallKeep][setMutedCall] Cant find uuid")
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
            print("[EYRCallKeep][endCall] Cant find uuid")
            return
        }
        let endCallAction = CXEndCallAction(call: uuid)
        let transaction = CXTransaction()
        transaction.addAction(endCallAction)
        
        // Request transaction
        self.requestTransaction(transaction)
    }
    
    @objc(answerIncomingCall:)
    func answerIncomingCall(_ uuidString: String) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("[EYRCallKeep][answerIncomingCall] Cant find uuid")
            return
        }
        
        let answerCallAction = CXAnswerCallAction(call: uuid)
        let transaction = CXTransaction(action: answerCallAction)
        self.requestTransaction(transaction)
    }
    
    @objc public func fulfillAnswerCallAction() {
        if let action = _answerCallAction {
            
            action.fulfill()
            _answerCallAction = nil
        }
    }
    
    @objc public func fulfillEndCallAction() {
        if let action = _endCallAction {
            
            action.fulfill()
            _endCallAction = nil
        }
    }
    
    @objc(getInitialEvents:reject:)
    public func getInitialEvents(_ resolve: RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        resolve(_delayedEvents)
    }
    
    @objc(reportEndCall:reason:)
    public func reportEndCall(_ uuidString: String, reason: Int) {
        
        guard let uuid = UUID(uuidString: uuidString) else {
            print("[EYRCallKeep][endCall] Cant find uuid")
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
    
    fileprivate func setup() {
        
        self.cxCallController = CXCallController()
        
        if let options = EYRCallKeep.options {
        
            // Save default settings
            let standard = UserDefaults.standard
            standard.set(options, forKey: _settingsKey)
            
            self.createCallKitProvider()
        } else {
            
            print("[EYRCallKeep][reportIncomingCall] Error: Options not set")
        }
    }
    
    @objc public func reportIncomingCall(noti: Notification) {
        
        let userInfo = noti.userInfo!
        let uuidString = userInfo["uuidString"] as! String
        guard let uuid = UUID(uuidString: uuidString) else {
            
            print("[EYRCallKeep][reportIncomingCall] Error: Cant create uuid from string")
            return
        }
        
        let supportsHolding = userInfo["supportsHolding"] as? Bool ?? true
        let hasVideo = userInfo["hasVideo"] as? Bool ?? true
        let handle = userInfo["handle"] as? String ?? ""
        let name = userInfo["localizedCallerName"] as? String ?? ""
        
        let cxCallUpdate = CXCallUpdate()
        cxCallUpdate.remoteHandle = CXHandle(type: .generic, value: handle)
        cxCallUpdate.supportsHolding = supportsHolding
        cxCallUpdate.hasVideo = hasVideo
        cxCallUpdate.localizedCallerName = name;
        
        self.createCallKitProvider()
        
        if let provider = callKeepProvider {
            
            //OTAudioDeviceManager.setAudioDevice(OTDefaultAudioDevice.sharedInstance())
            provider.reportNewIncomingCall(with: uuid,
                                                 update: cxCallUpdate,
                                                 completion: {err in
            })
        } else {
            
            print("[EYRCallKeep][reportIncomingCall] Error: Shared provider is nil")
        }
    }
    
    /// Return provider config
    /// - Parameter settings: List of available settings
    @objc(getProviderConfiguration:)
    class func getProviderConfiguration(settings: [String: Any]) -> CXProviderConfiguration? {
    
        guard let appName = settings["appName"] as? String else {
            print("[EYRCallKeep][getProviderConfiguration] Missing key: appName")
            return nil
        }
        
        let config = CXProviderConfiguration(localizedName: appName)
        
        if let supportsVideo = settings["supportsVideo"] as? Bool {
            config.supportsVideo = supportsVideo
        }
        
        if let ringtoneSound = settings["ringtoneSound"] as? String {
            config.ringtoneSound = ringtoneSound
        }
        
        config.maximumCallGroups = 1
        config.maximumCallsPerCallGroup = 1
        config.supportedHandleTypes = [.generic]
        
        return config
    }
    
    // MARK: - Private func

    fileprivate func requestTransaction(_ transaction: CXTransaction) {
        
        self.cxCallController.request(transaction, completion: { err in
            
            if let err = err {
                print("[EYRCallKeep][requestTransaction] Error request transaction \(transaction.actions): \(err)")
                
                return
            }
            
            print("[EYRCallKeep][requestTransaction] Requested transaction successfully")
        })
    }
    
    fileprivate func configureAudioSession() {
        
        let audioSession = AVAudioSession.sharedInstance()
        
        // All the calls below are throwable, so enclose them in try catch block
        
        do {
            
            try audioSession.setCategory(.playAndRecord,
                                         mode: .voiceChat,
                                         options: [.allowBluetooth, .defaultToSpeaker])
            
            let sampleRate = 44100.0
            try audioSession.setPreferredSampleRate(sampleRate)
            
            let bufferDuration: TimeInterval = 0.005
            try audioSession.setPreferredIOBufferDuration(bufferDuration)
            
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch let error as NSError {
            print("Audio Session error: \(error.localizedDescription)")
        }
    }
    
    private func createCallKitProvider() {
        
        let settings = UserDefaults.standard.object(forKey: _settingsKey) as! [String: Any]
        
        guard let config = EYRCallKeep.getProviderConfiguration(settings: settings) else {
            
            print("[EYRCallKeep][createCallKitProvider] Fails to retrieve config")
            return
        }
        
        callKeepProvider = CXProvider(configuration: config)
        callKeepProvider?.setDelegate(self, queue: nil)
    }
}

// MARK: - CXProvider delegate
extension EYRCallKeep: CXProviderDelegate {
    
    public func providerDidReset(_ provider: CXProvider) {
        
    }
    
    /// Answer incoming call
    public func provider(_ provider: CXProvider, perform action: CXAnswerCallAction) {
        
        self.configureAudioSession()
        self.sendEventWithNameWrapper(EYRCallKeepAnswerCallAction,
                                      body: ["callUUID": action.callUUID.uuidString.lowercased()])
        _answerCallAction = action
    }
    
    /// End ongoing call
    public func provider(_ provider: CXProvider, perform action: CXEndCallAction) {
        
        self.sendEventWithNameWrapper(EYRCallKeepPerformEndCallAction,
                                      body: ["callUUID": action.callUUID.uuidString.lowercased()])
        _endCallAction = action
    }
    
    /// Muted ongoing call
    public func provider(_ provider: CXProvider, perform action: CXSetMutedCallAction) {
        
        // Send event with name wrapper
        self.sendEventWithNameWrapper(EYRCallKeepDidPerformSetMutedCallAction,
                                      body: ["callUUID": action.callUUID.uuidString.lowercased()])
        action.fulfill()
    }
    
    public func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        
        let dict = [
            AVAudioSessionInterruptionTypeKey: AVAudioSession.InterruptionType.ended,
            AVAudioSessionInterruptionOptionKey: AVAudioSession.InterruptionOptions.shouldResume
        ] as [String : Any]
        NotificationCenter.default.post(name: AVAudioSession.interruptionNotification,
                                        object: nil,
                                        userInfo: dict)
        
        self.configureAudioSession()
        
    }
}

