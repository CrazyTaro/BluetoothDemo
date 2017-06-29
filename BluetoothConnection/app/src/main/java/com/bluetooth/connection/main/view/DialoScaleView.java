package com.bluetooth.connection.main.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

public class DialoScaleView extends View {
    // 画圆弧的画笔
    private Paint paint;
    // 正方形的宽高
    private int len;
    // 圆弧的半径
    private float radius;
    // 矩形
    private RectF oval;
    // 圆弧的起始角度
    private float startAngle = 120;
    // 圆弧的经过总范围角度角度
    private float sweepAngle = 300;

    private RectF mInnerPadding;
    private int mLinesCount = 100;

    private float[] mTargetAngleRange;
    // 刻度经过角度范围
    private float mCurrentAngle = 300;

    // 绘制文字
    Paint textPaint;

    // 监听角度变化对应的颜色变化
    private OnAngleColorListener onAngleColorListener;

    public DialoScaleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);

        textPaint = new Paint();
        textPaint.setARGB(255, 255, 255, 255);
        textPaint.setTextSize(20);
        textPaint.setAntiAlias(true);

        waterPaint = new Paint();
        waterPaint.setAntiAlias(true);

        mInnerPadding = new RectF();
        mTargetAngleRange = new float[2];

        setLinesCount(60);
        setTargetAngleRange(60, 65);
        setCurrentAngle(130);
        setScaleSweepAngle(180);
        moveWaterLine();
    }

    /**
     * 设置动画效果，开启子线程定时绘制
     *
     * @param trueAngle
     */
    // 前进或者后退的状态，1代表前进，2代表后退。初始为后退状态。
    int state = 2;
    // 每次后退时的值，实现越来越快的效果
    private int[] back = {2, 2, 4, 4, 6, 6, 8, 8, 10};
    // 每次前进时的值，实现越来越慢的效果
    private int[] go = {10, 10, 8, 8, 6, 6, 4, 4, 2};
    // 前进的下标
    private int go_index = 0;
    // 后退的下标
    private int back_index = 0;
    private int score;
    private int color;

    private boolean isRunning;

    public void setLinesCount(int count) {
        mLinesCount = count;
    }

    public void setScaleSweepAngle(float angle) {
        sweepAngle = angle;
        a = sweepAngle / mLinesCount;
        if (mCurrentAngle > sweepAngle) {
            mCurrentAngle = sweepAngle;
        }
        invalidate();
    }

    public void setCurrentAngle(float angle) {
        if (angle <= sweepAngle) {
            mCurrentAngle = angle;
        }
    }

    public void setTargetAngle(float targetAngle) {
        mTargetAngleRange[0] = targetAngle;
        mTargetAngleRange[1] = targetAngle;
    }

    public void setTargetAngleRange(float angle1, float angle2) {
        mTargetAngleRange[0] = angle1;
        mTargetAngleRange[1] = angle2;
    }

    public void change(final float trueAngle) {
        if (isRunning) {
            return;
        }
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                switch (state) {
                    case 1:
                        // 开始增加
                        mCurrentAngle += go[go_index];
                        go_index++;
                        if (go_index == go.length) {// 到最后的元素时，下标一直为最后的
                            go_index--;
                        }
                        if (mCurrentAngle >= trueAngle) {// 如果画过刻度大于等于真实角度
                            // 画过刻度=真实角度
                            mCurrentAngle = trueAngle;
                            // 状态改为2
                            state = 2;
                            isRunning = false;
                            timer.cancel();
                        }
                        break;
                    case 2:
                        isRunning = true;
                        mCurrentAngle -= back[back_index];
                        back_index++;
                        if (back_index == back.length) {
                            back_index--;
                        }

                        if (mCurrentAngle <= 0) {
                            mCurrentAngle = 0;
                            state = 1;
                        }
                        break;
                    default:
                        break;
                }
                // 计算当前比例应该的多少分
                score = (int) (mCurrentAngle / 300 * 100);
                // 计算出当前所占比例，应该增长多少
                up = (int) (mCurrentAngle / 360 * clipRadius * 2);

                postInvalidate();
            }
        }, 500, 30);

    }

    public void moveWaterLine() {
        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                move += 1;
                if (move == 100) {
                    timer.cancel();
                }
                postInvalidate();
            }
        }, 500, 200);
    }

    // 存放第一条水波Y值
    private float[] firstWaterLine;
    // 第二条
    private float[] secondWaterLine;
    // 画水球的画笔
    private Paint waterPaint;
    // 影响三角函数的初相
    private float move;
    // 剪切圆的半径
    private int clipRadius;
    // 水球的增长值
    int up = 0;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO Auto-generated method stub
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        // 通过测量规则获得宽和高
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        if (width > 0 && height > 0) {
            // 取出最小值
            float scaleWidth = width - getPaddingLeft() - getPaddingRight();
            float scaleHeight = height - getPaddingTop() - getPaddingBottom();
            len = (int) scaleWidth;
            if (len / 2 > scaleHeight) {
                len = (int) (scaleHeight * 2);
            }
            radius = len / 2;
            clipRadius = (len / 2) - 45;

            float left = (scaleWidth - len) / 2;
            float top = (scaleHeight - len / 2) / 2;
            mInnerPadding.set(left, top, left, top);
            if (widthMode != MeasureSpec.EXACTLY) {
                mInnerPadding.set(0, top, 0, top);
                setMeasuredDimension(len + getPaddingLeft() + getPaddingRight(), height);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 绘制一个圆弧，如果懂得坐标系的旋转，可以不写。
        // canvas.drawArc(oval, startAngle, sweepAngle, false, paint);
        // 画布，圆心左边，半径，起始角度，经过角度,
        // 说白了就是canvas没有提供画特殊图形的方法，就需要我们自己去实现这种功能了
        // 画刻度线
        drawLine(canvas, getPaddingLeft() + mInnerPadding.left, getPaddingTop() + mInnerPadding.top);
        // 画刻度线内的内容
//        drawText(canvas);

    }

    /**
     * 画水球的功能
     *
     * @param canvas
     */
    private void drawWaterView(Canvas canvas) {
        // y = Asin(wx+b)+h ，这个公式里：w影响周期，A影响振幅，h影响y位置，b为初相；
        // 将周期定为view总宽度
        float mCycleFactorW = (float) (2 * Math.PI / len);

        // 得到第一条波的y值
        for (int i = 0; i < len; i++) {
            firstWaterLine[i] = (float) (10 * Math
                    .sin(mCycleFactorW * i + move) - up);
        }
        // 得到第一条波的y值
        for (int i = 0; i < len; i++) {
            secondWaterLine[i] = (float) (15 * Math.sin(mCycleFactorW * i
                    + move + 10) - up);

        }

        canvas.save();

        // 裁剪成圆形区域
        Path path = new Path();
        waterPaint.setColor(color);
        path.reset();
        canvas.clipPath(path);

        path.addCircle(len / 2, len / 2, clipRadius, Path.Direction.CCW);
        canvas.clipPath(path, android.graphics.Region.Op.REPLACE);
        // 将坐标系移到底部
        canvas.translate(0, len / 2 + clipRadius);

        for (int i = 0; i < len; i++) {
            canvas.drawLine(i, firstWaterLine[i], i, len, waterPaint);
        }
        for (int i = 0; i < len; i++) {
            canvas.drawLine(i, secondWaterLine[i], i, len, waterPaint);
        }
        canvas.restore();
    }

    /**
     * 实现画刻度线内的内容
     *
     * @param canvas
     */
    private void drawText(Canvas canvas) {
        Paint cPaint = new Paint();
        // cPaint.setARGB(50, 236, 241, 243);
        cPaint.setAlpha(50);
        cPaint.setARGB(50, 236, 241, 243);
        // 画圆形背景
        RectF smalloval = new RectF(40, 40, radius * 2 - 40, radius * 2 - 40);
        // 画水波
        drawWaterView(canvas);
        canvas.drawArc(smalloval, 0, 360, true, cPaint);
        // 在小圆圈的外围画一个白色圈
        // canvas.drawArc(smalloval, 0, 360, false, paint);
        // 设置文本对齐方式，居中对齐
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(clipRadius / 2);
        // 画分数
        canvas.drawText("" + score, radius, radius, textPaint);

        textPaint.setTextSize(clipRadius / 6);

        // 画固定值分
        canvas.drawText("分", radius + clipRadius / 2, radius - clipRadius / 4, textPaint);
        textPaint.setTextSize(clipRadius / 6);
        // 画固定值立即优化
        canvas.drawText("点击优化", radius, radius + clipRadius / 2, textPaint);

    }

    float a = sweepAngle / 100;
    private Paint linePaint;

    /**
     * 实现画刻度线的功能
     *
     * @param canvas
     */
    private void drawLine(final Canvas canvas, float startX, float startY) {
        // 保存之前的画布状态
        canvas.save();
        // 移动画布，实际上是改变坐标系的位置
        canvas.translate(radius + startX, radius + startY);
        // 旋转坐标系,需要确定旋转角度
        float beginRotation = (360 - sweepAngle) / 2;
        canvas.rotate(beginRotation);
        // 初始化画笔
        linePaint = new Paint();
        // 设置画笔的宽度（线的粗细）
        linePaint.setStrokeWidth(2);
        // 设置抗锯齿
        linePaint.setAntiAlias(true);
        // 累计叠加的角度
        float drawAngle = 0;

        Paint.FontMetrics fm = textPaint.getFontMetrics();
        float baseline = textPaint.getTextSize() / 2 + (fm.bottom - fm.top) / 2 - fm.bottom;
        int red, green;
        while (drawAngle <= sweepAngle) {

            if (drawAngle <= mCurrentAngle && mCurrentAngle != 0) {
                //当前刻度在目标范围内
                if (drawAngle >= mTargetAngleRange[0] && drawAngle <= mTargetAngleRange[1]) {
                    red = 0;
                    green = 255;
                } else if (drawAngle < mTargetAngleRange[0]) {
                    double p = (mTargetAngleRange[0] - drawAngle) / mTargetAngleRange[0];
                    red = (int) (Math.sqrt(p) * 255);
                    green = 255 - red;
                } else if (drawAngle > mTargetAngleRange[1]) {
                    double p = (drawAngle - mTargetAngleRange[1]) / (180 - mTargetAngleRange[1]);
                    red = (int) (Math.sqrt(p) * 255);
                    green = 255 - red;
                } else {
                    red = 255;
                    green = 255;
                }
                linePaint.setARGB(255, red, green, 0);
            } else {
                linePaint.setARGB(60, 0, 0, 0);
            }

            if (Math.abs(drawAngle - mCurrentAngle) < a) {
                //画出当前位置刻度线更长一点点
                canvas.drawLine(0, radius + 10, 0, radius - 20, linePaint);
            } else {
                //普通刻度线
                canvas.drawLine(0, radius, 0, radius - 20, linePaint);
            }

            if (drawAngle % 30 == 0) {
                textPaint.setTextAlign(Paint.Align.CENTER);
                textPaint.setColor(Color.BLACK);
                int save = canvas.save();
                canvas.rotate(180, 0, radius - 30);
                canvas.drawText(String.valueOf((int) drawAngle), 0, radius - 30 + baseline, textPaint);
                canvas.restoreToCount(save);
            }

//            if (c <= mCurrentAngle && mCurrentAngle != 0) {// 如果累计画过的角度，小于当前有效刻度
//                // 计算累计划过的刻度百分比（画过的刻度比上中共进过的刻度）
//                double p = c / (double) sweepAngle;
//
//                int red = 255 - (int) (p * 255);
//                int green = (int) (p * 255);
//                color = linePaint.getColor();
//                if (onAngleColorListener != null) {
//                    onAngleColorListener.onAngleColorListener(red, green);
//                }
//                linePaint.setARGB(255, red, green, 50);
//                if (Math.abs(c - mCurrentAngle) < a) {
//                    canvas.drawLine(0, radius + 10, 0, radius - 20, linePaint);
//                } else {
//                    canvas.drawLine(0, radius, 0, radius - 20, linePaint);
//                }
//                Paint.FontMetrics fm = textPaint.getFontMetrics();
//                float baseline = textPaint.getTextSize() / 2 + (fm.bottom - fm.top) / 2 - fm.bottom;
//                if (c % 30 == 0) {
//                    textPaint.setTextAlign(Paint.Align.CENTER);
//                    textPaint.setColor(Color.BLACK);
//                    int save = canvas.save();
//                    canvas.rotate(180, 0, radius - 30);
//                    canvas.drawText(String.valueOf((int) c), 0, radius - 30 + baseline, textPaint);
//                    canvas.restoreToCount(save);
////                    if ((c + beginRotation) >= 135 && ((c + beginRotation) <= 225)) {
////                        int save = canvas.save();
////                        canvas.rotate(180, 0, radius - 30);
////                        canvas.drawText(String.valueOf((int) c), 0, radius - 30 + baseline, textPaint);
////                        canvas.restoreToCount(save);
////                    } else {
////                        canvas.drawText(String.valueOf((int) c), 0, radius - 30, textPaint);
////                    }
//                }
//                // 画过的角度进行叠加
//                c += a;
//            } else {
//                linePaint.setColor(Color.WHITE);
//                canvas.drawLine(0, radius, 0, radius - 20, linePaint);
//            }
            canvas.rotate(a);
            drawAngle += a;
        }
        // 恢复画布状态。
        canvas.restore();
    }

    public void setOnAngleColorListener(
            OnAngleColorListener onAngleColorListener) {
        this.onAngleColorListener = onAngleColorListener;
    }

    /**
     * 监听角度和颜色变化的接口
     *
     * @author Administrator
     */
    public interface OnAngleColorListener {
        void onAngleColorListener(int red, int green);
    }
}
