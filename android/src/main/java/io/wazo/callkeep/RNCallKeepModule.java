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

import android.content.ComponentName;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.IntentFilter;
import android.content.Intent;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.annotation.Nullable;

import android.accounts.AccountManager;
import android.accounts.Account;
import android.telecom.DisconnectCause;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;
import android.telecom.PhoneAccount;
import android.telecom.TelecomManager;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;

import android.os.Bundle;
import android.os.Build;
import android.net.Uri;
import android.app.Activity;
import android.Manifest;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.lang.SecurityException;

// @see https://github.com/kbagchiGWC/voice-quickstart-android/blob/9a2aff7fbe0d0a5ae9457b48e9ad408740dfb968/exampleConnectionService/src/main/java/com/twilio/voice/examples/connectionservice/VoiceConnectionServiceActivity.java
public class RNCallKeepModule extends ReactContextBaseJavaModule {
    public static final int REQUEST_READ_PHONE_STATE = 394858;
    public static final int REQUEST_REGISTER_CALL_PROVIDER = 394859;

    public static final String CHECKING_PERMS = "CHECKING_PERMS";
    public static final String EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME";
    public static final String ACTION_END_CALL = "ACTION_END_CALL";
    public static final String ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL";
    public static final String ACTION_MUTE_CALL = "ACTION_MUTE_CALL";
    public static final String ACTION_UNMUTE_CALL = "ACTION_UNMUTE_CALL";
    public static final String ACTION_DTMF_TONE = "ACTION_DTMF_TONE";
    public static final String ACTION_HOLD_CALL = "ACTION_HOLD_CALL";
    public static final String ACTION_UNHOLD_CALL = "ACTION_UNHOLD_CALL";
    public static final String ACTION_ONGOING_CALL = "ACTION_ONGOING_CALL";
    public static final String ACTION_AUDIO_SESSION = "ACTION_AUDIO_SESSION";

    private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
    private static final String REACT_NATIVE_MODULE_NAME = "RNCallKeep";

    private static TelecomManager telecomManager;
    private static Promise hasPhoneAccountPromise;
    private ReactApplicationContext reactContext;
    private PhoneAccountHandle pah;
    private boolean isReceiverRegistered = false;
    private VoiceBroadcastReceiver voiceBroadcastReceiver;

    public RNCallKeepModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.reactContext = reactContext;

        VoiceConnectionService.setActive(false);

