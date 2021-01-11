# Migration from CallKeep v3 to v4

The `reportNewIncomingCall` method on iOS is now more consistent.

Please update your `AppDelegate.m` file with this new signature:

```objc
[RNCallKeep reportNewIncomingCall: uuidString
                           handle: handle
                       handleType: handleType
                         hasVideo: YES
              localizedCallerName: localizedCallerName
                  supportsHolding: YES
                     supportsDTMF: YES
                 supportsGrouping: YES
               supportsUngrouping: YES
                      fromPushKit: YES
                          payload: nil
            withCompletionHandler: nil];
```
