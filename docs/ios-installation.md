# iOS installation

## Install

```sh
npm install react-native-callkeep
cd ios && pod install
```

Autolinking adds the `RNCallKeep` pod: there is nothing to link by hand in Xcode. The pod ships a privacy manifest, and it builds as a module, so Swift code can `import RNCallKeep`.

## Background modes and microphone

In Xcode, under *Signing & Capabilities*, add **Background Modes** and check **Voice over IP** and **Audio, AirPlay, and Picture in Picture**. In `Info.plist`, together with the text iOS shows when it asks for the microphone, this is:

```xml
<key>UIBackgroundModes</key>
<array>
  <string>audio</string>
  <string>voip</string>
</array>
<key>NSMicrophoneUsageDescription</key>
<string>Your app uses the microphone for calls.</string>
```

`voip` is what lets a VoIP push wake your app (see [below](#incoming-calls-when-the-app-isnt-running)).

## Calls started from Recents, Contacts or Siri

When the user calls a contact back from the Phone app's Recents, iOS opens your app with that call. Forward it to CallKeep, which sends [`didReceiveStartCallAction`](events.md#didreceivestartcallaction):

- With the scene life cycle, which apps built with the iOS 27 SDK must use: in your scene delegate, see [iOS 27: scene life cycle](guides/ios-scenes.md).
- Without scenes: in your app delegate.

  ```swift
  import RNCallKeep

  // In AppDelegate:
  func application(
    _ application: UIApplication,
    continue userActivity: NSUserActivity,
    restorationHandler: @escaping ([UIUserActivityRestoring]?) -> Void
  ) -> Bool {
    return RNCallKeep.continueUserActivity(userActivity)
  }
  ```

Your calls appear in Recents when `includesCallsInRecents` is `true` in the [setup options](setup.md#options), which is the default.

## Incoming calls when the app isn't running

iOS wakes a terminated app for an incoming call only through a VoIP push (PushKit), and the app must report the call to CallKit as soon as the push arrives. Add the **Push Notifications** capability, then follow [Incoming calls when the app isn't running](guides/background-calls.md).

## Set up CallKeep

Call `RNCallKeep.setup()` early in your JavaScript: see [Setup and options](setup.md).

Test calls on a device: the iOS Simulator has no CallKit service, so call actions fail there. The [example app](../example/) has this whole setup, tested on iOS 27.
