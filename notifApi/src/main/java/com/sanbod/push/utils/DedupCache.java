package com.sanbod.push.utils;

public final class DedupCache {
    private static final java.util.LinkedHashMap<Long, Boolean> MAP = new java.util.LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long, Boolean> e) {
            return size() > 200;
        }
    };
    public static synchronized boolean seen(Long key) {
        if (key == null) return false;
        if (MAP.containsKey(key)) return true;
        MAP.put(key, Boolean.TRUE);
        return false;
    }
}

