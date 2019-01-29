import { NativeModules, Platform, Alert } from 'react-native';

import { listeners } from './actions'

const RNCallKeepModule = NativeModules.RNCallKeep;
const isIOS = Platform.OS === 'ios';
const supportConnectionService = !isIOS && Platform.Version >= 23;

class RNCallKeep {

  constructor() {
    this._callkitEventHandlers = new Map();
  }


  addEventListener(type, handler) {
    const listener = listeners[type](handler);

    this._callkitEventHandlers.set(handler, listener);
  }

  removeEventListener(type, handler) {
    const listener = this._callkitEventHandlers.get(handler);
    if (!listener) {
      return;
    }

    listener.remove();
    this._callkitEventHandlers.delete(handler);
  }

  setup(options) {
    if (!isIOS) {
      return (async () => {
        return this._setupAndroid(options.android);
      })();
    }

    return this._setupIOS(options.ios);
  }

  displayIncomingCall(uuid, handle, localizedCallerName, handleType = 'number', hasVideo = false) {
    if (!isIOS) {
      RNCallKeepModule.displayIncomingCall(handle, localizedCallerName);
      return;
    }

    RNCallKeepModule.displayIncomingCall(uuid, handle, handleType, hasVideo, localizedCallerName);
  }

  startCall(uuid, handle, handleType = 'number', hasVideo = false, contactIdentifier) {
    if (!isIOS) {
      RNCallKeepModule.startCall(handle, contactIdentifier);
      return;
    }

    RNCallKeepModule.startCall(uuid, handle, handleType, hasVideo, contactIdentifier);
  }

  reportConnectedOutgoingCallWithUUID(uuid) {
    RNCallKeepModule.reportConnectedOutgoingCallWithUUID(uuid);
  }

  endCall(uuid) {
    isIOS ? RNCallKeepModule.endCall(uuid) : RNCallKeepModule.endCall();
  }

  endAllCalls() {
    isIOS ? RNCallKeepModule.endAllCalls() : RNCallKeepModule.endCall();
  }

  supportConnectionService() {
    return supportConnectionService;
  }

  async hasPhoneAccount() {
    return isIOS ? true : await RNCallKeepModule.hasPhoneAccount();
  }

  setMutedCAll(uuid, muted) {
     if (!isIOS) {
      // Can't mute on Android
      return;
    }

    RNCallKeepModule.setMutedCall(uuid, muted);
  }

  checkIfBusy() {
    return Platform.OS === 'ios'
      ? RNCallKeepModule.checkIfBusy()
      : Promise.reject('RNCallKeep.checkIfBusy was called from unsupported OS');
  };

  checkSpeaker() {
    return Platform.OS === 'ios'
      ? RNCallKeepModule.checkSpeaker()
      : Promise.reject('RNCallKeep.checkSpeaker was called from unsupported OS');
  }

  setActive = (state) => {
    if (isIOS) {
      return;
    }

    // Tell android that we are able to make outgoing calls
    RNCallKeepModule.setActive(state);
  }

  _setupIOS(options) {
    if (!options.appName) {
        throw new Error('RNCallKeep.setup: option "appName" is required');
    }
    if (typeof options.appName !== 'string') {
        throw new Error('RNCallKeep.setup: option "appName" should be of type "string"');
    }

    RNCallKeepModule.setup(options);
  }

  async _setupAndroid(options) {
    const hasAccount = await RNCallKeepModule.checkPhoneAccountPermission();
    if (hasAccount) {
      return;
    }

    Alert.alert(
      options.alertTitle,
      options.alertDescription,
      [
        {
          text: options.cancelButton,
          onPress: () => {},
          style: 'cancel',
        },
        { text: options.okButton,
          onPress: () => RNCallKeepModule.openPhoneAccounts()
        },
      ],
      { cancelable: true },
    );
  }

  /*
  static holdCall(uuid, onHold) {
    RNCallKeepModule.setHeldCall(uuid, onHold);
  }
  */
}

export default new RNCallKeep();
