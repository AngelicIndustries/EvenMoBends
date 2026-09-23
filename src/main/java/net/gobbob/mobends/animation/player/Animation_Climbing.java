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
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;

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
     * player's age) over it as fast as the player climbs. A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        BendsAnimator.playPose(model, data, FILE, "climb", "climb");
        BendsAnimator.playLoop(model, data, FILE, "climb_move", "climb_move", stride(data.motion.y));
    }
}
