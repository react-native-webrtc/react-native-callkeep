const path = require('path');

// Autolinks the native code of react-native-callkeep from this repository (the folder above):
// the iOS pod (RNCallKeep.podspec) and the Android library (android/).
module.exports = {
  dependencies: {
    'react-native-callkeep': {
      root: path.join(__dirname, '..'),
      platforms: {
        ios: {},
        android: {},
      },
    },
  },
};
