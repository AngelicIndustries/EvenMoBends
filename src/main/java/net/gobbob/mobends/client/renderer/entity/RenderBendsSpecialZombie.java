package net.gobbob.mobends.client.renderer.entity;

import net.minecraft.client.renderer.entity.RenderZombie;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import toast.specialMobs.client.RenderSpecialZombie;
import toast.specialMobs.entity.ISpecialMob;
import toast.specialMobs.entity.SpecialMobData;

/** Special Mobs' zombies and zombie pigmen, with their textures and sizes, animated as zombies. */
public class RenderBendsSpecialZombie extends RenderBendsZombie {

    /** Under a pack model, Special Mobs' own renderer draws them, as it would without Mo' Bends. */
    @Override
    protected RenderZombie createPackRenderer() {
        return new RenderSpecialZombie();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityZombie entity) {
        SpecialMobData data = ((ISpecialMob)entity).getSpecialData();
        return !(entity instanceof EntityPigZombie) && entity.isVillager() ? data.getTexture(1) : data.getTexture();
    }

    @Override
    protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {
        super.preRenderCallback(entity, partialTicks);
        float scale = ((ISpecialMob)entity).getSpecialData().getRenderScale();
        this.shadowSize = 0.5f * scale;
        GL11.glScalef(scale, scale, scale);
    }
}
