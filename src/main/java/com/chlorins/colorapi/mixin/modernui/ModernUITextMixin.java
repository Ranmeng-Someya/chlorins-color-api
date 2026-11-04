package com.chlorins.colorapi.mixin.modernui;

import com.chlorins.colorapi.mc.ColorText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Feeds Modern UI pre-coloured components.
 *
 * <p>Modern UI does not draw through {@code DrawContext}: it {@code @Overwrite}s
 * {@code TextRenderer.draw} and takes the text at component level, so the vanilla-side hooks never
 * see it. This mixin sits on its own entry point ({@code ModernTextRenderer.drawText}) and swaps the
 * {@link StringVisitable} argument for a coloured component, which Modern UI can then lay out and
 * cache like any normal text.</p>
 *
 * <p>Only active when Modern UI is installed; the config is optional.</p>
 */
@Mixin(targets = "icyllis.modernui.mc.text.ModernTextRenderer")
public abstract class ModernUITextMixin {

    @ModifyVariable(
            method = "drawText(Lnet/minecraft/text/StringVisitable;FFIZLorg/joml/Matrix4f;"
                    + "Lnet/minecraft/client/render/VertexConsumerProvider;"
                    + "Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)F",
            at = @At("HEAD"), argsOnly = true)
    private StringVisitable chlorins$colorize(StringVisitable text) {
        return text instanceof Text component ? ColorText.colorizeCached(component) : text;
    }
}
