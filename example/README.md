# react-native-callkeep example

A React Native 0.87 app that drives react-native-callkeep with simulated calls: CallKit on iOS, Telecom (ConnectionService) on Android. It runs the library from this repository, so it is also the quickest way to try a change to the library.

There is no VoIP server, so no audio flows. Everything else is real: the system call screens, the lock screen, the Phone app's Recents, hold, mute and DTMF from the call screen, and every event the library sends.

## Run it

You need what any React Native 0.87 app needs: Node.js 22.11 or later, Xcode 27 for iOS, and Android Studio with an emulator or a device for Android.

```bash
cd example
npm install

# Android: an emulator or a device
npm run android

# iOS: a real iPhone. The Simulator has no CallKit service, so calls fail there
# (the app still starts, which is enough to check the scene setup).
cd ios && pod install && cd ..
npm run ios -- --device
```

For a device, open `ios/CallKeepExample.xcworkspace` in Xcode once and pick your team under *Signing & Capabilities*.

Changes to the library's JavaScript (`../index.js`, `../actions.js`) reload with Metro. Changes to its native code (`../ios`, `../android`) need a rebuild of the app.

## What it shows

| In the app | What happens | Library |
|---|---|---|
| **Incoming call** | The system incoming-call screen (iOS, Android managed mode), or the app's own Answer / Decline buttons (Android self-managed mode). Lock the screen while it rings to see the lock-screen call UI. | `displayIncomingCall`; `showIncomingCallUi` (self-managed) |
| **Outgoing call** | The call starts, rings for two seconds, then "the remote side answers". | `startCall` → `didReceiveStartCallAction`; then `reportConnectingOutgoingCallWithUUID` / `reportConnectedOutgoingCallWithUUID` (iOS), `setCurrentCallActive` (Android) |
| **Answer**, **Decline** | Same as answering or declining on the system screen. | `answerIncomingCall` → `answerCall`; `rejectCall` → `endCall` |
| **Hold**, **Mute**, **DTMF 1** | The call row follows the events, so it also follows hold and mute done from the system call screen (iOS, Android managed mode). | `setOnHold`, `setMutedCall`, `sendDTMF` → `didToggleHoldCallAction`, `didPerformSetMutedCallAction`, `didPerformDTMFAction` |
| **Hang up** | The app ends the call. | `endCall` → `endCall` |
| **Remote hangs up** | The other side ended the call: the app reports it, and no event follows. | `reportEndCallWithUUID(uuid, CONSTANTS.END_CALL_REASONS.REMOTE_ENDED)` |
| **Audio routes** | Lists the speaker, receiver, Bluetooth and headset, and moves the active call to one. | `getAudioRoutes`, `setAudioRoute` |
| **Events** | Every event the library sends, with its payload. | all of them |
| **Android mode** | Switches between self-managed and managed mode and runs `setup` again. | `setup({ android: { selfManaged } })` |
| Call a contact back from **Recents** (iOS) | iOS opens the app and passes the call to the scene delegate; the app starts it. | `RNCallKeep.continueUserActivity` → `didReceiveStartCallAction` without `callUUID` → `startCall` |

## Where to look

| File | What it shows |
|---|---|
| [`src/callkeep.ts`](src/callkeep.ts) | The `setup()` options, Android runtime permissions, call UUIDs |
| [`src/useCallKeep.ts`](src/useCallKeep.ts) | The event listeners and what an app does on each one; events received before JavaScript was listening (`didLoadWithEvents`) |
| [`App.tsx`](App.tsx) | The screen |
| [`ios/CallKeepExample/SceneDelegate.swift`](ios/CallKeepExample/SceneDelegate.swift) | The scene life cycle that iOS 27 requires, and calls from Recents, Contacts or Siri |
| [`ios/CallKeepExample/AppDelegate.swift`](ios/CallKeepExample/AppDelegate.swift) | Where an app handles VoIP pushes |
| [`ios/CallKeepExample/Info.plist`](ios/CallKeepExample/Info.plist) | The `audio` and `voip` background modes, the microphone usage text, the scene manifest |
| [`android/app/src/main/AndroidManifest.xml`](android/app/src/main/AndroidManifest.xml) | The permissions, and `VoiceConnectionService` with its foreground service types |
| [`metro.config.js`](metro.config.js), [`react-native.config.js`](react-native.config.js) | How the example uses the library from this repository instead of npm |

In your own app you install the library from npm instead (`npm install react-native-callkeep`), and leave out `metro.config.js`'s `watchFolders`/`resolver` and `react-native.config.js`.

## What it leaves out

- **Calls that arrive while the app is in the background or not running.** JavaScript doesn't run then (a `setTimeout` waits until the app comes back), so apps report those calls natively when a push arrives: a VoIP push on iOS, with `RNCallKeep.reportNewIncomingCall` in the app delegate (see the main README's PushKit section), and a high-priority FCM message on Android. That needs a push server and certificates, which the example doesn't have.
- **The incoming-call notification in self-managed mode.** In the background, a self-managed app has to show its own full-screen notification when `showIncomingCallUi` arrives (for example with a notification library). The example only shows the call inside the app.
- **Media.** Connect your WebRTC or SIP stack where the example marks calls active (`answerCall`, an outgoing call that connects), and on iOS start the audio on `didActivateAudioSession`.

Tested with Android 10 (emulator) and Android 16 (Samsung Galaxy S24) in self-managed mode: incoming and outgoing calls, hold, mute, DTMF, hang-up from either side, and audio routes (the emulator has no receiver, so its route stays on the speaker). On an iPhone 15 Pro Max with iOS 27.0.1: incoming calls (answered from the lock screen too), hold, mute and DTMF from the CallKit screen, outgoing calls, audio routes, and calling back from Recents, with the app in the background and with the app not running.
