# iOS 27: scene life cycle

Apps built with the iOS 27 SDK must use the scene life cycle: without a scene delegate declared in `Info.plist`, iOS refuses to launch them ("UIScene life cycle is required for apps built with this SDK"). The React Native 0.87 template still creates its window in `AppDelegate`, so it needs this change.

With scenes, iOS no longer calls the app delegate's `application(_:continue:restorationHandler:)` when the user starts a call from Recents, Contacts or Siri: it calls the scene delegate. Forward those activities to `RNCallKeep.continueUserActivity(_:)`, which sends `didReceiveStartCallAction` as before:

```swift
import UIKit
import RNCallKeep

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
  var window: UIWindow?

  func scene(
    _ scene: UIScene,
    willConnectTo session: UISceneSession,
    options connectionOptions: UIScene.ConnectionOptions
  ) {
    guard let windowScene = scene as? UIWindowScene,
          let appDelegate = UIApplication.shared.delegate as? AppDelegate else { return }

    let window = UIWindow(windowScene: windowScene)
    self.window = window
    appDelegate.reactNativeFactory?.startReactNative(withModuleName: "YourApp", in: window, launchOptions: nil)

    // A call started from Recents, Contacts or Siri while the app was not running.
    for userActivity in connectionOptions.userActivities {
      RNCallKeep.continueUserActivity(userActivity)
    }
  }

  // A call started from Recents, Contacts or Siri while the app is running.
  func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    RNCallKeep.continueUserActivity(userActivity)
  }
}
```

In `AppDelegate`, keep creating the React Native factory in `application(_:didFinishLaunchingWithOptions:)`, but remove the window and the `startReactNative` call: the scene delegate does that now. Then declare the scene in `Info.plist`:

```xml
<key>UIApplicationSceneManifest</key>
<dict>
  <key>UIApplicationSupportsMultipleScenes</key>
  <false/>
  <key>UISceneConfigurations</key>
  <dict>
    <key>UIWindowSceneSessionRoleApplication</key>
    <array>
      <dict>
        <key>UISceneConfigurationName</key>
        <string>Default</string>
        <key>UISceneDelegateClassName</key>
        <string>$(PRODUCT_MODULE_NAME).SceneDelegate</string>
      </dict>
    </array>
  </dict>
</dict>
```

PushKit (`reportNewIncomingCall`) and a native `RNCallKeep.setup` stay in the app delegate: they don't depend on scenes. In Objective-C, the call is `[RNCallKeep continueUserActivity:userActivity]`.

The [example app](../../example/ios/CallKeepExample/SceneDelegate.swift) uses this setup, tested on iOS 27.
