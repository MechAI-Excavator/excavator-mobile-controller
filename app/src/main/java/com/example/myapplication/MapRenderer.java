package com.example.myapplication;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

public class MapRenderer {
    private final Paint roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint riverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint forestFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path reusablePath = new Path();
    private final PointF reusablePoint = new PointF();

    public MapRenderer() {
        roadPaint.setStyle(Paint.Style.STROKE);
        roadPaint.setColor(Color.parseColor("#7A7A7A"));
        roadPaint.setStrokeWidth(3f);
        roadPaint.setStrokeCap(Paint.Cap.ROUND);
        roadPaint.setStrokeJoin(Paint.Join.ROUND);

        riverPaint.setStyle(Paint.Style.STROKE);
        riverPaint.setColor(Color.parseColor("#4A90E2"));
        riverPaint.setStrokeWidth(3f);
        riverPaint.setStrokeCap(Paint.Cap.ROUND);
        riverPaint.setStrokeJoin(Paint.Join.ROUND);

        waterFillPaint.setStyle(Paint.Style.FILL);
        waterFillPaint.setColor(Color.parseColor("#8FCBFF"));

        forestFillPaint.setStyle(Paint.Style.FILL);
        forestFillPaint.setColor(Color.parseColor("#9CCB7F"));

        // Overall map transparency (0..255). Lower = more transparent.
        int mapAlpha = 170;
        roadPaint.setAlpha(mapAlpha);
        riverPaint.setAlpha(mapAlpha);
        waterFillPaint.setAlpha(140);
        forestFillPaint.setAlpha(140);

        markerFillPaint.setStyle(Paint.Style.FILL);
        markerFillPaint.setColor(Color.parseColor("#FF9800"));

        markerStrokePaint.setStyle(Paint.Style.STROKE);
        markerStrokePaint.setColor(Color.WHITE);
        markerStrokePaint.setStrokeWidth(2f);

        loadingPaint.setColor(Color.parseColor("#7A7A7A"));
        loadingPaint.setTextSize(28f);
    }

    public void draw(Canvas canvas, MapData mapData, MapViewport viewport,
                     double locationMercatorX, double locationMercatorY) {
        // Slightly transparent background so underlying UI shows through.
        canvas.drawColor(Color.argb(110, 0xF2, 0xEF, 0xE9));
        if (mapData == null) {
            canvas.drawText("Loading map...", 24f, canvas.getHeight() * 0.5f, loadingPaint);
            return;
        }

        drawFilledFeatures(canvas, mapData.waters, viewport, waterFillPaint);
        drawFilledFeatures(canvas, mapData.forests, viewport, forestFillPaint);
        drawPolylineFeatures(canvas, mapData.rivers, viewport, riverPaint);
        drawRoads(canvas, mapData.roads, viewport);

        if (!Double.isNaN(locationMercatorX) && !Double.isNaN(locationMercatorY)) {
            drawLocationMarker(canvas, viewport, locationMercatorX, locationMercatorY);
        }
    }

    private void drawRoads(Canvas canvas, Iterable<MapData.MapFeature> roads, MapViewport viewport) {
        double pixelsPerMeter = viewport.getPixelsPerMeter();
        for (MapData.MapFeature feature : roads) {
            double[] points = feature.mercatorPoints;
            if (points.length < 4) {
                continue;
            }
            float previousX = (float) ((points[0] - viewport.getCenterMercatorX()) * pixelsPerMeter
                    + viewport.getScreenWidth() * 0.5f);
            float previousY = (float) ((viewport.getCenterMercatorY() - points[1]) * pixelsPerMeter
                    + viewport.getScreenHeight() * 0.5f);
            for (int i = 2; i < points.length; i += 2) {
                float currentX = (float) ((points[i] - viewport.getCenterMercatorX()) * pixelsPerMeter
                        + viewport.getScreenWidth() * 0.5f);
                float currentY = (float) ((viewport.getCenterMercatorY() - points[i + 1]) * pixelsPerMeter
                        + viewport.getScreenHeight() * 0.5f);
                canvas.drawLine(previousX, previousY, currentX, currentY, roadPaint);
                previousX = currentX;
                previousY = currentY;
            }
        }
    }

    private void drawPolylineFeatures(Canvas canvas, Iterable<MapData.MapFeature> features,
                                      MapViewport viewport, Paint paint) {
        double pixelsPerMeter = viewport.getPixelsPerMeter();
        for (MapData.MapFeature feature : features) {
            double[] points = feature.mercatorPoints;
            if (points.length < 4) {
                continue;
            }
            reusablePath.reset();
            float startX = (float) ((points[0] - viewport.getCenterMercatorX()) * pixelsPerMeter
                    + viewport.getScreenWidth() * 0.5f);
            float startY = (float) ((viewport.getCenterMercatorY() - points[1]) * pixelsPerMeter
                    + viewport.getScreenHeight() * 0.5f);
            reusablePath.moveTo(startX, startY);
            for (int i = 2; i < points.length; i += 2) {
                float x = (float) ((points[i] - viewport.getCenterMercatorX()) * pixelsPerMeter
                        + viewport.getScreenWidth() * 0.5f);
                float y = (float) ((viewport.getCenterMercatorY() - points[i + 1]) * pixelsPerMeter
                        + viewport.getScreenHeight() * 0.5f);
                reusablePath.lineTo(x, y);
            }
            canvas.drawPath(reusablePath, paint);
        }
    }

    private void drawFilledFeatures(Canvas canvas, Iterable<MapData.MapFeature> features,
                                    MapViewport viewport, Paint paint) {
        double pixelsPerMeter = viewport.getPixelsPerMeter();
        for (MapData.MapFeature feature : features) {
            double[] points = feature.mercatorPoints;
            if (points.length < 6) {
                continue;
            }
            reusablePath.reset();
            float startX = (float) ((points[0] - viewport.getCenterMercatorX()) * pixelsPerMeter
                    + viewport.getScreenWidth() * 0.5f);
            float startY = (float) ((viewport.getCenterMercatorY() - points[1]) * pixelsPerMeter
                    + viewport.getScreenHeight() * 0.5f);
            reusablePath.moveTo(startX, startY);
            for (int i = 2; i < points.length; i += 2) {
                float x = (float) ((points[i] - viewport.getCenterMercatorX()) * pixelsPerMeter
                        + viewport.getScreenWidth() * 0.5f);
                float y = (float) ((viewport.getCenterMercatorY() - points[i + 1]) * pixelsPerMeter
                        + viewport.getScreenHeight() * 0.5f);
                reusablePath.lineTo(x, y);
            }
            if (feature.closed) {
                reusablePath.close();
            }
            canvas.drawPath(reusablePath, paint);
        }
    }

    private void drawLocationMarker(Canvas canvas, MapViewport viewport,
                                    double mercatorX, double mercatorY) {
        MapProjection.mercatorToScreen(mercatorX, mercatorY, viewport, reusablePoint);
        float radius = 10f; // bigger dot
        canvas.drawCircle(reusablePoint.x, reusablePoint.y, radius, markerFillPaint);
        canvas.drawCircle(reusablePoint.x, reusablePoint.y, radius, markerStrokePaint);
    }
}
