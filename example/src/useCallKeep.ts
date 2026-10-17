import { useEffect, useSyncExternalStore } from 'react';
import { Platform } from 'react-native';
import RNCallKeep, { CONSTANTS } from 'react-native-callkeep';
import type { AudioRoute, Events, EventsPayload } from 'react-native-callkeep';
import { newCallUUID, requestAndroidPermissions, setupOptions } from './callkeep';

export type Call = {
  uuid: string;
  handle: string;
  name: string;
  direction: 'incoming' | 'outgoing';
  state: 'ringing' | 'connecting' | 'active';
  held: boolean;
  muted: boolean;
};

export type LogEntry = { id: number; time: string; event: string; data?: string };

// Every event the library sends; the example logs them all. didLoadWithEvents comes first: iOS sends
// it as soon as the first listener is added, with the events received before JavaScript listened
// (an incoming call on a cold start, for example), so its listener must already exist.
const EVENTS: Events[] = [
  'didLoadWithEvents',
  'didReceiveStartCallAction',
  'answerCall',
  'endCall',
  'didDisplayIncomingCall',
  'didActivateAudioSession',
  'didDeactivateAudioSession',
  'didPerformSetMutedCallAction',
  'didToggleHoldCallAction',
  'didPerformDTMFAction',
  'didChangeAudioRoute',
  'didResetProvider',
  'showIncomingCallUi',
  'silenceIncomingCall',
  'createIncomingConnectionFailed',
  'checkReachability',
  'onHasActiveCall',
];

// Native names of the events that change the call list, to replay those received before JavaScript.
const REPLAYED: Record<string, Events> = {
  RNCallKeepDidReceiveStartCallAction: 'didReceiveStartCallAction',
  RNCallKeepPerformAnswerCallAction: 'answerCall',
  RNCallKeepPerformEndCallAction: 'endCall',
  RNCallKeepDidDisplayIncomingCall: 'didDisplayIncomingCall',
};

type Handlers = { [E in Events]?: (data: EventsPayload[E]) => void };

// The calls and the log live outside the screen, as an app's call state should. On Android, calls
// outlive the activity: a font size or language change, Back on Android 11 and earlier, or swiping
// the app away from Recents destroy it while the call goes on, and a new screen reads the state again.
type State = { ready: boolean; selfManaged: boolean; calls: Call[]; log: LogEntry[]; audioRoutes: AudioRoute[] };
let state: State = { ready: false, selfManaged: true, calls: [], log: [], audioRoutes: [] };
const subscribers = new Set<() => void>();

function setState(change: (current: State) => Partial<State>) {
  state = { ...state, ...change(state) };
  subscribers.forEach(notify => notify());
}

function subscribe(notify: () => void) {
  subscribers.add(notify);
  return () => {
    subscribers.delete(notify);
  };
}

let nextLogId = 0;
function addLog(event: string, data?: unknown) {
  const entry = {
    id: nextLogId++,
    time: new Date().toLocaleTimeString(),
    event,
    data: data === undefined ? undefined : JSON.stringify(data),
  };
  setState(({ log }) => ({ log: [entry, ...log].slice(0, 100) }));
}

function addCall(call: Call) {
  setState(({ calls }) => ({ calls: calls.some(c => c.uuid === call.uuid) ? calls : [...calls, call] }));
}
function updateCall(uuid: string, change: Partial<Call>) {
  setState(({ calls }) => ({ calls: calls.map(c => (c.uuid === uuid.toLowerCase() ? { ...c, ...change } : c)) }));
}
function removeCall(uuid: string) {
  setState(({ calls }) => ({ calls: calls.filter(c => c.uuid !== uuid.toLowerCase()) }));
}

// setup() runs from the screen, once per mode (see useCallKeep); events replayed from before it wait.
let setUpFor: boolean | undefined;
let markSetUp = () => {};
const setUpOnce = new Promise<void>(resolve => {
  markSetUp = resolve;
});

// The example has no VoIP server: an outgoing call "connects" two seconds after it starts. An app
// reports these steps when its signaling says the remote side is ringing, then has answered.
function connectOutgoingCall(uuid: string) {
  RNCallKeep.reportConnectingOutgoingCallWithUUID(uuid); // iOS only
  setTimeout(() => {
    RNCallKeep.reportConnectedOutgoingCallWithUUID(uuid); // iOS: CallKit starts the call timer
    RNCallKeep.setCurrentCallActive(uuid); // Android: Telecom marks the call active
    updateCall(uuid, { state: 'active' });
  }, 2000);
}

