package com.bluetooth.connection.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bluetooth.connection.GroupData;
import com.bluetooth.connection.LineData;
import com.bluetooth.connection.R;
import com.bluetooth.connection.main.core.BleService;
import com.bluetooth.connection.main.core.IBleService;
import com.bluetooth.connection.tool.BleDemoActivity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.lang.Integer.parseInt;

/**
 * Created by taro on 2017/6/13.
 */

public class SettingFragment extends BaseFragment implements View.OnClickListener {
    EditText mEtSpeed;
    TextView mTvCharge1;
    TextView mTvTemp1;
    TextView mTvCharge2;
    TextView mTvTemp2;
    ImageView mIvSwitch;

    TextView mTvStart;
    TextView mTvEnd;
    TextView mTvDebug;

    MainActivity mMainAct;
    Handler mHandler;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View contentView = inflater.inflate(R.layout.fragment_setting, container, false);
        mEtSpeed = (EditText) contentView.findViewById(R.id.et_setting_speed_hz);
        mTvCharge1 = (TextView) contentView.findViewById(R.id.tv_setting_charge_1);
        mTvTemp1 = (TextView) contentView.findViewById(R.id.tv_setting_temp_1);
        mTvCharge2 = (TextView) contentView.findViewById(R.id.tv_setting_charge_2);
        mTvTemp2 = (TextView) contentView.findViewById(R.id.tv_setting_temp_2);
        mIvSwitch = (ImageView) contentView.findViewById(R.id.iv_setting_switch);

        mTvStart = (TextView) contentView.findViewById(R.id.tv_setting_start);
        mTvEnd = (TextView) contentView.findViewById(R.id.tv_setting_end);
        mTvDebug = (TextView) contentView.findViewById(R.id.tv_setting_debug);

        mIvSwitch.setOnClickListener(this);
        mTvStart.setOnClickListener(this);
        mTvEnd.setOnClickListener(this);
        mTvDebug.setOnClickListener(this);

        mMainAct = (MainActivity) getActivity();
        mHandler = new UpdateHandler();

        mEtSpeed.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN
                        && keyCode == KeyEvent.KEYCODE_ENTER) {
                    String text = mEtSpeed.getText().toString();
                    boolean isSent = false;
                    if (TextUtils.isDigitsOnly(text)) {
                        int result = Integer.parseInt(text);
                        if (result >= 0 && result <= 100) {
                            sendCmd(new byte[]{(byte) result});
                            isSent = true;
                        }
                    }

                    if (!isSent) {
                        Toast.makeText(getActivity(), "数据不合法", Toast.LENGTH_LONG).show();
                    }
                    return true;
                } else {
                    return false;
                }
            }
        });
        return contentView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeMessages(0x110);
        mHandler = null;
        mMainAct = null;
    }

    @Override
    public void notifyDataSetChanged(String addr, @NonNull GroupData datas, @NonNull LineData item) {
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.tv_setting_debug) {
            startActivity(new Intent(getActivity(), BleDemoActivity.class));
            return;
        }

        if (BleService.getInstance() != null) {
            switch (v.getId()) {
                case R.id.iv_setting_switch:
                    //关闭硬件
                    //再延时断开所有连接(不管是否成功关闭)
                    sendCmd(new byte[]{0x00});
                    mHandler.sendEmptyMessageDelayed(0x119, 2000);
                    break;
                case R.id.tv_setting_start:
                    //开始扫描
                    BleService.getInstance().scanDevices(50000);
                    mHandler.sendEmptyMessageDelayed(0x110, 5000);
                    break;
                case R.id.tv_setting_end:
                    //清除数据
                    BleService.getInstance().releaseAllDevices();
                    break;
            }
        } else {
            Toast.makeText(getContext(), "蓝牙服务未连接上,请稍后重试", Toast.LENGTH_LONG).show();
        }
    }

    private void sendCmd(byte[] btyes) {
        if (btyes == null) {
            return;
        }

        IBleService instance = BleService.getInstance();
        List<String> list = instance.getDevicesAddr();
        for (String addr : list) {
            UUID writeId = BleService.getInstance().getDeviceUUID(addr).get("write");
            UUID serviceId = instance.getDeviceUUID(addr).get("service");
            if (serviceId != null && writeId != null) {
                //通知发送数据
                if (!BleService.getInstance().write(addr, serviceId.toString(), writeId.toString(), btyes)) {
                    Log.e("ble", addr + "|写入队列失败");
                    //写入失败
                }
            }
        }
    }

    private class UpdateHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0x110:
                    Map<String, GroupData> dataMap = mMainAct.getDeviceDatas();
                    for (Map.Entry<String, GroupData> entry : dataMap.entrySet()) {
                        String addr = entry.getKey();
                        GroupData group = entry.getValue();
                        int index = mMainAct.getDeviceIndex(addr);
                        int battery = 0, temp = 0;
                        if (group != null) {
                            battery = group.getBattery();
                            temp = group.getTemperature();
                        }

                        String batStr = battery != -1 ? String.valueOf(battery) + "%" : " - ";
                        String tempStr = temp != -1 ? String.valueOf(temp) + "°C" : " - ";
                        if (index == 1) {
                            mTvCharge1.setText(batStr);
                            mTvTemp1.setText(tempStr);
                        } else if (index == 2) {
                            mTvCharge2.setText(batStr);
                            mTvTemp2.setText(tempStr);
                        }
                    }

                    sendEmptyMessageDelayed(0x110, 10000);
                    break;
                case 0x119:
                    //关闭所有连接
                    if (BleService.getInstance() != null) {
                        BleService.getInstance().releaseAllDevices();
                    }
                    break;
            }
        }
    }
}
