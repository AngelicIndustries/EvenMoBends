package net.gobbob.mobends.compat;

import java.lang.reflect.Method;

import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.entity.Entity;

/**
 * Wall Grab: how far a player is into its wall slide and its wall jump, 0 to 1, from its client state. Wall Grab poses
 * a vanilla biped it swaps into the player renderer; this mod keeps its own models and plays the same poses on the
 * bones instead.
 */
public final class WallGrabCompat {
    private static final String CLIENT = "com.bogdantokarev.wallgrab.client.WallGrabClient";
    private static final boolean LOADED = Loader.isModLoaded("wallgrab");
    private static boolean resolved;
    private static boolean failed;
    private static Method poseFactor;
    private static Method jumpFactor;

    private WallGrabCompat() {}

    /** How far {@code entity} hangs in the wall slide: 0 when not. */
    public static float slide(Entity entity) {
        return LOADED && resolve() ? factor(poseFactor, entity) : 0.0f;
    }

    /** How far {@code entity} is into the push off the wall: 0 when not. */
    public static float jump(Entity entity) {
        return LOADED && resolve() ? factor(jumpFactor, entity) : 0.0f;
    }

    private static float factor(Method method, Entity entity) {
        if (failed) return 0.0f;
        try {
            float factor = ((Number)method.invoke(null, entity.getEntityId())).floatValue();
            return Float.isFinite(factor) ? Math.max(0.0f, Math.min(1.0f, factor)) : 0.0f;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            fail(e);
            return 0.0f;
        }
    }

    private static boolean resolve() {
        if (resolved) return !failed;
        resolved = true;
        try {
            Class<?> client = Class.forName(CLIENT);
            poseFactor = client.getMethod("poseFactor", int.class);
            jumpFactor = client.getMethod("jumpFactor", int.class);
            BendsLogger.log("Wall Grab detected: its wall slide and wall jump play on the bones.", BendsLogger.INFO);
        } catch (ReflectiveOperationException | LinkageError e) {
            fail(e);
        }
        return !failed;
    }

    private static void fail(Throwable e) {
        if (failed) return;
        failed = true;
        BendsLogger.log("Could not read Wall Grab's poses: " + e, BendsLogger.ERROR);
    }
}
