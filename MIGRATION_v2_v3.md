# Migration from CallKeep v2 to v3

Thanks to the Sangoma team, CallKeep now allows multi calls through native UI. 
Here's how to upgrade your codebase for this new version.

## Common

### setup

Since the v3, you have to call `setup` each time your application is launched.

### didPerformDTMFAction

`didPerformDTMFAction` now take `digits` instead of `dtmf` as key of its argument.

### didPerformSetMutedCallAction

`didPerformSetMutedCallAction` now returns an object as argument with `{ muted, callUUID }`.

### startCall

`startCall` takes 3 arguments now : `uuid`, `handle`, `contactIdentifier`.

### Call uuids

`setCurrentCallActive`, `endCall`, now takes a `callUuid` argument.

Events are also with an `callUUID` in the argument object.

### ⚠️ Lower case your uuids

There is no more check on the uuid case, everything is returned to your application in lower case.
So you have to send lower cased uuid to allow matching your calls.

### News methods

There is now new method like [updateDisplay]()

## Android

### Update `MainActivity.java`

- Add new imports

```diff
+ import android.support.annotation.NonNull;
+ import android.support.annotation.Nullable;
```

- Update `onRequestPermissionsResult` method:

```java
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
```
