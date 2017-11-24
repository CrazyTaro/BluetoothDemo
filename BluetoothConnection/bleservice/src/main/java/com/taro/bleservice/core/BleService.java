package com.taro.bleservice.core;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;

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

    //设备缓存容器
    Map<String, BleObj> mDevicesMap;
    //可复用的缓存处理列表,仅在一个方法中有效
    List<String> mCacheList;
    //已连接设备列表(与实际的连接设备列表可能不完全同步)
    List<String> mConnectedDevices;
    //需要被释放的设备列表
    Set<String> mWaitingReleaseDevices;
    //需要断开连接的设备列表
    Set<String> mWaitingDisconnectDevices;
    //需要等待连接的设备列表
    Set<String> mWaitingConnectDevices;
    //需要写入数据的队列
    Queue<BleWrite> mWaitingWriteQueue;

    //扫描回调
    ScanCallBack mScanCallback;
    //连接回调
    BluetoothGattCallback mConnectedCallback;
    //数据回调
    OnOperationCallback mDataCallback;

    IBleError mErrorCallback;
    //设备筛选配置相关回调
    IBleDevice mDeviceCallback;
    //设备状态改变回调
    IBleStatus mStatusCallback;

    //是否释放全部设备
    boolean mIsReleaseAll;
    //是否断开所有设备
    boolean mIsDisconnectedAll;
    //是否正在扫描中
    boolean mIsScanning;
    //是否正在写入数据中
    volatile boolean mIsWriting;
    //是否正在连接中
    volatile boolean mIsConnecting;

    static BleService mInstance;
    //服务连接对象
    static HashSet<ServiceConnection> mConnections;

    public static synchronized IBleService getInstance() {
        return mInstance;
    }

    /**
     * 添加服务连接状态回调对象
     *
     * @param connection
     */
    public static synchronized void addServiceConnection(ServiceConnection connection) {
        if (connection != null) {
            if (mConnections == null) {
                mConnections = new HashSet<>();
            }
            mConnections.add(connection);

            //若服务已经存在,直接回调已连接方法
            if (mInstance != null) {
                ComponentName name = new ComponentName(mInstance.getPackageName(), mInstance.getClass().getName());
                connection.onServiceConnected(name, null);
            }
        }
    }

    /**
     * 移除服务连接状态回调对象
     *
     * @param connection
     */
    public static synchronized void removeServiceConnection(ServiceConnection connection) {
        if (mConnections != null) {
            //当连接回调列表存在时,尝试移除当前的回调对象
            mConnections.remove(connection);
        }
    }


    /**
     * 清除所有服务连接状态回调对象
     */
    public static synchronized void clearServiceConnection() {
        if (mConnections != null) {
            mConnections.clear();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        //蓝牙服务
        mBluetoothMgr = (BluetoothManager) getSystemService(Service.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothMgr.getAdapter();
        //处理handler
        mHandler = new BleHandler();

        //复用的缓存列表
        mCacheList = new ArrayList<>(8);
        //设备容器
        mDevicesMap = new ArrayMap<>(8);
        //已连接设备列表
        mConnectedDevices = new ArrayList<>(8);
        //内部回调对象
        mScanCallback = new ScanCallBack();
        mConnectedCallback = new ConnectedCallback();
        //待处理设备列表
        mWaitingReleaseDevices = new HashSet<>();
        mWaitingConnectDevices = new HashSet<>();
        mWaitingDisconnectDevices = new HashSet<>();

        if (mConnections != null && mConnections.size() > 0) {
            //当服务被创建时,存在连接回调对象时,回调操作
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
            //当连接回调对象存在时,回调服务销毁操作
            ComponentName name = new ComponentName(mInstance.getPackageName(), mInstance.getClass().getName());
            for (ServiceConnection connection : mConnections) {
                if (connection != null) {
                    connection.onServiceDisconnected(name);
                }
            }
            mConnections.clear();
        }

        //释放所有的设备
        //TODO:释放设备需要时间,是否??
        releaseAllDevices();
        mScanCallback = null;
        mStatusCallback = null;
        mConnectedCallback = null;
        mWaitingReleaseDevices.clear();
        mWaitingConnectDevices.clear();
        mWaitingDisconnectDevices.clear();
        mDevicesMap.clear();
        mInstance = null;
        mConnections = null;
        mBluetoothAdapter = null;
        mBluetoothMgr = null;
        //清除所有的回调对象
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
            //设备需要释放所有的设备标识
            mIsReleaseAll = true;
            if (mIsConnecting) {
                //当前正在连接状态,则延迟处理
                return;
            }
            for (BleObj obj : mDevicesMap.values()) {
                //断开连接(后面再处理设备释放操作)
                if (disconnectDeviceInner(obj)) {
                    return;
                }
            }
        }
    }

    @Override
    public void disconnectAllDevices() {
        if (mDevicesMap.size() > 0) {
            //设备需要全部断开连接标识
            mIsDisconnectedAll = true;
            if (mIsConnecting) {
                //当前正在连接状态,则延迟处理
                return;
            }
            for (BleObj obj : mDevicesMap.values()) {
                if (disconnectDeviceInner(obj)) {
                    return;
                }
            }
        }
    }

    @Override
    public void releaseDevice(String addr) {
        //当前正在连接状态,则延迟处理
        if (mIsConnecting) {
            mWaitingReleaseDevices.add(addr);
            return;
        }
        BleObj obj = mDevicesMap.get(addr);
        if (disconnectDeviceInner(obj)) {
            //不断是否成功操作断开连接,都需要把设备添加到待释放列表中
            //因为断开连接后才会释放设备
            mWaitingReleaseDevices.add(addr);
        }
    }

    @Override
    public boolean scanDevices(long millsTimes) {
        //蓝牙无效不进行扫描
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            if (isErrorCallbackValid()) {
                mErrorCallback.onError(IBleError.ERROR_CODE_BLUETOOTH_DISABLE);
            }
            return false;
        }

        //当前正在扫描,不进行扫描
        if (mIsScanning) {
            if (isErrorCallbackValid()) {
                mErrorCallback.onError(IBleError.ERROR_CODE_STATUS_SCANNING);
            }
            return false;
        }

        //设备处理回调方法不存在,不进行扫描
        if (mDeviceCallback == null) {
            if (isErrorCallbackValid()) {
                mErrorCallback.onError(IBleError.ERROR_CODE_NO_DEVICE_CALLBACK);
            }
            return false;
        }

        //若状态回调存在,通知开始进行扫描
        if (mStatusCallback != null) {
            mStatusCallback.onScanBegin();
        }
        mIsScanning = true;
        //发送延迟停止扫描消息
        mHandler.sendEmptyMessageDelayed(MSG_CODE_STOP_SCAN, millsTimes);
        //重置扫描回调对象
        mScanCallback.reset();
        //开始扫描
        mBluetoothAdapter.startLeScan(mScanCallback);
        return true;
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
        Log.e("ble_write", "尝试 " + addr + " 写入指令 0x" + BluetoothHelper.bytesToHex(bytes));
        //创建写入对象
        BleWrite write = BleWrite.createWriter(addr, serviceUUid, characterUUid, bytes);
        if (write != null) {
            synchronized (this) {
                //若当前正在写入等待结果
                if (mIsWriting) {
                    if (mWaitingWriteQueue == null) {
                        mWaitingWriteQueue = new LinkedBlockingQueue<>();
                    }
                    //将写入操作置于队列中
                    mWaitingWriteQueue.add(write);
                    Log.e("ble_write", "进入排队待写状态");
                } else {
                    //否则尝试进行写入操作
                    writeInner(write);
                }
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
        if (mIsConnecting) {
            mWaitingDisconnectDevices.add(addr);
            return;
        }
        BleObj obj = mDevicesMap.get(addr);
        disconnectDeviceInner(obj);
    }

    @Override
    public boolean refreshDeviceCache(String addr) {
        BleObj obj = mDevicesMap.get(addr);
        if (obj != null && obj.mGatt != null) {
            try {
                final Method refresh = BluetoothGatt.class.getMethod("refresh");
                if (refresh != null) {
                    final boolean success = (Boolean) refresh.invoke(obj.mGatt);
                    return success;
                }
            } catch (Exception e) {
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
    public void setStatusCallback(IBleStatus callback) {
        mStatusCallback = callback;
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

    @NonNull
    @Override
    public List<String> getDevicesAddr() {
        return new ArrayList<>(mDevicesMap.keySet());
    }

    @Nullable
    @Override
    public BluetoothDevice getDeviceByAddr(String addr) {
        if (mDevicesMap != null) {
            BleObj obj = mDevicesMap.get(addr);
            if (obj != null) {
                return obj.mBlueToothDev;
            }
        }
        return null;
    }

    @NonNull
    @Override
    public List<BluetoothDevice> getConnectedDevFromManager() {
        if (mBluetoothMgr != null) {
            return mBluetoothMgr.getConnectedDevices(BluetoothProfile.GATT);
        } else {
            return new ArrayList<>(0);
        }
    }

    @NonNull
    @Override
    public List<String> getConnectedDevByIndex() {
        if (mConnectedDevices == null) {
            return new ArrayList<>(0);
        } else {
            return mConnectedDevices;
        }
    }

    /**
     * 内部写入方法
     *
     * @param write 待写入的对象
     * @return
     */
    private boolean writeInner(BleWrite write) {
        //若待写入对象有效,则尝试写入
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
                        boolean result = obj.mGatt.writeCharacteristic(character);
                        Log.e("ble_write", "写入返回状态:" + result);
                        return result;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 内部断开连接
     *
     * @param obj
     * @return
     */
    private boolean disconnectDeviceInner(BleObj obj) {
        if (obj == null || obj.mGatt == null) {
            return false;
        } else {
            //当前正在连接状态,则延迟处理
            if (mIsConnecting) {
                String addr = obj.mBlueToothDev.getAddress();
                //将设备添加到待断开列表中
                mWaitingDisconnectDevices.add(addr);
                return true;
            }
            //尝试断开连接
            obj.mGatt.disconnect();
            mIsConnecting = true;
            return true;
        }
    }

    /**
     * 内部连接设备
     *
     * @param device
     * @param autoConnect
     * @return
     */
    private boolean connectDeviceInner(BluetoothDevice device, boolean autoConnect) {
        if (device == null) {
            return false;
        }
        //当前正在连接状态,则延迟处理
        if (mIsConnecting) {
            mWaitingConnectDevices.add(device.getAddress());
            return true;
        }

        String addr = device.getAddress();
        BleObj oldObj = mDevicesMap.get(addr);
        if (oldObj == null) {
            //若缓存的设备对象不存在,则将设备添加到设备列表中
            oldObj = new BleObj(device);
            mDevicesMap.put(addr, oldObj);
        }
        //尝试创建连接
        device.connectGatt(this, autoConnect, mConnectedCallback);
        mIsConnecting = true;
        return true;
    }

    /**
     * 尝试继续连接其它的设备(如果有的话)
     *
     * @param autoConnect
     * @return
     */
    private boolean tryContinueConnect(boolean autoConnect) {
        boolean isOk = false;
        mCacheList.clear();
        if (mWaitingConnectDevices.size() > 0 && !mIsConnecting) {
            for (String addr : mWaitingConnectDevices) {
                BleObj obj = mDevicesMap.get(addr);
                if (obj != null) {
                    //获取需要连接的设备
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(obj.mBlueToothDev.getAddress());
                    //设备有效并连接成功时,返回
                    if (connectDeviceInner(device, autoConnect)) {
                        isOk = true;
                        break;
                    } else {
                        //设备无效则将地址置于缓存队列
                        mCacheList.add(addr);
                    }
                }
            }
            //移除所有无效的地址
            mWaitingConnectDevices.removeAll(mCacheList);
        }
        return isOk;
    }

    /**
     * 尝试断开连接其它的设备(如果有的话)
     *
     * @return
     */
    private boolean tryContinueDisconnect() {
        boolean isOk = false;
        mCacheList.clear();
        if (mWaitingDisconnectDevices.size() > 0 && !mIsConnecting) {
            for (String addr : mWaitingDisconnectDevices) {
                BleObj obj = mDevicesMap.get(addr);
                //尝试断开连接
                if (disconnectDeviceInner(obj)) {
                    isOk = true;
                    break;
                } else {
                    //将无效地址缓存起来
                    mCacheList.add(addr);
                }
            }
            //移除无效的断开连接地址
            mWaitingDisconnectDevices.removeAll(mCacheList);
        }

        //若非有任何断开连接操作并且当前的断开全部标识存在时
        //则进行判断是否所有设备已经断开了连接
        if (!isOk && mIsDisconnectedAll) {
            //是否存在未断开连接的设备
            boolean isNotAllDisconnected = false;
            for (BleObj obj : mDevicesMap.values()) {
                if (obj != null && obj.mGatt != null) {
                    isNotAllDisconnected = true;
                    break;
                }
            }
            //当前全部断开状态取决于是否还有设备未完全断开连接
            mIsDisconnectedAll = isNotAllDisconnected;
            if (!isNotAllDisconnected && mStatusCallback != null) {
                //若已经全部断开了连接,回调对应方法
                mStatusCallback.onAllDeviceDisconnected();
            }
        }
        return isOk;
    }

    /**
     * 尝试释放连接
     *
     * @return
     */
    private boolean tryReleaseConnect() {
        if (!mIsConnecting) {
            //已不存在设备时
            if (mDevicesMap.size() <= 0) {
                //仅当前为断开所有设备标识存在时才回调
                if (mIsReleaseAll && mStatusCallback != null) {
                    mStatusCallback.onAllDevicesRelease();
                }
                //取消断开所有设备的标识
                mIsReleaseAll = false;
                return false;
            }
            //释放所有设备
            if (mIsReleaseAll) {
                for (String addr : mDevicesMap.keySet()) {
                    BleObj obj = mDevicesMap.get(addr);
                    //断开连接
                    if (disconnectDeviceInner(obj)) {
                        return true;
                    }
                }
            } else if (mWaitingReleaseDevices.size() > 0) {
                for (String addr : mWaitingReleaseDevices) {
                    BleObj obj = mDevicesMap.get(addr);
                    if (disconnectDeviceInner(obj)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 尝试写入设备数据
     */
    private void tryWriteQueue() {
        synchronized (this) {
            //若存在需要写入的数据
            if (mWaitingWriteQueue != null && mWaitingWriteQueue.size() > 0) {
                while (mWaitingWriteQueue.size() > 0) {
                    //获取需要写入的数据
                    BleWrite write = mWaitingWriteQueue.poll();
                    //尝试写入数据
                    if (writeInner(write)) {
                        Log.e("ble_write", write.mAddr + "读取队列数据写入0x" + BluetoothHelper.bytesToHex(write.mValue));
                        return;
                    }
                }
            }
        }
    }

    /**
     * 是否错误回调方法有效
     *
     * @return
     */
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

    //停止扫描命令
    public static final int MSG_CODE_STOP_SCAN = 1;

    private class BluetoothReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    break;
                case BluetoothAdapter.STATE_OFF:
                    //当蓝牙被关闭时,断开所有的设备连接
                    disconnectAllDevices();
                    break;
            }
        }
    }

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
                    //停止扫描操作
                    if (mIsScanning) {
                        mBluetoothAdapter.stopLeScan(mScanCallback);
                        mIsScanning = false;
                        if (mStatusCallback != null) {
                            mStatusCallback.onScanFinished();
                        }
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
                //过滤扫描到的设备,每个设备只会进行一次处理
                if (mCacheScanSet.contains(addr)) {
                    return;
                }
                mCacheScanSet.add(addr);
                //判断是否有效设备
                if (mDeviceCallback.isDeviceValid(device, rssi, scanRecord)) {
                    //创建设备对象
                    BleObj obj = new BleObj(device);
                    mDevicesMap.put(addr, obj);

                    //判断是否需要立即连接
                    if (mDeviceCallback.isConnectAfterScan(device, rssi, scanRecord)) {
                        log(device, "try connecting");
                        //尝试连接
                        connectDeviceInner(device, false);
                    } else {
                        log(device, "device invalid filter");
                    }

                    //判断是否需要提交结束扫描操作
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
                //连接成功
                onConnectSuccess(gatt);
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                //断开连接
                onConnectFailure(gatt);
            }

            //重置连接状态
            mIsConnecting = false;

            //连接/断开连接与释放设备都是属于连接操作,与连接状态相关,同一时间只会处理一个连接
            //尝试释放设备
            if (tryReleaseConnect()) {
                return;
            }
            //尝试断开设备连接
            if (tryContinueDisconnect()) {
                return;
            }
            //尝试连接设备
            if (tryContinueConnect(false)) {
                return;
            }
        }

        private void onConnectSuccess(BluetoothGatt gatt) {
            log(gatt.getDevice(), "connected");
            String addr = gatt.getDevice().getAddress();
            BleObj obj = mDevicesMap.get(addr);
            obj.mGatt = gatt;

            //将该设备从等待连接的设备列表中移除
            mWaitingConnectDevices.remove(addr);
            //将该设备添加到已连接列表中
            mConnectedDevices.add(addr);

            //回调设备连接状态
            if (mStatusCallback != null
                    && mStatusCallback.onDeviceConnected(addr, gatt)) {
                //若返回true则自动尝试发现服务
                gatt.discoverServices();
            }
        }

        private void onConnectFailure(BluetoothGatt gatt) {
            if (gatt != null) {
                log(gatt.getDevice(), "disconnected");

                String addr = gatt.getDevice().getAddress();
                //清除设备连接及相关的数据
                BleObj obj = mDevicesMap.get(addr);
                obj.clearUUID();
                obj.mGatt = null;
                //关闭连接
                gatt.close();

                //将设备从已连接列表中移除
                mConnectedDevices.remove(addr);
                //将设备从等待断开连接列表中移除
                mWaitingDisconnectDevices.remove(addr);

                if (mConnectedDevices.size() <= 0) {
                    //所有连接设备已经断开
                    if (mStatusCallback != null) {
                        mStatusCallback.onAllDeviceDisconnected();
                    }
                }

                //若需要释放所有设备或者该设备需要释放
                if (mIsReleaseAll || mWaitingReleaseDevices.contains(addr)) {
                    //释放设备
                    obj.mBlueToothDev = null;
                    obj.clearUUID();
                    //从设备列表中移除
                    mDevicesMap.remove(addr);
                    mWaitingReleaseDevices.remove(addr);
                } else {
                    //回调设备断开连接
                    if (mStatusCallback != null
                            && mStatusCallback.onDeviceDisconnected(obj.mBlueToothDev)) {
                        if (mIsDisconnectedAll || mWaitingDisconnectDevices.contains(addr)) {
                            //若需要断开连接或者该设备需要断开连接,则不再尝试下面的断开连接操作
                            mWaitingDisconnectDevices.remove(addr);
                            return;
                        }
                        log(obj.mBlueToothDev, "try reconnecting");
                        //否则设备需要重连,将设备加入待重连列表中
                        mWaitingConnectDevices.add(addr);
                    }
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            log(gatt.getDevice(), "discover service");
            boolean isRead = false, isWrite = false, isNotify = false;
            BluetoothDevice device = gatt.getDevice();
            String addr = device.getAddress();
            BleObj obj = mDevicesMap.get(addr);
            List<BluetoothGattService> services = gatt.getServices();
            //服务处理
            if (services != null) {
                for (BluetoothGattService service : services) {
                    List<BluetoothGattCharacteristic> characters = service.getCharacteristics();

                    if (mDeviceCallback.isServiceValid(device, service)) {
                        UUID serviceId = service.getUuid();
                        //获取服务UUID的标识
                        String tag = mDeviceCallback.getStoreUUIDTag(IBleDevice.UUID_FROM_SERVICE, serviceId);
                        obj.putUUID(tag, serviceId);
                    }

                    //字段处理
                    for (BluetoothGattCharacteristic character : characters) {
                        int property = character.getProperties();
                        isNotify = ((BluetoothGattCharacteristic.PROPERTY_NOTIFY & property) > 0);
                        isRead = ((BluetoothGattCharacteristic.PROPERTY_READ & property) > 0);
                        isWrite = ((BluetoothGattCharacteristic.PROPERTY_WRITE & property) > 0);
                        if (mDeviceCallback.isCharacterValid(device, character, isNotify, isRead, isWrite)) {
                            UUID id = character.getUuid();
                            String tag = mDeviceCallback.getStoreUUIDTag(IBleDevice.UUID_FROM_CHARACTER, id);
                            obj.putUUID(tag, id);
                        }
                    }
                }
            }

            //回调发现服务结束事件
            if (mStatusCallback != null) {
                mStatusCallback.onDiscoveryServiceFinished(addr, gatt);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            String addr = gatt.getDevice().getAddress();
            BluetoothGattService service = characteristic.getService();
            String serviceId = service.getUuid().toString();
            String characterId = characteristic.getUuid().toString();
            byte[] value = characteristic.getValue();
            Log.e("ble_write", "写入回调:" + addr + ",写入指令:0x" + BluetoothHelper.bytesToHex(value));
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
