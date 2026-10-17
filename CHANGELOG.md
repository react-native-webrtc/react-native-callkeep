# Changelog

All notable changes to react-native-callkeep are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Dates are npm publish dates (UTC). react-native-callkeep was forked from
react-native-callkit 1.3.4; the react-native-callkit releases (1.0.0 to 1.3.4)
are not listed here.

## [Unreleased]

### Changed

- **Docs:** The Expo guide explains the config plugin's setup and what to add to it: the `audio` background mode and microphone text on iOS; on Android, the foreground service permissions, without which Android 14 and later refuse CallKeep's foreground service, and the `phoneCall|microphone` service types; `RECORD_AUDIO` with Expo SDK 54 and earlier. ([#849](https://github.com/react-native-webrtc/react-native-callkeep/pull/849), @sultanmyrza)
- **Docs:** The API reference now lists `setSettings`, `setReachable`, `reportConnectingOutgoingCallWithUUID`, `reportConnectedOutgoingCallWithUUID` (iOS) and `unregisterAndroidEvents` (Android), which were never documented. CI checks that every method matches across `index.js`, `index.d.ts` and the API reference's table, and that the relative links in the docs resolve.
- **Docs:** The iOS and Android installation guides are rewritten for autolinking: they still described `react-native link` and manual Xcode and Gradle steps from before React Native 0.60. They now follow the example app's setup, tested on devices: background modes, Recents and Siri (with or without scenes) and VoIP push on iOS; the manifest, foreground service types, runtime permissions, managed and self-managed modes on Android.
- **Docs:** The README is now short (what the library does, compatibility, installation, a quick start), and the documentation moved to `docs/`: setup and options, the API reference, events, guides (calls when the app isn't running, Android self-managed mode, the iOS 27 scene life cycle, Expo), troubleshooting and the migration guides. Fixed on the way: the method table's return types (most call methods return nothing, not a promise), missing methods (`sendDTMF`, `getAudioRoutes`, `setAudioRoute`), the `CONSTANTS` import example, the `foregroundService` and `notificationIcon` options, the claim that self-managed mode needs `READ_CALL_LOG` (the library doesn't use it), and the claim that nothing works on emulators (Android emulators do).

### Fixed

- **iOS:** `setup()` applies its options to CallKit right away. When the app had run before, CallKit kept the options of the previous launch (the module builds the CallKit provider from the stored settings when it loads), and calling `setup()` again didn't change them either. An app that sets up from native code can now call `[RNCallKeep setup:]` again to change them; `setup()` from JavaScript is still ignored then, as documented. ([#732](https://github.com/react-native-webrtc/react-native-callkeep/pull/732), @c960657)
- **Android:** `checkIsInManagedCall()`, `setAudioRoute()` and `toggleAudioRouteSpeaker()` no longer crash the app on Android 7 with `NoSuchMethodError`: they called Android 8 APIs. Before Android 8, `checkIsInManagedCall()` reports any ongoing call (there are no self-managed calls then), `setAudioRoute()` rejects and `toggleAudioRouteSpeaker()` does nothing.
- **Android:** `setAudioRoute()` rejects when no call has the given UUID. Its promise never settled.
- **Android:** Android lint passes on the library ([#646](https://github.com/react-native-webrtc/react-native-callkeep/issues/646)): `startCall` and `checkDefaultPhoneAccount` handle a `SecurityException` from Telecom (a permission can be revoked at any time), `endCall` uses `AudioManager.MODE_NORMAL`, and the library's `build.gradle` no longer declares the Android Gradle Plugin 3.0.1: the app's plugin builds it. ([#684](https://github.com/react-native-webrtc/react-native-callkeep/pull/684), @ramijarrar)
- **Android:** `backToForeground()` on a locked device asks the user to unlock (Android 8 and later), then shows the app. The app used to come to the front behind the lock screen: the window flags meant to show it over the lock screen were added to the intent, where the same values mean other intent flags (`FLAG_ACTIVITY_NEW_DOCUMENT`, `FLAG_ACTIVITY_RESET_TASK_IF_NEEDED`, `FLAG_ACTIVITY_BROUGHT_TO_FRONT`). ([#684](https://github.com/react-native-webrtc/react-native-callkeep/pull/684), @ramijarrar)
- **Android:** `didDisplayIncomingCall` is sent for calls shown with `displayIncomingCall`, once Telecom has created the call, as on iOS. It was only sent by the native `reportNewIncomingCall`, right away, even when the call was then refused or ignored. Its fields now follow the documentation: `localizedCallerName` (`name` is kept), `hasVideo` as `"1"` or `"0"` (it was `"true"` or `"false"`), `fromPushKit` as `"0"`, and the `payload` of `reportNewIncomingCall` or of the native `displayIncomingCall`. ([#672](https://github.com/react-native-webrtc/react-native-callkeep/pull/672), @RamsayRomero; fixes [#671](https://github.com/react-native-webrtc/react-native-callkeep/issues/671))
- The npm package no longer includes the release notes file (`notes.md`) and an IntelliJ project file (`android/react-native-callkeep.iml`), which 4.4.0 shipped by mistake.

## [4.4.0] - 2026-10-17

Includes everything tagged as 4.3.17 on 2026-09-10, which was never published to npm.

### Added

- **iOS:** `RNCallKeep.continueUserActivity(_:)`, for apps with a scene life cycle, which iOS 27 requires: forward the scene delegate's user activities to it so calls started from Recents, Contacts or Siri still send `didReceiveStartCallAction`. The pod now defines a module, so Swift code can `import RNCallKeep`. The README explains the scene setup.
- **Docs:** A new example app (`example/`, React Native 0.87, TypeScript) runs every part of the library with simulated calls from this repository's code, and documents the native setup on both platforms. It replaces the unmaintained Expo 34 example.
- **Docs:** `CONTRIBUTING.md` (how to try a change in an app, what a pull request needs), `SECURITY.md` (private vulnerability reporting), this changelog, and every contributor in `AUTHORS`.
- **iOS:** Privacy manifest (`PrivacyInfo.xcprivacy`, UserDefaults reason CA92.1, no collected data) shipped through a CocoaPods resource bundle. The podspec now uses React Native's minimum iOS version when available. ([#770](https://github.com/react-native-webrtc/react-native-callkeep/pull/770), @ujeon, follow-up by @sboily)

### Changed

- Releases are staged on npm by GitHub Actions, with provenance, when a tag named after the version is pushed (`4.4.0`, no `v` prefix), and published once a maintainer approves them. The podspec's source tag follows that format.
- **iOS:** The library no longer sets a preferred sample rate (44.1 kHz) and IO buffer duration (5 ms) on the call's audio session. On an iPhone 15 Pro Max with iOS 27 the hardware rate stays 48 kHz either way, and the IO buffer goes from 5 ms to iOS's default (about 23 ms). If your audio stack needs a short buffer and doesn't set one itself, call `AVAudioSession.sharedInstance().setPreferredIOBufferDuration(_:)` in your app. ([#858](https://github.com/react-native-webrtc/react-native-callkeep/pull/858), @fhilleriteau-fdi)
- **Docs:** `didDeactivateAudioSession`, `didResetProvider` and `createIncomingConnectionFailed` added to the events table. ([#804](https://github.com/react-native-webrtc/react-native-callkeep/pull/804), @davidliu)

### Fixed

- **JS/Typings:** `getAudioRoutes()` is typed as `Promise<AudioRoute[]>` (it was `Promise<void>`), and `setMutedCall` is no longer documented as iOS-only: Android has it too. The README documents the returned array and its `selected` field. ([#838](https://github.com/react-native-webrtc/react-native-callkeep/pull/838) found the return type, @palmtown)
- **iOS:** Events received before the module is initialized (for example an incoming call reported from `AppDelegate` on a VoIP push cold start) are no longer lost: `_delayedEvents` is now created when the singleton is allocated. ([#877](https://github.com/react-native-webrtc/react-native-callkeep/pull/877), @minhuwv)
- **iOS:** `getAudioRoutes`/`getAudioInputs` no longer activate the audio session, so they no longer interrupt other apps' audio, switch the output to the receiver, or bring up a hands-free (car) call after a call ends. Outside a call, `selected` now reflects the route actually in use. ([#863](https://github.com/react-native-webrtc/react-native-callkeep/pull/863), @siebeg-ideal)
- **iOS:** `setAudioRoute` and `getAudioRoutes` reject with the exception description (a string) instead of the `NSException` itself, as React Native 0.79 requires. ([#845](https://github.com/react-native-webrtc/react-native-callkeep/pull/845), @KorotkovAlex)
- **Android:** On Android 6 to 11, `displayIncomingCall` and `startCall` no longer hang with the call UI only showing after a reload ([#824](https://github.com/react-native-webrtc/react-native-callkeep/issues/824), [#842](https://github.com/react-native-webrtc/react-native-callkeep/issues/842)). The call-state listener ran `Looper.loop()` on React Native's module thread; it now runs on a `HandlerThread` and works from threads without a looper. ([#825](https://github.com/react-native-webrtc/react-native-callkeep/pull/825), @JoBolNex, follow-up by @sboily)
- **Android:** Ending one of several calls no longer stops the foreground service while another call is still active, so the remaining call keeps its microphone in the background. ([#826](https://github.com/react-native-webrtc/react-native-callkeep/pull/826), @FedorZarubin)
- **Android:** Events raised before React is ready on a cold start (for example answer or end from the native call UI) are kept and replayed in order once a React instance and listeners exist, and `getInitialEvents()` returns them. Before, they could be lost, or JS could be accessed before an instance existed. ([#875](https://github.com/react-native-webrtc/react-native-callkeep/pull/875), @sboily)
- **Android:** Self-managed incoming calls (`selfManaged: true`) no longer crash when `READ_PHONE_NUMBERS` is denied: the connection is flagged self-managed without looking up the phone account. ([#876](https://github.com/react-native-webrtc/react-native-callkeep/pull/876), @sboily)
- **Android:** `stopForegroundService` passes `Service.STOP_FOREGROUND_REMOVE` to `stopForeground` (it passed the microphone foreground-service type by mistake), so the foreground notification is removed. ([#742](https://github.com/react-native-webrtc/react-native-callkeep/pull/742), @ramijarrar)
- **Android:** `openPhoneAccounts()` opens the phone-account settings directly on Oppo devices. ([#751](https://github.com/react-native-webrtc/react-native-callkeep/pull/751), @rimonhanna)
- **Android:** Removed the three-argument overloads of `displayIncomingCall` and `startCall`, which React Native 0.76 rejects as duplicate method names. The JS API is unchanged. ([#816](https://github.com/react-native-webrtc/react-native-callkeep/pull/816), @Irfanwani)
- **Android:** The Gradle `namespace` (`io.wazo.callkeep`) is set, so the library builds with Android Gradle Plugin 8 (React Native 0.73). ([#781](https://github.com/react-native-webrtc/react-native-callkeep/pull/781), @gasolin)
- **Android:** `@RequiresApi(S)` added on `CallStateListener` (lint). ([#741](https://github.com/react-native-webrtc/react-native-callkeep/pull/741), @ramijarrar)
- **JS/Typings:** `NativeEvents.onHasActiveCall` now holds the native event name `'RNCallKeepHasActiveCall'` (it was `'onHasActiveCall'`), so the `InitialEvents` entries for this event are typed correctly.
- The npm package no longer ships the `tests/` folder.

## [4.3.16] - 2024-11-18

### Fixed

- **Android:** The legacy telephony listener only prepares a `Looper` when its thread does not already have one. ([#819](https://github.com/react-native-webrtc/react-native-callkeep/pull/819), @manuquentin)

## [4.3.15] - 2024-11-13

### Changed

- **Docs:** Android installation steps updated for Android 14 (target SDK 34), which requires the foreground service type and its permissions in the manifest. ([#806](https://github.com/react-native-webrtc/react-native-callkeep/pull/806), @wkwiatek)

### Fixed

- **Android:** The legacy `TelephonyManager` call-state listener (Android 11 and lower) no longer crashes when it is created on a thread without a `Looper`. ([#817](https://github.com/react-native-webrtc/react-native-callkeep/pull/817), @manuquentin)

## [4.3.14] - 2024-07-10

### Fixed

- **Android:** Fixed the `Trying to call native module before CatalystInstance has been set` error outside self-managed mode: the module uses its stored React context instead of `getReactApplicationContext()`. ([#794](https://github.com/react-native-webrtc/react-native-callkeep/pull/794), @manuquentin)

## [4.3.13] - 2024-04-23

### Fixed

- **Android:** A call ended with `reportEndCallWithUUID` (rather than `endCall`) now stops listening to the native call state, avoiding `IllegalStateException: ... exceeded the number of permissible registered listeners`. Waking the app up for a call no longer throws if the headless service cannot be started. ([#769](https://github.com/react-native-webrtc/react-native-callkeep/pull/769), @manuquentin)
- **iOS:** Fixed a crash in `getAudioOutput` when the current route has no output ([#755](https://github.com/react-native-webrtc/react-native-callkeep/issues/755)). ([#772](https://github.com/react-native-webrtc/react-native-callkeep/pull/772), @manuquentin)

## [4.3.12] - 2023-08-17

### Added

- **Android:** `checkIsInManagedCall()` tells whether a native call is active, and the `onHasActiveCall` event tells JS when a native call is answered during an app call. ([#724](https://github.com/react-native-webrtc/react-native-callkeep/pull/724), @saif-o99)

### Fixed

- **Android:** Calls are ended when the app is destroyed (`onHostDestroy`), and the call UUID no longer changes after `startCall` ([#625](https://github.com/react-native-webrtc/react-native-callkeep/issues/625)). ([#724](https://github.com/react-native-webrtc/react-native-callkeep/pull/724), @saif-o99)
- **iOS:** The event emitter's `_listenerCount` is kept in sync through key-value coding, so events keep working after a bridge invalidation on recent React Native versions ([#406](https://github.com/react-native-webrtc/react-native-callkeep/issues/406)). ([#574](https://github.com/react-native-webrtc/react-native-callkeep/pull/574), @glesperance)

## [4.3.11] - 2023-07-31

### Fixed

- **Android:** The `payload` variants of `displayIncomingCall` and `startCall` added in 4.3.10 are no longer exported as React methods; they stay available to native code. ([#721](https://github.com/react-native-webrtc/react-native-callkeep/pull/721), @manuquentin)

## [4.3.10] - 2023-07-31

### Added

- **Android:** `displayIncomingCall` and `startCall` accept an optional `payload` bundle, stored in the call extras, to carry extra call information (useful in self-managed mode). ([#700](https://github.com/react-native-webrtc/react-native-callkeep/pull/700), @manuquentin)
- **iOS:** `setup` options `audioSession.categoryOptions` and `audioSession.mode` override the default `AVAudioSession` category options and mode. ([#708](https://github.com/react-native-webrtc/react-native-callkeep/pull/708), @JoL0712)

### Changed

- **iOS:** Log messages use single brackets consistently. ([#718](https://github.com/react-native-webrtc/react-native-callkeep/pull/718), @jpotts18)
- **Docs:** README link fix. ([#711](https://github.com/react-native-webrtc/react-native-callkeep/pull/711), @jgarplind)
- Example app dependency updates.

## [4.3.9] - 2023-06-12

### Added

- **JS/Typings:** Several listeners can be registered for the same event. `addEventListener` no longer replaces the previous listener and returns an object with `remove()`; `removeEventListener(type, listener)` removes one listener, or every listener of that type when `listener` is omitted. ([#683](https://github.com/react-native-webrtc/react-native-callkeep/pull/683), @JoL0712)

### Changed

- **Docs:** README section on usage with Expo. ([#697](https://github.com/react-native-webrtc/react-native-callkeep/pull/697), @JorritKeijzer)
- Example app: held calls are removed correctly. ([#695](https://github.com/react-native-webrtc/react-native-callkeep/pull/695), @SuperBesse)
- Example app dependency updates.

### Fixed

- **Android:** The module instance is null-checked in `fetchStoredSettings`, fixing a `NullPointerException` seen on many devices. ([#693](https://github.com/react-native-webrtc/react-native-callkeep/pull/693), @kasperkberg)
- **JS/Typings:** Fixed the `didLoadWithEvents` and `didReceiveStartCallAction` payload types, the `setSettings` and `setForegroundServiceSettings` input shapes, and the `getCalls` return type. ([#686](https://github.com/react-native-webrtc/react-native-callkeep/pull/686), @ramijarrar)

## [4.3.8] - 2023-03-21

### Fixed

- **Android:** The broadcast receiver is marked as registered sooner, avoiding a double registration. ([#677](https://github.com/react-native-webrtc/react-native-callkeep/pull/677), @manuquentin)
- **Android:** Stored settings are no longer fetched in the module constructor, avoiding a race condition with the singleton. ([#674](https://github.com/react-native-webrtc/react-native-callkeep/pull/674), @manuquentin)

## [4.3.7] - 2023-03-01

### Added

- **iOS:** Car audio (`AVAudioSessionPortCarAudio`) is reported as the `CarAudio` audio route type. ([#673](https://github.com/react-native-webrtc/react-native-callkeep/pull/673), @bradzickafoose)

### Fixed

- **Android:** The `ArrayIndexOutOfBoundsException` thrown by `ReadableNativeMap.hasKey` while reading the foreground service settings is caught, and a failure to start or stop the foreground service no longer crashes the app. ([#669](https://github.com/react-native-webrtc/react-native-callkeep/pull/669), @manuquentin)

## [4.3.6] - 2023-01-31

### Fixed

- **Android:** Fixed a crash when the foreground service starts before the app activity exists (app killed): the notification only opens the app when the activity exists. ([#659](https://github.com/react-native-webrtc/react-native-callkeep/pull/659), @manuquentin)

## [4.3.5] - 2023-01-30

### Added

- **Android:** Tapping the foreground service notification opens the app. ([#652](https://github.com/react-native-webrtc/react-native-callkeep/pull/652), @manuquentin)
- **JS/Typings:** Event handler arguments are typed, with a new `InitialEvents` type also used by `getInitialEvents()`. ([#657](https://github.com/react-native-webrtc/react-native-callkeep/pull/657), @ramijarrar)

### Changed

- **Android:** `openPhoneAccounts()` opens the phone-account settings directly on OnePlus devices, as on Samsung. ([#653](https://github.com/react-native-webrtc/react-native-callkeep/pull/653), @kasperkberg)
- **Docs:** `didChangeAudioRoute` documentation updated ([#618](https://github.com/react-native-webrtc/react-native-callkeep/pull/618), @homersimpsons); iOS installation fixed for a `continueUserActivity` parameter-type warning ([#604](https://github.com/react-native-webrtc/react-native-callkeep/pull/604), @MrHertal).
- Example app dependency updates.

### Fixed

- **Android:** `onSilence` is no longer called below API level 29, fixing a `NoSuchMethodError` in self-managed mode ([#526](https://github.com/react-native-webrtc/react-native-callkeep/issues/526)). ([#658](https://github.com/react-native-webrtc/react-native-callkeep/pull/658), @manuquentin)
- **Android:** The audio mode is set back to normal after a call ends, so audio can be controlled again in the app and in other apps. ([#633](https://github.com/react-native-webrtc/react-native-callkeep/pull/633), @saif-o99)
- **iOS:** The speaker button of the CallKit UI no longer switches itself off (the `AllowBluetoothA2DP` category option is added). ([#649](https://github.com/react-native-webrtc/react-native-callkeep/pull/649), @agustinferrari)

## [4.3.4] - 2023-01-18

### Added

- **Android:** `displayIncomingCall` and `startCall` pass `hasVideo` to the connection (useful for self-managed apps that show video calls). ([#570](https://github.com/react-native-webrtc/react-native-callkeep/pull/570), @manuquentin)
- Audio routes returned by `getAudioRoutes()` have a `selected` property; on iOS, calling `getAudioRoutes()` no longer switches the audio. ([#481](https://github.com/react-native-webrtc/react-native-callkeep/pull/481), @mathias5r)

### Changed

- **Android:** A `VoiceConnectionService` log message now matches what the code does. ([#624](https://github.com/react-native-webrtc/react-native-callkeep/pull/624), @homersimpsons)
- **Docs:** `didLoadWithEvents` marked iOS only and anchors fixed ([#616](https://github.com/react-native-webrtc/react-native-callkeep/pull/616), @homersimpsons); the `RECORD_AUDIO` permission added to the Android installation steps ([#615](https://github.com/react-native-webrtc/react-native-callkeep/pull/615), @aguglie).

### Fixed

- **Android:** `createIncomingConnectionFailed` was sent twice (`Map already consumed` error) because of a missing `break`. ([#579](https://github.com/react-native-webrtc/react-native-callkeep/pull/579), @yotamishak)
- **Android:** Permissions are checked with the application context, so `displayIncomingCall` works when the app was killed (for example from Firebase background messaging). ([#576](https://github.com/react-native-webrtc/react-native-callkeep/pull/576), @mluksha)
- **Android:** `setSettings` updates the settings held by the module (they kept their first value). ([#582](https://github.com/react-native-webrtc/react-native-callkeep/pull/582), @manuquentin)
- **Android:** Added the `androidx.localbroadcastmanager` dependency ([#594](https://github.com/react-native-webrtc/react-native-callkeep/issues/594)). ([#596](https://github.com/react-native-webrtc/react-native-callkeep/pull/596), @ohheyitskartik)

## [4.3.3] - 2022-04-15

### Added

- `displayCallReachabilityTimeout` setup option: if the JS app has not called `setReachable()` when the timeout fires, it is considered crashed and the call is ended. Also adds `clearInitialEvents()` and `setSettings()`, and `getInitialEvents()` on Android. ([#503](https://github.com/react-native-webrtc/react-native-callkeep/pull/503), @manuquentin)
- **iOS:** `handleType` in `setup` accepts an array of handle types. ([#542](https://github.com/react-native-webrtc/react-native-callkeep/pull/542), @manuquentin)

### Changed

- **Docs:** `supportConnectionService` is not asynchronous. ([#549](https://github.com/react-native-webrtc/react-native-callkeep/pull/549), @linus-komnick)
- Example app dependency updates.

### Fixed

- **Android:** `hasPhoneAccount` checks permissions first, fixing a `SecurityException` crash on Samsung devices when the phone permission was denied. ([#556](https://github.com/react-native-webrtc/react-native-callkeep/pull/556), @geraintwhite)

## [4.3.2] - 2022-02-16

### Added

- `didChangeAudioRoute` event, sent when the audio route changes. ([#514](https://github.com/react-native-webrtc/react-native-callkeep/pull/514), @manuquentin)
- **Android:** `setConnectionState(uuid, state)` sets the state of a connection. ([#524](https://github.com/react-native-webrtc/react-native-callkeep/pull/524), @manuquentin)
- **Android:** `createIncomingConnectionFailed` event, sent when Android refuses an incoming call (`onCreateIncomingConnectionFailed`); self-managed apps should handle it. ([#500](https://github.com/react-native-webrtc/react-native-callkeep/pull/500), @jonastelzio)
- **iOS:** `didDisplayIncomingCall` includes an `errorCode` (`Unentitled`, `CallUUIDAlreadyExists`, `FilteredByDoNotDisturb`, `FilteredByBlockList` or `Unknown`) ([#515](https://github.com/react-native-webrtc/react-native-callkeep/issues/515)). ([#519](https://github.com/react-native-webrtc/react-native-callkeep/pull/519), @cjpete)

### Changed

- **Android:** Settings passed to `setup` or `setForegroundServiceSettings` are stored, so the foreground service can start when a call is displayed directly from a push notification. ([#499](https://github.com/react-native-webrtc/react-native-callkeep/pull/499), @manuquentin)
- **Android:** Delayed events use the same `{ name, data }` structure as on iOS. ([#521](https://github.com/react-native-webrtc/react-native-callkeep/pull/521), @MartinElvar)

### Fixed

- **Android:** `ConcurrentModificationException` when ending calls. ([#502](https://github.com/react-native-webrtc/react-native-callkeep/pull/502), @manuquentin)
- **Android:** `new NativeEventEmitter()` warnings about missing `addListener` and `removeListeners` methods. ([#505](https://github.com/react-native-webrtc/react-native-callkeep/pull/505), @mathias5r)
- **iOS:** Crash in the audio-route change handler when there is no output. ([#516](https://github.com/react-native-webrtc/react-native-callkeep/pull/516), @manuquentin)
- **iOS:** `RNCallKeepDidChangeAudioRoute` added to the supported events, and `didChangeAudioRoute` to the TypeScript events. ([#523](https://github.com/react-native-webrtc/react-native-callkeep/pull/523), @yunusaydinonline)

## [4.3.1] - 2021-10-13

### Fixed

- **Android:** Build error caused by `isSelfManaged` being declared twice in 4.3.0. ([`50a8ef8`](https://github.com/react-native-webrtc/react-native-callkeep/commit/50a8ef8), @manuquentin)

## [4.3.0] - 2021-10-13

### Added

- **Android:** An incoming call can be displayed natively from a push notification receiver, before React Native has started, with `RNCallKeepModule.getInstance(...)` and `reportNewIncomingCall(...)`. Events sent before JS listens are queued and delivered with `didLoadWithEvents`. Adds `unregisterAndroidEvents()`. ([#440](https://github.com/react-native-webrtc/react-native-callkeep/pull/440), @manuquentin)
- `getAudioRoutes()` and `setAudioRoute(uuid, routeName)` list and switch audio devices (phone, speaker, Bluetooth, headset). ([#462](https://github.com/react-native-webrtc/react-native-callkeep/pull/462), @mathias5r)
- **iOS:** `getInitialEvents()` returns the events queued before JS was ready. ([#417](https://github.com/react-native-webrtc/react-native-callkeep/pull/417), @remigijusbalc)
- **Android:** `silenceIncomingCall` event (`Connection.onSilence()`), to mute the incoming call notification on Android 10 and lower. ([#431](https://github.com/react-native-webrtc/react-native-callkeep/pull/431), @jonastelzio)

### Changed

- **Android:** The `READ_CALL_LOG` permission is removed from the library manifest, because Google Play refuses apps that declare it without self-managed mode. Self-managed apps must add it to their own manifest. ([#438](https://github.com/react-native-webrtc/react-native-callkeep/pull/438), @manuquentin)
- **Android:** `registerPhoneAccount()` takes the same options as `setup()`, so the `selfManaged` option applies when registering manually. ([#472](https://github.com/react-native-webrtc/react-native-callkeep/pull/472), @MrHertal)
- **Android:** The log tag no longer contains a colon (`adb logcat *:S RNCallKeep:V`), and more logs are written. ([#409](https://github.com/react-native-webrtc/react-native-callkeep/pull/409), @manuquentin)
- **Docs:** README navigation ([#464](https://github.com/react-native-webrtc/react-native-callkeep/pull/464), @goul4rt), documentation improvements ([#473](https://github.com/react-native-webrtc/react-native-callkeep/pull/473), @manuquentin) and `getInitialEvents` documentation ([#421](https://github.com/react-native-webrtc/react-native-callkeep/pull/421), @remigijusbalc).
- Example app dependency updates.

### Fixed

- **Android:** Answer and reject are handled whichever `Connection` callback the device calls (`onAnswer()` or `onAnswer(int)`, and the reject variants); some devices call only one of them. ([#420](https://github.com/react-native-webrtc/react-native-callkeep/pull/420), @manuquentin)
- **Android:** No crash when the `selfManaged` key is missing from the settings. ([#463](https://github.com/react-native-webrtc/react-native-callkeep/pull/463), @khanhduy62)
- **Android:** Compilation error caused by missing semicolons. ([#485](https://github.com/react-native-webrtc/react-native-callkeep/pull/485), @os30722)
- **Android:** Compilation error in the `onReject(int)` override. ([#468](https://github.com/react-native-webrtc/react-native-callkeep/pull/468), @CaptainJeff)
- **iOS:** The speaker button of the CallKit UI behaves correctly (the audio session mode is `AVAudioSessionModeDefault`). ([#495](https://github.com/react-native-webrtc/react-native-callkeep/pull/495), @vinh-thieu)

## [4.2.0] - 2021-05-17

### Added

- **Android:** Self-managed mode, enabled with `selfManaged: true` in the Android `setup` options, with the `showIncomingCallUi` event; nothing changes for apps that do not enable it. ([#395](https://github.com/react-native-webrtc/react-native-callkeep/pull/395), @jonastelzio)
- **iOS:** `[RNCallKeep setup:]` can be called from `AppDelegate.m`, so CallKit events such as answer and end are caught before JS has started. ([#394](https://github.com/react-native-webrtc/react-native-callkeep/pull/394), @jonastelzio)
- **JS/Typings:** `emit` is exported from the package, to re-emit the events received with `didLoadWithEvents`. ([#387](https://github.com/react-native-webrtc/react-native-callkeep/pull/387), @geraintwhite)

### Changed

- **Docs:** Android self-managed mode ([#407](https://github.com/react-native-webrtc/react-native-callkeep/pull/407), @jonastelzio) and `foregroundServiceType` on Android below 11 ([#405](https://github.com/react-native-webrtc/react-native-callkeep/pull/405), @manuquentin).
- Example app dependency updates.

### Fixed

- **Android:** The `mavenCentral()` repository is added, as JCenter is shutting down. ([#400](https://github.com/react-native-webrtc/react-native-callkeep/pull/400), @maheshwarimrinal)
- **JS/Typings:** `foregroundService` added to the Android options. ([#384](https://github.com/react-native-webrtc/react-native-callkeep/pull/384), @geraintwhite)

## [4.1.0] - 2021-03-22

### Added

- **iOS:** `answerIncomingCall(uuid)` answers an incoming call programmatically, for example from in-app UI when iOS 14 shows the call as a banner. ([#300](https://github.com/react-native-webrtc/react-native-callkeep/pull/300), @dtsolis)
- **iOS:** `getCalls()` returns the current calls and their states. ([#366](https://github.com/react-native-webrtc/react-native-callkeep/pull/366), @Jerome91410)
- **Android:** `toggleAudioRouteSpeaker(uuid, routeSpeaker)` switches the audio of a ConnectionService call between speaker and earpiece. ([#295](https://github.com/react-native-webrtc/react-native-callkeep/pull/295), @vtn-dev-prithipal)

### Changed

- **Docs:** Note on using `didLoadWithEvents` ([#333](https://github.com/react-native-webrtc/react-native-callkeep/pull/333), @zxcpoiu); new react-native-voip-push-notification link ([#370](https://github.com/react-native-webrtc/react-native-callkeep/pull/370), @dayze).
- Example app dependency updates.

### Fixed

- **iOS:** `isCallActive` no longer returns `undefined`. ([#364](https://github.com/react-native-webrtc/react-native-callkeep/pull/364), @Jerome91410)
- **Android:** The reachability check runs once per module lifetime instead of on every call, and `wakeUpApplication` is no longer called twice when the reachability timeout fires. ([#376](https://github.com/react-native-webrtc/react-native-callkeep/pull/376), @manuquentin)
- **Android:** AndroidX import restored (an app-compat import had come back with the 3.1.5 foreground service). ([#371](https://github.com/react-native-webrtc/react-native-callkeep/pull/371), @Kelt)
- **JS/Typings:** `includesCallsInRecents` option and its default documented ([#356](https://github.com/react-native-webrtc/react-native-callkeep/pull/356), @markgibaud-vtail); `setup` return type ([#362](https://github.com/react-native-webrtc/react-native-callkeep/pull/362), @martin-juul); `CONSTANTS` declared ([#373](https://github.com/react-native-webrtc/react-native-callkeep/pull/373), @maludwig).

## [4.0.1] - 2020-12-01

### Changed

- The library no longer registers its own `didLoadWithEvents` listener (which re-emitted every queued event, so the same event could reach several handlers); apps handle `didLoadWithEvents` themselves. ([#315](https://github.com/react-native-webrtc/react-native-callkeep/pull/315), @zxcpoiu)

### Fixed

- **Android:** The foreground service compiles with SDK versions below 30 ([#327](https://github.com/react-native-webrtc/react-native-callkeep/issues/327)). ([#331](https://github.com/react-native-webrtc/react-native-callkeep/pull/331), @manuquentin)
- **Android:** App crash after the permissions dialog is closed ([#288](https://github.com/react-native-webrtc/react-native-callkeep/issues/288)); permissions are requested through React Native's `PermissionsModule`. ([#324](https://github.com/react-native-webrtc/react-native-callkeep/pull/324), @nerdymind-dev)

## [4.0.0] - 2020-11-24

No code change since 3.1.5: this major version marks the iOS `reportNewIncomingCall` signature change shipped in 3.1.5 and adds MIGRATION_v3_v4.md.

### Changed

- **BREAKING** **iOS:** The native `reportNewIncomingCall` method takes `supportsHolding`, `supportsDTMF`, `supportsGrouping` and `supportsUngrouping` (introduced by [#238](https://github.com/react-native-webrtc/react-native-callkeep/pull/238) in 3.1.5). Migration: update the call in `AppDelegate.m` to the new signature, see MIGRATION_v3_v4.md. ([`3f8aaed`](https://github.com/react-native-webrtc/react-native-callkeep/commit/3f8aaed), @manuquentin)
- **Docs:** `notificationIcon` typo. ([#326](https://github.com/react-native-webrtc/react-native-callkeep/pull/326), @geraintwhite)

## [3.1.5] - 2020-11-23

### Added

- **iOS:** `displayIncomingCall` and `updateDisplay` accept an `options.ios` object (`supportsHolding`, `supportsDTMF`, `supportsGrouping`, `supportsUngrouping`, and `hasVideo` for `updateDisplay`). ([#238](https://github.com/react-native-webrtc/react-native-callkeep/pull/238), @zxcpoiu)
- **Android:** Optional foreground service (`foregroundService` setup option and `setForegroundServiceSettings()`), so the app keeps the microphone in the background on Android 11 ([#274](https://github.com/react-native-webrtc/react-native-callkeep/issues/274)). ([#321](https://github.com/react-native-webrtc/react-native-callkeep/pull/321), @manuquentin)

### Changed

- **BREAKING** **iOS:** The native `reportNewIncomingCall` method takes the four `supports*` flags and the shorter variants are removed. Migration: update `AppDelegate.m` as described in MIGRATION_v3_v4.md (the change was re-released as 4.0.0). ([#238](https://github.com/react-native-webrtc/react-native-callkeep/pull/238), @zxcpoiu)

## [3.1.5-rc.1] - 2020-11-23

No changelog recorded.

## [3.1.4] - 2020-11-11

### Fixed

- **Android:** The permissions requested depend on the OS version: `READ_PHONE_NUMBERS` on Android 11 and later, `READ_PHONE_STATE` before. ([#314](https://github.com/react-native-webrtc/react-native-callkeep/pull/314), @manuquentin)

## [3.1.3] - 2020-11-11

### Added

- **Android:** Android 11 permission support: `READ_PHONE_NUMBERS` is declared and requested, and `READ_PHONE_STATE` is limited to SDK 29 and lower. ([#313](https://github.com/react-native-webrtc/react-native-callkeep/pull/313), @sboily)

## [3.1.2] - 2020-11-11

### Added

- **Android:** `checkPhoneAccountEnabled()` checks whether the phone account is enabled without prompting, `isConnectionServiceAvailable()`, and `canMakeMultipleCalls(state)` to disable the "Add call" button. ([#298](https://github.com/react-native-webrtc/react-native-callkeep/pull/298), @manuquentin)

### Changed

- **Android:** Migrated to AndroidX (with Jetifier). ([#210](https://github.com/react-native-webrtc/react-native-callkeep/pull/210), @maximvoven)
- **Docs:** README typo ([#247](https://github.com/react-native-webrtc/react-native-callkeep/pull/247), @manuquentin); react-native-firebase documentation link ([#309](https://github.com/react-native-webrtc/react-native-callkeep/pull/309), @RinorDreshaj).

### Fixed

- **Android:** Potential `NullPointerException` in `hasPermissions` ([#305](https://github.com/react-native-webrtc/react-native-callkeep/issues/305)). ([#306](https://github.com/react-native-webrtc/react-native-callkeep/pull/306), @geraintwhite)
- **JS/Typings:** `An implementation cannot be declared in ambient contexts` errors in `index.d.ts`. ([#272](https://github.com/react-native-webrtc/react-native-callkeep/pull/272), @berdimyradov)

## [3.1.1] - 2020-07-21

### Added

- **Android:** `setup()` is split into `registerPhoneAccount()` and `registerAndroidEvents()`, so events can be bound without prompting the user when the phone account is disabled. ([#242](https://github.com/react-native-webrtc/react-native-callkeep/pull/242), @manuquentin)
- **iOS:** Events sent before JS was ready ([#107](https://github.com/react-native-webrtc/react-native-callkeep/issues/107)) are queued and delivered with the new `didLoadWithEvents` event. ([#205](https://github.com/react-native-webrtc/react-native-callkeep/pull/205), @manuquentin)

### Changed

- Example app dependency updates.

### Fixed

- **JS/Typings:** `setCurrentCallActive` takes a `callUUID`. ([#240](https://github.com/react-native-webrtc/react-native-callkeep/pull/240), @kafkas)

## [3.1.0] - 2020-07-06

### Changed

- **Android:** When a call is made from the native dialer while the app is killed, the app is woken up through a `HeadlessJsTaskService` again instead of an event. This reverts a change from 3.0.15 and breaks the Flutter integration. ([#234](https://github.com/react-native-webrtc/react-native-callkeep/pull/234), @manuquentin)

### Fixed

- **JS/Typings:** `answerIncomingCall` declared. ([#235](https://github.com/react-native-webrtc/react-native-callkeep/pull/235), @CaptainJeff)

## [3.0.15] - 2020-06-23

### Changed

- **Android:** The React Native part is separated from the rest of the Android code, so the ConnectionService code can be reused (for example from Flutter). ([#137](https://github.com/react-native-webrtc/react-native-callkeep/pull/137), @LinusU)

### Fixed

- **Android:** `backToForeground()` launches the app when its activity does not exist (app killed) instead of throwing a `NullPointerException`. ([#223](https://github.com/react-native-webrtc/react-native-callkeep/pull/223), @manuquentin)

## [3.0.14] - 2020-06-02

### Fixed

- **iOS:** Starting a call from Recents (`didReceiveStartCallAction`) works again: the default supported handle type is the phone number again (broken in 3.0.13, [#206](https://github.com/react-native-webrtc/react-native-callkeep/issues/206)). ([#207](https://github.com/react-native-webrtc/react-native-callkeep/pull/207), @geraintwhite)

## [3.0.13] - 2020-05-15

### Added

- **iOS:** `isCallActive(uuid)`. ([#172](https://github.com/react-native-webrtc/react-native-callkeep/pull/172), @ajnozari)
- **iOS:** `reportNewIncomingCall` accepts a completion handler, so PushKit's completion can be called once CallKit has reported the call. ([#191](https://github.com/react-native-webrtc/react-native-callkeep/pull/191), @lrorthmann)

### Changed

- **Docs:** `removeEventListener` documented. ([#165](https://github.com/react-native-webrtc/react-native-callkeep/pull/165), @manuquentin)

### Fixed

- **iOS:** The video button on the lock screen works: the provider's supported handle type follows the `handleType` setup option, defaulting to `generic`. ([#171](https://github.com/react-native-webrtc/react-native-callkeep/pull/171), @danhnn)
- **iOS:** The backward-compatible `reportNewIncomingCall` variant passes `fromPushKit` instead of `NO`. ([#174](https://github.com/react-native-webrtc/react-native-callkeep/pull/174), @zxcpoiu)
- `NSInvalidArgumentException` when `localizedCallerName` is empty ([#136](https://github.com/react-native-webrtc/react-native-callkeep/issues/136)): it defaults to an empty string. ([#166](https://github.com/react-native-webrtc/react-native-callkeep/pull/166), @manuquentin)

## [3.0.12] - 2020-03-31

### Fixed

- **iOS:** Crash when an event dictionary was built with `nil` values (`NSPlaceholderDictionary`). ([#175](https://github.com/react-native-webrtc/react-native-callkeep/pull/175), @zxcpoiu)

## [3.0.11] - 2020-03-26

### Added

- **iOS:** `reportNewIncomingCall` accepts a `payload` dictionary (data from the VoIP push), passed to JS in `didDisplayIncomingCall`. ([#149](https://github.com/react-native-webrtc/react-native-callkeep/pull/149), @rcidt)
- **iOS:** `reportEndCallWithUUID` is exposed as a native class method, to end an incoming call when a VoIP push cancels it. ([#164](https://github.com/react-native-webrtc/react-native-callkeep/pull/164), @mariouzae)
- **iOS:** `includesCallsInRecents` setup option (iOS 11 and later). ([#167](https://github.com/react-native-webrtc/react-native-callkeep/pull/167), @kylekurz)

### Changed

- **Docs:** `endCall` usage clarified, duplicated `endAllCalls` sections removed and typos fixed ([#168](https://github.com/react-native-webrtc/react-native-callkeep/pull/168), @zxcpoiu); `didDisplayIncomingCall` documentation ([`0028869`](https://github.com/react-native-webrtc/react-native-callkeep/commit/0028869), @manuquentin).

## [3.0.10] - 2020-02-26

### Changed

- **Android:** `setup()` resolves with the result of the phone-account prompt. ([#158](https://github.com/react-native-webrtc/react-native-callkeep/pull/158), @manuquentin)

### Fixed

- **Android:** `hasPhoneAccount` no longer fails when the Telecom service is not available (for example on an emulator). ([#151](https://github.com/react-native-webrtc/react-native-callkeep/pull/151), @manuquentin)

## [3.0.9] - 2020-01-20

### Changed

- **Docs:** Troubleshooting note on using a valid UUID ([#144](https://github.com/react-native-webrtc/react-native-callkeep/pull/144), @sanjaypojo); `didDisplayIncomingCall` documentation ([#145](https://github.com/react-native-webrtc/react-native-callkeep/pull/145), @geraintwhite).

### Fixed

- **iOS:** Crash when calling from the Recents list on iOS 13.2, where `INStartCallIntent.callCapability` is not available ([#130](https://github.com/react-native-webrtc/react-native-callkeep/issues/130)). ([#134](https://github.com/react-native-webrtc/react-native-callkeep/pull/134), @manuquentin)
- **JS/Typings:** Typing fixes. ([#147](https://github.com/react-native-webrtc/react-native-callkeep/pull/147), @28harishkumar)

## [3.0.8] - 2019-12-02

### Added

- **iOS:** `didDisplayIncomingCall` includes `hasVideo`. ([#128](https://github.com/react-native-webrtc/react-native-callkeep/pull/128), @shawerestart)
- `CONSTANTS.END_CALL_REASONS` is exported, with the iOS `ANSWERED_ELSEWHERE` and `DECLINED_ELSEWHERE` end reasons (declined elsewhere maps to remote ended on Android). ([#133](https://github.com/react-native-webrtc/react-native-callkeep/pull/133), @danjenkins)

### Fixed

- **iOS:** The podspec depends on `React`. ([#135](https://github.com/react-native-webrtc/react-native-callkeep/pull/135), @manuquentin)

## [3.0.7] - 2019-11-11

### Fixed

- **Android:** `didActivateAudioSession` is triggered, with the call attributes ([#101](https://github.com/react-native-webrtc/react-native-callkeep/issues/101)). ([#102](https://github.com/react-native-webrtc/react-native-callkeep/pull/102), @geraintwhite)

## [3.0.6] - 2019-10-03

Version 3.0.5 was tagged (`v3.0.5`) but not published to npm; its changes are listed here.

### Fixed

- **iOS:** Builds again with Xcode 10: the iOS 13 `INStartCallIntent` handling depends on the Xcode/SDK version ([#112](https://github.com/react-native-webrtc/react-native-callkeep/issues/112)). ([#113](https://github.com/react-native-webrtc/react-native-callkeep/pull/113), @bhuangy)
- **Android:** Calls made from the native phone app show the ConnectionService UI on some Samsung phones (Android 9 and later). ([#100](https://github.com/react-native-webrtc/react-native-callkeep/pull/100), @manuquentin)

## [3.0.4] - 2019-09-27

### Changed

- **Docs:** `updateDisplay` is available on iOS; README update. ([`184b0c8`](https://github.com/react-native-webrtc/react-native-callkeep/commit/184b0c8), [`ded8ecc`](https://github.com/react-native-webrtc/react-native-callkeep/commit/ded8ecc), @manuquentin)

### Fixed

- **iOS:** On iOS 13, outgoing calls from the call log work again, and the Recents details page no longer always shows the app as video capable. ([#109](https://github.com/react-native-webrtc/react-native-callkeep/pull/109), @bhuangy)

## [3.0.3] - 2019-09-25

### Added

- **Android:** Reachability check for calls made from the native phone app: CallKeep sends `checkReachability` and the app answers with `setReachable()`; without an answer before the timeout, the app is woken up in headless mode. ([#98](https://github.com/react-native-webrtc/react-native-callkeep/pull/98), @manuquentin)

### Changed

- **Docs:** README update. ([`13e1a97`](https://github.com/react-native-webrtc/react-native-callkeep/commit/13e1a97), @manuquentin)

### Fixed

- **iOS:** The `fromPushKit` value of the `didDisplayIncomingCall` payload is sent as `"1"` or `"0"`. ([`49870cd`](https://github.com/react-native-webrtc/react-native-callkeep/commit/49870cd), @manuquentin)

## [3.0.2] - 2019-09-25

### Added

- **Android:** `hasOutgoingCall()` checks that a call made from the Phone app while the app was killed is still alive, since the end event can be missed while the app starts. ([#87](https://github.com/react-native-webrtc/react-native-callkeep/pull/87), @manuquentin)
- **iOS:** New `reportNewIncomingCall` class method to report an incoming call when a VoIP push arrives, as iOS 13 requires. ([#86](https://github.com/react-native-webrtc/react-native-callkeep/pull/86), @r0b0t3d)

### Changed

- `reportUpdatedCall` is deprecated in favour of `updateDisplay`, and the TypeScript definitions are updated. ([#96](https://github.com/react-native-webrtc/react-native-callkeep/pull/96), @manuquentin)

### Fixed

- **Android:** No default phone account check when there is no SIM card. ([#63](https://github.com/react-native-webrtc/react-native-callkeep/pull/63), @geraintwhite)
- **iOS:** Crash (`Bridge is not set`) on calls made from the native app while the app was killed. ([#92](https://github.com/react-native-webrtc/react-native-callkeep/pull/92), @manuquentin)

## [3.0.1] - 2019-08-25

### Changed

- The npm package no longer includes the example app. ([`dd01028`](https://github.com/react-native-webrtc/react-native-callkeep/commit/dd01028), @manuquentin)

## [3.0.0] - 2019-08-25

### Added

- Multiple calls through the native UI and a JS API consistent between iOS and Android, contributed by Sangoma. New methods include `answerIncomingCall`, `rejectCall`, `reportEndCallWithUUID`, `setOnHold`, `sendDTMF`, `updateDisplay` and `reportConnectingOutgoingCallWithUUID` (iOS), and new events `didDeactivateAudioSession` and `didResetProvider`. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **Android:** The app is woken up when a call is made from the native Phone app. ([#79](https://github.com/react-native-webrtc/react-native-callkeep/pull/79), @manuquentin)
- **iOS:** `reportUpdatedCall` changes the caller name of a call. Calls started with `startCall` keep their caller name in the system call history, and video calls are shown as such ([#54](https://github.com/react-native-webrtc/react-native-callkeep/issues/54)). ([#60](https://github.com/react-native-webrtc/react-native-callkeep/pull/60), @chevonc)
- **iOS:** An audio session interruption-ended notification is posted when the audio session is activated, so WebRTC M75 and later can restart audio. ([#80](https://github.com/react-native-webrtc/react-native-callkeep/pull/80), @danjenkins)
- **Android:** Debug logs (`adb logcat *:S RNCallKeepModule:V`). ([#44](https://github.com/react-native-webrtc/react-native-callkeep/pull/44), @manuquentin)
- Example app (`example/`). ([#77](https://github.com/react-native-webrtc/react-native-callkeep/pull/77), @manuquentin)

### Changed

- **BREAKING:** `setup` must be called each time the application is launched. Migration: see MIGRATION_v2_v3.md. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **BREAKING:** `didPerformDTMFAction` passes `digits` instead of `dtmf`. Migration: read `digits`. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **BREAKING:** `didPerformSetMutedCallAction` passes `{ muted, callUUID }`. Migration: read `muted` from the object. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **BREAKING:** `startCall` takes `uuid, handle, contactIdentifier` (then `handleType, hasVideo`). Migration: move `contactIdentifier` to the third argument. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **BREAKING:** `setCurrentCallActive` and `endCall` take the call UUID, and events carry a `callUUID`. Migration: pass the call UUID. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **BREAKING:** UUIDs are returned in lower case. Migration: send lower-case UUIDs so your calls match. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **BREAKING** **Android:** `MainActivity.onRequestPermissionsResult` must forward the result to `RNCallKeepModule.onRequestPermissionsResult(requestCode, permissions, grantResults)`. Migration: see MIGRATION_v2_v3.md. ([#64](https://github.com/react-native-webrtc/react-native-callkeep/pull/64), @kylekurz)
- **Docs:** README fixes ([#58](https://github.com/react-native-webrtc/react-native-callkeep/pull/58), @LinusU; [#73](https://github.com/react-native-webrtc/react-native-callkeep/pull/73), @ThomasLarge); MIGRATION_v2_v3.md and README for the v3 release ([#83](https://github.com/react-native-webrtc/react-native-callkeep/pull/83), @manuquentin).

### Removed

- The library's `yarn.lock` ([#76](https://github.com/react-native-webrtc/react-native-callkeep/issues/76)). ([#81](https://github.com/react-native-webrtc/react-native-callkeep/pull/81), @manuquentin)

## [2.0.3] - 2019-05-22

### Fixed

- **JS/Typings:** Optional argument of `startCall`. ([#50](https://github.com/react-native-webrtc/react-native-callkeep/pull/50), @sambegin)

## [2.0.2] - 2019-05-06

### Added

- **JS/Typings:** TypeScript definitions (`index.d.ts`). ([#30](https://github.com/react-native-webrtc/react-native-callkeep/pull/30), @dy93)
- **Android:** `backToForeground()` brings the app back to the foreground. ([#30](https://github.com/react-native-webrtc/react-native-callkeep/pull/30), @dy93)
- **Android:** `hasDefaultPhoneAccount()` checks for a default outgoing phone account on Samsung devices and offers to open the phone-account settings ([#33](https://github.com/react-native-webrtc/react-native-callkeep/issues/33)). ([#34](https://github.com/react-native-webrtc/react-native-callkeep/pull/34), @geraintwhite)

### Changed

- **Docs:** iOS installation uses the `RNCallKeep` podspec name in the Podfile ([#42](https://github.com/react-native-webrtc/react-native-callkeep/pull/42), @sambegin); README update ([`5722bd5`](https://github.com/react-native-webrtc/react-native-callkeep/commit/5722bd5), @sboily).

### Fixed

- **Android:** `IllegalArgumentException` caused by the permission request code; Gradle resolves `google()` before `jcenter()`. ([#30](https://github.com/react-native-webrtc/react-native-callkeep/pull/30), @dy93)

## [2.0.1] - 2019-03-19

### Fixed

- **Android:** The manifest package is `io.wazo.callkeep`, fixing the wrong import with `react-native link` ([#15](https://github.com/react-native-webrtc/react-native-callkeep/issues/15)). ([#18](https://github.com/react-native-webrtc/react-native-callkeep/pull/18), @manuquentin)
- **Android:** `startCall` does nothing when the required permissions are missing. ([#17](https://github.com/react-native-webrtc/react-native-callkeep/pull/17), @manuquentin)
- **iOS:** `requiresMainQueueSetup` warning. ([#19](https://github.com/react-native-webrtc/react-native-callkeep/pull/19), @manuquentin)
- **iOS:** CallKeep works after the app is reloaded several times in dev mode: the CallKit provider is invalidated when the module is deallocated. ([#24](https://github.com/react-native-webrtc/react-native-callkeep/pull/24), @jitensuthar)

## [2.0.0] - 2019-02-05

### Added

- Hold support: `didToggleHoldCallAction` event (`{ hold, callUUID }`) on iOS and Android, and iOS calls support holding and grouping. ([#14](https://github.com/react-native-webrtc/react-native-callkeep/pull/14), @manuquentin)

### Changed

- `setup()` returns a Promise. On Android it rejects when the user cancels the phone-account prompt; on iOS it rejects instead of throwing when `appName` is missing or not a string. ([#12](https://github.com/react-native-webrtc/react-native-callkeep/pull/12), @manuquentin)
- **Docs:** Mute and DTMF documentation. ([#13](https://github.com/react-native-webrtc/react-native-callkeep/pull/13), @geraintwhite)

## [0.0.6] - 2019-01-29

### Added

- **Android:** `setCurrentCallActive()` marks a call made from the native Phone app as active. ([#11](https://github.com/react-native-webrtc/react-native-callkeep/pull/11), @manuquentin)

### Changed

- **BREAKING** **Android:** `setActive` is renamed `setAvailable`. Migration: call `setAvailable()` instead of `setActive()`. ([#11](https://github.com/react-native-webrtc/react-native-callkeep/pull/11), @manuquentin)

### Fixed

- The `setMutedCAll` method is renamed `setMutedCall`. ([#11](https://github.com/react-native-webrtc/react-native-callkeep/pull/11), @manuquentin)

## [0.0.5] - 2019-01-29

### Added

- **iOS:** DTMF support: `didPerformDTMFAction` is sent with `{ dtmf, callUUID }`. ([#10](https://github.com/react-native-webrtc/react-native-callkeep/pull/10), @manuquentin)

## [0.0.4] - 2019-01-29

### Added

- **Android:** `startCall`, with the `CALL_PHONE` permission, and the `didActivateAudioSession` event. ([#5](https://github.com/react-native-webrtc/react-native-callkeep/pull/5), @geraintwhite)

### Fixed

- **Android:** The connection uses the VoIP audio mode, fixing one-way audio on some devices ([#7](https://github.com/react-native-webrtc/react-native-callkeep/issues/7)). ([#8](https://github.com/react-native-webrtc/react-native-callkeep/pull/8), @danjenkins)
- **Android:** DTMF retrieval: the `didPerformDTMFAction` handler receives the event payload. ([#9](https://github.com/react-native-webrtc/react-native-callkeep/pull/9), @manuquentin)

## [0.0.3] - 2019-01-28

### Changed

- **Docs:** Setup example and `startCall` parameters. ([#4](https://github.com/react-native-webrtc/react-native-callkeep/pull/4), @geraintwhite; [#6](https://github.com/react-native-webrtc/react-native-callkeep/pull/6), @geraintwhite)

### Fixed

- `removeEventListener` failed because it read the handler map from the wrong place. ([#3](https://github.com/react-native-webrtc/react-native-callkeep/pull/3), @danjenkins)

## [0.0.2] - 2018-12-31

### Added

- **Android:** `supportConnectionService()` and `hasPhoneAccount()`. ([#2](https://github.com/react-native-webrtc/react-native-callkeep/pull/2), @manuquentin)

## [0.0.1] - 2018-12-28

### Added

- First release as react-native-callkeep, forked from react-native-callkit 1.3.4 (iOS CallKit, by @ianlin). Adds Android support through ConnectionService; the native module is renamed `RNCallKeep` and `setup` takes `{ ios, android }` options. ([#1](https://github.com/react-native-webrtc/react-native-callkeep/pull/1), @manuquentin)

[Unreleased]: https://github.com/react-native-webrtc/react-native-callkeep/compare/4.4.0...HEAD
[4.4.0]: https://github.com/react-native-webrtc/react-native-callkeep/compare/db693d5...4.4.0
[4.3.16]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.16
[4.3.15]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.15
[4.3.14]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.14
[4.3.13]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.13
[4.3.12]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.12
[4.3.11]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.11
[4.3.10]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.10
[4.3.9]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.9
[4.3.8]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.8
[4.3.7]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.7
[4.3.6]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.6
[4.3.5]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.5
[4.3.4]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.4
[4.3.3]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.3
[4.3.2]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.2
[4.3.1]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.1
[4.3.0]: https://www.npmjs.com/package/react-native-callkeep/v/4.3.0
[4.2.0]: https://github.com/react-native-webrtc/react-native-callkeep/compare/4.1.0...4.2.0
[4.1.0]: https://github.com/react-native-webrtc/react-native-callkeep/compare/4.0.1...4.1.0
[4.0.1]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3f8aaed...4.0.1
[4.0.0]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.1.5...3f8aaed
[3.1.5]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.1.4...3.1.5
[3.1.5-rc.1]: https://www.npmjs.com/package/react-native-callkeep/v/3.1.5-rc.1
[3.1.4]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.1.3...3.1.4
[3.1.3]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.1.2...3.1.3
[3.1.2]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.1.1...3.1.2
[3.1.1]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.1.0...3.1.1
[3.1.0]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.0.15...3.1.0
[3.0.15]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.0.14...3.0.15
[3.0.14]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.0.13...3.0.14
[3.0.13]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.0.12...3.0.13
[3.0.12]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.0.11...3.0.12
[3.0.11]: https://github.com/react-native-webrtc/react-native-callkeep/compare/3.0.10...3.0.11
[3.0.10]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.9...3.0.10
[3.0.9]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.8...v3.0.9
[3.0.8]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.7...v3.0.8
[3.0.7]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.6...v3.0.7
[3.0.6]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.4...v3.0.6
[3.0.4]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.3...v3.0.4
[3.0.3]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.2...v3.0.3
[3.0.2]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.1...v3.0.2
[3.0.1]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v3.0.0...v3.0.1
[3.0.0]: https://github.com/react-native-webrtc/react-native-callkeep/compare/2.0.1...v3.0.0
[2.0.3]: https://www.npmjs.com/package/react-native-callkeep/v/2.0.3
[2.0.2]: https://www.npmjs.com/package/react-native-callkeep/v/2.0.2
[2.0.1]: https://github.com/react-native-webrtc/react-native-callkeep/compare/v2.0.0...2.0.1
[2.0.0]: https://github.com/react-native-webrtc/react-native-callkeep/releases/tag/v2.0.0
[0.0.6]: https://www.npmjs.com/package/react-native-callkeep/v/0.0.6
[0.0.5]: https://www.npmjs.com/package/react-native-callkeep/v/0.0.5
[0.0.4]: https://www.npmjs.com/package/react-native-callkeep/v/0.0.4
[0.0.3]: https://www.npmjs.com/package/react-native-callkeep/v/0.0.3
[0.0.2]: https://www.npmjs.com/package/react-native-callkeep/v/0.0.2
[0.0.1]: https://www.npmjs.com/package/react-native-callkeep/v/0.0.1
