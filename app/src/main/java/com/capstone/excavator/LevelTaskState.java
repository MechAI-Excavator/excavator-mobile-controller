package com.capstone.excavator;

public final class LevelTaskState {

    public static final int REF_LEFT = 0;
    public static final int REF_MIDDLE = 1;
    public static final int REF_RIGHT = 2;

    private static volatile int referencePoint = REF_MIDDLE;
    private static volatile boolean heightMode = true;
    private static volatile String targetHeight = "";
    private static volatile String fillCut = "";
    private static volatile String targetLon = "";
    private static volatile String targetLat = "";
    private static volatile String targetZ = "";

    private LevelTaskState() {
    }

    public static void update(int ref, boolean isHeightMode, String height, String fill,
                              String lon, String lat, String z) {
        referencePoint = normalizeRef(ref);
        heightMode = isHeightMode;
        targetHeight = safe(height);
        fillCut = safe(fill);
        targetLon = safe(lon);
        targetLat = safe(lat);
        targetZ = safe(z);
    }

    public static int getReferencePoint() {
        return referencePoint;
    }

    public static String getReferencePointText() {
        switch (referencePoint) {
            case REF_LEFT:
                return "左斗尖";
            case REF_RIGHT:
                return "右斗尖";
            case REF_MIDDLE:
            default:
                return "中斗尖";
        }
    }

    public static boolean isHeightMode() {
        return heightMode;
    }

    public static String getModeText() {
        return heightMode ? "高度定点" : "坐标定点";
    }

    public static String getTargetHeight() {
        return targetHeight;
    }

    public static String getFillCut() {
        return fillCut;
    }

    public static String getTargetLon() {
        return targetLon;
    }

    public static String getTargetLat() {
        return targetLat;
    }

    public static String getTargetZ() {
        return targetZ;
    }

    private static int normalizeRef(int ref) {
        if (ref < REF_LEFT || ref > REF_RIGHT) {
            return REF_MIDDLE;
        }
        return ref;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
