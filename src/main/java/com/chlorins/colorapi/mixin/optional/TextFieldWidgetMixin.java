package com.chlorins.colorapi.mixin.optional;

import com.chlorins.colorapi.render.TextPipeline;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While a text field is being drawn the markers must stay visible.
 *
 * <p>Without this, typing {@code %#FF0000Sword} into the anvil (or chat, signs, books) would make
 * the characters disappear one by one: the render hook consumes markers, but the field's buffer
 * still contains them, so what you see no longer matches what you type.</p>
 *
 * <p>The guard is a nesting depth, because some screens render more than one field.</p>
 *
 * <p>This lives in an optional mixin config: if a future Minecraft version renames
 * {@code renderWidget}, the config is skipped with a warning instead of crashing the game.</p>
 */
@Mixin(TextFieldWidget.class)
public abstract class TextFieldWidgetMixin {

    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void chlorins$enterTextField(CallbackInfo ci) {
        TextPipeline.enterTextField();
    }

    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void chlorins$exitTextField(CallbackInfo ci) {
        TextPipeline.exitTextField();
    }
}
