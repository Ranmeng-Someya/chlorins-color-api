package com.chlorins.colorapi.render;

import com.chlorins.colorapi.parser.Animation;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import org.joml.Matrix4f;

/**
 * Draws text that needs more than one draw call: per-character alpha ({@code %aHH}) and the
 * {@code %wave} vertical offset.
 *
 * <p>Minecraft applies the alpha of the draw-call colour to every glyph, and {@code Style} only
 * carries 24-bit RGB, so per-segment transparency is impossible in a single call. This drawer keeps
 * the fast path fast: when every character shares one alpha it still issues exactly one draw.</p>
 */
public final class SegmentedDrawer {

    /** Eight directions, used for the per-character outline. */
    private static final float[] OUTLINE_OFFSETS = {-1, -1, 0, -1, 1, -1, -1, 0, 1, 0, -1, 1, 0, 1, 1, 1};

    /** The outline is the character's own colour, darkened so it reads as a border. */
    private static final float OUTLINE_DARKEN = 0.45f;

    private SegmentedDrawer() {
    }

    public static int draw(TextRenderer font, ChlorinsOrderedText text, Overlay overlay, long time,
                           float x, float y, int color, boolean shadow, Matrix4f matrix,
                           VertexConsumerProvider vertexConsumers, TextRenderer.TextLayerType layerType,
                           int backgroundColor, int light) {
        int uniform = overlay.uniformAlpha();
        boolean moving = overlay.hasPosition();
        boolean animatedAlpha = overlay.hasAnimatedAlpha();
        boolean outline = overlay.hasOutline();
        if (!moving && !animatedAlpha && !outline && uniform >= 0) {
            // one draw call, alpha merged into the incoming colour
            return font.draw(text, x, y, withAlpha(color, uniform), shadow, matrix, vertexConsumers,
                    layerType, backgroundColor, light);
        }
        float[] cursor = {x};
        TextPipeline.enterSegmentedDraw();
        try {
            text.accept((index, style, codePoint) -> {
                Animation animation = overlay.animation(index);
                float dx = 0.0f;
                float dy = 0.0f;
                if (animation != null && animation.isPosition()) {
                    dx = AnimationEngine.offsetX(animation, overlay.waveIndex(index), time);
                    dy = AnimationEngine.offsetY(animation, overlay.waveIndex(index), time);
                }
                int glyphAlpha = overlay.alpha(index);
                if (animation != null && animation.isAlpha()) {
                    glyphAlpha = Math.max(1, Math.round(glyphAlpha * AnimationEngine.fadeFactor(time)));
                }
                int glyphColor = withAlpha(color, glyphAlpha);
                OrderedText glyph = OrderedText.styled(codePoint, style);
                if (overlay.isOutlined(index)) {
                    // the ring uses the character's own (animated) colour, so the border follows the gradient
                    int base = style.getColor() != null ? style.getColor().getRgb() : (color & 0xFFFFFF);
                    OrderedText ring = OrderedText.styled(codePoint,
                            style.withColor(AnimationEngine.scaleRgb(base, OUTLINE_DARKEN)));
                    for (int k = 0; k < OUTLINE_OFFSETS.length; k += 2) {
                        font.draw(ring, cursor[0] + dx + OUTLINE_OFFSETS[k], y + dy + OUTLINE_OFFSETS[k + 1],
                                glyphColor, false, matrix, vertexConsumers, layerType, backgroundColor, light);
                    }
                }
                font.draw(glyph, cursor[0] + dx, y + dy, glyphColor, shadow, matrix, vertexConsumers,
                        layerType, backgroundColor, light);
                cursor[0] += font.getWidth(glyph);
                return true;
            });
        } finally {
            TextPipeline.exitSegmentedDraw();
        }
        return Math.round(cursor[0] - x);
    }

    /** Multiplies the incoming alpha (or treats 0 as opaque, like vanilla) with the overlay alpha. */
    static int withAlpha(int color, int overlayAlpha) {
        int incoming = (color >>> 24) & 0xFF;
        if (incoming == 0) {
            incoming = 255;
        }
        int alpha = Math.max(1, (incoming * overlayAlpha) / 255);
        return (alpha << 24) | (color & 0xFFFFFF);
    }
}
