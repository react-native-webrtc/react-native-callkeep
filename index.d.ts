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
  'didPerformSetMutedCallAction';

type HandleType = 'generic' | 'number' | 'email';

interface IOptions {
  ios: {
    appName: string,
    imageName?: string,
    supportsVideo?: boolean,
    maximumCallGroups?: string,
    maximumCallsPerCallGroup?: string,
    ringtoneSound?: string,
  },
  android: {
    alertTitle: string,
    alertDescription: string,
    cancelButton: string,
    okButton: string,
    imageName?: string,
    additionalPermissions: string[],
  },
}

export type DidReceiveStartCallActionPayload = { handle: string };
export type AnswerCallPayload = { callUUID: string };
export type EndCallPayload = AnswerCallPayload;
export type DidDisplayIncomingCallPayload = string | undefined;
export type DidPerformSetMutedCallActionPayload = boolean;

export default class RNCallKeep {
  static addEventListener(type: Events, handler: (args: any) => void) {

  }

  static removeEventListener(type: Events) {

  }

  static async setup(options: IOptions): Promise<void> {

  }

  static hasDefaultPhoneAccount(): boolean {

  }
  
  static answerIncomingCall(uuid: string) {

  }

  static displayIncomingCall(
    uuid: string,
    handle: string,
    localizedCallerName?: string,
    handleType?: HandleType,
    hasVideo?: boolean,
  ) {

  }

  static startCall(
    uuid: string,
    handle: string,
    contactIdentifier?: string,
    handleType?: HandleType,
    hasVideo?: boolean,
  ) {

  }
  static updateDisplay(
    uuid: string,
    displayName: string,
    handle: string,
  ) {

  }

  /**
     * @description reportConnectedOutgoingCallWithUUID method is available only on iOS.
  */
  static reportConnectedOutgoingCallWithUUID(uuid: string) {

  }

  /**
     * @description reportConnectedOutgoingCallWithUUID method is available only on iOS.
  */
  static reportConnectingOutgoingCallWithUUID(uuid: string): void {

  }
  static reportEndCallWithUUID(uuid: string, reason: number): void {

  }

  static rejectCall(uuid: string) {

  }

  static endCall(uuid: string) {

  }

  static endAllCalls() {

  }

  static setReachable() {

  }
  static isCallActive(uuid: string): Promise<boolean> {

  }
  /**
     * @description supportConnectionService method is available only on Android.
  */
  static supportConnectionService(): boolean {

  }

  /**
     * @description hasPhoneAccount method is available only on Android.
  */
  static async hasPhoneAccount(): Promise<boolean> {

  }

  static async hasOutgoingCall(): Promise<boolean> {

  }

  /**
     * @description setMutedCall method is available only on iOS.
  */
  static setMutedCall(uuid: string, muted: boolean) {

  }

  static setOnHold(uuid: string, held: boolean) {

  }

  /**
     * @descriptions sendDTMF is used to send DTMF tones to the PBX.
  */
  static sendDTMF(uuid: string, key: string) {

  }

  static checkIfBusy(): Promise<boolean> {

  }

  static checkSpeaker(): Promise<boolean> {

  }

  /**
     * @description setAvailable method is available only on Android.
  */
  static setAvailable(active: boolean) {

  }

  static setCurrentCallActive() {

  }

  static backToForeground() {

  }
}
