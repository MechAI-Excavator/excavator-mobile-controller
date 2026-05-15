package com.capstone.excavator;

/**
 * TCU ↔ 遥控器业务帧（帧头 {@code 0x55 0xAA}）解析。
 *
 * <p>与 100 ms 实时流 {@code 0xFA 0xFA} 分离；用于读取 {@code 0x51} 心跳里的 {@code LinkBitmap}
 * 以及 {@code 0x50} 初始化里的 {@code InitBitmap}，其中 <b>bit6 = IMU</b>：{@code 1} 正常，
 * {@code 0} 异常或未连接（见协议草案 §5.1 / §5.2）。
 *
 * <p>CRC：CRC-16-MODBUS，计算范围为从 {@code MsgID} 起至最后一个 {@code Data} 字节（含 {@code DataLen}），
 * 不含 CRC 自身与帧尾 {@code 0xFF}。
 */
public final class TcuBusinessFrameParser {

    private static final int FRAME_HEAD_1 = 0x55;
    private static final int FRAME_HEAD_2 = 0xAA;
    private static final int FRAME_TAIL = 0xFF;

    /** 初始化状态上报 */
    public static final int MSG_INIT_STATUS = 0x50;
    /** 连接状态 / 心跳（1 s） */
    public static final int MSG_LINK_HEARTBEAT = 0x51;

    /** InitBitmap / LinkBitmap：bit6 = IMU */
    public static final int BIT_IMU = 6;

    private TcuBusinessFrameParser() {
    }

    /**
     * 若 {@code data} 为一条完整且 CRC 正确的业务帧，且为 {@code 0x50} 或 {@code 0x51}，
     * 则根据位图更新 {@link ImuStatusState} 并返回 {@code true}。
     *
     * @return {@code true} 表示已消费该包（调用方不必再按 33 字节实时流解析）
     */
    public static boolean tryConsumeAndUpdateImuLink(byte[] data) {
        if (data == null || data.length < 7) {
            return false;
        }
        if ((data[0] & 0xFF) != FRAME_HEAD_1 || (data[1] & 0xFF) != FRAME_HEAD_2) {
            return false;
        }
        int msgId = data[2] & 0xFF;
        int dataLen = data[3] & 0xFF;
        int frameLen = 7 + dataLen;
        if (data.length < frameLen) {
            return false;
        }
        if ((data[frameLen - 1] & 0xFF) != FRAME_TAIL) {
            return false;
        }
        byte[] crcInput = new byte[2 + dataLen];
        System.arraycopy(data, 2, crcInput, 0, crcInput.length);
        int calcCrc = CRC16Modbus.calculateCRC16Modbus(crcInput);
        int recvCrc = CRC16Modbus.bytesToCRC(data, 4 + dataLen);
        if (calcCrc != recvCrc) {
            return false;
        }

        if (msgId == MSG_INIT_STATUS && dataLen >= 2) {
            int initBitmap = readUint16Be(data, 4);
            ImuStatusState.setTcuImuLinkFromBitmap(initBitmap);
            return true;
        }
        if (msgId == MSG_LINK_HEARTBEAT && dataLen >= 2) {
            int linkBitmap = readUint16Be(data, 4);
            ImuStatusState.setTcuImuLinkFromBitmap(linkBitmap);
            return true;
        }
        return false;
    }

    private static int readUint16Be(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }
}
