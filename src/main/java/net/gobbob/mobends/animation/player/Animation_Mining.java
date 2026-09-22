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

public class Animation_Mining
extends Animation {
    @Override
    public String getName() {
        return "mining";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData, ((EntityPlayer)argEntity).isSwingInProgress);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/mine");

    /**
     * Plays {@code player/mine.animation.json}: the stance {@code mine} while holding a pickaxe or a block, and
     * {@code mine_swing} over it while swinging, looping on the player's age. Falls back to the Java mining below.
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean swinging) {
        boolean played = PlayerAnimator.playPose(model, data, FILE, "mine", "tool")
            && (!swinging || PlayerAnimator.playLoop(model, data, FILE, "mine_swing", "tool_swing"));
        if (!played) apply(model, data.ticks, swinging);
    }

    /** The original Java mining, kept as the reference the Blockbench mining was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, boolean swinging) {
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(10.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-10.0f, 0.3f);
        model.renderOffset.setSmoothY(-1.5f, 0.3f);
        if (swinging) {
            float speed = 1.8f;
            float progress = ticks * speed / 20.0f % 1.0f;
            float progress2 = (ticks - 2) * speed / 20.0f % 1.0f;
            float armSwing = (MathHelper.cos(progress * (float)Math.PI * 2.0f) + 1.0f) / 2.0f * -60.0f - 30.0f + model.headRotationX * 0.5f - 30.0f;
            float armYRot = 30.0f + MathHelper.cos((armSwing - 90.0f) / 180.0f * 3.14f) * -5.0f;
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(armSwing, 0.7f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(-armYRot, 0.7f);
            model.renderItemRotation.setSmoothZ(-30.0f, 0.3f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(MathHelper.sin(progress2 * (float)Math.PI * 2.0f) * -20.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - model.bipedBody.rotateAngleX);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY - model.bipedBody.rotateAngleY);
        }
    }
}
