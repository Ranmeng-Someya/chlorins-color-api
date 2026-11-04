package com.chlorins.colorapi.mixin;

import com.chlorins.colorapi.ChlorinsColors;
import com.chlorins.colorapi.render.TextPipeline;
import net.minecraft.client.resource.language.TranslationStorage;
import net.minecraft.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Language reload hook: drops both caches (component cache and render overlay cache) every time the
 * translations are (re)built, so a resource-pack swap or {@code /reload} can never serve stale
 * colours.
 */
@Mixin(TranslationStorage.class)
public abstract class TranslationStorageMixin {

    @Inject(
            method = "load(Lnet/minecraft/resource/ResourceManager;Ljava/util/List;Z)Lnet/minecraft/client/resource/language/TranslationStorage;",
            at = @At("RETURN")
    )
    private static void chlorins$clearCache(ResourceManager manager, List<String> definitions, boolean rightToLeft,
                                            CallbackInfoReturnable<TranslationStorage> cir) {
        ChlorinsColors.clearCache();
        TextPipeline.clearCache();
        com.chlorins.colorapi.mc.ColorText.clearComponentCache();
    }
}
