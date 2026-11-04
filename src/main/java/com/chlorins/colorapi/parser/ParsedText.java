package com.chlorins.colorapi.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of parsing one string: an ordered list of {@link Run}s whose styles are fully resolved.
 *
 * <p>Runs are merged when two neighbours share the exact same {@link TextStyle}, so the result is
 * usually much shorter than the input.</p>
 */
public record ParsedText(List<Run> runs) {

    public static final ParsedText EMPTY = new ParsedText(List.of());

    public ParsedText {
        runs = List.copyOf(runs);
    }

    public boolean isEmpty() {
        return runs.isEmpty();
    }

    /** True when no run carries a colour or decoration, i.e. the text needs no special handling. */
    public boolean isPlain() {
        for (Run run : runs) {
            if (!run.style().isPlain()) {
                return false;
            }
        }
        return true;
    }

    /** The text with every marker removed. */
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (Run run : runs) {
            sb.append(run.text());
        }
        return sb.toString();
    }

    /**
     * Renders this parsed text as a legacy {@code §} string.
     *
     * <p>Only the 16 vanilla colours are expressed as {@code §0}–{@code §f}; RGB colours are emitted
     * as {@code §x§R§R§G§G§B§B}. Note that vanilla clients do <b>not</b> understand {@code §x} — only
     * servers that translate legacy strings into components (Paper, Adventure) do. Prefer
     * {@link #toComponents()} when you need real RGB.</p>
     */
    public String toLegacy() {
        StringBuilder sb = new StringBuilder();
        TextStyle previous = null;
        for (Run run : runs) {
            TextStyle style = run.style();
            if (previous != null && !style.equals(previous)) {
                sb.append('\u00a7').append('r');
                previous = null;
            }
            if (previous == null && !style.isPlain()) {
                if (style.rgb() != null) {
                    sb.append(legacyHex(style.rgb()));
                }
                if (style.bold()) {
                    sb.append('\u00a7').append('l');
                }
                if (style.italic()) {
                    sb.append('\u00a7').append('o');
                }
                if (style.underlined()) {
                    sb.append('\u00a7').append('n');
                }
                if (style.strikethrough()) {
                    sb.append('\u00a7').append('m');
                }
                if (style.obfuscated()) {
                    sb.append('\u00a7').append('k');
                }
            }
            sb.append(run.text());
            previous = style;
        }
        return sb.toString();
    }

    static String legacyHex(int rgb) {
        StringBuilder sb = new StringBuilder(14);
        sb.append('\u00a7').append('x');
        for (int shift = 20; shift >= 0; shift -= 4) {
            sb.append('\u00a7').append(Character.toUpperCase(Character.forDigit((rgb >> shift) & 0xF, 16)));
        }
        return sb.toString();
    }

    /** Convenience: flat list of {@link Run}s (never null). */
    public List<Run> runs() {
        return runs;
    }

    static ParsedText of(List<Run> runs) {
        return runs.isEmpty() ? EMPTY : new ParsedText(new ArrayList<>(runs));
    }
}
