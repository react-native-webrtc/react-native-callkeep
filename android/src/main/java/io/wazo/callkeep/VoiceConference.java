package io.wazo.callkeep;

import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;

public class VoiceConference extends Conference {

    VoiceConference(PhoneAccountHandle phoneAccountHandle) {
        super(phoneAccountHandle);
        this.setActive();
//        this.setConnectionCapabilities(Connection.CAPABILITY_MUTE | Connection.CAPABILITY_HOLD | Connection.CAPABILITY_SUPPORT_HOLD);
    }

    @Override
    public void onMerge() {
        super.onMerge();
    }

    @Override
    public void onSeparate(Connection connection) {
        super.onSeparate(connection);
    }

    @Override
    public void onDisconnect() {
        super.onDisconnect();
    }

    @Override
    public void onConnectionAdded(Connection connection) {
        super.onConnectionAdded(connection);
    }

    @Override
    public void onHold() {
        super.onHold();
    }

    @Override
    public void onUnhold() {
        super.onUnhold();
    }
}
