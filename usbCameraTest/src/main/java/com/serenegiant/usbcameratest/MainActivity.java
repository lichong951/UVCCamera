/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usbcameratest;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.USBCameraManager;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.SimpleUVCCameraTextureView;

import java.nio.ByteBuffer;

public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent {

    private SimpleUVCCameraTextureView mUVCCameraView;
    // for open&start / stop&close camera preview
    private ImageButton mCameraButton;

    private ImageView frameIv;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraButton = findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(mOnClickListener);
        frameIv = findViewById(R.id.iv_frame);

        mUVCCameraView = findViewById(R.id.UVCCameraTextureView1);
        mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);

        USBCameraManager.getInstance().setmIFrameCallback(mIFrameCallback);
        USBCameraManager.getInstance().setTextureView(mUVCCameraView);
        USBCameraManager.getInstance().init(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        USBCameraManager.getInstance().register();
        synchronized (USBCameraManager.getInstance().getmSync()) {
            USBCameraManager.getInstance().startPreview();
        }
    }

    @Override
    protected void onStop() {
        synchronized (USBCameraManager.getInstance().getmSync()) {
            USBCameraManager.getInstance().stopPreview();
            USBCameraManager.getInstance().unregister();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        synchronized (USBCameraManager.getInstance().getmSync()) {
            USBCameraManager.getInstance().releaseCamera();
            USBCameraManager.getInstance().destoryUSBMonitor();
        }
        mUVCCameraView = null;
        mCameraButton = null;
        super.onDestroy();
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (USBCameraManager.getInstance().getmSync()) {
                if (USBCameraManager.getInstance().getmUVCCamera() == null) {
                    CameraDialog.showDialog(MainActivity.this);
                } else {
                    USBCameraManager.getInstance().releaseCamera();
                }
            }
        }
    };
    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return USBCameraManager.getInstance().getmUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // FIXME
                }
            }, 0);
        }
    }

    public static final String TAG = MainActivity.class.getSimpleName();

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    final Bitmap bitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
    private final IFrameCallback mIFrameCallback = new IFrameCallback() {
        @Override
        public void onFrame(final ByteBuffer frame) {
            Log.d(TAG, "onFrame called!");
            frame.clear();
            synchronized (bitmap) {
                bitmap.copyPixelsFromBuffer(frame);
            }
            frameIv.post(mUpdateImageTask);
        }
    };
    private final Runnable mUpdateImageTask = new Runnable() {
        @Override
        public void run() {
            synchronized (bitmap) {
                frameIv.setImageBitmap(bitmap);
            }
        }
    };
}
