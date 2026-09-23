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
     * {@code dive_legs_still} as strongly as the player moves. A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        // The lean eases like the Java dive's body tilt did, before Molang reads it.
        data.divingTravelPitch += (travelPitch(data.motion.x, data.motion.y, data.motion.z) - data.divingTravelPitch)
            * Math.min(1.0f, data.ticksPerFrame * 0.25f);
        String upper = BendsAnimator.variant(GeckoLibResources.getBakedAnimations().getAnimations(FILE, null),
            "animation.mobends.player.dive", BendsAnimator.isHolding(data));
        loop(model, data, upper, "dive_upper", 1);
        loop(model, data, "animation.mobends.player.dive_legs_still", "dive_legs_still", 1);
        loop(model, data, "animation.mobends.player.dive_legs", "dive_legs", moving(data.motion.x, data.motion.y, data.motion.z));
    }

    private static boolean loop(ModelBendsPlayer model, Data_Player data, String animation, String slot, float weight) {
        Animation loaded = BendsAnimator.find(FILE, animation);
        if (loaded == null) return false;
        double seconds = loaded.length() > 0 ? (data.ticks / 20.0) % loaded.length() : 0;
        return BendsAnimator.play(model, data, FILE, animation, seconds, slot, 0.5, weight);
    }
}
