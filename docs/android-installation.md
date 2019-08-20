# Android Installation

## Automatic linking

```sh
react-native link react-native-callkeep
```

## Manual installation

1. In `android/app/build.gradle`
Add a line `compile project(':react-native-callkeep')` in `dependencies {}` section.

2. In `android/settings.gradle`
Add:

```java
include ':react-native-callkeep'
project(':react-native-callkeep').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-callkeep/android')
```

3. In `android/app/src/main/java/.../MainApplication.java`:

```java
import io.wazo.callkeep.RNCallKeepPackage; // Add this import line
//...

private static List<ReactPackage> getPackages() {
    return Arrays.<ReactPackage>asList(
        new MainReactPackage(),
        new RNCallKeepPackage() // Add this line
    );
}
```

4. Add permissionResult listener in `MainActivity.java`:

```java
import io.wazo.callkeep.RNCallKeepModule; // Add these import lines
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class MainActivity extends ReactActivity {
    // ...

    // Permission results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case RNCallKeepModule.REQUEST_READ_PHONE_STATE:
                RNCallKeepModule.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }
}
```

## Android common step installation

1. In `android/app/src/main/AndroidManifest.xml` add these permissions:


```xml
<uses-permission android:name="android.permission.BIND_TELECOM_CONNECTION_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.READ_PHONE_STATE" />
<uses-permission android:name="android.permission.CALL_PHONE" />

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

2. To be able to wake up your killed application when making an outgoing call form the native Phone application:

Add this in the `application` node of `android/app/src/main/AndroidManifest.xml` :

```xml
<service android:name="io.wazo.callkeep.RNCallKeepBackgroundMessagingService" />
```


In your `index.android.js` file :

```js
AppRegistry.registerHeadlessTask('RNCallKeepBackgroundMessage', () => ({ name, callUUID, handle }) => {
  // Make your call here
  
  return Promise.resolve();
});
```
