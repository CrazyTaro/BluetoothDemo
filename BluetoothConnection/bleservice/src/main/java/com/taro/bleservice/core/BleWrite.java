package com.taro.bleservice.core;

/**
 * Created by taro on 2017/6/20.
 */

public class BleWrite {
    public String mServiceId;
    public String mCharacterId;
    public byte[] mValue;
    public String mAddr;

    public static BleWrite createWriter(String addr, String service, String character, byte[] value) {
        if (addr != null && service != null
                && character != null && value != null) {
            return new BleWrite(addr, service, character, value);
        } else {
            return null;
        }
    }

    public BleWrite() {
    }

    public BleWrite(String addr, String service, String character, byte[] value) {
        mAddr = addr;
        mServiceId = service;
        mCharacterId = character;
        mValue = value;
    }

    public boolean isValid() {
        return mServiceId != null && mCharacterId != null
                && mValue != null && mAddr != null;
    }
}
