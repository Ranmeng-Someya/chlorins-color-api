package com.chlorins.colorapi.mixin.optional;

import com.chlorins.colorapi.mc.ColorText;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Colourises chat lines when they are added, before any renderer sees them.
 *
 * <p>Server messages such as {@code %flow[...]感谢你游玩...} arrive as literal components; doing the
 * work here means the result is renderer-independent - vanilla and Modern UI both just draw a normal
 * coloured component.</p>
 */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"), argsOnly = true)
    private Text chlorins$colorize(Text message) {
        return message == null ? null : ColorText.colorizeCached(message);
    }
}
