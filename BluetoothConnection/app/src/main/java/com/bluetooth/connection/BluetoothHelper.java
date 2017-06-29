package com.bluetooth.connection;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v4.util.SparseArrayCompat;
import android.util.Log;

/**
 * Created by taro on 2017/5/10.
 */

public class BluetoothHelper {
    //ACC/MAG/GYR/EUL/(w)QUA/LIA/GRV ,从第3个字节开始,每2个字节一个轴数据,每个轴都是低/高位
    //每个数据类型都有3个轴(x,y,z)其中QUA多了个w(w,x,y,z)
    //TEMP/CALIB/BATTERY/CRC 温度/备用/电量/校验位 都是一个字节

    public static final int TYPE_GROUP_0C = 0;
    public static final int TYPE_GROUP_0D = 1;
    public static final int TYPE_GROUP_0E = 2;
    public static final int TYPE_GROUP_0F = 3;
    public static final int TYPE_GROUP_ANGLE = 4;

    public static final int TYPE_DATA_ACC = 0;
    public static final int TYPE_DATA_MAG = 1;
    public static final int TYPE_DATA_GYR = 2;
    public static final int TYPE_DATA_EUL = 3;
    public static final int TYPE_DATA_QUA = 4;
    public static final int TYPE_DATA_LIA = 5;
    public static final int TYPE_DATA_GRV = 6;
    public static final int TYPE_EXTRA_TEMPERATURE = 7;
    public static final int TYPE_EXTRA_BATTERY = 8;
    //夹角
    public static final int TYPE_DATA_INCLUDED_ANGLE = 9;

    public static final int TYPE_AXIS_X = 0;
    public static final int TYPE_AXIS_Y = 1;
    public static final int TYPE_AXIS_Z = 2;
    public static final int TYPE_AXIS_W = 3;

    //夹角
    public static final int TYPE_AXIS_ANGLE_INCLUDED = TYPE_AXIS_X;
    //角度1
    public static final int TYPE_AXIS_ANGLE_ONE = TYPE_AXIS_Y;
    //角度2
    public static final int TYPE_AXIS_ANGLE_SECOND = TYPE_AXIS_Z;

    public static final SparseArrayCompat<Integer> K_MAP = new SparseArrayCompat<Integer>(8);
    public static final SparseArrayCompat<Integer> LIMIT = new SparseArrayCompat<>(8);

    static {
        K_MAP.put(TYPE_DATA_ACC, 1);
        K_MAP.put(TYPE_DATA_MAG, 16);
        K_MAP.put(TYPE_DATA_GYR, 16);
        K_MAP.put(TYPE_DATA_EUL, 16);
        //2的14次方
        K_MAP.put(TYPE_DATA_QUA, 1 << 14);
        K_MAP.put(TYPE_DATA_LIA, 1);
        K_MAP.put(TYPE_DATA_GRV, 1);

        LIMIT.put(TYPE_DATA_ACC, 32768);
        LIMIT.put(TYPE_DATA_MAG, 2048);
        LIMIT.put(TYPE_DATA_GYR, 2048);
        LIMIT.put(TYPE_DATA_EUL, 180);
        LIMIT.put(TYPE_DATA_QUA, 2);
        LIMIT.put(TYPE_DATA_LIA, 32768);
        LIMIT.put(TYPE_DATA_GRV, 32768);
    }

    public static LineData collectByte(byte[] bytes) {
        //判断CRC校验码
        if (computeCRC(bytes)) {
            //获取前两个字节,检测是否为 BB 0X
            int firstByte = bytes[0] & 0xFF;
            int secondByte = bytes[1] & 0xFF;
            //判断是否为 01-03
            if (firstByte == 0xBB && secondByte >= 0x0C && secondByte <= 0x0F) {
                switch (secondByte) {
                    case 0x0C:
                        return computeLine0C(bytes);
                    case 0x0D:
                        return computeLine0D(bytes);
                    case 0x0E:
                        return computeLine0E(bytes);
                    case 0x0F:
                        return computeLine0F(bytes);
                }
            }
        }
        return null;
    }

    public static LineData computeLine0C(byte[] bytes) {
        //计算第一组数据
        LineData lineData = new LineData(TYPE_GROUP_0C);
        int begin = 2;
        computeValue(lineData.mData[0], bytes, begin, K_MAP.get(TYPE_DATA_ACC), false);
        begin += 6;
        computeValue(lineData.mData[1], bytes, begin, K_MAP.get(TYPE_DATA_MAG), false);
        return lineData.build();
    }

    public static LineData computeLine0D(byte[] bytes) {
        //计算第二组数据
        LineData lineData = new LineData(TYPE_GROUP_0D);
        int begin = 2;
        computeValue(lineData.mData[0], bytes, begin, K_MAP.get(TYPE_DATA_GYR), false);
        begin += 6;
        computeValue(lineData.mData[1], bytes, begin, K_MAP.get(TYPE_DATA_EUL), false);
        begin += 6;
        //电量电池
        lineData.mExtra = computeBattery(bytes[begin] & 0xFF);
        return lineData.build();
    }

