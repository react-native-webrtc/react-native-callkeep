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
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.TelecomManager;
import android.net.Uri;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import static io.wazo.callkeep.Constants.ACTION_ANSWER_CALL;
import static io.wazo.callkeep.Constants.ACTION_AUDIO_SESSION;
import static io.wazo.callkeep.Constants.ACTION_DTMF_TONE;
import static io.wazo.callkeep.Constants.ACTION_END_CALL;
import static io.wazo.callkeep.Constants.ACTION_HOLD_CALL;
import static io.wazo.callkeep.Constants.ACTION_MUTE_CALL;
import static io.wazo.callkeep.Constants.ACTION_UNHOLD_CALL;
import static io.wazo.callkeep.Constants.ACTION_UNMUTE_CALL;
import static io.wazo.callkeep.Constants.EXTRA_CALLER_NAME;
import static io.wazo.callkeep.Constants.EXTRA_CALL_NUMBER;
import static io.wazo.callkeep.Constants.EXTRA_CALL_UUID;
import static io.wazo.callkeep.Constants.ACTION_SHOW_INCOMING_CALL_UI;
import static io.wazo.callkeep.Constants.ACTION_ON_SILENCE_INCOMING_CALL;
import static io.wazo.callkeep.Constants.ACTION_DID_CHANGE_AUDIO_ROUTE;

@TargetApi(Build.VERSION_CODES.M)
public class VoiceConnection extends Connection {
    private boolean isMuted = false;
    private boolean answered = false;
    private boolean rejected = false;
    private HashMap<String, String> handle;
    private Context context;
    private static final String TAG = "RNCallKeep";

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
        Log.d(TAG, "[VoiceConnection] onCallAudioStateChanged muted :" + (state.isMuted() ? "true" : "false"));

        handle.put("output", CallAudioState.audioRouteToString(state.getRoute()));
        sendCallRequestToActivity(ACTION_DID_CHANGE_AUDIO_ROUTE, handle);

        if (state.isMuted() == this.isMuted) {
            return;
        }

