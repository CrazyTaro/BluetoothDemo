package com.bluetooth.connection.main;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.util.ArrayMap;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.bluetooth.connection.R;
import com.taro.bleservice.core.BleService;
import com.taro.bleservice.core.BluetoothHelper;
import com.taro.bleservice.core.OnOperationCallback;
import com.taro.bleservice.entity.GroupData;
import com.taro.bleservice.entity.LineData;

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
    GroupData mAngleData;
    boolean mIsBlueReady;
    boolean mIsUpdateUi;
    MainFragmentPageAdapter mPageAdapter;

    Handler mHandler = new DeviceHandler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        final BluetoothManager manager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
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

                            //设备界面数据变化更新
                            BaseFragment fragment = mPageAdapter.getItem(index);
                            if (mIsUpdateUi && fragment != null) {
                                fragment.setDeviceTag(addr);
                                fragment.notifyDataSetChanged(addr, group, data);
                            }

                            //夹角界面数据变化更新
                            if (data.getGroupType() == BluetoothHelper.TYPE_GROUP_0D) {
                                float[] angle1 = null;
                                float[] angle2 = null;
                                //获取第一个设备数据
                                group = mDataMap.valueAt(0);
                                if (group != null) {
                                    angle1 = group.getLastLineData(BluetoothHelper.TYPE_GROUP_0D).getAxisValue(BluetoothHelper.TYPE_DATA_EUL);
                                }
                                //获取第二个设备数据
                                group = mDataMap.valueAt(1);
                                if (group != null) {
                                    angle2 = group.getLastLineData(BluetoothHelper.TYPE_GROUP_0D).getAxisValue(BluetoothHelper.TYPE_DATA_EUL);
                                }
                                //数据有效则计算夹角
                                if (angle1 != null && angle2 != null) {
                                    LineData angleLine = BluetoothHelper.computeLineAngle(angle1, angle2);
                                    if (mAngleData == null) {
                                        mAngleData = new GroupData();
                                    }
                                    //保存计算结果
                                    mAngleData.addNewData(BluetoothHelper.TYPE_GROUP_ANGLE, angleLine);

                                    //通知界面更新
                                    BaseFragment angleFragment = mPageAdapter.getItem(3);
                                    if (angleFragment != null) {
                                        angleFragment.notifyDataSetChanged("angle", mAngleData, angleLine);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public boolean onWriteCallback(String addr, String serviceId, String characterId, byte[] value, boolean isSuccess) {
                        Log.e("ble_write", addr + "|写入0x" + BluetoothHelper.bytesToHex(value) + (isSuccess ? "成功" : "失败"));
                        if (mHandler != null) {
                            String message = "设备 " + addr + " 写入指令 0x" + BluetoothHelper.bytesToHex(value) + (isSuccess ? "成功" : "失败");
                            Message msg = Message.obtain();
                            msg.what = 0x120;
                            msg.obj = message;
                            mHandler.sendMessage(msg);
                        }
                        return true;
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
                case 0x120:
                    String message = String.valueOf(msg.obj);
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }
}
