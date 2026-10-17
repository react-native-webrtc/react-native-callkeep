# Android installation

## Install

```sh
npm install react-native-callkeep
```

Autolinking adds the library: there is nothing to link by hand, and no code to add to `MainActivity` or `MainApplication`. The library's own manifest adds `CALL_PHONE`, `READ_PHONE_NUMBERS`, `MANAGE_OWN_CALLS` and, up to Android 10, `READ_PHONE_STATE`.

## Manifest

In `android/app/src/main/AndroidManifest.xml`, add the permissions and the connection service:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
  <!-- The microphone for the call, and the foreground service that keeps it in the background. -->
  <uses-permission android:name="android.permission.RECORD_AUDIO" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
  <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
  <!-- Android 13+: the foreground service's notification. -->
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

  <application>
    <!-- The ConnectionService Telecom talks to. The permission makes sure only the system binds to it. -->
    <service
      android:name="io.wazo.callkeep.VoiceConnectionService"
      android:label="Your app"
      android:permission="android.permission.BIND_TELECOM_CONNECTION_SERVICE"
      android:foregroundServiceType="phoneCall|microphone"
      android:exported="true">
      <intent-filter>
        <action android:name="android.telecom.ConnectionService" />
      </intent-filter>
    </service>
  </application>
</manifest>
```

- The library starts its foreground service with every type declared on the service, and each type has its own requirements. `phoneCall|microphone` fits audio calls. Add `camera` only for video calls: it also needs `FOREGROUND_SERVICE_CAMERA` and the camera permission.
- `BIND_TELECOM_CONNECTION_SERVICE` goes on the service only. Your app doesn't request it as a `uses-permission`.

## Runtime permissions

Ask for the microphone, and on Android 13 and later for notifications, before the first call. `RNCallKeep.setup()` asks for the phone permissions it needs itself.

```js
import { PermissionsAndroid, Platform } from 'react-native';

const permissions = [PermissionsAndroid.PERMISSIONS.RECORD_AUDIO];
if (Platform.Version >= 33) {
  permissions.push(PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS);
}
await PermissionsAndroid.requestMultiple(permissions);
```

## Managed or self-managed

- **Managed** (the default): Android shows its own call screens. The user has to enable your app's phone account in the system settings: `RNCallKeep.setup()` shows an alert with your `alertTitle` and `alertDescription`, and opens those settings.
- **Self-managed** (`selfManaged: true`, Android 8 and later): your app draws its own incoming-call screen when it receives [`showIncomingCallUi`](events.md#showincomingcallui), and no phone account needs enabling. See the [self-managed mode guide](guides/android-self-managed.md).

## Calls started from the Phone app (managed mode)

To wake your app when the user starts a call from the Phone app while your app isn't running, add this service to the `application` element of the manifest:

```xml
<service android:name="io.wazo.callkeep.RNCallKeepBackgroundMessagingService" />
```

and register its headless task in your `index.js`:

```js
import { AppRegistry } from 'react-native';

AppRegistry.registerHeadlessTask('RNCallKeepBackgroundMessage', () => ({ name, callUUID, handle }) => {
  // Start the call with your VoIP stack.
  return Promise.resolve();
});
```

## Set up CallKeep

Call `RNCallKeep.setup()` early in your JavaScript, with the `foregroundService` option to keep the microphone in the background: see [Setup and options](setup.md).

Calls work on emulators as well as devices. The [example app](../example/) has this whole setup, tested on Android 10 and Android 16.
