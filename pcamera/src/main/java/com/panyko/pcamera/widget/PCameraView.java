package com.panyko.pcamera.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.SurfaceTexture;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.panyko.pcamera.R;
import com.panyko.pcamera.listener.PCameraListener;

import com.panyko.pcamera.listener.PCaptureListener;
import com.panyko.pcamera.listener.TouchListener;
import com.panyko.pcamera.state.CameraState;
import com.panyko.pcamera.state.CaptureStateCode;
import com.panyko.pcamera.util.CommonValue;
import com.panyko.pcamera.util.PCameraManager;

import java.io.File;

public class PCameraView extends FrameLayout {
    private TextureView viewTexture;//预览的视图
    private ImageView viewPicture;//照片显示视图
    private ImageButton btnSwitchCamera;//切换摄像头按钮
    private ImageButton btnReturn; //返回按钮
    private ImageButton btnCancel; //取消按钮(拍摄之后)
    private ImageButton btnComplete; //完成按钮(拍摄之后)
    private TextView textTips;//提示框
    private CaptureButton btnCapture;//拍摄按钮
    private Context mContext;
    private PCameraManager mPCameraManager;
    private PCaptureListener mPCaptureListener;
    private String filePath;//文件路径
    private Bitmap bitmap;//照片
    private static final String TAG = "PCameraView";

    public PCameraView(@NonNull Context context) {
        this(context, null);
    }

