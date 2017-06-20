package com.bluetooth;

import android.support.v4.util.SparseArrayCompat;

import com.bluetooth.connection.BluetoothHelper;
import com.bluetooth.connection.LineData;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by taro on 2017/6/19.
 */

public class GroupData {
    private SparseArrayCompat<List<LineData>> mGroupDatas;

    public GroupData() {
        mGroupDatas = new SparseArrayCompat<>(4);
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0C, new LinkedList<LineData>());
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0D, new LinkedList<LineData>());
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0E, new LinkedList<LineData>());
        mGroupDatas.put(BluetoothHelper.TYPE_GROUP_0F, new LinkedList<LineData>());
    }

    public void addNewData(int typeGroup, LineData data) {
        if (data != null) {
            List<LineData> items = mGroupDatas.get(typeGroup);
            if (items != null) {
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
