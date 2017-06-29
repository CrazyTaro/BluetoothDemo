package com.bluetooth.connection;


import android.support.annotation.NonNull;

/**
 * Created by taro on 2017/6/19.
 */

public class LineData {
    public float[][] mData;
    public int mExtra;

    private long mTime;
    private int mGroupType;

    /**
     * 根据数据类型获取所在的分组类型
     *
     * @param typeData
     * @return
     */
    public static int getGroupType(int typeData) {
        switch (typeData) {
            case BluetoothHelper.TYPE_DATA_ACC:
            case BluetoothHelper.TYPE_DATA_MAG:
                return BluetoothHelper.TYPE_GROUP_0C;
            case BluetoothHelper.TYPE_DATA_EUL:
            case BluetoothHelper.TYPE_DATA_GYR:
                //电量
            case BluetoothHelper.TYPE_EXTRA_BATTERY:
                return BluetoothHelper.TYPE_GROUP_0D;
            case BluetoothHelper.TYPE_DATA_QUA:
                return BluetoothHelper.TYPE_GROUP_0E;
            case BluetoothHelper.TYPE_DATA_GRV:
            case BluetoothHelper.TYPE_DATA_LIA:
                //温度
            case BluetoothHelper.TYPE_EXTRA_TEMPERATURE:
                return BluetoothHelper.TYPE_GROUP_0F;
            //夹角
            case BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE:
                return BluetoothHelper.TYPE_GROUP_ANGLE;
            default:
                return -1;

        }
    }

    /**
     * 创建分组数据
     *
     * @return
     */
    public LineData build() {
        mTime = System.currentTimeMillis();
        return this;
    }

    /**
     * 获取分组数据创建的时间
     *
     * @return
     */
    public long getTime() {
        return mTime;
    }

    /**
     * 获取电量或者温度
     *
     * @param typeExtra
     * @return
     */
    public int getExtra(int typeExtra) {
        if (mGroupType == BluetoothHelper.TYPE_GROUP_0D
                && typeExtra == BluetoothHelper.TYPE_EXTRA_BATTERY) {
            return mExtra;
        } else if (mGroupType == BluetoothHelper.TYPE_GROUP_0F
                && typeExtra == BluetoothHelper.TYPE_EXTRA_TEMPERATURE) {
            return mExtra;
        } else {
            return -1;
        }
    }

    /**
     * 获取给定数据类型的所有坐标值,若当前分组数据不包含该数据类型,返回null
     *
     * @param typeData 数据类型
     * @return
     */
    public float[] getAxisValue(int typeData) {
        if (isSupportTypeData(typeData)) {
            int dataIndex = -1;
            switch (typeData) {
                case BluetoothHelper.TYPE_DATA_ACC:
                case BluetoothHelper.TYPE_DATA_GYR:
                case BluetoothHelper.TYPE_DATA_QUA:
                case BluetoothHelper.TYPE_DATA_LIA:
                    //夹角,x,y,z的位置分别存放 夹角/角度1/角度2
                case BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE:
                    dataIndex = 0;
                    break;
                case BluetoothHelper.TYPE_DATA_MAG:
                case BluetoothHelper.TYPE_DATA_EUL:
                case BluetoothHelper.TYPE_DATA_GRV:
                    dataIndex = 1;
                    break;
            }

            if (dataIndex >= 0 && dataIndex <= 1) {
                return mData[dataIndex];
            }
        }
        return null;
    }

    /**
     * 获取给定数据类型的坐标值,若当前分组数据不包含该数据类型,返回-1
     *
     * @param typeData 数据类型
     * @param typeAxis 坐标类型{@link BluetoothHelper#TYPE_AXIS_X}
     * @return
     */
    public float getValue(int typeData, int typeAxis) {
        float[] axis = getAxisValue(typeData);
        if (axis != null && typeAxis < 4 && typeAxis >= 0) {
            return axis[typeAxis];
        } else {
            return -1;
        }
    }

