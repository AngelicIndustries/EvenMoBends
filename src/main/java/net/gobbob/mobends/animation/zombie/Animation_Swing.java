package net.gobbob.mobends.animation.zombie;

import net.gobbob.mobends.animation.player.Animation_Attack;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.model.entity.ModelBendsZombie;
import net.gobbob.mobends.data.Data_Zombie;
import net.minecraft.entity.EntityLivingBase;

/**
 * A zombie swinging what it holds (a zombie pigman's sword): the player's sword swings, from
 * {@code player/sword.animation.json}, through the combo one swing per hit, the moving versions while it walks. The
 * zombie has no item bones, so the swings' turns of the sword itself don't apply.
 */
public final class Animation_Swing {
    private Animation_Swing() {}

    public static void play(ModelBendsZombie model, Data_Zombie data, EntityLivingBase zombie) {
        int attack = data.currentAttack;
        if (zombie.getHeldItem() == null || attack < 1 || data.ticksAfterPunch >= 10.0f) return;
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        BendsAnimator.playOnce(model, data, Animation_Attack.SWORD,
            "animation.mobends.player.sword_" + attack + (attack < 3 && moving ? "_moving" : ""), data.ticksAfterPunch / 20.0,
            "attack", Animation_Attack.SWING_FADE, null, false, data.swings);
    }
}
