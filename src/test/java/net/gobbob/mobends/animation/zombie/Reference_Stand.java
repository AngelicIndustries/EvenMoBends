package net.gobbob.mobends.animation.zombie;

import static net.gobbob.mobends.animation.zombie.Animation_Stand.*;

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

/** The original Java stand (zombie), the reference its Blockbench animation was baked from and is tested against. */
public final class Reference_Stand {
    private Reference_Stand() {}

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
