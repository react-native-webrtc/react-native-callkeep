/*
 * Copyright (c) 2020 The CallKeep Authors (see the AUTHORS file)
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

import android.net.Uri;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.view.Surface;

import java.lang.String;


/**
 * Implements the VideoCallProvider.
 */
public class VideoConnectionService extends Connection.VideoProvider {

    public VideoConnectionService() {
    }

    @Override
    public void onSetCamera(String cameraId) {
    }

    @Override
    public void onSetPreviewSurface(Surface surface) {
    }

    @Override
    public void onSetDisplaySurface(Surface surface) {
    }

    @Override
    public void onSetDeviceOrientation(int rotation) {
    }

    @Override
    public void onSetZoom(float value) {
    }

    @Override
    public void onSendSessionModifyRequest(final VideoProfile fromProfile, final VideoProfile requestProfile) {
    }

    @Override
    public void onSendSessionModifyResponse(VideoProfile responseProfile) {
    }

    @Override
    public void onRequestCameraCapabilities() {
    }

    @Override
    public void onRequestConnectionDataUsage() {
    }

    @Override
    public void onSetPauseImage(Uri uri) {
    }

}
