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

public class Animation_Stand
extends Animation {
    @Override
    public String getName() {
        return "stand";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/stand");

    /**
     * Plays {@code player/stand.animation.json}'s {@code stand}, looping on the player's age.
     * A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        BendsAnimator.playLoop(model, data, FILE, "stand", "locomotion");
    }
}

