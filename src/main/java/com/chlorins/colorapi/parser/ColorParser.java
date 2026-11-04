package com.chlorins.colorapi.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses Chlorins colour markers into a {@link ParsedText}.
 *
 * <h2>Syntax</h2>
 * <pre>
 *   {@code <prefix>#RRGGBB}                 solid colour until the next marker or {@code %r}
 *   {@code <prefix>#A-B}                    two-stop gradient
 *   {@code <prefix>#A-B-C-D…}               multi-stop gradient (rainbow, fire, …)
 *   {@code %rainbow[text]}                  hue rotates over time
 *   {@code %wave[text]}                     characters bob up and down
 *   {@code %pulse[text]}                    brightness breathes
 *   {@code %r} / {@code %R}                 reset to the default style
 *   {@code %%}                              a literal percent sign
 *   {@code §0}..{@code §f}, {@code §k}..{@code §r}, {@code &a}, {@code &l}, …   vanilla codes
 * </pre>
 *
 * <p>{@code <prefix>} is any character of {@link #DEFAULT_PREFIXES}: {@code % & @ #}. A bare
 * {@code #RRGGBB} is deliberately <b>not</b> a marker.</p>
 *
 * <p>Anything malformed is kept verbatim, so a typo can never crash the game.</p>
 *
 * <p>Pure Java: no Minecraft dependency, fully unit-testable.</p>
 */
public final class ColorParser {

    public static final String DEFAULT_PREFIXES = "%&@#";

    private static final String[] ANIMATION_NAMES = {"rainbow", "wave", "pulse", "flow", "blink", "fade", "bounce", "jitter"};

    /** Vanilla 16 colour palette, index 0-15, as 0xRRGGBB. */
    private static final int[] VANILLA = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
            0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
            0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private static final ColorParser DEFAULT = new ColorParser(DEFAULT_PREFIXES);

    private final boolean[] prefixes = new boolean[128];
    private final String prefixSet;

    public ColorParser(String prefixCharacters) {
        if (prefixCharacters == null || prefixCharacters.isEmpty()) {
            throw new IllegalArgumentException("prefixCharacters must not be empty");
        }
        this.prefixSet = prefixCharacters;
        for (int i = 0; i < prefixCharacters.length(); i++) {
            char c = prefixCharacters.charAt(i);
            if (c < 128) {
                prefixes[c] = true;
            }
        }
    }

    public static ParsedText parse(String input) {
        return DEFAULT.parse0(input);
    }

    public static ParsedText parse(String input, TextStyle initialStyle) {
        return DEFAULT.parse0(input, initialStyle);
    }

    /** @return {@code null} when the input has no marker at all */
    public static ParsedText parseIfMarked(String input) {
        return hasMarkers(input) ? parse(input) : null;
    }

    /** Cheap pre-check so plain strings never reach the parser. */
    public static boolean hasMarkers(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        int n = input.length();
        for (int i = 0; i < n; i++) {
            char c = input.charAt(i);
            if (c == '%' && i + 1 < n) {
                char next = input.charAt(i + 1);
                if (next == '%' || next == 'r' || next == 'R' || animationAt(input, i) != null) {
                    return true;
                }
                if ((next == 'a' || next == 'A') && i + 4 <= n && isHex2(input, i + 2)) {
                    return true;
                }
                if (decoCodeAt(input, i) != 0) {
                    return true;
                }
                if (outlineTagAt(input, i, n) != null) {
                    return true;
                }
            }
            if (c == '\u00a7') {
                return true;
            }
            if (c < 128 && DEFAULT.prefixes[c] && i + 7 < n && input.charAt(i + 1) == '#' && isHex6(input, i + 2)) {
                return true;
            }
        }
        return false;
    }

    /** True when the string contains a {@code %rainbow[} / {@code %wave[} / {@code %pulse[} tag. */
    public static boolean hasAnimation(String input) {
        if (input == null || input.indexOf('%') < 0) {
            return false;
        }
        for (int i = 0, n = input.length() - 1; i < n; i++) {
            if (input.charAt(i) == '%' && animationAt(input, i) != null) {
                return true;
            }
        }
        return false;
    }

    public String prefixCharacters() {
        return prefixSet;
    }

    public ParsedText parse0(String input) {
        return parse0(input, TextStyle.DEFAULT);
    }

    public ParsedText parse0(String input, TextStyle initialStyle) {
        if (input == null || input.isEmpty()) {
            return ParsedText.EMPTY;
        }
        input = LegacyTags.expand(input);
        List<Run> out = new ArrayList<>();
        parseRange(input, 0, input.length(),
                initialStyle == null ? TextStyle.DEFAULT : initialStyle, out);
        return ParsedText.of(out);
    }

    /** Parses {@code input[from, to)} starting from {@code startStyle}. */
    private void parseRange(String input, int from, int to, TextStyle startStyle, List<Run> out) {
        StringBuilder buffer = new StringBuilder();
        TextStyle style = startStyle;
        int i = from;
        while (i < to) {
            char c = input.charAt(i);

            if (c == '%' && i + 1 < to) {
                char next = input.charAt(i + 1);
                if (next == '%') {
                    buffer.append('%');
                    i += 2;
                    continue;
                }
                if ((next == 'a' || next == 'A') && i + 4 <= to && isHex2(input, i + 2)) {
                    // %aRR : alpha, kept out of the component path (Style has no alpha channel)
                    i += 4;
                    continue;
                }
                AnimationTag tag = animationAt(input, i);
                if (tag != null) {
                    flush(buffer, style, out);
                    emitAnimation(input, tag, style, out);
                    i = tag.after;
                    continue;
                }
                ScopedDeco scoped = scopedDecoAt(input, i, to);
                if (scoped != null) {
                    flush(buffer, style, out);
                    parseRange(input, scoped.contentStart, scoped.contentEnd,
                            style.withDecoration(scoped.code, true), out);
                    i = scoped.after;
                    continue;
                }
                int[] outline = outlineTagAt(input, i, to);
                if (outline != null) {
                    flush(buffer, style, out);
                    parseRange(input, outline[0], outline[1], style, out);
                    i = outline[2];
                    continue;
                }
                char decoCode = decoCodeAt(input, i);
                if (decoCode != 0) {
                    flush(buffer, style, out);
                    style = style.withDecoration(decoCode, true);
                    i += 2;
                    continue;
                }
                if (next == 'r' || next == 'R') {
                    flush(buffer, style, out);
                    style = TextStyle.DEFAULT;
                    i += 2;
                    continue;
                }
            }

            Marker marker = markerAt(input, i);
            if (marker != null) {
                flush(buffer, style, out);
                int end = segmentEnd(input, marker.contentStart);
                emitGradient(input, marker.contentStart, end, marker.stops, style, out);
                style = style.withColor(marker.stops[marker.stops.length - 1]);
                i = end;
                continue;
            }

            LegacyCode legacy = legacyAt(input, i);
            if (legacy != null) {
                flush(buffer, style, out);
                style = legacy.apply(style);
                i += legacy.length;
                continue;
            }

            int cp = input.codePointAt(i);
            buffer.appendCodePoint(cp);
            i += Character.charCount(cp);
        }
        flush(buffer, style, out);
    }

    // ------------------------------------------------------------------ scanning

    private Marker markerAt(String s, int i) {
        int n = s.length();
        if (i + 7 >= n) {
            return null;
        }
        char prefix = s.charAt(i);
        if (prefix >= 128 || !prefixes[prefix]) {
            return null;
        }
        if (s.charAt(i + 1) != '#' || !isHex6(s, i + 2)) {
            return null;
        }
        List<Integer> stops = new ArrayList<>(2);
        stops.add(hex(s, i + 2));
        int p = i + 8;
        while (p + 6 < n && s.charAt(p) == '-') {
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
        return new Marker(p, result);
    }

    private int segmentEnd(String s, int start) {
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
                if (next == 'r' || next == 'R' || animationAt(s, i) != null || outlineTagAt(s, i, n) != null) {
                    return i;
                }
            }
            if (markerAt(s, i) != null) {
                return i;
            }
            LegacyCode legacy = legacyAt(s, i);
            if (legacy != null) {
                if (legacy.type == LegacyCode.TYPE_RESET) {
                    return i;
                }
                i += legacy.length;
                continue;
            }
            i += Character.charCount(s.codePointAt(i));
        }
        return n;
    }

    private void emitGradient(String s, int start, int end, int[] stops, TextStyle base, List<Run> out) {
        List<String> chars = new ArrayList<>();
        List<TextStyle> styles = new ArrayList<>();
        TextStyle current = base;
        int i = start;
        while (i < end) {
            char c = s.charAt(i);
            if (c == '%' && i + 1 < end && s.charAt(i + 1) == '%') {
                chars.add("%");
                styles.add(current);
                i += 2;
                continue;
            }
            LegacyCode legacy = legacyAt(s, i);
            if (legacy != null) {
                current = legacy.apply(current);
                i += legacy.length;
                continue;
            }
            char deco = decoCodeAt(s, i);
            if (deco != 0) {
                current = current.withDecoration(deco, true);
                i += 2;
                continue;
            }
            int cp = s.codePointAt(i);
            chars.add(new String(Character.toChars(cp)));
            styles.add(current);
            i += Character.charCount(cp);
        }
        int count = chars.size();
        if (count == 0) {
            return;
        }
        if (stops.length == 1 && isUniform(styles)) {
            // a solid colour stays a single run instead of one run per character
            out.add(new Run(String.join("", chars), styles.get(0).withColor(stops[0])));
            return;
        }
        for (int k = 0; k < count; k++) {
            float t = count <= 1 ? 0.0f : (float) k / (float) (count - 1);
            out.add(new Run(chars.get(k), styles.get(k).withColor(colorAt(stops, t))));
        }
    }

    private static boolean isUniform(List<TextStyle> styles) {
        for (int i = 1; i < styles.size(); i++) {
            if (!styles.get(i).equals(styles.get(0))) {
                return false;
            }
        }
        return true;
    }

    private void emitAnimation(String s, AnimationTag tag, TextStyle base, List<Run> out) {
        List<String> chars = new ArrayList<>();
        List<TextStyle> styles = new ArrayList<>();
        TextStyle current = base;
        int i = tag.contentStart;
        while (i < tag.contentEnd) {
            char c = s.charAt(i);
            if (c == '%' && i + 1 < tag.contentEnd && s.charAt(i + 1) == '%') {
                chars.add("%");
                styles.add(current);
                i += 2;
                continue;
            }
            LegacyCode legacy = legacyAt(s, i);
            if (legacy != null) {
                current = legacy.apply(current);
                i += legacy.length;
                continue;
            }
            char deco = decoCodeAt(s, i);
            if (deco != 0) {
                current = current.withDecoration(deco, true);
                i += 2;
                continue;
            }
            int cp = s.codePointAt(i);
            chars.add(new String(Character.toChars(cp)));
            styles.add(current);
            i += Character.charCount(cp);
        }
        int count = chars.size();
        for (int k = 0; k < count; k++) {
            Animation animation = tag.kind == Animation.Kind.FLOW
                    ? Animation.flow(k, count, tag.stops)
                    : Animation.of(tag.kind, k, count);
            out.add(new Run(chars.get(k), styles.get(k), animation));
        }
    }

    /** {@code %b/%i/%u/%m/%k} and their {@code §}-style aliases {@code %l/%o/%n}. */
    /** @return {contentStart, contentEnd, after} for {@code %outline[..]}, or null */
    static int[] outlineTagAt(String s, int i, int limit) {
        if (!s.regionMatches(true, i, "%outline[", 0, 9)) {
            return null;
        }
        int contentStart = i + 9;
        int close = s.indexOf(']', contentStart);
        if (close < 0 || close > limit) {
            return null;
        }
        return new int[]{contentStart, close, close + 1};
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

    /** {@code %b[text]} - the decoration only applies inside the brackets. */
    static ScopedDeco scopedDecoAt(String s, int i, int limit) {
        if (s.charAt(i) != '%' || i + 2 >= limit || s.charAt(i + 2) != '[') {
            return null;
        }
        char code = decoCodeAt(s, i);
        if (code == 0) {
            return null;
        }
        int contentStart = i + 3;
        int close = s.indexOf(']', contentStart);
        if (close < 0 || close > limit) {
            return null;
        }
        return new ScopedDeco(code, contentStart, close, close + 1);
    }

    static AnimationTag animationAt(String s, int i) {
        if (s.charAt(i) != '%') {
            return null;
        }
        for (String name : ANIMATION_NAMES) {
            int open = i + 1 + name.length();
            if (open >= s.length() || s.charAt(open) != '['
                    || !s.regionMatches(true, i + 1, name, 0, name.length())) {
                continue;
            }
            Animation.Kind kind = Animation.Kind.valueOf(name.toUpperCase());
            int contentStart = open + 1;
            int[] stops = null;
            if (kind == Animation.Kind.FLOW && contentStart < s.length() && s.charAt(contentStart) == '#'
                    && isHex6(s, contentStart + 1)) {
                List<Integer> ramp = new ArrayList<>();
                ramp.add(hex(s, contentStart + 1));
                int p = contentStart + 7;
                while (p + 6 < s.length() && s.charAt(p) == '-') {
                    int q = p + 1;
                    if (s.charAt(q) == '#') {
                        q++;
                    }
                    if (!isHex6(s, q)) {
                        break;
                    }
                    ramp.add(hex(s, q));
                    p = q + 6;
                }
                if (p < s.length() && s.charAt(p) == '|') {
                    stops = new int[ramp.size()];
                    for (int k = 0; k < stops.length; k++) {
                        stops[k] = ramp.get(k);
                    }
                    contentStart = p + 1;
                }
            }
            int close = s.indexOf(']', contentStart);
            if (close <= contentStart) {
                return null; // unterminated -> treat as literal
            }
            return new AnimationTag(kind, contentStart, close, close + 1, stops);
        }
        return null;
    }

    // ------------------------------------------------------------------ helpers

    static void flush(StringBuilder buffer, TextStyle style, List<Run> out) {
        if (buffer.isEmpty()) {
            return;
        }
        String text = buffer.toString();
        buffer.setLength(0);
        if (!out.isEmpty() && !out.get(out.size() - 1).isAnimated()
                && out.get(out.size() - 1).style().equals(style)) {
            Run last = out.remove(out.size() - 1);
            out.add(new Run(last.text() + text, style, last.animation()));
        } else {
            out.add(new Run(text, style));
        }
    }

    static LegacyCode legacyAt(String s, int i) {
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
            return LegacyCode.color(rgb, 14);
        }
        if (code >= '0' && code <= '9') {
            return LegacyCode.color(VANILLA[code - '0'], 2);
        }
        if (code >= 'a' && code <= 'f') {
            return LegacyCode.color(VANILLA[code - 'a' + 10], 2);
        }
        if (code >= 'k' && code <= 'o') {
            return LegacyCode.decoration(code, 2);
        }
        if (code == 'r') {
            return LegacyCode.reset(2);
        }
        return null;
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

    static boolean isHex2(String s, int from) {
        return from + 2 <= s.length()
                && Character.digit(s.charAt(from), 16) >= 0
                && Character.digit(s.charAt(from + 1), 16) >= 0;
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

    static int hex(String s, int from) {
        int rgb = 0;
        for (int i = from; i < from + 6; i++) {
            rgb = (rgb << 4) | Character.digit(s.charAt(i), 16);
        }
        return rgb;
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

    record Marker(int contentStart, int[] stops) {
    }

    record AnimationTag(Animation.Kind kind, int contentStart, int contentEnd, int after, int[] stops) {
    }

    record ScopedDeco(char code, int contentStart, int contentEnd, int after) {
    }
}
