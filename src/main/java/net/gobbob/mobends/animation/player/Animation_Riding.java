package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.PlayerAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

public class Animation_Riding
extends Animation {
    @Override
    public String getName() {
        return "riding";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/ride");

    /**
     * Plays {@code player/ride.animation.json}: {@code ride} sitting still; moving, {@code ride_move} with
     * {@code ride_move_jiggle} (looping on the player's age) over it as strongly as the limbs swing. Falls back to
     * the Java ride below.
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        boolean played = moving
            ? PlayerAnimator.playPose(model, data, FILE, "ride_move", "ride")
                && PlayerAnimator.playLoop(model, data, FILE, "ride_move_jiggle", "ride_jiggle",
                    Math.max(0, Math.min(1, model.armSwingAmount)))
            : PlayerAnimator.playPose(model, data, FILE, "ride", "ride");
        if (!played) apply(model, data.ticks, moving);
    }

    /** The original Java ride, kept as the reference the Blockbench ride was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, boolean moving) {
        model.renderOffset.setSmoothY(1.5f, 0.3f);
        // Restore ordinary riding when switching mounts or disabling the rowing pose.
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-85.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(45.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-85.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(-45.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(60.0f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(60.0f);
        if (!moving) {
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-10.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-10.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(-10.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(10.0f, 0.3f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(-20.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(-20.0f, 0.3f);
        } else {
            float jiggle = MathHelper.cos(ticks * 0.6f) * model.armSwingAmount;
            float jiggle_hard = MathHelper.cos(ticks * 0.3f) * model.armSwingAmount;
            if (jiggle_hard < 0.0f) {
                jiggle_hard = -jiggle_hard;
            }
            model.renderOffset.setSmoothY(1.5f + jiggle_hard * 20.0f, 0.7f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(40.0f + jiggle * 300.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-45.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-45.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(-10.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(10.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(-30.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(-30.0f, 0.3f);
        }
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - model.bipedBody.rotateAngleX / (float)Math.PI * 180.0f, 0.3f);
    }
}
