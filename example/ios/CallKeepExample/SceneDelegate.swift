import UIKit
import RNCallKeep

// iOS 27 requires the scene life cycle: the window and React Native start here, not in AppDelegate.
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
    appDelegate.reactNativeFactory?.startReactNative(withModuleName: "CallKeepExample", in: window, launchOptions: nil)

    // A call started from Recents, Contacts or Siri while the app was not running.
    for userActivity in connectionOptions.userActivities {
      RNCallKeep.continueUserActivity(userActivity)
    }
  }

  // A call started from Recents, Contacts or Siri while the app is running: RNCallKeep sends
  // didReceiveStartCallAction with the number, and the app starts the call.
  func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    RNCallKeep.continueUserActivity(userActivity)
  }
}
