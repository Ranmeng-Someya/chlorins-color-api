package com.chlorins.colorapi.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates the tag syntax of the original MatrixColorAPI
 * ({@code <SOLID:#RRGGBB>…</SOLID>}, {@code <GRADIENT:#RRGGBB>…</GRADIENT:#RRGGBB>}) into the new
 * marker syntax so that existing language files keep working.
 *
 * <p>Pure Java, no Minecraft dependency.</p>
 */
public final class LegacyTags {

    private static final Pattern GRADIENT = Pattern.compile(
            "<GRADIENT:#?([A-Fa-f0-9]{6})>(.*?)</GRADIENT:#?([A-Fa-f0-9]{6})>", Pattern.DOTALL);
    private static final Pattern SOLID = Pattern.compile(
            "<SOLID:#?([A-Fa-f0-9]{6})>(.*?)(?:</SOLID>|$)", Pattern.DOTALL);

    private LegacyTags() {
    }

    public static boolean hasTags(String input) {
        return input != null && input.indexOf('<') >= 0
                && (input.contains("<GRADIENT:") || input.contains("<SOLID:"));
    }

    public static String expand(String input) {
        if (!hasTags(input)) {
            return input;
        }
        String result = GRADIENT.matcher(input).replaceAll(m -> Matcher.quoteReplacement(
                "%#" + m.group(1) + "-#" + m.group(3) + m.group(2) + "%r"));
        result = SOLID.matcher(result).replaceAll(m -> Matcher.quoteReplacement(
                "%#" + m.group(1) + m.group(2) + "%r"));
        return result;
    }
}
