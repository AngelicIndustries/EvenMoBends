package net.gobbob.mobends.client.renderer.entity;

import ganymedes01.etfuturum.client.renderer.entity.HuskRenderer;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Et Futurum Requiem's husks, with their texture and larger size, animated as zombies. */
public class RenderBendsHuskEtFuturum extends RenderBendsZombie {
    /** As Et Futurum draws them. */
    private static final float SCALE = 1.1f;

    /** Under a pack model, Et Futurum's own renderer draws them, as it would without Mo' Bends. */
    @Override
    protected RenderBiped createPackRenderer() {
        return new HuskRenderer();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie entity) {
        return HuskRenderer.texture;
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        super.preRenderCallback(entity, partialTicks);
        GL11.glScalef(SCALE, SCALE, SCALE);
    }
}
