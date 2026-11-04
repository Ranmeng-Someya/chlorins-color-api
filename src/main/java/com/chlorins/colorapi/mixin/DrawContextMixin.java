package com.chlorins.colorapi.mixin;

import com.chlorins.colorapi.compat.ModernUICompat;
import com.chlorins.colorapi.mc.ColorText;
import com.chlorins.colorapi.render.TextPipeline;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Component-level colourisation, used only when Modern UI is installed.
 *
 * <p><b>Targets.</b> Only the overloads that actually exist and are mapped in Yarn 1.21.1:</p>
 * <pre>
 *   drawText(TextRenderer, Text,   int, int, int, boolean) : int   (method_51439)
 *   drawText(TextRenderer, String, int, int, int, boolean) : int   (method_51433)
 * </pre>
 *
 * <p>The parameter-less "centred" variants ({@code drawText(..., III)}) are <b>not</b> named
 * {@code drawText} in Yarn, and they delegate to the six-argument overloads internally, so they are
 * covered anyway. Targeting them is what produced the
 * {@code InvalidInjectionException: could not find any targets} crash.</p>
 */
@Mixin(DrawContext.class)
public abstract class DrawContextMixin {

    /**
     * Replaces the {@link Text} argument with a fully coloured component.
     *
     * <p>No {@code ordinal} is needed: the handler parameter is {@link Text}, and there is exactly one
     * {@code Text} argument. {@code ordinal} only matters when several candidates share the handler's
     * type (e.g. the three {@code int}s x/y/color).</p>
     */
    @ModifyVariable(
            method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I",
            at = @At("HEAD"), argsOnly = true)
    private Text chlorins$colorize(Text text) {
        return ModernUICompat.present() ? ColorText.colorize(text) : text;
    }

    /**
     * The {@link String} overload cannot carry styles, so it is converted into a component and
     * re-dispatched to the {@link Text} overload (which the modifier above then colours).
     */
    @Inject(
            method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I",
            at = @At("HEAD"), cancellable = true)
    private void chlorins$colorizeString(TextRenderer textRenderer, String text, int x, int y, int color,
                                         boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (!ModernUICompat.present() || !TextPipeline.needsRewrite(text)) {
            return;
        }
        cir.setReturnValue(((DrawContext) (Object) this)
                .drawText(textRenderer, Text.literal(text), x, y, color, shadow));
    }
}
