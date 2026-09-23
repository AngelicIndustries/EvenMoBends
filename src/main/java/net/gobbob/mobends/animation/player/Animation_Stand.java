package net.gobbob.mobends.animation.player;

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

public class Animation_Stand
extends Animation {
    @Override
    public String getName() {
        return "stand";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/stand");

    /**
     * Plays {@code player/stand.animation.json}'s {@code stand}, looping on the player's age. Falls back to the Java
     * stand below if the file is missing.
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        if (!BendsAnimator.playLoop(model, data, FILE, "stand", "locomotion")) apply(model, data.ticks);
    }

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

