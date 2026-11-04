package com.chlorins.colorapi.mc;

import com.chlorins.colorapi.ChlorinsColors;
import com.chlorins.colorapi.render.AnimationEngine;
import com.chlorins.colorapi.render.TextPipeline;
import com.chlorins.colorapi.parser.ColorParser;
import com.chlorins.colorapi.parser.ParsedText;
import com.chlorins.colorapi.parser.Animation;
import com.chlorins.colorapi.parser.Run;
import com.chlorins.colorapi.parser.TextStyle;
import com.chlorins.colorapi.parser.TranslationTemplate;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Turns {@link ParsedText} into real Minecraft {@link Text} components.
 *
 * <p>Components (and not {@code §} strings) are used because vanilla clients only understand the 16
 * legacy colours: {@code §x§RRGGBB} is <b>not</b> recognised by {@code StringDecomposer}/{@code
 * ChatFormatting}, so a gradient expressed as a legacy string would collapse to a single colour.</p>
 */
public final class ColorText {

    private ColorText() {
    }

    /**
     * Replaces a component that still carries markers by a fully coloured one.
     *
     * <p>Returns the argument untouched when there is nothing to do, so the common path costs one
     * string scan and allocates nothing.</p>
     */
    public static Text colorize(Text text) {
        if (text == null) {
            return null;
        }
        String raw = text.getString();
        if (!ChlorinsColors.hasMarkers(raw)) {
            return text;
        }
        return fromParsed(ChlorinsColors.parse(raw));
    }

    private static final com.chlorins.colorapi.cache.LruCache<String, MutableText> COLORIZED =
            new com.chlorins.colorapi.cache.LruCache<>(2048);

    /**
     * Same as {@link #colorize(Text)} but returns a cached component.
     *
     * <p>Modern UI keys its layout cache by component identity, so handing it a freshly built
     * component every frame would defeat that cache. Caching by source string keeps the same
     * instance for as long as the string is unchanged.</p>
     */
    public static Text colorizeCached(Text text) {
        if (text == null) {
            return null;
        }
        String raw = text.getString();
        if (!ChlorinsColors.hasMarkers(raw)) {
            return text;
        }
        MutableText cached = COLORIZED.get(raw);
        if (cached == null) {
            cached = fromParsed(ChlorinsColors.parse(raw));
            COLORIZED.put(raw, cached);
        }
        return cached;
    }

    public static void clearComponentCache() {
        COLORIZED.clear();
    }

    /** Builds one component tree from already parsed runs. */
    public static MutableText fromParsed(ParsedText parsed) {
        MutableText out = Text.empty();
        for (Run run : parsed.runs()) {
            TextStyle style = run.style();
            Animation animation = run.animation();
            if (animation.isAnimated() && animation.isColor()) {
                // A component cannot animate, so bake the frame-0 colour instead of showing markers.
                style = style.withColor(AnimationEngine.colorOf(animation, 0xFFFFFF, 0L));
            }
            out.append(Text.literal(run.text()).setStyle(styleOf(style)));
        }
        return out;
    }

    /**
     * Builds a component tree from a translation template, substituting {@code %s} / {@code %n$s}
     * arguments while keeping colours running across the argument boundaries.
     */
    public static MutableText fromTemplate(String template, Object[] args) {
        List<TranslationTemplate.Part> parts = TranslationTemplate.split(template);
        MutableText out = Text.empty();
        Object[] safeArgs = args == null ? new Object[0] : args;
        TextStyle style = TextStyle.DEFAULT;
        for (TranslationTemplate.Part part : parts) {
            if (part.isArgument()) {
                int index = part.argIndex();
                if (index >= 0 && index < safeArgs.length) {
                    out.append(argument(safeArgs[index], style));
                }
                continue;
            }
            ParsedText parsed = ColorParser.parse(part.literal(), style);
            out.append(fromParsed(parsed));
            if (!parsed.isEmpty()) {
                style = parsed.runs().get(parsed.runs().size() - 1).style();
            }
        }
        return out;
    }

    private static MutableText argument(Object value, TextStyle style) {
        MutableText child = value instanceof Text text ? text.copy() : Text.literal(String.valueOf(value));
        if (style.isPlain()) {
            return child;
        }
        Style inherited = styleOf(style);
        // keep the argument's own styling, but let the surrounding colour show through
        return Text.empty().setStyle(inherited).append(child);
    }

    static Style styleOf(TextStyle style) {
        Style result = Style.EMPTY;
        if (style.rgb() != null) {
            result = result.withColor(style.rgb());
        }
        if (style.bold()) {
            result = result.withBold(Boolean.TRUE);
        }
        if (style.italic()) {
            result = result.withItalic(Boolean.TRUE);
        }
        if (style.underlined()) {
            result = result.withUnderline(Boolean.TRUE);
        }
        if (style.strikethrough()) {
            result = result.withStrikethrough(Boolean.TRUE);
        }
        if (style.obfuscated()) {
            result = result.withObfuscated(Boolean.TRUE);
        }
        return result;
    }
}
