package net.gobbob.mobends.compat;

import java.lang.reflect.Method;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.entity.Entity;

/**
 * Salamander's CEM support (OptiFine-format entity models from resource packs, e.g. Fresh Animations): whether a pack
 * model currently takes over an entity. When it does, Mo' Bends steps aside for that entity and lets the vanilla
 * renderer draw it, which Salamander then renders with the pack's model.
 *
 * <p>Looked up by reflection, so Salamander versions without CEM simply never take over anything.
 */
public final class CemCompat {
    static final String API = "org.fentanylsolutions.salamander.cem.SalamanderCem";

    private static String api = API;
    private static Method hasModel;
    private static boolean resolved;

    private CemCompat() {}

    /** Whether a resource pack's CEM model renders {@code entity} right now (it follows pack reloads and config). */
    public static boolean hasModel(Entity entity) {
        Method method = method();
        if (method == null || entity == null) return false;
        try {
            return (Boolean)method.invoke(null, entity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            hasModel = null;
            BendsLogger.log("Salamander's CEM check failed; Mo' Bends keeps rendering: " + e, BendsLogger.ERROR);
            return false;
        }
    }

    private static Method method() {
        if (!resolved) {
            resolved = true;
            try {
                hasModel = Class.forName(api).getMethod("hasModel", Entity.class);
            } catch (ReflectiveOperationException | LinkageError e) {
                hasModel = null;
            }
        }
        return hasModel;
    }

    /** Looks the check up in {@code className} from now on (tests). */
    static void useApi(String className) {
        api = className;
        resolved = false;
        hasModel = null;
    }
}
