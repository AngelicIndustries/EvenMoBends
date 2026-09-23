package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Falling.*;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

/** The original Java falling (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Falling {
    private Reference_Falling() {}

    /** The original Java fall, kept as the reference the Blockbench fall was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks) {
        float phase = ticks * 0.1f;

        float armTurn = MathHelper.cos(phase + (float)Math.PI / 2.0f) * 22.5f;
        float armSpread = MathHelper.cos(phase) * 45.0f;
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(armTurn, 0.25f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(armTurn, 0.25f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(90.0f + armSpread, 0.25f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-90.0f + armSpread, 0.25f);

        float elbowWave = MathHelper.cos(phase + (float)Math.PI / 2.0f) * 10.0f;
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(-20.0f + elbowWave, 0.25f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(-20.0f - elbowWave, 0.25f);

        float rightLegPitch = MathHelper.cos(phase + (float)Math.PI * 1.5f) * 22.5f + 11.25f;
        float leftLegPitch = MathHelper.cos(phase + (float)Math.PI / 2.0f) * 22.5f + 11.25f;
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(rightLegPitch, 0.25f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(leftLegPitch, 0.25f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(
            MathHelper.cos(phase) * 22.5f + 11.25f,
            0.25f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(
            MathHelper.cos(phase) * 22.5f - 11.25f,
            0.25f);

        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(
            20.0f - MathHelper.cos(phase + (float)Math.PI / 2.0f) * 10.0f,
            0.25f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(
            20.0f + MathHelper.cos(phase + (float)Math.PI / 2.0f) * 10.0f,
            0.25f);

        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
    }
}
