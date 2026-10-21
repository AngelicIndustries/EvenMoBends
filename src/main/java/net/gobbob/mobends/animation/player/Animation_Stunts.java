package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.TuckAndRollCompat;
import net.gobbob.mobends.compat.WallGrabCompat;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Other mods' stunts, over whatever the body was doing, each as far in as that mod says: Wall Grab's wall slide and
 * wall jump, and Tuck &amp; Roll's curl, which that mod turns over itself. From {@code player/stunts.animation.json}.
 */
public final class Animation_Stunts {
    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/stunts");

    private Animation_Stunts() {}

    public static void play(ModelBendsPlayer model, Data_Player data, EntityPlayer player, float partialTicks) {
        float slide = WallGrabCompat.slide(player);
        if (slide > 0.0f) BendsAnimator.playLoop(model, data, FILE, "wall_slide", "wall_slide", slide);
        float jump = WallGrabCompat.jump(player);
        if (jump > 0.0f) BendsAnimator.playPose(model, data, FILE, "wall_jump", "wall_jump", jump);
        float tuck = TuckAndRollCompat.tuck(player, partialTicks);
        if (tuck > 0.0f) BendsAnimator.playPose(model, data, FILE, "tuck", "tuck", tuck);
    }
}
