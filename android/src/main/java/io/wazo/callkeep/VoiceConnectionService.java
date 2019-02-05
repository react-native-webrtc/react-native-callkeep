/*
 * Copyright (c) 2016-2018 The CallKeep Authors (see the AUTHORS file)
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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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

// @see https://github.com/kbagchiGWC/voice-quickstart-android/blob/9a2aff7fbe0d0a5ae9457b48e9ad408740dfb968/exampleConnectionService/src/main/java/com/twilio/voice/examples/connectionservice/VoiceConnectionService.java
@TargetApi(Build.VERSION_CODES.M)
public class VoiceConnectionService extends ConnectionService {
    private static Connection connection;
    private static Boolean isAvailable = false;

    public static Connection getConnection() {
        return connection;
    }

    public static void setAvailable(Boolean value) {
        isAvailable = value;
    }


    public static void deinitConnection() {
        connection = null;
    }

    @Override
    public Connection onCreateIncomingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        Connection incomingCallConnection = createConnection(request);
        incomingCallConnection.setRinging();

        return incomingCallConnection;
    }

    @Override
    public Connection onCreateOutgoingConnection(PhoneAccountHandle connectionManagerPhoneAccount, ConnectionRequest request) {
        if (!this.canMakeOutgoingCall()) {
            return Connection.createFailedConnection(new DisconnectCause(DisconnectCause.LOCAL));
        }

        Connection outgoingCallConnection = createConnection(request);
        outgoingCallConnection.setDialing();
        outgoingCallConnection.setAudioModeIsVoip(true);

        sendCallRequestToActivity(ACTION_ONGOING_CALL, request.getAddress().getSchemeSpecificPart());
        sendCallRequestToActivity(ACTION_AUDIO_SESSION, null);

        return outgoingCallConnection;
    }

    private Boolean canMakeOutgoingCall() {
        return isAvailable;
    }

    private Connection createConnection(ConnectionRequest request) {
        connection = new Connection() {
            private boolean isMuted = false;

            @Override
            public void onCallAudioStateChanged(CallAudioState state) {
                if (state.isMuted() == this.isMuted) {
                    return;
                }

                this.isMuted = state.isMuted();

                sendCallRequestToActivity(isMuted ? ACTION_MUTE_CALL : ACTION_UNMUTE_CALL, null);
            }

            @Override
            public void onAnswer() {
                super.onAnswer();
                if (connection == null) {
                    return;
                }

                connection.setActive();
                connection.setAudioModeIsVoip(true);

                sendCallRequestToActivity(ACTION_ANSWER_CALL, null);
                sendCallRequestToActivity(ACTION_AUDIO_SESSION, null);
            }

            @Override
            public void onPlayDtmfTone(char dtmf) {
                sendCallRequestToActivity(ACTION_DTMF_TONE, String.valueOf(dtmf));
            }

            @Override
            public void onDisconnect() {
                super.onDisconnect();
                if (connection == null) {
                    return;
                }

                connection.setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
                connection.destroy();
                connection = null;

                sendCallRequestToActivity(ACTION_END_CALL, null);
            }

            @Override
            public void onAbort() {
                super.onAbort();
                if (connection == null) {
                    return;
                }

                connection.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
                connection.destroy();

                sendCallRequestToActivity(ACTION_END_CALL, null);
            }

            @Override
            public void onHold() {
                super.onHold();
                connection.setOnHold();

                sendCallRequestToActivity(ACTION_HOLD_CALL, null);
            }

            @Override
            public void onUnhold() {
                super.onUnhold();
                connection.setActive();

                sendCallRequestToActivity(ACTION_UNHOLD_CALL, null);
            }

            @Override
            public void onReject() {
                super.onReject();
                if (connection == null) {
                    return;
                }

                connection.setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
                connection.destroy();

                sendCallRequestToActivity(ACTION_END_CALL, null);
            }
        };

        Bundle extra = request.getExtras();

        connection.setConnectionCapabilities(Connection.CAPABILITY_MUTE | Connection.CAPABILITY_HOLD | Connection.CAPABILITY_SUPPORT_HOLD);
        connection.setAddress(request.getAddress(), TelecomManager.PRESENTATION_ALLOWED);
        connection.setExtras(extra);
        connection.setCallerDisplayName(extra.getString(EXTRA_CALLER_NAME), TelecomManager.PRESENTATION_ALLOWED);

        return connection;
    }

    /*
     * Send call request to the RNCallKeepModule
     */
    private void sendCallRequestToActivity(final String action, @Nullable final String attribute) {
        final VoiceConnectionService instance = this;
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(action);
                if (attribute != null) {
                    intent.putExtra("attribute", attribute);
                }

                LocalBroadcastManager.getInstance(instance).sendBroadcast(intent);
            }
        });
    }
}
