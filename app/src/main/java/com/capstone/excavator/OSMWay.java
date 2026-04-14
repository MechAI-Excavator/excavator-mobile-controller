package com.capstone.excavator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OSMWay {
    public final List<Long> nodeRefs = new ArrayList<>();
    public final Map<String, String> tags = new HashMap<>();

    public boolean hasTag(String key) {
        return tags.containsKey(key);
    }

    public String getTag(String key) {
        return tags.get(key);
    }

    public boolean isClosed() {
        int size = nodeRefs.size();
        return size > 2 && nodeRefs.get(0).equals(nodeRefs.get(size - 1));
    }
}
