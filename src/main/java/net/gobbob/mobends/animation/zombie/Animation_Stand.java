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
import net.minecraft.util.ResourceLocation;

public class Animation_Stand
extends Animation {
    @Override
    public String getName() {
        return "stand";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsZombie)argModel, (Data_Zombie)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "zombie/stand");

    /** Plays {@code zombie/stand.animation.json}'s {@code stand}, a held pose. Falls back to the Java stand below. */
    public static void play(ModelBendsZombie model, Data_Zombie data) {
        if (!BendsAnimator.playPose(model, data, FILE, "stand", "zombie")) apply(model);
    }

    /** The original Java zombie stand, kept as the reference the Blockbench stand was baked from. */
    public static void apply(BipedRigModel model) {
        model.rootOffset().setSmoothY(-3.0f);
        Bone.BODY.part(model).rotation.setSmoothX(30.0f, 0.3f);
        Bone.RIGHT_ARM.part(model).rotation.setSmoothX(-30.0f, 0.3f);
        Bone.LEFT_ARM.part(model).rotation.setSmoothX(-30.0f, 0.3f);
        Bone.RIGHT_LEG.part(model).rotation.setSmoothZ(10.0f, 0.3f);
        Bone.LEFT_LEG.part(model).rotation.setSmoothZ(-10.0f, 0.3f);
        Bone.RIGHT_LEG.part(model).rotation.setSmoothX(-20.0f, 0.3f);
        Bone.LEFT_LEG.part(model).rotation.setSmoothX(-20.0f, 0.3f);
        Bone.RIGHT_FORE_LEG.part(model).rotation.setSmoothX(25.0f, 0.3f);
        Bone.LEFT_FORE_LEG.part(model).rotation.setSmoothX(25.0f, 0.3f);
        Bone.HEAD.part(model).rotation.setX(model.headPitch() - 30.0f);
        Bone.HEAD.part(model).rotation.setY(model.headYaw());
    }
}
