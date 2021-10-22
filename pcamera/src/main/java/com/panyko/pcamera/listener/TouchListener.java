package com.panyko.pcamera.listener;

/**
 * 触摸接口
 */
public interface TouchListener {
    /**
     * 拍照动作
     */
    void onActionTakePicture();

    /**
     * 开始录像动作
     */
    void onActionRecordStart();

    /**
     * 结束录像回调
     *
     * @param time 录像时长 毫秒
     */
    void onActionRecordEnd(long time);

}
