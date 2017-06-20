package com.bluetooth.connection.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bluetooth.GroupData;
import com.bluetooth.connection.LineData;
import com.bluetooth.connection.R;

/**
 * Created by taro on 2017/6/13.
 */

public class AngleFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_angle, container, false);
        return contentView;
    }

    @Override
    public void notifyDataSetChanged(String addr, @NonNull GroupData datas, @NonNull LineData item) {

    }
}
