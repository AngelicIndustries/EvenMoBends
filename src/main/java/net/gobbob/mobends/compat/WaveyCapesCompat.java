package net.gobbob.mobends.compat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderPlayer;

/**
 * Wavey Capes cancels ModelBiped.renderCloak and draws its own cape from inside vanilla's
 * RenderPlayer.renderEquippedItems, which RenderBendsPlayer replaces: with both, no cape is drawn at all. Its cape
 * layer is drawn from here instead, on the body bone.
 */
public final class WaveyCapesCompat {
    private static boolean resolved;
    private static boolean failed;
    private static Constructor<?> layerConstructor;
    private static Method renderLayer;

    private WaveyCapesCompat() {}

    /** Whether Wavey Capes draws the cape, so the renderer's own cape must not. */
    public static boolean drawsCape() {
        return CompatibilityPolicy.isWaveyCapesLoaded() && !failed;
    }

    /** A cape layer for {@code renderer}, or null if Wavey Capes' layer is not there. */
    public static Object createLayer(RenderPlayer renderer, ModelBase model) {
        if (!drawsCape() || !resolve()) return null;
        try {
            return layerConstructor.newInstance(renderer, model);
        } catch (ReflectiveOperationException | LinkageError e) {
            fail(e);
            return null;
        }
    }

    /**
     * Draws {@code player}'s cape with {@code layer}, at vanilla's cape matrix: behind the top of the back, turned by
     * vanilla's cape swing, which Wavey Capes undoes. Its other entry point bakes the cape against an identity matrix.
     */
    public static void render(Object layer, AbstractClientPlayer player, float partialTicks) {
        if (layer == null) return;
        try {
            renderLayer.invoke(layer, player, 0.0f, 0.0f, partialTicks, 0.0f, 0.0f, 0.0f, 0.0f, true);
        } catch (ReflectiveOperationException | LinkageError e) {
            fail(e);
        }
    }

    private static boolean resolve() {
        if (!resolved) {
            resolved = true;
            try {
                Class<?> type = Class.forName("dev.tr7zw.waveycapes.renderlayers.CustomCapeRenderLayer");
                layerConstructor = type.getConstructor(RenderPlayer.class, ModelBase.class);
                renderLayer = type.getMethod("doRenderLayer", AbstractClientPlayer.class, float.class, float.class,
                    float.class, float.class, float.class, float.class, float.class, boolean.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                fail(e);
            }
        }
        return layerConstructor != null && renderLayer != null;
    }

    private static void fail(Throwable e) {
        if (!failed) BendsLogger.log("Could not draw Wavey Capes' cape: " + e, BendsLogger.ERROR);
        failed = true;
        layerConstructor = null;
        renderLayer = null;
    }
}
