package com.serenegiant.usb;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.serenegiant.utils.HandlerThreadHandler;

import java.nio.ByteBuffer;

public class USBCameraManager {
    public static final String TAG = USBCameraManager.class.getSimpleName();

    private final Object mSync = new Object();

    private Context context;
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private Surface mPreviewSurface;

    private  IFrameCallback mIFrameCallback;
   private TextureView textureView;
    private Handler mWorkerHandler;
    private long mWorkerThreadID = -1;


    ////////////////////////////////////////////////////////////////////////////
    //    内部静态类实现单例模式
    private USBCameraManager() {

    }

    public static USBCameraManager getInstance() {
        return USBCameraManagerHolder.instance;
    }

    static class USBCameraManagerHolder {
        private static USBCameraManager instance = new USBCameraManager();
    }
    ////////////////////////////////////////////////////////////////////////////

    public void init(Context context){
        this.context=context;
        if (mWorkerHandler == null) {
            mWorkerHandler = HandlerThreadHandler.createHandler(TAG);
            mWorkerThreadID = mWorkerHandler.getLooper().getThread().getId();
        }
        mUSBMonitor = new USBMonitor(context, mOnDeviceConnectListener);

    }

    public void register(){
        mUSBMonitor.register();
    }

    public void unregister(){
        if (mUSBMonitor != null) {
            mUSBMonitor.unregister();
        }
    }

    public IFrameCallback getmIFrameCallback() {
        return mIFrameCallback;
    }

    public void setmIFrameCallback(IFrameCallback mIFrameCallback) {
        this.mIFrameCallback = mIFrameCallback;
    }

    private final USBMonitor.OnDeviceConnectListener mOnDeviceConnectListener = new USBMonitor.OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(context, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock, final boolean createNew) {
            releaseCamera();
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    createUSBCamera(ctrlBlock);
                }
            }, 0);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final USBMonitor.UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
            releaseCamera();
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(context, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    private void createUSBCamera(USBMonitor.UsbControlBlock ctrlBlock) {
        final UVCCamera camera = new UVCCamera();
        camera.open(ctrlBlock);
        camera.setStatusCallback(new IStatusCallback() {
            @Override
            public void onStatus(final int statusClass, final int event, final int selector,
                                 final int statusAttribute, final ByteBuffer data) {
            }
        });
        camera.setButtonCallback(new IButtonCallback() {
            @Override
            public void onButton(final int button, final int state) {
            }
        });
//					camera.setPreviewTexture(camera.getSurfaceTexture());
        if (mPreviewSurface != null) {
            mPreviewSurface.release();
            mPreviewSurface = null;
        }
        try {
            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
        } catch (final IllegalArgumentException e) {
            // fallback to YUV mode
            try {
                camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
            } catch (final IllegalArgumentException e1) {
                camera.destroy();
                return;
            }
        }
        final SurfaceTexture st = textureView.getSurfaceTexture();
        if (st != null) {
            mPreviewSurface = new Surface(st);
            camera.setPreviewDisplay(mPreviewSurface);
            camera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565/*UVCCamera.PIXEL_FORMAT_NV21*/);
            camera.startPreview();
        }
        synchronized (mSync) {
            mUVCCamera = camera;
        }
    }

    /**
     * ワーカースレッド上で指定したRunnableを実行する
     * 未実行の同じRunnableがあればキャンセルされる(後から指定した方のみ実行される)
     * @param task
     * @param delayMillis
     */
    protected final synchronized void queueEvent(final Runnable task, final long delayMillis) {
        if ((task == null) || (mWorkerHandler == null)) return;
        try {
            mWorkerHandler.removeCallbacks(task);
            if (delayMillis > 0) {
                mWorkerHandler.postDelayed(task, delayMillis);
            } else if (mWorkerThreadID == Thread.currentThread().getId()) {
                task.run();
            } else {
                mWorkerHandler.post(task);
            }
        } catch (final Exception e) {
            // ignore
        }
    }
    public synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
    }


    public USBMonitor getmUSBMonitor() {
        return mUSBMonitor;
    }

    public void setmUSBMonitor(USBMonitor mUSBMonitor) {
        this.mUSBMonitor = mUSBMonitor;
    }

   public void  destoryUSBMonitor(){
       if (mUSBMonitor != null) {
           mUSBMonitor.destroy();
           mUSBMonitor = null;
       }
   }

    public UVCCamera getmUVCCamera() {
        return mUVCCamera;
    }

    public void setmUVCCamera(UVCCamera mUVCCamera) {
        this.mUVCCamera = mUVCCamera;
    }

    public void startPreview(){
        if (mUVCCamera != null) {
            mUVCCamera.startPreview();
        }
    }

    public void stopPreview(){
        if (mUVCCamera != null) {
            mUVCCamera.stopPreview();
        }
    }

    public Object getmSync() {
        return mSync;
    }

    public TextureView getTextureView() {
        return textureView;
    }

    public void setTextureView(TextureView textureView) {
        this.textureView = textureView;
    }
}
