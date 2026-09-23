package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Swimming.*;

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

/** The original Java swimming (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Swimming {
    private Reference_Swimming() {}

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
