package com.panyko.pcamera;


import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private TextureView mTextureView;
    private ImageView mImageView;
    private MediaPlayer mediaPlayer;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private String filePath;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextureView = findViewById(R.id.view_texture);
        mImageView = findViewById(R.id.view_picture);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "onSurfaceTextureAvailable: ");
                if (mSurfaceTexture == null) {
                    mSurfaceTexture = surface;
                } else {
                    mTextureView.setSurfaceTexture(mSurfaceTexture);
                }
                videoPlay(filePath);
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                Log.i(TAG, "onSurfaceTextureSizeChanged: ");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                Log.i(TAG, "onSurfaceTextureDestroyed: ");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        if (requestCode == 0) {
            switch (resultCode) {
                case 1:
                    int type = data.getIntExtra("type", -1);
                    filePath = data.getStringExtra("path");
                    File file = new File(filePath);
                    if (!file.exists()) return;
                    if (type == 0) {
                        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                        if (bitmap != null) {
                            mImageView.setImageBitmap(bitmap);
                            mImageView.setVisibility(View.VISIBLE);
                            mTextureView.setVisibility(View.GONE);
                        }
                    } else if (type == 1) {
                        mImageView.setVisibility(View.GONE);
                        mTextureView.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }

    public void captureClick(View view) {
        startActivityForResult(new Intent(this, CaptureActivity.class), 0);
    }

    /**
     * 播放视频
     */
    public void videoPlay(String videoPath) {
        try {
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
}