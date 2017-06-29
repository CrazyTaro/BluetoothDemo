package com.bluetooth.connection.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bluetooth.connection.BlueView;
import com.bluetooth.connection.BluetoothHelper;
import com.bluetooth.connection.GroupData;
import com.bluetooth.connection.LineData;
import com.bluetooth.connection.R;
import com.bluetooth.connection.main.view.DialoScaleView;

/**
 * Created by taro on 2017/6/13.
 */

public class AngleFragment extends BaseFragment {
    DialoScaleView mDsvAngle;
    BlueView mBvAngle;
    TextView mTvAngle;
    String mFormatStr = "角度一:%03.1f  角度二:%03.1f  夹角:%03.1f";

    UIRunnable mUiRunnable;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_angle, container, false);
        mDsvAngle = (DialoScaleView) contentView.findViewById(R.id.dsv_angle_include_angle);
        mTvAngle = (TextView) contentView.findViewById(R.id.tv_angle_value);
        mBvAngle = (BlueView) contentView.findViewById(R.id.bv_angle);

        mBvAngle.setDataTypeMask(BluetoothHelper.TYPE_DATA_INCLUDED_ANGLE);
        mBvAngle.addHideMask(BlueView.MASK_HIDE_Y);
        mBvAngle.addHideMask(BlueView.MASK_HIDE_Z);

        mUiRunnable = new UIRunnable();
        return contentView;
    }

    @Override
    public void notifyDataSetChanged(String addr, @NonNull GroupData datas, @NonNull LineData item) {
        if (mBvAngle != null) {
            mBvAngle.updateViewData(datas);

            if (getUserVisibleHint()) {
                mUiRunnable.setLineData(item);
                getActivity().runOnUiThread(mUiRunnable);
                mBvAngle.postInvalidate();
            }
        }
    }

    private void setAngle(LineData line) {
        if (line != null) {
            float include = line.mData[0][0];
            float one = line.mData[0][1];
            float second = line.mData[0][2];
            mDsvAngle.setCurrentAngle(include);
            mDsvAngle.invalidate();
            mTvAngle.setText(String.format(mFormatStr, one, second, include));
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
                setAngle(mLine);
            }
        }
    }
}
