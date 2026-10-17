# API reference

Every method is on the default export:

```js
import RNCallKeep from 'react-native-callkeep';
```

`setup()` and its options are described in [setup.md](setup.md), and the events in [events.md](events.md). On a platform marked ❌, a method does nothing, returns a fixed value (`hasPhoneAccount()` resolves `true` on iOS), or rejects (`checkIfBusy()` on Android).

## Methods

| Method | Returns | iOS | Android |
| --- | --- | :-: | :-: |
| [`setup()`](setup.md) | `Promise` | ✅ | ✅ |
| [`setSettings()`](#setsettings) | `void` | ✅ | ✅ |
| [`addEventListener()`](events.md) | `EventListener` | ✅ | ✅ |
| [`removeEventListener()`](#removeeventlistener) | `void` | ✅ | ✅ |
| [`getInitialEvents()`](#getinitialevents) | `Promise<{ name, data }[]>` | ✅ | ✅ |
| [`clearInitialEvents()`](#clearinitialevents) | `void` | ✅ | ✅ |
| [`displayIncomingCall()`](#displayincomingcall) | `void` | ✅ | ✅ |
| [`answerIncomingCall()`](#answerincomingcall) | `void` | ✅ | ✅ |
| [`startCall()`](#startcall) | `void` | ✅ | ✅ |
| [`updateDisplay()`](#updatedisplay) | `void` | ✅ | ✅ |
| [`endCall()`](#endcall) | `void` | ✅ | ✅ |
| [`endAllCalls()`](#endallcalls) | `void` | ✅ | ✅ |
| [`rejectCall()`](#rejectcall) | `void` | ✅ | ✅ |
| [`reportEndCallWithUUID()`](#reportendcallwithuuid) | `void` | ✅ | ✅ |
| [`setMutedCall()`](#setmutedcall) | `void` | ✅ | ✅ |
| [`setOnHold()`](#setonhold) | `void` | ✅ | ✅ |
| [`sendDTMF()`](#senddtmf) | `void` | ✅ | ✅ |
| [`getAudioRoutes()`](#getaudioroutes) | `Promise<AudioRoute[]>` | ✅ | ✅ |
| [`setAudioRoute()`](#setaudioroute) | `Promise` | ✅ | ✅ |
| [`setReachable()`](#setreachable) | `void` | ✅ | ✅ |
| [`isCallActive()`](#iscallactive) | `Promise<boolean>` | ✅ | ❌ |
| [`getCalls()`](#getcalls) | `Promise<object[]>` | ✅ | ❌ |
| [`checkIfBusy()`](#checkifbusy) | `Promise<boolean>` | ✅ | ❌ |
| [`checkSpeaker()`](#checkspeaker) | `Promise<boolean>` | ✅ | ❌ |
| [`reportConnectingOutgoingCallWithUUID()`](#reportconnectingoutgoingcallwithuuid) | `void` | ✅ | ❌ |
| [`reportConnectedOutgoingCallWithUUID()`](#reportconnectedoutgoingcallwithuuid) | `void` | ✅ | ❌ |
| [`setAvailable()`](#setavailable) | `void` | ❌ | ✅ |
| [`setForegroundServiceSettings()`](#setforegroundservicesettings) | `void` | ❌ | ✅ |
| [`canMakeMultipleCalls()`](#canmakemultiplecalls) | `void` | ❌ | ✅ |
| [`setCurrentCallActive()`](#setcurrentcallactive) | `void` | ❌ | ✅ |
| [`setConnectionState()`](#setconnectionstate) | `void` | ❌ | ✅ |
| [`checkIsInManagedCall()`](#checkisinmanagedcall) | `Promise<boolean>` | ❌ | ✅ |
| [`toggleAudioRouteSpeaker()`](#toggleaudioroutespeaker) | `void` | ❌ | ✅ |
| [`supportConnectionService()`](#supportconnectionservice) | `boolean` | ❌ | ✅ |
| [`hasPhoneAccount()`](#hasphoneaccount) | `Promise<boolean>` | ❌ | ✅ |
| [`hasOutgoingCall()`](#hasoutgoingcall) | `Promise<boolean>` | ❌ | ✅ |
| [`hasDefaultPhoneAccount()`](#hasdefaultphoneaccount) | `Promise<void>` | ❌ | ✅ |
| [`checkPhoneAccountEnabled()`](#checkphoneaccountenabled) | `Promise<boolean>` | ❌ | ✅ |
| [`isConnectionServiceAvailable()`](#isconnectionserviceavailable) | `Promise<boolean>` | ❌ | ✅ |
| [`backToForeground()`](#backtoforeground) | `void` | ❌ | ✅ |
| [`registerPhoneAccount()`](#registerphoneaccount) | `void` | ❌ | ✅ |
| [`registerAndroidEvents()`](#registerandroidevents) | `void` | ❌ | ✅ |
| [`unregisterAndroidEvents()`](#unregisterandroidevents) | `void` | ❌ | ✅ |

### setSettings

Stores the settings without the rest of `setup()`, which already calls it. It takes the same options as [`setup()`](setup.md#options) and keeps the `ios` or `android` part for the current platform.

On iOS, CallKit's configuration (`appName`, `imageName`, `ringtoneSound`, `supportsVideo`...) is read once, when `setup()` creates the CallKit provider: a later `setSettings()` doesn't change it.

```js
RNCallKeep.setSettings(options);
```

### setReachable

Tells CallKeep that your JavaScript is running and can handle calls. Call it when your app starts:

- With the `displayCallReachabilityTimeout` [setup option](setup.md#options), CallKeep ends an incoming call it displayed if `setReachable()` hasn't been called when that delay is over.
- On Android, when the user starts a call from the Phone app before your app has called `setup()` (typically because your app isn't running), CallKeep sends [`checkReachability`](events.md#checkreachability). Without a `setReachable()` within two seconds, it wakes your app with the [headless task](android-installation.md#calls-started-from-the-phone-app-managed-mode).

```js
RNCallKeep.setReachable();
```

### getInitialEvents

If there were some actions performed by user before JS context has been created, this method would return early fired events. This is alternative to "didLoadWithEvents" event.

```js
RNCallKeep.getInitialEvents();
```

### clearInitialEvents

Clear all pending actions returned by `getInitialEvents()`.

```js
RNCallKeep.clearInitialEvents();
```

### setAvailable
_This feature is available only on Android._

Tell _ConnectionService_ that the device is ready to make outgoing calls via the native Phone app.
If not the user will be stuck in the build UI screen without any actions.
Eg: Call it with `false` when disconnected from the sip client, when your token expires, when your user log out ...
Eg: When your used log out (or the connection to your server is broken, etc..), you have to call `setAvailable(false)` so CallKeep will refuse the call and your user will not be stuck in the native UI.

```js
RNCallKeep.setAvailable(true);
```

### setForegroundServiceSettings
_This feature is available only on Android._

Configures the [Foreground Service](https://developer.android.com/about/versions/11/privacy/foreground-services) used for Android 11 to get microphone access on background.
Similar to set the `foregroundService` key in the `setup()` method.

```js
RNCallKeep.setForegroundServiceSettings({
    channelId: 'com.company.my',
    channelName: 'Foreground service for my app',
    notificationTitle: 'My app is running on background',
    notificationIcon: 'Path to the resource icon of the notification',
});
```

### canMakeMultipleCalls
_This feature is available only on Android._

Disable the "Add call" button in ConnectionService UI.

```js
RNCallKeep.canMakeMultipleCalls(false); // Enabled by default
```

- `active`: boolean
  - Tell whether the app is ready or not

### setCurrentCallActive
_This feature is available only on Android._

Mark the current call as active (eg: when the callee has answered).
Necessary to set the correct Android capabilities (hold, mute) once the call is set as active.
Be sure to set this only after your call is ready for two way audio; used both incoming and outgoing calls.

```js
RNCallKeep.setCurrentCallActive(uuid);
```

- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`

### checkIsInManagedCall
_This feature is available only on Android._

Returns true if there is an active native call

```js
RNCallKeep.checkIsInManagedCall();
```


### isCallActive
_This feature is available only on IOS._

Returns true if the UUID passed matches an existing and answered call. 
This will return true ONLY if the call exists and the user has already answered the call. It will return false 
if the call does not exist or has not been answered. This is exposed to both React Native and Native sides.
This was exposed so a call can be canceled if ringing and the user answered on a different device.

```js
RNCallKeep.isCallActive(uuid);
```

- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`


### getCalls

_This feature is available only on IOS._

Returns a Promise. The result will be an array with all current calls and their states.

```js
RNCallKeep.getCalls();

response:
[{
  callUUID: "E26B14F7-2CDF-48D0-9925-532199AE7C48",
  hasConnected: true,
  hasEnded: false,
  onHold: false,
  outgoing: false,
}]
```

### displayIncomingCall

Display system UI for incoming calls

```js
RNCallKeep.displayIncomingCall(uid, handle, localizedCallerName = '', handleType = 'number', hasVideo = false, options = null);
```

- `uuid`: string
  - An `uuid` that should be stored and re-used for `stopCall`.
- `handle`: string
  - Phone number of the caller
- `localizedCallerName`: string (optional)
  - Name of the caller to be displayed on the native UI
- `handleType`: string (optional, iOS only)
  - `generic`
  - `number` (default)
  - `email`
- `hasVideo`: boolean (optional, iOS only)
  - `false` (default)
  - `true` (you know... when not false)
- `options`: object (optional)
  - `ios`: object
    - `supportsHolding`: boolean (optional, default true)
    - `supportsDTMF`: boolean (optional, default true)
    - `supportsGrouping`: boolean (optional, default true)
    - `supportsUngrouping`: boolean (optional, default true)
  - `android`: object (currently no-op)

### answerIncomingCall

Use this to tell the sdk a user answered a call from the app UI.

```js
RNCallKeep.answerIncomingCall(uuid)
```
- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`


### startCall

When you make an outgoing call, tell the device that a call is occurring. The argument list is slightly
different on iOS and Android:

iOS:
```js
RNCallKeep.startCall(uuid, handle, contactIdentifier, handleType, hasVideo);
```

Android:
```js
RNCallKeep.startCall(uuid, handle, contactIdentifier);
```

- `uuid`: string
  - An `uuid` that should be stored and re-used for `stopCall`.
- `handle`: string
  - Phone number of the callee
- `contactIdentifier`: string
  - The identifier is displayed in the native call UI, and is typically the name of the call recipient.
- `handleType`: string (optional, iOS only)
  - `generic`
  - `number` (default)
  - `email`
- `hasVideo`: boolean (optional, iOS only)
  - `false` (default)
  - `true` (you know... when not false)


### reportConnectingOutgoingCallWithUUID

_This feature is available only on iOS._

Tells CallKit that an outgoing call started connecting, for example when your server starts ringing the callee.

```js
RNCallKeep.reportConnectingOutgoingCallWithUUID(uuid);
```

- `uuid`: string
  - The `uuid` used for `startCall`

### reportConnectedOutgoingCallWithUUID

_This feature is available only on iOS._

Tells CallKit that the callee answered an outgoing call: the call screen shows it as connected. On Android, call [`setCurrentCallActive()`](#setcurrentcallactive) instead.

```js
RNCallKeep.reportConnectedOutgoingCallWithUUID(uuid);
```

- `uuid`: string
  - The `uuid` used for `startCall`

### updateDisplay
Use this to update the display after an outgoing call has started.

```js
RNCallKeep.updateDisplay(uuid, displayName, handle)
```
- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`
- `displayName`: string (optional)
  - Name of the caller to be displayed on the native UI
- `handle`: string
  - Phone number of the caller
- `options`: object (optional)
  - `ios`: object
    - `hasVideo`: boolean (optional)
    - `supportsHolding`: boolean (optional)
    - `supportsDTMF`: boolean (optional)
    - `supportsGrouping`: boolean (optional)
    - `supportsUngrouping`: boolean (optional)
  - `android`: object (currently no-op)

### endCall

When finish an incoming/outgoing call.  
(When user actively chooses to end the call from your app's UI.)

```js
RNCallKeep.endCall(uuid);
```

- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`

### endAllCalls

End all ongoing calls.

```js
RNCallKeep.endAllCalls();
```

### rejectCall

When you reject an incoming call.

```js
RNCallKeep.rejectCall(uuid);
```

- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`

### reportEndCallWithUUID

Report that the call ended without the user initiating.  
(Not ended by user, is usually due to the following reasons)


```js
RNCallKeep.reportEndCallWithUUID(uuid, reason);
```

- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`
- `reason`: int
  - Reason for the end call
    - Call failed: 1
    - Remote user ended call: 2
    - Remote user did not answer: 3
    - Call Answered elsewhere: 4
    - Call declined elsewhere: 5 (on Android this will map to Remote user ended call if you use the constants)
    - Missed: 6 (on iOS this will map to remote user ended call)
  - Access reasons as constants
  ```js
  import RNCallKeep, { CONSTANTS as CK_CONSTANTS } from 'react-native-callkeep';

  RNCallKeep.reportEndCallWithUUID(uuid, CK_CONSTANTS.END_CALL_REASONS.FAILED);
  ```

### setMutedCall

Switch the mic on/off.

```js
RNCallKeep.setMutedCall(uuid, true);
```

- `uuid`: string
  - uuid of the current call.
- `muted`: boolean

### setOnHold

Set a call on/off hold.

```js
RNCallKeep.setOnHold(uuid, true)
```

- `uuid`: string
  - uuid of the current call.
- `hold`: boolean

### sendDTMF

Plays DTMF tones on a call, for example from an in-app keypad. The library sends [`didPerformDTMFAction`](events.md#didperformdtmfaction) with the digits; your VoIP stack sends them to the remote side.

```js
RNCallKeep.sendDTMF(uuid, digits);
```

- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`
- `digits`: string
  - The DTMF digits to play

### setConnectionState

_This feature is available only on Android._

Change the state of the call

```js
RNCallKeep.setConnectionState(uuid, state)
```

- `uuid`: string
  - uuid of the current call.
- `state`: [See Connection.STATE_*](https://developer.android.com/reference/android/telecom/Connection#STATE_ACTIVE) documentation

### checkIfBusy

_This feature is available only on IOS._

Checks if there are any active calls on the device and returns a promise with a boolean value (`true` if there're active calls, `false` otherwise).

```js
RNCallKeep.checkIfBusy();
```

### checkSpeaker

_This feature is available only on IOS._

Checks if the device speaker is on and returns a promise with a boolean value (`true` if speaker is on, `false` otherwise).

```js
RNCallKeep.checkSpeaker();
```

### toggleAudioRouteSpeaker

_This feature is available only on Android._

Update the audio route of Audio Service on Android with a `routeSpeaker` boolean value (`true` if speaker need on, `false` otherwise).
When Phone call is active, Android control the audio via connection service. so this function help to toggle the audio to Speaker or wired/ear-piece or vice-versa 

Needs Android 8 (API 26) or later: on older versions it does nothing.

```js
RNCallKeep.toggleAudioRouteSpeaker(uuid, true);
```

- `uuid`: string
  - uuid of the current call.
- `routeSpeaker`: boolean

### getAudioRoutes

Get the list of available audio routes. i.e. bluetooth, wired/ear-piece, speaker and phone.

```js
await RNCallKeep.getAudioRoutes(): AudioRoute[];
```

```
type AudioRoute = {
    name: string,
    type: string,
    selected?: boolean // the route in use
}
```

### setAudioRoute

Set audio route using a route from `getAudioRoutes`.

```js
await RNCallKeep.setAudioRoute(uuid, routeName);
```

- `uuid`: string
  - uuid of the current call.
- `routeName`: String
  - AudioRoute.name. 

On Android, the promise is rejected when no call has this `uuid`, and before Android 8 (API 26), which has no API to change a call's audio route.

### supportConnectionService

_This feature is available only on Android._

Tells if `ConnectionService` is available on the device (returns a boolean).

```js
RNCallKeep.supportConnectionService();
```

### hasPhoneAccount

_This feature is available only on Android._

Checks if the user has enabled the [phone account](https://developer.android.com/reference/android/telecom/PhoneAccount) for your application.
A phone account must be enable to be able to display UI screen on incoming call and make outgoing calls from native Contact application.

Returns a promise of a boolean.

```js
await RNCallKeep.hasPhoneAccount();
```

### hasOutgoingCall

_This feature is available only on Android, useful when waking up the application for an outgoing call._

When waking up the Android application in background mode (eg: when the application is killed and the user make a call from the native Phone application).
The user can hang up the call before your application has been started in background mode, and you can lost the `RNCallKeepPerformEndCallAction` event.

To be sure that the outgoing call is still here, you can call `hasOutgoingCall` when you app waken up.


```js
const hasOutgoingCall = await RNCallKeep.hasOutgoingCall();
```

### hasDefaultPhoneAccount

_This feature is available only on Android._

Checks if the user has set a default [phone account](https://developer.android.com/reference/android/telecom/PhoneAccount).
If the user has not set a default they will be prompted to do so with an alert.

This is a workaround for an [issue](https://github.com/wazo-pbx/react-native-callkeep/issues/33) affecting some Samsung devices.

```js
const options = {
  alertTitle: 'Default not set',
  alertDescription: 'Please set the default phone account'
};

RNCallKeep.hasDefaultPhoneAccount(options);
```

### checkPhoneAccountEnabled

_This feature is available only on Android._

Checks if the user has set a default [phone account](https://developer.android.com/reference/android/telecom/PhoneAccount) and it's enabled.

It's useful for custom permission prompts. It should be used in pair with `registerPhoneAccount`
Similar to `hasDefaultPhoneAccount` but without trigering a prompt if the user doesn't have a phone account.


```js
RNCallKeep.checkPhoneAccountEnabled();
```

### isConnectionServiceAvailable

_This feature is available only on Android._

Check if the device support ConnectionService.


```js
RNCallKeep.checkPhoneAccountEnabled();
```

### backToForeground

_This feature is available only on Android._

Brings your app to the front, and opens it when it has no activity (for example from a headless task after the app was killed). Call it when your app should show a call, for example when the user answers from the system's call screen: Android lets the app come to the front then. Otherwise, Android 10 and later may block it while your app is in the background (see [restrictions on starting activities from the background](https://developer.android.com/guide/components/activities/background-starts)).

On a locked device, Android 8 and later ask the user to unlock (PIN, fingerprint...) before showing the app, as iOS does when the user opens the app from the call screen. To show a call screen over the lock screen instead, start that activity yourself, with `android:showWhenLocked="true"`, rather than calling `backToForeground()`.

```js
RNCallKeep.backToForeground();
```

### removeEventListener

Allows to remove the listener on an event.

```js
RNCallKeep.removeEventListener('checkReachability');
```

### registerPhoneAccount

Registers Android phone account manually, useful for custom permission prompts when you don't want to call `setup()`.
This method is called by `setup`, if you already use setup you don't need it.

_This feature is available only on Android._
_On iOS you still have to call `setup()`._

```js
RNCallKeep.registerPhoneAccount(options);
```

### registerAndroidEvents

Registers Android UI events, useful when you don't want to call `setup()`.
This method is called by `setup`, if you already use setup you don't need it.

_This feature is available only on Android._
_On iOS you still have to call `setup()`._

```js
RNCallKeep.registerAndroidEvents();
```

### unregisterAndroidEvents

_This feature is available only on Android._

The reverse of [`registerAndroidEvents()`](#registerandroidevents): the module stops sending events to JavaScript. It keeps the events that happen meanwhile, and sends them in a [`didLoadWithEvents`](events.md#didloadwithevents) event after the next `registerAndroidEvents()`.

```js
RNCallKeep.unregisterAndroidEvents();
```

## Constants

To make passing the right integer into methods easier, there are constants that are exported from the module.

```js
const CONSTANTS = {
  END_CALL_REASONS: {
    FAILED: 1,
    REMOTE_ENDED: 2,
    UNANSWERED: 3,
    ANSWERED_ELSEWHERE: 4,
    DECLINED_ELSEWHERE: 5,
    MISSED: 6
  }
};

import RNCallKeep, { CONSTANTS as CK_CONSTANTS } from 'react-native-callkeep';

console.log(CK_CONSTANTS.END_CALL_REASONS.FAILED) // outputs 1
```
