package com.chlorins.colorapi;

import com.chlorins.colorapi.client.ColorKeybinds;
import com.chlorins.colorapi.client.DebugHud;
import com.chlorins.colorapi.command.ColorCommand;
import com.chlorins.colorapi.mc.ColorText;
import com.chlorins.colorapi.render.TextPipeline;
import com.chlorins.colorapi.parser.ColorParser;
import com.chlorins.colorapi.parser.ParsedText;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.text.MutableText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chlorins Color API - client entry point.
 *
 * <p>The mod has no commands and no config screen: it teaches Minecraft to understand colour markers
 * inside translated text. All behaviour is driven by two mixins
 * ({@code TranslatableTextContentMixin} for rendering, {@code TranslationStorageMixin} for cache
 * invalidation on language reload).</p>
 */
public final class ChlorinsColorApi implements ClientModInitializer {

    public static final String MOD_ID = "chlorins_color_api";
    public static final String MOD_NAME = "Chlorins Color API";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[{}] colour markers enabled: prefixes '{}', solid + RGB gradient support",
                MOD_NAME, ColorParser.DEFAULT_PREFIXES);
        ColorCommand.register();
        ColorKeybinds.register();
        DebugHud.register();
    }

    // ------------------------------------------------------------------ public helper API

    /** Parses a string into resolved runs. Pure Java, no Minecraft dependency. */
    public static ParsedText parse(String text) {
        return ChlorinsColors.parse(text);
    }

    /** Converts a string with markers into a coloured {@code Text} component. */
    public static MutableText toText(String text) {
        return ColorText.fromParsed(ChlorinsColors.parse(text));
    }

    /** Converts a translation template (with {@code %s} arguments) into a coloured component. */
    public static MutableText toText(String template, Object... args) {
        return ColorText.fromTemplate(template, args);
    }

    /** Converts markers into a legacy {@code §} string (RGB is emitted as {@code §x}, servers only). */
    public static String toLegacy(String text) {
        return ChlorinsColors.toLegacy(text);
    }

    /** True when the given string contains at least one marker this mod would rewrite. */
    public static boolean hasMarkers(String text) {
        return ChlorinsColors.hasMarkers(text);
    }

    /** Drops both caches (component + render overlay); called automatically on language reload. */
    public static void clearCache() {
        ChlorinsColors.clearCache();
        TextPipeline.clearCache();
        ColorText.clearComponentCache();
    }

    /** True when the string contains a {@code %rainbow[} / {@code %wave[} / {@code %pulse[} tag. */
    public static boolean hasAnimation(String text) {
        return ChlorinsColors.hasAnimation(text);
    }
}
