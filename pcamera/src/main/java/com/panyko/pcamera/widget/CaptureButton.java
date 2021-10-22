package com.panyko.pcamera.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;


import com.panyko.pcamera.listener.TouchListener;
import com.panyko.pcamera.util.CommonValue;

public class CaptureButton extends View implements ViewTreeObserver.OnPreDrawListener {
    private Paint mPaint;
    private float circleRadius;//圆的半径，等同于外圆的半径
    private float outsideCircleRadius; //外圆的半径
    private float insideCircleRadius;//内圆的半径
    private int outsideCircleAddSize;//外圆放大的尺寸
    private int insideCircleReduceSize;//内圆缩小的尺寸
    private float progressWidth;//进度条宽度
    private float progress;//进度条
    private float x; //圆中心点的x轴
    private float y; //中心点的y轴
    private int state;//状态
    private static final int STATE_IDLE = 0X001;//空闲状态
    private static final int STATE_PRESS = 0x002;//点击状态
    private static final int STATE_LONG_PRESS = 0x003;//长按状态
    private CountDownTimer mCountDownTimer;
    private TouchListener mTouchListener;
    private long recordTime;//录像时间

    public CaptureButton(Context context) {
        super(context);
        init();
    }

    public CaptureButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public void setTouchListener(TouchListener mTouchListener) {
        this.mTouchListener = mTouchListener;
    }

    /**
     * 初始化
     */
    private void init() {
        getViewTreeObserver().addOnPreDrawListener(this);
        mPaint = new Paint();
        mPaint.setAntiAlias(true);//抗锯齿
        mCountDownTimer = new CountDownTimer(CommonValue.MAX_RECORD_TIME, 15 * 1000 / 360) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateProgress(millisUntilFinished);
            }

            @Override
            public void onFinish() {
                if (mTouchListener != null) {
                    mTouchListener.onActionRecordEnd(recordTime);
                }
                updateProgress(0);
                state = STATE_IDLE;
                progress = 0;
                startAnimation(outsideCircleRadius, circleRadius, insideCircleRadius, circleRadius * 0.75f);
            }
        };
    }

    /**
     * 重新设置大小
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //setViewSize();
        //setMeasuredDimension((int) ((outsideCircleRadius + outsideCircleAddSize) * 2), (int) ((outsideCircleRadius + outsideCircleAddSize) * 2));
    }

    /**
     * 设置图形尺寸
     */
    private void setViewSize() {
        //取宽度和高度的最小值作为圆的尺寸
        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        //进度条宽度
        progressWidth = size / 15f;
        //外圆缩放的大小
        outsideCircleAddSize = size / 2 / 5;
        //内圆缩放的大小
        insideCircleReduceSize = size / 2 / 10;
        //圆的半径
        circleRadius = (float) (size / 2 - outsideCircleAddSize);
        //外圆半径
        outsideCircleRadius = circleRadius;
        //内圆半径
        insideCircleRadius = circleRadius * 0.75f;
        //圆的x轴
        //x = outsideCircleRadius + outsideCircleAddSize;
        x = getMeasuredWidth() / 2f;
        //圆的y轴
        //y = outsideCircleRadius + outsideCircleAddSize;
        y = getMeasuredHeight() / 2f;
    }

    @Override
    public boolean onPreDraw() {
        setViewSize();
        getViewTreeObserver().removeOnPreDrawListener(this);
        return false;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //画笔类型
        mPaint.setStyle(Paint.Style.FILL);
        //绘制外圆
        mPaint.setColor(0xEEDCDCDC);
        canvas.drawCircle(x, y, outsideCircleRadius, mPaint);
        //绘制内圆
        mPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(x, y, insideCircleRadius, mPaint);
        if (state == STATE_LONG_PRESS) {
            //绘制进度条
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0xEE16AE16);
            mPaint.setStrokeWidth(progressWidth);
            //计算大小
            //路径要跟变大后的外圆重合
            RectF rectF = new RectF(getMeasuredWidth() / 2f - (circleRadius + outsideCircleAddSize) + progressWidth / 2,
                    getMeasuredHeight() / 2f - (circleRadius + outsideCircleAddSize) + progressWidth / 2,
                    getMeasuredWidth() / 2f + (circleRadius + outsideCircleAddSize) - progressWidth / 2,
                    getMeasuredHeight() / 2f + (circleRadius + outsideCircleAddSize) - progressWidth / 2);
            canvas.drawArc(rectF, -90, progress, false, mPaint);
        }
    }

    /**
     * 更新进度条
     *
     * @param millisUntilFinished 剩余时间
     */
    private void updateProgress(long millisUntilFinished) {
        //计算时间
        recordTime = CommonValue.MAX_RECORD_TIME - millisUntilFinished;
        //计算出进度
        //剩余时间÷总时间=剩余百分点，剩余百分点×总份数=剩余份数，总份数-剩余份数=已用份数
        progress = 360f - millisUntilFinished / ((float) (15 * 1000)) * 360f;
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: //按下动作
                state = STATE_PRESS;
                //300毫秒还没抬起，表示录像动作
                postDelayed(longPressRun, 500);
                break;
            case MotionEvent.ACTION_MOVE: //移动动作
                break;
            case MotionEvent.ACTION_UP: //抬起动作

                removeCallbacks(longPressRun);

                if (state == STATE_PRESS) { //拍照动作
                    if (mTouchListener != null) {
                        mTouchListener.onActionTakePicture();
                    }
                } else if (state == STATE_LONG_PRESS) { //录像动作
                    if (mTouchListener != null) {
                        mTouchListener.onActionRecordEnd(recordTime);
                    }
                    state = STATE_IDLE;
                    progress = 0;
                    mCountDownTimer.cancel();
                    startAnimation(outsideCircleRadius, circleRadius, insideCircleRadius, circleRadius * 0.75f);

                }
                break;
        }
        return true;
    }

    private final Runnable longPressRun = new Runnable() {
        @Override
        public void run() {
            state = STATE_LONG_PRESS;
            startAnimation(outsideCircleRadius, outsideCircleRadius + outsideCircleAddSize, insideCircleRadius, insideCircleRadius - insideCircleReduceSize);
        }
    };

    /**
     * 开始缩放动画效果
     *
     * @param outsideCircleStart 原大小
     * @param outsideCircleEnd   缩放大小
     */
    private void startAnimation(float outsideCircleStart, float outsideCircleEnd, float insideCircleStart, float insideCircleEnd) {
        //外圆动画
        ValueAnimator outsideAnimator = ValueAnimator.ofFloat(outsideCircleStart, outsideCircleEnd);
        outsideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                outsideCircleRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        //内圆动画
        ValueAnimator insideAnimator = ValueAnimator.ofFloat(insideCircleStart, insideCircleEnd);
        insideAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                insideCircleRadius = (float) animation.getAnimatedValue();
                invalidate();
            }
        });
        //动画集合
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(outsideAnimator, insideAnimator);
        animatorSet.setDuration(100);
        animatorSet.start();
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (state == STATE_LONG_PRESS) {
                    mCountDownTimer.start();
                    if (mTouchListener != null) {
                        mTouchListener.onActionRecordStart();
                    }
                }
            }
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }
}
