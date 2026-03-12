package com.example.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

public class MapView extends View {
    private final MapViewport viewport = new MapViewport();
    private final MapRenderer renderer = new MapRenderer();
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private MapData mapData;
    private boolean viewportConfigured;
    private boolean hasFixedLocation;
    private double fixedLocationMercatorX;
    private double fixedLocationMercatorY;

    public MapView(Context context) {
        this(context, null);
    }

    public MapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        gestureDetector = new GestureDetector(context, new GestureHandler());
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleHandler());
        setFocusable(true);
        setClickable(true);
    }

    public void setMapData(MapData data) {
        mapData = data;
        viewportConfigured = false;
        configureViewportIfPossible();
        invalidate();
    }

    public void setFixedLocation(double lat, double lon) {
        MapProjection.MercatorPoint p = MapProjection.latLonToMercator(lat, lon);
        fixedLocationMercatorX = p.x;
        fixedLocationMercatorY = p.y;
        hasFixedLocation = true;
        viewportConfigured = false;
        configureViewportIfPossible();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewport.setScreenSize(w, h);
        viewportConfigured = false;
        configureViewportIfPossible();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        renderer.draw(canvas, mapData, viewport,
                hasFixedLocation ? fixedLocationMercatorX : Double.NaN,
                hasFixedLocation ? fixedLocationMercatorY : Double.NaN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);
        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }

    private void configureViewportIfPossible() {
        if (viewportConfigured || mapData == null || getWidth() == 0 || getHeight() == 0) {
            return;
        }

        if (mapData.hasBounds()) {
            viewport.fitToBounds(
                    mapData.minMercatorX,
                    mapData.minMercatorY,
                    mapData.maxMercatorX,
                    mapData.maxMercatorY,
                    24
            );
        }

        // Initial view: center on fixed GPS point if available.
        if (hasFixedLocation) {
            viewport.setCenterMercator(fixedLocationMercatorX, fixedLocationMercatorY);
            viewport.setZoom(Math.max(viewport.getZoomLevel(), 16.0));
        }

        viewportConfigured = true;
    }

    private class GestureHandler extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            viewport.panByPixels(distanceX, distanceY);
            invalidate();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            viewport.zoomBy(2.0f, e.getX(), e.getY());
            invalidate();
            return true;
        }
    }

    private class ScaleHandler extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            viewport.zoomBy(detector.getScaleFactor(), detector.getFocusX(), detector.getFocusY());
            invalidate();
            return true;
        }
    }
}
