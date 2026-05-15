package com.capstone.excavator;

/**
 * 顶栏 / 预检等共用的 IMU 展示状态。
 *
 * <p>除「角度通道是否解析为有限数」外，增加 TCU 协议位图 {@code InitBitmap}(0x50) /
 * {@code LinkBitmap}(0x51) 的 <b>bit6 IMU</b>：为 {@code 0} 时不应显示绿色「IMU 全好」。
 */
public final class ImuStatusState {

    public static final int TOTAL_COUNT = 4;

    private static volatile int onlineCount = 0;

    /**
     * TCU 位图 IMU（bit6）：{@code -1} 尚未收到 {@code 0x50}/{@code 0x51}；{@code 0} 断开/异常；
     * {@code 1} 正常。
     */
    private static volatile int tcuImuLinkBit6 = -1;

    private ImuStatusState() {
    }

    public static void setOnlineCount(int count) {
        onlineCount = Math.max(0, Math.min(count, TOTAL_COUNT));
    }

    /**
     * 由 TCU {@code InitBitmap} 或 {@code LinkBitmap} 更新 IMU 链路位（仅看 bit6）。
     *
     * @param bitmap uint16 大端位图（协议 bit6 = IMU）
     */
    public static void setTcuImuLinkFromBitmap(int bitmap) {
        boolean imuOk = (bitmap & (1 << 6)) != 0;
        tcuImuLinkBit6 = imuOk ? 1 : 0;
    }

    /** @return -1 未知；0 TCU 报 IMU 未连接/异常；1 TCU 报 IMU 正常 */
    public static int getTcuImuLinkBit6() {
        return tcuImuLinkBit6;
    }

    /** TCU 明确报 IMU 位为 0 时不应显示绿色「全好」。 */
    public static boolean tcuDeniesImuHealthyGreen() {
        return tcuImuLinkBit6 == 0;
    }

    /** TCU 明确报 IMU 位为 1（可与角度通道数组合判定顶栏绿色）。 */
    public static boolean tcuAssertsImuHealthyGreen() {
        return tcuImuLinkBit6 == 1;
    }

    public static void clear() {
        onlineCount = 0;
        tcuImuLinkBit6 = -1;
    }

    public static int getOnlineCount() {
        return onlineCount;
    }

    public static boolean isAllOnline() {
        return onlineCount == TOTAL_COUNT;
    }

    /** 预检页等与顶栏一致：通道满且 TCU 位图未将 IMU 标为断开（bit6=0）。 */
    public static boolean isImuDataGoodForPrecheckUi() {
        return isAllOnline() && !tcuDeniesImuHealthyGreen();
    }
}
