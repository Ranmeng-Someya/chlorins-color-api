package com.chlorins.colorapi.mixin;

import com.chlorins.colorapi.render.ChlorinsOrderedText;
import com.chlorins.colorapi.render.Overlay;
import com.chlorins.colorapi.render.TextPipeline;
import com.chlorins.colorapi.render.SegmentedDrawer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Global text takeover.
 *
 * <p>Every piece of text on screen ends up in {@link TextRenderer#draw} - vanilla chat, item names,
 * tooltips, scoreboards and third-party HUDs alike - so hooking the three {@code draw} overloads
 * covers all of them with no per-mod integration.</p>
 *
 * <p>The rewrite is a no-op for strings without markers: {@link TextPipeline#needsRewrite} is a
 * single scan that bails out immediately, and no object is allocated.</p>
 */
@Mixin(TextRenderer.class)
public abstract class TextRendererMixin {

    private static final String DRAW_STRING =
            "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;"
                    + "Lnet/minecraft/client/font/TextRenderer$TextLayerType;IIZ)I";
    private static final String DRAW_TEXT =
            "draw(Lnet/minecraft/text/Text;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;"
                    + "Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I";
    private static final String DRAW_ORDERED =
            "draw(Lnet/minecraft/text/OrderedText;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;"
                    + "Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)I";

    private static final String WIDTH_STRING = "getWidth(Ljava/lang/String;)I";
    private static final String WIDTH_TEXT = "getWidth(Lnet/minecraft/text/StringVisitable;)I";
    private static final String DRAW_WITH_OUTLINE =
            "drawWithOutline(Lnet/minecraft/text/OrderedText;FFIILorg/joml/Matrix4f;"
                    + "Lnet/minecraft/client/render/VertexConsumerProvider;I)V";

    /**
     * Marker characters take up space but are never drawn, so every centred or trimmed text would be
     * offset by their width. Subtracting it makes {@code /title}, buttons, tooltips and scoreboards
     * line up exactly as if the markers were not there.
     */
    @Inject(method = WIDTH_STRING, at = @At("RETURN"), cancellable = true)
    private void chlorins$widthString(String text, CallbackInfoReturnable<Integer> cir) {
        if (TextPipeline.inWidthComputation() || !TextPipeline.needsRewrite(text)) {
            return;
        }
        int markers = TextPipeline.markerWidth((TextRenderer) (Object) this, text);
        if (markers > 0) {
            cir.setReturnValue(Math.max(0, cir.getReturnValue() - markers));
        }
    }

    @Inject(method = WIDTH_TEXT, at = @At("RETURN"), cancellable = true)
    private void chlorins$widthText(StringVisitable text, CallbackInfoReturnable<Integer> cir) {
        String raw = text.getString();
        if (TextPipeline.inWidthComputation() || !TextPipeline.needsRewrite(raw)) {
            return;
        }
        int markers = TextPipeline.markerWidth((TextRenderer) (Object) this, raw);
        if (markers > 0) {
            cir.setReturnValue(Math.max(0, cir.getReturnValue() - markers));
        }
    }

    /** Outlined text (entity waypoints, some HUD overlays) does not go through the draw(...) chain. */
    @Inject(method = DRAW_WITH_OUTLINE, at = @At("HEAD"), cancellable = true)
    private void chlorins$drawWithOutline(OrderedText text, float x, float y, int color, int outlineColor,
                                          Matrix4f matrix, VertexConsumerProvider vertexConsumers, int light,
                                          CallbackInfo ci) {
        if (text instanceof ChlorinsOrderedText || TextPipeline.inWave()
                || TextPipeline.inSegmentedDraw() || TextPipeline.inTextField()) {
            return;
        }
        String raw = chlorins$flatten(text);
        if (!TextPipeline.needsRewrite(raw)) {
            return;
        }
        Overlay overlay = TextPipeline.overlayOf(raw);
        if (overlay == null) {
            return;
        }
        long time = TextPipeline.now();
        ((TextRenderer) (Object) this).drawWithOutline(new ChlorinsOrderedText(text, overlay, time),
                x, y, color, outlineColor, matrix, vertexConsumers, light);
        ci.cancel();
    }

    @Inject(method = DRAW_STRING, at = @At("HEAD"), cancellable = true)
    private void chlorins$drawString(String text, float x, float y, int color, boolean shadow,
                                     Matrix4f matrix, VertexConsumerProvider vertexConsumers,
                                     TextRenderer.TextLayerType layerType, int backgroundColor, int light,
                                     boolean seeThrough, CallbackInfoReturnable<Integer> cir) {
        if (TextPipeline.inTextField() || !TextPipeline.needsRewrite(text)) {
            return;
        }
        // styledForwardsVisitedString keeps § codes working exactly like vanilla
        OrderedText ordered = OrderedText.styledForwardsVisitedString(text, Style.EMPTY);
        chlorins$apply((TextRenderer) (Object) this, ordered, text, x, y, color, shadow, matrix,
                vertexConsumers, layerType, backgroundColor, light, cir);
    }

    @Inject(method = DRAW_TEXT, at = @At("HEAD"), cancellable = true)
    private void chlorins$drawText(Text text, float x, float y, int color, boolean shadow,
                                   Matrix4f matrix, VertexConsumerProvider vertexConsumers,
                                   TextRenderer.TextLayerType layerType, int backgroundColor, int light,
                                   CallbackInfoReturnable<Integer> cir) {
        String raw = text.getString();
        if (TextPipeline.inTextField() || !TextPipeline.needsRewrite(raw)) {
            return;
        }
        chlorins$apply((TextRenderer) (Object) this, text.asOrderedText(), raw, x, y, color, shadow,
                matrix, vertexConsumers, layerType, backgroundColor, light, cir);
    }

    @Inject(method = DRAW_ORDERED, at = @At("HEAD"), cancellable = true)
    private void chlorins$drawOrdered(OrderedText text, float x, float y, int color, boolean shadow,
                                      Matrix4f matrix, VertexConsumerProvider vertexConsumers,
                                      TextRenderer.TextLayerType layerType, int backgroundColor, int light,
                                      CallbackInfoReturnable<Integer> cir) {
        // our own wrapper, or the inner loop of %wave -> nothing left to do
        if (text instanceof ChlorinsOrderedText || TextPipeline.inWave()
                || TextPipeline.inSegmentedDraw() || TextPipeline.inTextField()) {
            return;
        }
        String raw = chlorins$flatten(text);
        if (!TextPipeline.needsRewrite(raw)) {
            return;
        }
        chlorins$apply((TextRenderer) (Object) this, text, raw, x, y, color, shadow, matrix,
                vertexConsumers, layerType, backgroundColor, light, cir);
    }

    private static void chlorins$apply(TextRenderer font, OrderedText ordered, String raw,
                                       float x, float y, int color, boolean shadow, Matrix4f matrix,
                                       VertexConsumerProvider vertexConsumers, TextRenderer.TextLayerType layerType,
                                       int backgroundColor, int light, CallbackInfoReturnable<Integer> cir) {
        Overlay overlay = TextPipeline.overlayOf(raw);
        if (overlay == null) {
            return;
        }
        long time = TextPipeline.now();
        ChlorinsOrderedText wrapped = new ChlorinsOrderedText(ordered, overlay, time);
        if (overlay.hasPosition() || overlay.hasAlpha() || overlay.hasOutline()) {
            cir.setReturnValue(SegmentedDrawer.draw(font, wrapped, overlay, time, x, y, color, shadow, matrix,
                    vertexConsumers, layerType, backgroundColor, light));
        } else {
            cir.setReturnValue(font.draw(wrapped, x, y, color, shadow, matrix, vertexConsumers,
                    layerType, backgroundColor, light));
        }
    }

    /** Rebuilds the plain string behind an ordered text (only used for the rare direct-OrderedText path). */
    private static String chlorins$flatten(OrderedText text) {
        StringBuilder sb = new StringBuilder(32);
        text.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        return sb.toString();
    }
}
