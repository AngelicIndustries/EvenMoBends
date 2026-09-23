package net.gobbob.mobends.animation.player;

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

public class Animation_Jump
extends Animation {
    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData, argEntity.isSprinting());
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/jump");
    /** No blend in: the launch snaps into its crouch; what the jump holds still eases in as usual. */
    public static final double BLEND = 0;

    /**
     * Plays {@code player/jump.animation.json}, timed from liftoff: sprinting, {@code jump_sprint_right} or
     * {@code jump_sprint_left} by the leg leading this jump; otherwise {@code jump}, with the legs striding
     * ({@code jump_walk}, and {@code jump_walk_swing} by the limbs' swing) while moving. Falls back to the Java jump.
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean sprinting) {
        boolean moving = data.motion.x != 0.0f | data.motion.z != 0.0f;
        double seconds = data.ticksAfterLiftoff / 20.0;
        boolean played;
        if (moving && sprinting) {
            played = BendsAnimator.play(model, data, FILE,
                "animation.mobends.player.jump_sprint_" + (data.sprintJumpLeg ? "right" : "left"), seconds, "jump", BLEND);
        } else {
            played = BendsAnimator.play(model, data, FILE, "animation.mobends.player.jump", seconds, "jump", BLEND)
                && (!moving || BendsAnimator.playStride(model, data, FILE, "jump_walk", "jump_walk"));
        }
        if (!played) apply(model, data.ticksAfterLiftoff, moving, sprinting, data.sprintJumpLeg, data.motion.y);
    }

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

