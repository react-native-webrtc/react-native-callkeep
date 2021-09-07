//
//  EYRCallManager.swift
//  RNCallKeep
//
//  Created by N L on 2.9.2021.
//

/*
import Foundation

import UIKit
import CallKit

final class EYRCallManager: NSObject {
    
    enum Call: String {
        case end = "endCall"
    }

    let callController = CXCallController()


    func end(call: Call) {
        let endCallAction = CXEndCallAction(call: call.uuid)
        let transaction = CXTransaction()
        transaction.addAction(endCallAction)

        requestTransaction(transaction, action: Call.end.rawValue)
    }


    private func requestTransaction(_ transaction: CXTransaction, action: String = "") {
        callController.request(transaction) { error in
            if let error = error {
                print("Error requesting transaction: \(error)")
            } else {
                print("Requested transaction \(action) successfully")
            }
        }
    }

    // MARK: Call Management
    static let CallsChangedNotification = Notification.Name("CallManagerCallsChangedNotification")

    private(set) var calls = [Call]()

    func callWithUUID(uuid: UUID) -> Call? {
        guard let index = calls.index(where: { $0.uuid == uuid }) else {
            return nil
        }
        return calls[index]
    }

    func addCall(_ call: Call) {
        calls.append(call)

        call.stateDidChange = { [weak self] in
            self?.postCallsChangedNotification()
        }

        postCallsChangedNotification(userInfo: ["action": Call.start.rawValue])
    }

    func removeCall(_ call: SpeakerboxCall) {
        calls = calls.filter {$0 === call}
        postCallsChangedNotification(userInfo: ["action": Call.end.rawValue])
    }

    func removeAllCalls() {
        calls.removeAll()
        postCallsChangedNotification(userInfo: ["action": Call.end.rawValue])
    }

    private func postCallsChangedNotification(userInfo: [String: Any]? = nil) {
        NotificationCenter.default.post(name: type(of: self).CallsChangedNotification, object: self, userInfo: userInfo)
    }
}
*/
