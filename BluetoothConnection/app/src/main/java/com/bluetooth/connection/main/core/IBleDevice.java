package com.bluetooth.connection.main.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;

import java.util.Collection;
import java.util.UUID;

/**
 * Created by taro on 2017/6/16.
 */

public interface IBleDevice {
    public static final int UUID_FROM_SERVICE = 1;
    public static final int UUID_FROM_CHARACTER = 2;

    boolean isDeviceValid(BluetoothDevice device, int rssi, byte[] scanRecord);

    boolean isCharacterValid(BluetoothGattCharacteristic character, boolean isNotify, boolean isRead, boolean isWrite);

    boolean isServiceValid(BluetoothGattService service);

    boolean isStopScanAdvance(Collection<String> addr,int deviceCount);

    @NonNull
    String getStoreUUIDTag(int from, UUID uuid);

    boolean isConnectAfterScan(BluetoothDevice device, int rssi, byte[] scanRecord);

    boolean isReconnectedWhenDisconnected(BluetoothDevice device);

    void onAllDevicesRelease();

    void onDiscoveryServiceFinished(String addr, BluetoothGatt gatt);
}
