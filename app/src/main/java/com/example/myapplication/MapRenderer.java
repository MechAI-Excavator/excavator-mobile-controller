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
        canvas.drawColor(Color.argb(110, 0xF2, 0xEF, 0xE9));
        if (mapData == null) {
            canvas.drawText("Loading map...", 24f, canvas.getHeight() * 0.5f, loadingPaint);
            return;
        }

        // Pre-compute visible Mercator bounds for viewport culling.
        double ppm = viewport.getPixelsPerMeter();
        double hw = viewport.getScreenWidth() * 0.5 / ppm;
        double hh = viewport.getScreenHeight() * 0.5 / ppm;
        double cx = viewport.getCenterMercatorX();
        double cy = viewport.getCenterMercatorY();
        double vLeft = cx - hw;
        double vRight = cx + hw;
        double vBottom = cy - hh;
        double vTop = cy + hh;

        drawFilledFeatures(canvas, mapData.waters, viewport, waterFillPaint, vLeft, vRight, vBottom, vTop);
        drawFilledFeatures(canvas, mapData.forests, viewport, forestFillPaint, vLeft, vRight, vBottom, vTop);
        drawPolylineFeatures(canvas, mapData.rivers, viewport, riverPaint, vLeft, vRight, vBottom, vTop);
        drawRoads(canvas, mapData.roads, viewport, vLeft, vRight, vBottom, vTop);

        if (!Double.isNaN(locationMercatorX) && !Double.isNaN(locationMercatorY)) {
            drawLocationMarker(canvas, viewport, locationMercatorX, locationMercatorY);
        }
    }

    private static boolean isVisible(MapData.MapFeature f,
                                     double vLeft, double vRight,
                                     double vBottom, double vTop) {
        return f.bboxMaxX >= vLeft && f.bboxMinX <= vRight
                && f.bboxMaxY >= vBottom && f.bboxMinY <= vTop;
    }

    private void drawRoads(Canvas canvas, Iterable<MapData.MapFeature> roads,
                           MapViewport viewport,
                           double vLeft, double vRight, double vBottom, double vTop) {
        double pixelsPerMeter = viewport.getPixelsPerMeter();
        double cx = viewport.getCenterMercatorX();
        double cy = viewport.getCenterMercatorY();
        float hw = viewport.getScreenWidth() * 0.5f;
        float hh = viewport.getScreenHeight() * 0.5f;
        for (MapData.MapFeature feature : roads) {
            if (!isVisible(feature, vLeft, vRight, vBottom, vTop)) {
                continue;
            }
            double[] points = feature.mercatorPoints;
            if (points.length < 4) {
                continue;
            }
            reusablePath.reset();
            reusablePath.moveTo(
                    (float) ((points[0] - cx) * pixelsPerMeter + hw),
                    (float) ((cy - points[1]) * pixelsPerMeter + hh));
            for (int i = 2; i < points.length; i += 2) {
                reusablePath.lineTo(
                        (float) ((points[i] - cx) * pixelsPerMeter + hw),
                        (float) ((cy - points[i + 1]) * pixelsPerMeter + hh));
            }
            canvas.drawPath(reusablePath, roadPaint);
        }
    }

    private void drawPolylineFeatures(Canvas canvas, Iterable<MapData.MapFeature> features,
                                      MapViewport viewport, Paint paint,
                                      double vLeft, double vRight, double vBottom, double vTop) {
        double pixelsPerMeter = viewport.getPixelsPerMeter();
        double cx = viewport.getCenterMercatorX();
        double cy = viewport.getCenterMercatorY();
        float hw = viewport.getScreenWidth() * 0.5f;
        float hh = viewport.getScreenHeight() * 0.5f;
        for (MapData.MapFeature feature : features) {
            if (!isVisible(feature, vLeft, vRight, vBottom, vTop)) {
                continue;
            }
            double[] points = feature.mercatorPoints;
            if (points.length < 4) {
                continue;
            }
            reusablePath.reset();
            reusablePath.moveTo(
                    (float) ((points[0] - cx) * pixelsPerMeter + hw),
                    (float) ((cy - points[1]) * pixelsPerMeter + hh));
            for (int i = 2; i < points.length; i += 2) {
                reusablePath.lineTo(
                        (float) ((points[i] - cx) * pixelsPerMeter + hw),
                        (float) ((cy - points[i + 1]) * pixelsPerMeter + hh));
            }
            canvas.drawPath(reusablePath, paint);
        }
    }

    private void drawFilledFeatures(Canvas canvas, Iterable<MapData.MapFeature> features,
                                    MapViewport viewport, Paint paint,
                                    double vLeft, double vRight, double vBottom, double vTop) {
        double pixelsPerMeter = viewport.getPixelsPerMeter();
        double cx = viewport.getCenterMercatorX();
        double cy = viewport.getCenterMercatorY();
        float hw = viewport.getScreenWidth() * 0.5f;
        float hh = viewport.getScreenHeight() * 0.5f;
        for (MapData.MapFeature feature : features) {
            if (!isVisible(feature, vLeft, vRight, vBottom, vTop)) {
                continue;
            }
            double[] points = feature.mercatorPoints;
            if (points.length < 6) {
                continue;
            }
            reusablePath.reset();
            reusablePath.moveTo(
                    (float) ((points[0] - cx) * pixelsPerMeter + hw),
                    (float) ((cy - points[1]) * pixelsPerMeter + hh));
            for (int i = 2; i < points.length; i += 2) {
                reusablePath.lineTo(
                        (float) ((points[i] - cx) * pixelsPerMeter + hw),
                        (float) ((cy - points[i + 1]) * pixelsPerMeter + hh));
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