        this.isMuted = state.isMuted();
        sendCallRequestToActivity(isMuted ? ACTION_MUTE_CALL : ACTION_UNMUTE_CALL, handle);
    }

    @Override
    public void onAnswer(int videoState) {
        super.onAnswer(videoState);
        Log.d(TAG, "[VoiceConnection] onAnswer(int) executed");

        this._onAnswer(videoState);
    }

    @Override
    public void onAnswer() {
        super.onAnswer();
        Log.d(TAG, "[VoiceConnection] onAnswer() executed");

        this._onAnswer(0);
    }

    @Override
    public void onPlayDtmfTone(char dtmf) {
        Log.d(TAG, "[VoiceConnection] Playing DTMF : " + dtmf);
        try {
            handle.put("DTMF", Character.toString(dtmf));
        } catch (Throwable exception) {
            Log.e(TAG, "[VoiceConnection] Handle map error", exception);
        }
        sendCallRequestToActivity(ACTION_DTMF_TONE, handle);
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "[VoiceConnection] onDisconnect executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "[VoiceConnection] onDisconnect handle map error", exception);
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
            case 5:
                setDisconnected(new DisconnectCause(DisconnectCause.REMOTE));
                break;
            case 3:
                setDisconnected(new DisconnectCause(DisconnectCause.BUSY));
                break;
            case 4:
                setDisconnected(new DisconnectCause(DisconnectCause.ANSWERED_ELSEWHERE));
                break;
            case 6:
                setDisconnected(new DisconnectCause(DisconnectCause.MISSED));
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
        Log.d(TAG, "[VoiceConnection] onAbort executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "[VoiceConnection] onAbort handle map error", exception);
        }
        destroy();
    }

    @Override
    public void onHold() {
        Log.d(TAG, "[VoiceConnection] onHold");
        super.onHold();
        this.setOnHold();
        sendCallRequestToActivity(ACTION_HOLD_CALL, handle);
    }

    @Override
    public void onUnhold() {
        Log.d(TAG, "[VoiceConnection] onUnhold");
        super.onUnhold();
        sendCallRequestToActivity(ACTION_UNHOLD_CALL, handle);
        setActive();
    }

    public void onReject(int rejectReason) {
        Log.d(TAG, "[VoiceConnection] onReject(int) executed");

        this._onReject(rejectReason, null);
    }

    @Override
    public void onReject() {
        super.onReject();
        Log.d(TAG, "[VoiceConnection] onReject() executed");

        this._onReject(0, null);
    }

    @Override
    public void onReject(String replyMessage) {
        super.onReject(replyMessage);
        Log.d(TAG, "[VoiceConnection] onReject(String) executed");

        this._onReject(0, replyMessage);
    }

    @Override
    public void onCallEvent(String event, Bundle extras) {
        super.onCallEvent(event, extras);

        Log.d(TAG, "[VoiceConnection] onCallEvent called, event: " + event);
    }

    @Override
    public void onDeflect(Uri address) {
        super.onDeflect(address);

        Log.d(TAG, "[VoiceConnection] onDeflect called, address: " + address);
    }

    @Override
    public void onHandoverComplete() {
        super.onHandoverComplete();

        Log.d(TAG, "[VoiceConnection] onHandoverComplete called");
    }

    @Override
    public void onPostDialContinue(boolean proceed) {
        super.onPostDialContinue(proceed);

        Log.d(TAG, "[VoiceConnection] onPostDialContinue called, proceed: " + proceed);
    }

    @Override
    public void onPullExternalCall() {
        super.onPullExternalCall();

        Log.d(TAG, "[VoiceConnection] onPullExternalCall called");
    }

    @Override
    public void onSeparate() {
        super.onSeparate();

        Log.d(TAG, "[VoiceConnection] onSeparate called");
    }

    @Override
    public void onStateChanged(int state) {
        super.onStateChanged(state);

        Log.d(TAG, "[VoiceConnection] onStateChanged called, state : " + state);
    }

    @Override
    public void onSilence() {
        // onSilence was added on API level 29
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        super.onSilence();

        sendCallRequestToActivity(ACTION_ON_SILENCE_INCOMING_CALL, handle);
        Log.d(TAG, "[VoiceConnection] onSilence called");
    }

    @Override
    public void onStopDtmfTone() {
        super.onStopDtmfTone();

        Log.d(TAG, "[VoiceConnection] onStopDtmfTone called");
    }

    @Override
    public void onStopRtt() {
        super.onStopRtt();

        Log.d(TAG, "[VoiceConnection] onStopRtt called");
    }

    private void _onAnswer(int videoState) {
        Log.d(TAG, "[VoiceConnection] onAnswer called, videoState: " + videoState + ", answered: " + answered);
        // On some device (like Huawei P30 lite), both onAnswer() and onAnswer(int) are called
        // we have to trigger the callback only once
        if (answered) {
            return;
        }
        answered = true;

        setConnectionCapabilities(getConnectionCapabilities() | Connection.CAPABILITY_HOLD);
        setAudioModeIsVoip(true);

        sendCallRequestToActivity(ACTION_ANSWER_CALL, handle);
        sendCallRequestToActivity(ACTION_AUDIO_SESSION, handle);
        Log.d(TAG, "[VoiceConnection] onAnswer executed");
    }

    private void _onReject(int rejectReason, String replyMessage) {
        Log.d(TAG, "[VoiceConnection] onReject executed, rejectReason: " + rejectReason + ", replyMessage: " + replyMessage + ", rejected:" + rejected);
        if (rejected) {
            return;
        }
        rejected = true;

        setDisconnected(new DisconnectCause(DisconnectCause.REJECTED));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        Log.d(TAG, "[VoiceConnection] onReject executed");
        try {
            ((VoiceConnectionService) context).deinitConnection(handle.get(EXTRA_CALL_UUID));
        } catch(Throwable exception) {
            Log.e(TAG, "[VoiceConnection] onReject, handle map error", exception);
        }
        destroy();
    }

    @Override
    public void onShowIncomingCallUi() {
        Log.d(TAG, "[VoiceConnection] onShowIncomingCallUi");
        sendCallRequestToActivity(ACTION_SHOW_INCOMING_CALL_UI, handle);
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
