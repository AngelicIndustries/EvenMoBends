package net.gobbob.mobends.animation.player;

import java.util.Collections;
import java.util.Set;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo0;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo1;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo2;
import net.gobbob.mobends.animation.player.Animation_Attack_Punch;
import net.gobbob.mobends.animation.player.Animation_Attack_PunchStance;
import net.gobbob.mobends.animation.player.Animation_Attack_Stance;
import net.gobbob.mobends.client.animation.HandAction;
import net.gobbob.mobends.client.animation.PlayerAnimator;
import net.gobbob.mobends.client.animation.PlayerRig;
import net.gobbob.mobends.client.renderer.SwordTrail;
import net.gobbob.mobends.compat.BackhandCompat;
import net.gobbob.mobends.compat.CompatibilityPolicy;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.pack.BendsPack;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.util.ResourceLocation;

public class Animation_Attack
extends Animation {
    @Override
    public String getName() {
        return "attack";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        ModelBendsPlayer model = (ModelBendsPlayer)argModel;
        Data_Player data = (Data_Player)argData;
        EntityPlayer player = (EntityPlayer)argEntity;
        if (player.getCurrentEquippedItem() != null) {
            boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
            if (data.ticksAfterPunch < 10.0f) {
                int attack = data.currentAttack;
                if (attack >= 1 && attack <= 3 && PlayerAnimationConfig.isEnabled("swordCombo" + attack)) {
                    trail(player, model, data, attack);
                    if (!playSword(model, data, attack, moving, player.isRiding())) {
                        if (attack == 1) Animation_Attack_Combo0.apply(model, data.ticksAfterPunch, moving, player.isRiding());
                        else if (attack == 2) Animation_Attack_Combo1.apply(model, data.ticksAfterPunch, moving, player.isRiding());
                        else Animation_Attack_Combo2.apply(model, data.ticksAfterPunch);
                    }
                    BendsPack.animate(model, "player", "attack_" + (attack - 1));
                }
            } else if (data.ticksAfterPunch < 60.0f) {
                if (PlayerAnimationConfig.isEnabled("swordStance")) {
                    if (data.isOnGround() && !playStance(model, data, moving, player.isSprinting())) {
                        Animation_Attack_Stance.apply(model, moving, player.isSprinting());
                    }
                    BendsPack.animate(model, "player", "attack_stance");
                }
            }
        } else if (data.ticksAfterPunch < 10.0f) {
            if (PlayerAnimationConfig.isEnabled("punch")) {
                if (!playPunch(model, data, false)) Animation_Attack_Punch.animate((EntityPlayer)argEntity, model, data);
                BendsPack.animate(model, "player", "punch");
            }
        } else if (data.ticksAfterPunch < 60.0f) {
            if (PlayerAnimationConfig.isEnabled("punchStance")) {
                if (!playPunch(model, data, true)) Animation_Attack_PunchStance.animate((EntityPlayer)argEntity, model, data);
                BendsPack.animate(model, "player", "punch_stance");
            }
        }
    }

    /**
     * Backhand's off hand, over whatever the main hand does, for {@link #OFFHAND_TICKS} after it starts a swing: an
     * empty hand punches ({@code punch_left}); a pickaxe mines, a block mines while breaking or placing, an axe chops
     * while breaking, and anything else swings through the sword combo (a sword or an axe leaving a trail), each the
     * main hand's animation mirrored, then settles into the mirrored stance with the left arm only. While the main
     * hand holds something its arm is left alone; while the main hand acts too, whichever hand started last leads the
     * body and the other moves only its arm. Riding leaves the root to the mount.
     */
    public static void playOffhand(EntityPlayer player, ModelBendsPlayer model, Data_Player data) {
        if (!CompatibilityPolicy.isBackhandLoaded()) return;
        ItemStack item = BackhandCompat.getOffhandItem(player);
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        // As in the main hand: a pickaxe mines, a block mines while breaking or placing (attacking otherwise; other
        // players' swings count as placing), an axe chops only while breaking; anything else swings like a sword.
        boolean breaking = HandAction.isBreakingWithOffhand(player) || data.offhandSwingBreaks && data.ticksAfterOffhandSwing < 60.0f;
        boolean attacking = data.offhandSwingKnown && !data.offhandSwingIsUse && !breaking;
        boolean tool = item != null && (item.getItem() instanceof ItemPickaxe
            || item.getItem() instanceof ItemAxe && breaking
            || Block.getBlockFromItem(item.getItem()) != Blocks.air && !attacking);
        // Tools mine or chop as long as the hand keeps swinging; punches and sword swings last their own time.
        float since = tool ? data.ticksAfterOffhandSwing : data.ticksAfterOffhandPunch;
        if (since >= OFFHAND_TICKS) {
            if (item != null && !tool && data.ticksAfterOffhandPunch < 60.0f && data.isOnGround() && (!moving || player.isSprinting())
                && PlayerAnimationConfig.isEnabled("swordStance")) {
                PlayerAnimator.playPose(model, data, SWORD, moving ? "sword_stance_sprint" : "sword_stance", "offhand_stance", 1,
                    PlayerAnimator.ALL_BUT_LEFT_ARM, true);
            }
            return;
        }
        boolean mainLeads = data.ticksAfterPunch < since && data.ticksAfterPunch < OFFHAND_TICKS;
        Set<String> without = PlayerAnimator.ALL_BUT_LEFT_ARM;
        if (!mainLeads) {
            Set<String> keepMain = player.getHeldItem() != null ? PlayerAnimator.RIGHT_ARM : null;
            without = player.isRiding() ? PlayerAnimator.union(RIDING_MASK, keepMain) : keepMain;
        }
        double seconds = data.ticksAfterOffhandPunch / 20.0;
        if (item == null) {
            if (PlayerAnimationConfig.isEnabled("punch")) {
                if (data.offhandPunches != data.seenOffhandPunches) {
                    data.seenOffhandPunches = data.offhandPunches;
                    data.offhandPunchRetracts = retracts(data, false, data.ticksAfterOffhandPunch);
                }
                PlayerAnimator.playOnce(model, data, PlayerAnimator.PUNCH, "animation.mobends.player.punch_left" + (moving ? "_moving" : ""),
                    punchSeconds(data.ticksAfterOffhandPunch, data.offhandPunchRetracts), "offhand",
                    punchFade(data.offhandPunchRetracts), without, false, data.offhandPunches);
            }
        } else if (tool && item.getItem() instanceof ItemAxe) {
            if (PlayerAnimationConfig.isEnabled("axe")) {
                model.offhandTrail.add(SwordTrail.AXE);
                PlayerAnimator.playLoop(model, data, Animation_Axe.FILE, "axe_swing", "offhand", 1, without, true);
            }
        } else if (tool) {
            if (PlayerAnimationConfig.isEnabled("mining")) {
                PlayerAnimator.playLoop(model, data, Animation_Mining.FILE, "mine_swing", "offhand", 1, without, true);
            }
        } else {
            int attack = data.currentOffhandAttack;
            if (attack >= 1 && attack <= 3 && PlayerAnimationConfig.isEnabled("swordCombo" + attack)) {
                trail(model.offhandTrail, item, data.ticksAfterOffhandPunch, attack);
                String name = "animation.mobends.player.sword_" + attack + (attack < 3 && (moving || player.isRiding()) ? "_moving" : "");
                PlayerAnimator.playOnce(model, data, SWORD, name, seconds, "offhand", SWING_FADE, without, true);
            }
        }
    }

    /** How long an off-hand swing plays: a swing is 10 ticks, and mining or chopping keeps restarting it. */
    public static final float OFFHAND_TICKS = 10;

    public static final ResourceLocation SWORD = new ResourceLocation(MoBends.MODID, "player/sword");
    /** While riding, the mount places the body: swings leave the root alone. */
    private static final Set<String> RIDING_MASK = Collections.singleton(PlayerRig.ROOT);

    /**
     * Plays swing {@code attack} (1 to 3) from {@code player/sword.animation.json}, timed from the click:
     * {@code sword_<attack>}, or {@code sword_<attack>_moving} for the first two while moving or riding, when the
     * legs are left to walking (and the root to the mount). Each swing was baked out of the one before it; from
     * anywhere else, where the body was fades out over {@link #SWING_FADE} seconds. False if the animation is missing.
     */
    private static boolean playSword(ModelBendsPlayer model, Data_Player data, int attack, boolean moving, boolean riding) {
        String name = "animation.mobends.player.sword_" + attack + (attack < 3 && (moving || riding) ? "_moving" : "");
        return PlayerAnimator.playOnce(model, data, SWORD, name, data.ticksAfterPunch / 20.0, "attack", SWING_FADE,
            PlayerAnimator.freeHandsOnly(data, riding ? RIDING_MASK : null));
    }

    public static final double SWING_FADE = 0.15;

    /**
     * The stance a swing settles into: {@code sword_stance} standing, {@code sword_stance_sprint} sprinting, none
     * walking. Like the swings, it leaves the left arm alone while the off hand holds something. False if the
     * animation is missing.
     */
    private static boolean playStance(ModelBendsPlayer model, Data_Player data, boolean moving, boolean sprinting) {
        if (moving && !sprinting) return true;
        return PlayerAnimator.playPose(model, data, SWORD, moving ? "sword_stance_sprint" : "sword_stance", "attack", 1,
            PlayerAnimator.freeHandsOnly(data, null));
    }

    /** A sword or axe leaves a trail through its swing (the first only in its first 4 ticks). */
    private static void trail(EntityPlayer player, ModelBendsPlayer model, Data_Player data, int attack) {
        trail(model.swordTrail, player.getCurrentEquippedItem(), data.ticksAfterPunch, attack);
    }

    private static void trail(SwordTrail trail, ItemStack held, float ticks, int attack) {
        if (ticks < 0.5f) trail.reset();
        if (held != null && (held.getItem() instanceof ItemSword || held.getItem() instanceof ItemAxe)
            && (attack != 1 || ticks < 4.0f)) trail.add(SwordTrail.edgeOf(held));
    }

    /**
     * Plays the Blockbench punch from {@code player/punch.animation.json}; false if it is missing. Standing, one
     * animation covers the punch and the stance it settles into; moving, only the punch plays.
     */
    private static boolean playPunch(ModelBendsPlayer model, Data_Player data, boolean stance) {
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        if (stance && moving) return true;
        // With something in the off hand, only the empty main hand punches, and the off-hand arm keeps holding.
        boolean offhand = PlayerAnimator.isOffhandHolding(data);
        boolean right = data.fistPunchArm || offhand;
        if (data.punches != data.seenPunches) {
            data.seenPunches = data.punches;
            data.punchRetracts = retracts(data, right, data.ticksAfterPunch);
        }
        String name = "animation.mobends.player.punch_" + (right ? "right" : "left") + (moving ? "_moving" : "");
        return PlayerAnimator.playOnce(model, data, PlayerAnimator.PUNCH, name, punchSeconds(data.ticksAfterPunch, data.punchRetracts),
            "punch", punchFade(data.punchRetracts), PlayerAnimator.freeHandsOnly(data, null), false, data.punches);
    }

    /** A fist punching again this soon is still out: it pulls back in first. */
    public static final float FIST_OUT_TICKS = 15;
    /** How long pulling a fist back in takes, before the punch plays from its start. */
    public static final float RETRACT_TICKS = 2;

    /** Whether a punch the {@code right} or left fist started {@code ticksAgo} begins by pulling that fist back in. */
    private static boolean retracts(Data_Player data, boolean right, float ticksAgo) {
        int fist = right ? 0 : 1;
        float start = data.ticks - ticksAgo;
        boolean out = start - data.fistPunchedAt[fist] < FIST_OUT_TICKS;
        data.fistPunchedAt[fist] = start;
        return out;
    }

    /**
     * Where a punch is, {@code ticks} after the click: a fist that has to pull back in first holds the punch's
     * starting pose for {@link #RETRACT_TICKS} (while it fades there from where it was), then punches.
     */
    public static double punchSeconds(float ticks, boolean retracts) {
        return Math.max(0, ticks - (retracts ? RETRACT_TICKS : 0)) / 20.0;
    }

    /** A retracting fist fades back to the punch's start just as the punch begins; a fist already in fades quickly. */
    public static double punchFade(boolean retracts) {
        return retracts ? RETRACT_TICKS / 20.0 : SWING_FADE;
    }
}
