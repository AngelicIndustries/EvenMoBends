package net.gobbob.mobends.compat;

import java.awt.image.BufferedImage;

import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.wawelauth.WawelAuthBridge;
import net.gobbob.mobends.compat.wawelauth.WawelAuthLayers;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;

/**
 * Optional Wawel Auth integration. This facade holds no Wawel Auth types; the classes that use its API load only when
 * the mod is present.
 */
public final class WawelAuthCompat {

    private WawelAuthCompat() {}

    public static boolean isLoaded() {
        return CompatibilityPolicy.isWawelAuthLoaded();
    }

    /** The player model, on the 64x64 layout when Wawel Auth is present since it always serves modern skins. */
    public static ModelBendsPlayer createPlayerModel(float scale) {
        ModelBendsPlayer model = new ModelBendsPlayer(scale);
        if (isLoaded()) model.initModern();
        return model;
    }

    public static boolean isSlim(AbstractClientPlayer player) {
        return isLoaded() && player != null && WawelAuthBridge.isSlim(player);
    }

    /** The skin as decoded, before Wawel Auth's alpha fixes; null without Wawel Auth or before it loaded. */
    public static BufferedImage originalSkin(AbstractClientPlayer player) {
        return isLoaded() && player != null ? WawelAuthBridge.originalSkin(player) : null;
    }

    /** Draws the model with Wawel Auth's skin layers in place of its own overlay boxes. */
    public static void renderWithLayers(ModelBendsPlayer model, Entity entity, float scale) {
        WawelAuthLayers.render(model, entity, scale);
    }
}
