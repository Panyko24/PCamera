package com.panyko.pcamera.listener;

import android.graphics.Bitmap;

/**
 * 相机回调
 */
public interface PCameraListener {
    /**
     * 拍照成功回调
     */
    void onTakePictureSuccess(String picturePath, Bitmap bitmap);

    /**
     * 拍照失败回调
     */
    void onTakePictureFail(String reason);

    /**
     * 录像成功回调
     */
    void onRecordVideoSuccess(String videoPath, Bitmap bitmap);

    /**
     * 录像失败回调
     */
    void onRecordVideoFail(String reason);

    /**
     * 出错回调
     */
    void onError(String reason);

    /**
     * 结束回调
     */
    void onFinish();
}
