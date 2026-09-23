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

public class Animation_Walk
extends Animation {
    @Override
    public String getName() {
        return "walk";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    /**
     * Plays {@code player/walk.animation.json}: {@code walk} at the stride's phase (one loop per stride, so it
     * keeps pace with the walk) and {@code walk_swing} over it as strongly as the limbs swing.
     * A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        BendsAnimator.playStride(model, data, FILE, "walk", "locomotion");
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/walk");
}

