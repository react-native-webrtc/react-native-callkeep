/**
 * react-native-callkeep example: simulated calls through CallKit (iOS) and Telecom (Android).
 * There is no VoIP server, so no audio flows; see README.md for what each part shows.
 */
import React from 'react';
import { Platform, Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import { Call, useCallKeep } from './src/useCallKeep';

function Button({ title, onPress, disabled }: { title: string; onPress: () => void; disabled?: boolean }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={title}
      disabled={disabled}
      onPress={onPress}
      style={({ pressed }) => [styles.button, pressed && styles.pressed, disabled && styles.disabled]}>
      <Text style={styles.buttonText}>{title}</Text>
    </Pressable>
  );
}

function Section({ title, hint, children }: { title: string; hint?: string; children: React.ReactNode }) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>{title}</Text>
      {hint ? <Text style={styles.hint}>{hint}</Text> : null}
      {children}
    </View>
  );
}

export default function App() {
  const callkeep = useCallKeep();
  const activeCall = callkeep.calls.find(call => call.state === 'active');

  const callActions = (call: Call) =>
    call.state === 'ringing' ? (
      <View style={styles.row}>
        <Button title="Answer" onPress={() => callkeep.answer(call)} />
        <Button title="Decline" onPress={() => callkeep.decline(call)} />
      </View>
    ) : (
      <View style={styles.row}>
        <Button title={call.held ? 'Resume' : 'Hold'} onPress={() => callkeep.toggleHold(call)} />
        <Button title={call.muted ? 'Unmute' : 'Mute'} onPress={() => callkeep.toggleMute(call)} />
        <Button title="DTMF 1" onPress={() => callkeep.sendDTMF(call)} />
        <Button title="Hang up" onPress={() => callkeep.end(call)} />
        <Button title="Remote hangs up" onPress={() => callkeep.remoteEnd(call)} />
      </View>
    );

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.screen}>
        <ScrollView contentContainerStyle={styles.content}>
          <Text style={styles.title}>CallKeep Example</Text>
          <Text style={styles.hint}>{callkeep.ready ? 'Ready' : 'Setting up…'}</Text>

          {Platform.OS === 'android' ? (
            <Section
              title="Android mode"
              hint="Self-managed: the app draws its own incoming-call screen. Managed: Android shows its call screen once the phone account is enabled.">
              <View style={styles.row}>
                <Text style={styles.label}>Self-managed</Text>
                <Switch
                  accessibilityLabel="Self-managed"
                  value={callkeep.selfManaged}
                  onValueChange={callkeep.setSelfManaged}
                />
              </View>
            </Section>
          ) : null}

          <Section
            title="New call"
            hint="Lock the screen while a call rings to see the lock-screen call UI. JavaScript doesn't run while the app is in the background, so real apps report incoming calls from a push (see README.md).">
            <View style={styles.row}>
              <Button title="Incoming call" disabled={!callkeep.ready} onPress={callkeep.displayIncomingCall} />
              <Button title="Outgoing call" disabled={!callkeep.ready} onPress={callkeep.startCall} />
            </View>
          </Section>

          <Section title="Calls" hint={callkeep.calls.length ? undefined : 'No call.'}>
            {callkeep.calls.map(call => (
              <View key={call.uuid} style={styles.call}>
                <Text style={styles.label}>
                  {call.direction === 'incoming' ? '↙' : '↗'} {call.name} · {call.state}
                  {call.held ? ' · on hold' : ''}
                  {call.muted ? ' · muted' : ''}
                </Text>
                <Text style={styles.uuid}>{call.uuid}</Text>
                {callActions(call)}
              </View>
            ))}
          </Section>

          <Section title="Audio routes" hint="During a call: the speaker, the receiver, Bluetooth or a headset.">
            <View style={styles.row}>
              <Button title="Load routes" onPress={callkeep.loadAudioRoutes} />
              {callkeep.audioRoutes.map(route => (
                <Button
                  key={route.name}
                  title={`${route.selected ? '● ' : ''}${route.name}`}
                  disabled={!activeCall}
                  onPress={() => activeCall && callkeep.selectAudioRoute(activeCall, route)}
                />
              ))}
            </View>
          </Section>

          <Section title="Events" hint="Every event the library sends, newest first.">
            {callkeep.log.map(entry => (
              <Text key={entry.id} style={styles.log}>
                <Text style={styles.logTime}>{entry.time} </Text>
                <Text style={styles.logEvent}>{entry.event}</Text>
                {entry.data ? ` ${entry.data}` : ''}
              </Text>
            ))}
          </Section>
        </ScrollView>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: '#f4f5f7' },
  content: { padding: 16, gap: 16 },
  title: { fontSize: 24, fontWeight: '700', color: '#111' },
  section: { backgroundColor: '#fff', borderRadius: 12, padding: 12, gap: 8 },
  sectionTitle: { fontSize: 17, fontWeight: '600', color: '#111' },
  hint: { fontSize: 13, color: '#555' },
  row: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: 8 },
  label: { fontSize: 15, color: '#111' },
  button: { backgroundColor: '#1f6feb', borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8 },
  pressed: { opacity: 0.7 },
  disabled: { backgroundColor: '#9ab' },
  buttonText: { color: '#fff', fontSize: 14, fontWeight: '600' },
  call: { borderTopWidth: StyleSheet.hairlineWidth, borderTopColor: '#ccd', paddingTop: 8, gap: 6 },
  uuid: { fontSize: 11, color: '#777', fontFamily: Platform.select({ ios: 'Menlo', default: 'monospace' }) },
  log: { fontSize: 12, color: '#333', fontFamily: Platform.select({ ios: 'Menlo', default: 'monospace' }) },
  logTime: { color: '#888' },
  logEvent: { fontWeight: '700', color: '#111' },
});
