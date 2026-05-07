package com.capstone.excavator;

public final class ImuStatusState {

    public static final int TOTAL_COUNT = 4;

    private static volatile int onlineCount = 0;

    private ImuStatusState() {
    }

    public static void setOnlineCount(int count) {
        onlineCount = Math.max(0, Math.min(count, TOTAL_COUNT));
    }

    public static void clear() {
        onlineCount = 0;
    }

    public static int getOnlineCount() {
        return onlineCount;
    }

    public static boolean isAllOnline() {
        return onlineCount == TOTAL_COUNT;
    }
}
