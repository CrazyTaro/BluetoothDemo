package com.taro.bleservice.core;

import android.bluetooth.BluetoothDevice;

/**
 * Created by taro on 2017/6/19.
 */

public interface OnOperationCallback {
    public void onNotifyDataChanged(BluetoothDevice device, byte[] value);

    public boolean onWriteCallback(String addr, String serviceId, String characterId, byte[] value, boolean isSuccess);
}
