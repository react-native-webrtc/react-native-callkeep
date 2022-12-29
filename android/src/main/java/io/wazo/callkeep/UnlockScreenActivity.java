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

import android.app.Activity;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.internal.telephony.ITelephony;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.squareup.picasso.Picasso;

import java.lang.reflect.Method;
import java.util.HashMap;

import javax.annotation.Nullable;


public class UnlockScreenActivity extends AppCompatActivity implements UnlockScreenActivityInterface {
     
    private static final String TAG = "MessagingService";
    private TextView callerName;
    private TextView callerInfo;
    private ImageView callerAvatar;
    private String uuid = "";
    static boolean active = false;
    private static Vibrator v = (Vibrator) RNCallKeepModule.reactContext.getSystemService(Context.VIBRATOR_SERVICE);
    private long[] pattern = {0, 1000, 800};
    private static MediaPlayer player = MediaPlayer.create(RNCallKeepModule.reactContext, Settings.System.DEFAULT_RINGTONE_URI);
    private static Activity fa;
    Dialog dialog;
    LinearLayout linearLayout;
    KeyguardManager keyguardManager;

    private HashMap<String, String> handle = new HashMap<String, String>();;
    public static VoiceConnection  currentConnection;


    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onStop() {
        super.onStop();
        active = false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fa = this;

        setContentView(R.layout.activity_call_incoming);
        callerName = findViewById(R.id.callerName);
        callerInfo = findViewById(R.id.callerInfo);
        callerAvatar = findViewById(R.id.callerAvatar);
        linearLayout=(LinearLayout) findViewById(R.id.call_linear_layout);
        Bundle bundle = getIntent().getExtras();
        String name =""; 
        String info ="";
        if (bundle != null) {
            if (bundle.containsKey("uuid")) {
                uuid = bundle.getString("uuid");
            }
            if (bundle.containsKey("name")) {
                name = bundle.getString("name");
                callerName.setText(name);
            }
            if (bundle.containsKey("info")) {
                info = bundle.getString("info");
                callerInfo.setText(info);
            }
            if (bundle.containsKey("avatar")) {
                String avatar = bundle.getString("avatar");
                if (avatar != null) {
                    Picasso.get().load(avatar).transform(new CircleTransform()).into(callerAvatar);
                }
            }
        }

        handle.put(EXTRA_CALL_UUID, uuid);
        handle.put(EXTRA_CALLER_NAME, name);
        handle.put(EXTRA_CALL_NUMBER, info);
        handle.put(EXTRA_HAS_VIDEO, "true");

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        v.vibrate(pattern, 0);
        player.start();
        AnimateImage acceptCallBtn = findViewById(R.id.ivAcceptCall);
        acceptCallBtn.setOnClickListener(new View.OnClickListener() {
//            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
            @Override
            public void onClick(View view) {
                try {
                    v.cancel();
                    player.stop();
                    acceptDialing();
                } catch (Exception e) {
                    WritableMap params = Arguments.createMap();
                    params.putString("message", e.getMessage());
                    sendEvent("error", params);
                    dismissDialing(null);
                }
            }
        });
        AnimateImage rejectCallBtn = findViewById(R.id.ivDeclineCall);
        rejectCallBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing(null);
            }
        });
    }

    @Override
    public void onBackPressed() {
        // Dont back
    }

    public static void dismissIncoming() {
        v.cancel();
        player.stop();
        fa.finish();
    }

     public boolean isCallActive(ReactApplicationContext context){
        AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        Log.d(TAG,"isCallActive: manager.getMode() : "+ manager.getMode());
        return manager.getMode() == AudioManager.MODE_IN_CALL;
    }

    private void disconnectActiveCall(ReactApplicationContext context){
      TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class c = Class.forName(tm.getClass().getName());
            Method m = c.getDeclaredMethod("getITelephony");
            m.setAccessible(true);
            ITelephony telephonyService = (ITelephony) m.invoke(tm);
            Bundle bundle = getIntent().getExtras();
            String phoneNumber = bundle.getString("incoming_number");
            Log.d(TAG,"disconnectActiveCall: INCOMING NUMBER : "+ phoneNumber);
            Log.d(TAG,"disconnectActiveCall: telephonyService : "+ telephonyService);
            // if ((phoneNumber != null)) { 
                telephonyService.endCall();
                Log.d(TAG,"disconnectActiveCall: HANG UP : " +  phoneNumber);
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void acceptDialing() {
         keyguardManager = (KeyguardManager) RNCallKeepModule.reactContext.getSystemService(Context.KEYGUARD_SERVICE);

        if(isCallActive(RNCallKeepModule.reactContext)) {
           Log.d(TAG,"acceptDialing: An active call detected!!");

           disconnectActiveCall(RNCallKeepModule.reactContext);
        }

        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", true);
        params.putString("uuid", uuid);

        if (!RNCallKeepModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }
        Log.d(TAG, "acceptDialing: "+keyguardManager.isDeviceLocked());
        // sendEvent("RNCallKeepPerformAnswerCallAction", params);
        // sendEvent("answerCall", params);

        sendCallRequestToActivity(ACTION_ANSWER_CALL, handle);
        sendCallRequestToActivity(ACTION_AUDIO_SESSION, handle);

        if(keyguardManager.isDeviceLocked())
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                keyguardManager.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                    @Override
                    public void onDismissError() {
                        super.onDismissError();
                    }

                    @Override
                    public void onDismissSucceeded() {
                        super.onDismissSucceeded();
                    }

                    @Override
                    public void onDismissCancelled() {
                        super.onDismissCancelled();
                    }
                });
            }
        }
        finish();
    }

    private void dismissDialing(Integer message) {
        Log.d(TAG, "dismissDialing: "+message);
        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", false);
        params.putString("uuid", uuid);
        if (!RNCallKeepModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }
       
        // sendEvent("endCall", params);

        sendCallRequestToActivity(ACTION_END_CALL, handle);

        //reset
        handle = new HashMap();

        finish();
    }

    @Override
    public void onConnected() {
        Log.d(TAG, "onConnected: ");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

            }
        });
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected: ");

    }

    @Override
    public void onConnectFailure() {
        Log.d(TAG, "onConnectFailure: ");

    }

    @Override
    public void onIncoming(ReadableMap params) {
        Log.d(TAG, "onIncoming: ");
    }

    private void sendEvent(String eventName, WritableMap params) {
        RNCallKeepModule.reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }


    public void sendBroadcastEvent(Intent intent) {
            WritableMap args = Arguments.createMap();
            HashMap<String, String> attributeMap = (HashMap<String, String>)intent.getSerializableExtra("attributeMap");

            Log.d(TAG, "[RNCallKeepModule][onReceive] " + intent.getAction());
            Log.d(TAG, "[RNCallKeepModule][onReceive][args] " + args);

            switch (intent.getAction()) {
                case ACTION_END_CALL:
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEvent("RNCallKeepPerformEndCallAction", args);
                    break;
                case ACTION_ANSWER_CALL:
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    args.putBoolean("withVideo", Boolean.parseBoolean(attributeMap.get(EXTRA_HAS_VIDEO)));
                    sendEvent("RNCallKeepPerformAnswerCallAction", args);
                    break;
                case ACTION_HOLD_CALL:
                    args.putBoolean("hold", true);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEvent("RNCallKeepDidToggleHoldAction", args);
                    break;
                case ACTION_UNHOLD_CALL:
                    args.putBoolean("hold", false);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEvent("RNCallKeepDidToggleHoldAction", args);
                    break;
                case ACTION_MUTE_CALL:
                    args.putBoolean("muted", true);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEvent("RNCallKeepDidPerformSetMutedCallAction", args);
                    break;
                case ACTION_UNMUTE_CALL:
                    args.putBoolean("muted", false);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEvent("RNCallKeepDidPerformSetMutedCallAction", args);
                    break;
                case ACTION_DTMF_TONE:
                    args.putString("digits", attributeMap.get("DTMF"));
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEvent("RNCallKeepDidPerformDTMFAction", args);
                    break;
                case ACTION_ONGOING_CALL:
                    args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                    sendEvent("RNCallKeepDidReceiveStartCallAction", args);
                    break;
                case ACTION_AUDIO_SESSION:
                    sendEvent("RNCallKeepDidActivateAudioSession", null);
                    break;
                case ACTION_CHECK_REACHABILITY:
                    sendEvent("RNCallKeepCheckReachability", null);
                    break;
                case ACTION_SHOW_INCOMING_CALL_UI:
                    args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                    args.putString("hasVideo", attributeMap.get(EXTRA_HAS_VIDEO));
                    sendEvent("RNCallKeepShowIncomingCallUi", args);
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
                    sendEvent("RNCallKeepOnSilenceIncomingCall", args);
                    break;
                case ACTION_ON_CREATE_CONNECTION_FAILED:
                    args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                    sendEvent("RNCallKeepOnIncomingConnectionFailed", args);
                    break;
                case ACTION_DID_CHANGE_AUDIO_ROUTE:
                    args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    args.putString("output", attributeMap.get("output"));
                    sendEvent("RNCallKeepDidChangeAudioRoute", args);
                    break;
            }
        }


    private void sendCallRequestToActivity(final String action, @Nullable final HashMap attributeMap) {
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
                sendBroadcastEvent(intent);
            }
        });
    }

}
