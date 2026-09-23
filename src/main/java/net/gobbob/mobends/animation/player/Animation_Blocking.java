package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.PlayerBlockingCompat;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.util.ResourceLocation;

/** A per-arm overlay, applied after locomotion and attacks so the other hand can still swing. */
public final class Animation_Blocking {

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/block");

    private Animation_Blocking() {}

    /**
     * Plays {@code block_right} and/or {@code block_left} from {@code player/block.animation.json} for the blocking
     * hands; a missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data, int hands) {
        if ((hands & PlayerBlockingCompat.MAIN_HAND) != 0) BendsAnimator.playPose(model, data, FILE, "block_right", "block_right");
        if ((hands & PlayerBlockingCompat.OFF_HAND) != 0) BendsAnimator.playPose(model, data, FILE, "block_left", "block_left");
    }
}
