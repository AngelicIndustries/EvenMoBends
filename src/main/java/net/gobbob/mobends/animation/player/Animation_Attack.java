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
import net.gobbob.mobends.client.animation.PlayerAnimator;
import net.gobbob.mobends.client.animation.PlayerRig;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.pack.BendsPack;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
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

    /** A sword leaves a trail through its swing (the first only in its first 4 ticks). */
    private static void trail(EntityPlayer player, ModelBendsPlayer model, Data_Player data, int attack) {
        if (data.ticksAfterPunch < 0.5f) model.swordTrail.reset();
        ItemStack held = player.getCurrentEquippedItem();
        if (held != null && held.getItem() instanceof ItemSword && (attack != 1 || data.ticksAfterPunch < 4.0f)) {
            model.swordTrail.add(model);
        }
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
        String name = "animation.mobends.player.punch_" + (data.fistPunchArm || offhand ? "right" : "left") + (moving ? "_moving" : "");
        return PlayerAnimator.play(model, data, PlayerAnimator.PUNCH, name, data.ticksAfterPunch / 20.0, null, 0, 1,
            PlayerAnimator.freeHandsOnly(data, null));
    }
}
