/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Smart Moving 15.8.2-maka's diving animation:
 * https://github.com/makamys/SmartMoving/blob/4fe6a9dd080162859b3630b6a886bfe47d50f009/src/main/java/net/smart/moving/render/SmartMovingModel.java
 */
package net.gobbob.mobends.animation.player;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

/** A directional, streamlined pose used while travelling fully underwater. */
public class Animation_Diving extends net.gobbob.mobends.animation.Animation {
    @Override
    public String getName() {
        return "diving";
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/dive");

    @Override
    public void animate(EntityLivingBase entity, ModelBase baseModel, EntityData entityData) {
        play((ModelBendsPlayer)baseModel, (Data_Player)entityData);
    }

    /** How strongly the legs kick: 0 when still, 1 from 0.18 blocks per tick. */
    public static float moving(float motionX, float motionY, float motionZ) {
        double totalSpeed = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        return MathHelper.clamp_float((float)(totalSpeed / 0.18), 0.0f, 1.0f);
    }

    /** Degrees the body leans below horizontal along the travel direction, within +-35. */
    public static float travelPitch(float motionX, float motionY, float motionZ) {
        double horizontalSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);
        float travelPitch = (float)Math.toDegrees(Math.atan2(-motionY, Math.max(horizontalSpeed, 0.02)));
        return MathHelper.clamp_float(travelPitch, -35.0f, 35.0f);
    }

    /**
     * Plays the Blockbench dive from {@code player/dive.animation.json}: {@code dive} for the upper body (with a
     * {@code dive_holding} version for an occupied hand, if present), and the legs kicking {@code dive_legs} over
     * {@code dive_legs_still} as strongly as the player moves. Falls back to the Java dive below if it is missing.
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        // The lean eases like the Java dive's body tilt did, before Molang reads it.
        data.divingTravelPitch += (travelPitch(data.motion.x, data.motion.y, data.motion.z) - data.divingTravelPitch)
            * Math.min(1.0f, data.ticksPerFrame * 0.25f);
        String upper = BendsAnimator.variant(GeckoLibResources.getBakedAnimations().getAnimations(FILE, null),
            "animation.mobends.player.dive", BendsAnimator.isHolding(data));
        if (!loop(model, data, upper, "dive_upper", 1)
            || !loop(model, data, "animation.mobends.player.dive_legs_still", "dive_legs_still", 1)
            || !loop(model, data, "animation.mobends.player.dive_legs", "dive_legs",
                moving(data.motion.x, data.motion.y, data.motion.z))) {
            apply(model, data.ticks, data.motion.x, data.motion.y, data.motion.z);
        }
    }

    private static boolean loop(ModelBendsPlayer model, Data_Player data, String animation, String slot, float weight) {
        Animation loaded = BendsAnimator.find(FILE, animation);
        if (loaded == null) return false;
        double seconds = loaded.length() > 0 ? (data.ticks / 20.0) % loaded.length() : 0;
        return BendsAnimator.play(model, data, FILE, animation, seconds, slot, 0.5, weight);
    }

    /** The original Java dive, kept as the reference the Blockbench dive was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, float motionX, float motionY, float motionZ) {
        float moving = moving(motionX, motionY, motionZ);
        float phase = ticks * 0.24f;
        float armStroke = (MathHelper.cos(phase) + 1.0f) / 2.0f;
        float legKick = MathHelper.cos(phase * 1.8f) * moving;

        float travelPitch = travelPitch(motionX, motionY, motionZ);
        float bodyTilt = 80.0f + travelPitch;
        model.renderRotation.setSmoothX(bodyTilt, 0.25f);
        model.renderOffset.setSmoothZ(10.0f, 0.25f);

        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.3f);

        // Use a full breaststroke underwater. The first pass held the arms nearly straight,
        // which read as gliding instead of swimming.
        float upperArmPitch = -45.0f - armStroke * 120.0f;
        float armSpread = armStroke * 20.0f;
        float strokeProgress = (phase % ((float)Math.PI * 2.0f)) / ((float)Math.PI * 2.0f);
        float elbowBend = Math.min(armStroke * 2.0f - 1.0f, 0.0f) * -60.0f;
        if (strokeProgress >= 0.55f && strokeProgress <= 0.9f) {
            elbowBend = -60.0f;
        }

        ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothY(-90.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothY(90.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothZ(armSpread, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothZ(-armSpread, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(upperArmPitch, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(upperArmPitch, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(elbowBend, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(elbowBend, 0.3f);

        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(legKick * 35.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-legKick * 35.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(3.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-3.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(
            8.0f + Math.max(0.0f, -legKick) * 24.0f,
            0.3f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(
            8.0f + Math.max(0.0f, legKick) * 24.0f,
            0.3f);

        ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - bodyTilt * 0.45f, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
    }
}
