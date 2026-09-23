package net.gobbob.mobends.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderCaveSpider;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** The regular spider's bends with the vanilla cave spider appearance and scale. */
public class RenderBendsCaveSpider extends RenderBendsSpider {
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/entity/spider/cave_spider.png");

    public RenderBendsCaveSpider() {
        this.shadowSize *= 0.7f;
    }

    @Override
    protected RenderSpider createVanillaRenderer() {
        return new RenderCaveSpider();
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        GL11.glScalef(0.7f, 0.7f, 0.7f);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntitySpider entity) {
        return TEXTURE;
    }
}
