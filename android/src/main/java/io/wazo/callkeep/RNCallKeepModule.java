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

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telecom.CallAudioState;
import android.telecom.Connection;
import android.telecom.DisconnectCause;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import static android.support.v4.app.ActivityCompat.requestPermissions;

// @see https://github.com/kbagchiGWC/voice-quickstart-android/blob/9a2aff7fbe0d0a5ae9457b48e9ad408740dfb968/exampleConnectionService/src/main/java/com/twilio/voice/examples/connectionservice/VoiceConnectionServiceActivity.java
public class RNCallKeepModule extends ReactContextBaseJavaModule {
    public static final int REQUEST_READ_PHONE_STATE = 1337;
    public static final int REQUEST_REGISTER_CALL_PROVIDER = 394859;

    public static final String CHECKING_PERMS = "CHECKING_PERMS";
    public static final String EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME";
    public static final String EXTRA_CALL_UUID = "EXTRA_CALL_UUID";
    public static final String EXTRA_CALL_NUMBER = "EXTRA_CALL_NUMBER";
    public static final String ACTION_END_CALL = "ACTION_END_CALL";
    public static final String ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL";
    public static final String ACTION_MUTE_CALL = "ACTION_MUTE_CALL";
    public static final String ACTION_UNMUTE_CALL = "ACTION_UNMUTE_CALL";
    public static final String ACTION_DTMF_TONE = "ACTION_DTMF_TONE";
    public static final String ACTION_HOLD_CALL = "ACTION_HOLD_CALL";
    public static final String ACTION_UNHOLD_CALL = "ACTION_UNHOLD_CALL";
    public static final String ACTION_ONGOING_CALL = "ACTION_ONGOING_CALL";
    public static final String ACTION_AUDIO_SESSION = "ACTION_AUDIO_SESSION";
    public static final String ACTION_CHECK_REACHABILITY = "ACTION_CHECK_REACHABILITY";

    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
    private static final String REACT_NATIVE_MODULE_NAME = "RNCallKeep";
    private static final String[] permissions = { Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE, Manifest.permission.RECORD_AUDIO };

    private static final String TAG = "RNCK:RNCallKeepModule";
    private static TelecomManager telecomManager;
    private static TelephonyManager telephonyManager;
    private static Promise hasPhoneAccountPromise;
    private ReactApplicationContext reactContext;
    public static PhoneAccountHandle handle;
    private boolean isReceiverRegistered = false;
    private VoiceBroadcastReceiver voiceBroadcastReceiver;
    private ReadableMap _settings;

