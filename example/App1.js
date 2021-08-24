import React from 'react';
import {View, Text} from 'react-native';

import PushNotification, {Importance} from 'react-native-push-notification';

PushNotification.createChannel(
  {
    channelId: 'Callkit', // (required)
    channelName: 'Callkit', // (required)
    playSound: false, // (optional) default: true
    importance: Importance.HIGH, // (optional) default: Importance.HIGH. Int value of the Android notification importance
  },
  created => console.log(`createChannel returned '${created}'`), // (optional) callback returns whether the channel was created, false means it already existed.
);

export default function App() {
  return (
    <View style={{flex: 1}}>
      <Text>Test</Text>
    </View>
  );
}
