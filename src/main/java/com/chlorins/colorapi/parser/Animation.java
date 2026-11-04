package com.chlorins.colorapi.parser;

/**
 * Animation attached to a run of characters.
 *
 * <p>{@code index} is the position inside the animated segment and {@code count} the segment length,
 * so each character gets its own phase. {@code stops} is only used by {@link Kind#FLOW} (the colour
 * ramp that scrolls along the text).</p>
 */
public record Animation(Kind kind, int index, int count, int[] stops) {

    public enum Kind {
        NONE,
        RAINBOW,
        WAVE,
        PULSE,
        FLOW,
        BLINK,
        FADE,
        BOUNCE,
        JITTER;

        public boolean isPosition() {
            return this == WAVE || this == BOUNCE || this == JITTER;
        }

        public boolean isAlpha() {
            return this == FADE;
        }
    }

    public static final Animation NONE = new Animation(Kind.NONE, 0, 1, null);

    public Animation(Kind kind, int index, int count) {
        this(kind, index, count, null);
    }

    public static Animation of(Kind kind, int index, int count) {
        return kind == Kind.NONE ? NONE : new Animation(kind, index, Math.max(1, count), null);
    }

    public static Animation flow(int index, int count, int[] stops) {
        return new Animation(Kind.FLOW, index, Math.max(1, count),
                stops == null || stops.length == 0 ? DEFAULT_FLOW : stops);
    }

    /** Red -> yellow -> green -> cyan -> blue -> magenta -> red. */
    public static final int[] DEFAULT_FLOW = {
            0xFF0000, 0xFFFF00, 0x00FF00, 0x00FFFF, 0x0000FF, 0xFF00FF, 0xFF0000
    };

    public boolean isAnimated() {
        return kind != Kind.NONE;
    }

    public boolean isWave() {
        return kind == Kind.WAVE;
    }

    /** Animations that move glyphs instead of recolouring them. */
    public boolean isPosition() {
        return kind == Kind.WAVE || kind == Kind.BOUNCE || kind == Kind.JITTER;
    }

    /** Animations that change the alpha channel. */
    public boolean isAlpha() {
        return kind == Kind.FADE;
    }

    /** Animations that only change the colour. */
    public boolean isColor() {
        return kind == Kind.RAINBOW || kind == Kind.PULSE || kind == Kind.FLOW || kind == Kind.BLINK;
    }
}
