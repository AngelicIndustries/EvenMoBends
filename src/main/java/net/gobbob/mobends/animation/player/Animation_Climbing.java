/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Smart Moving 15.8.2-maka's climbing animation:
 * https://github.com/makamys/SmartMoving/blob/4fe6a9dd080162859b3630b6a886bfe47d50f009/src/main/java/net/smart/moving/render/SmartMovingModel.java
 */
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

/** Alternating hand-and-foot movement for vanilla ladders and vines. */
public class Animation_Climbing extends Animation {
    @Override
    public String getName() {
        return "climbing";
    }

    @Override
    public void animate(EntityLivingBase entity, ModelBase baseModel, EntityData entityData) {
        play((ModelBendsPlayer)baseModel, (Data_Player)entityData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/climb");

    /** How strongly the climbing stroke plays: 0 holding still, 1 from 0.12 blocks per tick. */
    public static float stride(float motionY) {
        float verticalSpeed = Math.abs(motionY);
        return verticalSpeed > 0.015f ? Math.min(verticalSpeed / 0.12f, 1.0f) : 0.0f;
    }

    /**
     * Plays {@code player/climb.animation.json}: {@code climb} holding on and {@code climb_move} (looping on the
     * player's age) over it as fast as the player climbs. Falls back to the Java climb below.
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        if (!BendsAnimator.playPose(model, data, FILE, "climb", "climb")
            || !BendsAnimator.playLoop(model, data, FILE, "climb_move", "climb_move", stride(data.motion.y))) {
            apply(model, data.ticks, data.motion.y);
        }
    }

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
