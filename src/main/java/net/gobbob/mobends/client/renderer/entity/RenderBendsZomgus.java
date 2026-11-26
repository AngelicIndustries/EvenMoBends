package net.gobbob.mobends.client.renderer.entity;

import com.wildmobsmod.entity.monster.zomgus.RenderZomgus;
import net.gobbob.mobends.client.model.entity.ModelBendsZombie;
import net.gobbob.mobends.client.model.entity.ModelBendsZomgus;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;

/** Wild Mobs' zomgus, with its texture and its mushrooms, animated as a zombie. */
public class RenderBendsZomgus extends RenderBendsZombie {
    /** As Wild Mobs draws every zomgus, villager or not. */
    private static final ResourceLocation TEXTURE = new ResourceLocation("wildmobsmod", "textures/entity/zombie/zomgus.png");

    @Override
    protected ModelBendsZombie createModel() {
        return new ModelBendsZomgus();
    }

    /** Wild Mobs draws villager zomgus with the same model; their armor stays the zombie villager's. */
    @Override
    protected ModelBiped createVillagerModel() {
        return new ModelBendsZomgus();
    }

    /** Under a pack model, or switched off, Wild Mobs' own renderer draws it. */
    @Override
    protected RenderBiped createPackRenderer() {
        return new RenderZomgus();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie entity) {
        return TEXTURE;
    }
}
