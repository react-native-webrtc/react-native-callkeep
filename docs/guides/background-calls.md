# Incoming calls when the app isn't running

In some case your application can be unreachable :
- when the user kill the application
- when it's in background since a long time (eg: after ~5mn the os will kill all connections).

To be able to wake up your application to display the incoming call, you can use [react-native-voip-push-notification](https://github.com/react-native-webrtc/react-native-voip-push-notification) on iOS, or [background messages from react-native-firebase](https://rnfirebase.io/messaging/usage#receiving-messages) on Android.

You have to send a push to your application, like with Firebase for Android and with a library supporting PushKit pushes for iOS.

On iOS the app is suspended in the background, so the call is reported from native code when the VoIP push arrives (PushKit, below). On Android, a high-priority FCM data message wakes the app, and react-native-firebase's background message handler, which runs your JavaScript, can call `RNCallKeep.displayIncomingCall`.

## PushKit (iOS)

Since iOS 13, you'll have to report the incoming calls that wakes up your application with a VoIP push. Add this in your `AppDelegate.m` if you're using VoIP pushes to wake up your application :

```objective-c
- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(PKPushType)type withCompletionHandler:(void (^)(void))completion {
  // Process the received push
  [RNVoipPushNotificationManager didReceiveIncomingPushWithPayload:payload forType:(NSString *)type];

  // Retrieve information like handle and callerName here
  // NSString *uuid = /* fetch for payload or ... */ [[[NSUUID UUID] UUIDString] lowercaseString];
  // NSString *callerName = @"caller name here";
  // NSString *handle = @"caller number here";
  // NSDictionary *extra = [payload.dictionaryPayload valueForKeyPath:@"custom.path.to.data"]; /* use this to pass any special data (ie. from your notification) down to RN. Can also be `nil` */

  [RNCallKeep reportNewIncomingCall: uuid
                             handle: handle
                         handleType: @"generic"
                           hasVideo: NO
                localizedCallerName: callerName
                    supportsHolding: YES
                       supportsDTMF: YES
                   supportsGrouping: YES
                 supportsUngrouping: YES
                        fromPushKit: YES
                            payload: extra
              withCompletionHandler: completion];
}
```

With the scene life cycle that iOS 27 requires, VoIP pushes are still handled in the app delegate; see the [scene guide](ios-scenes.md).
