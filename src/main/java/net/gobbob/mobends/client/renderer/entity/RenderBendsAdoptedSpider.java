package net.gobbob.mobends.client.renderer.entity;

import net.gobbob.mobends.compat.MobAdoption;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.util.ResourceLocation;

/**
 * Another mod's spider or cave spider on Mo' Bends' rig. Its own renderer still picks the texture, the size, the eyes
 * and the shadow; see {@link MobAdoption}.
 */
public class RenderBendsAdoptedSpider extends RenderBendsSpider {
    private static final ResourceLocation FALLBACK = new ResourceLocation("textures/entity/spider/spider.png");
    private final RenderSpider original;
    private final ModelBase originalModel;

    public RenderBendsAdoptedSpider(RenderSpider original) {
        this.original = original;
        this.originalModel = MobAdoption.mainModel(original);
        this.shadowSize = MobAdoption.shadow(original, this.shadowSize);
    }

    /** Under a pack model the mod's own renderer draws it, as it would without Mo' Bends. */
    @Override
    protected RenderSpider createVanillaRenderer() {
        return this.original;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySpider entity) {
        return MobAdoption.texture(this.original, entity, FALLBACK);
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        MobAdoption.preRender(this.original, entity, partialTicks);
        this.shadowSize = MobAdoption.shadow(this.original, this.shadowSize);
        MobAdoption.livingAnimations(this.originalModel, entity, partialTicks);
    }

    @Override
    protected int shouldRenderPass(EntitySpider entity, int pass, float partialTicks) {
        return MobAdoption.renderPass(this.original, entity, pass, partialTicks);
    }
}
