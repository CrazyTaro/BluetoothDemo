package com.bluetooth.connection.main.core;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;

import com.bluetooth.connection.BluetoothHelper;
import com.clj.fastble.utils.BleLog;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by taro on 2017/6/16.
 */

public class BleService extends Service implements IBleService {
    BluetoothManager mBluetoothMgr;
    BluetoothAdapter mBluetoothAdapter;
    Handler mHandler;

    Map<String, BleObj> mDevicesMap;
    Set<String> mReleaseDevices;
    Set<String> mWaitingConnectDevices;
    Queue<BleWrite> mWriteQueue;

    ScanCallBack mScanCallback;
    BluetoothGattCallback mConnectedCallback;
    OnOperationCallback mDataCallback;

    IBleError mErrorCallback;
    IBleDevice mDeviceCallback;

    boolean mIsReleaseAll;
    boolean mIsScanning;
    volatile boolean mIsWriting;
    volatile boolean mIsConnecting;

    static BleService mInstance;
    static HashSet<ServiceConnection> mConnections;

    public static synchronized IBleService getInstance() {
        return mInstance;
    }

    public static synchronized void addServiceConnection(ServiceConnection connection) {
        if (connection != null) {
            if (mConnections == null) {
                mConnections = new HashSet<>();
            }
            mConnections.add(connection);

            if (mInstance != null) {
                ComponentName name = new ComponentName(mInstance.getPackageName(), mInstance.getClass().getName());
                connection.onServiceConnected(name, null);
            }
        }
    }

    public static synchronized void removeServiceConnection(ServiceConnection connection) {
        if (mConnections != null) {
            mConnections.remove(connection);
        }
    }

