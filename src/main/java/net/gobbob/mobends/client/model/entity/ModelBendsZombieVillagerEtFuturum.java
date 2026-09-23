package net.gobbob.mobends.client.model.entity;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.minecraft.client.model.ModelRenderer;

/**
 * Et Futurum Requiem's zombie villager (its 1.14 look: the villager's taller head and nose, a deeper body under a
 * robe) on the zombie's rig, so it animates like any zombie. Arms and legs split at the elbow and knee as the zombie's
 * do, from the same texture layout as Et Futurum's own model.
 */
public class ModelBendsZombieVillagerEtFuturum
extends ModelBendsZombie {
    public ModelBendsZombieVillagerEtFuturum() {
        this(0.0f);
    }

    public ModelBendsZombieVillagerEtFuturum(float scale) {
        super(scale, 0.0f, 64, 64);
        reshape(this.bipedHead, 0, 0, -4, -10, -4, 8, 10, 8, scale);
        ModelRendererBends nose = new ModelRendererBends(this, 24, 0);
        nose.addBox(-1.0f, -1.0f, -6.0f, 2, 4, 2, scale);
        nose.setRotationPoint(0.0f, -2.0f, 0.0f);
        this.bipedHead.addChild(nose);
        this.bipedHeadwear.isHidden = true;

        reshape(this.bipedBody, 16, 20, -4, -12, -3, 8, 12, 6, scale);
        this.bipedBody.setTextureOffset(0, 38);
        this.bipedBody.addBox(-4.0f, -12.0f, -3.0f, 8, 18, 6, scale + 0.5f);

        arm(this.bipedRightArm, this.bipedRightForeArm, -3.0f, scale);
        arm(this.bipedLeftArm, this.bipedLeftForeArm, -1.0f, scale);
        leg(this.bipedRightLeg, this.bipedRightForeLeg, scale);
        leg(this.bipedLeftLeg, this.bipedLeftForeLeg, scale);
    }

    private static void reshape(ModelRenderer part, int u, int v, float x, float y, float z, int w, int h, int d, float scale) {
        part.cubeList.clear();
        part.setTextureOffset(u, v);
        part.addBox(x, y, z, w, h, d, scale);
    }

    /** An arm from Et Futurum's 4 x 12 x 4 arm texture at (44, 38), split at the elbow as the zombie's. */
    private static void arm(ModelRenderer upper, ModelRenderer lower, float x, float scale) {
        reshape(upper, 44, 38, x, -2.0f, -2.0f, 4, 6, 4, scale);
        joint(upper);
        reshape(lower, 44, 44, 0.0f, 0.0f, -4.0f, 4, 6, 4, scale);
        bottom(lower);
    }

    /** A leg from the 4 x 12 x 4 leg texture at (0, 22), split at the knee as the zombie's. */
    private static void leg(ModelRenderer upper, ModelRenderer lower, float scale) {
        reshape(upper, 0, 22, -2.0f, 0.0f, -2.0f, 4, 6, 4, scale);
        joint(upper);
        reshape(lower, 0, 28, -2.0f, 0.0f, 0.0f, 4, 6, 4, scale);
        bottom(lower);
    }

    /** Widened a hair against z-fighting where the halves meet, as the zombie's. */
    private static void joint(ModelRenderer upper) {
        ((ModelRendererBends)upper).offsetBox_Add(-0.01f, 0.0f, -0.01f).resizeBox(4.02f, 6.0f, 4.02f).updateVertices();
    }

    /** The lower half's bottom face shows the limb's real bottom, 6 px up the texture. */
    private static void bottom(ModelRenderer lower) {
        ((ModelRendererBends)lower).getBox().offsetTextureQuad(lower, 3, 0.0f, -6.0f);
    }
}
