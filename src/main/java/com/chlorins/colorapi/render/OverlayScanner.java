package com.chlorins.colorapi.render;

import com.chlorins.colorapi.parser.Animation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Turns a raw string into an {@link Overlay}: which characters to skip, what colour each visible
 * character gets, which decorations apply, what alpha it uses and whether it animates.
 *
 * <p>It also understands the vanilla {@code §}/{@code &} codes. That matters because a {@code Text}
 * component never parses them - vanilla only does that for plain strings - so {@code §k} inside an
 * item name would otherwise be printed literally instead of scrambling.</p>
 */
public final class OverlayScanner {

    private static final int[] VANILLA = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private static final String[] ANIMATIONS = {"rainbow", "wave", "pulse", "flow", "blink", "fade", "bounce", "jitter"};

    private OverlayScanner() {
    }

    /** @return the overlay, or {@code null} when the string contains nothing to rewrite */
    public static Overlay scan(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        int n = source.length();
        int[] color = new int[n];
        Arrays.fill(color, Overlay.NO_COLOR);
        boolean[] skip = new boolean[n];
        Animation[] animation = new Animation[n];
        int[] waveIndex = new int[n];
        byte[] decoOn = new byte[n];
        byte[] decoOff = new byte[n];
        int[] alpha = new int[n];
        Arrays.fill(alpha, Overlay.ALPHA_OPAQUE);
        boolean[] outline = new boolean[n];

        State state = new State();
        java.util.ArrayDeque<Scope> scopes = new java.util.ArrayDeque<>();
        boolean changed = false;
        boolean hasAnimation = false;
        boolean hasWave = false;
        boolean hasAlpha = false;
        boolean hasDecorations = false;
        boolean hasOutline = false;
        boolean hasPosition = false;
        boolean hasAnimatedAlpha = false;

        int i = 0;
        while (i < n) {
            while (!scopes.isEmpty() && i >= scopes.peek().end) {
                state.restore(scopes.pop().saved);
            }
            char c = source.charAt(i);
            if (c == '%' && i + 1 < n) {
                char next = source.charAt(i + 1);
                if (next == '%') {
                    skip[i + 1] = true;
                    changed = true;
                    i += 2;
                    continue;
                }
                if ((next == 'a' || next == 'A') && i + 4 <= n && isHex2(source, i + 2)) {
                    state.alpha = (Character.digit(source.charAt(i + 2), 16) << 4)
                            | Character.digit(source.charAt(i + 3), 16);
                    if (state.alpha == 0) {
                        state.alpha = 1; // vanilla forces alpha 0 to opaque, so keep it invisible-ish
                    }
                    markSkipped(skip, i, i + 4);
                    changed = true;
                    hasAlpha = true;
                    i += 4;
                    continue;
                }
                Tag tag = animationAt(source, i);
                if (tag != null) {
                    markSkipped(skip, i, tag.contentStart);
                    skip[tag.contentEnd] = true;
                    boolean wave = applyAnimation(source, tag, animation, waveIndex, decoOn, decoOff, alpha, state);
                    hasWave |= wave;
                    hasPosition |= tag.kind.isPosition();
                    hasAnimatedAlpha |= tag.kind.isAlpha();
                    hasAnimation = true;
                    changed = true;
                    i = tag.after;
                    continue;
                }
                if (next == 'r' || next == 'R') {
                    markSkipped(skip, i, i + 2);
                    state.reset();
                    changed = true;
                    i += 2;
                    continue;
                }
                int[] outlineTag = outlineAt(source, i);
                if (outlineTag != null) {
                    markSkipped(skip, i, outlineTag[0]);
                    skip[outlineTag[1]] = true;
                    applyOutline(source, outlineTag[0], outlineTag[1], state, color, decoOn, decoOff, alpha, outline);
                    changed = true;
                    hasOutline = true;
                    i = outlineTag[2];
                    continue;
                }
                char decoCode = decoCodeAt(source, i);
                if (decoCode != 0) {
                    if (i + 2 < n && source.charAt(i + 2) == '[') {
                        int close = source.indexOf(']', i + 3);
                        if (close > 0) {
                            markSkipped(skip, i, i + 3);
                            skip[close] = true;
                            scopes.push(new Scope(close + 1, state.copy()));
                            state.setDeco(decoBit(decoCode));
                            changed = true;
                            hasDecorations = true;
                            i += 3;
                            continue;
                        }
                    }
                    markSkipped(skip, i, i + 2);
                    state.setDeco(decoBit(decoCode));
                    changed = true;
                    hasDecorations = true;
                    i += 2;
                    continue;
                }
            }
            Legacy legacy = legacyAt(source, i);
            if (legacy != null) {
                markSkipped(skip, i, i + legacy.length);
                state.apply(legacy);
                changed = true;
                hasDecorations = true;
                i += legacy.length;
                continue;
            }
            int[] stops = markerStops(source, i);
            if (stops != null) {
                int start = markerContentStart(source, i);
                int end = segmentEnd(source, start);
                markSkipped(skip, i, start);
                applySegment(source, start, end, stops, state, color, decoOn, decoOff, alpha);
                state.color = stops[stops.length - 1];
                changed = true;
                i = end;
                continue;
            }
            color[i] = state.color == null ? Overlay.NO_COLOR : state.color;
            decoOn[i] = state.decoOn;
            decoOff[i] = state.decoOff;
            alpha[i] = state.alpha;
            i++;
        }
        if (!changed) {
            return null;
        }
        return new Overlay(color, skip, animation, waveIndex, decoOn, decoOff, alpha, outline,
                hasAnimation, hasWave, hasAlpha, hasDecorations, hasOutline, hasPosition, hasAnimatedAlpha);
    }

