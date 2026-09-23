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

public class Animation_Stand
extends Animation {
    @Override
    public String getName() {
        return "stand";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsZombie)argModel, (Data_Zombie)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "zombie/stand");

    /** Plays {@code zombie/stand.animation.json}'s {@code stand}, a held pose. A missing animation is skipped (the animator logs it). */
    public static void play(ModelBendsZombie model, Data_Zombie data) {
        BendsAnimator.playPose(model, data, FILE, "stand", "zombie");
    }
}
