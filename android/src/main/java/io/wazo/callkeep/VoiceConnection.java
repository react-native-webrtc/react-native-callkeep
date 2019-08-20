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
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static io.wazo.callkeep.RNCallKeepModule.ACTION_ANSWER_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_AUDIO_SESSION;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_DTMF_TONE;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_END_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_HOLD_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_MUTE_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_UNHOLD_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_UNMUTE_CALL;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALLER_NAME;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_NUMBER;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_UUID;

@TargetApi(Build.VERSION_CODES.M)
public class VoiceConnection extends Connection {
    private boolean isMuted = false;
    private HashMap<String, String> handle;
    private Context context;
    private static final String TAG = "RNCK:VoiceConnection";

    VoiceConnection(Context context, HashMap<String, String> handle) {
        super();
        this.handle = handle;
        this.context = context;

        String number = handle.get(EXTRA_CALL_NUMBER);
        String name = handle.get(EXTRA_CALLER_NAME);

        if (number != null) {
            setAddress(Uri.parse(number), TelecomManager.PRESENTATION_ALLOWED);
        }
        if (name != null && !name.equals("")) {
            setCallerDisplayName(name, TelecomManager.PRESENTATION_ALLOWED);
        }
    }

    @Override
    public void onExtrasChanged(Bundle extras) {
        super.onExtrasChanged(extras);
        HashMap attributeMap = (HashMap<String, String>)extras.getSerializable("attributeMap");
        if (attributeMap != null) {
            handle = attributeMap;
        }
    }

    @Override
    public void onCallAudioStateChanged(CallAudioState state) {
        if (state.isMuted() == this.isMuted) {
            return;
        }

        this.isMuted = state.isMuted();
        sendCallRequestToActivity(isMuted ? ACTION_MUTE_CALL : ACTION_UNMUTE_CALL, handle);
    }

    @Override
    public void onAnswer() {
        super.onAnswer();
        Log.d(TAG, "onAnswer called");

        setConnectionCapabilities(getConnectionCapabilities() | Connection.CAPABILITY_HOLD);
        setAudioModeIsVoip(true);

        sendCallRequestToActivity(ACTION_ANSWER_CALL, handle);
        sendCallRequestToActivity(ACTION_AUDIO_SESSION, null);
        Log.d(TAG, "onAnswer executed");
    }

    @Override
    public void onPlayDtmfTone(char dtmf) {
        try {
            handle.put("DTMF", Character.toString(dtmf));
        } catch (Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        sendCallRequestToActivity(ACTION_DTMF_TONE, handle);
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "onDisconnect executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        destroy();
    }

    public void reportDisconnect(int reason) {
        super.onDisconnect();
        switch (reason) {
            case 1:
                setDisconnected(new DisconnectCause(DisconnectCause.ERROR));
                break;
            case 2:
                setDisconnected(new DisconnectCause(DisconnectCause.REMOTE));
                break;
            case 3:
                setDisconnected(new DisconnectCause(DisconnectCause.BUSY));
                break;
            default:
                break;
        }
        ((VoiceConnectionService)context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        destroy();
    }

    @Override
    public void onAbort() {
        super.onAbort();
        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "onAbort executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        destroy();
    }

    @Override
    public void onHold() {
        super.onHold();
        this.setOnHold();
        sendCallRequestToActivity(ACTION_HOLD_CALL, handle);
    }

    @Override
    public void onUnhold() {
        super.onUnhold();
        sendCallRequestToActivity(ACTION_UNHOLD_CALL, handle);
        setActive();
    }

    @Override
    public void onReject() {
        super.onReject();
        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "onReject executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "Handle map error", exception);
        }
        destroy();
    }

    /*
     * Send call request to the RNCallKeepModule
     */
    private void sendCallRequestToActivity(final String action, @Nullable final HashMap attributeMap) {
        final VoiceConnection instance = this;
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
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });
    }
}
