import { useCallback, useEffect, useRef, useState } from 'react';
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

export function useCallKeep() {
  const [selfManaged, setSelfManaged] = useState(true);
  const [ready, setReady] = useState(false);
  const [calls, setCalls] = useState<Call[]>([]);
  const [log, setLog] = useState<LogEntry[]>([]);
  const [audioRoutes, setAudioRoutes] = useState<AudioRoute[]>([]);
  const nextLogId = useRef(0);

  const addLog = useCallback((event: string, data?: unknown) => {
    const entry = {
      id: nextLogId.current++,
      time: new Date().toLocaleTimeString(),
      event,
      data: data === undefined ? undefined : JSON.stringify(data),
    };
    setLog(entries => [entry, ...entries].slice(0, 100));
  }, []);

  const addCall = useCallback((call: Call) => {
    setCalls(list => (list.some(c => c.uuid === call.uuid) ? list : [...list, call]));
  }, []);
  const updateCall = useCallback((uuid: string, change: Partial<Call>) => {
    setCalls(list => list.map(c => (c.uuid === uuid.toLowerCase() ? { ...c, ...change } : c)));
  }, []);
  const removeCall = useCallback((uuid: string) => {
    setCalls(list => list.filter(c => c.uuid !== uuid.toLowerCase()));
  }, []);

  // The example has no VoIP server: an outgoing call "connects" two seconds after it starts. An app
  // reports these steps when its signaling says the remote side is ringing, then has answered.
  const connectOutgoingCall = useCallback(
    (uuid: string) => {
      RNCallKeep.reportConnectingOutgoingCallWithUUID(uuid); // iOS only
      setTimeout(() => {
        RNCallKeep.reportConnectedOutgoingCallWithUUID(uuid); // iOS: CallKit starts the call timer
        RNCallKeep.setCurrentCallActive(uuid); // Android: Telecom marks the call active
        updateCall(uuid, { state: 'active' });
      }, 2000);
    },
    [updateCall],
  );

  const handlers = useRef<Handlers>({});
  handlers.current = {
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
    didResetProvider: () => setCalls([]),
    didLoadWithEvents: events => {
      for (const { name, data } of events) {
        const event = REPLAYED[name];
        if (event) {
          (handlers.current[event] as ((payload: unknown) => void) | undefined)?.(data);
        }
      }
      RNCallKeep.clearInitialEvents();
    },
  };

  useEffect(() => {
    const subscriptions = EVENTS.map(event =>
      RNCallKeep.addEventListener(event, data => {
        addLog(event, data);
        (handlers.current[event] as ((payload: unknown) => void) | undefined)?.(data);
      }),
    );
    return () => subscriptions.forEach(subscription => subscription.remove());
  }, [addLog]);

  useEffect(() => {
    let active = true;
    (async () => {
      await requestAndroidPermissions();
      try {
        await RNCallKeep.setup(setupOptions(selfManaged));
        RNCallKeep.setAvailable(true); // Android: the app can take calls
        if (active) {
          setReady(true);
          addLog('setup done', Platform.OS === 'android' ? { selfManaged } : undefined);
        }
      } catch (error) {
        addLog('setup failed', String(error));
      }
    })();
    return () => {
      active = false;
    };
  }, [selfManaged, addLog]);

  const displayIncomingCall = useCallback(() => {
    const uuid = newCallUUID();
    addCall({ uuid, handle: '5551234', name: 'Alice (example)', direction: 'incoming', state: 'ringing', held: false, muted: false });
    RNCallKeep.displayIncomingCall(uuid, '5551234', 'Alice (example)', 'number', false);
  }, [addCall]);

  const startCall = useCallback(() => {
    const uuid = newCallUUID();
    addCall({ uuid, handle: '5555678', name: 'Bob (example)', direction: 'outgoing', state: 'connecting', held: false, muted: false });
    RNCallKeep.startCall(uuid, '5555678', 'Bob (example)', 'number', false);
  }, [addCall]);

  const loadAudioRoutes = useCallback(async () => {
    const routes = await RNCallKeep.getAudioRoutes();
    setAudioRoutes(routes);
    addLog('getAudioRoutes', routes);
  }, [addLog]);

  return {
    ready,
    selfManaged,
    setSelfManaged,
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
