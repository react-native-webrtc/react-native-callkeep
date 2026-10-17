# Setup

Call `RNCallKeep.setup(options)` once, early in your app and before you report or start calls. It configures CallKit on iOS, and the phone account and connection service on Android.

## Example

```js
import RNCallKeep from 'react-native-callkeep';

const options = {
  ios: {
    appName: 'My app name',
  },
  android: {
    alertTitle: 'Permissions required',
    alertDescription: 'This application needs to access your phone accounts',
    cancelButton: 'Cancel',
    okButton: 'ok',
    imageName: 'phone_account_icon',
    additionalPermissions: [],
    // Required to get audio in background when using Android 11
    foregroundService: {
      channelId: 'com.company.my',
      channelName: 'Foreground service for my app',
      notificationTitle: 'My app is running on background',
      notificationIcon: 'ic_launcher', // the name of a mipmap resource
    }, 
  }
};

RNCallKeep.setup(options).then(accepted => {});
```

## Options

- `options`: Object
  - `ios`: object
    - `appName`: string (required)
      It will be displayed on system UI when incoming calls received
    - `imageName`: string (optional)
      If provided, it will be displayed on system UI during the call
    - `ringtoneSound`: string (optional)
      If provided, it will be played when incoming calls received; the system will use the default ringtone if this is not provided
    - `handleType`: string|array (optional)
      If provided, it will tell iOS what kind of handle(s) (number) your app can handle.
      - `generic`
      - `number` (default)
      - `email`
    - `includesCallsInRecents`: boolean (optional)
      If provided, calls will be shown in the recent calls when true and not when false (ios 11 and above) (Default: true)
    - `maximumCallGroups`: string (optional)
      If provided, the maximum number of call groups supported by this application (Default: 3)
    - `maximumCallsPerCallGroup`: string (optional)
      If provided, the maximum number of calls in a single group, used for conferencing (Default: 1, no conferencing)
    - `supportsVideo`: boolean (optional)
      If provided, whether or not the application supports video calling (Default: true)
    - `displayCallReachabilityTimeout`: number in ms (optional)
      If provided, starts a timeout that checks if the application is reachable and ends the call if not (Default: null)
      You'll have to call `setReachable()` as soon as your Javascript application is started.
    - `audioSession`: object
      - `categoryOptions`: AudioSessionCategoryOption|number (optional)
        If provided, it will override the default AVAudioSession setCategory options.
      - `mode`: AudioSessionMode|string (optional)
        If provided, it will override the default AVAudioSession setMode mode.
  - `android`: object
    - `alertTitle`: string (required)
      When asking for _phone account_ permission, we need to provider a title for the `Alert` to ask the user for it
    - `alertDescription`: string (required)
      When asking for _phone account_ permission, we need to provider a description for the `Alert` to ask the user for it
    - `cancelButton`: string (required)
      Cancel button label
    - `okButton`: string (required)
      Ok button label
    - `imageName`: string (optional)
      The image to use in the Android Phone application's native UI for enabling/disabling calling accounts. Should be a 48x48 HDPI
      grayscale PNG image. Must be in your drawable resources for the parent application. Must be lowercase and underscore (_) characters
      only, as Java doesn't like capital letters on resources.
    - `additionalPermissions`: [PermissionsAndroid] (optional)
      Any additional permissions you'd like your app to have at first launch. Can be used to simplify permission flows and avoid
      multiple popups to the user at different times.
    - `foregroundService`: object (optional)
      Keeps the app and its microphone running while a call is in progress; see [Foreground service](#foreground-service-android-11-and-later).
      - `channelId`: string, the notification channel the service posts in. CallKeep creates it if it doesn't exist; an existing channel, such as one your app created, keeps its importance and sound, and takes `channelName`
      - `channelName`: string, the channel's name in the system settings
      - `notificationTitle`: string
      - `notificationIcon`: string (optional), the name of a `mipmap` resource, such as `ic_launcher`
      - `foregroundServiceType`: number (optional, Android 10 and later), the types the service starts with, as a mask of [`ServiceInfo.FOREGROUND_SERVICE_TYPE_*`](https://developer.android.com/reference/android/content/pm/ServiceInfo#FOREGROUND_SERVICE_TYPE_PHONE_CALL) values: `4` for `phoneCall`, `132` for `phoneCall` and `microphone`. Each type must be declared on the service in the manifest. Without it, the service starts with every type the manifest declares. CallKeep starts the service when the call is created and keeps these types for the whole call: with `4` alone, an answered call has no microphone access while the app is in the background. To add `microphone` once the call is answered, call `startForeground(Constants.FOREGROUND_SERVICE_NOTIFICATION_ID, notification, types)` on `VoiceConnectionService.currentConnectionService` from native code, with both types
    - `endCallsWhenReactNativeStops`: boolean (optional, default `true`)
      When React Native's instance stops while the app keeps running (a reload, an over-the-air update), CallKeep ends the calls: the JavaScript that handled them is gone. Set it to `false` when native code owns your calls, as with [setup from native code](#setup-from-native-code-android). See [Calls and the app's lifecycle](#calls-and-the-apps-lifecycle-android).
    - `selfManaged`: boolean (optional)
      When set to true, call keep will configure itself to run as a self managed connection service. See the [self-managed mode guide](guides/android-self-managed.md) and [Google's documentation](https://developer.android.com/guide/topics/connectivity/telecom/selfManaged).
      - `displayCallReachabilityTimeout`: number in ms (optional)
        If provided, starts a timeout that checks if the application is reachable and ends the call if not (Default: null)
        You'll have to call `setReachable()` as soon as your Javascript application is started.
      
`setup` calls internally `registerPhoneAccount`, `registerEvents` and `setSettings`.

You can call `setup()` again to change the options, for example when the user changes a preference. On iOS the new CallKit options (`includesCallsInRecents`, `supportsVideo`, `ringtoneSound`...) apply to the next calls. If you [set up from native code](#setup-from-native-code-ios), call `[RNCallKeep setup:]` again instead.

You can alternatively just call `setSettings()` with the same option as `setup()` to define only your settings.

## Setup from native code (iOS)

On iOS you can also perform setup in `AppDelegate`. Doing this allows capturing events prior to the react native event bridge being up. Please be aware that calling setup in `AppDelegate.m` will ignore any subsequent calls to `RNCallKeep.setup();` from JavaScript: to change the options, call `[RNCallKeep setup:]` again.

```objective-c
@implementation AppDelegate
- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{ 
  self.bridge = [[RCTBridge alloc] initWithDelegate:self launchOptions:launchOptions];

  [RNCallKeep setup:@{
    @"appName": @"Awesome App",
    @"maximumCallGroups": @3,
    @"maximumCallsPerCallGroup": @1,
    @"supportsVideo": @NO,
  }];

  RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:self.bridge
                                                   moduleName:@"App"
                                            initialProperties:nil];

  // ======== OTHER CODE REDACTED ==========

  return YES;
}

```

## Setup from native code (Android)

In self-managed mode, native code can set CallKeep up and show an incoming call before React Native runs, or without it: from a Firebase messaging service, for instance, when a call push wakes the app. `setupNative` takes the `android` options of `setup()`, with `selfManaged: true` (Android 8 and later):

```kotlin
RNCallKeepModule.setupNative(context, Arguments.createMap().apply {
  putBoolean("selfManaged", true)
})
RNCallKeepModule.displayIncomingCall(context, callUUID, number, callerName, false)
```

It stores the settings and registers the phone account as `setup()` does, but creates no module and no React context. Your app then shows its own incoming-call UI and handles the call from CallKeep's local broadcasts: register a receiver with `LocalBroadcastManager` for the `Constants.ACTION_*` actions (`ACTION_SHOW_INCOMING_CALL_UI`, `ACTION_ANSWER_CALL`, `ACTION_END_CALL`, ...); each intent carries the call's `Constants.EXTRA_CALL_UUID` in its `attributeMap` extra. When React Native starts later, its module uses the same settings. Register your receiver before `displayIncomingCall`: the call's actions (`ACTION_SHOW_INCOMING_CALL_UI`, `ACTION_ANSWER_CALL`, `ACTION_END_CALL`, hold, DTMF...) reach only the receivers registered when they happen. Only the connection service's events (`ACTION_DID_DISPLAY_INCOMING_CALL`, `ACTION_ON_CREATE_CONNECTION_FAILED`...) are kept for JavaScript when no receiver gets them, so once your receiver is registered, React Native's [`getInitialEvents()`](api.md#getinitialevents) doesn't return them either.

Telecom gives self-managed calls the call focus (Android 9 and later). To start a call's audio before React Native runs, set `VoiceConnectionService.nativeFocusListener`: `onFocusGained` tells when the call may use audio, and on `onFocusLost` release its audio and microphone, then call `connectionServiceFocusReleased()` on `VoiceConnectionService.currentConnectionService`. `VoiceConnectionService.hasConnectionServiceFocus()` reads the current state; it turns false when the last call ends, for which Telecom calls no `onFocusLost`.

## Calls and the app's lifecycle (Android)

A call lasts as long as the app can handle it, not as long as its screen:

- **The activity is destroyed**, while the app keeps running: a configuration change the activity doesn't handle (font size, language), Back on Android 11 and earlier, or the user swiping the app away from Recents. The call goes on, as in other calling apps; Telecom keeps the app's process running during a call. The user gets back to it from the app or its notification.
- **The app's process dies** (a crash, or the system stops it): Telecom ends its calls.
- **React Native's instance stops** while the process runs (a reload, an over-the-air update): CallKeep ends the calls, unless [`endCallsWhenReactNativeStops`](#options) is `false`.

So keep your call state, and register CallKeep's event listeners, outside your screens, once when the app starts (in `index.js`, for instance): a new activity renders the calls that are still going on. The [example app](../example/) does so.

## Foreground service (Android 11 and later)

Since Android 11, an app needs a [foreground service](https://developer.android.com/about/versions/11/privacy/foreground-services) to use the microphone in the background. Set the `foregroundService` key in the [`setup()` options](#options), and declare the service's types in the manifest: see [Android installation](android-installation.md#manifest).
