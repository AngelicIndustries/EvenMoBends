package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Climbing.*;

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

/** The original Java climbing (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Climbing {
    private Reference_Climbing() {}

    /** The original Java climb, kept as the reference the Blockbench climb was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, float motionY) {
        float stride = stride(motionY);
        boolean moving = stride > 0.0f;
        float phase = ticks * 0.32f;
        float rightCycle = MathHelper.cos(phase);
        float leftCycle = MathHelper.cos(phase + (float)Math.PI);

        float rightArmPitch = moving ? -125.0f + rightCycle * 38.0f * stride : -125.0f;
        float leftArmPitch = moving ? -125.0f + leftCycle * 38.0f * stride : -125.0f;
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(rightArmPitch, 0.35f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(leftArmPitch, 0.35f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(-8.0f, 0.35f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(8.0f, 0.35f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(12.0f, 0.35f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-12.0f, 0.35f);

        float rightElbow = moving ? -35.0f - rightCycle * 22.0f * stride : -35.0f;
        float leftElbow = moving ? -35.0f - leftCycle * 22.0f * stride : -35.0f;
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(rightElbow, 0.35f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(leftElbow, 0.35f);

        float rightLegPitch = moving ? 12.0f + leftCycle * 32.0f * stride : 12.0f;
        float leftLegPitch = moving ? 12.0f + rightCycle * 32.0f * stride : 12.0f;
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(rightLegPitch, 0.35f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(leftLegPitch, 0.35f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(-6.0f, 0.35f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(6.0f, 0.35f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(8.0f, 0.35f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-8.0f, 0.35f);

        float rightKnee = moving ? 28.0f - leftCycle * 18.0f * stride : 28.0f;
        float leftKnee = moving ? 28.0f - rightCycle * 18.0f * stride : 28.0f;
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(rightKnee, 0.35f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(leftKnee, 0.35f);

        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(5.0f, 0.35f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.35f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.35f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - 5.0f, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
    }
}
