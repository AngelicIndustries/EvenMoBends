package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo0;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo1;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo2;
import net.gobbob.mobends.animation.player.Animation_Attack_Punch;
import net.gobbob.mobends.animation.player.Animation_Attack_PunchStance;
import net.gobbob.mobends.animation.player.Animation_Attack_Stance;
import net.gobbob.mobends.client.animation.PlayerAnimator;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.pack.BendsPack;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

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
            if (data.ticksAfterPunch < 10.0f) {
                if (data.currentAttack == 1) {
                    if (PlayerAnimationConfig.isEnabled("swordCombo1")) {
                        Animation_Attack_Combo0.animate((EntityPlayer)argEntity, model, data);
                        BendsPack.animate(model, "player", "attack_0");
                    }
                } else if (data.currentAttack == 2) {
                    if (PlayerAnimationConfig.isEnabled("swordCombo2")) {
                        Animation_Attack_Combo1.animate((EntityPlayer)argEntity, model, data);
                        BendsPack.animate(model, "player", "attack_1");
                    }
                } else if (data.currentAttack == 3) {
                    if (PlayerAnimationConfig.isEnabled("swordCombo3")) {
                        Animation_Attack_Combo2.animate((EntityPlayer)argEntity, model, data);
                        BendsPack.animate(model, "player", "attack_2");
                    }
                }
            } else if (data.ticksAfterPunch < 60.0f) {
                if (PlayerAnimationConfig.isEnabled("swordStance")) {
                    Animation_Attack_Stance.animate((EntityPlayer)argEntity, model, data);
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
     * Plays the Blockbench punch from {@code player/punch.animation.json}; false if it is missing. Standing, one
     * animation covers the punch and the stance it settles into; moving, only the punch plays.
     */
    private static boolean playPunch(ModelBendsPlayer model, Data_Player data, boolean stance) {
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        if (stance && moving) return true;
        String name = "animation.mobends.player.punch_" + (data.fistPunchArm ? "right" : "left") + (moving ? "_moving" : "");
        return PlayerAnimator.play(model, data, PlayerAnimator.PUNCH, name, data.ticksAfterPunch / 20.0);
    }
}
