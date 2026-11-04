package com.chlorins.colorapi.mixin.bls;

import com.chlorins.colorapi.compat.bls.BetterLoadingScreenCompat;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the Better Loading Screen layout to the multiplayer "receiving level" screen.
 *
 * <p>In Yarn 1.21.1 that screen is {@code DownloadingTerrainScreen} (the Mojang name
 * {@code ReceivingLevelScreen} is not used here). Vanilla draws its status text centred at
 * {@code height / 2 - 50}; this moves it down above the bar and then draws the bar itself.</p>
 *
 * <p>Optional config: if the screen is ever renamed the mixin is skipped with a warning.</p>
 */
@Mixin(targets = "net.minecraft.client.gui.screen.DownloadingTerrainScreen")
public abstract class DownloadingTerrainScreenMixin {

    @Redirect(
            method = "render",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow("
                            + "Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"))
    private void chlorins$moveStatusText(DrawContext context, TextRenderer font, Text text,
                                         int x, int y, int color) {
        if (!BetterLoadingScreenCompat.active()) {
            context.drawCenteredTextWithShadow(font, text, x, y, color);
            return;
        }
        context.drawCenteredTextWithShadow(font, text, x, BetterLoadingScreenCompat.statusY(context), color);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void chlorins$drawBar(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        BetterLoadingScreenCompat.drawOverlay(context);
    }
}
