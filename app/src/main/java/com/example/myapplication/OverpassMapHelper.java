package com.example.myapplication;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OverpassMapHelper {
    private static final String TAG = "OfflineMap";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private OverpassMapHelper() {
    }

    public static void loadOfflineMap(Context context, MapView mapView) {
        EXECUTOR.execute(() -> {
            try (InputStream inputStream = openLocalMap(context)) {
                OSMParser parser = new OSMParser();
                OSMData osmData = parser.parse(inputStream);
                MapData mapData = buildMapData(osmData);
                Log.d(TAG, "Parsed " + osmData.nodes.size() + " nodes, "
                        + osmData.ways.size() + " ways → "
                        + mapData.roads.size() + " roads, "
                        + mapData.rivers.size() + " rivers, "
                        + mapData.waters.size() + " water, "
                        + mapData.forests.size() + " forests");
                new Handler(Looper.getMainLooper()).post(
                        () -> mapView.setMapData(mapData)
                );
            } catch (Exception exception) {
                Log.e(TAG, "Failed to load offline OSM map", exception);
            }
        });
    }

    private static InputStream openLocalMap(Context context) {
        Log.d(TAG, "Loading OSM from res/raw/nansha.osm");
        return context.getResources().openRawResource(R.raw.nansha);
    }

    static MapData buildMapData(OSMData osmData) {
        MapData mapData = new MapData();
        for (OSMWay way : osmData.ways) {
            if (way.nodeRefs.size() < 2) {
                continue;
            }

            boolean isRoad = way.hasTag("highway");
            boolean isRiver = way.hasTag("waterway");
            boolean isWater = "water".equals(way.getTag("natural"));
            boolean isForest = "forest".equals(way.getTag("landuse"))
                    || "wood".equals(way.getTag("natural"));

            if (!isRoad && !isRiver && !isWater && !isForest) {
                continue;
            }

            double[] points = projectWay(osmData, way, isWater || isForest);
            if (points == null) {
                continue;
            }

            MapData.MapFeature feature = new MapData.MapFeature(points, isWater || isForest);
            if (isWater) {
                mapData.addWater(feature);
            } else if (isForest) {
                mapData.addForest(feature);
            } else if (isRiver) {
                mapData.addRiver(feature);
            } else if (isRoad) {
                mapData.addRoad(feature);
            }
        }
        if (mapData.hasBounds()) {
            Log.d(TAG, "Map bounds mercator: ["
                    + mapData.minMercatorX + "," + mapData.minMercatorY + "] - ["
                    + mapData.maxMercatorX + "," + mapData.maxMercatorY + "]");
        } else {
            Log.w(TAG, "Map bounds missing (no drawable features?)");
        }
        return mapData;
    }

    private static double[] projectWay(OSMData osmData, OSMWay way, boolean closePolygon) {
        int validNodeCount = 0;
        for (Long nodeRef : way.nodeRefs) {
            if (osmData.nodes.containsKey(nodeRef)) {
                validNodeCount++;
            }
        }

        if (validNodeCount < 2 || (closePolygon && validNodeCount < 3)) {
            return null;
        }

        int arraySize = validNodeCount * 2;
        boolean shouldClose = closePolygon && !way.isClosed();
        if (shouldClose) {
            arraySize += 2;
        }

        double[] points = new double[arraySize];
        int index = 0;
        double firstX = 0.0;
        double firstY = 0.0;
        boolean firstPointSet = false;

        for (Long nodeRef : way.nodeRefs) {
            OSMNode node = osmData.nodes.get(nodeRef);
            if (node == null) {
                continue;
            }
            MapProjection.MercatorPoint mercator = MapProjection.latLonToMercator(node.lat, node.lon);
            if (!firstPointSet) {
                firstX = mercator.x;
                firstY = mercator.y;
                firstPointSet = true;
            }
            points[index++] = mercator.x;
            points[index++] = mercator.y;
        }

        if (shouldClose) {
            points[index++] = firstX;
            points[index] = firstY;
        }

        return points;
    }
}
