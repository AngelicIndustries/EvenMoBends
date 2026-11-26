package net.gobbob.mobends.compat;

import com.wildmobsmod.entity.monster.zomgus.EntityZomgus;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.client.renderer.entity.RenderBendsZomgus;

/**
 * Wild Mobs' zomgus: a zombie with mushrooms growing on it, which adoption leaves to Wild Mobs for those extra parts.
 * Mo' Bends draws it with the mushrooms on its rig instead.
 */
public final class WildMobsCompat {

    private WildMobsCompat() {}

    /** From init, after Wild Mobs registered its renderers in its own. */
    public static void registerRenderers() {
        if (Loader.isModLoaded("wildmobsmod")) Api.registerRenderers();
    }

    private static final class Api {

        private Api() {}

        private static void registerRenderers() {
            // Its switch is asked as it is drawn; off, Wild Mobs' own renderer draws it.
            RenderingRegistry.registerEntityRenderingHandler(EntityZomgus.class, new RenderBendsZomgus());
        }
    }
}
