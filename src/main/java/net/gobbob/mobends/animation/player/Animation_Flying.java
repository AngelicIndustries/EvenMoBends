/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Smart Moving 15.8.2-maka's flying animation:
 * https://github.com/makamys/SmartMoving/blob/4fe6a9dd080162859b3630b6a886bfe47d50f009/src/main/java/net/smart/moving/render/SmartMovingModel.java
 */
package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.PlayerAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/** A streamlined pose for Creative flight and Et Futurum Requiem Elytra gliding. */
public class Animation_Flying extends Animation {
    @Override
    public String getName() {
        return "flying";
    }

    @Override
    public void animate(EntityLivingBase entity, ModelBase baseModel, EntityData entityData) {
        boolean elytraFlying = entity instanceof EntityPlayer
            && EtFuturumRequiemCompat.isElytraFlying((EntityPlayer)entity);
        play((ModelBendsPlayer)baseModel, (Data_Player)entityData, elytraFlying);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/fly");

    /** How far flight has gone from hovering to flying forward: 1 from 0.35 blocks per tick. */
    public static float moving(float motionX, float motionY, float motionZ) {
        double speedSquared = motionX * motionX + motionY * motionY + motionZ * motionZ;
        return MathHelper.clamp_float((float)Math.sqrt(speedSquared) / 0.35f, 0.0f, 1.0f);
    }

    /**
     * Plays {@code player/fly.animation.json}: the hover ({@code fly}, or {@code fly_elytra} gliding with Et Futurum
     * elytra; looping on the player's age) and {@code fly_forward} / {@code fly_elytra_forward} over it as fast as the
     * player flies. Falls back to the Java flight below.
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean elytraFlying) {
        String hover = elytraFlying ? "fly_elytra" : "fly";
        if (!PlayerAnimator.playLoop(model, data, FILE, hover, "fly")
            || !PlayerAnimator.playPose(model, data, FILE, hover + "_forward", "fly_forward",
                moving(data.motion.x, data.motion.y, data.motion.z))) {
            apply(model, data.ticks, data.motion.x, data.motion.y, data.motion.z, elytraFlying);
        }
    }

    /** The original Java flight, kept as the reference the Blockbench flight was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, float motionX, float motionY, float motionZ,
        boolean elytraFlying) {
        float moving = moving(motionX, motionY, motionZ);
        float hovering = 1.0f - moving;
        float phase = ticks * 0.15f;

        // EFR rotates the entire RenderPlayer while gliding. Creative flight needs
        // its own forward tilt, but applying it to EFR would double the rotation.
        float bodyTilt = elytraFlying ? 0.0f : 70.0f * moving;
        model.renderRotation.setSmoothX(bodyTilt, 0.25f);
        model.renderOffset.setSmoothZ(elytraFlying ? 0.0f : 8.0f * moving, 0.25f);

        float armSpread = elytraFlying ? 0.0f : 45.0f + 33.75f * moving;
        float hoverTurn = MathHelper.cos(phase) * 22.5f * hovering;
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(hoverTurn, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(hoverTurn, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(armSpread, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-armSpread, 0.3f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(0.0f, 0.3f);

        float legWave = MathHelper.cos(phase) * 11.25f * hovering;
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(legWave, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-legWave, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(2.8f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-2.8f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(0.0f, 0.3f);

        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.3f);
        float headPitch = elytraFlying ? -45.0f : model.headRotationX - bodyTilt * 0.5f;
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(headPitch, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
    }
}
