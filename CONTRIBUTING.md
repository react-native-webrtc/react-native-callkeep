# Contributing to react-native-callkeep

Thank you for your interest in contributing! This guide explains how the library is laid out, how to try a change in an app, and what we look for in a pull request.

## Quick Start

react-native-callkeep is a native module: it only runs inside a React Native app. To work on it, link your clone into an app and rebuild that app.

```bash
# 1. Fork and clone
git clone https://github.com/<you>/react-native-callkeep.git

# 2. In a React Native app, install your clone instead of the npm release
cd my-app
npm install ../react-native-callkeep

# 3. Rebuild the native app after every native change
cd ios && pod install && cd ..
npx react-native run-ios        # or run-android
```

JavaScript changes reload with Metro; Objective-C and Java changes need a native rebuild. If Metro does not follow the symlink, install a tarball instead: run `npm pack` in your clone and `npm install /path/to/react-native-callkeep-<version>.tgz` in the app.

## Where Things Live

| Path | What it is |
|------|------------|
| `index.js` | The JavaScript API (`RNCallKeep`) |
| `actions.js` | Event names and listeners |
| `index.d.ts` | TypeScript typings for the API and the event payloads |
| `ios/RNCallKeep/RNCallKeep.m` | iOS: CallKit provider, call actions and audio session |
| `android/src/main/java/io/wazo/callkeep/` | Android: `RNCallKeepModule` (the module), `VoiceConnectionService` and `VoiceConnection` (Telecom) |
| `docs/` | Installation guides |
| `tests/android/` | Android instrumentation tests, run inside a React Native host app (see its README) |

## Finding Something to Work On

Look for issues labeled **help wanted** or **good first issue**. If you plan a larger change, open an issue first so we can agree on the approach before you write the code.

## Making a Change

1. **Create a branch** from `master`.
2. **Think about both platforms.** A new method or event should work on iOS and Android, or say clearly which platform it supports.
3. **Update everything a user sees** when you change the API or an event:
   - `index.js` and, for events, `actions.js`
   - `index.d.ts`
   - `README.md`: the method or event table and its section
4. **Add a line to `CHANGELOG.md`** under `[Unreleased]`, saying what changed for users.
5. **Open a pull request** that explains what changed, why, and how you tested it.

## Testing

There is no simulator-only shortcut for calls, so tell us in the pull request what you tested on: device or emulator, OS version, React Native version.

- **iOS:** test on a real device. The iOS Simulator has no CallKit service, so call actions fail there.
- **Android:** a device or an emulator works. Android 6–11 and Android 12+ use different call-state APIs (`PhoneStateListener` and `TelephonyCallback`); if you touch that code, test one of each. Self-managed and managed modes also take different paths.
- **Instrumentation tests:** `tests/android/` holds tests that run against the real module inside a React Native host app. Its README explains the setup. Add one when you fix an Android bug that a test can show.

## Code Style

- Match the code around your change: naming, comment density, and idioms (for example, the Android sources post work with anonymous `Runnable`s rather than lambdas).
- Keep the diff to your change. Reformatting or re-indenting a whole file hides the real change and conflicts with every other open pull request.
- Don't bump the version in `package.json`; maintainers do that when they release.

## Pull Request Guidelines

- **Keep PRs focused**: one logical change per pull request.
- **Write a clear description**: what changed, why, and how you tested it.
- **Don't break the public API**: if a method's arguments, return value or an event's payload must change, discuss it in an issue first.
- **Allow edits by maintainers** on your pull request, so small fixes can be pushed without another round trip.

## Reporting Security Issues

Please don't open a public issue: see [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contributions will be licensed under the project's license (ISC OR MIT, see [LICENSE](LICENSE) and the README).
