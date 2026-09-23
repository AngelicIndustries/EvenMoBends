/*
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * Adapted from Smart Moving 15.8.2-maka's falling animation:
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

/** A spread, slowly shifting pose for a sustained downward fall. */
public class Animation_Falling extends Animation {
    @Override
    public String getName() {
        return "falling";
    }

    @Override
    public void animate(EntityLivingBase entity, ModelBase baseModel, EntityData entityData) {
        play((ModelBendsPlayer)baseModel, (Data_Player)entityData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/fall");

    /** Plays {@code player/fall.animation.json}'s {@code fall}, looping on the player's age. */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        BendsAnimator.playLoop(model, data, FILE, "fall", "fall");
    }
}
