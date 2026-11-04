package com.chlorins.colorapi.render;

import com.chlorins.colorapi.parser.Animation;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;

/**
 * An {@link OrderedText} view that paints colours and animations on top of another ordered text.
 *
 * <p>Nothing is copied: the delegate is streamed and each character's {@link Style} is rewritten on
 * the fly. That is why rainbow/pulse cost the same as plain text - no component tree is built, only
 * one small immutable {@code Style} per visible character and frame.</p>
 */
public final class ChlorinsOrderedText implements OrderedText {

    private static final int DEFAULT_RGB = 0xFFFFFF;

    private final OrderedText delegate;
    private final Overlay overlay;
    private final long time;

    public ChlorinsOrderedText(OrderedText delegate, Overlay overlay, long time) {
        this.delegate = delegate;
        this.overlay = overlay;
        this.time = time;
    }

    public Overlay overlay() {
        return overlay;
    }

    /** Applies the {@code §k}/{@code §l}/… state collected by the scanner on top of the original style. */
    private static Style applyDecorations(Style style, byte on, byte off) {
        Style result = style;
        if ((on & Overlay.OBFUSCATED) != 0) {
            result = result.withObfuscated(Boolean.TRUE);
        } else if ((off & Overlay.OBFUSCATED) != 0) {
            result = result.withObfuscated(Boolean.FALSE);
        }
        if ((on & Overlay.BOLD) != 0) {
            result = result.withBold(Boolean.TRUE);
        } else if ((off & Overlay.BOLD) != 0) {
            result = result.withBold(Boolean.FALSE);
        }
        if ((on & Overlay.ITALIC) != 0) {
            result = result.withItalic(Boolean.TRUE);
        } else if ((off & Overlay.ITALIC) != 0) {
            result = result.withItalic(Boolean.FALSE);
        }
        if ((on & Overlay.UNDERLINE) != 0) {
            result = result.withUnderline(Boolean.TRUE);
        } else if ((off & Overlay.UNDERLINE) != 0) {
            result = result.withUnderline(Boolean.FALSE);
        }
        if ((on & Overlay.STRIKETHROUGH) != 0) {
            result = result.withStrikethrough(Boolean.TRUE);
        } else if ((off & Overlay.STRIKETHROUGH) != 0) {
            result = result.withStrikethrough(Boolean.FALSE);
        }
        return result;
    }

    public long time() {
        return time;
    }

    @Override
    public boolean accept(CharacterVisitor visitor) {
        return delegate.accept((index, style, codePoint) -> {
            if (overlay.isSkipped(index)) {
                return true;
            }
            Style styled = applyDecorations(style, overlay.decorationsOn(index), overlay.decorationsOff(index));
            Animation animation = overlay.animation(index);
            if (animation != null && animation.isAnimated() && !animation.isWave()) {
                int rgb = AnimationEngine.colorOf(animation, DEFAULT_RGB, time);
                return visitor.accept(index, styled.withColor(rgb), codePoint);
            }
            int rgb = overlay.color(index);
            if (rgb != Overlay.NO_COLOR) {
                return visitor.accept(index, styled.withColor(rgb), codePoint);
            }
            return visitor.accept(index, styled, codePoint);
        });
    }
}
