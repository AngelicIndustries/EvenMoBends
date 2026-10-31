package net.gobbob.mobends.compat;

import java.awt.image.BufferedImage;

import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.salamander.SalamanderLayers;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;

import industries.angelic.salamander.api.skin.PlayerSkin;
import industries.angelic.salamander.api.skin.PlayerSkins;

/**
 * Players' skins as Salamander serves them: 64x64, from Wawel Auth when it is loaded and from vanilla's downloads
 * otherwise. Salamander draws the overlay layers on this mod's joints.
 */
public final class SalamanderSkins {

    private SalamanderSkins() {}

    /** The player model, on the 64x64 layout Salamander always serves. */
    public static ModelBendsPlayer createPlayerModel(float scale) {
        ModelBendsPlayer model = new ModelBendsPlayer(scale);
        model.initModern();
        return model;
    }

    public static boolean isSlim(AbstractClientPlayer player) {
        return player != null && PlayerSkins.slim(player);
    }

    /** The skin as decoded, before its alpha was fixed up; null before it loaded. */
    public static BufferedImage originalSkin(AbstractClientPlayer player) {
        if (player == null) return null;
        PlayerSkin skin = PlayerSkins.get(player);
        return skin.available() ? skin.originalImage() : null;
    }

    /** Draws the model with Salamander's skin layers in place of its own overlay boxes. */
    public static void renderWithLayers(ModelBendsPlayer model, Entity entity, float scale) {
        SalamanderLayers.render(model, entity, scale);
    }
}
