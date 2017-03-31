package cn.co.willow.android.ultimate.gpuimage.manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;

import cn.co.willow.android.ultimate.gpuimage.core_config.RecordCoderState;
import cn.co.willow.android.ultimate.gpuimage.core_config.Rotation;
import cn.co.willow.android.ultimate.gpuimage.core_record_18.VideoRecorderRenderer;
import cn.co.willow.android.ultimate.gpuimage.core_render_filter.GPUImageFilter;
import cn.co.willow.android.ultimate.gpuimage.utils.CameraUtil;
import cn.co.willow.android.ultimate.gpuimage.utils.LogUtil;

/**
 * 视频录制滤镜管理器
 * <p>
 * Created by willow.li on 2016/10/26.
 */
public class VideoFilterManager {

    private VideoRecorderRenderer mRenderer;
    private GLSurfaceView mGlSurfaceView;
    private GPUImageFilter mFilter;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public VideoFilterManager(final Context context) {
        supportsOpenGLES3(context);
        mFilter = new GPUImageFilter();
        mRenderer = new VideoRecorderRenderer(mFilter);
    }

    /*关键设置======================================================================================*/
    /** 检测是否支持OpenGl */
    private void supportsOpenGLES3(final Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        if (configurationInfo.reqGlEsVersion < 0x30000) {
            throw new IllegalStateException("OpenGL ES 3.0 is not supported on this phone.");
        }
    }

    /** 初始化GLSurfaceView */
    public void setGLSurfaceView(final GLSurfaceView view) {
        mGlSurfaceView = view;
        mGlSurfaceView.setEGLContextClientVersion(2);
        mGlSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        mGlSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        mGlSurfaceView.setRenderer(mRenderer);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        mGlSurfaceView.requestRender();
    }

    /** 设置相机，并切换角度 */
    public void setUpCamera(final Camera camera, boolean isFrontCame,Activity context) {
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            camera.setDisplayOrientation(0);//todo added by zhourihu 2017年3月31日14:25:14 这块判断逻辑有问题 google手机会倒置 魅族手机会预览和录制方向相反
            mRenderer.setUpSurfaceTexture(camera);
           int mCurrentCameraId=isFrontCame?1:0;
            int orientation = CameraUtil.getCameraDisplayOrientation(
                    context, mCurrentCameraId);
//            mCameraInstance.setDisplayOrientation(orientation); todo 魅族手机录制结果和预览结果方向差180度

            Rotation rotation = Rotation.NORMAL;
            switch (orientation) {
                case 90:
                    rotation = Rotation.ROTATION_90;
                    break;
                case 180:
                    rotation = Rotation.ROTATION_180;
                    break;
                case 270:
                    rotation = Rotation.ROTATION_270;
                    break;
            }
            Log.e("rotation",String.format("orientation==%s,isFrontCame==%s",orientation,isFrontCame));
            mRenderer.setRotation(rotation,false, isFrontCame);

//            mRenderer.setRotation(isFrontCame ? Rotation.ROTATION_270 : Rotation.ROTATION_90, false, isFrontCame);
        } else {
            camera.setPreviewCallback(mRenderer);
            camera.startPreview();
        }
    }

    /** 设置滤镜 */
    public void setFilter(final GPUImageFilter filter) {
        mFilter = filter;
        mRenderer.setFilter(mFilter);
        requestRender();
    }

    /** 请求刷新渲染器 */
    public void requestRender() {
        if (mGlSurfaceView != null) {
            mGlSurfaceView.requestRender();
        }
    }

    /*录制逻辑======================================================================================*/
    public void clearRecorderInstance() {
        mRenderer.clearAll();
        mRenderer = null;
    }

    public void createNewRecorderInstance(File mOutputRecFile, final CamcorderProfile mProfile) {
        mRenderer.prepareCoder(mOutputRecFile,
                mProfile.videoFrameHeight,          // 系统旋转长宽相反
                mProfile.videoFrameWidth,
                mProfile.videoBitRate);
    }

    public void start() {
        mRenderer.changeCoderState(true);
    }

    public void stop() {
        mRenderer.changeCoderState(false);
    }

    public void release() {
        mRenderer.releaseCoder();
    }

    public RecordCoderState getCurrentState() {
        return mRenderer.getCurrentState();
    }


    /*渲染流程关联性监听============================================================================*/
    public void setOnSurfaceSetListener(VideoRenderer.OnSurfaceSetListener mOnSurfaceSetListener) {
        mRenderer.setOnSurfaceSetListener(mOnSurfaceSetListener);
    }

    public void setOnRecordStateListener(VideoRecorderRenderer.OnRecordStateListener mOnRecordStateListener) {
        mRenderer.setOnRecordStateListener(mOnRecordStateListener);
    }
}
