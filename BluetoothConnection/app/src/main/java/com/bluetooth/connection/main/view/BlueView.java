package com.bluetooth.connection.main.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.bluetooth.connection.R;
import com.taro.bleservice.core.BluetoothHelper;
import com.taro.bleservice.entity.GroupData;
import com.taro.bleservice.entity.LineData;

import java.util.List;


/**
 * Created by taro on 2017/5/15.
 */

public class BlueView extends View {
    public static final int MASK_HIDE_X = 1;
    public static final int MASK_HIDE_Y = 2;
    public static final int MASK_HIDE_Z = 4;

    private Paint mPaint;
    private GroupData mDatas;
    private Path[] mPaths;
    private long mIntervalTimeStamp;
    private long mFirstTimeStamp;
    private int mDegreeHeight = 1;
    private int mTimeUnitWidth = 10;
    private boolean mIsInit = false;

    private int mTypeMask = BluetoothHelper.TYPE_DATA_ACC;
    private int mHideMask = 0;
    private OnDegreeValueDescDraw mDescDrawListener;

    public BlueView(Context context) {
        super(context);
        init(context, null);
    }

    public BlueView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public BlueView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        if (mIsInit) {
            Log.e("init", "already init");
            return;
        }

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(2);
        mPaint.setTextSize(15);

        mPaths = new Path[3];
        for (int i = 0; i < mPaths.length; i++) {
            mPaths[i] = new Path();
        }
        if (!mIsInit && attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.BlueView);
            int height = ta.getInt(R.styleable.BlueView_degreeHeight, -1);
            int width = ta.getIndex(R.styleable.BlueView_timeUnitWidth - 1);
            boolean isShowX = ta.getBoolean(R.styleable.BlueView_showX, true);
            boolean isShowY = ta.getBoolean(R.styleable.BlueView_showY, true);
            boolean isShowZ = ta.getBoolean(R.styleable.BlueView_showZ, true);

            setDegreeHeight(height);
            setTimeUnitWidth(width);
            if (isShowX) {
                removeHideMask(MASK_HIDE_X);
            } else {
                addHideMask(MASK_HIDE_X);
            }

            if (isShowY) {
                removeHideMask(MASK_HIDE_Y);
            } else {
                addHideMask(MASK_HIDE_Y);
            }

            if (isShowZ) {
                removeHideMask(MASK_HIDE_Z);
            } else {
                addHideMask(MASK_HIDE_Z);
            }

