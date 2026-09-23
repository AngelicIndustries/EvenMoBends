package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.util.GUtil;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public class Animation_Sprint
extends Animation {
    @Override
    public String getName() {
        return "sprint";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    /**
     * Plays {@code player/sprint.animation.json}: {@code sprint} at the stride's phase (one loop per stride, so it
     * keeps pace with the walk) and {@code sprint_swing} over it as strongly as the limbs swing. Falls back to the
     * Java sprint below if the file is missing.
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        if (!BendsAnimator.playStride(model, data, FILE, "sprint", "locomotion")) apply(model);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/sprint");

    /** The original Java sprint, kept as the reference the Blockbench sprint was baked from. */
    public static void apply(ModelBendsPlayer model) {
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(1.1f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f + (float)Math.PI) * 2.0f * model.armSwingAmount * 0.5f) / Math.PI * 180.0));
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(1.1f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f) * 2.0f * model.armSwingAmount * 0.5f) / Math.PI * 180.0));
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(5.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-5.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-5.0f + 0.9f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-5.0f + 0.9f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f + (float)Math.PI) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(0.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(0.0f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(2.0f, 0.2f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-2.0f, 0.2f);
        float var = (float)((double)(model.armSwing * 0.6662f) / Math.PI) % 2.0f;
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(var > 1.0f ? 45 : 0, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(var > 1.0f ? 0 : 45, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(var > 1.0f ? -10 : -45, 0.3f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(var > 1.0f ? -45 : -10, 0.3f);
        float var2 = (float)Math.cos(model.armSwing * 0.6662f) * -40.0f;
        float var3 = (float)(Math.cos(model.armSwing * 0.6662f * 2.0f) * 0.5 + 0.5) * 20.0f;
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(var2, 0.5f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(var3);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY - var2, 0.5f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - var3);
        float var10 = model.headRotationY * 0.3f;
        var10 = GUtil.max(var10, 20.0f);
        var10 = GUtil.min(var10, -20.0f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(-var10, 0.3f);
        model.renderOffset.setSmoothY(var3 * 0.15f, 0.9f);
    }
}

