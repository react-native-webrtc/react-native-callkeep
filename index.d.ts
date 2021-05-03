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
    'didLoadWithEvents' |
    'showIncomingCallUi';

  type HandleType = 'generic' | 'number' | 'email';

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
    static addEventListener(type: Events, handler: (args: any) => void): void

    static removeEventListener(type: Events): void

    static setup(options: IOptions): Promise<boolean>

    static hasDefaultPhoneAccount(): boolean

    static answerIncomingCall(uuid: string): void

    static registerPhoneAccount(): void

    static registerAndroidEvents(): void

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

    /**
     * @description isCallActive method is available only on iOS.
     */
    static isCallActive(uuid: string): Promise<boolean>

    static getCalls(): Promise<object>

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
