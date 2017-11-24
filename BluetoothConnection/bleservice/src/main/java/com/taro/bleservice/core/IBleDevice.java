package com.taro.bleservice.core;

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
    /**
     * 来自服务的UUID
     */
    public static final int UUID_FROM_SERVICE = 1;
    /**
     * 来自字段的UUID
     */
    public static final int UUID_FROM_CHARACTER = 2;

    /**
     * 判断当前设备是否为有效设备,用于扫描过滤
     *
     * @param device     设备
     * @param rssi
     * @param scanRecord
     * @return
     */
    boolean isDeviceValid(BluetoothDevice device, int rssi, byte[] scanRecord);

    /**
     * 判断当前字段是否为有效字段,用于字段过滤
     *
     * @param device    字段所属的设备
     * @param character 字段值
     * @param isNotify  是否为通知属性
     * @param isRead    是否为读取属性
     * @param isWrite   是否为写属性
     * @return
     */
    boolean isCharacterValid(BluetoothDevice device, BluetoothGattCharacteristic character, boolean isNotify, boolean isRead, boolean isWrite);

    /**
     * 判断当前服务是否为有效服务,用于服务过滤
     *
     * @param device  服务所属的设备
     * @param service 服务值
     * @return
     */
    boolean isServiceValid(BluetoothDevice device, BluetoothGattService service);

    /**
     * 是否提前停止扫描操作
     *
     * @param addr        已扫描到的设备地址
     * @param deviceCount 已扫描到的设备数量
     * @return
     */
    boolean isStopScanAdvance(Collection<String> addr, int deviceCount);

    /**
     * 获取用于存储的UUID tag,此UUID可能来自字段值,也可能是服务
     *
     * @param from 来自字段{@link #UUID_FROM_CHARACTER}或者服务{@link #UUID_FROM_SERVICE}的UUID
     * @param uuid UUID值
     * @return
     */
    @NonNull
    String getStoreUUIDTag(int from, UUID uuid);

    /**
     * 是否在扫描到该设备后直接进行连接操作
     *
     * @param device     扫描到的设备
     * @param rssi
     * @param scanRecord
     * @return
     */
    boolean isConnectAfterScan(BluetoothDevice device, int rssi, byte[] scanRecord);
}
