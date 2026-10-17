const path = require('path');
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');

// The example runs react-native-callkeep from this repository (the folder above), not from npm,
// so a change to the library shows up in the app on the next reload.
const root = path.resolve(__dirname, '..');
const escape = (value) => value.replace(/[/\\^$*+?.()|[\]{}]/g, '\\$&');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const config = {
  watchFolders: [root],
  resolver: {
    extraNodeModules: { 'react-native-callkeep': root },
    // The library's own imports (react-native) resolve to the example's node_modules.
    nodeModulesPaths: [path.resolve(__dirname, 'node_modules')],
    // A node_modules folder in the repository root would load React Native twice.
    blockList: [new RegExp(`^${escape(path.join(root, 'node_modules'))}\\/.*$`)],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