const handlers: Handlers = {
  didReceiveStartCallAction: ({ handle, callUUID }) => {
    if (!callUUID) {
      // iOS, a call started from Recents, Contacts or Siri: the system only gives the number, and
      // the app starts the call itself.
      const uuid = newCallUUID();
      addCall({ uuid, handle, name: handle, direction: 'outgoing', state: 'connecting', held: false, muted: false });
      RNCallKeep.startCall(uuid, handle, handle, 'number', false);
      return;
    }
    // Sent for every call startCall() starts, and on Android for calls started from the dialer.
    addCall({
      uuid: callUUID.toLowerCase(),
      handle,
      name: handle,
      direction: 'outgoing',
      state: 'connecting',
      held: false,
      muted: false,
    });
    connectOutgoingCall(callUUID);
  },
  answerCall: ({ callUUID }) => {
    // Answered from the system call screen or with answerIncomingCall(). An app connects the media
    // here; on Android it then tells Telecom the call is active.
    RNCallKeep.setCurrentCallActive(callUUID);
    updateCall(callUUID, { state: 'active' });
  },
  endCall: ({ callUUID }) => removeCall(callUUID),
  didDisplayIncomingCall: ({ callUUID, error }) => {
    if (error) {
      removeCall(callUUID); // CallKit refused the call (Do Not Disturb, blocked number, ...)
    }
  },
  didToggleHoldCallAction: ({ hold, callUUID }) => updateCall(callUUID, { held: hold }),
  didPerformSetMutedCallAction: ({ muted, callUUID }) => updateCall(callUUID, { muted }),
  // Android, self-managed: the app shows its own incoming-call screen. Here, the call's Answer and
  // Decline buttons.
  showIncomingCallUi: ({ callUUID, handle, name }) =>
    addCall({
      uuid: callUUID.toLowerCase(),
      handle,
      name,
      direction: 'incoming',
      state: 'ringing',
      held: false,
      muted: false,
    }),
  createIncomingConnectionFailed: ({ callUUID }) => removeCall(callUUID),
  didResetProvider: () => setState(() => ({ calls: [] })),
  didLoadWithEvents: events => {
    // These can come before setup() has run, as the listeners exist from startup: a call started
    // from Recents on a cold start (iOS) needs it to start the call.
    setUpOnce.then(() => {
      for (const { name, data } of events) {
        const event = REPLAYED[name];
        if (event) {
          (handlers[event] as ((payload: unknown) => void) | undefined)?.(data);
        }
      }
      RNCallKeep.clearInitialEvents();
    });
  },
};

// Called once from index.js, before React renders: the listeners stay for the life of the JavaScript
// runtime, whatever happens to the screen.
export function startCallKeep() {
  for (const event of EVENTS) {
    RNCallKeep.addEventListener(event, data => {
      addLog(event, data);
      (handlers[event] as ((payload: unknown) => void) | undefined)?.(data);
    });
  }
}

async function setup(selfManaged: boolean) {
  setUpFor = selfManaged;
  await requestAndroidPermissions();
  try {
    await RNCallKeep.setup(setupOptions(selfManaged));
    RNCallKeep.setAvailable(true); // Android: the app can take calls
    setState(() => ({ ready: true }));
    addLog('setup done', Platform.OS === 'android' ? { selfManaged } : undefined);
    markSetUp();
  } catch (error) {
    addLog('setup failed', String(error));
  }
}

function displayIncomingCall() {
  const uuid = newCallUUID();
  addCall({ uuid, handle: '5551234', name: 'Alice (example)', direction: 'incoming', state: 'ringing', held: false, muted: false });
  RNCallKeep.displayIncomingCall(uuid, '5551234', 'Alice (example)', 'number', false);
}

function startCall() {
  const uuid = newCallUUID();
  addCall({ uuid, handle: '5555678', name: 'Bob (example)', direction: 'outgoing', state: 'connecting', held: false, muted: false });
  RNCallKeep.startCall(uuid, '5555678', 'Bob (example)', 'number', false);
}

async function loadAudioRoutes() {
  const routes = await RNCallKeep.getAudioRoutes();
  setState(() => ({ audioRoutes: routes }));
  addLog('getAudioRoutes', routes);
}

export function useCallKeep() {
  const { ready, selfManaged, calls, log, audioRoutes } = useSyncExternalStore(subscribe, () => state);

  // setup() needs the activity on Android, where it may ask for permissions: the screen runs it, once
  // per mode, and not again when a new screen replaces it.
  useEffect(() => {
    if (setUpFor !== selfManaged) {
      setup(selfManaged);
    }
  }, [selfManaged]);

  return {
    ready,
    selfManaged,
    // Android: setup() runs again in the other mode.
    setSelfManaged: (value: boolean) => setState(() => ({ selfManaged: value, ready: false })),
    calls,
    log,
    audioRoutes,
    displayIncomingCall,
    startCall,
    // Each of these is followed by an event (answerCall, endCall, ...) that updates the call list.
    answer: (call: Call) => RNCallKeep.answerIncomingCall(call.uuid),
    decline: (call: Call) => RNCallKeep.rejectCall(call.uuid),
    end: (call: Call) => RNCallKeep.endCall(call.uuid),
    toggleHold: (call: Call) => {
      RNCallKeep.setOnHold(call.uuid, !call.held);
      updateCall(call.uuid, { held: !call.held });
    },
    toggleMute: (call: Call) => {
      RNCallKeep.setMutedCall(call.uuid, !call.muted);
      updateCall(call.uuid, { muted: !call.muted });
    },
    sendDTMF: (call: Call) => RNCallKeep.sendDTMF(call.uuid, '1'),
    // The remote side hung up: report it. CallKit and Telecom don't send endCall for this.
    remoteEnd: (call: Call) => {
      RNCallKeep.reportEndCallWithUUID(call.uuid, CONSTANTS.END_CALL_REASONS.REMOTE_ENDED);
      removeCall(call.uuid);
    },
    loadAudioRoutes,
    selectAudioRoute: (call: Call, route: AudioRoute) => RNCallKeep.setAudioRoute(call.uuid, route.name),
  };
}
