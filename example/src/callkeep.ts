import { PermissionsAndroid, Platform } from 'react-native';
import type { Permission } from 'react-native';

// The options passed to RNCallKeep.setup(). The README's setup section describes every key.
export function setupOptions(selfManaged: boolean) {
  return {
    ios: {
      appName: 'CallKeep Example',
      supportsVideo: true,
      maximumCallGroups: '2',
      maximumCallsPerCallGroup: '1',
      // Calls appear in the Phone app's Recents; calling back from there sends didReceiveStartCallAction.
      includesCallsInRecents: true,
    },
    android: {
      alertTitle: 'Phone account',
      alertDescription: 'CallKeep Example needs a phone account to show calls.',
      cancelButton: 'Cancel',
      okButton: 'OK',
      additionalPermissions: [],
      // Self-managed: the app shows its own incoming-call screen (the showIncomingCallUi event).
      // Managed: Android shows its call screen, once the user enables the app's phone account.
      selfManaged,
      // Keeps the app and its microphone running while a call is in progress.
      foregroundService: {
        channelId: 'io.wazo.callkeep.example.calls',
        channelName: 'Calls',
        notificationTitle: 'Call in progress',
        notificationIcon: 'ic_launcher',
      },
    },
  };
}

// Android 13+ needs the notification permission for the foreground service's notification, and a
// call needs the microphone. RNCallKeep.setup() asks for the phone permissions it needs itself.
export async function requestAndroidPermissions(): Promise<void> {
  if (Platform.OS !== 'android') {
    return;
  }
  const permissions: Permission[] = [PermissionsAndroid.PERMISSIONS.RECORD_AUDIO];
  if (Platform.Version >= 33) {
    permissions.push(PermissionsAndroid.PERMISSIONS.POST_NOTIFICATIONS);
  }
  await PermissionsAndroid.requestMultiple(permissions);
}

// CallKit and Telecom identify calls by UUID. This is enough for the example; an app would use the
// `uuid` package, or the id its VoIP server gives the call.
export function newCallUUID(): string {
  const hex = (range: number, offset = 0) => (offset + Math.floor(Math.random() * range)).toString(16);
  // Version 4: the third group starts with 4, the fourth with 8, 9, a or b.
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, c => (c === 'x' ? hex(16) : hex(4, 8)));
}
