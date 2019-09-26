# React Native CallKeep

[![npm version](https://badge.fury.io/js/react-native-callkeep.svg)](https://badge.fury.io/js/react-native-callkeep)
[![npm downloads](https://img.shields.io/npm/dm/react-native-callkeep.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-callkeep.svg?maxAge=2592000)

**React Native CallKeep** utilises a brand new iOS 10 framework **CallKit** and Android **ConnectionService** to make the life easier for VoIP developers using React Native.

For more information about **CallKit** on iOS, please see [Official CallKit Framework Document](https://developer.apple.com/reference/callkit?language=objc) or [Introduction to CallKit by Xamarin](https://developer.xamarin.com/guides/ios/platform_features/introduction-to-ios10/callkit/)

For more information about **ConnectionService** on Android, please see [Android Documentation](https://developer.android.com/reference/android/telecom/ConnectionService) and [Build a calling app](https://developer.android.com/guide/topics/connectivity/telecom/selfManaged)

# Demo

A demo of `react-native-callkeep` is available in the [wazo-react-native-demo](https://github.com/wazo-pbx/wazo-react-native-demo) repository.

## Android
![Connection Service](docs/pictures/connection-service.jpg)

## iOS
![Connection Service](docs/pictures/call-kit.png)

# Installation

```sh
npm install --save react-native-callkeep
# or
yarn add react-native-callkeep
```

- [iOS](docs/ios-installation.md)
- [Android](docs/android-installation.md)

# Usage

## Setup

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
    additionalPermissions: [PermissionsAndroid.PERMISSIONS.example]
  }
};

RNCallKeep.setup(options);
```

- `options`: Object
  - `ios`: object
    - `appName`: string (required)
      It will be displayed on system UI when incoming calls received
    - `imageName`: string (optional)
      If provided, it will be displayed on system UI during the call
    - `ringtoneSound`: string (optional)
      If provided, it will be played when incoming calls received; the system will use the default ringtone if this is not provided
    - `maximumCallGroups`: string (optional)
      If provided, the maximum number of call groups supported by this application (Default: 3)
    - `maximumCallsPerCallGroup`: string (optional)
      If provided, the maximum number of calls in a single group, used for conferencing (Default: 1, no conferencing)
    - `supportsVideo`: boolean (optional)
      If provided, whether or not the application supports video calling (Default: true)
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

## Methods

### setAvailable
_This feature is available only on Android._

Tell _ConnectionService_ that the device is ready to make outgoing calls via the native Phone app.
If not the user will be stuck in the build UI screen without any actions.
Eg: Call it with `false` when disconnected from the sip client, when your token expires, when your user log out ...
Eg: When your used log out (or the connection to your server is broken, etc..), you have to call `setAvailable(false)` so CallKeep will refuse the call and your user will not be stuck in the native UI.

```js
RNCallKeep.setAvailable(true);
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

### displayIncomingCall

Display system UI for incoming calls

````js
RNCallKeep.displayIncomingCall(uuid, handle, localizedCallerName);
````

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

### answerIncomingCall
_This feature is available only on Android._

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

### endCall

When you finish an incoming/outgoing call.

```js
RNCallKeep.endCall(uuid);
```

- `uuid`: string
  - The `uuid` used for `startCall` or `displayIncomingCall`

### endAllCalls

End all ongoing connections.

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

Report that the call ended without the user initiating

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
  - `CXCallEndedReason` constants used for iOS. `DisconnectCause` used for Android.
  - Example enum for reasons
  ```js
  END_CALL_REASON = {
    failed: 1,
    remoteEnded: 2,
    unanswered: 3
  }
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

### endAllCalls

End all calls that have been started on the device.

```js
RNCallKeep.endAllCalls();
```

### checkIfBusy

Checks if there are any active calls on the device and returns a promise with a boolean value (`true` if there're active calls, `false` otherwise).
_This feature is available only on iOS._

```js
RNCallKeep.checkIfBusy();
```

### checkSpeaker

Checks if the device speaker is on and returns a promise with a boolean value (`true` if speaker is on, `false` otherwise).
_This feature is available only on iOS._

```js
RNCallKeep.checkSpeaker();
```

### supportConnectionService (async)

Tells if `ConnectionService` is available on the device (returns a boolean).

_This feature is available only on Android._

```js
RNCallKeep.supportConnectionService();
```

### hasPhoneAccount (async)

Checks if the user has enabled the [phone account](https://developer.android.com/reference/android/telecom/PhoneAccount) for your application.
A phone account must be enable to be able to display UI screen on incoming call and make outgoing calls from native Contact application.

Returns a promise of a boolean.

_This feature is available only on Android._

```js
await RNCallKeep.hasPhoneAccount();
```

### hasOutgoingCall (async)

_This feature is available only on Android, useful when waking up the application for an outgoing call._

When waking up the Android application in background mode (eg: when the application is killed and the user make a call from the native Phone application).
The user can hang up the call before your application has been started in background mode, and you can lost the `RNCallKeepPerformEndCallAction` event.

To be sure that the outgoing call is still here, you can call `hasOutgoingCall` when you app waken up.


```js
const hasOutgoingCall = await RNCallKeep.hasOutgoingCall();
```

### hasDefaultPhoneAccount

Checks if the user has set a default [phone account](https://developer.android.com/reference/android/telecom/PhoneAccount).
If the user has not set a default they will be prompted to do so with an alert.

This is a workaround for an [issue](https://github.com/wazo-pbx/react-native-callkeep/issues/33) affecting some Samsung devices.

_This feature is available only on Android._

```js
const options = {
  alertTitle: 'Default not set',
  alertDescription: 'Please set the default phone account'
};

RNCallKeep.hasDefaultPhoneAccount(options);
```


## Events

### didReceiveStartCallAction

Device sends this event once it decides the app is allowed to start a call, either from the built-in phone screens (iOS/_Recents_, Android/_Contact_),
or by the app calling `RNCallKeep.startCall`.

Try to start your app call action from here (e.g. get credentials of the user by `data.handle` and/or send INVITE to your SIP server)

Note: on iOS `callUUID` is not defined as the call is not yet managed by CallKit. You have to generate your own and call `startCall`.

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

### - answerCall

User answer the incoming call

```js
RNCallKeep.addEventListener('answerCall', ({ callUUID }) => {
  // Do your normal `Answering` actions here.
});
```

- `callUUID` (string)
  - The UUID of the call that is to be answered.

### - endCall

User finish the call.

```js
RNCallKeep.addEventListener('endCall', ({ callUUID }) => {
  // Do your normal `Hang Up` actions here
});
```

- `callUUID` (string)
  - The UUID of the call that is to be ended.

### - didActivateAudioSession

The `AudioSession` has been activated by **RNCallKeep**.

```js
RNCallKeep.addEventListener('didActivateAudioSession', () => {
  // you might want to do following things when receiving this event:
  // - Start playing ringback if it is an outgoing call
});
```

### - didDisplayIncomingCall

Callback for `RNCallKeep.displayIncomingCall`

```js
RNCallKeep.addEventListener('didDisplayIncomingCall', ({ error, uuid, handle, localizedCallerName, fromPushKit }) => {
  // you might want to do following things when receiving this event:
  // - Start playing ringback if it is an outgoing call
});
```

- `error` (string)
  - iOS only.

### - didPerformSetMutedCallAction

A call was muted by the system or the user:

```js
RNCallKeep.addEventListener('didPerformSetMutedCallAction', ({ muted, callUUID }) => {

});
```

- `muted` (boolean)
- `callUUID` (string)
  - The UUID of the call.

### - didToggleHoldCallAction

A call was held or unheld by the current user

```js
RNCallKeep.addEventListener('didToggleHoldCallAction', ({ hold, callUUID }) => {

});
```

- `hold` (boolean)
- `callUUID` (string)
  - The UUID of the call.

### - didPerformDTMFAction

Used type a number on his dialer

```js
RNCallKeep.addEventListener('didPerformDTMFAction', ({ digits, callUUID }) => {

});
```

- `digits` (string)
  - The digits that emit the dtmf tone
- `callUUID` (string)
  - The UUID of the call.
  
### - checkReachability

On Android when the application is in background, after a certain delay the OS will close every connection with informing about it.
So we have to check if the application is reachable before making a call from the native phone application.

```js
RNCallKeep.addEventListener('checkReachability', () => {
  RNCallKeep.setReachable();
});
```

## Example

A full example is available in the [example](https://github.com/react-native-webrtc/react-native-callkeep/tree/master/example) folder.

```javascript
import React from 'react';
import RNCallKeep from 'react-native-callkeep';
import uuid from 'uuid';

class RNCallKeepExample extends React.Component {
  constructor(props) {
    super(props);

    this.currentCallId = null;

    // Add RNCallKeep Events
    RNCallKeep.addEventListener('didReceiveStartCallAction', this.didReceiveStartCallAction);
    RNCallKeep.addEventListener('answerCall', this.onAnswerCallAction);
    RNCallKeep.addEventListener('endCall', this.onEndCallAction);
    RNCallKeep.addEventListener('didDisplayIncomingCall', this.onIncomingCallDisplayed);
    RNCallKeep.addEventListener('didPerformSetMutedCallAction', this.onToggleMute);
    RNCallKeep.addEventListener('didToggleHoldCallAction', this.onToggleHold);
    RNCallKeep.addEventListener('didPerformDTMFAction', this.onDTMFAction);
    RNCallKeep.addEventListener('didActivateAudioSession', this.audioSessionActivated);
  }

  // Initialise RNCallKeep
  setup = () => {
    const options = {
      ios: {
        appName: 'ReactNativeWazoDemo',
        imageName: 'sim_icon',
        supportsVideo: false,
        maximumCallGroups: '1',
        maximumCallsPerCallGroup: '1'
      },
      android: {
        alertTitle: 'Permissions Required',
        alertDescription:
          'This application needs to access your phone calling accounts to make calls',
        cancelButton: 'Cancel',
        okButton: 'ok',
        imageName: 'sim_icon',
        additionalPermissions: [PermissionsAndroid.PERMISSIONS.READ_CONTACTS]
      }
    };

    try {
      RNCallKeep.setup(options);
      RNCallKeep.setAvailable(true); // Only used for Android, see doc above.
    } catch (err) {
      console.error('initializeCallKeep error:', err.message);
    }
  }

  // Use startCall to ask the system to start a call - Initiate an outgoing call from this point
  startCall = ({ handle, localizedCallerName }) => {
    // Your normal start call action
    RNCallKeep.startCall(this.getCurrentCallId(), handle, localizedCallerName);
  };

  reportEndCallWithUUID = (callUUID, reason) => {
    RNCallKeep.reportEndCallWithUUID(callUUID, reason);
  }

  // Event Listener Callbacks

  didReceiveStartCallAction(data) => {
    let { handle, callUUID, name } = data;
    // Get this event after the system decides you can start a call
    // You can now start a call from within your app
  };

  onAnswerCallAction = (data) => {
    let { callUUID } = data;
    // Called when the user answers an incoming call
  };

  onEndCallAction = (data) => {
    let { callUUID } = data;
    RNCallKeep.endCall(this.getCurrentCallId());

    this.currentCallId = null;
  };

  // Currently iOS only
  onIncomingCallDisplayed = (data) => {
    let { error } = data;
    // You will get this event after RNCallKeep finishes showing incoming call UI
    // You can check if there was an error while displaying
  };

  onToggleMute = (data) => {
    let { muted, callUUID } = data;
    // Called when the system or user mutes a call
  };

  onToggleHold = (data) => {
    let { hold, callUUID } = data;
    // Called when the system or user holds a call
  };

  onDTMFAction = (data) => {
    let { digits, callUUID } = data;
    // Called when the system or user performs a DTMF action
  };

  audioSessionActivated = (data) => {
    // you might want to do following things when receiving this event:
    // - Start playing ringback if it is an outgoing call
  };

  getCurrentCallId = () => {
    if (!this.currentCallId) {
      this.currentCallId = uuid.v4();
    }

    return this.currentCallId;
  };

  render() {
  }
}
```

## Receiving a call when the application is not reachable.

In some case your application can be unreachable :
- when the user kill the application 
- when it's in background since a long time (eg: after ~5mn the os will kill all connections).

To be able to wake up your application to display the incoming call, you can use [https://github.com/ianlin/react-native-voip-push-notification](react-native-voip-push-notification) on iOS or BackgroundMessaging from [react-native-firebase](https://rnfirebase.io/docs/v5.x.x/messaging/receiving-messages#4)-(Optional)(Android-only)-Listen-for-FCM-messages-in-the-background).

You have to send a push to your application, like with Firebase for Android and with a library supporting PushKit pushes for iOS.

### PushKit

Since iOS 13, you'll have to report the incoming calls that wakes up your application with a VoIP push. Add this in your `AppDelegate.m` if you're using VoIP pushes to wake up your application :

```objective-c
- (void)pushRegistry:(PKPushRegistry *)registry didReceiveIncomingPushWithPayload:(PKPushPayload *)payload forType:(PKPushType)type withCompletionHandler:(void (^)(void))completion {
  // Process the received push
  [RNVoipPushNotificationManager didReceiveIncomingPushWithPayload:payload forType:(NSString *)type];
  
  // Retrieve information like handle and callerName here
  // NSString *uuid = /* fetch for payload or ... */ [[[NSUUID UUID] UUIDString] lowercaseString];
  // NSString *callerName = @"caller name here";
  // NSString *handle = @"caller number here";
  
  [RNCallKeep reportNewIncomingCall:uuid handle:handle handleType:@"generic" hasVideo:false localizedCallerName:callerName fromPushKit: YES];

  completion();
}
```

## Debug

### Android

```
adb logcat *:S RNCallKeepModule:V
```

## Contributing

Any pull request, issue report and suggestion are highly welcome!

## License

This work is dual-licensed under ISC and MIT.
Previous work done by @ianlin on iOS is on ISC Licence.
We choose MIT for the rest of the project.

`SPDX-License-Identifier: ISC OR MIT`
