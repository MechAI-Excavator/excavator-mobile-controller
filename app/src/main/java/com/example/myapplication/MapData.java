package com.example.myapplication;

import java.util.ArrayList;
import java.util.List;

public class MapData {
    public final List<MapFeature> roads = new ArrayList<>();
    public final List<MapFeature> rivers = new ArrayList<>();
    public final List<MapFeature> waters = new ArrayList<>();
    public final List<MapFeature> forests = new ArrayList<>();

    public double minMercatorX = Double.POSITIVE_INFINITY;
    public double minMercatorY = Double.POSITIVE_INFINITY;
    public double maxMercatorX = Double.NEGATIVE_INFINITY;
    public double maxMercatorY = Double.NEGATIVE_INFINITY;

    public boolean hasBounds() {
        return minMercatorX <= maxMercatorX && minMercatorY <= maxMercatorY;
    }

    public static class MapFeature {
        public final double[] mercatorPoints;
        public final boolean closed;
        public final double bboxMinX;
        public final double bboxMinY;
        public final double bboxMaxX;
        public final double bboxMaxY;

        public MapFeature(double[] mercatorPoints, boolean closed) {
            this.mercatorPoints = mercatorPoints;
            this.closed = closed;
            double x0 = Double.MAX_VALUE, y0 = Double.MAX_VALUE;
            double x1 = -Double.MAX_VALUE, y1 = -Double.MAX_VALUE;
            for (int i = 0; i < mercatorPoints.length; i += 2) {
                double x = mercatorPoints[i];
                double y = mercatorPoints[i + 1];
                if (x < x0) x0 = x;
                if (y < y0) y0 = y;
                if (x > x1) x1 = x;
                if (y > y1) y1 = y;
            }
            bboxMinX = x0;
            bboxMinY = y0;
            bboxMaxX = x1;
            bboxMaxY = y1;
        }
    }

    public void addRoad(MapFeature feature) {
        roads.add(feature);
        expandBounds(feature.mercatorPoints);
    }

    public void addRiver(MapFeature feature) {
        rivers.add(feature);
        expandBounds(feature.mercatorPoints);
    }

    public void addWater(MapFeature feature) {
        waters.add(feature);
        expandBounds(feature.mercatorPoints);
    }

    public void addForest(MapFeature feature) {
        forests.add(feature);
        expandBounds(feature.mercatorPoints);
    }

    private void expandBounds(double[] points) {
        for (int i = 0; i < points.length; i += 2) {
            double x = points[i];
            double y = points[i + 1];
            minMercatorX = Math.min(minMercatorX, x);
            minMercatorY = Math.min(minMercatorY, y);
            maxMercatorX = Math.max(maxMercatorX, x);
            maxMercatorY = Math.max(maxMercatorY, y);
        }
    }
}
