package net.gobbob.mobends.animation.player;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.PlayerBlockingCompat;
import net.minecraft.client.model.ModelRenderer;
import org.junit.jupiter.api.Test;

class AnimationBlockingTest {
    @Test
    void offhandBlockOverridesBothSegmentsButPreservesMainHandAttack() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        part(model.bipedRightArm).rotation.setX(-120);
        part(model.bipedRightForeArm).rotation.setX(-60);
        part(model.bipedBody).rotation.setY(25);
        model.renderItemRotation.setX(65);
        part(model.bipedLeftArm).pre_rotation.setZ(-80);
        part(model.bipedLeftForeArm).pre_rotation.setY(90);

        Reference_Blocking.apply(model, PlayerBlockingCompat.OFF_HAND);

        assertPose(model.bipedLeftArm, model.bipedLeftForeArm, 0);
        assertEquals(-120, part(model.bipedRightArm).rotation.vFinal.x);
        assertEquals(-60, part(model.bipedRightForeArm).rotation.vFinal.x);
        assertEquals(25, part(model.bipedBody).rotation.vFinal.y);
        assertEquals(65, model.renderItemRotation.vFinal.x);
    }

    @Test
    void mainHandBlockClearsItemOnlyWeaponRotation() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.renderItemRotation.setX(65);
        part(model.bipedLeftArm).rotation.setX(42);
        Reference_Blocking.apply(model, PlayerBlockingCompat.MAIN_HAND);
        assertPose(model.bipedRightArm, model.bipedRightForeArm, -30);
        assertEquals(0, model.renderItemRotation.vFinal.lengthSquared());
        assertEquals(42, part(model.bipedLeftArm).rotation.vFinal.x);
    }

    @Test
    void bothHandsCanBlockWithoutChangingSlimOrArmorPivots() {
        for (float scale : new float[] {0, 0.5f, 1}) {
            ModelBendsPlayer model = new ModelBendsPlayer(scale);
            model.initModern();
            model.setSlim(true);
            float shoulderY = model.bipedRightArm.rotationPointY;
            float elbowX = model.bipedRightForeArm.rotationPointX;
            Reference_Blocking.apply(model, PlayerBlockingCompat.MAIN_HAND | PlayerBlockingCompat.OFF_HAND);
            assertPose(model.bipedRightArm, model.bipedRightForeArm, -30);
            assertPose(model.bipedLeftArm, model.bipedLeftForeArm, 0);
            assertEquals(shoulderY, model.bipedRightArm.rotationPointY);
            assertEquals(elbowX, model.bipedRightForeArm.rotationPointX);
        }
    }

    @Test
    void inactiveBlockLeavesTheBaseAnimationAlone() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        part(model.bipedLeftArm).rotation.setX(23);
        part(model.bipedRightArm).rotation.setX(32);
        Reference_Blocking.apply(model, 0);
        assertEquals(23, part(model.bipedLeftArm).rotation.vFinal.x);
        assertEquals(32, part(model.bipedRightArm).rotation.vFinal.x);
    }

    private static void assertPose(ModelRenderer arm, ModelRenderer forearm, float yaw) {
        assertEquals(-24, part(arm).rotation.vFinal.x);
        assertEquals(-30, part(forearm).rotation.vFinal.x);
        assertEquals(-54, part(arm).rotation.vFinal.x + part(forearm).rotation.vFinal.x);
        assertEquals(yaw, part(arm).rotation.vFinal.y);
        assertEquals(0, part(arm).rotation.vFinal.z);
        assertEquals(0, part(forearm).rotation.vFinal.y);
        assertEquals(0, part(forearm).rotation.vFinal.z);
        assertEquals(0, part(arm).pre_rotation.vFinal.lengthSquared());
        assertEquals(0, part(forearm).pre_rotation.vFinal.lengthSquared());
    }

    private static ModelRendererBends part(ModelRenderer part) {
        return (ModelRendererBends)part;
    }
}
