# Android self-managed mode

_This feature is available only on Android._

Android supports calling apps running in what's called "Self Managed". This means the apps are able (and required) to provide their own UI for managing calls. This includes both in call UI elements and incoming call notification UI. This method is all or nothing. You can't mix partial elements, such as having a custom in call view, but use the default incoming call UI.

To implement a self managed calling app, the following steps are necessary:
- Set `selfManaged: true` in setup.
- On an incoming call, from react native, call `RNCallKeep.displayIncomingCall`
- CallKeep will then fire the `showIncomingCallUi` event.
- When `showIncomingCallUi` is fired, you must show an incoming call UI. This would be a high priority notification ([Android: Display time-sensitive notifications](https://developer.android.com/training/notify-user/time-sensitive)).
- If the user answers the call, you call the appropriate RNCallKeep actions such as `answerIncomingCall`, `rejectCall` or `endCall`
- In certain cases Android will not allow you to show an incoming call notification. In that case the 'createIncomingConnectionFailed' event is fired and you should reject the incoming SIP Invite.

Self Managed calling apps are an advanced topic, and there are many steps involved in implementing them, but here are some things to keep in mind:
- React Native Headless Tasks are a great way to execute React Native code. Remember to start up the headless task as a Foreground Service.
- Android will deprioritize your high priority FCM notifications if you fail to show an incoming call ui when receiving them.
- You can avoid getting flooded with sticky foreground service notifications by not defining a Foreground Service for CallKeep, and instead managing this on your own.

The [example app](../../example/) runs in self-managed mode by default and shows the incoming call with in-app Answer and Decline buttons.
