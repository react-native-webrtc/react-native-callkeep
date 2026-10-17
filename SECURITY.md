# Security Policy

## Supported Versions

Security fixes go into the latest 4.x release. Older major versions are no longer maintained.

| Version | Supported |
|---------|-----------|
| 4.x (latest) | ✅ |
| < 4.0 | ❌ |

## Reporting a Vulnerability

If you discover a security vulnerability in react-native-callkeep, please report it privately through GitHub:

**[Report a vulnerability](https://github.com/react-native-webrtc/react-native-callkeep/security/advisories/new)**

Please include:

- A description of the vulnerability
- Steps to reproduce the issue (platform and OS version, React Native version, react-native-callkeep version)
- Any potential impact

We will acknowledge your report within 7 days and work to address the issue promptly.

**Please do not open public GitHub issues for security vulnerabilities.**

## Scope

react-native-callkeep connects your app to CallKit on iOS and to ConnectionService (Telecom) on Android. It does not carry media, signaling or network traffic: those belong to your VoIP stack (for example react-native-webrtc or your SIP library), and vulnerabilities there should be reported to that project.

In scope are issues in the library itself, for example:

- call data (numbers, caller names, payloads) exposed by the library to other apps or to logs
- permissions or exported Android components broader than the feature needs
- a way to start, answer or end calls without going through your app
