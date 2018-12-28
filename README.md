# React Native CallKeep

[![npm version](https://badge.fury.io/js/react-native-callkeep.svg)](https://badge.fury.io/js/react-native-callkeep)
[![npm downloads](https://img.shields.io/npm/dm/react-native-callkeep.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-callkeep.svg?maxAge=2592000)

**React Native CallKit** utilises a brand new iOS 10 framework **CallKit** and Android **ConnectionService** to make the life easier for VoIP developers using React Native.

For more information about **CallKit** on iOS, please see [Official CallKit Framework Document](https://developer.apple.com/reference/callkit?language=objc) or [Introduction to CallKit by Xamarin](https://developer.xamarin.com/guides/ios/platform_features/introduction-to-ios10/callkit/)

For more information about **ConnectionService** on Android, please see [Android Documentation](https://developer.android.com/reference/android/telecom/ConnectionService) and [Build a calling app](https://developer.android.com/guide/topics/connectivity/telecom/selfManaged)


# Installation

```sh
npm install --save react-native-callkeep
# or
yarn add react-native-callkeep
```

## Automatic linking

```sh
react-native link react-native-callkeep
```

### IOS (with CocoaPods)

Include in a Podfile in your react-native ios directory:

```
pod 'react-native-callkeep', :path => '../node_modules/react-native-callkeep'
```

Then:
```bash
cd ios
pod install
```

## Manual linking

### Android

1. In `android/app/build.gradle`
Should have a line `compile project(':react-native-callkeep')` in `dependencies {}` section.

2. In `android/settings.gradle`
Should have:

```java
include ':react-native-callkeep'
project(':react-native-callkeep').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-callkeep/android')
```

3. In `MainApplication.java`:

```java
import io.wazo.callkeep.RNCallKeepPackage;

private static List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new RNCallKeepPackage() // Add this line
    );
}
```

4. Add permissionResult listener in `MainActivity.java`:

```java
import io.wazo.callkeep.RNCallKeepModule;

public class MainActivity extends ReactActivity {
    // ...
    
    // Permission results
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case RNCallKeepModule.REQUEST_READ_PHONE_STATE:
                RNCallKeepModule.onRequestPermissionsResult(grantResults);
                break;
        }
    }
}
```

### iOS

1. Drag `node_modules/react-native-callkeep/ios/RNCallKeep.xcodeproj` under `<your_xcode_project>/Libraries`.

2. Select <your_xcode_project> --> Build Phases --> Link Binary With Libraries.
Drag `Libraries/RNCallKeep.xcodeproj/Products/libRNCallKeep.a` to Link Binary With Libraries.

3. Select <your_xcode_project> --> Build Settings
In `Header Search Paths`, add `$(SRCROOT)/../node_modules/react-native-callkeep/ios/RNCallKeep`.


## iOS installation common steps

### Info.plist (iOS)

Add `voip` under `UIBackgroundModes`

Note that it must be done via editing `Info.plist` as in Xcode 9 there is no `voip` option in `Capabilities`.

```
<key>UIBackgroundModes</key>
<array>
  <string>voip</string>
</array>
```

### Add Frameworks

In `Xcode` -> `Build Phases` -> `Link Binary With Libraries`, add `CallKit.framework` and `Intents.framework` with `Optional` status

### AppDelegate.m

#### Import Library

```obj-c
#import "RNCallKeep.h"
```

#### Handling User Activity

This delegate will be called when the user tries to start a call from native Phone App

```obj-c
- (BOOL)application:(UIApplication *)application
continueUserActivity:(NSUserActivity *)userActivity
  restorationHandler:(void(^)(NSArray * __nullable restorableObjects))restorationHandler
{
  return [RNCallKeep application:application
           continueUserActivity:userActivity
             restorationHandler:restorationHandler];
}
```

## Android common step installation

1. In `android/app/src/main/AndroidManifest.xml` add these permissions:


```xml
<uses-permission android:name="android.permission.BIND_TELECOM_CONNECTION_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />

<application> 
    // ...
    <service android:name="io.wazo.callkeep.VoiceConnectionService"
        android:label="Wazo"
        android:permission="android.permission.BIND_TELECOM_CONNECTION_SERVICE">
        <intent-filter>
            <action android:name="android.telecom.ConnectionService" />
        </intent-filter>
    </service>
    // ....
</application>
```

## API

### setup

- **options**: object
  - **appName**: string (required)
    - It will be displayed on system UI when incoming calls received
  - **imageName**: string (optional)
    - If provided, it will be displayed on system UI during the call
  - **ringtoneSound**: string (optional)
    - If provided, it will be played when incoming calls received; the system will use the default ringtone if this is not provided

Initialise RNCallKeep with options

### displayIncomingCall

- **uuid**: string
- **handle**: string
- **handleType**: string (optional)
  - generic
  - number (default)
  - email
- **hasVideo**: boolean (optional)
  - false (default)
- **localizedCallerName**: string (optional)

Call when you receive incoming calls to display system UI

### startCall

- **uuid**: string
- **handle**: string
- **handleType**: string (optional)
  - generic
  - number (default)
  - email
- **contactIdentifier**: string (optional)
  - The identifier is displayed in the native call UI, and is typically the name of the call recipient.

Call when you make an outgoing call

### endCall

- **uuid**: string

Call when you finish an incoming/outgoing call

### setMutedCall

- **uuid**: string
- **muted**: boolean

Switch the mic on/off

### checkIfBusy

Checks if there are any active calls on the device and returns a promise with a boolean value (`true` if there're active calls, `false` otherwise).

### checkSpeaker

Checks if the device speaker is on and returns a promise with a boolean value (`true` if speaker is on, `false` otherwise).

## Events

### - didReceiveStartCallAction

**data**:

```javascript
{
  handle: '886900000000' // The number/name got from Recents in built-in Phone app
}
```

User start call action from **Recents** in built-in **Phone** app

Try to start your call action from here (e.g. get credentials of the user by `data.handle` and/or send INVITE to your SIP server)

After all works are done, remember to call `RNCallKeep.startCall(uuid, calleeNumber)`

### - answerCall

User answer the incoming call

Do your normal `Answering` actions here

**data**:

```javascript
{
  callUUID: 'f0ee907b-6dbd-45a8-858a-903decb198f8' // The UUID of the call that is to be answered
}
```

### - endCall

User finish the call

Do your normal `Hang Up` actions here

**data**:

```javascript
{
  callUUID: 'f0ee907b-6dbd-45a8-858a-903decb198f8' // The UUID of the call that is to be hung
}
```

### - didActivateAudioSession

The `AudioSession` has been activated by **RNCallKeep**, you might want to do following things when receiving this event:

- Start playing ringback if it is an outgoing call

### - didDisplayIncomingCall

Callback for `RNCallKeep.displayIncomingCall`

**error**: string (optional)

### - didPerformSetMutedCallAction

A call was muted by the system or the user:

**muted**: boolean

## Usage

```javascript
import React from 'react';
import RNCallKeep from 'react-native-callkeep';

import uuid from 'uuid';

class RNCallKeepExample extends React.Component {
  constructor(props) {

    // Initialise RNCallKeep
    let options = {
        appName: 'RNCallKeepExample',
        imageName: 'my_image_name_in_bundle',
        ringtoneSound: 'my_ringtone_sound_filename_in_bundle',
    };
    try {
        RNCallKeep.setup(options);
    } catch (err) {
        console.log('error:', err.message);
    }

    // Add RNCallKeep Events
    RNCallKeep.addEventListener('didReceiveStartCallAction', this.onRNCallKeepDidReceiveStartCallAction);
    RNCallKeep.addEventListener('answerCall', this.onRNCallKeepPerformAnswerCallAction);
    RNCallKeep.addEventListener('endCall', this.onRNCallKeepPerformEndCallAction);
    RNCallKeep.addEventListener('didActivateAudioSession', this.onRNCallKeepDidActivateAudioSession);
    RNCallKeep.addEventListener('didDisplayIncomingCall', this.onRNCallKeepDidDisplayIncomingCall);
    RNCallKeep.addEventListener('didPerformSetMutedCallAction', this.onRNCallKeepDidPerformSetMutedCallAction);
  }

  onRNCallKeepDidReceiveStartCallAction(data) {
    /*
     * Your normal start call action
     *
     * ...
     *
     */

    let _uuid = uuid.v4();
    RNCallKeep.startCall(_uuid, data.handle);
  }

  onRNCallKeepPerformAnswerCallAction(data) {
    /* You will get this event when the user answer the incoming call
     *
     * Try to do your normal Answering actions here
     *
     * e.g. this.handleAnswerCall(data.callUUID);
     */
  }

  onRNCallKeepPerformEndCallAction(data) {
    /* You will get this event when the user finish the incoming/outgoing call
     *
     * Try to do your normal Hang Up actions here
     *
     * e.g. this.handleHangUpCall(data.callUUID);
     */
  }

  onRNCallKeepDidActivateAudioSession(data) {
    /* You will get this event when the the AudioSession has been activated by **RNCallKeep**,
     * you might want to do following things when receiving this event:
     *
     * - Start playing ringback if it is an outgoing call
     */
  }

  onRNCallKeepDidDisplayIncomingCall(error) {
    /* You will get this event after RNCallKeep finishes showing incoming call UI
     * You can check if there was an error while displaying
     */
  }

  onRNCallKeepDidPerformSetMutedCallAction(muted) {
    /* You will get this event after the system or the user mutes a call
     * You can use it to toggle the mic on your custom call UI
     */
  }

  // This is a fake function where you can receive incoming call notifications
  onIncomingCall() {
    // Store the generated uuid somewhere
    // You will need this when calling RNCallKeep.endCall()
    let _uuid = uuid.v4();
    RNCallKeep.displayIncomingCall(_uuid, "886900000000")
  }

  // This is a fake function where you make outgoing calls
  onOutgoingCall() {
    // Store the generated uuid somewhere
    // You will need this when calling RNCallKeep.endCall()
    let _uuid = uuid.v4();
    RNCallKeep.startCall(_uuid, "886900000000")
  }

  // This is a fake function where you hang up calls
  onHangUpCall() {
    // get the _uuid you stored earlier
    RNCallKeep.endCall(_uuid)
  }

  render() {
  }
}

```

## Contributing

Any pull request, issue report and suggestion are highly welcome!

## License

[ISC License][3] (functionality equivalent to **MIT License**)
