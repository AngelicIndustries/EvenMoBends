package net.gobbob.mobends.compat;

import astrotibs.villagenames.config.GeneralConfig;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.client.renderer.entity.RenderBendsZombieVillageNames;
import net.minecraft.entity.monster.EntityZombie;

/**
 * Village Names, with its modern zombie skins on, draws every zombie with a renderer of its own, registered over Mo'
 * Bends'. Draw them with Mo' Bends again, zombie villagers in Village Names' skins and headwear.
 */
public final class VillageNamesCompat {

    private VillageNamesCompat() {}

    /** From init, after Village Names registered its renderers in its own. */
    public static void registerRenderers() {
        if (Loader.isModLoaded("VillageNames")) Api.registerRenderers();
    }

    private static final class Api {

        private Api() {}

        private static void registerRenderers() {
            if (!GeneralConfig.modernZombieSkins) return;
            // The zombie's switch is asked as it is drawn; off, Village Names' own renderer draws it.
            RenderingRegistry.registerEntityRenderingHandler(EntityZombie.class, new RenderBendsZombieVillageNames());
        }
    }
}
