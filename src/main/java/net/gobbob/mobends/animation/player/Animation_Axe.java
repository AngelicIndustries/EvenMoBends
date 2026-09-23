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

public class Animation_Axe
extends Animation {
    @Override
    public String getName() {
        return "axe";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData, ((EntityPlayer)argEntity).isSwingInProgress);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/axe");

    /**
     * Plays {@code player/axe.animation.json}: the two-handed hold {@code axe} while holding an axe, and
     * {@code axe_swing} over it while swinging, looping on the player's age. An occupied off hand
     * keeps its arm. Falls back to the Java axe below.
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean swinging) {
        if (swinging) model.swordTrail.add(net.gobbob.mobends.client.renderer.SwordTrail.AXE);
        java.util.Set<String> without = PlayerAnimator.freeHandsOnly(data, null);
        boolean played = PlayerAnimator.playPose(model, data, FILE, "axe", "tool", 1, without)
            && (!swinging || PlayerAnimator.playLoop(model, data, FILE, "axe_swing", "tool_swing", 1, without));
        if (!played) apply(model, data.ticks, swinging);
    }

    /** The original Java axe, kept as the reference the Blockbench axe was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, boolean swinging) {
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZero(0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZero(0.3f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothZero(0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothZero(0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(10.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-10.0f, 0.3f);
        model.renderOffset.setSmoothY(-1.5f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(90.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothX(-80.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(90.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothY(-40.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothX(-70.0f, 0.3f);
        if (swinging) {
            float speed = 1.8f;
            float progress = ticks * speed / 20.0f % 1.0f;
            float progress2 = (ticks - 2) * speed / 20.0f % 1.0f;
            float armSwing = (MathHelper.cos(progress * (float)Math.PI * 2.0f) + 1.0f) / 2.0f * 2.0f;
            if (armSwing > 1.0f) {
                armSwing = 1.0f;
            }
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(90.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothY(40.0f - 90.0f * armSwing, 0.7f);
            ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothX(-80.0f, 0.7f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(90.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothY(0.0f - 70.0f * armSwing, 0.7f);
            ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothX(-70.0f, 0.7f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(armSwing * 20.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - armSwing * 20.0f, 0.3f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY - armSwing * 20.0f, 0.3f);
        }
    }
}