    public static LineData computeLine0E(byte[] bytes) {
        //计算第三组数据
        LineData lineData = new LineData(TYPE_GROUP_0E);
        int begin = 2;
        computeValue(lineData.mData[0], bytes, begin, K_MAP.get(TYPE_DATA_QUA), true);
        return lineData.build();
    }

    public static LineData computeLine0F(byte[] bytes) {
        //计算第四组数据
        LineData lineData = new LineData(TYPE_GROUP_0F);
        int begin = 2;
        computeValue(lineData.mData[0], bytes, begin, K_MAP.get(TYPE_DATA_LIA), false);
        begin += 6;
        computeValue(lineData.mData[1], bytes, begin, K_MAP.get(TYPE_DATA_GRV), false);
        begin += 6;
        lineData.mExtra = bytes[begin] & 0xFF;
        return lineData.build();
    }

    public static LineData computeLineAngle(@NonNull float[] angle1, @NonNull float[] angle2) {
        float value = computeIncludedAngleValue(angle1, angle2);
        LineData lineData = new LineData(TYPE_GROUP_ANGLE);
        //夹角
        lineData.mData[0][0] = value;
        //角度1
        lineData.mData[0][1] = angle1[2];
        //角度2
        lineData.mData[0][2] = angle2[2];
        return lineData.build();
    }


    public static boolean computeCRC(byte[] bytes) {
        if (bytes == null || bytes.length <= 0) {
            return false;
        } else {
            int i, j, CRC, length = bytes.length - 1;
            CRC = 0x00;
            for (i = 0; i < length; i++) {
                //必须转成整数再进行运算,否则数据无法正常校验
                CRC = bytes[i] & 0xFF ^ CRC;
                for (j = 0; j < 8; j++) {
                    if ((CRC & 0x80) != 0) {
                        CRC = ((CRC << 1) % 256) ^ 0x31;
                    } else {
                        CRC = (CRC << 1) % 256;
                    }
                }
            }
            return CRC == (bytes[length] & 0xFF);
        }
    }

    static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    public static void convertStringToHexValue(@NonNull String hex, @NonNull int[] value) {
        if (hex.length() <= 18) {
            for (int i = 0; i < hex.length() / 2; i++) {
                String sub = hex.substring(i * 2, i * 2 + 2);
                value[i] = Integer.parseInt(sub, 16);
            }
        }
    }

    public static void convertByteToHexValue(@NonNull byte[] bytes, @NonNull int[] value) {
        for (int j = 0; j < bytes.length; j++) {
            value[j] = bytes[j] & 0xFF;
        }
    }

    /**
     * 计算某一个类型数据的全部坐标轴数据
     *
     * @param out        输出数据缓存容器
     * @param bytes      数据源
     * @param start      开始位置
     * @param k          权重
     * @param isComputeW 是否计算W轴数据
     * @return
     */
    public static boolean computeValue(@NonNull float[] out, @NonNull byte[] bytes, int start, int k, boolean isComputeW) {
        int length = isComputeW ? 8 : 6;
        if (start < bytes.length - length) {
            int index = 0;
            for (int i = start; i < start + 6; i += 2) {
                out[index] = computeAxisValue(bytes[i] & 0xFF, bytes[i + 1] & 0xFF, k);
                index++;
            }
            if (isComputeW) {
                out[3] = computeAxisValue(bytes[start + 6] & 0xFF, bytes[start + 7] & 0xFF, k);
            }
            return true;
        }
        return false;
    }

    public static float computeIncludedAngleValue(@NonNull float[] angle1, @NonNull float[] angle2) {
        float z1 = angle1[2], z2 = angle2[2];
        float y1 = angle1[1], y2 = angle2[1];
        float result = 0;
        if (z1 * z2 > 0) {
            result = Math.abs(z1 - z2);
        } else {
            result = 360 - Math.abs(z1 - z2);
        }
        if (result > 180) {
            result = 360 - result;
        }
        return result + (y1 + y2) * 0.06f;
    }

    /**
     * 根据z轴数据计算夹角
     *
     * @param z
     * @param z2
     * @return
     */
    public static float computeIncludedAngleValue(float z, float z2) {
        float result = 0;
        if (z * z2 > 0) {
            result = Math.abs(z - z2);
        } else {
            result = 360 - Math.abs(z - z2);
        }
        if (result > 180) {
            result = 360 - result;
        }
        return result;
    }

    /**
     * 计算任何一个轴的数据
     *
     * @param low   低位
     * @param hight 高位
     * @param k     权值
     * @return
     */
    public static float computeAxisValue(int low, int hight, float k) {
        int value = hight * 256 + low;
        if (value >= 32768) {
            value -= 65536;
        }
        return value / k;
    }

    public static int computeBattery(int value) {
        float battery = ((value * 1f - 0x2a) / (0xa3 - 0x2a)) * 100;
        if (battery <= 0 || battery > 100) {
            return 0;
        } else {
            return (int) battery;
        }
    }
}
