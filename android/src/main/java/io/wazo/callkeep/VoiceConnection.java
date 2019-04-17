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
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import static io.wazo.callkeep.RNCallKeepModule.ACTION_ANSWER_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_AUDIO_SESSION;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_DTMF_TONE;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_END_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_HOLD_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_MUTE_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_UNHOLD_CALL;
import static io.wazo.callkeep.RNCallKeepModule.ACTION_UNMUTE_CALL;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_UUID;

@TargetApi(Build.VERSION_CODES.M)
public class VoiceConnection extends Connection {private String TAG = "VoiceConnection";
    private boolean isMuted = false;
    private String handle = "";
    private Context context;

    VoiceConnection(Context context, String handle) {
        super();
        this.handle = handle;
        this.context = context;
    }

    @Override
    public void onExtrasChanged(Bundle extras) {
        super.onExtrasChanged(extras);
        handle = extras.getString(EXTRA_CALL_UUID);
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

        setActive();
        setAudioModeIsVoip(true);

        sendCallRequestToActivity(ACTION_ANSWER_CALL, handle);
    }

    @Override
    public void onPlayDtmfTone(char dtmf) {
        sendCallRequestToActivity(ACTION_DTMF_TONE, String.valueOf(dtmf));
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
        setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        destroy();
    }

    @Override
    public void onAbort() {
        super.onAbort();

        setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        destroy();
    }

    @Override
    public void onHold() {
        super.onHold();
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

        setDisconnected(new DisconnectCause(DisconnectCause.CANCELED));
        sendCallRequestToActivity(ACTION_END_CALL, handle);
        destroy();
    }

    /*
     * Send call request to the RNCallKeepModule
     */
    private void sendCallRequestToActivity(final String action, @Nullable final String attribute) {
        final VoiceConnection instance = this;
        final Handler handler = new Handler();

        handler.post(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(action);
                if (attribute != null) {
                    intent.putExtra("attribute", attribute);
                }

                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
            }
        });
    }
}
