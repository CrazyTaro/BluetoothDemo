package com.bluetooth.connection.main;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.bluetooth.connection.main.view.BlueView;
import com.bluetooth.connection.R;
import com.taro.bleservice.core.BluetoothHelper;
import com.taro.bleservice.entity.GroupData;
import com.taro.bleservice.entity.LineData;

/**
 * Created by taro on 2017/6/13.
 */

public class DeviceFragment extends BaseFragment implements CompoundButton.OnCheckedChangeListener, RadioGroup.OnCheckedChangeListener {
    BlueView mBlueView;
    TextView mTvTag;
    RadioGroup mRgType;
    CheckBox mCbShowX;
    CheckBox mCbShowY;
    CheckBox mCbShowZ;
    UIRunnable mUiRunnable;

    String[] mFormats;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_device, container, false);
        mBlueView = (BlueView) contentView.findViewById(R.id.bv_device_chart);
        mTvTag = (TextView) contentView.findViewById(R.id.tv_device_tag);
        mRgType = (RadioGroup) contentView.findViewById(R.id.rg_device_choice);
        mCbShowX = (CheckBox) contentView.findViewById(R.id.cbtn_device_x);
        mCbShowY = (CheckBox) contentView.findViewById(R.id.cbtn_device_y);
        mCbShowZ = (CheckBox) contentView.findViewById(R.id.cbtn_device_z);
        mCbShowX.setOnCheckedChangeListener(this);
        mCbShowY.setOnCheckedChangeListener(this);
        mCbShowZ.setOnCheckedChangeListener(this);
        mRgType.setOnCheckedChangeListener(this);

        mFormats = new String[3];
        mFormats[0] = "X %.2f";
        mFormats[1] = "Y %.2f";
        mFormats[2] = "Z %.2f";

        mUiRunnable = new UIRunnable();
        return contentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        GroupData data = mBlueView.getViewData();
        if (data != null) {
            int groupType = LineData.getGroupType(mBlueView.getDataTypeMask());
            LineData last = data.getLastLineData(groupType);
            if (last != null) {
                setAxisText(last);
            }
            mBlueView.invalidate();
        }

        mTvTag.setText(String.format("设备标识:%s", getDeviceTag()));
    }

    @Override
    public void setDeviceTag(String tag) {
        if (!isDeviceTagChanged(tag)) {
            super.setDeviceTag(tag);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTvTag.setText(String.format("设备标识:%s", getDeviceTag()));
                }
            });
        }
    }

    @Override
    public void notifyDataSetChanged(String addr, @NonNull GroupData datas, @NonNull LineData item) {
        if (mBlueView != null) {
            mBlueView.updateViewData(datas);

            if (getUserVisibleHint() && item.isSupportTypeData(mBlueView.getDataTypeMask())) {
                mUiRunnable.setLineData(item);
                getActivity().runOnUiThread(mUiRunnable);
                mBlueView.postInvalidate();
            }
        }
    }

    private void setAxisText(@NonNull LineData item) {
        float[] axis = item.getAxisValue(mBlueView.getDataTypeMask());
        if (axis != null) {
            mCbShowX.setText(String.format(mFormats[0], axis[0]));
            mCbShowY.setText(String.format(mFormats[1], axis[1]));
            mCbShowZ.setText(String.format(mFormats[2], axis[2]));
        } else {
            mCbShowX.setText(" - ");
            mCbShowY.setText(" - ");
            mCbShowZ.setText(" - ");
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.cbtn_device_x:
                changedAxisShowStatus(isChecked, BlueView.MASK_HIDE_X);
                break;
            case R.id.cbtn_device_y:
                changedAxisShowStatus(isChecked, BlueView.MASK_HIDE_Y);
                break;
            case R.id.cbtn_device_z:
                changedAxisShowStatus(isChecked, BlueView.MASK_HIDE_Z);
                break;
        }
    }

    public BlueView getBlueDataView() {
        return mBlueView;
    }

    private void changedAxisShowStatus(boolean isShow, int mask) {
        if (isShow) {
            mBlueView.removeHideMask(mask);
        } else {
            mBlueView.addHideMask(mask);
        }
        mBlueView.invalidate();
    }

    private void changedDataType(int typeData) {
        mBlueView.setDataTypeMask(typeData);
        mBlueView.invalidate();

        GroupData group = mBlueView.getViewData();
        int typeGroup = LineData.getGroupType(typeData);
        if (group != null) {
            LineData data = group.getLastLineData(typeGroup);
            setAxisText(data);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
        switch (checkedId) {
            //加速度
            case R.id.rbtn_device_acc:
                changedDataType(BluetoothHelper.TYPE_DATA_ACC);
                break;
            //磁场
            case R.id.rbtn_device_mag:
                changedDataType(BluetoothHelper.TYPE_DATA_MAG);
                break;
            //角速度
            case R.id.rbtn_device_gyro:
                changedDataType(BluetoothHelper.TYPE_DATA_GYR);
                break;
            //欧拉角
            case R.id.rbtn_device_eul:
                changedDataType(BluetoothHelper.TYPE_DATA_EUL);
                break;
            //
            case R.id.rbtn_device_qua:
                changedDataType(BluetoothHelper.TYPE_DATA_QUA);
                break;
            //线性加速度
            case R.id.rbtn_device_lacc:
                changedDataType(BluetoothHelper.TYPE_DATA_LIA);
                break;
            //重力角速度
            case R.id.rbtn_device_grv:
                changedDataType(BluetoothHelper.TYPE_DATA_GRV);
                break;
        }
    }

    private class UIRunnable implements Runnable {
        LineData mLine;

        public void setLineData(@NonNull LineData data) {
            mLine = data;
        }

        @Override
        public void run() {
            if (mLine != null) {
                setAxisText(mLine);
            }
        }
    }
}
