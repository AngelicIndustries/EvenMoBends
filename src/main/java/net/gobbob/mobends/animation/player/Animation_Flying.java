/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Smart Moving 15.8.2-maka's flying animation:
 * https://github.com/makamys/SmartMoving/blob/4fe6a9dd080162859b3630b6a886bfe47d50f009/src/main/java/net/smart/moving/render/SmartMovingModel.java
 */
package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
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
     * player flies. A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean elytraFlying) {
        String hover = elytraFlying ? "fly_elytra" : "fly";
        BendsAnimator.playLoop(model, data, FILE, hover, "fly");
        BendsAnimator.playPose(model, data, FILE, hover + "_forward", "fly_forward", moving(data.motion.x, data.motion.y, data.motion.z));
    }
}