    public static synchronized void clearServiceConnection() {
        if (mConnections != null) {
            mConnections.clear();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mBluetoothMgr = (BluetoothManager) getSystemService(Service.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothMgr.getAdapter();
        mHandler = new BleHandler();

        mDevicesMap = new ArrayMap<>(8);
        mScanCallback = new ScanCallBack();
        mConnectedCallback = new ConnectedCallback();
        mReleaseDevices = new HashSet<>();
        mWaitingConnectDevices = new HashSet<>();

        if (mConnections != null && mConnections.size() > 0) {
            ComponentName name = new ComponentName(mInstance.getPackageName(), mInstance.getClass().getName());
            for (ServiceConnection connection : mConnections) {
                connection.onServiceConnected(name, null);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mConnections != null && mConnections.size() > 0) {
            ComponentName name = new ComponentName(mInstance.getPackageName(), mInstance.getClass().getName());
            for (ServiceConnection connection : mConnections) {
                if (connection != null) {
                    connection.onServiceDisconnected(name);
                }
            }
            mConnections.clear();
        }

        releaseAllDevices();
        mScanCallback = null;
        mConnectedCallback = null;
        mReleaseDevices.clear();
        mWaitingConnectDevices.clear();
        mDevicesMap.clear();
        mInstance = null;
        mConnections = null;
        mBluetoothAdapter = null;
        mBluetoothMgr = null;
        clearServiceConnection();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void enabledBlueTooth() {
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }

    @Override
    public void disableBlueTooth() {
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    @Override
    public void releaseAllDevices() {
        if (mDevicesMap.size() > 0) {
            mIsReleaseAll = true;
            if (mIsConnecting) {
                return;
            }
            for (BleObj obj : mDevicesMap.values()) {
                if (obj != null && obj.mGatt != null) {
                    obj.mGatt.disconnect();
                    obj.mGatt = null;
                    return;
                }
            }
        }
    }

    @Override
    public void disconnectAllDevices() {
        if (mDevicesMap.size() > 0) {
            for (BleObj obj : mDevicesMap.values()) {
                if (obj != null && obj.mGatt != null) {
                    obj.mGatt.disconnect();
                }
            }
        }
    }

    @Override
    public void relaseDevice(String addr) {
        if (mIsConnecting) {
            mReleaseDevices.add(addr);
            return;
        }
        BleObj obj = mDevicesMap.get(addr);
        if (obj != null) {
            mReleaseDevices.add(addr);
            if (obj.mGatt != null) {
                obj.mGatt.disconnect();
                log(obj.mBlueToothDev, "release device");
            }
        }
    }

    @Override
    public void scanDevices(long millsTimes) {
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            return;
        }

        if (mIsScanning) {
            if (isErrorCallbackValid()) {
                mErrorCallback.onError(IBleError.ERROR_CODE_STATUS_SCANNING);
            }
            return;
        }

        if (mDeviceCallback == null) {
            if (isErrorCallbackValid()) {
                mErrorCallback.onError(IBleError.ERROR_CODE_NO_DEVICE_CALLBACK);
            }
            return;
        }

        mIsScanning = true;
        mHandler.sendEmptyMessageDelayed(MSG_CODE_STOP_SCAN, millsTimes);
        mScanCallback.reset();
        mBluetoothAdapter.startLeScan(mScanCallback);
    }

    @Override
    public void stopScan() {
        mHandler.sendEmptyMessage(MSG_CODE_STOP_SCAN);
    }

    @Override
    public boolean isScanning() {
        return mIsScanning;
    }

    @Override
    public boolean notify(String addr, String serviceUUid, String characterUUid, boolean isOpen) {
        BleObj obj = mDevicesMap.get(addr);
        if (obj != null && obj.mGatt != null) {
            BluetoothGattService service = obj.mGatt.getService(UUID.fromString(serviceUUid));
            if (service != null) {
                BluetoothGattCharacteristic character = service.getCharacteristic(UUID.fromString(characterUUid));
                if (character != null
                        && (character.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                    return obj.mGatt.setCharacteristicNotification(character, isOpen);
                }
            }
        }
        return false;
    }

    @Override
    public boolean write(String addr, String serviceUUid, String characterUUid, byte[] bytes) {
        BleWrite write = BleWrite.createWriter(addr, serviceUUid, characterUUid, bytes);
        if (write != null) {
            if (mIsWriting) {
                if (mWriteQueue == null) {
                    mWriteQueue = new LinkedBlockingQueue<>();
                }
                mWriteQueue.add(write);
            } else {
                writeInner(write);
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean writeNoResponse(String addr, String serviceUUid, String characterUUid, byte[] bytes) {
        BleObj obj = mDevicesMap.get(addr);
        if (obj != null && obj.mGatt != null) {
            BluetoothGattService service = obj.mGatt.getService(UUID.fromString(serviceUUid));
            if (service != null) {
                BluetoothGattCharacteristic character = service.getCharacteristic(UUID.fromString(characterUUid));
                if (character != null
                        && (character.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                    character.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    character.setValue(bytes);
                    return obj.mGatt.writeCharacteristic(character);
                }
            }
        }
        return false;
    }

    @Override
    public void connectDevice(String addr, boolean autoConnect) {
        BleObj obj = mDevicesMap.get(addr);
        if (obj != null) {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(obj.mBlueToothDev.getAddress());
            connectDeviceInner(device, autoConnect);
        }
    }

    @Override
    public void disconnectDevice(String addr) {
        BleObj obj = mDevicesMap.get(addr);
        if (obj != null && obj.mGatt != null) {
            obj.mGatt.disconnect();
        }
    }

    @Override
    public boolean refreshDeviceCache(String addr) {
        BleObj obj = mDevicesMap.get(addr);
        if (obj != null && obj.mGatt != null) {
            try {
                final Method refresh = BluetoothGatt.class.getMethod("refresh");
                if (refresh != null) {
                    final boolean success = (Boolean) refresh.invoke(obj.mGatt);
                    BleLog.i("refreshDeviceCache, is success:  " + success);
                    return success;
                }
            } catch (Exception e) {
                BleLog.i("exception occur while refreshing device: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean refreshAllDeviceCache() {
        if (mDevicesMap.size() > 0) {
            boolean isOk = false;
            for (String key : mDevicesMap.keySet()) {
                isOk |= refreshDeviceCache(key);
            }
            return isOk;
        }
        return false;
    }

    @Override
    public void setDeviceCallback(IBleDevice callback) {
        mDeviceCallback = callback;
    }

    @Override
    public void setErrorCallback(IBleError callback) {
        mErrorCallback = callback;
    }

    @Override
    public void setOnDataChangedListener(OnOperationCallback listener) {
        mDataCallback = listener;
    }

    @Override
    public void stopService() {
        stopSelf();
    }

    @Nullable
    @Override
    public Map<String, UUID> getDeviceUUID(String addr) {
        BleObj obj = mDevicesMap.get(addr);
        return obj != null ? obj.mUUIDs : null;
    }

    @Override
    public List<String> getDevicesAddr() {
        return new ArrayList<>(mDevicesMap.keySet());
    }

    private boolean writeInner(BleWrite write) {
        if (write != null && write.isValid()) {
            BleObj obj = mDevicesMap.get(write.mAddr);
            if (obj != null && obj.mGatt != null) {
                BluetoothGattService service = obj.mGatt.getService(UUID.fromString(write.mServiceId));
                if (service != null) {
                    BluetoothGattCharacteristic character = service.getCharacteristic(UUID.fromString(write.mCharacterId));
                    if (character != null
                            && (character.getProperties() & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                        mIsWriting = true;
                        character.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                        character.setValue(write.mValue);
                        return obj.mGatt.writeCharacteristic(character);
                    }
                }
            }
        }
        return false;
    }

    private boolean connectDeviceInner(BluetoothDevice device, boolean autoConnect) {
        if (device == null) {
            return false;
        }
        if (mIsConnecting) {
            mWaitingConnectDevices.add(device.getAddress());
            return false;
        }

        String addr = device.getAddress();
        BleObj oldObj = mDevicesMap.get(addr);
        if (oldObj == null) {
            oldObj = new BleObj(device);
            mDevicesMap.put(addr, oldObj);
        }
        device.connectGatt(this, autoConnect, mConnectedCallback);
        mIsConnecting = true;
        return true;
    }

    private void tryContinueConnect(boolean autoConnect) {
        if (mWaitingConnectDevices.size() > 0 && !mIsConnecting) {
            for (String addr : mWaitingConnectDevices) {
                BleObj obj = mDevicesMap.get(addr);
                if (obj != null) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(obj.mBlueToothDev.getAddress());
                    connectDeviceInner(device, autoConnect);
                    return;
                }
            }
        }
    }

    private void tryReleaseConnect() {
        if (!mIsConnecting) {
            if (mIsReleaseAll) {
                if (mDevicesMap.size() > 0) {
                    for (String addr : mDevicesMap.keySet()) {
                        BleObj obj = mDevicesMap.get(addr);
                        if (obj != null && obj.mGatt != null) {
                            obj.mGatt.disconnect();
                            mIsConnecting = true;
                            return;
                        }
                    }
                } else {
                    if (mDeviceCallback != null) {
                        reset();
                        mDeviceCallback.onAllDevicesRelease();
                    }
                }
            } else if (mReleaseDevices.size() > 0) {
                for (String addr : mReleaseDevices) {
                    BleObj obj = mDevicesMap.get(addr);
                    if (obj != null && obj.mGatt != null) {
                        obj.mGatt.disconnect();
                        mIsConnecting = true;
                        return;
                    }
                }
            }
        }
    }

    private void tryWriteQueue() {
        if (mWriteQueue != null && mWriteQueue.size() > 0) {
            while (mWriteQueue.size() > 0) {
                BleWrite write = mWriteQueue.poll();
                if (writeInner(write)) {
                    return;
                }
            }
        }
    }

    private void reset() {
        mIsConnecting = false;
        mIsScanning = false;
        mIsReleaseAll = false;
    }

    private boolean isErrorCallbackValid() {
        return mErrorCallback != null;
    }

    private void log(BluetoothDevice device, String msg) {
        if (msg == null) {
            return;
        }
        String tag = "ble";
        if (device != null) {
            String format = "%s|%s";
            msg = String.format(format, device.getAddress(), msg);
        }
        Log.e(tag, msg);
    }

    public static final int MSG_CODE_STOP_SCAN = 1;

    private class BleHandler extends Handler {
        public BleHandler() {
            super();
        }

        public BleHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CODE_STOP_SCAN:
                    if (mIsScanning) {
                        mBluetoothAdapter.stopLeScan(mScanCallback);
                        mIsScanning = false;
                    }
                    break;
            }
        }
    }

    private class ScanCallBack implements BluetoothAdapter.LeScanCallback {
        private HashSet<String> mCacheScanSet;

        public ScanCallBack() {
            mCacheScanSet = new HashSet<>();
        }

        public void reset() {
            mCacheScanSet.clear();
        }

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (device != null) {
                String addr = device.getAddress();
                if (mCacheScanSet.contains(addr)) {
                    return;
                }
                mCacheScanSet.add(addr);
                if (mDeviceCallback.isDeviceValid(device, rssi, scanRecord)) {
                    BleObj obj = new BleObj(device);
                    mDevicesMap.put(addr, obj);

                    if (mDeviceCallback.isConnectAfterScan(device, rssi, scanRecord)) {
                        log(device, "try connecting");
                        connectDeviceInner(device, false);
                    } else {
                        log(device, "device invalid filter");
                    }

                    if (mDeviceCallback.isStopScanAdvance(mDevicesMap.keySet(), mDevicesMap.size())) {
                        mHandler.sendEmptyMessage(MSG_CODE_STOP_SCAN);
                    }
                }
            }
        }
    }

    private class ConnectedCallback extends BluetoothGattCallback {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                onConnectSuccess(gatt);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                onConnectFailure(gatt);
            }

            mIsConnecting = false;
            tryContinueConnect(false);

            tryReleaseConnect();
        }

        private void onConnectSuccess(BluetoothGatt gatt) {
            log(gatt.getDevice(), "connected");
            String addr = gatt.getDevice().getAddress();
            BleObj obj = mDevicesMap.get(addr);
            obj.mGatt = gatt;

            mWaitingConnectDevices.remove(addr);
            gatt.discoverServices();
        }

        private void onConnectFailure(BluetoothGatt gatt) {
            if (gatt != null) {
                log(gatt.getDevice(), "disconnected");

                String addr = gatt.getDevice().getAddress();
                BleObj obj = mDevicesMap.get(addr);
                obj.clearUUID();
                obj.mGatt = null;
                gatt.close();

                if (mIsReleaseAll || mReleaseDevices.contains(addr)) {
                    obj.mBlueToothDev = null;
                    obj.clearUUID();
                    mDevicesMap.remove(addr);
                    mReleaseDevices.remove(addr);
                } else {
                    if (mDeviceCallback.isReconnectedWhenDisconnected(obj.mBlueToothDev)) {
                        log(obj.mBlueToothDev, "try reconnecting");
                        mWaitingConnectDevices.add(addr);
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            log(gatt.getDevice(), "discover service");
            boolean isRead = false, isWrite = false, isNotify = false;
            String addr = gatt.getDevice().getAddress();
            BleObj obj = mDevicesMap.get(addr);
            List<BluetoothGattService> services = gatt.getServices();
            if (services != null) {
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characters = service.getCharacteristics();
                    if (mDeviceCallback.isServiceValid(service)) {
                        UUID serviceId = service.getUuid();
                        String tag = mDeviceCallback.getStoreUUIDTag(IBleDevice.UUID_FROM_SERVICE, serviceId);
                        obj.putUUID(tag, serviceId);
                    }

                    for (BluetoothGattCharacteristic character : characters) {
                        int property = character.getProperties();
                        isNotify = ((BluetoothGattCharacteristic.PROPERTY_NOTIFY & property) > 0);
                        isRead = ((BluetoothGattCharacteristic.PROPERTY_READ & property) > 0);
                        isWrite = ((BluetoothGattCharacteristic.PROPERTY_WRITE & property) > 0);
                        if (mDeviceCallback.isCharacterValid(character, isNotify, isRead, isWrite)) {
                            UUID id = character.getUuid();
                            String tag = mDeviceCallback.getStoreUUIDTag(IBleDevice.UUID_FROM_CHARACTER, id);
                            obj.putUUID(tag, id);
                        }
                    }
                }
            }

            mDeviceCallback.onDiscoveryServiceFinished();
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            String addr = gatt.getDevice().getAddress();
            BluetoothGattService service = characteristic.getService();
            String serviceId = service.getUuid().toString();
            String characterId = characteristic.getUuid().toString();
            byte[] value = characteristic.getValue();
            if (mDataCallback != null) {
                mIsWriting = false;
                if (mDataCallback.onWriteCallback(addr, serviceId, characterId, value, status == BluetoothGatt.GATT_SUCCESS)) {
                    tryWriteQueue();
                }
            } else {
                mIsWriting = false;
                tryWriteQueue();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic
                characteristic) {
            byte[] values = characteristic.getValue();
            log(gatt.getDevice(), BluetoothHelper.bytesToHex(values));
            //收集数据并处理回调
            if (mDataCallback != null) {
                mDataCallback.onNotifyDataChanged(gatt.getDevice(), values);
            }
        }
    }
}
