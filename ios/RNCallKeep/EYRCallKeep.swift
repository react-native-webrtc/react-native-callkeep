import Foundation
import CallKit
import AVKit

@objc(EYRCallKeep)
public class EYRCallKeep: RCTEventEmitter {
    
    var cxCallController: CXCallController?
    var callKeepProvider: CXProvider?
    
    // MARK: - Private params
    private var _hasListener = false
    private var _delayedEvents = [[String: Any]]()
    
    fileprivate var _answerCallAction: CXAnswerCallAction?
    fileprivate var _endCallAction: CXEndCallAction?
    
    private let _settingsKey = "RNCallKeepSettings"
    
    let EYRCallKeepDidLoadWithEvents = "RNCallKeepDidLoadWithEvents"
    let EYRCallKeepDidReceiveStartCallAction = "RNCallKeepDidReceiveStartCallAction"
    let EYRCallKeepDidDisplayIncomingCall = "RNCallKeepDidDisplayIncomingCall"
    let EYRCallKeepPerformEndCallAction = "RNCallKeepPerformEndCallAction"
    let EYRCallKeepSetMutedCallAction = "RNCallKeepSetMutedCallAction"
    let EYRCallKeepAnswerCallAction = "RNCallKeepPerformAnswerCallAction"
    
    
    // MARK: - Singleton init
    
