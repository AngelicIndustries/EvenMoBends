package net.gobbob.mobends.compat;

import java.lang.reflect.Method;

import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.entity.Entity;

/**
 * Tuck &amp; Roll: how far a player is curled up for its roll, 0 to 1, from its client state. Tuck &amp; Roll turns the
 * whole player for the roll itself and makes it sneak; the curl is played on the bones here.
 */
public final class TuckAndRollCompat {
    private static final String STATE = "com.bogdantokarev.tuckandroll.client.RollClientState";
    private static final boolean LOADED = Loader.isModLoaded("tuckandroll");
    private static boolean resolved;
    private static boolean failed;
    private static Method isRolling;
    private static Method tuckAmount;

    private TuckAndRollCompat() {}

    /** How far {@code entity} is curled up: 0 when not rolling. */
    public static float tuck(Entity entity, float partialTicks) {
        if (!LOADED || failed || !resolve()) return 0.0f;
        try {
            int id = entity.getEntityId();
            if (!(Boolean)isRolling.invoke(null, id)) return 0.0f;
            float tuck = ((Number)tuckAmount.invoke(null, id, partialTicks)).floatValue();
            return Float.isFinite(tuck) ? Math.max(0.0f, Math.min(1.0f, tuck)) : 0.0f;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            fail(e);
            return 0.0f;
        }
    }

    private static boolean resolve() {
        if (resolved) return !failed;
        resolved = true;
        try {
            Class<?> state = Class.forName(STATE);
            isRolling = state.getMethod("isRolling", int.class);
            tuckAmount = state.getMethod("tuckAmount", int.class, float.class);
            BendsLogger.log("Tuck & Roll detected: its roll curls up on the bones.", BendsLogger.INFO);
        } catch (ReflectiveOperationException | LinkageError e) {
            fail(e);
        }
        return !failed;
    }

    private static void fail(Throwable e) {
        if (failed) return;
        failed = true;
        BendsLogger.log("Could not read Tuck & Roll's roll: " + e, BendsLogger.ERROR);
    }
}
