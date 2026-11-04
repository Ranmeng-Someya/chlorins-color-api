package com.chlorins.colorapi.mixin;

import com.chlorins.colorapi.ChlorinsColors;
import com.chlorins.colorapi.mc.ColorText;
import com.chlorins.colorapi.parser.TranslationTemplate;
import net.minecraft.text.MutableText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Colourises translated text at the component level.
 *
 * <p>Animated translations are deliberately <b>not</b> converted here: the markers are copied into a
 * plain literal instead, so they survive until {@code TextRenderer} draws them and the render-path
 * hook can animate them frame by frame. Baking them into a static component would freeze the
 * animation.</p>
 */
@Mixin(TranslatableTextContent.class)
public abstract class TranslatableTextContentMixin {

    @Shadow
    private List<StringVisitable> translations;

    @Inject(method = "forEachPart", at = @At("HEAD"), cancellable = true)
    private void chlorins$colorizeParts(String translation, Consumer<StringVisitable> parts, CallbackInfo ci) {
        if (translation == null || !ChlorinsColors.hasMarkers(translation)) {
            return;
        }
        Object[] args = ((TranslatableTextContent) (Object) this).getArgs();
        if (ChlorinsColors.hasAnimation(translation)) {
            // keep the markers alive for the render path; only the arguments are resolved here
            parts.accept(Text.literal(TranslationTemplate.substitute(translation, args)));
            ci.cancel();
            return;
        }
        MutableText component = ColorText.fromTemplate(translation, args);
        parts.accept(component);
        ci.cancel();
    }

    @Inject(method = "updateTranslations", at = @At("TAIL"))
    private void chlorins$colorize(CallbackInfo ci) {
        List<StringVisitable> parts = this.translations;
        if (parts == null || parts.isEmpty()) {
            return;
        }
        List<StringVisitable> replaced = null;
        Object[] args = null;
        for (int i = 0; i < parts.size(); i++) {
            StringVisitable part = parts.get(i);
            if (part instanceof Text) {
                continue;
            }
            String raw = part.getString();
            if (!ChlorinsColors.hasMarkers(raw)) {
                continue;
            }
            if (replaced == null) {
                replaced = new ArrayList<>(parts);
                args = ((TranslatableTextContent) (Object) this).getArgs();
            }
            replaced.set(i, ColorText.fromTemplate(raw, args));
        }
        if (replaced != null) {
            this.translations = replaced;
        }
    }
}
