package net.gobbob.mobends.animation.zombie;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.animation.BipedRigModel;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.entity.ModelBendsZombie;
import net.gobbob.mobends.data.Data_Zombie;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

public class Animation_Walk
extends Animation {
    @Override
    public String getName() {
        return "walk";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsZombie)argModel, (Data_Zombie)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "zombie/walk");

    /**
     * Plays {@code zombie/walk.animation.json}: {@code walk} and {@code walk_swing} timed by the stride like the
     * player's walk, and {@code walk_arms_up} over them while the zombie reaches forward (it takes turns at that
     * every few seconds). Falls back to the Java walk below.
     */
    public static void play(ModelBendsZombie model, Data_Zombie data) {
        boolean armsUp = data.currentWalkingState == 1;
        boolean played = BendsAnimator.playStride(model, data, FILE, "walk", "zombie")
            && (!armsUp || BendsAnimator.playPose(model, data, FILE, "walk_arms_up", "zombie_arms"));
        if (!played) apply(model, armsUp);
    }

    /** The original Java zombie walk, kept as the reference the Blockbench walk was baked from. */
    public static void apply(BipedRigModel model, boolean armsUp) {
        model.rootOffset().setSmoothY(-3.0f);
        float var2 = 30.0f + MathHelper.cos(model.limbSwing() * 0.6662f * 2.0f) * 10.0f;
        Bone.BODY.part(model).rotation.setSmoothX(var2, 0.3f);
        Bone.RIGHT_ARM.part(model).rotation.setSmoothX(0.9f * (float)((double)(MathHelper.cos(model.limbSwing() * 0.6662f + (float)Math.PI) * 2.0f * model.limbSwingAmount() * 0.5f) / Math.PI * 180.0));
        Bone.LEFT_ARM.part(model).rotation.setSmoothX(0.9f * (float)((double)(MathHelper.cos(model.limbSwing() * 0.6662f) * 2.0f * model.limbSwingAmount() * 0.5f) / Math.PI * 180.0));
        Bone.RIGHT_ARM.part(model).rotation.setSmoothZ(5.0f, 0.3f);
        Bone.LEFT_ARM.part(model).rotation.setSmoothZ(-5.0f, 0.3f);
        Bone.RIGHT_LEG.part(model).rotation.setSmoothX(-5.0f + 0.9f * (float)((double)(MathHelper.cos(model.limbSwing() * 0.6662f) * 1.4f * model.limbSwingAmount()) / Math.PI * 180.0), 1.0f);
        Bone.LEFT_LEG.part(model).rotation.setSmoothX(-5.0f + 0.9f * (float)((double)(MathHelper.cos(model.limbSwing() * 0.6662f + (float)Math.PI) * 1.4f * model.limbSwingAmount()) / Math.PI * 180.0), 1.0f);
        Bone.RIGHT_LEG.part(model).rotation.setSmoothY(0.0f);
        Bone.LEFT_LEG.part(model).rotation.setSmoothY(0.0f);
        Bone.RIGHT_LEG.part(model).rotation.setSmoothZ(10.0f, 0.2f);
        Bone.LEFT_LEG.part(model).rotation.setSmoothZ(-10.0f, 0.2f);
        float var = (float)((double)(model.limbSwing() * 0.6662f) / Math.PI) % 2.0f;
        Bone.LEFT_FORE_LEG.part(model).rotation.setSmoothX(var > 1.0f ? 45 : 0, 0.3f);
        Bone.RIGHT_FORE_LEG.part(model).rotation.setSmoothX(var > 1.0f ? 0 : 45, 0.3f);
        Bone.LEFT_FORE_ARM.part(model).rotation.setSmoothX((float)(Math.cos((double)(model.limbSwing() * 0.6662f) + 1.5707963267948966) + 1.0) / 2.0f * -20.0f, 1.0f);
        Bone.RIGHT_FORE_ARM.part(model).rotation.setSmoothX((float)(Math.cos(model.limbSwing() * 0.6662f) + 1.0) / 2.0f * -20.0f, 0.3f);
        float var1 = MathHelper.cos(model.limbSwing() * 0.6662f + (float)Math.PI) / (float)Math.PI * 180.0f * 0.5f;
        Bone.BODY.part(model).rotation.setSmoothY(MathHelper.cos(model.limbSwing() * 0.6662f + (float)Math.PI) / (float)Math.PI * 180.0f * 0.5f, 0.3f);
        if (armsUp) {
            Bone.RIGHT_ARM.part(model).rotation.setSmoothX(-120.0f);
            Bone.LEFT_ARM.part(model).rotation.setSmoothX(-120.0f);
        }
        Bone.HEAD.part(model).rotation.setX(model.headPitch() - 30.0f);
        Bone.HEAD.part(model).rotation.setY(model.headYaw() - var1);
    }
}
