package com.capstone.excavator;

public final class DitchTaskState {

    public static final int DITCH_SQUARE = 0;
    public static final int DITCH_TRAPEZOID = 1;

    public static final int REF_LEFT = 0;
    public static final int REF_MIDDLE = 1;
    public static final int REF_RIGHT = 2;

    private static volatile int ditchType = DITCH_SQUARE;
    private static volatile int refA = REF_MIDDLE;
    private static volatile int refB = REF_MIDDLE;
    private static volatile String abDistance = "";
    private static volatile String longitudinalParam1 = "";
    private static volatile String longitudinalParam2 = "";
    private static volatile String longitudinalParam3 = "";
    private static volatile String longitudinalParam4 = "";
    private static volatile String sideParam1 = "";
    private static volatile String sideParam2 = "";
    private static volatile String sideParam3 = "";
    private static volatile String sideParam4 = "";

    private DitchTaskState() {
    }

    public static void updateBase(int type, int selectedRefA, int selectedRefB, String distance) {
        ditchType = normalizeDitchType(type);
        refA = normalizeRef(selectedRefA);
        refB = normalizeRef(selectedRefB);
        abDistance = safe(distance);
    }

    public static void updateLongitudinalParams(String param1, String param2, String param3, String param4) {
        longitudinalParam1 = safe(param1);
        longitudinalParam2 = safe(param2);
        longitudinalParam3 = safe(param3);
        longitudinalParam4 = safe(param4);
    }

    public static void updateSideParams(String param1, String param2, String param3, String param4) {
        sideParam1 = safe(param1);
        sideParam2 = safe(param2);
        sideParam3 = safe(param3);
        sideParam4 = safe(param4);
    }

    public static void reset() {
        ditchType = DITCH_SQUARE;
        refA = REF_MIDDLE;
        refB = REF_MIDDLE;
        abDistance = "";
        longitudinalParam1 = "";
        longitudinalParam2 = "";
        longitudinalParam3 = "";
        longitudinalParam4 = "";
        sideParam1 = "";
        sideParam2 = "";
        sideParam3 = "";
        sideParam4 = "";
    }

    public static int getDitchType() {
        return ditchType;
    }

    public static int getRefA() {
        return refA;
    }

    public static int getRefB() {
        return refB;
    }

    public static boolean isSquareDitch() {
        return ditchType == DITCH_SQUARE;
    }

    public static String getDitchTypeText() {
        return isSquareDitch() ? "方形沟" : "梯形沟";
    }

    public static String getRefAText() {
        return refText(refA);
    }

    public static String getRefBText() {
        return refText(refB);
    }

    public static String getAbDistance() {
        return abDistance;
    }

    public static String getLongitudinalParam1() {
        return longitudinalParam1;
    }

    public static String getLongitudinalParam2() {
        return longitudinalParam2;
    }

    public static String getLongitudinalParam3() {
        return longitudinalParam3;
    }

    public static String getLongitudinalParam4() {
        return longitudinalParam4;
    }

    public static String getSideParam1() {
        return sideParam1;
    }

    public static String getSideParam2() {
        return sideParam2;
    }

    public static String getSideParam3() {
        return sideParam3;
    }

    public static String getSideParam4() {
        return sideParam4;
    }

    private static int normalizeDitchType(int type) {
        return type == DITCH_TRAPEZOID ? DITCH_TRAPEZOID : DITCH_SQUARE;
    }

    private static int normalizeRef(int ref) {
        if (ref < REF_LEFT || ref > REF_RIGHT) {
            return REF_MIDDLE;
        }
        return ref;
    }

    private static String refText(int ref) {
        switch (ref) {
            case REF_LEFT:
                return "左斗尖";
            case REF_RIGHT:
                return "右斗尖";
            case REF_MIDDLE:
            default:
                return "中斗尖";
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
