package com.panyko.pcamera.util;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Environment;

import com.panyko.pcamera.state.CameraState;

/**
 * 通用值
 */
public class CommonValue {
    //最大录像时间
    public static long MAX_RECORD_TIME = 15 * 1000;
    //最小录像时间
    public static long MIN_RECORD_TIME = 2 * 1000;
    //拍摄状态
    public static int CAMERA_STATE = CameraState.STATE_IDLE;
    //预览的宽度
    public static int PREVIEW_WIDTH = 1080;
    //预览的高度
    public static int PREVIEW_HEIGHT = 1920;
    //拍摄保存的宽度
    public static int CAPTURE_WIDTH = 1080;
    //拍摄保存的高度
    public static int CAPTURE_HEIGHT = 1920;
    //texture的宽度
    public static int TEXTURE_WIDTH = 1080;
    //texture的高度
    public static int TEXTURE_HEIGHT = 1920;
    //手机方向
    public static int DISPLAY_ROTATION;
    //摄像头方向
    public static int CAMERA_ORIENTATION = 0;
    //摄像头，默认后置
    public static int CAMERA_FACING = CameraCharacteristics.LENS_FACING_BACK;
    //拍照片保存的文件夹路径
    public static String PICTURE_FOLDER_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
    //录像保存的文件夹路径
    public static String VIDEO_FOLDER_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
}
