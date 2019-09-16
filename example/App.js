import React, { useState, useEffect } from 'react';
import { Platform, StyleSheet, Text, View, TouchableOpacity, ScrollView } from 'react-native';
import uuid from 'uuid';
import RNCallKeep from 'react-native-callkeep';
import BackgroundTimer from 'react-native-background-timer';
import DeviceInfo from 'react-native-device-info';

BackgroundTimer.start();

const hitSlop = { top: 10, left: 10, right: 10, bottom: 10};
const styles = StyleSheet.create({
  container: {
    flex: 1,
    marginTop: 20,
    backgroundColor: '#fff',
    alignItems: 'center',
    justifyContent: 'center',
  },
  button: {
    marginTop: 20,
    marginBottom: 20,
  },
  callButtons: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingHorizontal: 30,
    width: '100%',
  },
  logContainer: {
    flex: 3,
    width: '100%',
    backgroundColor: '#D9D9D9',
  },
  log: {
    fontSize: 10,
  }
});

RNCallKeep.setup({
  ios: {
    appName: 'CallKeepDemo',
  },
  android: {
     alertTitle: 'Permissions required',
    alertDescription: 'This application needs to access your phone accounts',
    cancelButton: 'Cancel',
    okButton: 'ok',
  },
});

const getNewUuid = () => uuid.v4().toLowerCase();

const format = uuid => uuid.split('-')[0];

const getRandomNumber = () => String(Math.floor(Math.random() * 100000));

const isIOS = Platform.OS === 'ios';

