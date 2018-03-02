# React Native CallKit - iOS >= 10.0 only

[![npm version](https://badge.fury.io/js/react-native-callkit.svg)](https://badge.fury.io/js/react-native-callkit)
[![npm downloads](https://img.shields.io/npm/dm/react-native-callkit.svg?maxAge=2592000)](https://img.shields.io/npm/dm/react-native-callkit.svg?maxAge=2592000)

**React Native CallKit** utilises a brand new iOS 10 framework **CallKit** to make the life easier for VoIP developers using React Native.

For more information about **CallKit**, please see [Official CallKit Framework Document][1] or [Introduction to CallKit by Xamarin][2]

**Note**: Since CallKit is quite new, this module will be updated frequently so be careful with the version you are using.

## Version

Use version >= **1.1.0** if you're using react native >= 0.40

## Installation (without CocoaPods)

### NPM module

```bash
npm install --save react-native-callkit
```

### Link Library

```bash
rnpm link react-native-callkit
```

## Installation (with CocoaPods)

### NPM module

```bash
npm install --save react-native-callkit
```

### CocaPods
```bash
cd ios
pod install
```

## Installation common steps

### Info.plist

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

#### - Import Library

```obj-c
#import "RNCallKit.h"
```

#### - Change the way you initialise React Root View (required if <= 1.2.1)

Initialise **RNCallKit** first, since we need our custom `observer` of `NSNotificationCenter` to be started as soon as the app is initialising

```diff

// This is how you normally initialise React Root View, delete it
-RCTRootView *rootView = [[RCTRootView alloc] initWithBundleURL:jsCodeLocation
-                                                    moduleName:@"MyApp"
-                                             initialProperties:nil
-                                                 launchOptions:launchOptions];

// Initialise RNCallKit
+RNCallKit *rncallkit = [[RNCallKit alloc] init];

// Initialise React Bridge with RNCallKit
+RCTBridge *bridge = [[RCTBridge alloc] initWithBundleURL:jsCodeLocation
+                                          moduleProvider:^{ return @[rncallkit]; }
+                                           launchOptions:launchOptions];

// Initialise React Root View with React Bridge you've just created
+RCTRootView *rootView = [[RCTRootView alloc] initWithBridge:bridge
+                                                 moduleName:@"MyApp"
+                                          initialProperties:nil];
```

#### - Handling User Activity

This delegate will be called when the user tries to start a call from native Phone App

```obj-c

- (BOOL)application:(UIApplication *)application
continueUserActivity:(NSUserActivity *)userActivity
  restorationHandler:(void(^)(NSArray * __nullable restorableObjects))restorationHandler
{
  return [RNCallKit application:application
           continueUserActivity:userActivity
             restorationHandler:restorationHandler];
}


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

Initialise RNCallKit with options

### displayIncomingCall

- **uuid**: string
- **handle**: string
- **handleType**: string (optional)
  - generic
  - number (default)
  - email
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

After all works are done, remember to call `RNCallKit.startCall(uuid, calleeNumber)`

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

The `AudioSession` has been activated by **RNCallKit**, you might want to do following things when receiving this event:

- Start playing ringback if it is an outgoing call

## Usage

```javascript
import React from 'react';
import RNCallKit from 'react-native-callkit';

import uuid from 'uuid';

class RNCallKitExample extends React.Component {
  constructor(props) {

    // Initialise RNCallKit
    let options = {
        appName: 'RNCallKitExample',
        imageName: 'my_image_name_in_bundle',
        ringtoneSound: 'my_ringtone_sound_filename_in_bundle',
    };
    try {
        RNCallKit.setup(options);
    } catch (err) {
        console.log('error:', err.message);
    }

    // Add RNCallKit Events
    RNCallKit.addEventListener('didReceiveStartCallAction', this.onRNCallKitDidReceiveStartCallAction);
    RNCallKit.addEventListener('answerCall', this.onRNCallKitPerformAnswerCallAction);
    RNCallKit.addEventListener('endCall', this.onRNCallKitPerformEndCallAction);
    RNCallKit.addEventListener('didActivateAudioSession', this.onRNCallKitDidActivateAudioSession);
  }

  onRNCallKitDidReceiveStartCallAction(data) {
    /*
     * Your normal start call action
     *
     * ...
     *
     */

    let _uuid = uuid.v4();
    RNCallKit.startCall(_uuid, data.handle);
  }

  onRNCallKitPerformAnswerCallAction(data) {
    /* You will get this event when the user answer the incoming call
     *
     * Try to do your normal Answering actions here
     *
     * e.g. this.handleAnswerCall(data.callUUID);
     */
  }

  onRNCallKitPerformEndCallAction(data) {
    /* You will get this event when the user finish the incoming/outgoing call
     *
     * Try to do your normal Hang Up actions here
     *
     * e.g. this.handleHangUpCall(data.callUUID);
     */
  }

  onRNCallKitDidActivateAudioSession(data) {
    /* You will get this event when the the AudioSession has been activated by **RNCallKit**,
     * you might want to do following things when receiving this event:
     *
     * - Start playing ringback if it is an outgoing call
     */
  }

  // This is a fake function where you can receive incoming call notifications
  onIncomingCall() {
    // Store the generated uuid somewhere
    // You will need this when calling RNCallKit.endCall()
    let _uuid = uuid.v4();
    RNCallKit.displayIncomingCall(_uuid, "886900000000")
  }

  // This is a fake function where you make outgoing calls
  onOutgoingCall() {
    // Store the generated uuid somewhere
    // You will need this when calling RNCallKit.endCall()
    let _uuid = uuid.v4();
    RNCallKit.startCall(_uuid, "886900000000")
  }

  // This is a fake function where you hang up calls
  onHangUpCall() {
    // get the _uuid you stored earlier
    RNCallKit.endCall(_uuid)
  }

  render() {
  }
}

```

## Contributing

Any pull request, issue report and suggestion are highly welcome!

## License

[ISC License][3] (functionality equivalent to **MIT License**)

[1]: https://developer.apple.com/reference/callkit?language=objc
[2]: https://developer.xamarin.com/guides/ios/platform_features/introduction-to-ios10/callkit/
[3]: https://opensource.org/licenses/ISC
[4]: https://github.com/zxcpoiu/react-native-incall-manager
