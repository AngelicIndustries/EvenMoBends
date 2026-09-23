package net.gobbob.mobends.animation.zombie;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.model.entity.ModelBendsZombie;
import net.gobbob.mobends.data.Data_Zombie;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

public class Animation_Walk
extends Animation {
    @Override
    public String getName() {
        return "walk";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsZombie)argModel, (Data_Zombie)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "zombie/walk");

    /**
     * Plays {@code zombie/walk.animation.json}: {@code walk} and {@code walk_swing} timed by the stride like the
     * player's walk, and {@code walk_arms_up} over them while the zombie reaches forward (it takes turns at that
     * every few seconds). A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsZombie model, Data_Zombie data) {
        boolean armsUp = data.currentWalkingState == 1;
        BendsAnimator.playStride(model, data, FILE, "walk", "zombie");
        if (armsUp) BendsAnimator.playPose(model, data, FILE, "walk_arms_up", "zombie_arms");
    }
}