    public PCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public PCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        initView();
        initListener();
    }

    public void setPCaptureListener(PCaptureListener mPCaptureListener) {
        this.mPCaptureListener = mPCaptureListener;
    }

    public void onResume() {
        if (viewTexture.isAvailable()) {
            if (CommonValue.CAMERA_STATE == CameraState.STATE_IDLE || CommonValue.CAMERA_STATE == CameraState.STATE_PREVIEW) {
                mPCameraManager.setCamera();
            } else if (CommonValue.CAMERA_STATE == CameraState.STATE_VIDEO_PLAY) {
                mPCameraManager.videoPlay();
            }
        } else {
            viewTexture.setSurfaceTextureListener(surfaceTextureListener);
        }
    }

    public void onPause() {
        mPCameraManager.release();
    }

    private void initView() {
        setWillNotDraw(false);
        View view = LayoutInflater.from(mContext).inflate(R.layout.view_camera, this);
        viewTexture = view.findViewById(R.id.view_texture);
        btnSwitchCamera = view.findViewById(R.id.btn_camera_switch);
        btnReturn = view.findViewById(R.id.btn_return);
        btnCancel = view.findViewById(R.id.btn_cancel);
        btnComplete = view.findViewById(R.id.btn_complete);
        textTips = view.findViewById(R.id.text_tips);
        btnCapture = view.findViewById(R.id.btn_capture);
        viewPicture = view.findViewById(R.id.view_picture);
        mPCameraManager = new PCameraManager(mContext);
    }

    private void initListener() {

        //切换摄像头
        btnSwitchCamera.setOnClickListener(v -> {
            mPCameraManager.cameraSwitch();
        });
        //返回
        btnReturn.setOnClickListener(v -> {
            if (mPCaptureListener != null) {
                mPCaptureListener.onFinish();
            }
        });
        //取消
        btnCancel.setOnClickListener(v -> {
            if (!TextUtils.isEmpty(filePath)) {
                File file = new File(filePath);
                if (file.exists()) {
                    file.delete();
                }
            }
            filePath = null;
            bitmap = null;
            textTips.setVisibility(VISIBLE);
            viewTexture.setVisibility(VISIBLE);
            viewPicture.setVisibility(GONE);
            btnCancel.setVisibility(GONE);
            btnCapture.setVisibility(VISIBLE);
            btnComplete.setVisibility(GONE);
            btnReturn.setVisibility(VISIBLE);
            btnSwitchCamera.setVisibility(VISIBLE);
            mPCameraManager.release();
            CommonValue.CAMERA_STATE = CameraState.STATE_PREVIEW;
            if (viewTexture.isAvailable()) {
                mPCameraManager.setCamera();
            } else {
                viewTexture.setSurfaceTextureListener(surfaceTextureListener);
            }
        });
        //完成
        btnComplete.setOnClickListener(v -> {
            if (CommonValue.CAMERA_STATE == CameraState.STATE_PICTURE_SHOW) {
                mPCaptureListener.onTakePicture(CaptureStateCode.CODE_SUCCESS, filePath, bitmap, "拍照成功");
            } else if (CommonValue.CAMERA_STATE == CameraState.STATE_VIDEO_PLAY) {
                mPCaptureListener.onRecordVideo(CaptureStateCode.CODE_SUCCESS, filePath, bitmap, "录像成功");
            }
            if (mPCaptureListener != null) {
                mPCaptureListener.onFinish();
            }
        });
        //拍摄
        btnCapture.setTouchListener(new TouchListener() {
            @Override
            public void onActionTakePicture() {
                //拍照动作
                mPCameraManager.takePhoto();
            }

            @Override
            public void onActionRecordStart() {
                //开始录像动作
                mPCameraManager.startRecord();
            }

            @Override
            public void onActionRecordEnd(long time) {
                //结束录像动作
                mPCameraManager.stopRecord(time);
            }
        });

        mPCameraManager.setCameraListener(pCameraListener);
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            if (CommonValue.CAMERA_STATE == CameraState.STATE_IDLE || CommonValue.CAMERA_STATE == CameraState.STATE_PREVIEW) {
                CommonValue.TEXTURE_WIDTH = width;
                CommonValue.TEXTURE_HEIGHT = height;
                mPCameraManager.setSurfaceTexture(surface);
                mPCameraManager.setCamera();
            } else if (CommonValue.CAMERA_STATE == CameraState.STATE_VIDEO_PLAY) {
                mPCameraManager.videoPlay();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            //预览销毁
            mPCameraManager.release();
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    /**
     * 拍摄回调
     */
    private PCameraListener pCameraListener = new PCameraListener() {
        @Override
        public void onTakePictureSuccess(String picturePath, Bitmap pictureBitmap) {
            filePath = picturePath;
            bitmap = pictureBitmap;
            textTips.setVisibility(GONE);
            viewPicture.setImageBitmap(bitmap);
            viewTexture.setVisibility(GONE);
            viewPicture.setVisibility(VISIBLE);
            btnCancel.setVisibility(VISIBLE);
            btnCapture.setVisibility(GONE);
            btnComplete.setVisibility(VISIBLE);
            btnReturn.setVisibility(GONE);
            btnSwitchCamera.setVisibility(GONE);

        }

        @Override
        public void onTakePictureFail(String reason) {
            Toast.makeText(mContext, reason, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onRecordVideoSuccess(String videoPath, Bitmap coverBitmap) {
            filePath = videoPath;
            bitmap = coverBitmap;
            textTips.setVisibility(GONE);
            viewTexture.setVisibility(VISIBLE);
            viewPicture.setVisibility(GONE);
            btnCancel.setVisibility(VISIBLE);
            btnCapture.setVisibility(GONE);
            btnComplete.setVisibility(VISIBLE);
            btnReturn.setVisibility(GONE);
            btnSwitchCamera.setVisibility(GONE);
            mPCameraManager.releaseCamera();
            mPCameraManager.videoPlay();
        }

        @Override
        public void onRecordVideoFail(String reason) {
            Toast.makeText(mContext, reason, Toast.LENGTH_SHORT).show();
            mPCameraManager.releaseCamera();
            mPCameraManager.setCamera();
        }

        @Override
        public void onError(String reason) {
            mPCaptureListener.onError(CaptureStateCode.CODE_ERROR, reason);
        }

        @Override
        public void onFinish() {
            mPCaptureListener.onFinish();
        }
    };

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

}
