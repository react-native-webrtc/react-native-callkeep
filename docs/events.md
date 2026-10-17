# Events

Listen to an event with `addEventListener`, which returns a subscription:

```js
const subscription = RNCallKeep.addEventListener('answerCall', ({ callUUID }) => {
  // ...
});

subscription.remove();
```

| Event                                                             |  iOS | Android |
| ----------------------------------------------------------------- | :--: | :-----: |
| [didReceiveStartCallAction](#didreceivestartcallaction)         |  ✅  |   ✅    |
| [answerCall](#answercall)                                       |  ✅  |   ✅    |
| [endCall](#endcall)                                             |  ✅  |   ✅    |
| [didActivateAudioSession](#didactivateaudiosession)             |  ✅  |   ✅    |
| [didDeactivateAudioSession](#diddeactivateaudiosession)         |  ✅  |   ❌    |
| [didDisplayIncomingCall](#diddisplayincomingcall)               |  ✅  |   ✅    |
| [didPerformSetMutedCallAction](#didperformsetmutedcallaction)   |  ✅  |   ✅    |
| [didToggleHoldCallAction](#didtoggleholdcallaction)             |  ✅  |   ✅    |
| [didPerformDTMFAction](#didperformdtmfaction)                   |  ✅  |   ✅    |
| [didResetProvider](#didresetprovider)                           |  ✅  |   ❌    |
| [didLoadWithEvents](#didloadwithevents)                         |  ✅  |   ❌    |
| [showIncomingCallUi](#showincomingcallui)                       |  ❌  |   ✅    |
| [silenceIncomingCall](#silenceincomingcall)                     |  ❌  |   ✅    |
| [createIncomingConnectionFailed](#createincomingconnectionfailed) |  ❌  |   ✅    |
| [checkReachability](#checkreachability)                         |  ❌  |   ✅    |
| [didChangeAudioRoute](#didchangeaudioroute)                     |  ✅  |   ✅    |
| [onHasActiveCall](#onhasactivecall)                             |  ❌  |   ✅    |

### didReceiveStartCallAction

Device sends this event once it decides the app is allowed to start a call, either from the built-in phone screens (iOS/_Recents_, Android/_Contact_),
or by the app calling `RNCallKeep.startCall`.

Try to start your app call action from here (e.g. get credentials of the user by `data.handle` and/or send INVITE to your SIP server)

Note: on iOS, when the call comes from Recents, Contacts or Siri, `callUUID` is not defined: CallKit doesn't manage the call yet. Generate a UUID and call `startCall`, which sends this event again with the `callUUID`.

```js
RNCallKeep.addEventListener('didReceiveStartCallAction', ({ handle, callUUID, name }) => {

});
```

- `handle` (string)
  - Phone number of the callee
- `callUUID` (string)
  - The UUID of the call that is to be answered
- `name` (string)
  - Name of the callee

### answerCall

User answer the incoming call

```js
RNCallKeep.addEventListener('answerCall', ({ callUUID }) => {
  // Do your normal `Answering` actions here.
});
```

- `callUUID` (string)
  - The UUID of the call that is to be answered.

### endCall

User finish the call.

```js
RNCallKeep.addEventListener('endCall', ({ callUUID }) => {
  // Do your normal `Hang Up` actions here
});
```

- `callUUID` (string)
  - The UUID of the call that is to be ended.

### didActivateAudioSession

The `AudioSession` has been activated by **RNCallKeep**.

```js
RNCallKeep.addEventListener('didActivateAudioSession', () => {
  // you might want to do following things when receiving this event:
  // - Start playing ringback if it is an outgoing call
});
```

### didDeactivateAudioSession

iOS only.

The `AudioSession` has been deactivated by **RNCallKeep**.

```js
RNCallKeep.addEventListener('didDeactivateAudioSession', () => {

});
```

### didDisplayIncomingCall

Sent for an incoming call reported with `displayIncomingCall`, or from native code with `reportNewIncomingCall` (for example when a push arrives):

- On iOS, once CallKit has accepted or refused the call. `error` is set when it refused.
- On Android, once Telecom has created the call, so methods that act on the call work from this event. If Android refuses the call, you get [`createIncomingConnectionFailed`](#createincomingconnectionfailed) instead.

```js
RNCallKeep.addEventListener('didDisplayIncomingCall', ({ error, callUUID, handle, localizedCallerName, hasVideo, fromPushKit, payload }) => {
  // you might want to do following things when receiving this event:
  // - Start playing ringback if it is an outgoing call
});
```

- `error` (string)
  - iOS only.
- `errorCode` (string)
  - iOS only. Possible values: "Unentitled", "CallUUIDAlreadyExists", "FilteredByDoNotDisturb", "FilteredByBlockList", "Unknown". See https://developer.apple.com/documentation/callkit/cxerrorcodeincomingcallerror for more information.
- `callUUID` (string)
  - The UUID of the call.
- `handle` (string)
  - Phone number of the caller
- `localizedCallerName` (string)
  - Name of the caller to be displayed on the native UI
- `name` (string)
  - Android only. The same as `localizedCallerName`, for apps written when only `reportNewIncomingCall` sent this event.
- `hasVideo` (string)
  - `1` (video enabled)
  - `0` (video not enabled)
- `fromPushKit` (string)
  - `1` (call triggered from PushKit)
  - `0` (call not triggered from PushKit, and always on Android)
- `payload` (object or string)
  - iOS: the VoIP push payload passed to `reportNewIncomingCall`.
  - Android: the payload passed to the native `reportNewIncomingCall` (a string) or `displayIncomingCall` (a `Bundle`, sent as an object). Absent when there is none.

### didPerformSetMutedCallAction

A call was muted by the system or the user:

```js
RNCallKeep.addEventListener('didPerformSetMutedCallAction', ({ muted, callUUID }) => {

});
```

- `muted` (boolean)
- `callUUID` (string)
  - The UUID of the call.

### didToggleHoldCallAction

A call was held or unheld by the current user

```js
RNCallKeep.addEventListener('didToggleHoldCallAction', ({ hold, callUUID }) => {

});
```

- `hold` (boolean)
- `callUUID` (string)
  - The UUID of the call.

### didChangeAudioRoute

Triggered when the audio route has been changed.

```js
RNCallKeep.addEventListener('didChangeAudioRoute', ({ output }) => {

});
```

- `output` (string) ⚠️ Will send `Speaker` on iOS but `SPEAKER` on Android.
- `reason` (number, iOS only) See case's in https://developer.apple.com/documentation/avfaudio/avaudiosession/routechangereason
- `handle` (string, Android only) Phone number of the incoming caller
- `callUUID` (string, Android only) The UUID of the call

### didPerformDTMFAction

Used type a number on his dialer

```js
RNCallKeep.addEventListener('didPerformDTMFAction', ({ digits, callUUID }) => {

});
```

- `digits` (string)
  - The digits that emit the dtmf tone
- `callUUID` (string)
  - The UUID of the call.

### didResetProvider

iOS only.

The underlying native provider for RNCallKeep has been reset.

```js
RNCallKeep.addEventListener('didResetProvider', () => {
  // Likely an error has occurred, you may want to hang up all calls
  // and log errors.
});
```

### didLoadWithEvents

iOS only.

Called as soon as JS context initializes if there were some actions performed by user before JS context has been created.

Since iOS 13, you must display incoming call on receiving PushKit push notification. But if app was killed, it takes some time to create JS context. If user answers the call (or ends it) before JS context has been initialized, user actions will be passed as events array of this event. Similar situation can happen if user would like to start a call from Recents or similar iOS app, assuming that your app was in killed state.

In order for this event to reliably fire, it's necessary to perform setup in `AppDelegate.m`

**NOTE: You still need to subscribe / handle the rest events as usuall. This is just a helper whcih cache and propagate early fired events if and only if for "the native events which DID fire BEFORE js bridge is initialed", it does NOT mean this will have events each time when the app reopened.**

```js
// register `didLoadWithEvents` somewhere early in your app when it is ready to handle callkeep events.

RNCallKeep.addEventListener('didLoadWithEvents', (events) => {
  // `events` is passed as an Array chronologically, handle or ignore events based on the app's logic
  // see example usage in https://github.com/react-native-webrtc/react-native-callkeep/pull/169 or https://github.com/react-native-webrtc/react-native-callkeep/pull/205
});
```

- `events` Array
  - `name`: string
    Native event name like: `RNCallKeepPerformAnswerCallAction`
  - `data`: object
    Object with data passed together with specific event so it can be handled in the same way like original event, for example `({ callUUID })` for `answerCall` event if `name` is `RNCallKeepPerformAnswerCallAction`

### showIncomingCallUi

_Android only. Self Managed only._

Only when CallKeep is setup to be in self managed mode. Signals that the app must show an incoming call UI. The implementor must either call `displayIncomingCall` from react native or native android code to make this event fire.

```js
RNCallKeep.addEventListener('showIncomingCallUi', ({ handle, callUUID, name }) => {

});
```

The following values will match those initially passed to `displayIncomingCall`

- `handle` (string)
  - Phone number of the incoming caller.
- `callUUID` (string)
  - The UUID of the call.
- `name` (string)
  - Caller Name.

### silenceIncomingCall

_Android only. Self Managed only._

Corresponds to the native [onSilence event](https://developer.android.com/reference/android/telecom/Connection#onSilence()). The implementor should silence the corresponding incoming calls notification sound when and if this event is fired.

```js
RNCallKeep.addEventListener('silenceIncomingCall', ({ handle, callUUID, name }) => {

});
```

The following values will match those initially passed to `silenceIncomingCall`

- `handle` (string)
  - Phone number of the incoming caller.
- `callUUID` (string)
  - The UUID of the call.
- `name` (string)
  - Caller Name.

### createIncomingConnectionFailed

_Android only. Self Managed only._

Corresponds to the native [onCreateIncomingConnectionFailed callback](https://developer.android.com/reference/android/telecom/ConnectionService#onCreateIncomingConnectionFailed(android.telecom.PhoneAccountHandle,%20android.telecom.ConnectionRequest)). The implementor should reject the incoming SIP INVITE with an appropriate status code, such as 483 User Busy. Android unfortunately does not provide the exact reason for refusing to let you accept an incoming call, but they do list a set of reasons [here](https://developer.android.com/guide/topics/connectivity/telecom/selfManaged#constraints)

```js
RNCallKeep.addEventListener('createIncomingConnectionFailed', ({ handle, callUUID, name }) => {

});
```

The following values will match those initially passed to `silenceIncomingCall`

- `handle` (string)
  - Phone number of the incoming caller.
- `callUUID` (string)
  - The UUID of the call.
- `name` (string)
  - Caller Name.

### checkReachability

_Android only._

On Android when the application is in background, after a certain delay the OS will close every connection with informing about it.
So we have to check if the application is reachable before making a call from the native phone application.

```js
RNCallKeep.addEventListener('checkReachability', () => {
  RNCallKeep.setReachable();
});

```

### onHasActiveCall

_Android only._

A listener that tells the JS side if a native call has been answered while there was an active self-managed call

```js
RNCallKeep.addEventListener('onHasActiveCall', () => {
  // eg: End active app call if native call is answered
});

```
