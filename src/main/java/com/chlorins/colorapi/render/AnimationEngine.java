package com.chlorins.colorapi.render;

import com.chlorins.colorapi.parser.Animation;

/**
 * Pure-Java animation maths. No Minecraft dependency, therefore unit-testable.
 *
 * <p>All effects are driven by an absolute time in milliseconds so that every element of a frame
 * stays in sync and nothing needs to be ticked.</p>
 */
public final class AnimationEngine {

    /** Degrees per millisecond for the rainbow hue rotation. */
    public static final float RAINBOW_SPEED = 0.09f;
    /** Degrees of hue difference between two neighbouring characters. */
    public static final float RAINBOW_STEP = 24.0f;

    /** One full breathing cycle. */
    public static final long PULSE_PERIOD_MS = 1600L;
    public static final float PULSE_MIN = 0.45f;
    public static final float PULSE_MAX = 1.00f;

    /** Vertical amplitude of the wave, in pixels. */
    public static final float WAVE_AMPLITUDE = 2.5f;
    /** Phase difference between two neighbouring characters. */
    public static final float WAVE_STEP = 0.7f;
    /** Radians per millisecond. */
    public static final float WAVE_SPEED = 0.008f;

    private AnimationEngine() {
    }

    /** How far the flow ramp scrolls per millisecond (1.0 = one full ramp per second). */
    public static final float FLOW_SPEED = 0.00035f;

    public static int colorOf(Animation animation, int baseRgb, long timeMs) {
        return switch (animation.kind()) {
            case RAINBOW -> rainbow(animation.index(), timeMs);
            case PULSE -> pulse(animation.index(), animation.count(), timeMs, baseRgb);
            case FLOW -> flow(animation.index(), animation.count(), timeMs, animation.stops());
            case BLINK -> blink(animation.index(), timeMs);
            default -> baseRgb;
        };
    }

    /**
     * Scrolling gradient: the ramp is laid out over the segment and the whole thing slides with
     * time, which is what makes the colours "flow" through the text.
     */
    public static int flow(int index, int count, long timeMs, int[] stops) {
        int[] ramp = stops == null || stops.length < 2 ? Animation.DEFAULT_FLOW : stops;
        float position = (float) index / Math.max(1, count) - timeMs * FLOW_SPEED;
        position = position - (float) Math.floor(position);
        float scaled = position * (ramp.length - 1);
        int segment = Math.min((int) scaled, ramp.length - 2);
        float local = scaled - segment;
        return lerp(ramp[segment], ramp[segment + 1], local);
    }

    static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int rainbow(int index, long timeMs) {
        float hue = (timeMs * RAINBOW_SPEED + index * RAINBOW_STEP) % 360.0f;
        if (hue < 0) {
            hue += 360.0f;
        }
        return hsvToRgb(hue, 1.0f, 1.0f);
    }

    public static int pulse(int index, int count, long timeMs, int baseRgb) {
        double phase = (timeMs % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        float wave = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
        float factor = PULSE_MIN + (PULSE_MAX - PULSE_MIN) * wave;
        int rgb = baseRgb >= 0 ? baseRgb : 0xFFFFFF;
        return scaleRgb(rgb, factor);
    }

    /** Hard on/off blink, roughly 2.5 times per second. */
    public static final long BLINK_PERIOD_MS = 400L;

    public static int blink(int index, long timeMs) {
        boolean on = ((timeMs + index * 40L) / (BLINK_PERIOD_MS / 2)) % 2 == 0;
        return on ? 0xFFFFFF : 0x303030;
    }

    /** Alpha multiplier for {@code %fade}, 0.25 .. 1.0. */
    public static float fadeFactor(long timeMs) {
        double phase = (timeMs % PULSE_PERIOD_MS) / (double) PULSE_PERIOD_MS;
        float wave = (float) (0.5 - 0.5 * Math.cos(phase * Math.PI * 2.0));
        return 0.25f + 0.75f * wave;
    }

    public static float bounce(int index, long timeMs) {
        double t = timeMs * 0.005 + index * 0.45;
        return -(float) Math.abs(Math.sin(t)) * 3.5f;
    }

    /** Deterministic per-character jitter; nothing is stored, it is a hash of (index, time bucket). */
    public static float jitterX(int index, long timeMs) {
        return hash01(index, timeMs, 0) * 2.0f - 1.0f;
    }

    public static float jitterY(int index, long timeMs) {
        return hash01(index, timeMs, 1) * 2.0f - 1.0f;
    }

    private static float hash01(int index, long timeMs, int salt) {
        long bucket = timeMs / 50L;
        long h = index * 73856093L ^ bucket * 19349663L ^ (salt * 83492791L);
        h ^= (h >>> 33);
        h *= 0xff51afd7ed558ccdL;
        h ^= (h >>> 33);
        return (h & 0xFFFF) / 65535.0f;
    }

    /** Vertical/horizontal offset of a glyph for the position-based animations. */
    public static float offsetX(Animation animation, int index, long timeMs) {
        return animation.isPosition() && animation.kind() == Animation.Kind.JITTER
                ? jitterX(index, timeMs) : 0.0f;
    }

    public static float offsetY(Animation animation, int index, long timeMs) {
        return switch (animation.kind()) {
            case WAVE -> wave(index, timeMs);
            case BOUNCE -> bounce(index, timeMs);
            case JITTER -> jitterY(index, timeMs);
            default -> 0.0f;
        };
    }

    public static float wave(int index, long timeMs) {
        return (float) Math.sin(index * WAVE_STEP - timeMs * WAVE_SPEED) * WAVE_AMPLITUDE;
    }

    public static int scaleRgb(int rgb, float factor) {
        int r = clamp(Math.round(((rgb >> 16) & 0xFF) * factor));
        int g = clamp(Math.round(((rgb >> 8) & 0xFF) * factor));
        int b = clamp(Math.round((rgb & 0xFF) * factor));
        return (r << 16) | (g << 8) | b;
    }

    public static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float x = c * (1.0f - Math.abs((h / 60.0f) % 2.0f - 1.0f));
        float m = v - c;
        float r;
        float g;
        float b;
        int sector = (int) (h / 60.0f) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (clamp(Math.round((r + m) * 255.0f)) << 16)
                | (clamp(Math.round((g + m) * 255.0f)) << 8)
                | clamp(Math.round((b + m) * 255.0f));
    }

    private static int clamp(int value) {
        return value < 0 ? 0 : Math.min(value, 255);
    }
}
