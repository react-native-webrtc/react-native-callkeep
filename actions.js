import { 
    NativeModules,
    NativeEventEmitter,
} from 'react-native';

const _RNCallKit = NativeModules.RNCallKit;
const _RNCallKitEmitter = new NativeEventEmitter(_RNCallKit);

const RNCallKitDidReceiveStartCallAction = 'RNCallKitDidReceiveStartCallAction';
const RNCallKitPerformAnswerCallAction = 'RNCallKitPerformAnswerCallAction';
const RNCallKitPerformEndCallAction = 'RNCallKitPerformEndCallAction';
const RNCallKitDidActivateAudioSession = 'RNCallKitDidActivateAudioSession';
const RNCallKitDidDisplayIncomingCall = 'RNCallKitDidDisplayIncomingCall';
const RNCallKitDidPerformSetMutedCallAction = 'RNCallKitDidPerformSetMutedCallAction';

didReceiveStartCallAction = handler => {
    const listener = _RNCallKitEmitter.addListener(
        RNCallKitDidReceiveStartCallAction,
        (data) => { handler(data);}
    );
    _RNCallKit._startCallActionEventListenerAdded();
    return listener;
}

answerCall = handler => (
    _RNCallKitEmitter.addListener(
        RNCallKitPerformAnswerCallAction,
        (data) => { handler(data);}
    )
)

endCall = handler => (
    _RNCallKitEmitter.addListener(
        RNCallKitPerformEndCallAction,
        (data) => { handler(data); }
    )
)

didActivateAudioSession = handler => (
    _RNCallKitEmitter.addListener(
        RNCallKitDidActivateAudioSession,
        () => { handler(); }
    )
)

didDisplayIncomingCall = handler => (
    _RNCallKitEmitter.addListener(
        RNCallKitDidDisplayIncomingCall,
        (data) => { handler(data.error); }
    )
)

didPerformSetMutedCallAction = handler => (
    _RNCallKitEmitter.addListener(
        RNCallKitDidPerformSetMutedCallAction,
        (data) => { handler(data.muted); }
    )
)

export const listeners = { 
    didReceiveStartCallAction,
    answerCall,
    endCall,
    didActivateAudioSession,
    didDisplayIncomingCall,
    didPerformSetMutedCallAction,
};

