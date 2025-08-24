package com.sanbod.push.utils;

// data-only: همیشه ما
public final class DedupCache {
    // حلقه کوچک از آخرین کلیدها (messageId یا dedup_id)
    private static final java.util.LinkedHashMap<Long, Boolean> MAP = new java.util.LinkedHashMap<>() {
        @Override protected boolean removeEldestEntry(java.util.Map.Entry<Long, Boolean> e) {
            return size() > 200; // آخرین 200 تا
        }
    };
    public static synchronized boolean seen(Long key) {
        if (key == null) return false;
        if (MAP.containsKey(key)) return true;
        MAP.put(key, Boolean.TRUE);
        return false;
    }
}

