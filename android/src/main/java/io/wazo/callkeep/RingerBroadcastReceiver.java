package io.wazo.callkeep;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.DisconnectCause;
import android.util.Log;
import android.telecom.Connection;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import static io.wazo.callkeep.RNCallKeepModule.ACTION_ANSWER_CALL;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_UUID;

public class RingerBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "RNCK:RingerBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Class mainActivityClass = Class.forName("com.telzio.softphone.android.MainActivity");
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            String actionPerformed = intent.getStringExtra("actionPerformed");
            String callUuid = intent.getStringExtra(EXTRA_CALL_UUID);

            WritableMap args = Arguments.createMap();
            args.putString("callUUID", callUuid);

            VoiceConnection connection = (VoiceConnection)VoiceConnectionService.getConnection(callUuid);
            if(connection != null) {
                Log.d(TAG, "Button clicked: " + actionPerformed + " " + callUuid);
                if("REJECT".equals(actionPerformed)) {
                    Log.d(TAG, "REJECT: signalRejectToRN() " + callUuid);
                    connection.signalRejectToRN();
                }
                else {
                    Log.d(TAG, "ANSWER: signalAnswerToRN() " + callUuid);
                    connection.signalAnswerToRN();

                    Intent i = new Intent(context, mainActivityClass);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(i);
                }
            }
            else {
                Log.e(TAG, "No connection found with calluuid: " + callUuid);
            }

            int notificationId = intent.getIntExtra("NOTIFICATION_ID", 0);
            if(notificationId > 0) {
                manager.cancel(notificationId);
            }
        }
        catch(Exception e) {
            Log.e(TAG,"Exception in ringer broadcast receiver", e);
        }
    }
}
