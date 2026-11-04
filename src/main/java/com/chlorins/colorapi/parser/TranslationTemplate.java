package com.chlorins.colorapi.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a translation template into literal chunks and {@code %s} argument slots.
 *
 * <p>Minecraft's own splitter throws as soon as a translation contains a {@code %} that is not
 * {@code %s}, {@code %<n>$s} or {@code %%} - exactly what our {@code %#RRGGBB} markers look like.
 * We therefore do the split ourselves.</p>
 */
public final class TranslationTemplate {

    private static final Pattern ARG = Pattern.compile("%(?:(\\d+)\\$)?s");

    private TranslationTemplate() {
    }

    /**
     * @param literal  literal text ({@code null} for argument slots)
     * @param argIndex zero-based argument index, or {@code -1} for literal text
     */
    public record Part(String literal, int argIndex) {

        public boolean isArgument() {
            return argIndex >= 0;
        }
    }

    public static List<Part> split(String template) {
        List<Part> parts = new ArrayList<>();
        if (template == null || template.isEmpty()) {
            return parts;
        }
        StringBuilder literal = new StringBuilder();
        int cursor = 0;
        int implicit = 0;
        Matcher matcher = ARG.matcher(template);
        while (matcher.find()) {
            if (matcher.start() > cursor) {
                literal.append(template, cursor, matcher.start());
            }
            String index = matcher.group(1);
            int argIndex = index != null ? Integer.parseInt(index) - 1 : implicit++;
            if (argIndex < 0) {
                literal.append(template, matcher.start(), matcher.end());
            } else {
                if (!literal.isEmpty()) {
                    parts.add(new Part(literal.toString(), -1));
                    literal.setLength(0);
                }
                parts.add(new Part(null, argIndex));
            }
            cursor = matcher.end();
        }
        if (cursor < template.length()) {
            literal.append(template, cursor, template.length());
        }
        if (!literal.isEmpty()) {
            parts.add(new Part(literal.toString(), -1));
        }
        return parts;
    }

    /** Flattens {@code %%} and {@code %s} into a plain string; unknown arguments are dropped. */
    public static String substitute(String template, Object[] args) {
        if (template == null) {
            return "";
        }
        List<Part> parts = split(template);
        StringBuilder sb = new StringBuilder(template.length());
        for (Part part : parts) {
            if (part.isArgument()) {
                int index = part.argIndex();
                if (args != null && index < args.length && args[index] != null) {
                    sb.append(args[index]);
                }
            } else {
                sb.append(part.literal());
            }
        }
        return sb.toString();
    }

    /** @return the flattened literal, or {@code null} when the template has argument slots */
    public static String flatten(String template) {
        List<Part> parts = split(template);
        StringBuilder sb = new StringBuilder();
        for (Part part : parts) {
            if (part.isArgument()) {
                return null;
            }
            sb.append(part.literal());
        }
        return sb.toString();
    }
}
