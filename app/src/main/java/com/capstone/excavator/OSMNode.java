package com.capstone.excavator;

public class OSMNode {
    public final long id;
    public final double lat;
    public final double lon;

    public OSMNode(long id, double lat, double lon) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
    }
}
