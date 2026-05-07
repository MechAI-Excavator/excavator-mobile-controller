package com.capstone.excavator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.concurrent.CopyOnWriteArrayList;

public final class RtkState {

    public interface OnRtkChangeListener {
        void onRtkChanged(double lat, double lon, boolean valid);
    }

    private static volatile double lat = 0.0;
    private static volatile double lon = 0.0;
    private static volatile boolean valid = false;
    private static final CopyOnWriteArrayList<OnRtkChangeListener> listeners = new CopyOnWriteArrayList<>();

    private RtkState() {
    }

    public static double getLat() {
        return lat;
    }

    public static double getLon() {
        return lon;
    }

    public static boolean isValid() {
        return valid;
    }

    public static void update(double newLat, double newLon) {
        boolean newValid = isValidCoordinate(newLat, newLon);
        lat = newLat;
        lon = newLon;
        valid = newValid;
        notifyListeners();
    }

    public static void clear() {
        lat = 0.0;
        lon = 0.0;
        valid = false;
        notifyListeners();
    }

    public static void addListener(@NonNull OnRtkChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(@Nullable OnRtkChangeListener listener) {
        listeners.remove(listener);
    }

    public static boolean isValidCoordinate(double lat, double lon) {
        return !Double.isNaN(lat)
                && !Double.isInfinite(lat)
                && !Double.isNaN(lon)
                && !Double.isInfinite(lon)
                && lat >= -90.0
                && lat <= 90.0
                && lon >= -180.0
                && lon <= 180.0
                && !(lat == 0.0 && lon == 0.0);
    }

    private static void notifyListeners() {
        for (OnRtkChangeListener listener : listeners) {
            listener.onRtkChanged(lat, lon, valid);
        }
    }
}
