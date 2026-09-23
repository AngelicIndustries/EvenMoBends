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
import net.minecraft.entity.player.EntityPlayer;

public class Animation_Mining
extends Animation {
    @Override
    public String getName() {
        return "mining";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData, ((EntityPlayer)argEntity).isSwingInProgress);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/mine");

    /**
     * Plays {@code player/mine.animation.json}: the stance {@code mine} while holding a pickaxe or a block, and
     * {@code mine_swing} over it while swinging, looping on the player's age. An occupied off hand
     * keeps its arm. A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean swinging) {
        java.util.Set<String> without = BendsAnimator.freeHandsOnly(data, null);
        BendsAnimator.playPose(model, data, FILE, "mine", "tool", 1, without);
        if (swinging) BendsAnimator.playLoop(model, data, FILE, "mine_swing", "tool_swing", 1, without);
    }
}
