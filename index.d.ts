declare module 'react-native-callkeep' {
  export type Events =
    'didReceiveStartCallAction' |
    'answerCall' |
    'endCall' |
    'didActivateAudioSession' |
    'didDeactivateAudioSession' |
    'didDisplayIncomingCall' |
    'didToggleHoldCallAction' |
    'didPerformDTMFAction' |
    'didResetProvider' |
    'checkReachability' |
    'didPerformSetMutedCallAction' |
    'didChangeAudioRoute' |
    'didLoadWithEvents' |
    'showIncomingCallUi' |
    'silenceIncomingCall' |
    'createIncomingConnectionFailed';

  export type InitialEvent<Event extends Events> = {
    name: NativeEvents[Event],
    data: EventHandlers[Event]
  }
  export type InitialEvents = Array<InitialEvent<Events>>;

  export type NativeEvents = {
    didReceiveStartCallAction: 'RNCallKeepDidReceiveStartCallAction';
    answerCall: 'RNCallKeepPerformAnswerCallAction';
    endCall: 'RNCallKeepPerformEndCallAction';
    didActivateAudioSession: 'RNCallKeepDidActivateAudioSession';
    didDeactivateAudioSession: 'RNCallKeepDidDeactivateAudioSession';
    didDisplayIncomingCall: 'RNCallKeepDidDisplayIncomingCall';
    didPerformSetMutedCallAction: 'RNCallKeepDidPerformSetMutedCallAction';
    didToggleHoldCallAction: 'RNCallKeepDidToggleHoldAction';
    didChangeAudioRoute: 'RNCallKeepDidChangeAudioRoute';
    didPerformDTMFAction: 'RNCallKeepDidPerformDTMFAction';
    didLoadWithEvents: 'RNCallKeepDidLoadWithEvents';
    showIncomingCallUi: 'RNCallKeepShowIncomingCallUi';
    silenceIncomingCall: 'RNCallKeepOnSilenceIncomingCall';
    createIncomingConnectionFailed: 'RNCallKeepOnIncomingConnectionFailed';
    checkReachability: 'RNCallKeepCheckReachability';
    didResetProvider: 'RNCallKeepProviderReset';
  }
  export type EventHandlers = {
    didReceiveStartCallAction: (args: { handle: string, callUUID: string, name: string }) => void;
    answerCall: (args: { callUUID: string }) => void;
    endCall: (args: { callUUID: string }) => void;
    didActivateAudioSession: () => void;
    didDeactivateAudioSession: () => void;
    didDisplayIncomingCall: (args: {
        error?: string,
        errorCode?: 'Unentitled' | 'CallUUIDAlreadyExists' | 'FilteredByDoNotDisturb' | 'FilteredByBlockList' | 'Unknown',
        callUUID: string,
        handle: string,
        localizedCallerName: string,
        hasVideo: '1' | '0',
        fromPushKit: '1' | '0',
        payload: object,
    }) => void;
    didPerformSetMutedCallAction: (args: { muted: boolean, callUUID: string }) => void;
    didToggleHoldCallAction: (args: { hold: boolean, callUUID: string }) => void;
    didChangeAudioRoute: (args: {
      output: string,
      reason?: number,
      handle?: string,
      callUUID?: string,
    }) => void;
    didPerformDTMFAction: (args: { digits: string, callUUID: string }) => void;
    didLoadWithEvents: (args: { events: InitialEvents }) => void;
    showIncomingCallUi: (args: { handle: string, callUUID: string, name: string}) => void;
    silenceIncomingCall: (args: { handle: string, callUUID: string, name: string}) => void;
    createIncomingConnectionFailed: (args: { handle: string, callUUID: string, name: string}) => void;
    checkReachability: () => void;
    didResetProvider: () => void;
  }

  type HandleType = 'generic' | 'number' | 'email';

  export type AudioRoute = {
    name: string,
    type: string,
    selected?: boolean
  }

  interface IOptions {
    ios: {
      appName: string,
      imageName?: string,
      supportsVideo?: boolean,
      maximumCallGroups?: string,
      maximumCallsPerCallGroup?: string,
      ringtoneSound?: string,
      includesCallsInRecents?: boolean
    },
    android: {
      alertTitle: string,
      alertDescription: string,
      cancelButton: string,
      okButton: string,
      imageName?: string,
      additionalPermissions: string[],
      selfManaged?: boolean,
      foregroundService?: {
        channelId: string,
        channelName: string,
        notificationTitle: string,
        notificationIcon?: string
      }
    }
  }

  export type DidReceiveStartCallActionPayload = { handle: string };
  export type AnswerCallPayload = { callUUID: string };
  export type EndCallPayload = AnswerCallPayload;
  export type DidDisplayIncomingCallPayload = string | undefined;
  export type DidPerformSetMutedCallActionPayload = boolean;

  export const CONSTANTS: {
    END_CALL_REASONS: {
      FAILED: 1,
      REMOTE_ENDED: 2,
      UNANSWERED: 3,
      ANSWERED_ELSEWHERE: 4,
      DECLINED_ELSEWHERE: 5 | 2,
      MISSED: 2 | 6
    }
  };

  export default class RNCallKeep {
    static getInitialEvents(): Promise<InitialEvents>

    static clearInitialEvents(): void

    static addEventListener<Event extends Events>(
      type: Event,
      handler: EventHandlers[Event],
    ): void

    static removeEventListener(type: Events): void

    static setup(options: IOptions): Promise<boolean>

    static hasDefaultPhoneAccount(): boolean

    static answerIncomingCall(uuid: string): void

    static registerPhoneAccount(options: IOptions): void

    static registerAndroidEvents(): void

    static unregisterAndroidEvents(): void

    static displayIncomingCall(
      uuid: string,
      handle: string,
      localizedCallerName?: string,
      handleType?: HandleType,
      hasVideo?: boolean,
      options?: object,
    ): void

    static startCall(
      uuid: string,
      handle: string,
      contactIdentifier?: string,
      handleType?: HandleType,
      hasVideo?: boolean,
    ): void

    static updateDisplay(
      uuid: string,
      displayName: string,
      handle: string,
      options?: object,
    ): void

    static checkPhoneAccountEnabled(): Promise<boolean>;

    static isConnectionServiceAvailable(): Promise<boolean>;

    /**
     * @description reportConnectedOutgoingCallWithUUID method is available only on iOS.
     */
    static reportConnectedOutgoingCallWithUUID(uuid: string): void

    /**
     * @description reportConnectedOutgoingCallWithUUID method is available only on iOS.
     */
    static reportConnectingOutgoingCallWithUUID(uuid: string): void

    static reportEndCallWithUUID(uuid: string, reason: number): void

    static rejectCall(uuid: string): void

    static endCall(uuid: string): void

    static endAllCalls(): void

    static setReachable(): void

    static setSettings(settings: Object): void;

    /**
     * @description isCallActive method is available only on iOS.
     */
    static isCallActive(uuid: string): Promise<boolean>

    static getCalls(): Promise<object>

    static getAudioRoutes(): Promise<void>

    static setAudioRoute: (uuid:string, inputName: string) => Promise<void>

    /**
     * @description supportConnectionService method is available only on Android.
     */
    static supportConnectionService(): boolean

    /**
     * @description hasPhoneAccount method is available only on Android.
     */
    static hasPhoneAccount(): Promise<boolean>

    static hasOutgoingCall(): Promise<boolean>

    /**
     * @description setMutedCall method is available only on iOS.
     */
    static setMutedCall(uuid: string, muted: boolean): void

    /**
     * @description toggleAudioRouteSpeaker method is available only on Android.
     * @param uuid
     * @param routeSpeaker
     */
    static toggleAudioRouteSpeaker(uuid: string, routeSpeaker: boolean): void

    static setOnHold(uuid: string, held: boolean): void

    static setConnectionState(uuid: string, state: number): void

    /**
     * @descriptions sendDTMF is used to send DTMF tones to the PBX.
     */
    static sendDTMF(uuid: string, key: string): void

    static checkIfBusy(): Promise<boolean>

    static checkSpeaker(): Promise<boolean>

    /**
     * @description setAvailable method is available only on Android.
     */
    static setAvailable(active: boolean): void

    static setForegroundServiceSettings(settings: Object): void

    static canMakeMultipleCalls(allow: boolean): void

    static setCurrentCallActive(callUUID: string): void

    static backToForeground(): void
  }
}
