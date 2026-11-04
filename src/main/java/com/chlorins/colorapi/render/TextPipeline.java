package com.chlorins.colorapi.render;

import com.chlorins.colorapi.cache.LruCache;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;

/**
 * Entry point for the render path: decides whether a string needs rewriting, caches the parsed
 * overlay and hands back a streaming {@link OrderedText}.
 *
 * <p>Cost per frame for already-seen text: one hash lookup plus one small object. No component
 * trees, no string rebuilding, no regex.</p>
 */
public final class TextPipeline {

    private static final LruCache<String, Overlay> OVERLAYS = new LruCache<>(2048);
    private static final LruCache<String, Integer> MARKER_WIDTHS = new LruCache<>(2048);

    /** Guards the per-character draw loop of {@code %wave} against re-entering our own hooks. */
    private static final ThreadLocal<Boolean> IN_WAVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Guards the width measurement used for centring against recursing into the width hook. */
    private static final ThreadLocal<Boolean> IN_WIDTH = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Guards the per-character draw loop (alpha / wave) against re-entering our own hooks. */
    private static final ThreadLocal<Boolean> IN_SEGMENTED = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Nesting depth of text-field rendering; markers must stay visible while editing. */
    private static final ThreadLocal<int[]> TEXT_FIELD_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

    private TextPipeline() {
    }

    /** Fast pre-check; returns false for the vast majority of text drawn in a frame. */
    public static boolean needsRewrite(String raw) {
        return OverlayScanner.hasMarkers(raw);
    }

    public static Overlay overlayOf(String raw) {
        Overlay cached = OVERLAYS.get(raw);
        if (cached != null) {
            return cached;
        }
        Overlay scanned = OverlayScanner.scan(raw);
        if (scanned != null) {
            OVERLAYS.put(raw, scanned);
        }
        return scanned;
    }

    /** @return a lazily styled view, or {@code null} when the text needs no rewrite */
    public static OrderedText wrap(OrderedText original, String raw, long time) {
        Overlay overlay = overlayOf(raw);
        return overlay == null ? null : new ChlorinsOrderedText(original, overlay, time);
    }

    public static long now() {
        return System.currentTimeMillis();
    }

    public static boolean inWave() {
        return IN_WAVE.get();
    }

    static void enterWave() {
        IN_WAVE.set(Boolean.TRUE);
    }

    static void exitWave() {
        IN_WAVE.set(Boolean.FALSE);
    }

    public static boolean inTextField() {
        return TEXT_FIELD_DEPTH.get()[0] > 0;
    }

    public static void enterTextField() {
        TEXT_FIELD_DEPTH.get()[0]++;
    }

    public static void exitTextField() {
        int[] depth = TEXT_FIELD_DEPTH.get();
        if (depth[0] > 0) {
            depth[0]--;
        }
    }

    public static boolean inSegmentedDraw() {
        return IN_SEGMENTED.get();
    }

    static void enterSegmentedDraw() {
        IN_SEGMENTED.set(Boolean.TRUE);
    }

    static void exitSegmentedDraw() {
        IN_SEGMENTED.set(Boolean.FALSE);
    }

    /**
     * Width taken up by marker characters only, so callers can subtract it and get the width of the
     * text the player actually sees.
     *
     * <p>Without this, everything that centres or trims text - {@code /title}, buttons, tooltips,
     * scoreboards, the tab list - is offset by the length of the markers.</p>
     */
    public static int markerWidth(TextRenderer font, String raw) {
        if (!needsRewrite(raw)) {
            return 0;
        }
        Integer cached = MARKER_WIDTHS.get(raw);
        if (cached != null) {
            return cached;
        }
        Overlay overlay = overlayOf(raw);
        if (overlay == null) {
            return 0;
        }
        StringBuilder markers = new StringBuilder(8);
        for (int i = 0; i < raw.length(); i++) {
            if (overlay.isSkipped(i)) {
                markers.append(raw.charAt(i));
            }
        }
        int width;
        if (markers.isEmpty()) {
            width = 0;
        } else if (IN_WIDTH.get()) {
            width = 0;
        } else {
            IN_WIDTH.set(Boolean.TRUE);
            try {
                width = font.getWidth(markers.toString());
            } finally {
                IN_WIDTH.set(Boolean.FALSE);
            }
        }
        MARKER_WIDTHS.put(raw, width);
        return width;
    }

    /** True while we measure marker widths; the width hook must not recurse. */
    public static boolean inWidthComputation() {
        return IN_WIDTH.get();
    }

    public static void clearCache() {
        OVERLAYS.clear();
        MARKER_WIDTHS.clear();
    }

    public static int cacheSize() {
        return OVERLAYS.size();
    }
}
