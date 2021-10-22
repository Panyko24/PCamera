package com.panyko.pcamera.state;

public class CameraState {
    public static final int STATE_IDLE = 0X001;//闲置状态
    public static final int STATE_PREVIEW = 0X002;//预览状态
    public static final int STATE_PICTURE_TAKE = 0X003;//拍照状态
    public static final int STATE_VIDEO_RECORD = 0X004;//录像状态
    public static final int STATE_PICTURE_SHOW = 0X005; //显示图片状态
    public static final int STATE_VIDEO_PLAY = 0X006; //播放视频状态
}
