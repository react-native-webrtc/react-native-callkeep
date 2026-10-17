# Contributing to react-native-callkeep

Thank you for your interest in contributing! This guide explains how the library is laid out, how to try a change in an app, and what we look for in a pull request.

Everyone taking part in the project, in issues, pull requests or elsewhere, is expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Quick Start

react-native-callkeep is a native module: it only runs inside a React Native app. The [example app](example/) uses this repository's code directly, so it is the quickest way to try a change:

```bash
# 1. Fork and clone
git clone https://github.com/<you>/react-native-callkeep.git
cd react-native-callkeep/example

# 2. Install and run (iOS needs a real device for calls; see example/README.md)
npm install
npm run android                 # or: cd ios && pod install && cd .. && npm run ios -- --device
```

JavaScript changes reload with Metro; Objective-C and Java changes need a native rebuild.

To try a change in your own app instead, install your clone there (`npm install ../react-native-callkeep`) and rebuild the app. If Metro does not follow the symlink, install a tarball: run `npm pack` in your clone and `npm install /path/to/react-native-callkeep-<version>.tgz` in the app.

## Where Things Live

| Path | What it is |
|------|------------|
| `index.js` | The JavaScript API (`RNCallKeep`) |
| `actions.js` | Event names and listeners |
| `index.d.ts` | TypeScript typings for the API and the event payloads |
| `ios/RNCallKeep/RNCallKeep.m` | iOS: CallKit provider, call actions and audio session |
| `android/src/main/java/io/wazo/callkeep/` | Android: `RNCallKeepModule` (the module), `VoiceConnectionService` and `VoiceConnection` (Telecom) |
| `docs/` | The documentation: setup, API reference, events, guides, troubleshooting |
| `example/` | The example app, which runs the library from this repository |
| `tests/android/` | Android instrumentation tests, run inside a React Native host app (see its README) |

## Finding Something to Work On

Look for issues labeled **help wanted** or **good first issue**. If you plan a larger change, open an issue first so we can agree on the approach before you write the code.

## Issue Policy

Open issues are the work we intend to look at, so they shouldn't pile up:

- An issue with **no activity for two years** gets the `stale` label and a comment.
- It is **closed 30 days later** unless someone comments. A comment keeps it open, so if the problem still happens with the latest release, say so, with your react-native-callkeep, React Native and OS versions.
- After it is closed, open a new issue with the details and a link to the old one (GitHub doesn't let authors reopen an issue someone else closed).
- Issues labeled **help wanted** or **good first issue** are never closed this way. Pull requests aren't either.

A GitHub Actions workflow (`.github/workflows/stale.yml`) applies this policy every day.

## Making a Change

1. **Create a branch** from `master`.
2. **Think about both platforms.** A new method or event should work on iOS and Android, or say clearly which platform it supports.
3. **Update everything a user sees** when you change the API or an event:
   - `index.js` and, for events, `actions.js`
   - `index.d.ts`
   - `docs/api.md` or `docs/events.md`: the table and the method's or event's section
4. **Add a line to `CHANGELOG.md`** under `[Unreleased]`, saying what changed for users.
5. **Run `npm run check`** (see below).
6. **Open a pull request** that explains what changed, why, and how you tested it.

## Checks

CI runs these on every pull request. Run them before you push; they need only Node.js and git:

```bash
npm run check   # all of the below
npm run check:types     # index.d.ts compiles with TypeScript in strict mode
npm run check:events    # every event matches across actions.js, index.d.ts and the docs/events.md table
npm run check:api       # every method matches across index.js, index.d.ts and the docs/api.md table
npm run check:links     # relative links and #anchors in the Markdown files point somewhere
npm run check:package   # what npm publish would ship: the native sources are in, tests and scripts are out
```

CI also evaluates `RNCallKeep.podspec` with CocoaPods and checks that the privacy manifest ships in its resource bundle.

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

## Releasing (maintainers)

1. In a pull request, set the new version in `package.json`, and in `CHANGELOG.md` turn `[Unreleased]` into the new version and date: keep an empty `[Unreleased]` above it, and update the links at the bottom.
2. Merge it, then tag the merge commit on `master` with the bare version and push the tag:

   ```bash
   git tag 4.4.0 <merge commit> && git push origin 4.4.0
   ```

3. The Release workflow checks that the tag matches `package.json` and is on `master`, runs `npm run check`, **stages** the version on npm (with provenance), and drafts the GitHub release from the changelog section.
4. Approve the staged version with your 2FA, which publishes it:

   ```bash
   npm stage list react-native-callkeep    # shows the stage id
   npm stage approve <stage-id>
   ```

   (or approve it on npmjs.com). To drop it instead: `npm stage reject <stage-id>`.
5. Publish the draft GitHub release.

The package's settings on npmjs.com trust this repository's `release.yml` (Trusted Publishing), so no npm token is stored in GitHub, and that trust only allows staging: nothing reaches npm users without a maintainer's approval.

## Reporting Security Issues

Please don't open a public issue: see [SECURITY.md](SECURITY.md).

## License

By contributing, you agree that your contributions will be licensed under the project's license (ISC OR MIT, see [LICENSE](LICENSE) and the README).
