package com.capstone.excavator;

public final class SlopeRepairTaskState {

    public static final int TYPE_TOP_LINE = 0;
    public static final int TYPE_BOTTOM_LINE = 1;

    public static final int REF_LEFT = 0;
    public static final int REF_MIDDLE = 1;
    public static final int REF_RIGHT = 2;

    private static volatile int repairType = TYPE_TOP_LINE;
    private static volatile int refA = REF_MIDDLE;
    private static volatile int refB = REF_MIDDLE;
    private static volatile int refC = REF_MIDDLE;
    private static volatile String abDistance = "";
    private static volatile String abLift = "";
    private static volatile String abHeightDiff = "";
    private static volatile String slopeRatio = "";
    private static volatile String verticalHeight = "";
    private static volatile String horizontalDistance = "";
    private static volatile boolean slopeDirectionRight = true;

    private SlopeRepairTaskState() {
    }

    public static void updateRepairType(int type) {
        repairType = type == TYPE_BOTTOM_LINE ? TYPE_BOTTOM_LINE : TYPE_TOP_LINE;
    }

    public static void updateSecondStep(int selectedRefA, int selectedRefB,
                                        String distance, String lift, String heightDiff) {
        refA = normalizeRef(selectedRefA);
        refB = normalizeRef(selectedRefB);
        abDistance = safe(distance);
        abLift = safe(lift);
        abHeightDiff = safe(heightDiff);
    }

    public static void updateThirdStep(int selectedRefC, String ratio, String height,
                                       String horizontal, boolean directionRight) {
        refC = normalizeRef(selectedRefC);
        slopeRatio = safe(ratio);
        verticalHeight = safe(height);
        horizontalDistance = safe(horizontal);
        slopeDirectionRight = directionRight;
    }

    public static void reset() {
        repairType = TYPE_TOP_LINE;
        refA = REF_MIDDLE;
        refB = REF_MIDDLE;
        refC = REF_MIDDLE;
        abDistance = "";
        abLift = "";
        abHeightDiff = "";
        slopeRatio = "";
        verticalHeight = "";
        horizontalDistance = "";
        slopeDirectionRight = true;
    }

    public static int getRepairType() {
        return repairType;
    }

    public static String getRepairTypeText() {
        return repairType == TYPE_TOP_LINE ? "上开口线" : "下开口线";
    }

    public static int getRefA() {
        return refA;
    }

    public static int getRefB() {
        return refB;
    }

    public static int getRefC() {
        return refC;
    }

    public static String getRefAText() {
        return refText(refA);
    }

    public static String getRefBText() {
        return refText(refB);
    }

    public static String getRefCText() {
        return refText(refC);
    }

    public static String getAbDistance() {
        return abDistance;
    }

    public static String getAbLift() {
        return abLift;
    }

    public static String getAbHeightDiff() {
        return abHeightDiff;
    }

    public static String getSlopeRatio() {
        return slopeRatio;
    }

    public static String getVerticalHeight() {
        return verticalHeight;
    }

    public static String getHorizontalDistance() {
        return horizontalDistance;
    }

    public static boolean isSlopeDirectionRight() {
        return slopeDirectionRight;
    }

    public static String getSlopeDirectionText() {
        return slopeDirectionRight ? "右侧" : "左侧";
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
