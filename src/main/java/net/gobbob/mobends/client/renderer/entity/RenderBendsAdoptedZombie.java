package net.gobbob.mobends.client.renderer.entity;

import net.gobbob.mobends.compat.MobAdoption;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;

/**
 * Another mod's zombie on Mo' Bends' rig, armor and held items included. Its own renderer still picks the texture,
 * the size and the shadow, and its model any tint it sets; see {@link MobAdoption}.
 */
public class RenderBendsAdoptedZombie extends RenderBendsZombie {
    private final RenderBiped original;
    private final ModelBase originalModel;

    public RenderBendsAdoptedZombie(RenderBiped original) {
        this.original = original;
        this.originalModel = MobAdoption.mainModel(original);
        this.shadowSize = MobAdoption.shadow(original, this.shadowSize);
    }

    /** Under a pack model the mod's own renderer draws it, as it would without Mo' Bends. */
    @Override
    protected RenderBiped createPackRenderer() {
        return this.original;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie entity) {
        return MobAdoption.texture(this.original, entity, super.getEntityTexture(entity));
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        MobAdoption.preRender(this.original, entity, partialTicks);
        this.shadowSize = MobAdoption.shadow(this.original, this.shadowSize);
        MobAdoption.livingAnimations(this.originalModel, entity, partialTicks);
    }
}
