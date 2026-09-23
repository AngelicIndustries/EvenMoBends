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
import net.minecraft.entity.player.EntityPlayer;

public class Animation_Bow
extends Animation {
    @Override
    public String getName() {
        return "bow";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        ModelBendsPlayer model = (ModelBendsPlayer)argModel;
        EntityPlayer player = (EntityPlayer)argEntity;
        play(model, (Data_Player)argData, player != null ? player.getItemInUseDuration() + model.partialTicks() : 0);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/bow");
    /** Drawing takes this many ticks, then the aim settles. */
    public static final float DRAW_TICKS = 10;
    /** How long where the body was fades out as the draw starts. */
    public static final double FADE = 0.15;

    /**
     * Plays {@code player/bow.animation.json}, timed from when the player started drawing: {@code bow_draw} for
     * {@link #DRAW_TICKS}, then {@code bow_aim}, which holds its last frame.
     * A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data, float useTicks) {
        play(model, data, useTicks, false);
    }

    /** As above; {@code mirror} for a bow in the off hand, held in the left hand and drawn with the right. */
    public static void play(ModelBendsPlayer model, Data_Player data, float useTicks, boolean mirror) {
        if (useTicks < DRAW_TICKS) {
            BendsAnimator.playOnce(model, data, FILE, "animation.mobends.player.bow_draw", useTicks / 20.0, "bow", FADE, null, mirror);
        } else {
            BendsAnimator.playOnce(model, data, FILE, "animation.mobends.player.bow_aim", (useTicks - DRAW_TICKS) / 20.0, "bow",
                FADE, null, mirror);
        }
    }
}
