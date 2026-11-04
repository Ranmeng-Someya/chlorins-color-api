package com.chlorins.colorapi.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small thread-safe LRU cache.
 *
 * <p>Rendering happens on the render thread, but reloads and other threads may touch the cache, so
 * every operation is synchronised. Contention is negligible: lookups are O(1) and the critical
 * section is a hash map access.</p>
 */
public final class LruCache<K, V> {

    private final int capacity;
    private final LinkedHashMap<K, V> map;

    public LruCache(int capacity) {
        this.capacity = Math.max(16, capacity);
        this.map = new LinkedHashMap<>(Math.min(this.capacity, 512), 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > LruCache.this.capacity;
            }
        };
    }

    public synchronized V get(K key) {
        return map.get(key);
    }

    public synchronized void put(K key, V value) {
        map.put(key, value);
    }

    /** Returns the cached value, computing and storing it when absent. */
    public synchronized V computeIfAbsent(K key, java.util.function.Function<K, V> factory) {
        V value = map.get(key);
        if (value == null) {
            value = factory.apply(key);
            if (value != null) {
                map.put(key, value);
            }
        }
        return value;
    }

    public synchronized void clear() {
        map.clear();
    }

    public synchronized int size() {
        return map.size();
    }
}