            ta.recycle();
            mIsInit = true;
        }
    }

    public void setDegreeHeight(int heightPx) {
        if (heightPx > 0 && heightPx < 50) {
            mDegreeHeight = heightPx;
        }
    }

    public void setTimeUnitWidth(int widthPx) {
        if (widthPx > 5 && widthPx < 800) {
            mTimeUnitWidth = widthPx;
        }
    }

    public void setHideMask(int mask) {
        mHideMask = mask;
    }

    public void addHideMask(int mask) {
        mHideMask |= mask;
    }

    public void removeHideMask(int mask) {
        mHideMask &= ~mask;
    }

    public void setOnDegreeValueDrawListener(OnDegreeValueDescDraw listener) {
        mDescDrawListener = listener;
    }

    public void setDataTypeMask(int typeMask) {
        mTypeMask = typeMask;
    }

    public int getDataTypeMask() {
        return mTypeMask;
    }

    public GroupData getViewData() {
        return mDatas;
    }

    public void updateViewData(GroupData data) {
        mDatas = data;
    }

    private float getDrawValue(float value, float total, float drawHeight, float startY) {
//        return (int) ((180 - value) / 360 * drawHeight + 20);
        return (1 - (value * 2 / total)) * drawHeight / 2 + startY;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
//        int height = 20 + 20 + 360 * mDegreeHeight;
//        setMeasuredDimension(width, height);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //宽/高
        int screenWidth = getWidth();
        int width = getWidth();
        int height = getHeight() - 40;

        if (height < 0 || width < 0) {
            return;
        }

        mPaint.setStrokeWidth(1);
        mPaint.setColor(Color.BLACK);

        float total = 180 * 2, result = 0;
        float lineHeight = height / 20f;
        //中线
        float begin = height / 2f;
        float drawX, drawY, startX, startY;
        String desc;
        startX = 0;
        startY = 20;

        if (mDatas != null) {
            total = mDatas.getMaxValue(mTypeMask) * 2;
            if (total <= 0) {
                total = 180 * 2;
            }
        }

        mPaint.setAlpha(102);
        mPaint.setTextSize(lineHeight);
        for (int i = 0; i < 10; i++) {
            if ((i & 1) == 0) {
                //绘制文本
                mPaint.setAlpha(255);
                //上半部分的文本
                drawY = begin - i * lineHeight;
                //结果应该为正数
                result = Math.abs(drawY / height * total - total / 2);
                if (mDescDrawListener != null) {
                    desc = mDescDrawListener.getDescValue(result);
                } else {
                    desc = String.format("%.1f", result);
                }
                if (desc != null) {
                    canvas.drawText(desc, lineHeight, drawY + startY, mPaint);
                }

                //下半部分的文本
                drawY = begin + i * lineHeight + startY;
                //结果应该为负数
                result = total / 2 - drawY / height * total;
                if (mDescDrawListener != null) {
                    desc = mDescDrawListener.getDescValue(result);
                } else {
                    desc = String.format("%.1f", result);
                }
                if (desc != null) {
                    canvas.drawText(desc, lineHeight, drawY + startY, mPaint);
                }
            } else {
                mPaint.setAlpha(102);
            }

            //上半部分的线
            drawY = begin - i * lineHeight;
            canvas.drawLine(0, drawY + startY, width, drawY + startY, mPaint);
            //下半部分的线
            drawY = begin + i * lineHeight;
            canvas.drawLine(0, drawY + startY, width, drawY + startY, mPaint);
        }

//        //计算绘制区域的高度,除去标题底栏文本等
//        int canvasHeight = 360 * mDegreeHeight + 20;
//        //计算每行的高度
//        int lineHeight = 12 * mDegreeHeight;
//        //绘制水平分隔线
//        for (int i = 20; i <= canvasHeight; i += lineHeight) {
//            //360度分为5份进行绘制,每72度一个大格
//            if ((i - 20) % (lineHeight * 6) == 0) {
//                mPaint.setTextSize(15 + mDegreeHeight);
//                mPaint.setAlpha(255);
//                //绘制纵坐标文本
//                if (mDescDrawListener != null) {
//                    //文本生成回调,0-360的距离
//                    canvas.drawText(String.valueOf(mDescDrawListener.getDescValue((i - 20) / mDegreeHeight)), 0, i + 20, mPaint);
//                } else {
//                    //默认-180到180
//                    canvas.drawText(String.valueOf(180 - ((i - 20) / mDegreeHeight)), 0, i + 20, mPaint);
//                }
//            } else {
//                mPaint.setAlpha(102);
//            }
//            canvas.drawLine(0, i, width, i, mPaint);
//        }

        //绘制线条标识
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAlpha(255);
        //绘制数据线
        if (mDatas != null) {
            List<LineData> data = null;
            int groupType = LineData.getGroupType(mTypeMask);
            data = mDatas.getLineDatas(groupType);

            if (data != null && data.size() > 0) {
                int i = 1;
                float pos = 0;
                long lastTime = 0;
                width -= mTimeUnitWidth;
                mIntervalTimeStamp = 0;
                mPaths[0].reset();
                mPaths[1].reset();
                mPaths[2].reset();

                //设置第一个数据
                LineData value = null;
                value = data.get(0);
                float x = value.getValue(mTypeMask, BluetoothHelper.TYPE_AXIS_X);
                float y = value.getValue(mTypeMask, BluetoothHelper.TYPE_AXIS_Y);
                float z = value.getValue(mTypeMask, BluetoothHelper.TYPE_AXIS_Z);

                mFirstTimeStamp = value.getTime();
                pos = getDrawValue(x, total, height, startY);
                mPaths[0].moveTo(width, pos);
                pos = getDrawValue(y, total, height, startY);
                mPaths[1].moveTo(width, pos);
                pos = getDrawValue(z, total, height, startY);
                mPaths[2].moveTo(width, pos);

                //数据大于1时进行数据加载
                if (data.size() > 1) {
                    //当数据存在且绘制位置不超过界面时,进行绘制
                    while (width > 0 && i < data.size()) {
                        value = data.get(i);
                        if (lastTime == 0) {
                            lastTime = value.getTime();
                            mIntervalTimeStamp = value.getTime();
                        }
                        //根据时间单位计算点与点的距离
                        width -= Math.abs((value.getTime() - lastTime)) / mTimeUnitWidth;
                        lastTime = value.getTime();

                        x = value.getValue(mTypeMask, BluetoothHelper.TYPE_AXIS_X);
                        y = value.getValue(mTypeMask, BluetoothHelper.TYPE_AXIS_Y);
                        z = value.getValue(mTypeMask, BluetoothHelper.TYPE_AXIS_Z);
                        //创建数据源并连接
                        pos = getDrawValue(x, total, height, startY);
                        mPaths[0].lineTo(width, pos);
                        pos = getDrawValue(y, total, height, startY);
                        mPaths[1].lineTo(width, pos);
                        pos = getDrawValue(z, total, height, startY);
                        mPaths[2].lineTo(width, pos);

                        long interval = Math.abs(value.getTime() - mIntervalTimeStamp);
                        //每超过1秒则更新一次数据时间
                        if (interval > 1000) {
                            mIntervalTimeStamp = value.getTime();
                            mPaint.setColor(Color.BLACK);
                            mPaint.setStrokeWidth(1);
                            canvas.drawLine(width, height + startY, width, height + startX + 5, mPaint);
                            canvas.drawText(String.format("%1$.02f", Math.abs((value.getTime() - mFirstTimeStamp)) / 1000f), width, getHeight(), mPaint);
                        }

                        i++;
                    }
                }

                mPaint.setStrokeWidth(3);
                //判断是否需要绘制X轴数据
                if ((mHideMask & MASK_HIDE_X) == 0) {
                    mPaint.setColor(Color.RED);
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(mPaths[0], mPaint);
                }
                //判断是否需要绘制Y轴数据
                if ((mHideMask & MASK_HIDE_Y) == 0) {
                    mPaint.setColor(Color.GREEN);
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(mPaths[1], mPaint);
                }
                //判断是否需要绘制Z轴数据
                if ((mHideMask & MASK_HIDE_Z) == 0) {
                    mPaint.setColor(Color.BLUE);
                    mPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(mPaths[2], mPaint);
                }
            }
        }


        if (mTypeMask != BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE) {
            //非夹角时绘制标线
            float lineStartX = screenWidth - 150;
            float lineStartY = 50;
            float lineWidth = 5;
            //判断是否需要绘制X轴数据
            if ((mHideMask & MASK_HIDE_X) == 0) {
                drawLineStatus(canvas, lineStartX, lineStartY, Color.RED, lineWidth, "X");
                lineStartY += 50;
            }
            //判断是否需要绘制Y轴数据
            if ((mHideMask & MASK_HIDE_Y) == 0) {
                drawLineStatus(canvas, lineStartX, lineStartY, Color.GREEN, lineWidth, "Y");
                lineStartY += 50;
            }
            //判断是否需要绘制Z轴数据
            if ((mHideMask & MASK_HIDE_Z) == 0) {
                drawLineStatus(canvas, lineStartX, lineStartY, Color.BLUE, lineWidth, "Z");
            }
        }
    }

    private void drawLineStatus(Canvas canvas, float startX, float startY, int lineColor, float lineWidth, @NonNull String lineText) {
        mPaint.setTextSize(50);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setColor(Color.BLACK);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(lineText, startX, startY, mPaint);

        startY -= 15;
        mPaint.setStrokeWidth(lineWidth);
        mPaint.setColor(lineColor);
        canvas.drawLine(startX + 25, startY, startX + 125, startY, mPaint);
    }

    public interface OnDegreeValueDescDraw {
        public String getDescValue(float degreeFromFirst);
    }
}
