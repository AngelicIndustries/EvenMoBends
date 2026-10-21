package net.gobbob.mobends.compat.wawelauth;

import industries.angelic.wawelauth.api.skin.WawelSkinApi;

/** Probes the installed Wawel Auth for the skin API this build uses. Loaded only when Wawel Auth is present. */
public final class WawelAuthApi {

    private WawelAuthApi() {}

    /** Whether Wawel Auth puts Ears' features on the bones the layers are submitted on; false before it could. */
    public static boolean earsFollowLayers() {
        try {
            return WawelSkinApi.earsFollowLayers();
        } catch (LinkageError olderWawelAuth) {
            return false;
        }
    }

    /** False for a Wawel Auth from before the API, or one whose API changed since. */
    public static boolean compatible() {
        try {
            return WawelSkinApi.version() == WawelSkinApi.VERSION;
        } catch (LinkageError olderWawelAuth) {
            return false;
        }
    }
}
