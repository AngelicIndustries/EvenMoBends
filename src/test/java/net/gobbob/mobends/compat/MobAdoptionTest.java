package net.gobbob.mobends.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;

class MobAdoptionTest {

    /** As Twilight Forest's and Natura's spiders: a texture, a size, nothing else. */
    static class TexturedSpider extends RenderSpider {
        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return new ResourceLocation("test:spider.png");
        }

        @Override
        protected void preRenderCallback(EntityLivingBase entity, float partialTicks) {}
    }

    static class DrawingSpider extends TexturedSpider {
        @Override
        public void doRender(EntityLiving entity, double x, double y, double z, float yaw, float partialTicks) {}
    }

    static class TexturedZombie extends RenderBiped {
        TexturedZombie(ModelBiped model) {
            super(model, 0.5f);
        }

        @Override
        protected ResourceLocation getEntityTexture(Entity entity) {
            return new ResourceLocation("test:zombie.png");
        }
    }

    /** An overlay pass of its own: Mo' Bends draws zombies' passes (armor) on its rig, so this one stays. */
    static class OverlayZombie extends TexturedZombie {
        OverlayZombie() {
            super(new ModelZombie());
        }

        @Override
        protected int shouldRenderPass(EntityLiving entity, int pass, float partialTicks) {
            return -1;
        }
    }

    /** As Twilight Forest's lich minion: the zombie's boxes, only a tint of its own. */
    static class TintedZombieModel extends ModelZombie {
        @Override
        public void setLivingAnimations(EntityLivingBase entity, float swing, float amount, float partialTicks) {}
    }

    static class HornedZombieModel extends ModelZombie {
        HornedZombieModel() {
            ModelRenderer horn = new ModelRenderer(this, 0, 0);
            horn.addBox(-1, -12, -1, 2, 4, 2);
            this.bipedHead.addChild(horn);
        }
    }

    @Test
    void vanillaModelRenderersThatOnlyPickTextureAndSizeAreAdopted() {
        assertTrue(MobAdoption.adoptable(MobAdoption.Family.SPIDER, new TexturedSpider()));
        assertTrue(MobAdoption.adoptable(MobAdoption.Family.ZOMBIE, new TexturedZombie(new ModelZombie())));
        assertTrue(MobAdoption.adoptable(MobAdoption.Family.ZOMBIE, new TexturedZombie(new TintedZombieModel())));
    }

    @Test
    void renderersThatDrawMoreOrModelsWithPartsOfTheirOwnKeepTheirMobs() {
        assertFalse(MobAdoption.adoptable(MobAdoption.Family.SPIDER, new DrawingSpider()));
        assertFalse(MobAdoption.adoptable(MobAdoption.Family.ZOMBIE, new OverlayZombie()));
        assertFalse(MobAdoption.adoptable(MobAdoption.Family.ZOMBIE, new TexturedZombie(new HornedZombieModel())));
        // A spider renderer can't stand in for a zombie, nor the other way round.
        assertFalse(MobAdoption.adoptable(MobAdoption.Family.ZOMBIE, new TexturedSpider()));
        assertFalse(MobAdoption.adoptable(MobAdoption.Family.SPIDER, new TexturedZombie(new ModelZombie())));
    }

    @Test
    void geometryIsComparedByBoxesAndTextureSize() {
        assertTrue(MobAdoption.sameGeometry(new ModelZombie(), new ModelZombie()));
        // A plain biped maps a 64x32 texture, the zombie a 64x64 one: Mo' Bends' zombie couldn't draw it.
        assertFalse(MobAdoption.sameGeometry(new ModelBiped(), new ModelZombie()));
        // Inflated boxes, as armor layers have, aren't the mob's own model.
        assertFalse(MobAdoption.sameGeometry(new ModelBiped(0.5f), new ModelZombie()));
        assertFalse(MobAdoption.sameGeometry(new ModelSpider(), new ModelZombie()));
    }
}
