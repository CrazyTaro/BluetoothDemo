package com.bluetooth.connection.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.bluetooth.GroupData;
import com.bluetooth.connection.BluetoothHelper;
import com.bluetooth.connection.LineData;
import com.bluetooth.connection.R;
import com.bluetooth.connection.Test;
import com.bluetooth.connection.main.core.BleService;
import com.bluetooth.connection.main.core.IBleDevice;
import com.bluetooth.connection.main.core.IBleService;
import com.bluetooth.connection.main.core.OnOperationCallback;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by taro on 2017/6/13.
 */

public class MainActivity extends AppCompatActivity {
    ViewPager mVpContent;
    TabLayout mTlTab;

    ArrayMap<String, GroupData> mDataMap;
    ArrayMap<String, Integer> mIndexAddrMap;
    boolean mIsBlueReady;
    boolean mIsUpdateUi;
    MainFragmentPageAdapter mPageAdapter;

    Handler mHandler = new DeviceHandler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Test.compute();

        mVpContent = (ViewPager) findViewById(R.id.vp_content);
        mTlTab = (TabLayout) findViewById(R.id.tl_tab);

        mPageAdapter = new MainFragmentPageAdapter(getSupportFragmentManager());
        mVpContent.setOffscreenPageLimit(4);
        mVpContent.setAdapter(mPageAdapter);
        mTlTab.setupWithViewPager(mVpContent);
        mTlTab.getTabAt(0).setText("设置");
        mTlTab.getTabAt(1).setText("设备一");
        mTlTab.getTabAt(2).setText("设备二");
        mTlTab.getTabAt(3).setText("夹角");

        mDataMap = new ArrayMap<>(2);
        mIndexAddrMap = new ArrayMap<>(2);

        BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = manager.getAdapter();
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, 0x110);
        } else {
            mIsBlueReady = true;
        }

        BleService.addServiceConnection(new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, final IBinder service) {
                Toast.makeText(MainActivity.this, "蓝牙服务已启动", Toast.LENGTH_LONG).show();
                BleService.getInstance().setOnDataChangedListener(new OnOperationCallback() {
                    @Override
                    public void onNotifyDataChanged(BluetoothDevice device, byte[] value) {
                        String addr = device.getAddress();
                        LineData data = BluetoothHelper.collectByte(value);
                        if (data != null) {
                            GroupData group = mDataMap.get(addr);
                            if (group == null) {
                                group = new GroupData();
                                mDataMap.put(addr, group);
                            }
                            group.addNewData(data.getGroupType(), data);

                            Integer index = mIndexAddrMap.get(addr);
                            if (index == null) {
                                int page = 1;
                                if (mIndexAddrMap.values().contains(1)) {
                                    page = 2;
                                }
                                mIndexAddrMap.put(addr, page);
                                index = page;
                            }
                            BaseFragment fragment = mPageAdapter.getItem(index);
                            if (mIsUpdateUi && fragment != null) {
                                fragment.setDeviceTag(addr);
                                fragment.notifyDataSetChanged(addr, group, data);
                            }
                        }
                    }

                    @Override
                    public boolean onWriteCallback(String addr, String serviceId, String characterId, byte[] value, boolean isSuccess) {
                        Log.e("ble", addr + "|写入0x" + BluetoothHelper.bytesToHex(value) + (isSuccess ? "成功" : "失败"));
                        return true;
                    }
                });

                BleService.getInstance().setDeviceCallback(new IBleDevice() {
                    @Override
                    public boolean isDeviceValid(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        String name = device.getName();
                        String addr = device.getAddress();
                        return name != null && name.contains("Yiwei") && (addr.contains(":95") || addr.contains(":45"));
                    }

                    @Override
                    public boolean isCharacterValid(BluetoothGattCharacteristic character, boolean isNotify, boolean isRead, boolean isWrite) {
                        UUID uuid = character.getUuid();
                        String id = uuid.toString();
                        if (isNotify && id.contains("fff4")) {
                            return true;
                        } else if (isWrite && id.contains("fff1")) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    @Override
                    public boolean isServiceValid(BluetoothGattService service) {
                        UUID uuid = service.getUuid();
                        String id = uuid.toString();
                        return id.contains("fff0");
                    }

                    @Override
                    public boolean isStopScanAdvance(Collection<String> addr, int deviceCount) {
                        return deviceCount >= 2;
                    }

                    @NonNull
                    @Override
                    public String getStoreUUIDTag(int from, UUID uuid) {
                        String id = uuid.toString();
                        if (id.contains("fff0") && from == UUID_FROM_SERVICE) {
                            return "service";
                        } else if (from == UUID_FROM_CHARACTER) {
                            if (id.contains("fff1")) {
                                return "write";
                            } else if (id.contains("fff4")) {
                                return "notify";
                            }
                        }
                        return id;
                    }

                    @Override
                    public boolean isConnectAfterScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                        return true;
                    }

                    @Override
                    public boolean isReconnectedWhenDisconnected(BluetoothDevice device) {
                        return true;
                    }

                    @Override
                    public void onAllDevicesRelease() {
                        Log.e("devices", "release all devices");
                    }

                    @Override
                    public void onDiscoveryServiceFinished() {
                        IBleService instance = BleService.getInstance();
                        List<String> list = instance.getDevicesAddr();
                        for (String addr : list) {
                            UUID writeId = BleService.getInstance().getDeviceUUID(addr).get("write");
                            UUID serviceId = instance.getDeviceUUID(addr).get("service");
                            UUID notifyId = instance.getDeviceUUID(addr).get("notify");
                            if (serviceId != null && notifyId != null) {
                                if (!instance.notify(addr, serviceId.toString(), notifyId.toString(), true)) {
                                    //开启通知失败
                                    Log.e("ble", addr + "|开启通知失败");
                                    Message msg = Message.obtain();
                                    msg.what = 0x13;
                                    msg.obj = addr;
                                    msg.arg1 = 3;
                                    mHandler.sendMessageDelayed(msg, 100);
                                }
                            }
                            if (serviceId != null && writeId != null) {
                                //通知发送数据
                                if (!BleService.getInstance().write(addr, serviceId.toString(), writeId.toString(), new byte[]{0x66})) {
                                    Log.e("ble", addr + "|写入队列失败");
                                    //写入失败
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Toast.makeText(MainActivity.this, "蓝牙服务已关闭", Toast.LENGTH_LONG).show();
            }
        });
        startService(new Intent(this, BleService.class));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0x110 && resultCode == RESULT_OK) {
            mIsBlueReady = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsUpdateUi = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsUpdateUi = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (BleService.getInstance() != null) {
            BleService.getInstance().stopService();
        }
    }

    public Map<String, GroupData> getDeviceDatas() {
        return mDataMap;
    }

    public int getDeviceIndex(String addr) {
        Integer index = mIndexAddrMap.get(addr);
        return index != null ? index.intValue() : -1;
    }

    private class DeviceHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            int times;
            byte cmd;
            String addr;
            UUID writeId, notifyId, serviceId;
            switch (msg.what) {
                case 0x13:
                    times = msg.arg1;
                    addr = String.valueOf(msg.obj);
                    notifyId = BleService.getInstance().getDeviceUUID(addr).get("notify");
                    serviceId = BleService.getInstance().getDeviceUUID(addr).get("service");
                    if (serviceId != null && notifyId != null) {
                        if (!BleService.getInstance().notify(addr, serviceId.toString(), notifyId.toString(), true)) {
                            Log.e("ble", addr + "|重新开启通知失败");
                            //写入失败
                            times--;
                        }
                    }
                    if (times <= 0) {
                        removeMessages(msg.what);
                    } else {
                        Message newMsg = Message.obtain();
                        newMsg.copyFrom(msg);
                        newMsg.arg1 = times;
                        sendMessageDelayed(newMsg, 100);
                    }
                    break;
            }
        }
    }
}
