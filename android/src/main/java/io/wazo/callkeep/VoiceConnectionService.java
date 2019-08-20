/*
 * Copyright (c) 2016-2019 The CallKeep Authors (see the AUTHORS file)
 * SPDX-License-Identifier: ISC, MIT
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package io.wazo.callkeep;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.Context;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.Voice;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.ConnectionRequest;
import android.telecom.ConnectionService;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.Log;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.LifecycleState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.wazo.callkeep.RNCallKeepModule.ACTION_ANSWER_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_AUDIO_SESSION;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_DTMF_TONE;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_END_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_HOLD_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_MUTE_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_ONGOING_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_UNHOLD_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_UNMUTE_CALL;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALLER_NAME;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_NUMBER;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_UUID;
import static io.wazo.callkeep.RNCallKeepModule.handle;

// @see https://github.com/kbagchiGWC/voice-quickstart-android/blob/9a2aff7fbe0d0a5ae9457b48e9ad408740dfb968/exampleConnectionService/src/main/java/com/twilio/voice/examples/connectionservice/VoiceConnectionService.java
@TargetApi(Build.VERSION_CODES.M)
public class VoiceConnectionService extends ConnectionService {
    private static Boolean isAvailable = false;
    private static String TAG = "RNCK:VoiceConnectionService";
    public static Map<String, VoiceConnection> currentConnections = new HashMap<>();

    public static Connection getConnection(String connectionId) {
        if (currentConnections.containsKey(connectionId)) {
            return currentConnections.get(connectionId);
        }
        return null;
    }

    public VoiceConnectionService() {
        super();
        Log.e(TAG, "Constructor");
    }

    public static void setAvailable(Boolean value) {
        isAvailable = value;
    }


    public static void deinitConnection(String connectionId) {
        Log.d(TAG, "deinitConnection:" + connectionId);
        if (currentConnections.containsKey(connectionId)) {
            currentConnections.remove(connectionId);
        }
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Bundle extra = request.getExtras();
        Uri number = request.getAddress();
        String name = extra.getString(EXTRA_CALLER_NAME);
        Connection incomingCallConnection = createConnection(request);
        incomingCallConnection.setRinging();
        incomingCallConnection.setInitialized();

        return incomingCallConnection;
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Bundle extras = request.getExtras();
        Connection outgoingCallConnection = null;
        String number = request.getAddress().getSchemeSpecificPart();
        String extrasNumber = extras.getString(EXTRA_CALL_NUMBER);
        String displayName = extras.getString(EXTRA_CALLER_NAME);
        String uuid = UUID.randomUUID().toString();

         Log.d(TAG, "onCreateOutgoingConnection:" + uuid + ", number: " + number);

        // Wakeup application if needed
        if (!VoiceConnectionService.isRunning(this.getApplicationContext())) {
            Log.d(TAG, "onCreateOutgoingConnection: Waking up application");
            Intent headlessIntent = new Intent(
                this.getApplicationContext(),
                RNCallKeepBackgroundMessagingService.class
            );
            headlessIntent.putExtra("callUUID", uuid);
            headlessIntent.putExtra("name", displayName);
            headlessIntent.putExtra("handle", number);
            ComponentName name = this.getApplicationContext().startService(headlessIntent);
            if (name != null) {
              HeadlessJsTaskService.acquireWakeLockNow(this.getApplicationContext());
            }
        } else if (!this.canMakeOutgoingCall()) {
            return Connection.createFailedConnection(new DisconnectCause(DisconnectCause.LOCAL));
        }

        // TODO: Hold all other calls
        if (extrasNumber != null && extrasNumber.equals(number)) {
            outgoingCallConnection = createConnection(request);
        } else {
            extras.putString(EXTRA_CALL_UUID, uuid);
            extras.putString(EXTRA_CALLER_NAME, displayName);
            extras.putString(EXTRA_CALL_NUMBER, number);
            outgoingCallConnection = createConnection(request);
        }

        outgoingCallConnection.setDialing();
        outgoingCallConnection.setAudioModeIsVoip(true);
        outgoingCallConnection.setCallerDisplayName(displayName, TelecomManager.PRESENTATION_ALLOWED);
        outgoingCallConnection.setInitialized();

        HashMap<String, String> extrasMap = this.bundleToMap(extras);

        sendCallRequestToActivity(ACTION_ONGOING_CALL, extrasMap);
        sendCallRequestToActivity(ACTION_AUDIO_SESSION, null);

        return outgoingCallConnection;
    }

    private Boolean canMakeOutgoingCall() {
        return isAvailable;
    }

    private Connection createConnection(ConnectionRequest request) {

        Bundle extras = request.getExtras();
        HashMap<String, String> extrasMap = this.bundleToMap(extras);
        extrasMap.put(EXTRA_CALL_NUMBER, request.getAddress().toString());
        VoiceConnection connection = new VoiceConnection(this, extrasMap);
        connection.setConnectionCapabilities(Connection.CAPABILITY_MUTE | Connection.CAPABILITY_SUPPORT_HOLD);
        connection.setInitializing();
        connection.setExtras(extras);
        currentConnections.put(extras.getString(EXTRA_CALL_UUID), connection);

        // Get other connections for conferencing
        Map<String, VoiceConnection> otherConnections = new HashMap<>();
        for (Map.Entry<String, VoiceConnection> entry : currentConnections.entrySet()) {
            if(!(extras.getString(EXTRA_CALL_UUID).equals(entry.getKey()))) {
                otherConnections.put(entry.getKey(), entry.getValue());
            }
        }
        List<Connection> conferenceConnections = new ArrayList<Connection>(otherConnections.values());
        connection.setConferenceableConnections(conferenceConnections);

        return connection;
    }

    @Override
    public void onConference(Connection connection1, Connection connection2) {
        super.onConference(connection1, connection2);
        VoiceConnection voiceConnection1 = (VoiceConnection) connection1;
        VoiceConnection voiceConnection2 = (VoiceConnection) connection2;

        PhoneAccountHandle phoneAccountHandle = RNCallKeepModule.handle;

        VoiceConference voiceConference = new VoiceConference(handle);
        voiceConference.addConnection(voiceConnection1);
        voiceConference.addConnection(voiceConnection2);

        connection1.onUnhold();
        connection2.onUnhold();

        this.addConference(voiceConference);
    }

    /*
     * Send call request to the RNCallKeepModule
     */
    private void sendCallRequestToActivity(final String action, @Nullable final HashMap attributeMap) {
        final VoiceConnectionService instance = this;
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(action);
                if (attributeMap != null) {
                    Bundle extras = new Bundle();
                    extras.putSerializable("attributeMap", attributeMap);
                    intent.putExtras(extras);
                }
                LocalBroadcastManager.getInstance(instance).sendBroadcast(intent);
            }
        });
    }

    private HashMap<String, String> bundleToMap(Bundle extras) {
        HashMap<String, String> extrasMap = new HashMap<>();
        Set<String> keySet = extras.keySet();
        Iterator<String> iterator = keySet.iterator();

        while(iterator.hasNext()) {
            String key = iterator.next();
            if (extras.get(key) != null) {
                extrasMap.put(key, extras.get(key).toString());
            }
        }
        return extrasMap;
    }

    /**
     * https://stackoverflow.com/questions/5446565/android-how-do-i-check-if-activity-is-running
     *
     * @param context Context
     * @return boolean
     */
    public static boolean isRunning(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningTaskInfo> tasks = activityManager.getRunningTasks(Integer.MAX_VALUE);

        for (RunningTaskInfo task : tasks) {
            if (context.getPackageName().equalsIgnoreCase(task.baseActivity.getPackageName()))
                return true;
        }

        return false;
    }
}
