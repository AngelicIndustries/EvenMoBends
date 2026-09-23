package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Flying.*;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/** The original Java flying (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Flying {
    private Reference_Flying() {}

    /** The original Java flight, kept as the reference the Blockbench flight was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks, float motionX, float motionY, float motionZ,
        boolean elytraFlying) {
        float moving = moving(motionX, motionY, motionZ);
        float hovering = 1.0f - moving;
        float phase = ticks * 0.15f;

        // EFR rotates the entire RenderPlayer while gliding. Creative flight needs
        // its own forward tilt, but applying it to EFR would double the rotation.
        float bodyTilt = elytraFlying ? 0.0f : 70.0f * moving;
        model.renderRotation.setSmoothX(bodyTilt, 0.25f);
        model.renderOffset.setSmoothZ(elytraFlying ? 0.0f : 8.0f * moving, 0.25f);

        float armSpread = elytraFlying ? 0.0f : 45.0f + 33.75f * moving;
        float hoverTurn = MathHelper.cos(phase) * 22.5f * hovering;
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(hoverTurn, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(hoverTurn, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(armSpread, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-armSpread, 0.3f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(0.0f, 0.3f);

        float legWave = MathHelper.cos(phase) * 11.25f * hovering;
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(legWave, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-legWave, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(2.8f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-2.8f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(0.0f, 0.3f);

        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothZ(0.0f, 0.3f);
        float headPitch = elytraFlying ? -45.0f : model.headRotationX - bodyTilt * 0.5f;
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(headPitch, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
    }
}
