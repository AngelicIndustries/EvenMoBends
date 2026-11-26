package net.gobbob.mobends.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import toast.specialMobs.client.RenderSpecialSpider;
import toast.specialMobs.entity.ISpecialMob;

/**
 * Special Mobs' spiders and cave spiders, with their textures, glowing eyes and sizes (a cave spider's smaller size is
 * part of its own), animated as spiders.
 */
public class RenderBendsSpecialSpider extends RenderBendsSpider {

    /** Under a pack model, Special Mobs' own renderer draws them, as it would without Mo' Bends. */
    @Override
    protected RenderSpider createVanillaRenderer() {
        return new RenderSpecialSpider();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySpider entity) {
        return ((ISpecialMob)entity).getSpecialData().getTexture();
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        super.preRenderCallback(entity, partialTicks);
        float scale = ((ISpecialMob)entity).getSpecialData().getRenderScale();
        this.shadowSize = scale;
        GL11.glScalef(scale, scale, scale);
    }

    /** The eyes as vanilla draws them, from the spider's own eye texture. */
    @Override
    protected int shouldRenderPass(EntitySpider entity, int pass, float partialTicks) {
        int result = super.shouldRenderPass(entity, pass, partialTicks);
        if (result > 0) this.bindTexture(((ISpecialMob)entity).getSpecialData().getTexture(1));
        return result;
    }
}
