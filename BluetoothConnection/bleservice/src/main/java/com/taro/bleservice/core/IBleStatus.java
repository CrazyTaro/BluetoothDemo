package com.taro.bleservice.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

/**
 * Created by taro on 2017/7/7.
 */

public interface IBleStatus {

    /**
     * 开始扫描
     */
    void onScanBegin();

    /**
     * 结束扫描
     */
    void onScanFinished();


    /**
     * 当设备连接上时,需要发现服务则返回true,否则返回false不进行任何操作
     *
     * @param addr 设备地址
     * @param gatt 连接对象
     * @return
     */
    boolean onDeviceConnected(String addr, BluetoothGatt gatt);

    /**
     * 当设备断开连接时,需要尝试重连返回true,否则返回false不进行任何操作
     *
     * @param device 设备对象
     * @return
     */
    boolean onDeviceDisconnected(BluetoothDevice device);

    /**
     * 当所有设备都断开连接时
     */
    void onAllDeviceDisconnected();

    /**
     * 当所有的设备都被释放时
     */
    void onAllDevicesRelease();

    /**
     * 当发现服务操作结束时,一般在整个设备的过滤服务和字段后回调
     *
     * @param addr 设备地址
     * @param gatt 连接对象
     */
    void onDiscoveryServiceFinished(String addr, BluetoothGatt gatt);
}
