import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const RNCallKeepModule = NativeModules.RNCallKeep;
const eventEmitter = new NativeEventEmitter(RNCallKeepModule);

const RNCallKeepDidReceiveStartCallAction = 'RNCallKeepDidReceiveStartCallAction';
const RNCallKeepPerformAnswerCallAction = 'RNCallKeepPerformAnswerCallAction';
const RNCallKeepPerformEndCallAction = 'RNCallKeepPerformEndCallAction';
const RNCallKeepDidActivateAudioSession = 'RNCallKeepDidActivateAudioSession';
const RNCallKeepDidDeactivateAudioSession = 'RNCallKeepDidDeactivateAudioSession';
const RNCallKeepDidDisplayIncomingCall = 'RNCallKeepDidDisplayIncomingCall';
const RNCallKeepDidPerformSetMutedCallAction = 'RNCallKeepDidPerformSetMutedCallAction';
const RNCallKeepDidToggleHoldAction = 'RNCallKeepDidToggleHoldAction';
const RNCallKeepDidPerformDTMFAction = 'RNCallKeepDidPerformDTMFAction';
const RNCallKeepProviderReset = 'RNCallKeepProviderReset';
const RNCallKeepCheckReachability = 'RNCallKeepCheckReachability';
const isIOS = Platform.OS === 'ios';

const didReceiveStartCallAction = handler => {
  if (isIOS) {
    // Tell CallKeep that we are ready to receive `RNCallKeepDidReceiveStartCallAction` event and prevent delay
    RNCallKeepModule._startCallActionEventListenerAdded();
  }

  return eventEmitter.addListener(RNCallKeepDidReceiveStartCallAction, (data) => handler(data));
};

const answerCall = handler =>
  eventEmitter.addListener(RNCallKeepPerformAnswerCallAction, (data) => handler(data));

const endCall = handler =>
  eventEmitter.addListener(RNCallKeepPerformEndCallAction, (data) => handler(data));

const didActivateAudioSession = handler =>
  eventEmitter.addListener(RNCallKeepDidActivateAudioSession, handler);

const didDeactivateAudioSession = handler =>
  eventEmitter.addListener(RNCallKeepDidDeactivateAudioSession, handler);

const didDisplayIncomingCall = handler =>
  eventEmitter.addListener(RNCallKeepDidDisplayIncomingCall, (data) => handler(data));

const didPerformSetMutedCallAction = handler =>
  eventEmitter.addListener(RNCallKeepDidPerformSetMutedCallAction, (data) => handler(data));

const didToggleHoldCallAction = handler =>
  eventEmitter.addListener(RNCallKeepDidToggleHoldAction, handler);

const didPerformDTMFAction = handler =>
  eventEmitter.addListener(RNCallKeepDidPerformDTMFAction, (data) => handler(data));

const didResetProvider = handler =>
  eventEmitter.addListener(RNCallKeepProviderReset, handler);

const checkReachability = handler =>
  eventEmitter.addListener(RNCallKeepCheckReachability, handler);

export const listeners = {
  didReceiveStartCallAction,
  answerCall,
  endCall,
  didActivateAudioSession,
  didDeactivateAudioSession,
  didDisplayIncomingCall,
  didPerformSetMutedCallAction,
  didToggleHoldCallAction,
  didPerformDTMFAction,
  didResetProvider,
  checkReachability,
};
