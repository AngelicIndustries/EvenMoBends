package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.PlayerAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.util.GUtil;
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
     * {@link #DRAW_TICKS}, then {@code bow_aim}, which holds its last frame. Falls back to the Java bow below.
     */
    public static void play(ModelBendsPlayer model, Data_Player data, float useTicks) {
        play(model, data, useTicks, false);
    }

    /** As above; {@code mirror} for a bow in the off hand, held in the left hand and drawn with the right. */
    public static void play(ModelBendsPlayer model, Data_Player data, float useTicks, boolean mirror) {
        boolean played = useTicks < DRAW_TICKS
            ? PlayerAnimator.playOnce(model, data, FILE, "animation.mobends.player.bow_draw", useTicks / 20.0, "bow", FADE, null, mirror)
            : PlayerAnimator.playOnce(model, data, FILE, "animation.mobends.player.bow_aim", (useTicks - DRAW_TICKS) / 20.0, "bow",
                FADE, null, mirror);
        if (!played) apply(model, useTicks);
    }

    /** The original Java bow, kept as the reference the Blockbench bow was baked from. */
    public static void apply(ModelBendsPlayer model, float useTicks) {
        float aimedBowDuration = Math.min(useTicks, 15.0f);
        if (aimedBowDuration < 10.0f) {
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(30.0f, 0.3f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(0.0f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-30.0f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-30.0f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(80.0f);
            float var = aimedBowDuration / 10.0f;
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(var * -50.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - 30.0f, 0.3f);
        } else {
            float var1 = 20.0f - (aimedBowDuration - 10.0f) / 5.0f * 20.0f;
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(var1, 0.3f);
            float var = (aimedBowDuration - 10.0f) / 5.0f * -25.0f;
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(var + model.headRotationY, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-90.0f - var1, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-30.0f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(80.0f);
            float var2 = aimedBowDuration / 10.0f;
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(var2 * -30.0f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(-var);
            float var5 = -90.0f + model.headRotationX;
            var5 = GUtil.min(var5, -120.0f);
            ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothX(var5, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothX(model.headRotationX);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(-var);
            ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(-var1, 0.3f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX);
        }
    }
}
