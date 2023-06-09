declare module 'react-native-callkeep' {
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
    showIncomingCallUi: 'RNCallKeepShowIncomingCallUi';
    silenceIncomingCall: 'RNCallKeepOnSilenceIncomingCall';
    createIncomingConnectionFailed: 'RNCallKeepOnIncomingConnectionFailed';
    checkReachability: 'RNCallKeepCheckReachability';
    didResetProvider: 'RNCallKeepProviderReset';
    didLoadWithEvents: 'RNCallKeepDidLoadWithEvents';
  }

  export type InitialEvents = Array<{
    [Event in Events]: { name: NativeEvents[Event], data: EventsPayload[Event] }
  }[Events]>

  export type Events = keyof NativeEvents;
  export type EventsPayload = {
    didReceiveStartCallAction: { handle: string, callUUID?: string, name?: string };
    answerCall: { callUUID: string };
    endCall: { callUUID: string };
    didActivateAudioSession: undefined;
    didDeactivateAudioSession: undefined;
    didDisplayIncomingCall: {
      error?: string,
      errorCode?: 'Unentitled' | 'CallUUIDAlreadyExists' | 'FilteredByDoNotDisturb' | 'FilteredByBlockList' | 'Unknown',
      callUUID: string,
      handle: string,
      localizedCallerName: string,
      hasVideo: '1' | '0',
      fromPushKit: '1' | '0',
      payload: object,
    };
    didPerformSetMutedCallAction: { muted: boolean, callUUID: string };
    didToggleHoldCallAction: { hold: boolean, callUUID: string };
    didChangeAudioRoute: {
      output: string,
      reason?: number,
      handle?: string,
      callUUID?: string,
    };
    didPerformDTMFAction: { digits: string, callUUID: string };
    showIncomingCallUi: { handle: string, callUUID: string, name: string };
    silenceIncomingCall: { handle: string, callUUID: string, name: string };
    createIncomingConnectionFailed: { handle: string, callUUID: string, name: string };
    checkReachability: undefined;
    didResetProvider: undefined;
    didLoadWithEvents: InitialEvents;
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

  export class EventListener {
    remove(): void
  }

  export default class RNCallKeep {
    static getInitialEvents(): Promise<InitialEvents>

    static clearInitialEvents(): void

    static addEventListener<Event extends Events>(
      type: Event,
      handler: (args: EventsPayload[Event]) => void,
    ): EventListener

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

    static setSettings(settings: IOptions): void;

    /**
     * @description isCallActive method is available only on iOS.
     */
    static isCallActive(uuid: string): Promise<boolean>

    static getCalls(): Promise<{
      callUUID: string,
      hasConnected: boolean,
      hasEnded: boolean,
      onHold: boolean,
      outgoing: boolean
    }[] | void>

    static getAudioRoutes(): Promise<void>

    static setAudioRoute: (uuid: string, inputName: string) => Promise<void>

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

    static setForegroundServiceSettings(settings: NonNullable<IOptions['android']['foregroundService']>): void

    static canMakeMultipleCalls(allow: boolean): void

    static setCurrentCallActive(callUUID: string): void

    static backToForeground(): void
  }
}
