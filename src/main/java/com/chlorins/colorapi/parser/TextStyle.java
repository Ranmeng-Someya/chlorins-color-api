package com.chlorins.colorapi.parser;

/**
 * An immutable snapshot of the formatting state that applies to a piece of text.
 *
 * <p>{@code rgb == null} means "no explicit colour" (inherit / default), which is what
 * {@code %r} resets back to.</p>
 */
public record TextStyle(Integer rgb, boolean bold, boolean italic, boolean underlined, boolean strikethrough,
                        boolean obfuscated) {

    public static final TextStyle DEFAULT = new TextStyle(null, false, false, false, false, false);

    public TextStyle withColor(Integer color) {
        return new TextStyle(color, bold, italic, underlined, strikethrough, obfuscated);
    }

    public TextStyle withColor(int color) {
        return withColor(Integer.valueOf(color));
    }

    public TextStyle withDecoration(char code, boolean on) {
        return switch (Character.toLowerCase(code)) {
            case 'k' -> new TextStyle(rgb, bold, italic, underlined, strikethrough, on);
            case 'l' -> new TextStyle(rgb, on, italic, underlined, strikethrough, obfuscated);
            case 'm' -> new TextStyle(rgb, bold, italic, underlined, on, obfuscated);
            case 'n' -> new TextStyle(rgb, bold, italic, on, strikethrough, obfuscated);
            case 'o' -> new TextStyle(rgb, bold, on, underlined, strikethrough, obfuscated);
            default -> this;
        };
    }

    /** True when this style carries no colour and no decorations. */
    public boolean isPlain() {
        return rgb == null && !bold && !italic && !underlined && !strikethrough && !obfuscated;
    }
}
