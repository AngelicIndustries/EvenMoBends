package net.gobbob.mobends.compat;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.client.renderer.entity.RenderBendsSpecialSpider;
import net.gobbob.mobends.client.renderer.entity.RenderBendsSpecialZombie;
import toast.specialMobs.entity.cavespider.Entity_SpecialCaveSpider;
import toast.specialMobs.entity.pigzombie.Entity_SpecialPigZombie;
import toast.specialMobs.entity.spider.Entity_SpecialSpider;
import toast.specialMobs.entity.zombie.Entity_SpecialZombie;

/**
 * Father Toast's Special Mobs gives its zombies, zombie pigmen, spiders and cave spiders renderers of their own, which
 * Mo' Bends' never reach. Draw them with Mo' Bends' instead, each as its own switch says.
 */
public final class SpecialMobsCompat {

    private SpecialMobsCompat() {}

    /** From init, after Special Mobs registered its renderers in its own. */
    public static void registerRenderers() {
        if (Loader.isModLoaded("SpecialMobs")) Api.registerRenderers();
    }

    private static final class Api {

        private Api() {}

        private static void registerRenderers() {
            // Each mob's switch is asked as it is drawn; off, Special Mobs' own renderer draws it.
            RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialZombie.class, new RenderBendsSpecialZombie());
            RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialPigZombie.class, new RenderBendsSpecialZombie());
            RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialSpider.class, new RenderBendsSpecialSpider());
            RenderingRegistry.registerEntityRenderingHandler(Entity_SpecialCaveSpider.class, new RenderBendsSpecialSpider());
        }
    }
}