        if (isAvailable()) {
            this.registerPhoneAccount(this.getAppContext());
            voiceBroadcastReceiver = new VoiceBroadcastReceiver();
            registerReceiver();
            VoiceConnectionService.setActive(false);
        }
    }

    public static void onRequestPermissionsResult(int[] grantResults) {
        if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
            hasPhoneAccountPromise.resolve(hasPhoneAccount());
             return;
        }

        hasPhoneAccountPromise.resolve(false);
    }

    @Override
    public String getName() {
        return REACT_NATIVE_MODULE_NAME;
    }

    @ReactMethod
    public void displayIncomingCall(String number, String callerName) {
        if (!this.hasPhoneAccount()) {
            return;
        }

        Bundle extras = new Bundle();
        Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);

        extras.putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri);
        extras.putString(EXTRA_CALLER_NAME, callerName);

        telecomManager.addNewIncomingCall(this.pah, extras);
    }

    @ReactMethod
    public void startCall(String number, String callerName) {
        if (!this.hasPhoneAccount()) {
            return;
        }

        Bundle extras = new Bundle();
        Uri uri = Uri.fromParts(PhoneAccount.SCHEME_TEL, number, null);

        extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, this.pah);
        extras.putString(EXTRA_CALLER_NAME, callerName);

        telecomManager.placeCall(uri, extras);
    }

    @ReactMethod
    public void endCall() {
        if (!hasPhoneAccount()) {
            return;
        }

        Connection conn = VoiceConnectionService.getConnection();
        if (conn == null) {
            return;
        }

        conn.setDisconnected(new DisconnectCause(DisconnectCause.LOCAL));
        conn.destroy();
        VoiceConnectionService.deinitConnection();
    }

    @ReactMethod
    public void checkPhoneAccountPermission(Promise promise) {
        if (!isAvailable()) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "ConnectionService not available for this version of Android.");
            return;
        }
        if (this.getCurrentActivity() == null) {
            promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
            return;
        }

        hasPhoneAccountPromise = promise;
        String[] permissions = { Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE };
        if (!this.checkPermissions(permissions, REQUEST_READ_PHONE_STATE)) {
            return;
        }

        promise.resolve(hasPhoneAccount());
    }

    @ReactMethod
    public void hasPhoneAccount(Promise promise) {
        promise.resolve(hasPhoneAccount());
    }

    @ReactMethod
    public void setActive(Boolean active) {
        VoiceConnectionService.setActive(active);
    }

    @ReactMethod
    public void openPhoneAccounts() {
        if (!isAvailable()) {
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

        Intent intent = new Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        this.getAppContext().startActivity(intent);
    }

    @ReactMethod
    public static Boolean isAvailable() {
        // PhoneAccount is available since api level 23
        return Build.VERSION.SDK_INT >= 23;
    }

    private void registerPhoneAccount(Context appContext) {
        ComponentName cName = new ComponentName(this.getAppContext(), VoiceConnectionService.class);
        String appName = this.getApplicationName(appContext);

        this.pah = new PhoneAccountHandle(cName, appName);

        PhoneAccount account = new PhoneAccount.Builder(pah, appName)
                .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER)
                .build();

        PhoneAccountHandle handle = new PhoneAccountHandle(cName, appName);

        telecomManager = (TelecomManager) this.getAppContext().getSystemService(this.getAppContext().TELECOM_SERVICE);
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

    private Boolean checkPermissions(String[] permissions, int id) {
        Activity currentActivity = this.getCurrentActivity();

        boolean hasPermissions = true;
        for (String permission : permissions) {
            int permissionCheck = ContextCompat.checkSelfPermission(currentActivity, permission);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                hasPermissions = false;
            }
        }

        if (!hasPermissions) {
            ActivityCompat.requestPermissions(currentActivity, permissions, id);
        }

        return hasPermissions;
    }

    private static boolean hasPhoneAccount() {
        if (!isAvailable()) {
            return false;
        }

        List<PhoneAccountHandle> enabledAccounts = telecomManager.getCallCapablePhoneAccounts();

        for (PhoneAccountHandle account : enabledAccounts) {
            if (account.getComponentName().getClassName().equals(VoiceConnectionService.class.getCanonicalName())) {
                return true;
            }
        }

        return false;
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
            LocalBroadcastManager.getInstance(this.reactContext).registerReceiver(voiceBroadcastReceiver, intentFilter);
            isReceiverRegistered = true;
        }
    }

    private Context getAppContext() {
        return this.reactContext.getApplicationContext();
    }

    private class VoiceBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            WritableMap args = Arguments.createMap();

            switch (intent.getAction()) {
                case ACTION_END_CALL:
                    sendEventToJS("RNCallKeepPerformEndCallAction", null);
                    break;
                case ACTION_ANSWER_CALL:
                    sendEventToJS("RNCallKeepPerformAnswerCallAction", null);
                    break;
                case ACTION_HOLD_CALL:
                    sendEventToJS("RNCallKeepDidPerformHoldAction", null);
                    break;
                case ACTION_UNHOLD_CALL:
                    sendEventToJS("RNCallKeepDidPerformUnHoldAction", null);
                    break;
                case ACTION_MUTE_CALL:
                    args.putBoolean("muted", true);

                    sendEventToJS("RNCallKeepDidPerformSetMutedCallAction", args);
                    break;
                case ACTION_UNMUTE_CALL:
                    args.putBoolean("muted", false);

                    sendEventToJS("RNCallKeepDidPerformSetMutedCallAction", args);
                    break;
                case ACTION_DTMF_TONE:
                    args.putString("dtmf", intent.getStringExtra("attribute"));

                    sendEventToJS("RNCallKeepDidPerformDTMFAction", args);
                    break;
                case ACTION_ONGOING_CALL:
                    args.putString("number", intent.getStringExtra("attribute"));

                    sendEventToJS("RNCallKeepDidReceiveStartCallAction", args);
                    break;
                case ACTION_AUDIO_SESSION:
                    sendEventToJS("RNCallKeepDidActivateAudioSession", null);
                    break;
            }
        }
    }
}
