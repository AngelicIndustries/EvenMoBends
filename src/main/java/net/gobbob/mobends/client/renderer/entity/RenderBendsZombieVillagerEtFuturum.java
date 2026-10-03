package net.gobbob.mobends.client.renderer.entity;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.model.entity.ModelBendsZombie;
import net.gobbob.mobends.client.model.entity.ModelBendsZombieVillagerEtFuturum;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;

/** Et Futurum Requiem's zombie villagers, with its profession textures, animated as zombies. */
public class RenderBendsZombieVillagerEtFuturum
extends RenderBiped {
    /** By Et Futurum's villager type: farmer, librarian, priest, smith, butcher, and the plain villager otherwise. */
    private static final ResourceLocation[] TEXTURES = {
        new ResourceLocation("textures/entity/zombie_villager/zombie_farmer.png"),
        new ResourceLocation("textures/entity/zombie_villager/zombie_librarian.png"),
        new ResourceLocation("textures/entity/zombie_villager/zombie_priest.png"),
        new ResourceLocation("textures/entity/zombie_villager/zombie_smith.png"),
        new ResourceLocation("textures/entity/zombie_villager/zombie_butcher.png"),
        new ResourceLocation("textures/entity/zombie_villager/zombie_villager.png")};
    private int refreshModel = 0;
    private final ganymedes01.etfuturum.client.renderer.entity.VillagerZombieRenderer packRenderer =
        new ganymedes01.etfuturum.client.renderer.entity.VillagerZombieRenderer();

    public RenderBendsZombieVillagerEtFuturum() {
        super(new ModelBendsZombieVillagerEtFuturum(), 0.5f, 1.0f);
    }

    @Override
    protected void func_82421_b() {
        this.field_82423_g = new ModelBendsZombie(1.0f, true);
        this.field_82425_h = new ModelBendsZombie(0.5f, true);
    }

    @Override
    public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {
        if (net.gobbob.mobends.compat.CemCompat.hasModel(entity, this.packRenderer.modelBipedMain, getEntityTexture(entity))) {
            this.packRenderer.setRenderManager(this.renderManager);
            this.packRenderer.doRender(entity, x, y, z, yaw, partialTicks);
            return;
        }
        if (this.refreshModel != MoBends.refreshModel) {
            this.mainModel = this.modelBipedMain = new ModelBendsZombieVillagerEtFuturum();
            this.refreshModel = MoBends.refreshModel;
        }
        super.doRender(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityLiving entity) {
        int type = EtFuturumRequiemCompat.getZombieVillagerType(entity);
        return TEXTURES[type >= 0 && type < TEXTURES.length - 1 ? type : TEXTURES.length - 1];
    }

    @Override
    protected ResourceLocation getEntityTexture(Entity entity) {
        return this.getEntityTexture((EntityLiving)entity);
    }
}
