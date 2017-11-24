package com.taro.bleservice.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import java.util.Map;
import java.util.UUID;

/**
 * Created by taro on 2017/6/16.
 */

public class BleObj {
    public BluetoothDevice mBlueToothDev;
    public BluetoothGatt mGatt;
    public Map<String, UUID> mUUIDs;

    public BleObj(@NonNull BluetoothDevice dev) {
        mBlueToothDev = dev;
        mUUIDs = new ArrayMap<>();
    }

    public void putUUID(String tag, UUID id) {
        mUUIDs.put(tag, id);
    }

    public void removeUUID(String tag) {
        mUUIDs.remove(tag);
    }

    public UUID getUUID(String tag) {
        return mUUIDs.get(tag);
    }

    public void clearUUID() {
        mUUIDs.clear();
    }
}
