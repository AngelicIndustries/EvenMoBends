package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Bow.*;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.util.GUtil;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/** The original Java bow (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Bow {
    private Reference_Bow() {}

    /** The original Java bow, kept as the reference the Blockbench bow was baked from. */
    public static void apply(ModelBendsPlayer model, float useTicks) {
        float aimedBowDuration = Math.min(useTicks, 15.0f);
        if (aimedBowDuration < 10.0f) {
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(30.0f, 0.3f);
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(0.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(0.0f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-30.0f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-30.0f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(80.0f);
            float var = aimedBowDuration / 10.0f;
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(var * -50.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - 30.0f, 0.3f);
        } else {
            float var1 = 20.0f - (aimedBowDuration - 10.0f) / 5.0f * 20.0f;
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothX(var1, 0.3f);
            float var = (aimedBowDuration - 10.0f) / 5.0f * -25.0f;
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(var + model.headRotationY, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-90.0f - var1, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(-30.0f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothY(80.0f);
            float var2 = aimedBowDuration / 10.0f;
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(var2 * -30.0f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(-var);
            float var5 = -90.0f + model.headRotationX;
            var5 = GUtil.min(var5, -120.0f);
            ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothX(var5, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothX(model.headRotationX);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(-var);
            ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(-var1, 0.3f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX);
        }
    }
}
