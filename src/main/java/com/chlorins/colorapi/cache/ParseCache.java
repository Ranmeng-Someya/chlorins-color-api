package com.chlorins.colorapi.cache;

import com.chlorins.colorapi.parser.ColorParser;
import com.chlorins.colorapi.parser.ParsedText;

import java.util.concurrent.atomic.AtomicLong;

/**
 * LRU cache of parsed {@link ParsedText} values (component path: translations, public API).
 *
 * <p>The render path uses {@code TextPipeline}'s overlay cache instead, because that one stores
 * per-character arrays rather than component trees.</p>
 */
public final class ParseCache {

    private static final int DEFAULT_LIMIT = 4096;

    private final LruCache<String, ParsedText> cache;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public ParseCache() {
        this(DEFAULT_LIMIT);
    }

    public ParseCache(int limit) {
        this.cache = new LruCache<>(limit);
    }

    public ParsedText get(String raw) {
        if (raw == null || raw.isEmpty()) {
            return ParsedText.EMPTY;
        }
        ParsedText cached = cache.get(raw);
        if (cached != null) {
            hits.incrementAndGet();
            return cached;
        }
        misses.incrementAndGet();
        ParsedText parsed = ColorParser.parse(raw);
        cache.put(raw, parsed);
        return parsed;
    }

    public static ParsedText parseUncached(String raw) {
        return ColorParser.parse(raw);
    }

    public void clear() {
        cache.clear();
        hits.set(0);
        misses.set(0);
    }

    public int size() {
        return cache.size();
    }

    public long hits() {
        return hits.get();
    }

    public long misses() {
        return misses.get();
    }
}
