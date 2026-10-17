# Troubleshooting

## Debug logs

### Android

```
adb logcat *:S RNCallKeep:V
```

### iOS

The library logs with the `[RNCallKeep]` prefix: filter on it in Xcode's console, or in the Console app with the device selected.

## Common problems

- Ensure that you construct a valid `uuid` by importing the `uuid` library and running `uuid.v4()` as shown in the examples. If you don't do this and use a custom string, the incoming call screen will never be shown on iOS.
- On iOS, calls fail in the Simulator: it has no CallKit service. Test calls on a device (the app itself starts in the Simulator). Android emulators support calls.
