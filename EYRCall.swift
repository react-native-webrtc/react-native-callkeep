//
//  EYRCall.swift
//  RNCallKeep
//
//  Created by N L on 2.9.2021.
//

/*
import Foundation
import OpenTok

final class Call: NSObject {

    // MARK: Metadata Properties
    let uuid: UUID
    let isOutgoing: Bool
    var handle: String?

    // MARK: Call State Properties
    var isMuted = false {
        didSet {
            publisher?.publishAudio = !isMuted
        }
    }

    // MARK: State change callback blocks
    var stateDidChange: (() -> Void)?
    var hasStartedConnectingDidChange: (() -> Void)?
    var hasConnectedDidChange: (() -> Void)?
    var hasEndedDidChange: (() -> Void)?
    var audioChange: (() -> Void)?

    // MARK: Derived Properties
    var hasStartedConnecting: Bool {
        get {
            return connectingDate != nil
        }
        set {
            connectingDate = newValue ? Date() : nil
        }
    }
    var hasConnected: Bool {
        get {
            return connectDate != nil
        }
        set {
            connectDate = newValue ? Date() : nil
        }
    }
    var hasEnded: Bool {
        get {
            return endDate != nil
        }
        set {
            endDate = newValue ? Date() : nil
        }
    }
    var duration: TimeInterval {
        guard let connectDate = connectDate else {
            return 0
        }

        return Date().timeIntervalSince(connectDate)
    }

    // MARK: Initialization
    init(uuid: UUID, isOutgoing: Bool = false) {
        self.uuid = uuid
        self.isOutgoing = isOutgoing
    }

    // MARK: Actions
    var session: OTSession?
    var publisher: OTPublisher?
    var subscriber: OTSubscriber?
    
    var canAnswerCall: ((Bool) -> Void)?
    func answerCall(withAudioSession audioSession: AVAudioSession, completion: ((_ success: Bool) -> Void)?) {
        OTAudioDeviceManager.setAudioDevice(OTDefaultAudioDevice.sharedInstance(with: audioSession))
        if session == nil {
            session = OTSession(apiKey: apiKey, sessionId: sessionId, delegate: self)
        }
        
        canAnswerCall = completion
        
        var error: OTError?
        hasStartedConnecting = true
        session?.connect(withToken: token, error: &error)
        if error != nil {
            print(error!)
        }
    }
    
    func startAudio() {
        if publisher == nil {
            let settings = OTPublisherSettings()
            settings.name = UIDevice.current.name
            settings.audioTrack = true
            settings.videoTrack = false
            publisher = OTPublisher.init(delegate: self, settings: settings)
        }
        
        var error: OTError?
        session?.publish(publisher!, error: &error)
        if error != nil {
            print(error!)
        }
    }
    
    func endCall() {
        /*
         Simulate the end taking effect immediately, since
         the example app is not backed by a real network service
         */
        if let publisher = publisher {
            var error: OTError?
            session?.unpublish(publisher, error: &error)
            if error != nil {
                print(error!)
            }
        }
        publisher = nil
        
        if let session = session {
            var error: OTError?
            session.disconnect(&error)
            if error != nil {
                print(error!)
            }
        }
        session = nil
        
        hasEnded = true
    }
}

extension Call: OTSessionDelegate {
    func sessionDidConnect(_ session: OTSession) {
        print(#function)
        
        hasConnected = true
        canStartCall?(true)
        canAnswerCall?(true)
    }
    
    func sessionDidDisconnect(_ session: OTSession) {
        print(#function)
    }
    
    func sessionDidBeginReconnecting(_ session: OTSession) {
        print(#function)
    }
    
    func sessionDidReconnect(_ session: OTSession) {
        print(#function)
    }
    
    func session(_ session: OTSession, didFailWithError error: OTError) {
        print(#function, error)
        
        hasConnected = false
        canStartCall?(false)
        canAnswerCall?(false)
    }
    
    func session(_ session: OTSession, streamCreated stream: OTStream) {
        print(#function)
        subscriber = OTSubscriber.init(stream: stream, delegate: self)
        subscriber?.subscribeToVideo = false
        if let subscriber = subscriber {
            var error: OTError?
            session.subscribe(subscriber, error: &error)
            if error != nil {
                print(error!)
            }
        }
    }
    
    
    func session(_ session: OTSession, streamDestroyed stream: OTStream) {
        print(#function)
    }
}

extension SpeakerboxCall: OTPublisherDelegate {
    func publisher(_ publisher: OTPublisherKit, didFailWithError error: OTError) {
        print(#function)
    }
}

extension Call: OTSubscriberDelegate {
    func subscriberDidConnect(toStream subscriber: OTSubscriberKit) {
        print(#function)
    }
    
    func subscriber(_ subscriber: OTSubscriberKit, didFailWithError error: OTError) {
        print(#function)
    }
}
*/
