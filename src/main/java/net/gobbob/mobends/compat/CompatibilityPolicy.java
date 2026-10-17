package net.gobbob.mobends.compat;

import cpw.mods.fml.common.Loader;
import net.minecraft.launchwrapper.Launch;
import net.gobbob.mobends.compat.wawelauth.WawelAuthApi;
import net.gobbob.mobends.util.BendsLogger;

/** Centralizes ownership decisions for other player-rendering mods. */
public final class CompatibilityPolicy {
    private static final boolean SMART_MOVING_LOADED = Loader.isModLoaded("SmartMoving");
    private static final boolean SMART_RENDER_LOADED = Loader.isModLoaded("SmartRender");
    private static final boolean WAWEL_AUTH_PRESENT = Loader.isModLoaded("wawelauth");
    private static final boolean WAWEL_AUTH_LOADED = WAWEL_AUTH_PRESENT && WawelAuthApi.compatible();
    private static final boolean SIMPLE_SKIN_BACKPORT_LOADED = Loader.isModLoaded("simpleskinbackport");
    private static final boolean JUST_A_SHIELD_LOADED = Loader.isModLoaded("targaseule");
    private static final boolean AQUA_ACROBATICS_LOADED = Loader.isModLoaded("aquaacrobatics");
    private static final boolean ET_FUTURUM_REQUIEM_LOADED = Loader.isModLoaded("etfuturum");
    private static final boolean BACKHAND_LOADED = Loader.isModLoaded("backhand");
    private static final boolean WAVEY_CAPES_LOADED = Loader.isModLoaded("waveycapes");
    /** Ears anchors its features to the vanilla limbs, which Mo' Bends' split limbs don't have. */
    private static final boolean EARS_LOADED = Loader.isModLoaded("ears") || foamFixEarsActive();
    private static final boolean MOB_ONLY_MODE = SMART_MOVING_LOADED || SMART_RENDER_LOADED || EARS_LOADED;

    private static boolean modeLogged;

    private CompatibilityPolicy() {}

    /** FoamFix bundles Ears and applies it unless its config turns it off. */
    private static boolean foamFixEarsActive() {
        if (!Loader.isModLoaded("foamfix")) return false;
        try {
            Class<?> transformer = Class.forName("pl.asie.foamfix.bugfixmod.coremod.BugfixModClassTransformer", false,
                Launch.classLoader);
            Object instance = transformer.getField("instance").get(null);
            return instance != null && (Boolean) transformer.getMethod("applyEarsPatch").invoke(instance);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    public static boolean arePlayerAnimationsEnabled() {
        return !MOB_ONLY_MODE;
    }

    public static boolean isMobOnlyMode() {
        return MOB_ONLY_MODE;
    }

    /** Wawel Auth with the skin API this build uses; an older one is left alone. */
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

    public static boolean isWaveyCapesLoaded() {
        return WAVEY_CAPES_LOADED;
    }

    public static boolean isBackhandLoaded() {
        return BACKHAND_LOADED;
    }

    public static void logActiveMode() {
        if (modeLogged) {
            return;
        }
        modeLogged = true;

        if (WAWEL_AUTH_PRESENT && !WAWEL_AUTH_LOADED) {
            BendsLogger.log("Wawel Auth detected without a compatible skin API: skipping its integration.",
                BendsLogger.INFO);
        }
        if (MOB_ONLY_MODE) {
            String owner = SMART_MOVING_LOADED ? "Smart Moving" : SMART_RENDER_LOADED ? "Smart Render" : "Ears";
            BendsLogger.log(
                owner + " detected: disabling all Mo' Bends player rendering and keeping mob animations enabled.",
                BendsLogger.INFO);
        }
    }
}
