package com.chlorins.colorapi.render;

import com.chlorins.colorapi.parser.Animation;

/**
 * Per-character styling instructions for one source string, aligned 1:1 with its characters
 * (marker characters are flagged as {@link #isSkipped(int) skipped}).
 */
public final class Overlay {

    public static final int NO_COLOR = -1;
    public static final int ALPHA_OPAQUE = 255;

    public static final byte BOLD = 1;
    public static final byte ITALIC = 2;
    public static final byte UNDERLINE = 4;
    public static final byte STRIKETHROUGH = 8;
    public static final byte OBFUSCATED = 16;
    public static final byte ALL_DECORATIONS = BOLD | ITALIC | UNDERLINE | STRIKETHROUGH | OBFUSCATED;

    private final int[] color;
    private final boolean[] skip;
    private final Animation[] animation;
    private final int[] waveIndex;
    private final byte[] decoOn;
    private final byte[] decoOff;
    private final int[] alpha;
    private final boolean[] outline;
    private final boolean hasAnimation;
    private final boolean hasWave;
    private final boolean hasAlpha;
    private final boolean hasDecorations;
    private final boolean hasOutline;
    private final boolean hasPosition;
    private final boolean hasAnimatedAlpha;
    private final int uniformAlpha;

    Overlay(int[] color, boolean[] skip, Animation[] animation, int[] waveIndex, byte[] decoOn,
            byte[] decoOff, int[] alpha, boolean[] outline, boolean hasAnimation, boolean hasWave,
            boolean hasAlpha, boolean hasDecorations, boolean hasOutline,
            boolean hasPosition, boolean hasAnimatedAlpha) {
        this.color = color;
        this.skip = skip;
        this.animation = animation;
        this.waveIndex = waveIndex;
        this.decoOn = decoOn;
        this.decoOff = decoOff;
        this.alpha = alpha;
        this.outline = outline;
        this.hasAnimation = hasAnimation;
        this.hasWave = hasWave;
        this.hasAlpha = hasAlpha;
        this.hasDecorations = hasDecorations;
        this.hasOutline = hasOutline;
        this.hasPosition = hasPosition;
        this.hasAnimatedAlpha = hasAnimatedAlpha;
        this.uniformAlpha = computeUniformAlpha(alpha, skip);
    }

    /** @return the single alpha value shared by every visible character, or -1 when mixed */
    private static int computeUniformAlpha(int[] alpha, boolean[] skip) {
        int value = -1;
        for (int i = 0; i < alpha.length; i++) {
            if (skip[i]) {
                continue;
            }
            if (value < 0) {
                value = alpha[i];
            } else if (value != alpha[i]) {
                return -1;
            }
        }
        return value < 0 ? ALPHA_OPAQUE : value;
    }

    public int uniformAlpha() {
        return uniformAlpha;
    }

    public int length() {
        return color.length;
    }

    public boolean isSkipped(int index) {
        return index >= 0 && index < skip.length && skip[index];
    }

    public int color(int index) {
        return index >= 0 && index < color.length ? color[index] : NO_COLOR;
    }

    public Animation animation(int index) {
        return index >= 0 && index < animation.length ? animation[index] : null;
    }

    public boolean isWave(int index) {
        Animation a = animation(index);
        return a != null && a.isWave();
    }

    public int waveIndex(int index) {
        return index >= 0 && index < waveIndex.length ? waveIndex[index] : 0;
    }

    public byte decorationsOn(int index) {
        return index >= 0 && index < decoOn.length ? decoOn[index] : 0;
    }

    public byte decorationsOff(int index) {
        return index >= 0 && index < decoOff.length ? decoOff[index] : 0;
    }

    public int alpha(int index) {
        return index >= 0 && index < alpha.length ? alpha[index] : ALPHA_OPAQUE;
    }

    public boolean hasAnimation() {
        return hasAnimation;
    }

    public boolean hasWave() {
        return hasWave;
    }

    public boolean hasAlpha() {
        return hasAlpha;
    }

    public boolean hasDecorations() {
        return hasDecorations;
    }

    /** True when this character should be drawn with an outline in its own (animated) colour. */
    public boolean isOutlined(int index) {
        return index >= 0 && index < outline.length && outline[index];
    }

    public boolean hasOutline() {
        return hasOutline;
    }

    /** True when some characters move ({@code %wave}, {@code %bounce}, {@code %jitter}). */
    public boolean hasPosition() {
        return hasPosition;
    }

    /** True when some characters animate their alpha ({@code %fade}). */
    public boolean hasAnimatedAlpha() {
        return hasAnimatedAlpha;
    }
}
