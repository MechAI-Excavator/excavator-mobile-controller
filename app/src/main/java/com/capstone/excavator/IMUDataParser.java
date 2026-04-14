package com.capstone.excavator;

import android.util.Log;

public class IMUDataParser {

    private static final String TAG = "IMUDataParser";

    // 帧格式：0xFA 0xFA + 3-30字节数据 + CRC(2字节) + 0xFF
    private static final int PACKET_SIZE = 33;
    private static final byte FRAME_HEADER_1 = (byte) 0xFA;
    private static final byte FRAME_HEADER_2 = (byte) 0xFA;
    private static final byte FRAME_TAIL = (byte) 0xFF;

    private static final int HEADER_START = 0;
    private static final int DATA_START = 2;   // 3-30字节
    private static final int DATA_LENGTH = 28; // 3-30字节
    private static final int CRC_START = 30;   // 31-32字节
    private static final int TAIL_POS = 32;    // 33字节

    // IMU（BCD）起始位置
    private static final int BOOM_START = 2;         // bytes 3-5
    private static final int STICK_START = 5;        // bytes 6-8
    private static final int BUCKET_START = 8;       // bytes 9-11
    private static final int CABIN_PITCH_START = 11; // bytes 12-14
    private static final int CABIN_ROLL_START = 14;  // bytes 15-17

    // 编码器（BCD）
    private static final int ENCODER_START = 17;     // bytes 18-20

    // RTK（int40，1e-9）：纬度 + 经度
    private static final int RTK_START = 20;         // bytes 21-30

    public interface ParseResultCallback {
        void onParseSuccess(float boomAngle, float stickAngle, float bucketAngle);
        void onParseError(String error);
    }

    public static class ParsedData {
        public final float boomAngle;
        public final float stickAngle;
        public final float bucketAngle;
        public final float cabinPitchAngle;
        public final float cabinRollAngle;
        public final float encoderAngle;
        public final double rtkLat;
        public final double rtkLon;

        public ParsedData(float boomAngle, float stickAngle, float bucketAngle,
                          float cabinPitchAngle, float cabinRollAngle, float encoderAngle,
                          double rtkLat, double rtkLon) {
            this.boomAngle = boomAngle;
            this.stickAngle = stickAngle;
            this.bucketAngle = bucketAngle;
            this.cabinPitchAngle = cabinPitchAngle;
            this.cabinRollAngle = cabinRollAngle;
            this.encoderAngle = encoderAngle;
            this.rtkLat = rtkLat;
            this.rtkLon = rtkLon;
        }
    }

    public interface ParseResultCallbackV2 {
        void onParseSuccess(ParsedData data);
        void onParseError(String error);
    }

    public static boolean parseData(byte[] data, ParseResultCallback callback) {
        try {
            ParsedData parsed = parseDataInternal(data);
            if (callback != null) {
                callback.onParseSuccess(parsed.boomAngle, parsed.stickAngle, parsed.bucketAngle);
            }
            return true;
        } catch (ParseException e) {
            if (callback != null) {
                callback.onParseError(e.getMessage());
            }
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    public static boolean parseData(byte[] data, ParseResultCallbackV2 callback) {
        try {
            ParsedData parsed = parseDataInternal(data);
            if (callback != null) {
                callback.onParseSuccess(parsed);
            }
            return true;
        } catch (ParseException e) {
            if (callback != null) {
                callback.onParseError(e.getMessage());
            }
            Log.e(TAG, e.getMessage());
            return false;
        }
    }

    private static class ParseException extends Exception {
        ParseException(String message) {
            super(message);
        }
    }

    private static ParsedData parseDataInternal(byte[] data) throws ParseException {
        if (data == null || data.length != PACKET_SIZE) {
            throw new ParseException("Invalid packet length, expected 33 bytes, got " + (data == null ? "null" : data.length));
        }
        if (data[HEADER_START] != FRAME_HEADER_1 || data[HEADER_START + 1] != FRAME_HEADER_2) {
            throw new ParseException(String.format("Invalid header: 0x%02X 0x%02X", data[HEADER_START] & 0xFF, data[HEADER_START + 1] & 0xFF));
        }
        if (data[TAIL_POS] != FRAME_TAIL) {
            throw new ParseException(String.format("Invalid tail: 0x%02X", data[TAIL_POS] & 0xFF));
        }

        byte[] dataForCrc = new byte[DATA_LENGTH];
        System.arraycopy(data, DATA_START, dataForCrc, 0, DATA_LENGTH);
        int calculatedCrc = CRC16Modbus.calculateCRC16Modbus(dataForCrc);
        int receivedCrc = CRC16Modbus.bytesToCRC(data, CRC_START);
        if (calculatedCrc != receivedCrc) {
            throw new ParseException(String.format("CRC mismatch: calc=0x%04X, recv=0x%04X", calculatedCrc, receivedCrc));
        }

        try {
            float boomAngle = parseBCDAngle(data, BOOM_START);
            float stickAngle = parseBCDAngle(data, STICK_START);
            float bucketAngle = parseBCDAngle(data, BUCKET_START);
            float cabinPitchAngle = parseBCDAngle(data, CABIN_PITCH_START);
            float cabinRollAngle = parseBCDAngle(data, CABIN_ROLL_START);
            float encoderAngle = parseBCDAngle(data, ENCODER_START);

            double rtkLat = parseRtkInt40(data, RTK_START);
            double rtkLon = parseRtkInt40(data, RTK_START + 5);

            return new ParsedData(
                    boomAngle, stickAngle, bucketAngle,
                    cabinPitchAngle, cabinRollAngle, encoderAngle,
                    rtkLat, rtkLon
            );
        } catch (Exception e) {
            throw new ParseException("Angle/RTK parse error: " + e.getMessage());
        }
    }

    // RTK经纬度：5字节int40，最高位为符号位，剩余39位为数值，单位1e-9度
    private static double parseRtkInt40(byte[] data, int offset) {
        int b0 = data[offset] & 0xFF;
        boolean negative = (b0 & 0x80) != 0;
        long magnitude = ((long) (b0 & 0x7F) << 32)
                | ((long) (data[offset + 1] & 0xFF) << 24)
                | ((long) (data[offset + 2] & 0xFF) << 16)
                | ((long) (data[offset + 3] & 0xFF) << 8)
                | (long) (data[offset + 4] & 0xFF);
        long signedValue = negative ? -magnitude : magnitude;
        return signedValue * 1e-9;
    }

    private static float parseBCDAngle(byte[] data, int offset) {
        byte byte1 = data[offset];
        int byte1Unsigned = byte1 & 0xFF;
        int high4Bits = (byte1Unsigned >> 4) & 0x0F;
        boolean isNegative = (high4Bits == 0x01);

        int integerHigh = byte1Unsigned & 0x0F;
        byte byte2 = data[offset + 1];
        int integerLow = ((byte2 >> 4) & 0x0F) * 10 + (byte2 & 0x0F);

        byte byte3 = data[offset + 2];
        int decimal = ((byte3 >> 4) & 0x0F) * 10 + (byte3 & 0x0F);

        int integerPart = integerHigh * 100 + integerLow;
        float decimalPart = decimal / 100.0f;
        float angle = integerPart + decimalPart;
        return isNegative ? -angle : angle;
    }
}
