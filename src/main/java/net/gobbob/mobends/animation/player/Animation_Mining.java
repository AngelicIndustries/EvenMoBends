package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.animation.HandAction;
import net.gobbob.mobends.client.animation.PlayerRig;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
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
        EntityPlayer player = (EntityPlayer)argEntity;
        boolean planted = plants(player.getCurrentEquippedItem(), HandAction.isBreakingBlock(player));
        play((ModelBendsPlayer)argModel, (Data_Player)argData, player.isSwingInProgress, planted);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/mine");

    /** The root and legs: the mining stance's lowered body and spread feet. */
    public static final Set<String> FEET = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        PlayerRig.ROOT, PlayerRig.Bone.RIGHT_LEG.id, PlayerRig.Bone.RIGHT_FORE_LEG.id, PlayerRig.Bone.LEFT_LEG.id,
        PlayerRig.Bone.LEFT_FORE_LEG.id)));

    /**
     * Whether mining with {@code item} plants the feet: a pickaxe does, a block only while it breaks one. Holding or
     * placing a block leaves the legs to walking and standing.
     */
    public static boolean plants(ItemStack item, boolean breaking) {
        return breaking || item == null || Block.getBlockFromItem(item.getItem()) == Blocks.air;
    }

    /**
     * Plays {@code player/mine.animation.json}: the stance {@code mine} while holding a pickaxe, or a block that is
     * breaking, and {@code mine_swing} over it while swinging, looping on the player's age; unplanted, the swing
     * moves the arms only. An occupied off hand keeps its arm. A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean swinging, boolean planted) {
        Set<String> without = BendsAnimator.freeHandsOnly(data, planted ? null : FEET);
        if (planted) BendsAnimator.playPose(model, data, FILE, "mine", "tool", 1, without);
        if (swinging) BendsAnimator.playLoop(model, data, FILE, "mine_swing", "tool_swing", 1, without);
    }
}