export default function App() {
  const [logText, setLog] = useState('');
  const [heldCalls, setHeldCalls] = useState({}); // callKeep uuid: held
  const [mutedCalls, setMutedCalls] = useState({}); // callKeep uuid: muted
  const [calls, setCalls] = useState({}); // callKeep uuid: number

  const log = (text) => {
    console.info(text);
    setLog(logText + "\n" + text);
  };

  const addCall = (callUUID, number) => {
    setHeldCalls({ ...heldCalls, [callUUID]: false });
    setCalls({ ...calls, [callUUID]: number });
  };

  const removeCall = (callUUID) => {
    const { [callUUID]: _, ...updated } = calls;
    const { [callUUID]: __, ...updatedHeldCalls } = heldCalls;

    setCalls(updated);
    setCalls(updatedHeldCalls);
  };

  const setCallHeld = (callUUID, held) => {
    setHeldCalls({ ...heldCalls, [callUUID]: held });
  };

  const setCallMuted = (callUUID, muted) => {
    setMutedCalls({ ...mutedCalls, [callUUID]: muted });
  };

  const displayIncomingCall = (number) => {
    const callUUID = getNewUuid();
    addCall(callUUID, number);

    log(`[displayIncomingCall] ${format(callUUID)}, number: ${number}`);

    RNCallKeep.displayIncomingCall(callUUID, number, number, 'number', false);
  };

  const displayIncomingCallNow = () => {
    displayIncomingCall(getRandomNumber());
  };

  const displayIncomingCallDelayed = () => {
    BackgroundTimer.setTimeout(() => {
      displayIncomingCall(getRandomNumber());
    }, 3000);
  };

  const answerCall = ({ callUUID }) => {
    const number = calls[callUUID];
    log(`[answerCall] ${format(callUUID)}, number: ${number}`);

    RNCallKeep.startCall(callUUID, number, number);

    BackgroundTimer.setTimeout(() => {
      log(`[setCurrentCallActive] ${format(callUUID)}, number: ${number}`);
      RNCallKeep.setCurrentCallActive(callUUID);
    }, 1000);
  };

  const didPerformDTMFAction = ({ callUUID, digits }) => {
    const number = calls[callUUID];
    log(`[didPerformDTMFAction] ${format(callUUID)}, number: ${number} (${digits})`);
  };

  const didReceiveStartCallAction = ({ handle }) => {
    if (!handle) {
      // @TODO: sometime we receive `didReceiveStartCallAction` with handle` undefined`
      return;
    }
    const callUUID = getNewUuid();
    addCall(callUUID, handle);

    log(`[didReceiveStartCallAction] ${callUUID}, number: ${handle}`);

    RNCallKeep.startCall(callUUID, handle, handle);

    BackgroundTimer.setTimeout(() => {
      log(`[setCurrentCallActive] ${format(callUUID)}, number: ${handle}`);
      RNCallKeep.setCurrentCallActive(callUUID);
    }, 1000);
  };

  const didPerformSetMutedCallAction = ({ muted, callUUID }) => {
    const number = calls[callUUID];
    log(`[didPerformSetMutedCallAction] ${format(callUUID)}, number: ${number} (${muted})`);

    setCallMuted(callUUID, muted);
  };

  const didToggleHoldCallAction = ({ hold, callUUID }) => {
    const number = calls[callUUID];
    log(`[didToggleHoldCallAction] ${format(callUUID)}, number: ${number} (${hold})`);

    setCallHeld(callUUID, hold);
  };

  const endCall = ({ callUUID }) => {
    const handle = calls[callUUID];
    log(`[endCall] ${format(callUUID)}, number: ${handle}`);

    removeCall(callUUID);
  };

  const hangup = (callUUID) => {
    RNCallKeep.endCall(callUUID);
    removeCall(callUUID);
  };

  const setOnHold = (callUUID, held) => {
    const handle = calls[callUUID];
    RNCallKeep.setOnHold(callUUID, held);
    log(`[setOnHold: ${held}] ${format(callUUID)}, number: ${handle}`);

    setCallHeld(callUUID, held);
  };

  const setOnMute = (callUUID, muted) => {
    const handle = calls[callUUID];
    RNCallKeep.setMutedCall(callUUID, muted);
    log(`[setMutedCall: ${muted}] ${format(callUUID)}, number: ${handle}`);

    setCallMuted(callUUID, muted);
  };

  const updateDisplay = (callUUID) => {
    const number = calls[callUUID];
    // Workaround because Android doesn't display well displayName, se we have to switch ...
    if (isIOS) {
      RNCallKeep.updateDisplay(callUUID, 'New Name', number);
    } else {
      RNCallKeep.updateDisplay(callUUID, number, 'New Name');
    }

    log(`[updateDisplay: ${number}] ${format(callUUID)}`);
  };

  useEffect(() => {
    RNCallKeep.addEventListener('answerCall', answerCall);
    RNCallKeep.addEventListener('didPerformDTMFAction', didPerformDTMFAction);
    RNCallKeep.addEventListener('didReceiveStartCallAction', didReceiveStartCallAction);
    RNCallKeep.addEventListener('didPerformSetMutedCallAction', didPerformSetMutedCallAction);
    RNCallKeep.addEventListener('didToggleHoldCallAction', didToggleHoldCallAction);
    RNCallKeep.addEventListener('endCall', endCall);

    return () => {
      RNCallKeep.removeEventListener('answerCall', answerCall);
      RNCallKeep.removeEventListener('didPerformDTMFAction', didPerformDTMFAction);
      RNCallKeep.removeEventListener('didReceiveStartCallAction', didReceiveStartCallAction);
      RNCallKeep.removeEventListener('didPerformSetMutedCallAction', didPerformSetMutedCallAction);
      RNCallKeep.removeEventListener('didToggleHoldCallAction', didToggleHoldCallAction);
      RNCallKeep.removeEventListener('endCall', endCall);
    }
  }, []);

  if (isIOS && DeviceInfo.isEmulator()) {
    return <Text style={styles.container}>CallKeep doesn't work on iOS emulator</Text>;
  }

  return (
    <View style={styles.container}>
      <TouchableOpacity onPress={displayIncomingCallNow} style={styles.button} hitSlop={hitSlop}>
        <Text>Display incoming call now</Text>
      </TouchableOpacity>

      <TouchableOpacity onPress={displayIncomingCallDelayed} style={styles.button} hitSlop={hitSlop}>
        <Text>Display incoming call now in 3s</Text>
      </TouchableOpacity>

      {Object.keys(calls).map(callUUID => (
        <View key={callUUID} style={styles.callButtons}>
          <TouchableOpacity
            onPress={() => setOnHold(callUUID, !heldCalls[callUUID])}
            style={styles.button}
            hitSlop={hitSlop}
          >
            <Text>{heldCalls[callUUID] ? 'Unhold' : 'Hold'} {calls[callUUID]}</Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => updateDisplay(callUUID)}
            style={styles.button}
            hitSlop={hitSlop}
          >
            <Text>Update display</Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => setOnMute(callUUID, !mutedCalls[callUUID])}
            style={styles.button}
            hitSlop={hitSlop}
          >
            <Text>{mutedCalls[callUUID] ? 'Unmute' : 'Mute'} {calls[callUUID]}</Text>
          </TouchableOpacity>

          <TouchableOpacity onPress={() => hangup(callUUID)} style={styles.button} hitSlop={hitSlop}>
            <Text>Hangup {calls[callUUID]}</Text>
          </TouchableOpacity>
        </View>
      ))}

      <ScrollView style={styles.logContainer}>
        <Text style={styles.log}>
          {logText}
        </Text>
      </ScrollView>
    </View>
  );
}
