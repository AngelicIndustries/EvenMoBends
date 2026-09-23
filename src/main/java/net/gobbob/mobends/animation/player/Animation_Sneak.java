package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public class Animation_Sneak
extends Animation {
    @Override
    public String getName() {
        return "sneak";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    /**
     * Plays {@code player/sneak.animation.json}. Moving, {@code sneak_walk} (with {@code sneak_walk_swing} by the limbs'
     * swing) replaces the walk underneath with the sneaking gait; standing, {@code sneak} crouches over the stand and
     * leaves the axes it keeps ({@code q.mobends_keep}) to it. Both play at the stride's phase. Falls back to the Java
     * sneak below if the file is missing.
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        boolean played = moving
            ? BendsAnimator.playStride(model, data, FILE, "sneak_walk", "sneak")
            : BendsAnimator.playStridePose(model, data, FILE, "sneak", "sneak");
        if (!played) apply(model);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/sneak");

    /** The original Java sneak, kept as the reference the Blockbench sneak was baked from. */
    public static void apply(ModelBendsPlayer model) {
        float var = (float)((double)(model.armSwing * 0.6662f) / Math.PI) % 2.0f;
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-5.0f + 1.1f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-5.0f + 1.1f * (float)((double)(MathHelper.cos(model.armSwing * 0.6662f + (float)Math.PI) * 1.4f * model.armSwingAmount) / Math.PI * 180.0), 1.0f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(10.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-10.0f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-20.0f + 20.0f * MathHelper.cos(model.armSwing * 0.6662f + (float)Math.PI));
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-20.0f + 20.0f * MathHelper.cos(model.armSwing * 0.6662f));
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(var > 1.0f ? 45 : 10, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(var > 1.0f ? 10 : 45, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(var > 1.0f ? -10 : -45, 0.01f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(var > 1.0f ? -45 : -10, 0.01f);
        float var2 = 25.0f + (float)Math.cos(model.armSwing * 0.6662f * 2.0f) * 5.0f;
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(var2);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - var2, 0.3f);
    }
}

