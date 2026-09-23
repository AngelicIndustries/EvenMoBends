package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Jump.*;

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

/** The original Java jump (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Jump {
    private Reference_Jump() {}

    /** The original Java jump, kept as the reference the Blockbench jump was baked from. */
    public static void apply(ModelBendsPlayer model, float ticksAfterLiftoff, boolean moving, boolean sprinting,
        boolean sprintJumpLeg, float motionY) {
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.1f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(45.0f, 0.05f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-45.0f, 0.05f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(0.0f, 0.5f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(0.0f, 0.5f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(10.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-10.0f, 0.1f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-20.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-20.0f, 0.1f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-45.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-45.0f, 0.1f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(50.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(50.0f, 0.1f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - model.bipedBody.rotateAngleX, 0.3f);
        if (ticksAfterLiftoff < 2.0f) {
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(20.0f, 2.0f);
            ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(0.0f, 2.0f);
            ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(0.0f, 2.0f);
            ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(0.0f, 2.0f);
            ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(0.0f, 2.0f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(2.0f, 2.0f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-2.0f, 2.0f);
            ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(-20.0f, 2.0f);
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(-20.0f, 2.0f);
        }
        if (moving) {
            if (sprinting) {
                float bodyRot = 0.0f;
                float bodyLean = motionY;
                if (bodyLean < -0.2f) {
                    bodyLean = -0.2f;
                }
                if (bodyLean > 0.2f) {
                    bodyLean = 0.2f;
                }
                bodyLean = bodyLean * -100.0f + 20.0f;
                ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(bodyLean, 0.3f);
                ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(5.0f, 0.3f);
                ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-5.0f, 0.3f);
                ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(10.0f, 0.3f);
                ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-10.0f, 0.3f);
                if (sprintJumpLeg) {
                    ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-45.0f, 0.4f);
                    ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(45.0f, 0.4f);
                    ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(50.0f, 0.3f);
                    ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-50.0f, 0.3f);
                    bodyRot = 20.0f;
                } else {
                    ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(45.0f, 0.4f);
                    ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-45.0f, 0.4f);
                    ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-50.0f, 0.3f);
                    ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(50.0f, 0.3f);
                    bodyRot = -20.0f;
                }
                ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(bodyRot, 0.3f);
                ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY - bodyRot, 0.3f);
                ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - 20.0f, 0.3f);
                float var = model.bipedRightLeg.rotateAngleX;
                ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(var < 0.0f ? 45 : 2, 0.3f);
                ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(var < 0.0f ? 2 : 45, 0.3f);
            } else {
                ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-5.0f + 0.5f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
                ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-5.0f + 0.5f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f + (float)Math.PI) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
                float var = (float)((double)(model.armSwing * 0.6662f) / Math.PI) % 2.0f;
                ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(var > 1.0f ? 45 : 0, 0.3f);
                ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(var > 1.0f ? 0 : 45, 0.3f);
                ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX((float)(Math.cos((double)(model.armSwing * 0.6662f) + 1.5707963267948966) + 1.0) / 2.0f * -20.0f, 1.0f);
                ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX((float)(Math.cos(model.armSwing * 0.6662f) + 1.0) / 2.0f * -20.0f, 0.3f);
            }
        }
    }
}
