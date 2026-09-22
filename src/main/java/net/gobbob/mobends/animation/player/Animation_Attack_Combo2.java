package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.util.GUtil;
import net.minecraft.util.MathHelper;
import org.lwjgl.util.vector.Vector3f;

public class Animation_Attack_Combo2 {
    /** The original Java spinning third sword swing, kept as the reference the Blockbench {@code sword_3} was baked from. */
    public static void apply(ModelBendsPlayer model, float ticksAfterPunch) {
        float var2;
        float attackState = ticksAfterPunch / 10.0f;
        float armSwing = attackState * 2.0f;
        armSwing = GUtil.max(armSwing, 1.0f);
        float var5 = attackState * 1.6f;
        var5 = GUtil.max(var5, 1.0f);
        float var = 50.0f + 360.0f * var5;
        for (var2 = 50.0f + 360.0f * var5; var2 > 360.0f; var2 -= 360.0f) {
        }
        if (var > 360.0f) {
            model.renderRotation.vOld.y = var2;
            model.renderRotation.vFinal.y = var2;
            model.renderRotation.completion.y = 0.0f;
        } else {
            model.renderRotation.setSmoothY(var, 0.7f);
        }
        Vector3f bodyRot = new Vector3f(0.0f, 0.0f, 0.0f);
        bodyRot.x = 20.0f - attackState * 20.0f;
        bodyRot.y = -40.0f * attackState;
        ((ModelRendererBends)model.bipedBody).rotation.setSmooth(bodyRot, 0.9f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX);
        ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(-bodyRot.x, 0.9f);
        ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothY(-bodyRot.y, 0.9f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-30.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-30.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(10.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-10.0f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(30.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(30.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothZ(-(-60.0f - var5 * 80.0f), 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(-20.0f + armSwing * 70.0f, 3.0f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothY(0.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(0.0f, 0.9f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(20.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothZ(-80.0f, 0.3f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(-20.0f, 0.3f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(-60.0f, 0.3f);
        model.renderItemRotation.setSmoothX(90.0f * attackState, 0.9f);
        float var61 = ticksAfterPunch * 5.0f;
        float var62 = ticksAfterPunch * 5.0f;
        var61 = (MathHelper.cos(var61 * 0.0625f) + 1.0f) / 2.0f * 20.0f;
        var62 = (MathHelper.cos(var62 * 0.0625f) + 1.0f) / 2.0f * 20.0f;
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(0.0f, 0.9f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(-25.0f, 0.9f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(var61);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-var61);
        model.renderOffset.setSmoothY(-2.0f);
    }
}

