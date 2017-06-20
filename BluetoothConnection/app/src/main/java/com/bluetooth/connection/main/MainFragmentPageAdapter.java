package com.bluetooth.connection.main;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

/**
 * Created by taro on 2017/6/13.
 */

public class MainFragmentPageAdapter extends FragmentPagerAdapter {
    BaseFragment[] mFragments;

    public MainFragmentPageAdapter(FragmentManager fm) {
        super(fm);
        mFragments = new BaseFragment[4];
        mFragments[0] = new SettingFragment();
        mFragments[1] = new DeviceFragment();
        mFragments[2] = new DeviceFragment();
        mFragments[3] = new AngleFragment();
    }

    @Override
    public BaseFragment getItem(int position) {
        return mFragments[position];
    }

    @Override
    public int getCount() {
        return mFragments.length;
    }
}
