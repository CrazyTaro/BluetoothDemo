package com.bluetooth.connection;

import android.support.v4.util.SparseArrayCompat;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by taro on 2017/6/19.
 */

public class GroupData {
    private float[] mCacheMaxs;
    private SparseArrayCompat<List<LineData>> mGroupDatas;
    private SparseArrayCompat<Float> mMaxValues;

    public GroupData() {
        mGroupDatas = new SparseArrayCompat<>(4);
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0C, new LinkedList<LineData>());
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0D, new LinkedList<LineData>());
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0E, new LinkedList<LineData>());
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0F, new LinkedList<LineData>());
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_ANGLE, new LinkedList<LineData>());

        mMaxValues = new SparseArrayCompat<>(8);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_ACC, 0f);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_MAG, 0f);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_GYR, 0f);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_EUL, 0f);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_QUA, 0f);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_LIA, 0f);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_GRV, 0f);
        mMaxValues.put(BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE, 0f);

        mCacheMaxs = new float[2];
    }

    public float getMaxValue(int typeData) {
//        Integer result = BluetoothHelper.LIMIT.get(typeData);
//        if (result != null) {
//            return result;
//        } else {
//            return 0;
//        }
        Float value = mMaxValues.get(typeData);
        if (value != null) {
            return value;
        } else {
            return 0;
        }
    }

    public void addNewData(int typeGroup, LineData data) {
        if (data != null) {
            List<LineData> items = mGroupDatas.get(typeGroup);
            if (items != null) {
                data.getMaxValue(mCacheMaxs);
                float oldMax;
                switch (typeGroup) {
                    case BluetoothHelper.TYPE_GROUP_0C:
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_ACC);
                        if (oldMax < mCacheMaxs[0]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_ACC, mCacheMaxs[0]);
                        }
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_MAG);
                        if (oldMax < mCacheMaxs[1]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_MAG, mCacheMaxs[1]);
                        }
                        break;
                    case BluetoothHelper.TYPE_GROUP_0D:
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_EUL);
                        if (oldMax < mCacheMaxs[0]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_EUL, mCacheMaxs[0]);
                        }
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_GYR);
                        if (oldMax < mCacheMaxs[1]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_GYR, mCacheMaxs[1]);
                        }
                        break;
                    case BluetoothHelper.TYPE_GROUP_0E:
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_QUA);
                        if (oldMax < mCacheMaxs[0]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_QUA, mCacheMaxs[0]);
                        }
                        break;
                    case BluetoothHelper.TYPE_GROUP_0F:
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_GRV);
                        if (oldMax < mCacheMaxs[0]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_GRV, mCacheMaxs[0]);
                        }
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_LIA);
                        if (oldMax < mCacheMaxs[1]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_LIA, mCacheMaxs[1]);
                        }
                        break;
                    case BluetoothHelper.TYPE_GROUP_ANGLE:
                        oldMax = mMaxValues.get(BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE);
                        if (oldMax < mCacheMaxs[0]) {
                            mMaxValues.put(BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE, mCacheMaxs[0]);
                        }
                        break;
                }
                items.add(0, data);
            }

            if (mGroupDatas.size() > 1000) {
                mGroupDatas.removeAtRange(500, mGroupDatas.size() - 500);
            }
        }
    }

    public LineData getLastLineData(int typeGroup) {
        List<LineData> items = mGroupDatas.get(typeGroup);
        if (items != null && items.size() > 0) {
            return items.get(0);
        }
        return null;
    }

    public List<LineData> getLineDatas(int typeGroup) {
        return mGroupDatas.get(typeGroup);
    }

    public int getIncludedAngle() {
        LineData angleLine = getLastLineData(BluetoothHelper.TYPE_GROUP_ANGLE);
        if (angleLine != null) {
            return angleLine.getExtra(BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE);
        } else {
            return -1;
        }
    }

    public int getBattery() {
        LineData batLine = getLastLineData(BluetoothHelper.TYPE_GROUP_0D);
        if (batLine != null) {
            return batLine.getExtra(BluetoothHelper.TYPE_EXTRA_BATTERY);
        } else {
            return -1;
        }
    }

    public int getTemperature() {
        LineData tepLine = getLastLineData(BluetoothHelper.TYPE_GROUP_0F);
        if (tepLine != null) {
            return tepLine.getExtra(BluetoothHelper.TYPE_EXTRA_TEMPERATURE);
        } else {
            return -1;
        }
    }

}
