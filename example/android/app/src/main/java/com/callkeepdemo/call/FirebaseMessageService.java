package com.callkeepdemo.call;

import com.google.firebase.messaging.RemoteMessage;

import io.invertase.firebase.messaging.ReactNativeFirebaseMessagingService;

public class FirebaseMessageService extends ReactNativeFirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
    }
}