    public override init() {
        super.init()
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(setup(noti:)),
                                               name: Notification.Name("EYRCallKeep.setup"),
                                               object: nil)
        
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(reportIncomingCall(noti:)),
                                               name: Notification.Name("EYRCallKeep.reportIncomingCall"),
                                               object: nil)
    }
    
    func createCallKitProvider() {
        
        let settings = UserDefaults.standard.object(forKey: _settingsKey) as! [String: Any]
        
        guard let config = EYRCallKeep.getProviderConfiguration(settings: settings) else {
            
            print("[RNCallKeep][createCallKitProvider] Fails to retrieve config")
            return
        }
        
        callKeepProvider = CXProvider(configuration: config)
        callKeepProvider?.setDelegate(self, queue: nil)
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
    
    func sendEventWithNameWrapper(_ name: String, body: Any) {
        
        if _hasListener {
            
            self.sendEvent(withName: name, body: body)
        } else {
            
            let dict: [String: Any] = ["name": name, "body": body]
            _delayedEvents.append(dict)
        }
    }
    
    public override func supportedEvents() -> [String]! {
        return [
            EYRCallKeepDidDisplayIncomingCall,
            EYRCallKeepDidLoadWithEvents,
            EYRCallKeepDidReceiveStartCallAction,
            EYRCallKeepAnswerCallAction,
            EYRCallKeepPerformEndCallAction
        ]
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
            print("[RNCallKeep][endCall] Cant find uuid")
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
            print("[RNCallKeep][answerIncomingCall] Cant find uuid")
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
    @objc func setup(noti: Notification) {
        
        let options = noti.userInfo!
        self.cxCallController = CXCallController()
        
        // Save default settings
        let standard = UserDefaults.standard
        standard.set(options, forKey: _settingsKey)
        
        self.createCallKitProvider()
    }
    
    @objc
    public func reportIncomingCall(noti: Notification) {
        
        let userInfo = noti.userInfo!
        let uuidString = userInfo["uuidString"] as! String
        guard let uuid = UUID(uuidString: uuidString) else {
            
            print("[RNCallKeep][reportIncomingCall] Error: Cant create uuid from string")
            return
        }
        
        let supportsHolding = userInfo["supportsHolding"] as? Bool ?? true
        let hasVideo = userInfo["hasVideo"] as? Bool ?? true
        let handleType = userInfo["handleType"] as? String ?? "generic"
        let handle = userInfo["handle"] as? String ?? ""
        let name = userInfo["localizedCallerName"] as? String ?? ""
        
        let cxCallUpdate = CXCallUpdate()
        let _handleType = EYRCallKeep.getHandleType(handleType: handleType)
        cxCallUpdate.remoteHandle = CXHandle(type: _handleType, value: handle)
        cxCallUpdate.supportsHolding = supportsHolding
        cxCallUpdate.hasVideo = hasVideo
        cxCallUpdate.localizedCallerName = name;
        
        self.createCallKitProvider()
        
        if let provider = callKeepProvider {
            
            provider.reportNewIncomingCall(with: uuid,
                                                 update: cxCallUpdate,
                                                 completion: {err in
                
                let dict = [
                        "error": err?.localizedDescription ?? "",
                        "callUUID": uuidString,
                        "handle":handle,
                        "localizedCallerName": name,
                        "hasVideo": hasVideo,
                        "supportHolding": supportsHolding,
                        "fromPushKit": false,
                        "payload": [:]
                        
                    ] as [String : Any]
                
                self.sendEventWithNameWrapper(self.EYRCallKeepDidDisplayIncomingCall,
                                              body: dict)
                    if err == nil {
                        
                        self.configureAudioSession()
                    }
            })
        } else {
            
            print("[RNCallKeep][reportIncomingCall] Error: Shared provider is nil")
             
        }
    }
    
    @objc
    public func application(_ application: UIApplication,
                                  continue userActivity: NSUserActivity,
                                  restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void) -> Bool {
        
        guard let interaction =  userActivity.interaction else {
            
            print("[EYRCallKeep][applicationError] Cant get interaction")
            return false
        }
        
        var isAudioCall = false
        var isVideoCall = false
        
        if #available(iOS 13, *) {
            
            let intent = interaction.intent as? INStartCallIntent
            if let intent = intent {
                
                if intent.responds(to: #selector(getter: INCallRecord.callCapability)) {
                    isAudioCall = intent.callCapability == INCallCapability.audioCall;
                    isVideoCall = intent.callCapability == INCallCapability.videoCall;
                } else {
                    isAudioCall = userActivity.activityType == INStartAudioCallIntentIdentifier
                    isVideoCall = userActivity.activityType == INStartVideoCallIntentIdentifier
                }
            }
        } else {
            
            isAudioCall = userActivity.activityType == INStartAudioCallIntentIdentifier
            isVideoCall = userActivity.activityType == INStartVideoCallIntentIdentifier
        }
        
        var contact: INPerson?
        var handle: String?

        if isAudioCall {
            let startAudioCallIntent = interaction.intent as? INStartAudioCallIntent
            contact = (startAudioCallIntent?.contacts?.first!)!
        } else if isVideoCall {
            let startVideoCallIntent = interaction.intent as? INStartVideoCallIntent
            contact = (startVideoCallIntent?.contacts?.first!)!
        }

        if let contact = contact {
            handle = contact.personHandle?.value
        }

        if let handle = handle {
            if handle.count > 0 {
                let userInfo = [
                    "handle": handle,
                    "video": NSNumber(value: isVideoCall)
                ] as [String : Any]

                self.sendEvent(withName: EYRCallKeepDidReceiveStartCallAction, body: userInfo)
                return true
            }
        }
        
        return false
    }
    
    @objc
    public class func application(_ app: UIApplication,
                                  open url: URL,
                                  options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        
        
        
        return true
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
        
        config.maximumCallGroups = 1
        config.maximumCallsPerCallGroup = 1
        config.supportedHandleTypes = [.generic]
        
        return config
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
    
    /// Convert Handle type string to Handle type nume
    class func getHandleType(handleType: String) -> CXHandle.HandleType {
        
        if handleType == "generic" { return CXHandle.HandleType.generic}
        if handleType == "number" { return CXHandle.HandleType.phoneNumber}
        if handleType == "email" { return CXHandle.HandleType.emailAddress}
        
        return CXHandle.HandleType.generic
    }
    
    // END OF CLASS FUNC
    
    /// Setup CXProvider and CXCallController
    /// - Parameter options: List of options
 
    
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
        
            try audioSession.setCategory(.playAndRecord,
                                         mode: .videoChat,
                                         options: [.allowBluetooth, .defaultToSpeaker, .allowBluetoothA2DP])
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
    }
    
    /// Held ongoing call
    public func provider(_ provider: CXProvider, perform action: CXSetHeldCallAction) {
        
        // Send event with name wrapper
    }
    
    public func provider(_ provider: CXProvider, didActivate audioSession: AVAudioSession) {
        
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
    
    public func provider(_ provider: CXProvider, didDeactivate audioSession: AVAudioSession) {
        
        // Send event with name wrapper
    }
}

