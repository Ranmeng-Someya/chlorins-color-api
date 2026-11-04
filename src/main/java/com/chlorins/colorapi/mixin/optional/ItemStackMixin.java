package com.chlorins.colorapi.mixin.optional;

import com.chlorins.colorapi.mc.ColorText;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Colourises item names and tooltips at the source.
 *
 * <p>This is renderer-independent: whether the text is drawn by vanilla or by Modern UI, the
 * component handed to the renderer already carries the colours, so it cannot be bypassed.</p>
 *
 * <p>Components are taken from a cache keyed by the source string, so the same instance is returned
 * for the same name - Modern UI keys its layout cache by identity.</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void chlorins$name(CallbackInfoReturnable<Text> cir) {
        Text name = cir.getReturnValue();
        if (name != null) {
            cir.setReturnValue(ColorText.colorizeCached(name));
        }
    }

    @Inject(method = "getTooltip", at = @At("RETURN"), cancellable = true)
    private void chlorins$tooltip(CallbackInfoReturnable<List<Text>> cir) {
        List<Text> lines = cir.getReturnValue();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        List<Text> colored = null;
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            Text replaced = ColorText.colorizeCached(line);
            if (replaced != line) {
                if (colored == null) {
                    colored = new ArrayList<>(lines);
                }
                colored.set(i, replaced);
            }
        }
        if (colored != null) {
            cir.setReturnValue(colored);
        }
    }
}
