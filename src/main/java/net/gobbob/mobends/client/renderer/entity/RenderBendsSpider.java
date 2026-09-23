package net.gobbob.mobends.client.renderer.entity;

import net.gobbob.mobends.data.Data_Spider;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.model.entity.ModelBendsSpider;
import net.gobbob.mobends.compat.CemCompat;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.EntityLiving;

public class RenderBendsSpider
extends RenderSpider {
    public int refreshModel = 0;

    public RenderBendsSpider() {
        this.mainModel = new ModelBendsSpider();
        this.setRenderPassModel(new ModelBendsSpider());
    }

    private RenderSpider vanilla;

    /** The vanilla renderer this spider steps aside to when a resource pack's CEM model takes it over. */
    protected RenderSpider createVanillaRenderer() {
        return new RenderSpider();
    }

    public void doRender(EntityLiving p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        if (CemCompat.hasModel(p_76986_1_)) {
            // A pack model (Fresh Animations, say) supersedes Mo' Bends: the vanilla renderer draws the spider, and
            // Salamander swaps in the pack's model, texture and eyes there. Checked every frame, so pack reloads and
            // Salamander's CEM switch take effect at once.
            if (this.vanilla == null) this.vanilla = this.createVanillaRenderer();
            this.vanilla.setRenderManager(this.renderManager);
            this.vanilla.doRender(p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
            return;
        }
        if (this.refreshModel != MoBends.refreshModel) {
            this.mainModel = new ModelBendsSpider();
            this.setRenderPassModel(new ModelBendsSpider());
            this.refreshModel = MoBends.refreshModel;
        }
        super.doRender(p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
    }

    /** Then the root's transform from the spider's animations, as the player's renderer applies it. */
    @Override
    protected void rotateCorpse(EntityLivingBase entity, float age, float yaw, float partialTicks) {
        super.rotateCorpse(entity, age, yaw, partialTicks);
        Data_Spider data = Data_Spider.get(entity.getEntityId());
        GL11.glTranslatef(data.renderOffset.getX() * 0.0625f, data.renderOffset.getY() * 0.0625f, data.renderOffset.getZ() * 0.0625f);
        GL11.glRotatef(-data.renderRotation.getX(), 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(-data.renderRotation.getY(), 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(data.renderRotation.getZ(), 0.0f, 0.0f, 1.0f);
    }

}
