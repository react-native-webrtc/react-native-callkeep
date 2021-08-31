
import { NativeModules, Platform, Alert } from 'react-native';

import { listeners, emit } from './actions';

const EYRCallKeepModule = NativeModules.EYRCallKeep;
const RNCallKeepModule = NativeModules.RNCallKeep;
const isIOS = Platform.OS === 'ios';
const supportConnectionService = !isIOS && Platform.Version >= 23;

console.log(NativeModules)
const CONSTANTS = {
  END_CALL_REASONS: {
    FAILED: 1,
    REMOTE_ENDED: 2,
    UNANSWERED: 3,
    ANSWERED_ELSEWHERE: 4,
    DECLINED_ELSEWHERE: isIOS ? 5 : 2, // make declined elsewhere link to "Remote ended" on android because that's kinda true
    MISSED: isIOS ? 2 : 6,
  },
};

export { emit, CONSTANTS };

class RNCallKeep {
  constructor() {
    this._callkeepEventHandlers = new Map();
  }

  addEventListener = (type, handler) => {
    const listener = listeners[type](handler);

    this._callkeepEventHandlers.set(type, listener);
  };

  removeEventListener = (type) => {
    const listener = this._callkeepEventHandlers.get(type);
    if (!listener) {
      return;
    }

    listener.remove();
    this._callkeepEventHandlers.delete(type);
  };

  setup = async (options) => {
    if (!isIOS) {
      return this._setupAndroid(options.android);
    }

    return this._setupIOS(options.ios);
  };

  registerAndroidEvents = () => {
    if (isIOS) {
      return;
    }
    RNCallKeepModule.registerEvents();
  };

  answerIncomingCall = (uuid) => {
    EYRCallKeepModule.answerIncomingCalld(uuid);
    //RNCallKeepModule.answerIncomingCall(uuid);
  };

  fulfillAnswerCallAction = () => {
      if (!isIOS) return;
      EYRCallKeepModule.fulfillAnswerCallAction();
  }

  checkPhoneAccountEnabled = async () => {
    if (isIOS) {
      return;
    }

    return RNCallKeepModule.checkPhoneAccountEnabled();
  };

  isConnectionServiceAvailable = async () => {
    if (isIOS) {
      return true;
    }

    return RNCallKeepModule.isConnectionServiceAvailable();
  };

  // Using new module
  reportEndCallWithUUID = (uuid, reason) => EYRCallKeepModule.reportEndCall(uuid, reason);
    //reportEndCallWithUUID = (uuid, reason) => RNCallKeepModule.reportEndCallWithUUID(uuid, reason);
  /*
   * Android explicitly states we reject a call
   * On iOS we just notify of an endCall
   */
  rejectCall = (uuid) => {
    if (!isIOS) {
      RNCallKeepModule.rejectCall(uuid);
    } else {
      EYRCallKeepModule.endCall(uuid);
    }
  };

  isCallActive = async (uuid) => await RNCallKeepModule.isCallActive(uuid);

  getCalls = () => {
    if (isIOS) {
      return RNCallKeepModule.getCalls();
    }
  };

  endCall = (uuid) => EYRCallKeepModule.endCall(uuid);
  //endCall = (uuid) => RNCallKeepModule.endCallWithUUID(uuid);
    
  fulfillEndCallAction = () => {
      if (!isIOS) return;
      EYRCallKeepModule.fulfillEndCallAction();
  }

  endAllCalls = () => RNCallKeepModule.endAllCalls();

  hasOutgoingCall = async () => (isIOS ? null : await RNCallKeepModule.hasOutgoingCall());

  setMutedCall = (uuid, shouldMute) => {
    RNCallKeepModule.setMutedCall(uuid, shouldMute);
  };

  /**
   * @description when Phone call is active, Android control the audio service via connection service. so this function help to toggle the audio to Speaker or wired/ear-piece or vice-versa
   * @param {*} uuid
   * @param {*} routeSpeaker
   * @returns Audio route state of audio service
   */
  toggleAudioRouteSpeaker = (uuid, routeSpeaker) => isIOS ? null : RNCallKeepModule.toggleAudioRouteSpeaker(uuid, routeSpeaker);

  getAudioRoutes = () => RNCallKeepModule.getAudioRoutes();

  setAudioRoute = (uuid, inputName) => RNCallKeepModule.setAudioRoute(uuid, inputName);

  setAvailable = (state) => {
    if (isIOS) {
      return;
    }

    // Tell android that we are able to make outgoing calls
    RNCallKeepModule.setAvailable(state);
  };

  setForegroundServiceSettings = (settings) => {
    if (isIOS) {
      return;
    }

    RNCallKeepModule.setForegroundServiceSettings({
      foregroundService: settings,
    });
  };

  setCurrentCallActive = (callUUID) => {
    if (isIOS) {
      return;
    }

    RNCallKeepModule.setCurrentCallActive(callUUID);
  };

    displayIncomingCall = (
        uuid,
        handle,
        localizedCallerName = '',
        handleType = 'number',
        hasVideo = false,
        options = null
      ) => {
        if (!isIOS) {
          RNCallKeepModule.displayIncomingCall(uuid, handle, localizedCallerName);
          return;
        }

        // should be boolean type value
        let supportsHolding = !!(options?.ios?.supportsHolding ?? true);
        let supportsDTMF = !!(options?.ios?.supportsDTMF ?? true);
        let supportsGrouping = !!(options?.ios?.supportsGrouping ?? true);
        let supportsUngrouping = !!(options?.ios?.supportsUngrouping ?? true);

        EYRCallKeepModule.displayIncomingCall(
          uuid,
          handle,
          handleType,
          hasVideo,
          localizedCallerName,
        );
      };
    
  updateDisplay = (uuid, displayName, handle, options = null) => {
    if (!isIOS) {
      RNCallKeepModule.updateDisplay(uuid, displayName, handle);
      return;
    }

    let iosOptions = {};
    if (options && options.ios) {
      iosOptions = {
        ...options.ios,
      };
    }
    RNCallKeepModule.updateDisplay(uuid, displayName, handle, iosOptions);
  };

  setOnHold = (uuid, shouldHold) => RNCallKeepModule.setOnHold(uuid, shouldHold);

  setReachable = () => RNCallKeepModule.setReachable();

  _setupIOS = async (options) =>
    new Promise((resolve, reject) => {
      if (!options.appName) {
        reject('RNCallKeep.setup: option "appName" is required');
      }
      if (typeof options.appName !== 'string') {
        reject('RNCallKeep.setup: option "appName" should be of type "string"');
      }

      resolve(EYRCallKeepModule.setup(options));
    });

  _setupAndroid = async (options) => {
    RNCallKeepModule.setup(options);

    if (options.selfManaged) {
      return false;
    }

    const showAccountAlert = await RNCallKeepModule.checkPhoneAccountPermission(options.additionalPermissions || []);
    const shouldOpenAccounts = await this._alert(options, showAccountAlert);

    if (shouldOpenAccounts) {
      RNCallKeepModule.openPhoneAccounts();
      return true;
    }

    return false;
  };
    
  backToForeground() {
    if (isIOS) {
      return;
    }

    NativeModules.RNCallKeep.backToForeground();
  }

  getInitialEvents() {
    if (isIOS) {
      return EYRCallKeepModule.getInitialEvents()
    }
    return Promise.resolve([])
  }
}

export default new RNCallKeep();


