package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Sneak.*;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

/** The original Java sneak (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Sneak {
    private Reference_Sneak() {}

    /** The original Java sneak, kept as the reference the Blockbench sneak was baked from. */
    public static void apply(ModelBendsPlayer model) {
        float var = (float)((double)(model.armSwing * 0.6662f) / Math.PI) % 2.0f;
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-5.0f + 1.1f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-5.0f + 1.1f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f + (float)Math.PI) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(10.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-10.0f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-20.0f + 20.0f * MathHelper.cos(model.armSwing * 0.6662f + (float)Math.PI));
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-20.0f + 20.0f * MathHelper.cos(model.armSwing * 0.6662f));
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(var > 1.0f ? 45 : 10, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(var > 1.0f ? 10 : 45, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(var > 1.0f ? -10 : -45, 0.01f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(var > 1.0f ? -45 : -10, 0.01f);
        float var2 = 25.0f + (float)Math.cos(model.armSwing * 0.6662f * 2.0f) * 5.0f;
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(var2);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - var2, 0.3f);
    }
}
