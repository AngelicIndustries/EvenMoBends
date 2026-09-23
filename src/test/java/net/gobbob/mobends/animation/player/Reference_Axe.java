package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Axe.*;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/** The original Java axe (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Axe {
    private Reference_Axe() {}

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
