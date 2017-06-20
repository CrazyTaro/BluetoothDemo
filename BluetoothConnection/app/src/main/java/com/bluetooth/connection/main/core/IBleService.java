package com.bluetooth.connection.main.core;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by taro on 2017/6/16.
 */

public interface IBleService {

    public void enabledBlueTooth();

    public void disableBlueTooth();

    public void releaseAllDevices();

    public void disconnectAllDevices();

    public void relaseDevice(String addr);

    public void scanDevices(long millsTimes);

    public void stopScan();

    public boolean isScanning();

    public boolean notify(String addr, String serviceUUid, String characterUUid, boolean isOpen);

    public boolean write(String addr, String serviceUUid, String characterUUid, byte[] bytes);

    public boolean writeNoResponse(String addr, String serviceUUid, String characterUUid, byte[] bytes);

    public void connectDevice(String addr, boolean autoConnect);

    public void disconnectDevice(String addr);

    public boolean refreshDeviceCache(String addr);

    public boolean refreshAllDeviceCache();

    public void setDeviceCallback(IBleDevice callback);

    public void setErrorCallback(IBleError callback);

    public void setOnDataChangedListener(OnOperationCallback listener);

    public void stopService();

    @Nullable
    public Map<String, UUID> getDeviceUUID(String addr);

    @NonNull
    public List<String> getDevicesAddr();
}
