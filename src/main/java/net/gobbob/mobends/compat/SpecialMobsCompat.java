package net.gobbob.mobends.compat;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.client.renderer.entity.RenderBendsSpecialSpider;
import net.gobbob.mobends.client.renderer.entity.RenderBendsSpecialZombie;
import toast.specialMobs.entity.cavespider.Entity_SpecialCaveSpider;
import toast.specialMobs.entity.pigzombie.Entity_SpecialPigZombie;
import toast.specialMobs.entity.spider.Entity_SpecialSpider;
import toast.specialMobs.entity.zombie.Entity_SpecialZombie;

/**
 * Father Toast's Special Mobs gives its zombies, zombie pigmen, spiders and cave spiders renderers of their own, which
 * Mo' Bends' never reach. Draw them with Mo' Bends' instead, wherever their vanilla kin animate.
 */
public final class SpecialMobsCompat {

    private SpecialMobsCompat() {}

    /** From init, after Special Mobs registered its renderers in its own. */
    public static void registerRenderers() {
        if (Loader.isModLoaded("SpecialMobs")) Api.registerRenderers();
    }

    private static boolean animates(String id) {
        for (AnimatedEntity entity : AnimatedEntity.animatedEntities) {
            if (entity.id.equals(id)) return entity.animate;
        }
        return false;
    }

    private static final class Api {

        private Api() {}

        private static void registerRenderers() {
            if (animates("zombie")) {
                RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialZombie.class, new RenderBendsSpecialZombie());
                RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialPigZombie.class, new RenderBendsSpecialZombie());
            }
            if (animates("spider")) {
                RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialSpider.class, new RenderBendsSpecialSpider());
            }
            if (animates("cave_spider")) {
                RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialCaveSpider.class, new RenderBendsSpecialSpider());
            }
        }
    }
}
