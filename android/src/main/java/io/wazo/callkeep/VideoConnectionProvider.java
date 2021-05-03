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

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.telecom.Connection;
import android.telecom.VideoProfile;
import android.telecom.VideoProfile.CameraCapabilities;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import java.lang.IllegalArgumentException;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import android.os.Handler;
import android.os.HandlerThread;


public class VideoConnectionProvider extends Connection.VideoProvider {
    private static String TAG = "RNCK:VideoConnectionProvider";

    private Connection mConnection;
    private CameraCapabilities mCameraCapabilities;
    private Surface mPreviewSurface;
    private Context mContext;
    private CameraManager mCameraManager;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mCaptureSession;
    private CaptureRequest mPreviewRequest;
    private CaptureRequest.Builder mCaptureRequest;
    private String mCameraId;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread;

    public VideoConnectionProvider(Context context, Connection connection) {
        mConnection = connection;
        mContext = context;
        mCameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public void onSetCamera(String cameraId) {
        Log.d(TAG, "Set camera to " + cameraId);

        mCameraId = cameraId;
        setCameraCapabilities(mCameraId);
    }

    @Override
    public void onSetPreviewSurface(Surface surface) {
        Log.d(TAG, "Set preview surface " + (surface == null ? "unset" : "set"));

        mPreviewSurface = surface;
        if (!TextUtils.isEmpty(mCameraId) && mPreviewSurface != null) {
            startCamera(mCameraId);
        }
    }

    @Override
    public void onSetDisplaySurface(Surface surface) {
        Log.d(TAG, "Set display surface " + (surface == null ? "unset" : "set"));
        // Get the video flux from webrtc
    }

    @Override
    public void onSetDeviceOrientation(int rotation) {
        Log.d(TAG, "Set device orientation " + rotation);
    }

    /**
     * Sets the zoom value, creating a new CallCameraCapabalities object. If the zoom value is
     * non-positive, assume that zoom is not supported.
     */
    @Override
    public void onSetZoom(float value) {
        Log.d(TAG, ("Set zoom to " + value);
    }

    /**
     * "Sends" a request with a video call profile. Assumes that this response succeeds and sends
     * the response back via the CallVideoClient.
     */
    @Override
    public void onSendSessionModifyRequest(final VideoProfile fromProfile, final VideoProfile requestProfile) {
        Log.d(TAG, "On send session modify request");
    }

    @Override
    public void onSendSessionModifyResponse(VideoProfile responseProfile) {
        Log.d(TAG, "On send session modify response");
    }

    /**
     * Returns a CallCameraCapabilities object without supporting zoom.
     */
    @Override
    public void onRequestCameraCapabilities() {
        Log.d(TAG, "Requested camera capabilities");
        changeCameraCapabilities(mCameraCapabilities);
    }

    /**
     * Randomly reports data usage of value ranging from 10MB to 60MB.
     */
    @Override
    public void onRequestConnectionDataUsage() {
        Log.d(TAG, "Requested connection data usage");
    }

    /**
     * We do not have a need to set a paused image.
     */
    @Override
    public void onSetPauseImage(Uri uri) {
        Log.d(TAG, "Set pause image");
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreview();
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

    };

    /**
     * Starts displaying the camera image on the preview surface.
     *
     * @param cameraId
     */
    private void startCamera(String cameraId) {
        startBackgroundThread();

        try {
            mCameraManager.openCamera(cameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.w(TAG, "CameraAccessException: " + e);
            return;
        }
    }

    private void createCameraPreview() {
        Log.d(TAG, "Create camera preview");

        if (mPreviewSurface == null) {
            return;
        }

        startBackgroundThread();

        try {
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface),
                new CameraCaptureSession.StateCallback() {

                    @Override
                    public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                        if (null == mCameraDevice) {
                            return;
                        }

                        mCaptureSession = cameraCaptureSession;
                        try {
                            mCaptureRequest.set(CaptureRequest.CONTROL_AF_MODE,
                                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                            mPreviewRequest = mCaptureRequest.build();
                            mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                    null, null);
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    }

                }, null
            );

            mCaptureRequest = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequest.addTarget(mPreviewSurface);

        } catch (CameraAccessException e) {
            Log.w(TAG, "CameraAccessException: " + e);
            return;
        }

    }

    /**
     * Stops the camera and looper thread.
     */
    public void stopCamera() {
        stopBackgroundThread();

        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * Uses the camera manager to retrieve the camera capabilities for the chosen camera.
     *
     * @param cameraId The camera ID to get the capabilities for.
     */
    private void setCameraCapabilities(String cameraId) {
        Log.d(TAG, "Set camera capabilities");
        if (cameraId == null) {
            return;
        }

        CameraManager cameraManager = (CameraManager) mContext.getSystemService(
                Context.CAMERA_SERVICE);
        CameraCharacteristics c = null;
        try {
            c = cameraManager.getCameraCharacteristics(cameraId);
        } catch (IllegalArgumentException | CameraAccessException e) {
            // Ignoring camera problems.
        }
        if (c != null) {
            // Get the video size for the camera
            StreamConfigurationMap map = c.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            mCameraCapabilities = new CameraCapabilities(previewSize.getWidth(),
                    previewSize.getHeight());
        }
    }
}
