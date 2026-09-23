package net.gobbob.mobends.animation.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelRenderer;
import org.junit.jupiter.api.Test;

class AnimationAquaAcrobaticsTest {
    @Test
    void removesPreviousWholeBodyTiltAndOffsetsRatherThanStackingOnAa() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.renderRotation.setX(80);
        model.renderOffset.setZ(10);
        model.renderOffset.setY(-1);
        part(model.bipedRightArm).pre_rotation.setY(-90);
        Animation_AquaAcrobatics.apply(model, 1, true, 10, 0.5f, true);
        assertEquals(0, model.renderRotation.vSmooth.lengthSquared());
        assertEquals(0, model.renderOffset.vSmooth.lengthSquared());
        assertEquals(0, part(model.bipedRightArm).pre_rotation.vFinal.lengthSquared());
    }

    @Test
    void stationaryCrawlingKeepsItsBentArmsAndKnees() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        Animation_AquaAcrobatics.apply(model, 1, false, 0, 0, true);
        assertEquals(-105, part(model.bipedRightArm).rotation.vFinal.x);
        assertEquals(-60, part(model.bipedRightForeArm).rotation.vFinal.x);
        assertEquals(20, part(model.bipedRightForeLeg).rotation.vFinal.x);
        Animation_AquaAcrobatics.apply(model, 1, false, 10, 0, true);
        assertEquals(-105, part(model.bipedRightArm).rotation.vFinal.x);
    }

    @Test
    void crawlReachesAlternateWhileSwimmingArmsStrokeTogether() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        Animation_AquaAcrobatics.apply(model, 1, false, 0, 1, true);
        assertNotEquals(part(model.bipedRightArm).rotation.vFinal.x, part(model.bipedLeftArm).rotation.vFinal.x);
        Animation_AquaAcrobatics.apply(model, 1, true, 0, 1, true);
        assertEquals(part(model.bipedRightArm).rotation.vFinal.x, part(model.bipedLeftArm).rotation.vFinal.x);
        assertEquals(-part(model.bipedRightArm).rotation.vFinal.z, part(model.bipedLeftArm).rotation.vFinal.z);
    }

    @Test
    void disablingLimbCycleRetainsAFittingLowPose() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        Animation_AquaAcrobatics.apply(model, 1, false, 0, 1, false);
        assertEquals(-105, part(model.bipedRightArm).rotation.vFinal.x);
        Animation_AquaAcrobatics.apply(model, 1, false, 15, 1, false);
        assertEquals(-105, part(model.bipedRightArm).rotation.vFinal.x);
        Animation_AquaAcrobatics.apply(model, 1, true, 15, 1, false);
        assertEquals(-145, part(model.bipedRightArm).rotation.vFinal.x);
    }

    @Test
    void limbsBlendBackToUprightWithAaWithoutChangingSlimOrArmorPivots() {
        for (float scale : new float[] {0, 0.5f, 1}) {
            ModelBendsPlayer model = new ModelBendsPlayer(scale);
            model.initModern();
            model.setSlim(true);
            model.headRotationX = 20;
            float shoulderY = model.bipedRightArm.rotationPointY;
            float elbowX = model.bipedRightForeArm.rotationPointX;
            Animation_AquaAcrobatics.apply(model, 0.5f, false, 0, 0, true);
            assertEquals(-52.5f, part(model.bipedRightArm).rotation.vFinal.x);
            Animation_AquaAcrobatics.apply(model, 0, false, 0, 0, true);
            assertEquals(0, part(model.bipedRightArm).rotation.vFinal.x, 0.0001f);
            assertEquals(20, part(model.bipedHead).rotation.vFinal.x);
            assertEquals(shoulderY, model.bipedRightArm.rotationPointY);
            assertEquals(elbowX, model.bipedRightForeArm.rotationPointX);
        }
    }

    @Test
    void fullCyclesStayFinite() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        for (boolean inWater : new boolean[] {false, true}) {
            for (int tick = 0; tick < 200; tick++) {
                Animation_AquaAcrobatics.apply(model, 1, inWater, tick * 0.25f, 0.4f, true);
                assertTrue(Float.isFinite(part(model.bipedRightArm).rotation.vFinal.x));
                assertTrue(Float.isFinite(part(model.bipedLeftForeLeg).rotation.vFinal.x));
                assertEquals(0, model.renderRotation.vFinal.lengthSquared());
            }
        }
    }

    @Test
    void treadingWaterDoesNotAddHorizontalTiltButLegacySwimmingStillDoes() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        Reference_Swimming.apply(model, 20, false);
        assertEquals(0, model.renderRotation.vFinal.x);
        Reference_Swimming.apply(model, 20, true);
        assertEquals(70, model.renderRotation.vFinal.x);
    }

    private static ModelRendererBends part(ModelRenderer part) {
        return (ModelRendererBends)part;
    }
}
