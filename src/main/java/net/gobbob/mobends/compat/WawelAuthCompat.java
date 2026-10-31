package net.gobbob.mobends.compat;



/**
 * Optional Wawel Auth integration, for accounts and skin uploads. This facade holds no Wawel Auth types; the classes
 * that use its API load only when the mod is present.
 */
public final class WawelAuthCompat {

    private WawelAuthCompat() {}

    /** Wawel Auth with the API this build uses, for accounts and skin uploads. */
    public static boolean isLoaded() {
        return CompatibilityPolicy.isWawelAuthLoaded();
    }
}
