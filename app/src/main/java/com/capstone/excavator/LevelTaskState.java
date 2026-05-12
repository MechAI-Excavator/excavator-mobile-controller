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

    private static volatile double targetHeightM = Double.NaN;
    private static volatile double fillCutM = Double.NaN;

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

        targetHeightM = parseMeters(targetHeight);
        fillCutM = parseMeters(fillCut);
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

    /** 桶尖到地面距离（米），无数据时返回 NaN。 */
    public static double getTargetHeightM() {
        return targetHeightM;
    }

    /** 填挖量（米），通常为负，无数据返回 NaN。 */
    public static double getFillCutM() {
        return fillCutM;
    }

    /**
     * 当前参考点和（米）= 目标高度 + 填挖量，
     * 物理含义：在「设置时刻」斗尖相对设计面的高度差。
     */
    public static double getReferenceSumM() {
        if (Double.isNaN(targetHeightM) || Double.isNaN(fillCutM)) {
            return Double.NaN;
        }
        return targetHeightM + fillCutM;
    }

    public static boolean hasNumericValues() {
        return !Double.isNaN(targetHeightM) && !Double.isNaN(fillCutM);
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

    private static double parseMeters(String value) {
        if (value == null) return Double.NaN;
        String s = value.trim().replace('−', '-');
        if (s.isEmpty() || s.equals("--")) return Double.NaN;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