    /**
     * 判断是否支持给定的数据类型,在不同分组中的数据保存不同类型的数据,只有在所属分组中的数据类型才支持
     *
     * @param typeData 数据类型{@link BluetoothHelper#TYPE_DATA_ACC}等
     * @return
     */
    public boolean isSupportTypeData(int typeData) {
        switch (typeData) {
            case BluetoothHelper.TYPE_DATA_ACC:
            case BluetoothHelper.TYPE_DATA_MAG:
                return mGroupType == BluetoothHelper.TYPE_GROUP_0C;
            case BluetoothHelper.TYPE_DATA_EUL:
            case BluetoothHelper.TYPE_DATA_GYR:
            case BluetoothHelper.TYPE_EXTRA_BATTERY:
                return mGroupType == BluetoothHelper.TYPE_GROUP_0D;
            case BluetoothHelper.TYPE_DATA_QUA:
                return mGroupType == BluetoothHelper.TYPE_GROUP_0E;
            case BluetoothHelper.TYPE_DATA_GRV:
            case BluetoothHelper.TYPE_DATA_LIA:
            case BluetoothHelper.TYPE_EXTRA_TEMPERATURE:
                return mGroupType == BluetoothHelper.TYPE_GROUP_0F;
            case BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE:
                return mGroupType == BluetoothHelper.TYPE_GROUP_ANGLE;
            default:
                return false;

        }
    }

    /**
     * 创建分组数据,分组类型为四种中的一种,{@link BluetoothHelper#TYPE_GROUP_0C}等
     *
     * @param groupType
     */
    public LineData(int groupType) {
        switch (groupType) {
            case BluetoothHelper.TYPE_GROUP_0C:
                mData = new float[2][3];
                break;
            case BluetoothHelper.TYPE_GROUP_0D:
                mData = new float[2][3];
                break;
            case BluetoothHelper.TYPE_GROUP_0E:
                //多了个W轴
                mData = new float[1][4];
                break;
            case BluetoothHelper.TYPE_GROUP_0F:
                mData = new float[2][3];
                break;
            case BluetoothHelper.TYPE_GROUP_ANGLE:
                mData = new float[1][3];
                break;
        }
        mGroupType = groupType;
    }

    /**
     * 获取当前数据中每组数据的最大值(取正数)
     *
     * @param out
     * @return
     */
    public boolean getMaxValue(@NonNull float[] out) {
        //夹角
        if (mGroupType == BluetoothHelper.TYPE_GROUP_ANGLE) {
            out[0] = mData[0][0];
            return true;
        }
        //非夹角
        if (mData == null || out.length < mData.length) {
            return false;
        } else {
            for (int i = 0; i < mData.length; i++) {
                out[i] = computeMaxValue(mData[i]);
            }
            return true;
        }
    }

    /**
     * 计算一组数据中的最大值,取绝对值比较
     *
     * @param values
     * @return
     */
    private float computeMaxValue(float[] values) {
        float i = Float.MIN_VALUE;
        for (float value : values) {
            if (Math.abs(value) > i) {
                i = Math.abs(value);
            }
        }
        return i;
    }

    /**
     * 获取当前的分组类型
     *
     * @return
     */
    public int getGroupType() {
        return mGroupType;
    }

    @Override
    public String toString() {
        String format = null;
        String result = "";
        switch (mGroupType) {
            case BluetoothHelper.TYPE_GROUP_0C:
                format = "ACC[%.2f,%.2f,%.2f]    MAG[%.2f,%.2f,%.2f]";
                result = String.format(format, mData[0][0], mData[0][1], mData[0][2], mData[1][0], mData[1][1], mData[1][2]);
                break;
            case BluetoothHelper.TYPE_GROUP_0D:
                format = "GYR[%.2f,%.2f,%.2f]    EUL[%.2f,%.2f,%.2f]    BAT[%d]";
                result = String.format(format, mData[0][0], mData[0][1], mData[0][2], mData[1][0], mData[1][1], mData[1][2], mExtra);
                break;
            case BluetoothHelper.TYPE_GROUP_0E:
                format = "QUA[%.2f,%.2f,%.2f]";
                result = String.format(format, mData[0][0], mData[0][1], mData[0][2]);
                break;
            case BluetoothHelper.TYPE_GROUP_0F:
                format = "LIA[%.2f,%.2f,%.2f]    GRV[%.2f,%.2f,%.2f]    TEP[%d]";
                result = String.format(format, mData[0][0], mData[0][1], mData[0][2], mData[1][0], mData[1][1], mData[1][2], mExtra);
                break;
        }
        return result;
    }
}
