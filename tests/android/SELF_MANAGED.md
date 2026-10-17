# Self-managed incoming connection regression

`CallKeepSelfManagedTest.kt` uses Android's actual Telecom service and a React
Native 0.87 host. Copy it into the host application's
`android/app/src/androidTest/java/io/wazo/callkeep/` directory, with the AndroidX
test runner and JUnit dependencies (`runner:1.7.0`, `ext:junit:1.3.0`). The host's
Application must load React Native's native libraries, as the stock template does.

Declare `io.wazo.callkeep.VoiceConnectionService` in the host manifest according
to CallKeep's installation instructions, with the
`android.permission.BIND_TELECOM_CONNECTION_SERVICE` permission. Keep
`MANAGE_OWN_CALLS`, but leave `READ_PHONE_NUMBERS` **denied**, or remove it with
`tools:node="remove"`. The test asserts this precondition.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w \
  -e class io.wazo.callkeep.CallKeepSelfManagedTest \
  YOUR_APPLICATION_ID.test/androidx.test.runner.AndroidJUnitRunner
```

The test submits one incoming call without JavaScript, waits for Telecom to
create its connection, asserts `PROPERTY_SELF_MANAGED`, and rejects it in
cleanup. There is no media or network session. On unmodified 4.3.17, recent
Android versions throw `SecurityException` from
`TelecomManager.getPhoneAccount` while creating the connection.

Both incoming and outgoing requests carry the self-managed flag. The explicit
flag approach follows [PR #685](https://github.com/react-native-webrtc/react-native-callkeep/pull/685)
by Donald Wong, extending it to the incoming path that triggers this regression.
