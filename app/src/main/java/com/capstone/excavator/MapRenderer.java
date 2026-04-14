package com.capstone.excavator;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;

public class MapRenderer {
    private final Paint roadPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint riverPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint waterFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint forestFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint locationDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint locationDotStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint locationHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint locationGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint compassFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint compassStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint compassTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect reusableTextBounds = new Rect();
    private final Paint loadingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path reusablePath = new Path();
    private final PointF reusablePoint = new PointF();

    public MapRenderer() {
        roadPaint.setStyle(Paint.Style.STROKE);
        roadPaint.setColor(Color.parseColor("#E6FFFFFF"));
        roadPaint.setStrokeWidth(0.8f);
        roadPaint.setStrokeCap(Paint.Cap.ROUND);
        roadPaint.setStrokeJoin(Paint.Join.ROUND);

        riverPaint.setStyle(Paint.Style.STROKE);
        // Render all polylines in the same light style (matches contour-like look).
        riverPaint.setColor(Color.parseColor("#E6FFFFFF"));
        riverPaint.setStrokeWidth(2.0f);
        riverPaint.setStrokeCap(Paint.Cap.ROUND);
        riverPaint.setStrokeJoin(Paint.Join.ROUND);

        waterFillPaint.setStyle(Paint.Style.FILL);
        waterFillPaint.setColor(Color.TRANSPARENT);

        forestFillPaint.setStyle(Paint.Style.FILL);
        forestFillPaint.setColor(Color.TRANSPARENT);

        // Overall map transparency (0..255). Lower = more transparent.
        int mapAlpha = 150;
        roadPaint.setAlpha(mapAlpha);
        riverPaint.setAlpha(mapAlpha);
        waterFillPaint.setAlpha(0);
        forestFillPaint.setAlpha(0);

        locationDotPaint.setStyle(Paint.Style.FILL);
        locationDotPaint.setColor(Color.parseColor("#1E7BFF"));

        locationDotStrokePaint.setStyle(Paint.Style.STROKE);
        locationDotStrokePaint.setColor(Color.parseColor("#B3FFFFFF"));
        locationDotStrokePaint.setStrokeWidth(2f);

        locationHaloPaint.setStyle(Paint.Style.FILL);
        locationGlowPaint.setStyle(Paint.Style.FILL);

        compassFillPaint.setStyle(Paint.Style.FILL);
        compassFillPaint.setColor(Color.parseColor("#66000000"));
        compassStrokePaint.setStyle(Paint.Style.STROKE);
        compassStrokePaint.setColor(Color.parseColor("#66FFFFFF"));
        compassStrokePaint.setStrokeWidth(1.5f);
        compassTextPaint.setColor(Color.WHITE);
        compassTextPaint.setTextSize(26f);
        compassTextPaint.setFakeBoldText(true);

        loadingPaint.setColor(Color.parseColor("#7A7A7A"));
        loadingPaint.setTextSize(28f);
    }

    public void draw(Canvas canvas, MapData mapData, MapViewport viewport,
                     double locationMercatorX, double locationMercatorY) {
        // Don't paint an opaque/bright base color here; let the parent card background show through.
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

        drawNorthCompass(canvas);
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

        // A soft outer halo (very faint) plus a tighter glow around the dot.
        float haloRadius = 58f;
        locationHaloPaint.setShader(new RadialGradient(
                reusablePoint.x, reusablePoint.y,
                haloRadius,
                new int[]{
                        Color.argb(70, 0x1E, 0x7B, 0xFF),
                        Color.argb(0, 0x1E, 0x7B, 0xFF)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(reusablePoint.x, reusablePoint.y, haloRadius, locationHaloPaint);

        float glowRadius = 34f;
        locationGlowPaint.setShader(new RadialGradient(
                reusablePoint.x, reusablePoint.y,
                glowRadius,
                new int[]{
                        Color.argb(140, 0x1E, 0x7B, 0xFF),
                        Color.argb(0, 0x1E, 0x7B, 0xFF)
                },
                new float[]{0f, 1f},
                Shader.TileMode.CLAMP
        ));
        canvas.drawCircle(reusablePoint.x, reusablePoint.y, glowRadius, locationGlowPaint);

        float dotRadius = 12f;
        canvas.drawCircle(reusablePoint.x, reusablePoint.y, dotRadius, locationDotPaint);
        canvas.drawCircle(reusablePoint.x, reusablePoint.y, dotRadius, locationDotStrokePaint);
    }

    private void drawNorthCompass(Canvas canvas) {
        float cx = canvas.getWidth() - 34f;
        float cy = 34f;
        float r = 22f;

        canvas.drawCircle(cx, cy, r, compassFillPaint);
        canvas.drawCircle(cx, cy, r, compassStrokePaint);

        String n = "N";
        compassTextPaint.getTextBounds(n, 0, n.length(), reusableTextBounds);
        float textX = cx - reusableTextBounds.width() * 0.5f - reusableTextBounds.left;
        float textY = cy + reusableTextBounds.height() * 0.5f - reusableTextBounds.bottom;
        canvas.drawText(n, textX, textY, compassTextPaint);
    }
}
