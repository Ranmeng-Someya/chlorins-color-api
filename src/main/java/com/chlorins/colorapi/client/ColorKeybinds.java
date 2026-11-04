package com.chlorins.colorapi.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Registers the debug key under {@code Options → Controls → Key Binds → Chlorins Color API}.
 *
 * <p>Default is {@code F9}; it can be rebound like any vanilla key. Pressing it toggles the debug
 * overlay, which is also a live self-test of every animation.</p>
 */
public final class ColorKeybinds {

    public static final String CATEGORY = "category.chlorins_color_api";
    public static final String DEBUG_KEY = "key.chlorins_color_api.debug";

    private static KeyBinding debug;

    private ColorKeybinds() {
    }

    /** Localised name of the currently bound key, for the debug overlay. */
    public static String boundKeyName() {
        return debug == null ? "?" : debug.getBoundKeyLocalizedText().getString();
    }

    public static void register() {
        debug = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                DEBUG_KEY, InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F9, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (debug.wasPressed()) {
                DebugState.toggle();
            }
        });
    }
}
