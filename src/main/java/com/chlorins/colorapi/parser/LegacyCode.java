package com.chlorins.colorapi.parser;

/** One parsed {@code §} or {@code &} formatting code. */
final class LegacyCode {

    static final int TYPE_COLOR = 0;
    static final int TYPE_DECORATION = 1;
    static final int TYPE_RESET = 2;

    final int type;
    final int rgb;
    final char code;
    final int length;

    private LegacyCode(int type, int rgb, char code, int length) {
        this.type = type;
        this.rgb = rgb;
        this.code = code;
        this.length = length;
    }

    static LegacyCode color(int rgb, int length) {
        return new LegacyCode(TYPE_COLOR, rgb, 'r', length);
    }

    static LegacyCode decoration(char code, int length) {
        return new LegacyCode(TYPE_DECORATION, 0, code, length);
    }

    static LegacyCode reset(int length) {
        return new LegacyCode(TYPE_RESET, 0, 'r', length);
    }

    TextStyle apply(TextStyle style) {
        return switch (type) {
            case TYPE_COLOR -> style.withColor(rgb);
            case TYPE_DECORATION -> style.withDecoration(code, true);
            default -> TextStyle.DEFAULT;
        };
    }
}
