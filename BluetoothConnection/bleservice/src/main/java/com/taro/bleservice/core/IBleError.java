package com.taro.bleservice.core;

/**
 * Created by taro on 2017/6/18.
 */

public interface IBleError {
    public static final int ERROR_CODE_BLUETOOTH_DISABLE = -1;
    public static final int ERROR_CODE_NO_DEVICE_CALLBACK = 1;

    public static final int ERROR_CODE_STATUS_SCANNING = 100;

    void onError(int errorCode);
}
