package com.capstone.excavator;

import android.graphics.PointF;

public final class MapProjection {
    public static final double EARTH_RADIUS = 6378137.0;
    public static final double MAX_LATITUDE = 85.05112878;
    public static final double WORLD_MERCATOR_WIDTH = 2.0 * Math.PI * EARTH_RADIUS;
    public static final double TILE_SIZE = 256.0;

    private MapProjection() {
    }

    public static MercatorPoint latLonToMercator(double lat, double lon) {
        double clampedLat = Math.max(-MAX_LATITUDE, Math.min(MAX_LATITUDE, lat));
        double x = Math.toRadians(lon) * EARTH_RADIUS;
        double y = Math.log(Math.tan(Math.PI * 0.25 + Math.toRadians(clampedLat) * 0.5))
                * EARTH_RADIUS;
        return new MercatorPoint(x, y);
    }

    public static void mercatorToScreen(double mercatorX, double mercatorY,
                                        MapViewport viewport, PointF outPoint) {
        double pixelsPerMeter = viewport.getPixelsPerMeter();
        outPoint.x = (float) ((mercatorX - viewport.getCenterMercatorX()) * pixelsPerMeter
                + viewport.getScreenWidth() * 0.5f);
        outPoint.y = (float) ((viewport.getCenterMercatorY() - mercatorY) * pixelsPerMeter
                + viewport.getScreenHeight() * 0.5f);
    }

    public static final class MercatorPoint {
        public final double x;
        public final double y;

        public MercatorPoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
