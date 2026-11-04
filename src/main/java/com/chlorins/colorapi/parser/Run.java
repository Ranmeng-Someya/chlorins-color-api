package com.chlorins.colorapi.parser;

/** A contiguous piece of text that shares one {@link TextStyle} and (optionally) one animation. */
public record Run(String text, TextStyle style, Animation animation) {

    public Run(String text, TextStyle style) {
        this(text, style, Animation.NONE);
    }

    public Run {
        if (animation == null) {
            animation = Animation.NONE;
        }
    }

    public boolean isAnimated() {
        return animation.isAnimated();
    }
}
