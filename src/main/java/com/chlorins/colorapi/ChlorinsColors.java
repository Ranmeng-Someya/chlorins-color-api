package com.chlorins.colorapi;

import com.chlorins.colorapi.cache.ParseCache;
import com.chlorins.colorapi.parser.ColorParser;
import com.chlorins.colorapi.parser.ParsedText;
import com.chlorins.colorapi.parser.TranslationTemplate;

import java.util.List;

/**
 * Public, Minecraft-agnostic entry point of Chlorins Color API.
 *
 * <p>Everything here can be used from unit tests, a server plugin or a mod; the Fabric/Minecraft
 * glue lives in {@code com.chlorins.colorapi.mc}.</p>
 */
public final class ChlorinsColors {

    private static final ParseCache CACHE = new ParseCache();

    private ChlorinsColors() {
    }

    /** True when the string contains anything this library would change. */
    public static boolean hasMarkers(String text) {
        return ColorParser.hasMarkers(text) || com.chlorins.colorapi.parser.LegacyTags.hasTags(text);
    }

    /** Parses (and caches) a string into fully resolved runs. */
    public static ParsedText parse(String text) {
        if (text == null || text.isEmpty()) {
            return ParsedText.EMPTY;
        }
        if (!hasMarkers(text)) {
            return new ParsedText(List.of(new com.chlorins.colorapi.parser.Run(text,
                    com.chlorins.colorapi.parser.TextStyle.DEFAULT)));
        }
        return CACHE.get(text);
    }

    /** Parses without caching. */
    public static ParsedText parseUncached(String text) {
        return ColorParser.parse(text);
    }

    /**
     * Converts markers into a legacy {@code §} string.
     *
     * <p>Compatibility helper: RGB colours become {@code §x§R§R§G§G§B§B}, which vanilla clients do
     * <b>not</b> render. Use the component API for real RGB colours.</p>
     */
    public static String toLegacy(String text) {
        return parse(text).toLegacy();
    }

    /** Splits a translation template into literal chunks and {@code %s} slots. */
    public static List<TranslationTemplate.Part> splitTemplate(String template) {
        return TranslationTemplate.split(template);
    }

    /** True when the text carries a {@code %rainbow[} / {@code %wave[} / {@code %pulse[} tag. */
    public static boolean hasAnimation(String text) {
        return ColorParser.hasAnimation(text);
    }

    /** Drops the parse cache. Must be called on every language/resource reload. */
    public static void clearCache() {
        CACHE.clear();
    }

    public static int cacheSize() {
        return CACHE.size();
    }
}
