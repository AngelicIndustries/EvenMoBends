package net.gobbob.mobends.compat;

import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.util.BendsLogger;

/** Centralizes ownership decisions for other player-rendering mods. */
public final class CompatibilityPolicy {
    private static final boolean SMART_MOVING_LOADED = Loader.isModLoaded("SmartMoving");
    private static final boolean SMART_RENDER_LOADED = Loader.isModLoaded("SmartRender");
    private static final boolean WAWEL_AUTH_LOADED = Loader.isModLoaded("wawelauth");
    private static final boolean SIMPLE_SKIN_BACKPORT_LOADED = Loader.isModLoaded("simpleskinbackport");
    private static final boolean JUST_A_SHIELD_LOADED = Loader.isModLoaded("targaseule");
    private static final boolean AQUA_ACROBATICS_LOADED = Loader.isModLoaded("aquaacrobatics");
    private static final boolean ET_FUTURUM_REQUIEM_LOADED = Loader.isModLoaded("etfuturum");
    private static final boolean BACKHAND_LOADED = Loader.isModLoaded("backhand");
    private static final boolean MOB_ONLY_MODE = SMART_MOVING_LOADED || SMART_RENDER_LOADED;

    private static boolean modeLogged;

    private CompatibilityPolicy() {}

    public static boolean arePlayerAnimationsEnabled() {
        return !MOB_ONLY_MODE;
    }

    public static boolean isMobOnlyMode() {
        return MOB_ONLY_MODE;
    }

    public static boolean isWawelAuthLoaded() {
        return WAWEL_AUTH_LOADED;
    }

    public static boolean isSimpleSkinBackportLoaded() {
        return SIMPLE_SKIN_BACKPORT_LOADED;
    }

    public static boolean isJustAShieldLoaded() {
        return JUST_A_SHIELD_LOADED;
    }

    public static boolean isAquaAcrobaticsLoaded() {
        return AQUA_ACROBATICS_LOADED;
    }

    public static boolean isEtFuturumRequiemLoaded() {
        return ET_FUTURUM_REQUIEM_LOADED;
    }

    public static boolean isBackhandLoaded() {
        return BACKHAND_LOADED;
    }

    public static void logActiveMode() {
        if (modeLogged) {
            return;
        }
        modeLogged = true;

        if (MOB_ONLY_MODE) {
            String owner = SMART_MOVING_LOADED ? "Smart Moving" : "Smart Render";
            BendsLogger.log(
                owner + " detected: disabling all Mo' Bends player rendering and keeping mob animations enabled.",
                BendsLogger.INFO);
        }
    }
}
