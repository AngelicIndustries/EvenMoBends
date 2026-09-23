package net.gobbob.mobends.animation.player;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.util.GUtil;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

public class Animation_Swimming
extends net.gobbob.mobends.animation.Animation {
    @Override
    public String getName() {
        return "swimming";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        ModelBendsPlayer model = (ModelBendsPlayer)argModel;
        Data_Player data = (Data_Player)argData;
        play(model, data, data.motion.x != 0.0f || data.motion.z != 0.0f);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/swim");

    /**
     * Plays the Blockbench swim from {@code player/swim.animation.json}: {@code swim} / {@code swim_idle} for the
     * upper body and {@code swim_legs} / {@code swim_idle_legs} for the kick, which loops at its own speed. While the
     * hand holds something, a {@code _holding} version of each is used if the file has one. Falls back to the Java
     * swim below if the file is missing.
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean moving) {
        String upper = moving ? "swim" : "swim_idle";
        boolean holding = BendsAnimator.isHolding(data);
        boolean played = loop(model, data, upper, "swim_upper", holding);
        played &= loop(model, data, upper + "_legs", "swim_legs", holding);
        if (!played) apply(model, data.ticks, moving);
    }

    private static boolean loop(ModelBendsPlayer model, Data_Player data, String name, String slot, boolean holding) {
        String animation = BendsAnimator.variant(GeckoLibResources.getBakedAnimations().getAnimations(FILE, null),
            "animation.mobends.player." + name, holding);
        Animation loaded = BendsAnimator.find(FILE, animation);
        if (loaded == null) return false;
        double seconds = loaded.length() > 0 ? (data.ticks / 20.0) % loaded.length() : 0;
        // Blend when switching between moving and idle or empty and holding, as the Java swim eased between them.
        return BendsAnimator.play(model, data, FILE, animation, seconds, slot, 0.5);
    }

    /** The original Java swim, kept as the reference the Blockbench swim was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, boolean moving) {
        float armSway = (MathHelper.cos(ticks * 0.1625f) + 1.0f) / 2.0f;
        float armSway2 = (-MathHelper.sin(ticks * 0.1625f) + 1.0f) / 2.0f;
        float legFlap = MathHelper.cos(ticks * 0.4625f);
        float foreArmSway = (float)((double)(ticks * 0.1625f) % (Math.PI * 2)) / ((float)Math.PI * 2);
        float foreArmStretch = armSway * 2.0f;
        foreArmStretch -= 1.0f;
        foreArmStretch = GUtil.min(foreArmStretch, 0.0f);
        if (!moving) {
            armSway = (MathHelper.cos(ticks * 0.0825f) + 1.0f) / 2.0f;
            armSway2 = (-MathHelper.sin(ticks * 0.0825f) + 1.0f) / 2.0f;
            legFlap = MathHelper.cos(ticks * 0.2625f);
            ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(armSway2 * 30.0f - 15.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(armSway2 * 30.0f - 15.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-armSway * 30.0f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(armSway * 30.0f);
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(armSway2 * -40.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(armSway2 * -40.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(legFlap * 40.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-legFlap * 40.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(5.0f, 0.4f);
            ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(5.0f, 0.4f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(armSway * 10.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY);
        } else {
            ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(-70.0f - armSway * -20.0f, 0.3f);
            model.renderRotation.setSmoothX(70.0f, 0.3f);
            model.renderOffset.setSmoothZ(10.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothY(90.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothY(-90.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(armSway * -120.0f - 45.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(armSway * -120.0f - 45.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothZ(armSway * -20.0f);
            ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothZ(-(armSway * -20.0f));
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(foreArmSway < 0.55f | (double)foreArmSway > 0.9 ? foreArmStretch * -60.0f : -60.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(foreArmSway < 0.55f | (double)foreArmSway > 0.9 ? foreArmStretch * -60.0f : -60.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(legFlap * 40.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-legFlap * 40.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(5.0f, 0.4f);
            ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(5.0f, 0.4f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(armSway * -20.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY);
            model.renderItemRotation.setSmoothX(armSway * 120.0f, 0.3f);
        }
    }
}