    /** Cheap pre-check so plain strings never touch the cache. */
    public static boolean hasMarkers(String source) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        int n = source.length();
        for (int i = 0; i < n; i++) {
            char c = source.charAt(i);
            if (c == '%' && i + 1 < n) {
                char next = source.charAt(i + 1);
                if (next == '%' || next == 'r' || next == 'R') {
                    return true;
                }
                if ((next == 'a' || next == 'A') && i + 4 <= n && isHex2(source, i + 2)) {
                    return true;
                }
                if (animationAt(source, i) != null) {
                    return true;
                }
                if (decoCodeAt(source, i) != 0) {
                    return true;
                }
                if (outlineAt(source, i) != null) {
                    return true;
                }
            }
            if (c == '\u00a7' || (c == '&' && legacyAt(source, i) != null)) {
                return true;
            }
            if (i + 7 < n && isPrefix(source, i) && source.charAt(i + 1) == '#' && isHex6(source, i + 2)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ state

    private static final class State {
        byte decoOn;
        byte decoOff;
        int alpha = Overlay.ALPHA_OPAQUE;
        Integer color;

        void reset() {
            decoOn = 0;
            decoOff = Overlay.ALL_DECORATIONS;
            alpha = Overlay.ALPHA_OPAQUE;
            color = null;
        }

        State copy() {
            State copy = new State();
            copy.decoOn = decoOn;
            copy.decoOff = decoOff;
            copy.alpha = alpha;
            copy.color = color;
            return copy;
        }

        void restore(State other) {
            decoOn = other.decoOn;
            decoOff = other.decoOff;
            alpha = other.alpha;
            color = other.color;
        }

        void setDeco(byte bit) {
            decoOn |= bit;
            decoOff &= (byte) ~bit;
        }

        void apply(Legacy legacy) {
            switch (legacy.type) {
                case 'c' -> color = legacy.rgb;
                case 'r' -> reset();
                default -> {
                    byte bit = (byte) legacy.bit;
                    if ((decoOn & bit) == 0) {
                        decoOn |= bit;
                        decoOff &= (byte) ~bit;
                    }
                }
            }
        }
    }

    private record Legacy(char type, char bit, int rgb, int length) {
    }

    private record Scope(int end, State saved) {
    }

    static char decoCodeAt(String s, int i) {
        if (s.charAt(i) != '%' || i + 1 >= s.length()) {
            return 0;
        }
        return switch (Character.toLowerCase(s.charAt(i + 1))) {
            case 'b', 'l' -> 'l';
            case 'i', 'o' -> 'o';
            case 'u', 'n' -> 'n';
            case 'm' -> 'm';
            case 'k' -> 'k';
            default -> 0;
        };
    }

    static byte decoBit(char code) {
        return switch (code) {
            case 'l' -> Overlay.BOLD;
            case 'o' -> Overlay.ITALIC;
            case 'n' -> Overlay.UNDERLINE;
            case 'm' -> Overlay.STRIKETHROUGH;
            default -> Overlay.OBFUSCATED;
        };
    }

    /** @return the legacy code starting at {@code i}, or {@code null} */
    static Legacy legacyAt(String s, int i) {
        int n = s.length();
        char c = s.charAt(i);
        if ((c != '\u00a7' && c != '&') || i + 1 >= n) {
            return null;
        }
        char code = Character.toLowerCase(s.charAt(i + 1));
        if (code == 'x') {
            if (i + 14 > n) {
                return null;
            }
            int rgb = 0;
            for (int k = 0; k < 6; k++) {
                char section = s.charAt(i + 2 + k * 2);
                if (section != '\u00a7' && section != '&') {
                    return null;
                }
                int digit = Character.digit(s.charAt(i + 3 + k * 2), 16);
                if (digit < 0) {
                    return null;
                }
                rgb = (rgb << 4) | digit;
            }
            return new Legacy('c', (char) 0, rgb, 14);
        }
        if (code >= '0' && code <= '9') {
            return new Legacy('c', (char) 0, VANILLA[code - '0'], 2);
        }
        if (code >= 'a' && code <= 'f') {
            return new Legacy('c', (char) 0, VANILLA[code - 'a' + 10], 2);
        }
        byte bit = switch (code) {
            case 'k' -> Overlay.OBFUSCATED;
            case 'l' -> Overlay.BOLD;
            case 'm' -> Overlay.STRIKETHROUGH;
            case 'n' -> Overlay.UNDERLINE;
            case 'o' -> Overlay.ITALIC;
            default -> 0;
        };
        if (bit != 0) {
            return new Legacy((char) 'd', (char) bit, 0, 2);
        }
        if (code == 'r') {
            return new Legacy('r', (char) 0, 0, 2);
        }
        return null;
    }

    // ------------------------------------------------------------------ segments

    private static void applySegment(String s, int start, int end, int[] stops, State state,
                                     int[] color, byte[] decoOn, byte[] decoOff, int[] alpha) {
        int count = 0;
        for (int p = start; p < end; ) {
            char c = s.charAt(p);
            if (c == '%' && p + 1 < end && s.charAt(p + 1) == '%') {
                count++;
                p += 2;
                continue;
            }
            Legacy legacy = legacyAt(s, p);
            if (legacy != null) {
                p += legacy.length;
                continue;
            }
            if (decoCodeAt(s, p) != 0) {
                p += 2;
                continue;
            }
            count++;
            p += Character.charCount(s.codePointAt(p));
        }
        int k = 0;
        for (int p = start; p < end; ) {
            char c = s.charAt(p);
            if (c == '%' && p + 1 < end && s.charAt(p + 1) == '%') {
                color[p] = colorAt(stops, ratio(k, count));
                decoOn[p] = state.decoOn;
                decoOff[p] = state.decoOff;
                alpha[p] = state.alpha;
                k++;
                p += 2;
                continue;
            }
            Legacy legacy = legacyAt(s, p);
            if (legacy != null) {
                state.apply(legacy);
                p += legacy.length;
                continue;
            }
            char deco = decoCodeAt(s, p);
            if (deco != 0) {
                state.setDeco(decoBit(deco));
                p += 2;
                continue;
            }
            int cp = s.codePointAt(p);
            color[p] = colorAt(stops, ratio(k, count));
            decoOn[p] = state.decoOn;
            decoOff[p] = state.decoOff;
            alpha[p] = state.alpha;
            k++;
            p += Character.charCount(cp);
        }
    }

    /** @return {contentStart, contentEnd, after} for {@code %outline[..]}, or null */
    static int[] outlineAt(String s, int i) {
        if (s.charAt(i) != '%' || !s.regionMatches(true, i, "%outline[", 0, 9)) {
            return null;
        }
        int contentStart = i + 9;
        int close = s.indexOf(']', contentStart);
        if (close < 0) {
            return null;
        }
        return new int[]{contentStart, close, close + 1};
    }

    private static void applyOutline(String s, int start, int end, State state, int[] color,
                                     byte[] decoOn, byte[] decoOff, int[] alpha, boolean[] outline) {
        for (int p = start; p < end; ) {
            char c = s.charAt(p);
            if (c == '%' && p + 1 < end && s.charAt(p + 1) == '%') {
                color[p] = state.color == null ? Overlay.NO_COLOR : state.color;
                decoOn[p] = state.decoOn;
                decoOff[p] = state.decoOff;
                alpha[p] = state.alpha;
                outline[p] = true;
                p += 2;
                continue;
            }
            Legacy legacy = legacyAt(s, p);
            if (legacy != null) {
                state.apply(legacy);
                p += legacy.length;
                continue;
            }
            char deco = decoCodeAt(s, p);
            if (deco != 0) {
                state.setDeco(decoBit(deco));
                p += 2;
                continue;
            }
            color[p] = state.color == null ? Overlay.NO_COLOR : state.color;
            decoOn[p] = state.decoOn;
            decoOff[p] = state.decoOff;
            alpha[p] = state.alpha;
            outline[p] = true;
            p += Character.charCount(s.codePointAt(p));
        }
    }

    private static boolean applyAnimation(String s, Tag tag, Animation[] animation, int[] waveIndex,
                                          byte[] decoOn, byte[] decoOff, int[] alpha, State state) {
        List<Integer> visible = new ArrayList<>();
        for (int p = tag.contentStart; p < tag.contentEnd; ) {
            char c = s.charAt(p);
            if (c == '%' && p + 1 < tag.contentEnd && s.charAt(p + 1) == '%') {
                visible.add(p);
                p += 2;
                continue;
            }
            Legacy legacy = legacyAt(s, p);
            if (legacy != null) {
                state.apply(legacy);
                p += legacy.length;
                continue;
            }
            char deco = decoCodeAt(s, p);
            if (deco != 0) {
                state.setDeco(decoBit(deco));
                p += 2;
                continue;
            }
            visible.add(p);
            p += Character.charCount(s.codePointAt(p));
        }
        int count = visible.size();
        boolean wave = tag.kind == Animation.Kind.WAVE;
        for (int k = 0; k < count; k++) {
            int p = visible.get(k);
            animation[p] = tag.kind == Animation.Kind.FLOW
                    ? Animation.flow(k, count, tag.stops)
                    : Animation.of(tag.kind, k, count);
            waveIndex[p] = k;
            decoOn[p] = state.decoOn;
            decoOff[p] = state.decoOff;
            alpha[p] = state.alpha;
        }
        return wave;
    }

    // ------------------------------------------------------------------ tags

    private record Tag(Animation.Kind kind, int contentStart, int contentEnd, int after, int[] stops) {
    }

    private static Tag animationAt(String s, int i) {
        if (s.charAt(i) != '%') {
            return null;
        }
        for (String name : ANIMATIONS) {
            int open = i + 1 + name.length();
            if (open >= s.length() || s.charAt(open) != '['
                    || !s.regionMatches(true, i + 1, name, 0, name.length())) {
                continue;
            }
            Animation.Kind kind = Animation.Kind.valueOf(name.toUpperCase());
            int contentStart = open + 1;
            int[] stops = null;
            if (kind == Animation.Kind.FLOW && contentStart < s.length() && s.charAt(contentStart) == '#') {
                int[] parsed = markerStops(s, contentStart);
                if (parsed != null) {
                    int afterStops = markerContentStart(s, contentStart);
                    if (afterStops < s.length() && s.charAt(afterStops) == '|') {
                        stops = parsed;
                        contentStart = afterStops + 1;
                    }
                }
            }
            int close = s.indexOf(']', contentStart);
            if (close < 0) {
                return null;
            }
            return new Tag(kind, contentStart, close, close + 1, stops);
        }
        return null;
    }

    private static float ratio(int k, int count) {
        return count <= 1 ? 0.0f : (float) k / (float) (count - 1);
    }

    // ------------------------------------------------------------------ helpers

    private static boolean isPrefix(String s, int i) {
        char c = s.charAt(i);
        return c == '%' || c == '&' || c == '@' || c == '#';
    }

    public static int[] markerStops(String s, int i) {
        if (i + 7 >= s.length() || !isPrefix(s, i) || s.charAt(i + 1) != '#' || !isHex6(s, i + 2)) {
            return null;
        }
        List<Integer> stops = new ArrayList<>(2);
        stops.add(hex(s, i + 2));
        int p = i + 8;
        while (p + 6 < s.length() && s.charAt(p) == '-') {
            int q = p + 1;
            if (s.charAt(q) == '#') {
                q++;
            }
            if (!isHex6(s, q)) {
                break;
            }
            stops.add(hex(s, q));
            p = q + 6;
        }
        int[] result = new int[stops.size()];
        for (int k = 0; k < result.length; k++) {
            result[k] = stops.get(k);
        }
        return result;
    }

    public static int markerContentStart(String s, int i) {
        int p = i + 8;
        while (p + 6 < s.length() && s.charAt(p) == '-') {
            int q = p + 1;
            if (s.charAt(q) == '#') {
                q++;
            }
            if (!isHex6(s, q)) {
                break;
            }
            p = q + 6;
        }
        return p;
    }

    private static int segmentEnd(String s, int start) {
        int n = s.length();
        int i = start;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '%' && i + 1 < n) {
                char next = s.charAt(i + 1);
                if (next == '%') {
                    i += 2;
                    continue;
                }
                if (next == 'r' || next == 'R' || animationAt(s, i) != null || outlineAt(s, i) != null) {
                    return i;
                }
            }
            Legacy legacy = legacyAt(s, i);
            if (legacy != null) {
                if (legacy.type == 'r') {
                    return i;
                }
                i += legacy.length;
                continue;
            }
            if (markerStops(s, i) != null) {
                return i;
            }
            i += Character.charCount(s.codePointAt(i));
        }
        return n;
    }

    private static void markSkipped(boolean[] skip, int fromInclusive, int toExclusive) {
        for (int p = fromInclusive; p < toExclusive && p < skip.length; p++) {
            skip[p] = true;
        }
    }

    static int colorAt(int[] stops, float t) {
        if (stops.length == 1) {
            return stops[0];
        }
        float scaled = Math.max(0.0f, Math.min(1.0f, t)) * (stops.length - 1);
        int segment = Math.min((int) scaled, stops.length - 2);
        float local = scaled - segment;
        return lerp(stops[segment], stops[segment + 1], local);
    }

    static int lerp(int from, int to, float t) {
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    static boolean isHex6(String s, int from) {
        if (from + 6 > s.length()) {
            return false;
        }
        for (int i = from; i < from + 6; i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    static boolean isHex2(String s, int from) {
        if (from + 2 > s.length()) {
            return false;
        }
        return Character.digit(s.charAt(from), 16) >= 0 && Character.digit(s.charAt(from + 1), 16) >= 0;
    }

    static int hex(String s, int from) {
        int rgb = 0;
        for (int i = from; i < from + 6; i++) {
            rgb = (rgb << 4) | Character.digit(s.charAt(i), 16);
        }
        return rgb;
    }
}
