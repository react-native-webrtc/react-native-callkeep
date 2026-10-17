/**
 * @format
 */

import { AppRegistry } from 'react-native';
import App from './App';
import { name as appName } from './app.json';
import { startCallKeep } from './src/useCallKeep';

startCallKeep();
AppRegistry.registerComponent(appName, () => App);
