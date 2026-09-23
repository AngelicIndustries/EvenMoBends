package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.PlayerBlockingCompat;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.util.ResourceLocation;

/** A per-arm overlay, applied after locomotion and attacks so the other hand can still swing. */
public final class Animation_Blocking {
    private static final float SMOOTHNESS = 0.5f;

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/block");

    private Animation_Blocking() {}

    /**
     * Plays {@code block_right} and/or {@code block_left} from {@code player/block.animation.json} for the blocking
     * hands; a hand whose animation is missing falls back to the Java pose.
     */
    public static void play(ModelBendsPlayer model, Data_Player data, int hands) {
        int java = 0;
        if ((hands & PlayerBlockingCompat.MAIN_HAND) != 0
            && !BendsAnimator.playPose(model, data, FILE, "block_right", "block_right")) {
            java |= PlayerBlockingCompat.MAIN_HAND;
        }
        if ((hands & PlayerBlockingCompat.OFF_HAND) != 0
            && !BendsAnimator.playPose(model, data, FILE, "block_left", "block_left")) {
            java |= PlayerBlockingCompat.OFF_HAND;
        }
        apply(model, java);
    }

    /** The original Java blocking, kept as the reference the Blockbench blocking was baked from. */
    public static void apply(ModelBendsPlayer model, int hands) {
        if ((hands & PlayerBlockingCompat.MAIN_HAND) != 0) {
            pose((ModelRendererBends)model.bipedRightArm, (ModelRendererBends)model.bipedRightForeArm, -30.0f);
            // Weapon animations normally add an extra item-only rotation. A blocking item
            // must instead follow the posed forearm and its own renderer's blocking transform.
            model.renderItemRotation.setSmoothZero(SMOOTHNESS);
        }
        if ((hands & PlayerBlockingCompat.OFF_HAND) != 0) {
            pose((ModelRendererBends)model.bipedLeftArm, (ModelRendererBends)model.bipedLeftForeArm, 0.0f);
        }
    }

    private static void pose(ModelRendererBends upper, ModelRendererBends lower, float yaw) {
        upper.pre_rotation.setSmoothZero(SMOOTHNESS);
        lower.pre_rotation.setSmoothZero(SMOOTHNESS);
        // Split vanilla's 54-degree blocking pitch at the elbow. Retaining the total
        // orientation lets Backhand and shield item renderers keep their own transforms.
        upper.rotation.setSmoothX(-24.0f, SMOOTHNESS);
        upper.rotation.setSmoothY(yaw, SMOOTHNESS);
        upper.rotation.setSmoothZ(0.0f, SMOOTHNESS);
        lower.rotation.setSmoothX(-30.0f, SMOOTHNESS);
        lower.rotation.setSmoothY(0.0f, SMOOTHNESS);
        lower.rotation.setSmoothZ(0.0f, SMOOTHNESS);
    }
}
