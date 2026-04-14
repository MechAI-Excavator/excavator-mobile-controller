package com.capstone.excavator;

public class MapViewport {
    private static final double MIN_ZOOM = 2.0;
    private static final double MAX_ZOOM = 22.0;

    private double centerLatitude;
    private double centerLongitude;
    private double centerMercatorX;
    private double centerMercatorY;
    private double zoomLevel = 16.0;
    private int screenWidth;
    private int screenHeight;

    public void setCenter(double latitude, double longitude) {
        centerLatitude = latitude;
        centerLongitude = longitude;
        MapProjection.MercatorPoint point = MapProjection.latLonToMercator(latitude, longitude);
        centerMercatorX = point.x;
        centerMercatorY = point.y;
    }

    public void setCenterMercator(double mercatorX, double mercatorY) {
        centerMercatorX = mercatorX;
        centerMercatorY = mercatorY;
    }

    public void setZoom(double zoom) {
        zoomLevel = clampZoom(zoom);
    }

    public void setScreenSize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
    }

    public void panByPixels(float distanceX, float distanceY) {
        double pixelsPerMeter = getPixelsPerMeter();
        if (pixelsPerMeter <= 0.0) {
            return;
        }
        centerMercatorX += distanceX / pixelsPerMeter;
        centerMercatorY -= distanceY / pixelsPerMeter;
    }

    public void zoomBy(float scaleFactor, float focusX, float focusY) {
        if (scaleFactor <= 0f || screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        double beforeX = screenToMercatorX(focusX);
        double beforeY = screenToMercatorY(focusY);

        zoomLevel = clampZoom(zoomLevel + Math.log(scaleFactor) / Math.log(2.0));

        double pixelsPerMeter = getPixelsPerMeter();
        centerMercatorX = beforeX - (focusX - screenWidth * 0.5f) / pixelsPerMeter;
        centerMercatorY = beforeY + (focusY - screenHeight * 0.5f) / pixelsPerMeter;
    }

    public void fitToBounds(double minX, double minY, double maxX, double maxY, int paddingPx) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            return;
        }

        double widthMeters = Math.max(1.0, maxX - minX);
        double heightMeters = Math.max(1.0, maxY - minY);
        int availableWidth = Math.max(1, screenWidth - paddingPx * 2);
        int availableHeight = Math.max(1, screenHeight - paddingPx * 2);
        double scaleX = availableWidth / widthMeters;
        double scaleY = availableHeight / heightMeters;
        double pixelsPerMeter = Math.max(1e-9, Math.min(scaleX, scaleY));
        double zoom = Math.log(
                (pixelsPerMeter * MapProjection.WORLD_MERCATOR_WIDTH) / MapProjection.TILE_SIZE
        ) / Math.log(2.0);

        zoomLevel = clampZoom(zoom);
        centerMercatorX = (minX + maxX) * 0.5;
        centerMercatorY = (minY + maxY) * 0.5;
    }

    public double getPixelsPerMeter() {
        return (MapProjection.TILE_SIZE * Math.pow(2.0, zoomLevel))
                / MapProjection.WORLD_MERCATOR_WIDTH;
    }

    public double screenToMercatorX(float screenX) {
        return centerMercatorX + (screenX - screenWidth * 0.5f) / getPixelsPerMeter();
    }

    public double screenToMercatorY(float screenY) {
        return centerMercatorY - (screenY - screenHeight * 0.5f) / getPixelsPerMeter();
    }

    public double getCenterLatitude() {
        return centerLatitude;
    }

    public double getCenterLongitude() {
        return centerLongitude;
    }

    public double getCenterMercatorX() {
        return centerMercatorX;
    }

    public double getCenterMercatorY() {
        return centerMercatorY;
    }

    public double getZoomLevel() {
        return zoomLevel;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    private double clampZoom(double zoom) {
        return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom));
    }
}
