'use strict';

import {
    NativeModules,
    NativeEventEmitter,
    Platform,
} from 'react-native';

const _RNCallKit = NativeModules.RNCallKit;
const _RNCallKitEmitter = new NativeEventEmitter(_RNCallKit);

const _callkitEventHandlers = new Map();

const RNCallKitDidReceiveStartCallAction = 'RNCallKitDidReceiveStartCallAction';
const RNCallKitPerformAnswerCallAction = 'RNCallKitPerformAnswerCallAction';
const RNCallKitPerformEndCallAction = 'RNCallKitPerformEndCallAction';
const RNCallKitDidActivateAudioSession = 'RNCallKitDidActivateAudioSession';

export default class RNCallKit {
    static addEventListener(type, handler) {
        if (Platform.OS !== 'ios') return;
        var listener;
        if (type === 'didReceiveStartCallAction') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitDidReceiveStartCallAction,
                (data) => { handler(data);}
            );
            _RNCallKit._startCallActionEventListenerAdded();
        } else if (type === 'answerCall') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitPerformAnswerCallAction,
                (data) => { handler(data);}
            );
        } else if (type === 'endCall') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitPerformEndCallAction,
                (data) => { handler(data); }
            );
        } else if (type === 'didActivateAudioSession') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitDidActivateAudioSession,
                () => { handler(); }
            );
        }
        _callkitEventHandlers.set(handler, listener);
    }

    static removeEventListener(type, handler) {
        if (Platform.OS !== 'ios') return;
        var listener = _callkitEventHandlers.get(handler);
        if (!listener) {
            return;
        }
        listener.remove();
        _callkitEventHandlers.delete(handler);
    }

    static setup(options) {
        if (Platform.OS !== 'ios') return;
        if (!options.appName) {
            throw new Error('RNCallKit.setup: option "appName" is required');
        }
        if (typeof options.appName !== 'string') {
            throw new Error('RNCallKit.setup: option "appName" should be of type "string"');
        }
        _RNCallKit.setup(options);
    }

    static displayIncomingCall(uuid, handle, handleType = 'number', hasVideo = false, localizedCallerName?: String) {
        if (Platform.OS !== 'ios') return;
        _RNCallKit.displayIncomingCall(uuid, handle, handleType, hasVideo, localizedCallerName);
    }

    static startCall(uuid, handle, handleType = 'number', hasVideo = false) {
        if (Platform.OS !== 'ios') return;
        _RNCallKit.startCall(uuid, handle, handleType, hasVideo);
    }

    static reportConnectedOutgoingCallWithUUID(uuid) {
        if (Platform.OS !== 'ios') return;
        _RNCallKit.reportConnectedOutgoingCallWithUUID(uuid);
    }

    static endCall(uuid) {
        if (Platform.OS !== 'ios') return;
        _RNCallKit.endCall(uuid);
    }

    /*
    static setHeldCall(uuid, onHold) {
        if (Platform.OS !== 'ios') return;
        _RNCallKit.setHeldCall(uuid, onHold);
    }
    */
}
