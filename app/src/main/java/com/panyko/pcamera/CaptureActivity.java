package com.panyko.pcamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.panyko.pcamera.listener.PCameraListener;
import com.panyko.pcamera.listener.PCaptureListener;
import com.panyko.pcamera.widget.PCameraView;

public class CaptureActivity extends AppCompatActivity {
    private PCameraView viewPCamera;
    private static final String TAG = "CaptureActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_capture);
        viewPCamera = findViewById(R.id.view_pcamera);
        init();
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewPCamera.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        viewPCamera.onPause();
    }

    private void init() {
        viewPCamera.setPCaptureListener(new PCaptureListener() {
            @Override
            public void onTakePicture(int code, String picturePath, Bitmap bitmap, String msg) {
                Log.i(TAG, String.format("拍照返回：%s,路径：%s,消息：%s", code, picturePath, msg));
                Intent intent = new Intent();
                intent.putExtra("type", 0);
                intent.putExtra("path", picturePath);
                setResult(1, intent);
                finish();
            }

            @Override
            public void onRecordVideo(int code, String videoPath, Bitmap coverBitmap, String msg) {
                Log.i(TAG, String.format("录像返回：%s,路径：%s,消息：%s", code, videoPath, msg));
                Intent intent = new Intent();
                intent.putExtra("type", 1);
                intent.putExtra("path", videoPath);
                setResult(1, intent);
                finish();
            }

            @Override
            public void onError(int code, String reason) {
                Toast.makeText(CaptureActivity.this, reason, Toast.LENGTH_SHORT).show();
                Log.i(TAG, String.format("拍摄出错：%s,原因：%s", code, reason));
            }

            @Override
            public void onFinish() {
                finish();
            }
        });
    }
}