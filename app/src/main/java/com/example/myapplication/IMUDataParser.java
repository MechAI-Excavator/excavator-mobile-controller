package com.example.myapplication;

import android.util.Log;

public class IMUDataParser {

    private static final String TAG = "IMUDataParser";

    // Frame format: 0xFA 0xFA + bytes 3-44 + CRC(2 bytes) + 0xFF
    private static final int PACKET_SIZE = 47;
    private static final byte FRAME_HEADER_1 = (byte) 0xFA;
    private static final byte FRAME_HEADER_2 = (byte) 0xFA;
    private static final byte FRAME_TAIL = (byte) 0xFF;

    private static final int HEADER_START = 0;
    private static final int DATA_START = 2;   // bytes 3-44
    private static final int DATA_LENGTH = 42; // bytes 3-44
    private static final int CRC_START = 44;   // bytes 45-46
    private static final int TAIL_POS = 46;    // byte 47

    // IMU (BCD) offsets
    private static final int BOOM_START = 2;         // bytes 3-5
    private static final int STICK_START = 5;        // bytes 6-8
    private static final int BUCKET_START = 8;       // bytes 9-11
    private static final int CABIN_PITCH_START = 11; // bytes 12-14
    private static final int CABIN_ROLL_START = 14;  // bytes 15-17

    // Encoder (BCD)
    private static final int ENCODER_START = 17;     // bytes 18-20

    // RTK (int32, 1e-7)
    private static final int RTK_START = 20;         // bytes 21-44

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
        public final double rtkX;
        public final double rtkY;
        public final double rtkZ;
        public final double rtkYaw;
        public final double rtkPitch;
        public final double rtkRoll;

        public ParsedData(float boomAngle, float stickAngle, float bucketAngle,
                          float cabinPitchAngle, float cabinRollAngle, float encoderAngle,
                          double rtkX, double rtkY, double rtkZ,
                          double rtkYaw, double rtkPitch, double rtkRoll) {
            this.boomAngle = boomAngle;
            this.stickAngle = stickAngle;
            this.bucketAngle = bucketAngle;
            this.cabinPitchAngle = cabinPitchAngle;
            this.cabinRollAngle = cabinRollAngle;
            this.encoderAngle = encoderAngle;
            this.rtkX = rtkX;
            this.rtkY = rtkY;
            this.rtkZ = rtkZ;
            this.rtkYaw = rtkYaw;
            this.rtkPitch = rtkPitch;
            this.rtkRoll = rtkRoll;
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
            throw new ParseException("Invalid packet length, expected 47 bytes, got " + (data == null ? "null" : data.length));
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

            double rtkX = parseRtkValue(data, RTK_START);
            double rtkY = parseRtkValue(data, RTK_START + 4);
            double rtkZ = parseRtkValue(data, RTK_START + 8);
            double rtkYaw = parseRtkValue(data, RTK_START + 12);
            double rtkPitch = parseRtkValue(data, RTK_START + 16);
            double rtkRoll = parseRtkValue(data, RTK_START + 20);

            return new ParsedData(
                    boomAngle, stickAngle, bucketAngle,
                    cabinPitchAngle, cabinRollAngle, encoderAngle,
                    rtkX, rtkY, rtkZ, rtkYaw, rtkPitch, rtkRoll
            );
        } catch (Exception e) {
            throw new ParseException("Angle/RTK parse error: " + e.getMessage());
        }
    }

    private static double parseRtkValue(byte[] data, int offset) {
        int value = ((data[offset] & 0xFF) << 24)
                | ((data[offset + 1] & 0xFF) << 16)
                | ((data[offset + 2] & 0xFF) << 8)
                | (data[offset + 3] & 0xFF);
        return value * 1e-7;
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
