# Android cold-start event regression

`CallKeepBootstrapTest.kt` is an instrumentation fixture for a React Native
**0.87** host application. It needs React Native's actual JNI implementations of
`WritableNativeArray` and `ReadableNativeArray`; a mocked JavaScript unit test
does not exercise their cached Java-side reads.

Copy the fixture into the host's `android/app/src/androidTest/java/io/wazo/callkeep/`
directory. The host must load React Native's native libraries in its Application
(the stock 0.87 template does), and include CallKeep through autolinking.
Use `androidx.test:runner:1.7.0` and `androidx.test.ext:junit:1.3.0` as
`androidTestImplementation` dependencies and
`androidx.test.runner.AndroidJUnitRunner` as `testInstrumentationRunner`.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w \
  -e class io.wazo.callkeep.CallKeepBootstrapTest \
  YOUR_APPLICATION_ID.test/androidx.test.runner.AndroidJUnitRunner
```

The fixture creates a context without starting JavaScript and tests:

- An answer queued after native setup remains visible.
- Answer and end queued after setup are emitted once, in order, when React is ready.
- Observing before React starts leaves actions pending instead of accessing JS.
- Repeated `getInitialEvents` calls return fresh complete snapshots, preserve
  event payloads, and are cleared only by explicit acknowledgement or replay.

The test event context replaces only the JS event sink. Native array creation,
JNI reads and CallKeep methods are real. This validates event retention and
replay, not FCM delivery, real bridge startup, Telecom UI, or audio.
