package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;

public class Animation_Sneak
extends Animation {
    @Override
    public String getName() {
        return "sneak";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    /**
     * Plays {@code player/sneak.animation.json}. Moving, {@code sneak_walk} (with {@code sneak_walk_swing} by the limbs'
     * swing) replaces the walk underneath with the sneaking gait; standing, {@code sneak} crouches over the stand and
     * leaves the axes it keeps ({@code q.mobends_keep}) to it. Both play at the stride's phase.
     * A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        if (moving) BendsAnimator.playStride(model, data, FILE, "sneak_walk", "sneak");
        else BendsAnimator.playStridePose(model, data, FILE, "sneak", "sneak");
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/sneak");
}

