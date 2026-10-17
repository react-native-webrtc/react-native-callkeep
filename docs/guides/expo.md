# Expo

CallKeep has native code, so it doesn't run in Expo Go: use a [development build](https://docs.expo.dev/develop/development-builds/create-a-build/). As without Expo, iOS calls need a device (the Simulator has no CallKit service), and Android calls work on emulators too. The New Architecture works: there is no need to turn it off.

## Install

```sh
npx expo install react-native-callkeep @config-plugins/react-native-callkeep
```

[`@config-plugins/react-native-callkeep`](https://github.com/expo/config-plugins/tree/main/packages/react-native-callkeep) is maintained in Expo's config-plugins repository, with one major version per Expo SDK; `npx expo install` picks the one for yours. When you run `npx expo prebuild` (or `eas build`), it adds:

- iOS: the `voip` background mode, and CallKit to the linked frameworks.
- Android: the permissions CallKeep checks (`READ_PHONE_STATE`, `READ_PHONE_NUMBERS`, `CALL_PHONE`, `RECORD_AUDIO`) and `FOREGROUND_SERVICE`, `VoiceConnectionService` with the `phoneCall` foreground service type, and `RNCallKeepBackgroundMessagingService`.

## App config

The plugin leaves out the audio settings that the [iOS](../ios-installation.md#background-modes-and-microphone) and [Android](../android-installation.md#manifest) installation guides add. In `app.json`:

```json
{
  "expo": {
    "ios": {
      "infoPlist": {
        "UIBackgroundModes": ["audio", "voip"],
        "NSMicrophoneUsageDescription": "This app uses the microphone for calls."
      }
    },
    "android": {
      "permissions": [
        "android.permission.FOREGROUND_SERVICE_PHONE_CALL",
        "android.permission.FOREGROUND_SERVICE_MICROPHONE",
        "android.permission.POST_NOTIFICATIONS"
      ]
    },
    "plugins": [
      "@config-plugins/react-native-callkeep",
      "./plugins/withCallKeepMicrophone"
    ]
  }
}
```

And `plugins/withCallKeepMicrophone.js`, which gives `VoiceConnectionService` the `phoneCall|microphone` types of the Android guide (the config plugin declares `phoneCall` only, and doesn't change a service that is already declared, so this plugin works in either order):

```js
const { AndroidConfig, withAndroidManifest } = require('expo/config-plugins');

module.exports = function withCallKeepMicrophone(config) {
  return withAndroidManifest(config, (config) => {
    const app = AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);
    app.service = app.service ?? [];
    let service = app.service.find(
      (item) => item.$['android:name'] === 'io.wazo.callkeep.VoiceConnectionService',
    );
    if (!service) {
      service = {
        $: {
          'android:name': 'io.wazo.callkeep.VoiceConnectionService',
          'android:permission': 'android.permission.BIND_TELECOM_CONNECTION_SERVICE',
          'android:exported': 'true',
        },
        'intent-filter': [{ action: [{ $: { 'android:name': 'android.telecom.ConnectionService' } }] }],
      };
      app.service.push(service);
    }
    service.$['android:foregroundServiceType'] = 'phoneCall|microphone';
    return config;
  });
};
```

Without `FOREGROUND_SERVICE_PHONE_CALL`, Android 14 and later refuse to start CallKeep's foreground service (the [`foregroundService` setup option](../setup.md#foreground-service-android-11-and-later)) in apps that target them, as Expo apps do. The call still works, but the app isn't kept running with its microphone in the background; on Android 16 the log shows `Can't start foreground service : java.lang.SecurityException: Starting FGS with type phoneCall ... requires permissions`.

With an Expo SDK older than 55 (plugin 12 and earlier), also add `android.permission.RECORD_AUDIO` to `android.permissions`. Without it, CallKeep in managed mode doesn't consider its phone account usable, and `displayIncomingCall` logs `displayIncomingCall ignored due to no ConnectionService or no phone account`.

## Then

- Ask for the microphone at runtime, and for notifications on Android 13 and later: see [Runtime permissions](../android-installation.md#runtime-permissions).
- Call `RNCallKeep.setup()`: see [Setup and options](../setup.md).
- Calls started from Recents or Siri, and VoIP pushes, need code in the iOS app delegate or scene delegate ([iOS installation](../ios-installation.md#calls-started-from-recents-contacts-or-siri), [incoming calls when the app isn't running](background-calls.md)). In an Expo app, add it with a config plugin of your own.
