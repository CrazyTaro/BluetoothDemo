package com.bluetooth.connection.main;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.bluetooth.connection.BlueStatus;
import com.bluetooth.connection.R;
import com.taro.bleservice.core.BleService;
import com.taro.bleservice.core.IBleDevice;
import com.taro.bleservice.core.IBleService;
import com.taro.bleservice.core.IBleStatus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Created by taro on 2017/7/6.
 */

public class DevicesActivity extends AppCompatActivity implements DevicesAdapter.OnViewClickListener, View.OnClickListener {
    Button mBtnScan;
    Button mBtnRemoveAll;
    RecyclerView mRvContent;

    DevicesAdapter mAdapter;
    Handler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devices);
        mBtnScan = (Button) findViewById(R.id.btn_devices_scan);
        mBtnRemoveAll = (Button) findViewById(R.id.btn_devices_remove_all);
        mRvContent = (RecyclerView) findViewById(R.id.rv_content);
        mRvContent.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new DevicesAdapter();
        mAdapter.setOnViewClickListener(this);
        mRvContent.setAdapter(mAdapter);

        mBtnScan.setOnClickListener(this);
        mBtnRemoveAll.setOnClickListener(this);

        mHandler = new DeviceHandler();

        if (BleService.getInstance() != null) {
            refreshDevices();
            BleService.getInstance().setStatusCallback(new IBleStatus() {
                @Override
                public void onScanBegin() {

                }

                @Override
                public void onScanFinished() {
                    refreshDevices();
                }

                @Override
                public boolean onDeviceConnected(String addr, BluetoothGatt gatt) {
                    List<BlueStatus> list = mAdapter.getDatas();
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            BlueStatus item = list.get(i);
                            if (addr.equals(item.addr)) {
                                item.isConnected = true;
                                final int position = i;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAdapter.notifyItemChanged(position);
                                    }
                                });
                                break;
                            }
                        }
                    }
                    return true;
                }

                @Override
                public boolean onDeviceDisconnected(BluetoothDevice device) {
                    String addr = device.getAddress();
                    List<BlueStatus> list = mAdapter.getDatas();
                    if (list != null) {
                        for (int i = 0; i < list.size(); i++) {
                            BlueStatus item = list.get(i);
                            if (addr.equals(item.addr)) {
                                item.isConnected = false;
                                final int position = i;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mAdapter.notifyItemChanged(position);
                                    }
                                });
                                break;
                            }
                        }
                    }
                    return false;
                }

                @Override
                public void onAllDeviceDisconnected() {

                }

                @Override
                public void onAllDevicesRelease() {
                    Log.e("devices", "release all devices");
                }

                @Override
                public void onDiscoveryServiceFinished(String addr, BluetoothGatt gatt) {
                    IBleService instance = BleService.getInstance();
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
                            Log.e("ble_write", addr + "|写入队列失败");
                            //写入失败
                        }
                    }
                }
            });
            BleService.getInstance().setDeviceCallback(new IBleDevice() {
                @Override
                public boolean isDeviceValid(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String name = device.getName();
                    String addr = device.getAddress();
                    return name != null && name.contains("Yiwei");
                }

                @Override
                public boolean isCharacterValid(BluetoothDevice device, BluetoothGattCharacteristic character, boolean isNotify, boolean isRead, boolean isWrite) {
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
                public boolean isServiceValid(BluetoothDevice device, BluetoothGattService service) {
                    UUID uuid = service.getUuid();
                    String id = uuid.toString();
                    return id.contains("fff0");
                }

                @Override
                public boolean isStopScanAdvance(Collection<String> addr, int deviceCount) {
                    return false;
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
                    return false;
                }
            });
        }
    }

    private void refreshDevices() {
        List<String> devices = BleService.getInstance().getDevicesAddr();
        List<String> connectedDevices = BleService.getInstance().getConnectedDevByIndex();
        List<BlueStatus> datas = new ArrayList<BlueStatus>();
        for (int i = 0; i < devices.size(); i++) {
            BluetoothDevice device = BleService.getInstance().getDeviceByAddr(devices.get(i));
            if (device != null) {
                BlueStatus status = new BlueStatus();
                status.addr = devices.get(i);
                status.name = device.getName();
                status.isConnected = connectedDevices.contains(status.addr);
                datas.add(status);
            }
        }
        mAdapter.setDatas(datas);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onViewClick(View v, int position) {
        BlueStatus status = mAdapter.getItem(position);
        switch (v.getId()) {
            case R.id.btn_item_devices_changed_status:
                if (status.isConnected) {
                    BleService.getInstance().disconnectDevice(status.addr);
                } else {
                    BleService.getInstance().connectDevice(status.addr, false);
                }
                break;
            case R.id.btn_item_devices_remove:
                BleService.getInstance().releaseDevice(status.addr);
                mAdapter.getDatas().remove(position);
                mAdapter.notifyItemRemoved(position);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        if (BleService.getInstance() != null) {
            switch (v.getId()) {
                case R.id.btn_devices_scan:
                    BleService.getInstance().scanDevices(30000);
                    break;
                case R.id.btn_devices_remove_all:
                    BleService.getInstance().releaseAllDevices();
                    mAdapter.getDatas().clear();
                    mAdapter.notifyDataSetChanged();
                    break;
            }
        }
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
                case 0x120:
                    String message = String.valueOf(msg.obj);
                    Toast.makeText(DevicesActivity.this, message, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
