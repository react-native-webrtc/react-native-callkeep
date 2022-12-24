package io.wazo.callkeep;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.View;
import android.net.Uri;
import android.os.Vibrator;
import android.content.Context;
import android.media.MediaPlayer;
import android.provider.Settings;
import java.util.List;
import android.app.Activity;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import io.wazo.callkeep.R;

import com.squareup.picasso.Picasso;

public class UnlockScreenActivity extends AppCompatActivity implements UnlockScreenActivityInterface {
    private static final String TAG = "MessagingService";
    private TextView tvName;
    private TextView tvInfo;
    private ImageView ivAvatar;
    private String uuid = "";
    static boolean active = false;
    private static Vibrator v = (Vibrator) RNCallKeepModule.reactContext.getSystemService(Context.VIBRATOR_SERVICE);
    private long[] pattern = {0, 1000, 800};
    private static MediaPlayer player = MediaPlayer.create(RNCallKeepModule.reactContext, Settings.System.DEFAULT_RINGTONE_URI);
    private static Activity fa;
    private Button messageBtn;
    Dialog dialog;
    LinearLayout linearLayout;
    KeyguardManager keyguardManager;
    private Button rejbtn1,rejbtn2,rejbtn3,rejbtn4,rejbtn5,rejbtn6;
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
        tvName = findViewById(R.id.tvName);
        tvInfo = findViewById(R.id.tvInfo);
        ivAvatar = findViewById(R.id.ivAvatar);
        messageBtn = findViewById(R.id.messageBtn);
        linearLayout=(LinearLayout) findViewById(R.id.call_linear_layout);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            if (bundle.containsKey("uuid")) {
                uuid = bundle.getString("uuid");
            }
            if (bundle.containsKey("name")) {
                String name = bundle.getString("name");
                tvName.setText(name);
            }
            if (bundle.containsKey("info")) {
                String info = bundle.getString("info");
                tvInfo.setText(info);
            }
            if (bundle.containsKey("avatar")) {
                String avatar = bundle.getString("avatar");
                if (avatar != null) {
                    Picasso.get().load(avatar).transform(new CircleTransform()).into(ivAvatar);
                }
            }
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        v.vibrate(pattern, 0);
        player.start();
        AnimateImage acceptCallBtn = findViewById(R.id.ivAcceptCall);
        acceptCallBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
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
        messageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialogueBox();
            }
        });






    }

    public void showDialogueBox(){
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.message_dialogue);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        dialog.show();
        rejbtn1= dialog.findViewById(R.id.rej1);
        rejbtn2= dialog.findViewById(R.id.rej2);
        rejbtn3= dialog.findViewById(R.id.rej3);
        rejbtn4= dialog.findViewById(R.id.rej4);
        rejbtn5= dialog.findViewById(R.id.rej5);
        rejbtn6= dialog.findViewById(R.id.rej6);
        linearLayout=dialog.findViewById(R.id.call_linear_layout);
        linearLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        rejbtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing(1);
                dialog.dismiss();
            }
        });
        rejbtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing(2);
                dialog.dismiss();
            }
        });
        rejbtn3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing(3);
                dialog.dismiss();
            }
        });
        rejbtn4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing(4);
                dialog.dismiss();
            }
        });
        rejbtn5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing(5);
                dialog.dismiss();
            }
        });
        rejbtn6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                v.cancel();
                player.stop();
                dismissDialing(6);
                dialog.dismiss();
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

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    private void acceptDialing() {
         keyguardManager = (KeyguardManager) RNCallKeepModule.reactContext.getSystemService(Context.KEYGUARD_SERVICE);

        WritableMap params = Arguments.createMap();
        params.putBoolean("accept", true);
        params.putString("uuid", uuid);
        if (!RNCallKeepModule.reactContext.hasCurrentActivity()) {
            params.putBoolean("isHeadless", true);
        }
        Log.d(TAG, "acceptDialing: "+keyguardManager.isDeviceLocked());
        sendEvent("answerCall", params);
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
        if(message!=null)
        {
            params.putInt("msg",message);
            sendEvent("rejectMessage", params);
        }
        else
        {
            sendEvent("endCall", params);
        }



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
}
