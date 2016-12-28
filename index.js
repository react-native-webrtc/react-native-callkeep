'use strict';

import {
    NativeModules,
    NativeEventEmitter,
} from 'react-native';

const _RNCallKit = NativeModules.RNCallKit;
const _RNCallKitEmitter = new NativeEventEmitter(_RNCallKit);

const _callkitEventHandlers = new Map();

const RNCallKitDidReceiveStartCallAction = 'RNCallKitDidReceiveStartCallAction';
const RNCallKitPerformAnswerCallAction = 'RNCallKitPerformAnswerCallAction';
const RNCallKitPerformEndCallAction = 'RNCallKitPerformEndCallAction';
const RNCallKitConfigureAudioSession = 'RNCallKitConfigureAudioSession';

export default class RNCallKit {
    static addEventListener(type, handler) {
        var listener;
        if (type === 'didReceiveStartCallAction') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitDidReceiveStartCallAction,
                (data) => { handler(data);}
            );
        } else if (type === 'answerCall') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitPerformAnswerCallAction,
                () => { handler();}
            );
        } else if (type === 'endCall') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitPerformEndCallAction,
                () => { handler(); }
            );
        } else if (type === 'configureAudioSession') {
            listener = _RNCallKitEmitter.addListener(
                RNCallKitConfigureAudioSession,
                (data) => { handler(data); }
            );
        }
        _callkitEventHandlers.set(handler, listener);
    }

    static removeEventListener(type, handler) {
        var listener = _callkitEventHandlers.get(handler);
        if (!listener) {
            return;
        }
        listener.remove();
        _callkitEventHandlers.delete(handler);
    }

    static setupWithAppName(appName) {
        _RNCallKit.setupWithAppName(appName);
    }

    static displayIncomingCall(uuid, handle, hasVideo = false) {
        _RNCallKit.displayIncomingCall(uuid, handle, hasVideo);
    }

    static startCall(uuid, handle, hasVideo = false) {
        _RNCallKit.startCall(uuid, handle, hasVideo);
    }

    static endCall(uuid) {
        _RNCallKit.endCall(uuid);
    }

	/*
    static setHeldCall(uuid, onHold) {
        _RNCallKit.setHeldCall(uuid, onHold);
    }
	*/
}
