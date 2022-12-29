package io.wazo.callkeep;

import static io.wazo.callkeep.Constants.ACTION_ANSWER_CALL;
import static io.wazo.callkeep.Constants.ACTION_AUDIO_SESSION;
import static io.wazo.callkeep.Constants.ACTION_CHECK_REACHABILITY;
import static io.wazo.callkeep.Constants.ACTION_DID_CHANGE_AUDIO_ROUTE;
import static io.wazo.callkeep.Constants.ACTION_DTMF_TONE;
import static io.wazo.callkeep.Constants.ACTION_END_CALL;
import static io.wazo.callkeep.Constants.ACTION_HOLD_CALL;
import static io.wazo.callkeep.Constants.ACTION_MUTE_CALL;
import static io.wazo.callkeep.Constants.ACTION_ONGOING_CALL;
import static io.wazo.callkeep.Constants.ACTION_ON_CREATE_CONNECTION_FAILED;
import static io.wazo.callkeep.Constants.ACTION_ON_SILENCE_INCOMING_CALL;
import static io.wazo.callkeep.Constants.ACTION_SHOW_INCOMING_CALL_UI;
import static io.wazo.callkeep.Constants.ACTION_UNHOLD_CALL;
import static io.wazo.callkeep.Constants.ACTION_UNMUTE_CALL;
import static io.wazo.callkeep.Constants.ACTION_WAKE_APP;
import static io.wazo.callkeep.Constants.EXTRA_CALLER_NAME;
import static io.wazo.callkeep.Constants.EXTRA_CALL_NUMBER;
import static io.wazo.callkeep.Constants.EXTRA_CALL_UUID;
import static io.wazo.callkeep.Constants.EXTRA_HAS_VIDEO;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;

public class VoiceBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "VoiceBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        WritableMap args = Arguments.createMap();
        HashMap<String, String> attributeMap = (HashMap<String, String>) intent.getSerializableExtra("attributeMap");

        String stateStr = intent.getExtras().getString(TelephonyManager.EXTRA_STATE);
        String number = intent.getExtras().getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
        int state = 0;
        if(stateStr.equals(TelephonyManager.EXTRA_STATE_IDLE)){
            state = TelephonyManager.CALL_STATE_IDLE;
        }
        else if(stateStr.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)){
            state = TelephonyManager.CALL_STATE_OFFHOOK;
        }
        else if(stateStr.equals(TelephonyManager.EXTRA_STATE_RINGING)){
            state = TelephonyManager.CALL_STATE_RINGING;
        }
        Log.d(TAG, "[RNCallKeepModule][onReceive][state] " + state);
        Log.d(TAG, "[RNCallKeepModule][onReceive][stateStr] " + stateStr);



        Log.d(TAG, "[RNCallKeepModule][onReceive] " + intent.getAction());
        Log.d(TAG, "[RNCallKeepModule][onReceive][args] " + args);

        switch (intent.getAction()) {
            case ACTION_END_CALL:
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                RNCallKeepModule.sendEventToJS("RNCallKeepPerformEndCallAction", args);
                break;
            case ACTION_ANSWER_CALL:
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                args.putBoolean("withVideo", Boolean.valueOf(attributeMap.get(EXTRA_HAS_VIDEO)));
                RNCallKeepModule.sendEventToJS("RNCallKeepPerformAnswerCallAction", args);
                break;
            case ACTION_HOLD_CALL:
                args.putBoolean("hold", true);
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                RNCallKeepModule.sendEventToJS("RNCallKeepDidToggleHoldAction", args);
                break;
            case ACTION_UNHOLD_CALL:
                args.putBoolean("hold", false);
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                RNCallKeepModule.sendEventToJS("RNCallKeepDidToggleHoldAction", args);
                break;
            case ACTION_MUTE_CALL:
                args.putBoolean("muted", true);
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                RNCallKeepModule.sendEventToJS("RNCallKeepDidPerformSetMutedCallAction", args);
                break;
            case ACTION_UNMUTE_CALL:
                args.putBoolean("muted", false);
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                RNCallKeepModule.sendEventToJS("RNCallKeepDidPerformSetMutedCallAction", args);
                break;
            case ACTION_DTMF_TONE:
                args.putString("digits", attributeMap.get("DTMF"));
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                RNCallKeepModule.sendEventToJS("RNCallKeepDidPerformDTMFAction", args);
                break;
            case ACTION_ONGOING_CALL:
                args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                RNCallKeepModule.sendEventToJS("RNCallKeepDidReceiveStartCallAction", args);
                break;
            case ACTION_AUDIO_SESSION:
                RNCallKeepModule.sendEventToJS("RNCallKeepDidActivateAudioSession", null);
                break;
            case ACTION_CHECK_REACHABILITY:
                RNCallKeepModule.sendEventToJS("RNCallKeepCheckReachability", null);
                break;
            case ACTION_SHOW_INCOMING_CALL_UI:
                args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                args.putString("hasVideo", attributeMap.get(EXTRA_HAS_VIDEO));
                RNCallKeepModule.sendEventToJS("RNCallKeepShowIncomingCallUi", args);
                break;
            case ACTION_WAKE_APP:
                Intent headlessIntent = new Intent(RNCallKeepModule.reactContext, RNCallKeepBackgroundMessagingService.class);
                headlessIntent.putExtra("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                headlessIntent.putExtra("name", attributeMap.get(EXTRA_CALLER_NAME));
                headlessIntent.putExtra("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                Log.d(TAG, "[RNCallKeepModule] wakeUpApplication: " + attributeMap.get(EXTRA_CALL_UUID) + ", number : " + attributeMap.get(EXTRA_CALL_NUMBER) + ", displayName:" + attributeMap.get(EXTRA_CALLER_NAME));

                ComponentName name = RNCallKeepModule.reactContext.startService(headlessIntent);
                if (name != null) {
                    HeadlessJsTaskService.acquireWakeLockNow(RNCallKeepModule.reactContext);
                }
                break;
            case ACTION_ON_SILENCE_INCOMING_CALL:
                args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                RNCallKeepModule.sendEventToJS("RNCallKeepOnSilenceIncomingCall", args);
                break;
            case ACTION_ON_CREATE_CONNECTION_FAILED:
                args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                RNCallKeepModule.sendEventToJS("RNCallKeepOnIncomingConnectionFailed", args);
                break;
            case ACTION_DID_CHANGE_AUDIO_ROUTE:
                args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                args.putString("output", attributeMap.get("output"));
                RNCallKeepModule.sendEventToJS("RNCallKeepDidChangeAudioRoute", args);
                break;
        }

        if( state==TelephonyManager.CALL_STATE_OFFHOOK ) {
            System.out.println("Answered the incoming call!!");
            args.putString("IncomingNumber", number);
            args.putBoolean("hold", true);
            args.putString("callUUID", RNCallKeepModule.activeCallUUID);
            RNCallKeepModule.sendEventToJS("RNCallKeepDidToggleHoldAction", args);
        }

        if( state==TelephonyManager.CALL_STATE_IDLE ) {
            System.out.println("Disconnected the incoming call!!");
            args.putString("IncomingNumber", number);
            args.putBoolean("hold", false);
            args.putString("callUUID", RNCallKeepModule.activeCallUUID);
            RNCallKeepModule.sendEventToJS("RNCallKeepDidToggleHoldAction", args);
        }
    }
}
