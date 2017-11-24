package com.bluetooth.connection.main;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;

import com.taro.bleservice.entity.GroupData;
import com.taro.bleservice.entity.LineData;


/**
 * Created by taro on 2017/6/19.
 */

public abstract class BaseFragment extends Fragment {
    protected String mTag;

    public abstract void notifyDataSetChanged(String addr, @NonNull GroupData datas, @NonNull LineData item);

    public String getDeviceTag() {
        return mTag != null ? mTag : " - ";
    }

    public void setDeviceTag(String tag) {
        mTag = tag;
    }

    public boolean isDeviceTagChanged(String compareTag) {
        if (mTag != null) {
            return mTag.equals(compareTag);
        } else {
            return compareTag == null;
        }
    }
}