    public RNCallKeepModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return REACT_NATIVE_MODULE_NAME;
    }

    @ReactMethod
    public void setup(ReadableMap options) {
        VoiceConnectionService.setAvailable(false);
        this._settings = options;

        if (isConnectionServiceAvailable()) {
            this.registerPhoneAccount(this.getAppContext());
            voiceBroadcastReceiver = new VoiceBroadcastReceiver();
            registerReceiver();
            VoiceConnectionService.setAvailable(true);
        }
    }

    @ReactMethod
    public void displayIncomingCall(String uuid, String number, String callerName) {
        if (!isConnectionServiceAvailable() || !hasPhoneAccount()) {
            return;
        }

        Log.d(TAG, "displayIncomingCall number: " + number + ", callerName: " + callerName);

        Bundle extras = new Bundle();
        Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);

        extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri);
        extras.putString(EXTRA_CALLER_NAME, callerName);
        extras.putString(EXTRA_CALL_UUID, uuid);

        telecomManager.addNewIncomingCall(handle, extras);
    }

    @ReactMethod
    public void answerIncomingCall(String uuid) {
        if (!isConnectionServiceAvailable() || !hasPhoneAccount()) {
            return;
        }

        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }

        conn.onAnswer();
    }

    @ReactMethod
    public void startCall(String uuid, String number, String callerName) {
        if (!isConnectionServiceAvailable() || !hasPhoneAccount() || !hasPermissions() || number == null) {
            return;
        }

        Log.d(TAG, "startCall number: " + number + ", callerName: " + callerName);

        Bundle extras = new Bundle();
        Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);

        Bundle callExtras = new Bundle();
        callExtras.putString(EXTRA_CALLER_NAME, callerName);
        callExtras.putString(EXTRA_CALL_UUID, uuid);
        callExtras.putString(EXTRA_CALL_NUMBER, number);

        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle);
        extras.putParcelable(TelecomManager.EXTRA_OUTGOING_CALL_EXTRAS, callExtras);

        telecomManager.placeCall(uri, extras);
    }

    @ReactMethod
    public void endCall(String uuid) {
        Log.d(TAG, "endCall called");
        if (!isConnectionServiceAvailable() || !hasPhoneAccount()) {
            return;
        }

        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }
        conn.onDisconnect();

        Log.d(TAG, "endCall executed");
    }

    @ReactMethod
    public void endAllCalls() {
        Log.d(TAG, "endAllCalls called");
        if (!isConnectionServiceAvailable() || !hasPhoneAccount()) {
            return;
        }

        Map<String, VoiceConnection> currentConnections = VoiceConnectionService.currentConnections;
        for (Map.Entry<String, VoiceConnection> connectionEntry : currentConnections.entrySet()) {
            Connection connectionToEnd = connectionEntry.getValue();
            connectionToEnd.onDisconnect();
        }

        Log.d(TAG, "endAllCalls executed");
    }

    @ReactMethod
    public void checkPhoneAccountPermission(ReadableArray optionalPermissions, Promise promise) {
        Activity currentActivity = this.getCurrentActivity();

        if (!isConnectionServiceAvailable()) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "ConnectionService not available for this version of Android.");
            return;
        }
        if (currentActivity == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }
        String[] optionalPermsArr = new String[optionalPermissions.size()];
        for (int i = 0; i < optionalPermissions.size(); i++) {
            optionalPermsArr[i] = optionalPermissions.getString(i);
        }

        String[] allPermissions = Arrays.copyOf(permissions, permissions.length + optionalPermsArr.length);
        System.arraycopy(optionalPermsArr, 0, allPermissions, permissions.length, optionalPermsArr.length);

        hasPhoneAccountPromise = promise;

        if (!this.hasPermissions()) {
            requestPermissions(currentActivity, allPermissions, REQUEST_READ_PHONE_STATE);
             return;
        }

        promise.resolve(!hasPhoneAccount());
    }

    @ReactMethod
    public void checkDefaultPhoneAccount(Promise promise) {
        if (!isConnectionServiceAvailable() || !hasPhoneAccount()) {
            promise.resolve(true);
            return;
        }

        if (!Build.MANUFACTURER.equalsIgnoreCase("Samsung")) {
            promise.resolve(true);
            return;
        }

        boolean hasSim = telephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
        boolean hasDefaultAccount = telecomManager.getDefaultOutgoingPhoneAccount("tel") != null;

        promise.resolve(!hasSim || hasDefaultAccount);
    }

    @ReactMethod
    public void setOnHold(String uuid, boolean shouldHold) {
        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }

        if (shouldHold == true) {
            conn.onHold();
        } else {
            conn.onUnhold();
        }
    }

    @ReactMethod
    public void reportEndCallWithUUID(String uuid, int reason) {
        if (!isConnectionServiceAvailable() || !hasPhoneAccount()) {
            return;
        }

        VoiceConnection conn = (VoiceConnection) VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }
        conn.reportDisconnect(reason);
    }

    @ReactMethod
    public void rejectCall(String uuid) {
        if (!isConnectionServiceAvailable() || !hasPhoneAccount()) {
            return;
        }

        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }

        conn.onReject();
    }

    @ReactMethod
    public void setMutedCall(String uuid, boolean shouldMute) {
        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }

        CallAudioState newAudioState = null;
        //if the requester wants to mute, do that. otherwise unmute
        if (shouldMute) {
            newAudioState = new CallAudioState(true, conn.getCallAudioState().getRoute(),
                    conn.getCallAudioState().getSupportedRouteMask());
        } else {
            newAudioState = new CallAudioState(false, conn.getCallAudioState().getRoute(),
                    conn.getCallAudioState().getSupportedRouteMask());
        }
        conn.onCallAudioStateChanged(newAudioState);
    }

    @ReactMethod
    public void sendDTMF(String uuid, String key) {
        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }
        char dtmf = key.charAt(0);
        conn.onPlayDtmfTone(dtmf);
    }

    @ReactMethod
    public void updateDisplay(String uuid, String displayName, String uri) {
        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }

        conn.setAddress(Uri.parse(uri), TelecomManager.PRESENTATION_ALLOWED);
        conn.setCallerDisplayName(displayName, TelecomManager.PRESENTATION_ALLOWED);
    }

    @ReactMethod
    public void hasPhoneAccount(Promise promise) {
        promise.resolve(hasPhoneAccount());
    }

    @ReactMethod
    public void hasOutgoingCall(Promise promise) {
        promise.resolve(VoiceConnectionService.hasOutgoingCall);
    }

    @ReactMethod
    public void hasPermissions(Promise promise) {
        promise.resolve(this.hasPermissions());
    }

    @ReactMethod
    public void setAvailable(Boolean active) {
        VoiceConnectionService.setAvailable(active);
    }

    @ReactMethod
    public void setReachable() {
        VoiceConnectionService.setReachable();
    }

    @ReactMethod
    public void setCurrentCallActive(String uuid) {
        Connection conn = VoiceConnectionService.getConnection(uuid);
        if (conn == null) {
            return;
        }

        conn.setConnectionCapabilities(conn.getConnectionCapabilities() | Connection.CAPABILITY_HOLD);
        conn.setActive();
    }

    @ReactMethod
    public void openPhoneAccounts() {
        if (!isConnectionServiceAvailable()) {
            return;
        }

        if (Build.MANUFACTURER.equalsIgnoreCase("Samsung")) {
            Intent intent = new Intent();
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            intent.setComponent(new ComponentName("com.android.server.telecom",
                    "com.android.server.telecom.settings.EnableAccountPreferenceActivity"));

            this.getAppContext().startActivity(intent);
            return;
        }

        openPhoneAccountSettings();
    }

    @ReactMethod
    public void openPhoneAccountSettings() {
        if (!isConnectionServiceAvailable()) {
            return;
        }

        Intent intent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        this.getAppContext().startActivity(intent);
    }

    @ReactMethod
    public static Boolean isConnectionServiceAvailable() {
        // PhoneAccount is available since api level 23
        return Build.VERSION.SDK_INT >= 23;
    }

    @ReactMethod
    public void backToForeground() {
        Context context = getAppContext();
        String packageName = context.getApplicationContext().getPackageName();
        Intent focusIntent = context.getPackageManager().getLaunchIntentForPackage(packageName).cloneFilter();

        focusIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        Activity activity = getCurrentActivity();
        activity.startActivity(focusIntent);
    }

    private void registerPhoneAccount(Context appContext) {
        if (!isConnectionServiceAvailable()) {
            return;
        }

        ComponentName cName = new ComponentName(this.getAppContext(), VoiceConnectionService.class);
        String appName = this.getApplicationName(appContext);

        handle = new PhoneAccountHandle(cName, appName);

        PhoneAccount.Builder builder = new PhoneAccount.Builder(handle, appName)
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER);

        if (_settings != null && _settings.hasKey("imageName")) {
            int identifier = appContext.getResources().getIdentifier(_settings.getString("imageName"), "drawable", appContext.getPackageName());
            Icon icon = Icon.createWithResource(appContext, identifier);
            builder.setIcon(icon);
        }

        PhoneAccount account = builder.build();

        telephonyManager = (TelephonyManager) this.getAppContext().getSystemService(Context.TELEPHONY_SERVICE);
        telecomManager = (TelecomManager) this.getAppContext().getSystemService(Context.TELECOM_SERVICE);

        telecomManager.registerPhoneAccount(account);
    }

    private void sendEventToJS(String eventName, @Nullable WritableMap params) {
        this.reactContext.getJSModule(RCTDeviceEventEmitter.class).emit(eventName, params);
    }

    private String getApplicationName(Context appContext) {
        ApplicationInfo applicationInfo = appContext.getApplicationInfo();
        int stringId = applicationInfo.labelRes;

        return stringId == 0 ? applicationInfo.nonLocalizedLabel.toString() : appContext.getString(stringId);
    }

    private Boolean hasPermissions() {
        Activity currentActivity = this.getCurrentActivity();

        boolean hasPermissions = true;
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(currentActivity, permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                hasPermissions = false;
            }
        }

        return hasPermissions;
    }

    private static boolean hasPhoneAccount() {
        return isConnectionServiceAvailable() && telecomManager.getPhoneAccount(handle).isEnabled();
    }

    private void registerReceiver() {
        if (!isReceiverRegistered) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ACTION_END_CALL);
            intentFilter.addAction(ACTION_ANSWER_CALL);
            intentFilter.addAction(ACTION_MUTE_CALL);
            intentFilter.addAction(ACTION_UNMUTE_CALL);
            intentFilter.addAction(ACTION_DTMF_TONE);
            intentFilter.addAction(ACTION_UNHOLD_CALL);
            intentFilter.addAction(ACTION_HOLD_CALL);
            intentFilter.addAction(ACTION_ONGOING_CALL);
            intentFilter.addAction(ACTION_AUDIO_SESSION);
            intentFilter.addAction(ACTION_CHECK_REACHABILITY);
            LocalBroadcastManager.getInstance(this.reactContext).registerReceiver(voiceBroadcastReceiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    private Context getAppContext() {
        return this.reactContext.getApplicationContext();
    }

    public static void onRequestPermissionsResult(int requestCode, String[] grantedPermissions, int[] grantResults) {
        int permissionsIndex = 0;
        List<String> permsList = Arrays.asList(permissions);
        for (int result : grantResults) {
            if (permsList.contains(grantedPermissions[permissionsIndex]) && result != PackageManager.PERMISSION_GRANTED) {
                hasPhoneAccountPromise.resolve(false);
                return;
            }
            permissionsIndex++;
        }
        hasPhoneAccountPromise.resolve(true);
    }

    private class VoiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            WritableMap args = Arguments.createMap();
            HashMap<String, String> attributeMap = (HashMap<String, String>)intent.getSerializableExtra("attributeMap");

            switch (intent.getAction()) {
                case ACTION_END_CALL:
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEventToJS("RNCallKeepPerformEndCallAction", args);
                    break;
                case ACTION_ANSWER_CALL:
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEventToJS("RNCallKeepPerformAnswerCallAction", args);
                    break;
                case ACTION_HOLD_CALL:
                    args.putBoolean("hold", true);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEventToJS("RNCallKeepDidToggleHoldAction", args);
                    break;
                case ACTION_UNHOLD_CALL:
                    args.putBoolean("hold", false);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEventToJS("RNCallKeepDidToggleHoldAction", args);
                    break;
                case ACTION_MUTE_CALL:
                    args.putBoolean("muted", true);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEventToJS("RNCallKeepDidPerformSetMutedCallAction", args);
                    break;
                case ACTION_UNMUTE_CALL:
                    args.putBoolean("muted", false);
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEventToJS("RNCallKeepDidPerformSetMutedCallAction", args);
                    break;
                case ACTION_DTMF_TONE:
                    args.putString("digits", attributeMap.get("DTMF"));
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    sendEventToJS("RNCallKeepDidPerformDTMFAction", args);
                    break;
                case ACTION_ONGOING_CALL:
                    args.putString("handle", attributeMap.get(EXTRA_CALL_NUMBER));
                    args.putString("callUUID", attributeMap.get(EXTRA_CALL_UUID));
                    args.putString("name", attributeMap.get(EXTRA_CALLER_NAME));
                    sendEventToJS("RNCallKeepDidReceiveStartCallAction", args);
                    break;
                case ACTION_AUDIO_SESSION:
                    sendEventToJS("RNCallKeepDidActivateAudioSession", null);
                    break;
                case ACTION_CHECK_REACHABILITY:
                    sendEventToJS("RNCallKeepCheckReachability", null);
                    break;
            }
        }
    }
}
