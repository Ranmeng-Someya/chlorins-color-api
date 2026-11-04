package com.chlorins.colorapi.compat;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Compatibility switch for Modern UI.
 *
 * <p>Modern UI {@code @Overwrite}s {@code TextRenderer.draw(String/Text/OrderedText, …)} with
 * {@code mixinPriority 3}, i.e. before anything we inject, and it looks layouts up by
 * <em>OrderedText identity</em> ({@code TextLayoutEngine.lookupFormattedLayout}). Handing it a fresh
 * wrapper every frame would defeat that cache, so under Modern UI we colourise one level earlier -
 * at the {@code DrawContext} call site - and hand it an ordinary, stable {@code Text} component.</p>
 */
public final class ModernUICompat {

    private static final boolean PRESENT = FabricLoader.getInstance().isModLoaded("modernui");

    private ModernUICompat() {
    }

    /** True when Modern UI is installed and the component-level path should be used. */
    public static boolean present() {
        return PRESENT;
    }
}
