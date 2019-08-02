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

export default function App() {
  const [logText, setLog] = useState('');
  const [calls, setCalls] = useState({}); // callKeep uuid: number

  const log = (text) => {
    console.info(text);
    setLog(logText + "\n" + text);
  };

  const addCall = (callUUID, number) => setCalls({ ...calls, [callUUID]: number });
  const removeCall = (callUUID) => {
    const { [callUUID]: _, ...updated } = calls;
    setCalls(updated);
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
    RNCallKeep.setCurrentCallActive(callUUID);
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
    RNCallKeep.setCurrentCallActive(callUUID);
  };

  const didPerformSetMutedCallAction = ({ muted, callUUID }) => {
    const number = calls[callUUID];
    log(`[didPerformSetMutedCallAction] ${format(callUUID)}, number: ${number} (${muted})`);
  };

  const didToggleHoldCallAction = ({ hold, callUUID }) => {
    const number = calls[callUUID];
    log(`[didToggleHoldCallAction] ${format(callUUID)}, number: ${number} (${hold})`);
  };

  const endCall = ({ callUUID }) => {
    const handle = calls[callUUID];
    log(`[endCall] ${format(callUUID)}, number: ${handle}`);
    console.log('handle', handle);

    removeCall(callUUID);
  };

  const hangup = (callUUID) => {
    RNCallKeep.endCall(callUUID);
    removeCall(callUUID);
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
  });

  if (Platform.OS === 'ios' && DeviceInfo.isEmulator()) {
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
        <TouchableOpacity key={callUUID} onPress={() => hangup(callUUID)} style={styles.button} hitSlop={hitSlop}>
          <Text>Hangup {calls[callUUID]}</Text>
        </TouchableOpacity>
      ))}

      <ScrollView style={styles.logContainer}>
        <Text style={styles.log}>
          {logText}
        </Text>
      </ScrollView>
    </View>
  );
}
