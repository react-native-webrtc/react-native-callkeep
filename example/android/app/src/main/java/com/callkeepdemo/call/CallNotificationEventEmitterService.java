package com.callkeepdemo.call;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.*;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

public class CallNotificationEventEmitterService extends HeadlessJsTaskService {
    @Nullable
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
        Bundle extras = intent.getExtras();
        return new HeadlessJsTaskConfig(
                "CallNotificationEventEmitter",
                extras != null ? Arguments.fromBundle(extras) : new WritableNativeMap(),
                60000,
                true);
    }
}
