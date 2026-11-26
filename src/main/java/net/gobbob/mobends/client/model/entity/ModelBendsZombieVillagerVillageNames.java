package net.gobbob.mobends.client.model.entity;

import astrotibs.villagenames.client.model.ModelZombieVillagerModern;
import astrotibs.villagenames.config.GeneralConfig;
import astrotibs.villagenames.ieep.ExtendedZombieVillager;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import org.lwjgl.opengl.GL11;

/**
 * Village Names' modern zombie villager (the 1.14 look, as Et Futurum's) on the zombie's rig, with Village Names' own
 * profession headwear and hat rims drawn on the bent head, under the same rules as its model.
 */
public class ModelBendsZombieVillagerVillageNames
extends ModelBendsZombieVillagerEtFuturum {
    /** Village Names' model, for its headwear and hat rim parts only. */
    private final ModelZombieVillagerModern hats = new ModelZombieVillagerModern(0.0f);

    public ModelBendsZombieVillagerVillageNames() {
        super(0.0f);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSpeed, float age, float headYaw, float headPitch, float scale) {
        super.render(entity, limbSwing, limbSpeed, age, headYaw, headPitch, scale);
        if (!(entity instanceof EntityZombie) || !((EntityZombie)entity).isVillager() || !this.drawsHeadwear((EntityZombie)entity)) return;
        if (this.bipedHead.isHidden || !this.bipedHead.showModel) return;
        String bone = this.geometry().rig.name(this.bipedHead);
        if (bone == null) return;
        ModelRendererBends head = (ModelRendererBends)this.bipedHead;
        GL11.glPushMatrix();
        try {
            if (this.isChild) {
                // As ModelBendsZombie draws a child: everything at half size, the head half as big again.
                GL11.glScalef(0.5f, 0.5f, 0.5f);
                GL11.glTranslatef(0.0f, 24.0f * scale, 0.0f);
                head.scaleX *= 1.5f;
                head.scaleY *= 1.5f;
                head.scaleZ *= 1.5f;
            }
            try {
                this.geometry().attach(bone, scale);
            } finally {
                if (this.isChild) {
                    head.scaleX /= 1.5f;
                    head.scaleY /= 1.5f;
                    head.scaleZ /= 1.5f;
                }
            }
            // The bone already turned them with the head.
            for (ModelRenderer part : new ModelRenderer[] {this.hats.zombieVillagerHeadwear,
                this.hats.zombieVillagerHatRimHigh, this.hats.zombieVillagerHatRimLow}) {
                part.rotateAngleX = part.rotateAngleY = part.rotateAngleZ = 0.0f;
                part.render(scale);
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** As Village Names' model decides: vanilla professions and children always, other mods' as its config says. */
    private boolean drawsHeadwear(EntityZombie zombie) {
        ExtendedZombieVillager data = ExtendedZombieVillager.get(zombie);
        if (data == null) return false;
        int profession = data.getProfession();
        return !(profession > 5 && !this.isChild && !GeneralConfig.moddedVillagerHeadwearWhitelist.contains(profession)
            && (GeneralConfig.moddedVillagerHeadwearBlacklist.contains(profession) || !GeneralConfig.moddedVillagerHeadwear));
    }
}
