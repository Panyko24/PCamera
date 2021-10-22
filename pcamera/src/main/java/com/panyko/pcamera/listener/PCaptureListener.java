package com.panyko.pcamera.listener;

import android.graphics.Bitmap;

/**
 * 拍摄接口
 * code 0-成功 其他-失败
 */
public interface PCaptureListener {
    /**
     * 拍照回调
     *
     * @param code        返回码
     * @param picturePath 图片路径
     * @param bitmap      图片
     * @param msg         提示消息
     */
    void onTakePicture(int code, String picturePath, Bitmap bitmap, String msg);

    /**
     * 录像回调
     *
     * @param code        返回码
     * @param videoPath   视频路径
     * @param coverBitmap 封面图
     * @param msg         提示消息
     */
    void onRecordVideo(int code, String videoPath, Bitmap coverBitmap, String msg);

    /**
     * 出错回调
     *
     * @param code   返回码
     * @param reason 原因
     */
    void onError(int code, String reason);

    /**
     * 结束回调
     */
    void onFinish();
}
