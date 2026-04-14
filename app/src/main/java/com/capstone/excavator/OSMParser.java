package com.capstone.excavator;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class OSMParser {

    public OSMData parse(InputStream inputStream) throws IOException, XmlPullParserException {
        OSMData data = new OSMData();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, "UTF-8");

        OSMWay currentWay = null;
        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if ("node".equals(name)) {
                    long id = parseLong(parser.getAttributeValue(null, "id"), 0L);
                    double lat = parseDouble(parser.getAttributeValue(null, "lat"), 0.0);
                    double lon = parseDouble(parser.getAttributeValue(null, "lon"), 0.0);
                    data.nodes.put(id, new OSMNode(id, lat, lon));
                } else if ("way".equals(name)) {
                    currentWay = new OSMWay();
                } else if ("nd".equals(name) && currentWay != null) {
                    long ref = parseLong(parser.getAttributeValue(null, "ref"), -1L);
                    if (ref >= 0L) {
                        currentWay.nodeRefs.add(ref);
                    }
                } else if ("tag".equals(name) && currentWay != null) {
                    String key = parser.getAttributeValue(null, "k");
                    String value = parser.getAttributeValue(null, "v");
                    if (key != null && value != null) {
                        currentWay.tags.put(key, value);
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if ("way".equals(parser.getName()) && currentWay != null) {
                    data.ways.add(currentWay);
                    currentWay = null;
                }
            }
            eventType = parser.next();
        }

        return data;
    }

    private long parseLong(String value, long fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double parseDouble(String value, double fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
