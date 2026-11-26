package net.gobbob.mobends.client.renderer.entity;

import astrotibs.villagenames.client.renderer.entity.RenderZombieVillagerModern;
import net.gobbob.mobends.client.model.entity.ModelBendsZombie;
import net.gobbob.mobends.client.model.entity.ModelBendsZombieVillagerVillageNames;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;

/**
 * Zombies with Village Names' modern zombie skins: zombie villagers wear its layered biome, profession and level skins
 * and its headwear, all animated as zombies.
 */
public class RenderBendsZombieVillageNames extends RenderBendsZombie {
    /** Village Names' renderer, for the layered skin it puts together for each zombie villager. */
    private final Skins skins = new Skins();

    @Override
    protected ModelBiped createVillagerModel() {
        return new ModelBendsZombieVillagerVillageNames();
    }

    /** As for Et Futurum's: armor follows the zombie's rig. */
    @Override
    protected ModelBiped createVillagerArmor(float scale) {
        return new ModelBendsZombie(scale, true);
    }

    /** Under a pack model, Village Names' own renderer draws them, as it would without Mo' Bends. */
    @Override
    protected RenderBiped createPackRenderer() {
        return new RenderZombieVillagerModern();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie zombie) {
        return zombie.isVillager() && !(zombie instanceof EntityPigZombie) ? this.skins.skin(zombie)
            : super.getEntityTexture(zombie);
    }

    private static final class Skins extends RenderZombieVillagerModern {
        ResourceLocation skin(EntityZombie zombie) {
            return this.getEntityTexture(zombie);
        }
    }
}
