package io.wazo.callkeep;

public class Constants {
    public static final String ACTION_ANSWER_CALL = "ACTION_ANSWER_CALL";
    public static final String ACTION_AUDIO_SESSION = "ACTION_AUDIO_SESSION";
    public static final String ACTION_CHECK_REACHABILITY = "ACTION_CHECK_REACHABILITY";
    public static final String ACTION_DTMF_TONE = "ACTION_DTMF_TONE";
    public static final String ACTION_END_CALL = "ACTION_END_CALL";
    public static final String ACTION_HOLD_CALL = "ACTION_HOLD_CALL";
    public static final String ACTION_MUTE_CALL = "ACTION_MUTE_CALL";
    public static final String ACTION_ONGOING_CALL = "ACTION_ONGOING_CALL";
    public static final String ACTION_UNHOLD_CALL = "ACTION_UNHOLD_CALL";
    public static final String ACTION_UNMUTE_CALL = "ACTION_UNMUTE_CALL";
    public static final String ACTION_WAKE_APP = "ACTION_WAKE_APP";
    public static final String ACTION_SHOW_INCOMING_CALL_UI = "ACTION_SHOW_INCOMING_CALL_UI";

    public static final String EXTRA_CALL_NUMBER = "EXTRA_CALL_NUMBER";
    public static final String EXTRA_CALL_NUMBER_SCHEMA = "EXTRA_CALL_NUMBER_SCHEMA";
    public static final String EXTRA_CALL_UUID = "EXTRA_CALL_UUID";
    public static final String EXTRA_CALLER_NAME = "EXTRA_CALLER_NAME";
    // Can't use telecom.EXTRA_DISABLE_ADD_CALL ...
    public static final String EXTRA_DISABLE_ADD_CALL = "android.telecom.extra.DISABLE_ADD_CALL";

    public static final int FOREGROUND_SERVICE_TYPE_MICROPHONE = 128;
}
