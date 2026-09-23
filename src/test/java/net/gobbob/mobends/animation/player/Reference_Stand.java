package net.gobbob.mobends.animation.player;

import static net.gobbob.mobends.animation.player.Animation_Stand.*;

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
import org.lwjgl.util.vector.Vector3f;

/** The original Java stand (player), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Stand {
    private Reference_Stand() {}

    /** The original Java stand, kept as the reference the Blockbench stand was baked from. */
    public static void apply(ModelBendsPlayer model, float ticks) {
        ((ModelRendererBends)model.bipedBody).rotation.setSmooth(new Vector3f(0.0f, 0.0f, 0.0f), 0.5f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothZ(2.0f, 0.2f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothZ(-2.0f, 0.2f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothX(0.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothX(0.0f, 0.1f);
        ((ModelRendererBends)model.bipedRightLeg).rotation.setSmoothY(5.0f);
        ((ModelRendererBends)model.bipedLeftLeg).rotation.setSmoothY(-5.0f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothX(0.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothX(0.0f, 0.1f);
        ((ModelRendererBends)model.bipedRightForeLeg).rotation.setSmoothX(4.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftForeLeg).rotation.setSmoothX(4.0f, 0.1f);
        ((ModelRendererBends)model.bipedRightForeArm).rotation.setSmoothX(-4.0f, 0.1f);
        ((ModelRendererBends)model.bipedLeftForeArm).rotation.setSmoothX(-4.0f, 0.1f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothX(model.headRotationX, 0.3f);
        ((ModelRendererBends)model.bipedHead).rotation.setSmoothY(model.headRotationY, 0.3f);
        ((ModelRendererBends)model.bipedBody).rotation.setSmoothX((float)((Math.cos(ticks / 10.0f) - 1.0) / 2.0) * -3.0f);
        ((ModelRendererBends)model.bipedLeftArm).rotation.setSmoothZ(-((float)((Math.cos((double)(ticks / 10.0f) + 1.5707963267948966) - 1.0) / 2.0)) * -5.0f);
        ((ModelRendererBends)model.bipedRightArm).rotation.setSmoothZ(-((float)((Math.cos((double)(ticks / 10.0f) + 1.5707963267948966) - 1.0) / 2.0)) * 5.0f);
    }
}
