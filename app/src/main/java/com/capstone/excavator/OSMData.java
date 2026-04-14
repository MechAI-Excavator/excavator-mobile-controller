package com.capstone.excavator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class OSMData {
    public final HashMap<Long, OSMNode> nodes = new HashMap<>();
    public final List<OSMWay> ways = new ArrayList<>();
}
