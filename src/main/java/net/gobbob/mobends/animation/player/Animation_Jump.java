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

public class Animation_Jump
extends Animation {
    @Override
    public String getName() {
        return "jump";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData, argEntity.isSprinting());
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/jump");
    /** No blend in: the launch snaps into its crouch; what the jump holds still eases in as usual. */
    public static final double BLEND = 0;

    /**
     * Plays {@code player/jump.animation.json}, timed from liftoff: sprinting, {@code jump_sprint_right} or
     * {@code jump_sprint_left} by the leg leading this jump; otherwise {@code jump}, with the legs striding
     * ({@code jump_walk}, and {@code jump_walk_swing} by the limbs' swing) while moving.
     * A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean sprinting) {
        boolean moving = data.motion.x != 0.0f | data.motion.z != 0.0f;
        double seconds = data.ticksAfterLiftoff / 20.0;
        if (moving && sprinting) {
            BendsAnimator.play(model, data, FILE,
                "animation.mobends.player.jump_sprint_" + (data.sprintJumpLeg ? "right" : "left"), seconds, "jump", BLEND);
        } else {
            BendsAnimator.play(model, data, FILE, "animation.mobends.player.jump", seconds, "jump", BLEND);
            if (moving) BendsAnimator.playStride(model, data, FILE, "jump_walk", "jump_walk");
        }
    }
}

