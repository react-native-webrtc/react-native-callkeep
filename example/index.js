/**
 * @format
 */

import {AppRegistry} from 'react-native';
import App from './App1';
import {name as appName} from './app.json';

AppRegistry.registerComponent(appName, () => App);

AppRegistry.registerHeadlessTask(
  'CallNotificationEventEmitter',
  () => async (taskData) => {
    try {
      console.log(
        'CallNotificationEventEmitter called from background service',
      );
      console.log('Second log');
      //   if (data) {
      //     console.log('has data');
      //   } else {
      //     console.log('No data');
      //   }
    } catch (error) {
      console.log(`Error trying to run headless task: ${error.message}`);
    }
    return new Promise(resolve => {
      setTimeout(() => {
        console.log('timer called CallNotificationEventEmitter');
        resolve();
      }, 2000);
    });
  },
);
