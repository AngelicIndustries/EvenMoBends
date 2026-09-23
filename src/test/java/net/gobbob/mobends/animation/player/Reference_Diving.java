package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Diving.*;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

/** The original Java diving (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Diving {
    private Reference_Diving() {}

    /** The original Java dive, kept as the reference the Blockbench dive was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, float motionX, float motionY, float motionZ) {
        float moving = moving(motionX, motionY, motionZ);
        float phase = ticks * 0.24f;
        float armStroke = (MathHelper.cos(phase) + 1.0f) / 2.0f;
        float legKick = MathHelper.cos(phase * 1.8f) * moving;

        float travelPitch = travelPitch(motionX, motionY, motionZ);
        float bodyTilt = 80.0f + travelPitch;
        model.renderRotation.setSmoothX(bodyTilt, 0.25f);
        model.renderOffset.setSmoothZ(10.0f, 0.25f);

        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.3f);

        // Use a full breaststroke underwater. The first pass held the arms nearly straight,
        // which read as gliding instead of swimming.
        float upperArmPitch = -45.0f - armStroke * 120.0f;
        float armSpread = armStroke * 20.0f;
        float strokeProgress = (phase % ((float)Math.PI * 2.0f)) / ((float)Math.PI * 2.0f);
        float elbowBend = Math.min(armStroke * 2.0f - 1.0f, 0.0f) * -60.0f;
        if (strokeProgress >= 0.55f && strokeProgress <= 0.9f) {
            elbowBend = -60.0f;
        }

        ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothY(-90.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothY(90.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothZ(armSpread, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothZ(-armSpread, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(upperArmPitch, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(upperArmPitch, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(elbowBend, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(elbowBend, 0.3f);

        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(legKick * 35.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-legKick * 35.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(3.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-3.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(
            8.0f + Math.max(0.0f, -legKick) * 24.0f,
            0.3f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(
            8.0f + Math.max(0.0f, legKick) * 24.0f,
            0.3f);

        ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - bodyTilt * 0.45f, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
    }
}
