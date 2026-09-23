package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Mining.*;

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

/** The original Java mining (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Mining {
    private Reference_Mining() {}

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
