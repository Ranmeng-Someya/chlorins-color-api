package icyllis.modernui.mc.text;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.text.StringVisitable;
import org.joml.Matrix4f;

/**
 * Compile-time stub for Modern UI's renderer.
 *
 * <p>Modern UI is an optional runtime mod and is not on our compile classpath, but the Mixin
 * annotation processor validates {@code @Mixin(targets = ...)} at compile time. This stub gives it a
 * signature to resolve; the class is excluded from the built jar by {@code jar { exclude 'icyllis/**' }}.</p>
 */
public class ModernTextRenderer {

    public float drawText(StringVisitable text, float x, float y, int color, boolean shadow, Matrix4f matrix,
                          VertexConsumerProvider vertexConsumers, TextRenderer.TextLayerType layerType,
                          int backgroundColor, int light) {
        return 0.0F;
    }
}
