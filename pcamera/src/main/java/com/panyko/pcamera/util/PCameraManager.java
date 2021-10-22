package com.panyko.pcamera.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import com.panyko.pcamera.listener.PCameraListener;
import com.panyko.pcamera.state.CameraState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PCameraManager {
    private static final String TAG = "PCameraManager";
    private Context mContext;
    private Handler mainHandler, childHandler;
    private CameraManager mCameraManager;//摄像头管理器
    private ImageReader mImageReader;
    private CameraDevice mCameraDevice;//描述系统摄像头，类似于早期的Camera
    private CameraCaptureSession mCameraCaptureSession; //CaptureSession会话
    private Surface mSurface;
    private SurfaceTexture mSurfaceTexture;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;
    private int mDisplayRotation;//手机方向
    private int mCameraSensorOrientation = 0;//摄像头方向
    private Size mPreviewSize;//预览的大小
    private Size mCaptureSize;//保存的大小
    private String mCameraId;//摄像头id
    private boolean canCapture;//是否能拍摄
    private String picturePath;//照片路径
    private String videoPath;//视频路径
    private PCameraListener mCameraListener;

    public PCameraManager(Context mContext) {
        this.mContext = mContext;
        init();
    }

    public void setCameraListener(PCameraListener mCameraListener) {
        this.mCameraListener = mCameraListener;
    }

    public void setSurfaceTexture(SurfaceTexture mSurfaceTexture) {
        this.mSurfaceTexture = mSurfaceTexture;
    }

    private void init() {
        mCameraId = "0";
        CommonValue.CAMERA_STATE = CameraState.STATE_IDLE;
        mDisplayRotation = CommonValue.DISPLAY_ROTATION;
        mCameraSensorOrientation = CommonValue.CAMERA_ORIENTATION;
        mPreviewSize = new Size(CommonValue.PREVIEW_WIDTH, CommonValue.PREVIEW_HEIGHT);
        mCaptureSize = new Size(CommonValue.CAPTURE_WIDTH, CommonValue.CAPTURE_HEIGHT);
    }

    /**
     * 设置相机
     */
    @SuppressLint("MissingPermission")
    public void setCamera() {
        try {
            HandlerThread handlerThread = new HandlerThread("Camera2");
            handlerThread.start();
            childHandler = new Handler(handlerThread.getLooper());
            mainHandler = new Handler(mContext.getMainLooper());
            mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            //获取所有可用的摄像头
            String[] cameraIdList = mCameraManager.getCameraIdList();
            if (cameraIdList == null || cameraIdList.length == 0) {
                Toast.makeText(mContext, "没有可用的摄像头", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String id : cameraIdList) {
                //获取到相机的各种信息
                CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(id);
                if (cameraCharacteristics != null) {
                    int facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing == CommonValue.CAMERA_FACING) {
                        mCameraId = id;
                        //获取摄像头方向
                        mCameraSensorOrientation = cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        StreamConfigurationMap streamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        if (streamConfigurationMap != null) {
                            Size[] saveSupportSizes = streamConfigurationMap.getOutputSizes(ImageFormat.JPEG);
                            Size[] previewSupportSizes = streamConfigurationMap.getOutputSizes(SurfaceTexture.class);
                            //判断是否需要交换宽高
                            boolean exchange = exchangeWidthAndHeight(mDisplayRotation, mCameraSensorOrientation);
                            //计算出最佳尺寸大小
                            if (previewSupportSizes != null) {
                                if (exchange) {
                                    mPreviewSize = getBestSize(CommonValue.PREVIEW_HEIGHT, CommonValue.PREVIEW_WIDTH, CommonValue.TEXTURE_HEIGHT, CommonValue.TEXTURE_WIDTH, Arrays.asList(previewSupportSizes));

                                } else {
                                    mPreviewSize = getBestSize(CommonValue.PREVIEW_WIDTH, CommonValue.PREVIEW_HEIGHT, CommonValue.TEXTURE_WIDTH, CommonValue.TEXTURE_HEIGHT, Arrays.asList(previewSupportSizes));
                                }
                            }
                            if (saveSupportSizes != null) {
                                if (exchange) {
                                    mCaptureSize = getBestSize(CommonValue.CAPTURE_HEIGHT, CommonValue.CAPTURE_WIDTH, CommonValue.CAPTURE_HEIGHT, CommonValue.CAPTURE_WIDTH, Arrays.asList(saveSupportSizes));

                                } else {
                                    mCaptureSize = getBestSize(CommonValue.CAPTURE_WIDTH, CommonValue.CAPTURE_HEIGHT, CommonValue.CAPTURE_WIDTH, CommonValue.CAPTURE_HEIGHT, Arrays.asList(saveSupportSizes));
                                }
                            }

                        }
                    }
                }
            }
            Log.i(TAG, String.format("预览最佳的尺寸，宽度：%s,高度：%s", mPreviewSize.getWidth(), mPreviewSize.getHeight()));
            Log.i(TAG, String.format("照片最佳的尺寸，宽度：%s,高度：%s", mCaptureSize.getWidth(), mCaptureSize.getHeight()));
            //设置预览尺寸
            mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            //设置照片尺寸
            mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(), ImageFormat.JPEG, 1);
            mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    FileOutputStream fos = null;
                    try {
                        // 拿到拍照照片数据
                        Image image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        //必须关闭Image，否则无法继续拍照
                        image.close();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        //前置摄像头
                        if (CommonValue.CAMERA_FACING == CameraCharacteristics.LENS_FACING_FRONT) {
                            Matrix matrix = new Matrix();
                            matrix.postScale(-1, 1);//镜像水平翻转
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        }
                        File file = new File(CommonValue.PICTURE_FOLDER_PATH, System.currentTimeMillis() + ".jpg");
                        picturePath = file.getAbsolutePath();
                        fos = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                        fos.flush();

                        if (mCameraListener != null) {
                            if (!TextUtils.isEmpty(videoPath)) {
                                File videoFile = new File(videoPath);
                                if (videoFile.exists()) {
                                    videoFile.delete();
                                }
                            }
                            CommonValue.CAMERA_STATE = CameraState.STATE_PICTURE_SHOW;
                            mCameraListener.onTakePictureSuccess(picturePath, bitmap);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (!TextUtils.isEmpty(picturePath) && new File(picturePath).exists()) {
                            new File(picturePath).delete();
                        }
                        if (mCameraListener != null) {
                            CommonValue.CAMERA_STATE = CameraState.STATE_PREVIEW;
                            mCameraListener.onError(e.getMessage());
                        }
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }, mainHandler);
            mCameraManager.openCamera(mCameraId, stateCallback, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据提供的屏幕方向 [displayRotation] 和相机方向 [sensorOrientation] 返回是否需要交换宽高
     */
    private boolean exchangeWidthAndHeight(int displayRotation, int sensorOrientation) {
        boolean exchane = false;
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    exchane = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    exchane = true;
                }
                break;
        }
        Log.i(TAG, "屏幕方向：" + displayRotation);
        Log.i(TAG, "相机方向：" + sensorOrientation);
        return exchane;
    }

    /**
     * 摄像头切换
     */
    public void cameraSwitch() {
        if (!canCapture || mCameraDevice == null) return;
        if (CommonValue.CAMERA_FACING == CameraCharacteristics.LENS_FACING_BACK) {
            CommonValue.CAMERA_FACING = CameraCharacteristics.LENS_FACING_FRONT;
        } else {
            CommonValue.CAMERA_FACING = CameraCharacteristics.LENS_FACING_BACK;
        }
        releaseCamera();
        setCamera();
    }

    /**
     * 根据提供的参数值返回与指定宽高相等或最接近的尺寸
     *
     * @param targetWidth  目标宽度
     * @param targetHeight 目标高度
     * @param maxWidth     最大宽度(即TextureView的宽度)
     * @param maxHeight    最大高度(即TextureView的高度)
     * @param sizeList     支持的Size列表
     * @return 返回与指定宽高相等或最接近的尺寸
     */
    private Size getBestSize(int targetWidth, int targetHeight, int maxWidth, int maxHeight, List<Size> sizeList) {
        //从小到大排序
        for (int i = 0; i < sizeList.size() - 1; i++) {
            for (int j = i + 1; j < sizeList.size(); j++) {
                if (sizeList.get(i).getWidth() > sizeList.get(j).getWidth()) {
                    Size tempSize = sizeList.get(i);
                    sizeList.set(i, sizeList.get(j));
                    sizeList.set(j, tempSize);
                }
            }
        }
        List<Size> largeSizeList = new ArrayList<>();
        List<Size> smallSizeList = new ArrayList<>();
        for (Size supportSize : sizeList) {
            if (supportSize.getWidth() <= maxWidth
                    && supportSize.getHeight() <= maxHeight
                    && supportSize.getWidth() == (supportSize.getHeight() * targetWidth / targetHeight)) {
                if (supportSize.getWidth() >= targetWidth && supportSize.getHeight() >= targetHeight) {
                    largeSizeList.add(supportSize);
                } else {
                    smallSizeList.add(supportSize);
                }
            }
        }
        if (largeSizeList.size() > 0) {
            return largeSizeList.get(0);//取最小的那个
        } else if (smallSizeList.size() > 0) {
            return smallSizeList.get(smallSizeList.size() - 1);//取最大的那个
        } else {
            if (sizeList.size() > 0) {
                return sizeList.get(sizeList.size() - 1);
            } else {
                return new Size(targetWidth, targetHeight);
            }
        }
    }

    /**
     * 打开摄像头回调
     */
    private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            //打开摄像头
            mCameraDevice = camera;
            CommonValue.CAMERA_STATE = CameraState.STATE_PREVIEW;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            //关闭摄像头
            if (mCameraDevice != null) {
                mCameraDevice.close();
            }
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            //出错
            Toast.makeText(mContext, "打开摄像头失败：" + error, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 开始预览
     */
    private void startPreview() {

        try {
            if (mSurface == null) {
                mSurface = new Surface(mSurfaceTexture);
            }
            //初始化录音
            initMediaRecorder();
            // 创建预览需要的CaptureRequest.Builder
            //TEMPLATE_PREVIEW : 创建预览的请求
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 将TextureView的surface作为CaptureRequest.Builder的目标
            builder.addTarget(mSurface);
            if (mediaRecorder != null) {
                builder.addTarget(mediaRecorder.getSurface());
            }
            // 自动对焦
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 打开闪光灯
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            // 创建CameraCaptureSession，该对象负责管理处理预览请求、拍照请求和录像请求
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface(), mediaRecorder.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        // 当摄像头已经准备好时，开始显示预览
                        mCameraCaptureSession = session;
                        // 显示预览
                        CaptureRequest previewRequest = builder.build();
                        mCameraCaptureSession.setRepeatingRequest(previewRequest, captureCallback, childHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(mContext, "配置失败", Toast.LENGTH_SHORT).show();
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始预览回调
     */
    private final CameraCaptureSession.CaptureCallback captureCallback = new CameraCaptureSession.CaptureCallback() {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);
            canCapture = true;
        }

        @Override
        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
            super.onCaptureFailed(session, request, failure);
            Toast.makeText(mContext, "开启预览失败", Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 初始化录像
     */
    private void initMediaRecorder() {
        releaseMediaRecorder();
        mediaRecorder = new MediaRecorder();
        //设置音频来源
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        //设置视频来源
        //camera2要选择SURFACE
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        //设置输出格式
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        //设置音频编码
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //设置视频编码
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        //设置比特率 一般是 1*分辨率~10*分辨率之间
        mediaRecorder.setVideoEncodingBitRate(8 * mPreviewSize.getWidth() * mPreviewSize.getHeight());
        //设置帧数，一般30以内
        mediaRecorder.setVideoFrameRate(30);
        //设置尺寸
        mediaRecorder.setVideoSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        //设置方向
        mediaRecorder.setOrientationHint(mCameraSensorOrientation);
        //设置预览
        mediaRecorder.setPreviewDisplay(mSurface);
        //设置输出路径
        File folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        videoPath = new File(folder, System.currentTimeMillis() + ".mp4").getAbsolutePath();
        mediaRecorder.setOutputFile(videoPath);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            mediaRecorder = null;
        }
    }

    /**
     * 开始拍照
     */
    public void takePhoto() {
        if (!canCapture || mCameraDevice == null) {
            //Toast.makeText(mContext, "无法拍照", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            CommonValue.CAMERA_STATE = CameraState.STATE_PICTURE_TAKE;
            //创建capturerequest
            //TEMPLATE_STILL_CAPTURE: 创建一个适合于静态图像捕获的请求，图像质量优先于帧速率
            CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将ImageReader的surface作为CaptureRequest.Builder的目标
            builder.addTarget(mImageReader.getSurface());
            // 自动对焦
            builder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 自动曝光
            builder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            //获取手机方向
//            int rotation = getWindowManager().getDefaultDisplay().getRotation();
//            if (rotation == 0) {
//                builder.set(CaptureRequest.JPEG_ORIENTATION, Surface.ROTATION_0);
//            } else if (rotation == 90) {
//                builder.set(CaptureRequest.JPEG_ORIENTATION, Surface.ROTATION_90);
//            } else if (rotation == 180) {
//                builder.set(CaptureRequest.JPEG_ORIENTATION, Surface.ROTATION_180);
//            } else {
//                builder.set(CaptureRequest.JPEG_ORIENTATION, Surface.ROTATION_270);
//            }
            //根据摄像头方向对照片进行旋转
            builder.set(CaptureRequest.JPEG_ORIENTATION, mCameraSensorOrientation);
            CaptureRequest captureRequest = builder.build();
            mCameraCaptureSession.capture(captureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始录像
     */
    public void startRecord() {
        if (!canCapture || mCameraDevice == null || mediaRecorder == null) {
            //Toast.makeText(mContext, "无法录像", Toast.LENGTH_SHORT).show();
            return;
        }
        CommonValue.CAMERA_STATE = CameraState.STATE_VIDEO_RECORD;
        mediaRecorder.start();
    }

    /**
     * 停止录像
     */
    public void stopRecord(long time) {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.setOnErrorListener(null);
                mediaRecorder.setOnInfoListener(null);
                mediaRecorder.setPreviewDisplay(null);
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
            mediaRecorder.reset();
            mediaRecorder.release();
        } finally {
            mediaRecorder = null;
        }
        if (time < CommonValue.MIN_RECORD_TIME) { //小于最小录制时间，删除文件
            if (!TextUtils.isEmpty(videoPath)) {
                File file = new File(videoPath);
                if (file.exists()) {
                    file.delete();
                }
            }
            if (CommonValue.CAMERA_STATE == CameraState.STATE_VIDEO_RECORD) {
                if (mCameraListener != null) {
                    CommonValue.CAMERA_STATE = CameraState.STATE_PREVIEW;
                    mCameraListener.onRecordVideoFail("录制时间太短");
                }
            }
        } else {
            if (CommonValue.CAMERA_STATE == CameraState.STATE_VIDEO_RECORD) {
                if (mCameraListener != null) {
                    CommonValue.CAMERA_STATE = CameraState.STATE_VIDEO_PLAY;
                    mCameraListener.onRecordVideoSuccess(videoPath, null);
                }
            }
        }

    }

    /**
     * 播放视频
     */
    public void videoPlay() {
        try {
            Log.i(TAG, "videoPlay: ");
            if (TextUtils.isEmpty(videoPath)) return;
            mediaPlayer = new MediaPlayer();
            //设置音频模式
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //设置数据源
            mediaPlayer.setDataSource(videoPath);
            if (mSurface == null) {
                mSurface = new Surface(mSurfaceTexture);
            }
            mediaPlayer.setSurface(mSurface);
            //播放准备回调
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    //开始播放
                    mp.start();
                }
            });
            //播放完成回调
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.seekTo(0);
                    mp.start();
                }
            });
            //播放出错回调
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    return false;
                }
            });
            //准备
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    /**
     * 释放相机
     */
    public void releaseCamera() {

        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }

        canCapture = false;
        childHandler.removeCallbacksAndMessages(null);
        mainHandler.removeCallbacksAndMessages(null);
    }

    /**
     * 播放播放器
     */
    public void releaseMediaPlayer() {
        if (mediaPlayer == null) return;

        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.setSurface(null);
            mediaPlayer.reset();
            mediaPlayer.release();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            mediaPlayer.release();
        } finally {
            mediaPlayer = null;
        }
    }

    /**
     * 释放录像
     */
    public void releaseMediaRecorder() {
        if (mediaRecorder == null) return;
        try {
            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setOnInfoListener(null);
            mediaRecorder.setPreviewDisplay(null);
            if (CommonValue.CAMERA_STATE == CameraState.STATE_VIDEO_RECORD) {
                mediaRecorder.stop();
            }
            mediaRecorder.release();
        } catch (Exception e) {
            e.printStackTrace();
            mediaRecorder.release();
        } finally {
            mediaRecorder = null;
        }
    }

    public void release() {
        if (CommonValue.CAMERA_STATE == CameraState.STATE_PREVIEW) {
            if (!TextUtils.isEmpty(picturePath)) {
                File file = new File(picturePath);
                if (file.exists()) {
                    file.delete();
                }
            }
            if (!TextUtils.isEmpty(videoPath)) {
                File file = new File(videoPath);
                if (file.exists()) {
                    file.delete();
                }
            }
        }
        releaseCamera();
        releaseMediaPlayer();
        releaseMediaRecorder();
    }
}
