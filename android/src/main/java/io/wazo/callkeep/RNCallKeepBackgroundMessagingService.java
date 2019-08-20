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

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALLER_NAME;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_NUMBER;
import static io.wazo.callkeep.RNCallKeepModule.EXTRA_CALL_UUID;

import javax.annotation.Nullable;

public class RNCallKeepBackgroundMessagingService extends HeadlessJsTaskService {
  @Override
  protected @Nullable
  HeadlessJsTaskConfig getTaskConfig(Intent intent) {
    Bundle extras = intent.getExtras();

    return new HeadlessJsTaskConfig(
      "RNCallKeepBackgroundMessage",
      Arguments.fromBundle(extras),
      60000,
      false
    );
  }
}
