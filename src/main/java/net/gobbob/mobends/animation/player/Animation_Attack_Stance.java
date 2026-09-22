package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.lwjgl.util.vector.Vector3f;

public class Animation_Attack_Stance {
    /**
     * The original Java sword stance, kept as the reference the Blockbench {@code sword_stance} (standing) and
     * {@code sword_stance_sprint} were baked from. On the ground only; walking without sprinting has none.
     */
    public static void apply(ModelBendsPlayer model, boolean moving, boolean sprinting) {
        if (!moving) {
            model.renderRotation.setSmoothY(30.0f, 0.3f);
            Vector3f bodyRot = new Vector3f(0.0f, 0.0f, 0.0f);
            bodyRot.x = 20.0f;
            ((ModelRendererBends)model.bipedBody).rotation.setSmooth(bodyRot, 0.3f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY - 30.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX);
            ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothX(-bodyRot.x, 0.3f);
            ((ModelRendererBends)model.bipedHead).pre_rotation.setSmoothY(-bodyRot.y, 0.3f);
            ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(-30.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(-30.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(-25.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(10.0f);
            ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-10.0f);
            ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(30.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(30.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).pre_rotation.setSmoothZ(60.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(60.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(20.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftArm).pre_rotation.setSmoothZ(-80.0f, 0.3f);
            ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(-20.0f, 0.3f);
            ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(-60.0f, 0.3f);
            model.renderItemRotation.setSmoothX(65.0f, 0.3f);
            model.renderOffset.setSmoothY(-2.0f);
        } else if (sprinting) {
            ((ModelRendererBends)model.bipedBody).rotation.setSmoothY(20.0f, 0.3f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY - 20.0f);
            ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX - 15.0f);
            ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(0.0f);
            ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(0.0f);
            ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(60.0f, 0.3f);
            model.renderItemRotation.setSmoothX(90.0f, 0.3f);
        }
    }
}

