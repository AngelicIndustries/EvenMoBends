package net.gobbob.mobends.client.model.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.gobbob.mobends.client.model.ModelBoxBends;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import org.junit.jupiter.api.Test;

class ModelBendsPlayerArmorTest {
    private static final double EPSILON = 0.00001;

    @Test
    void ssbCannotOverrideTheShapeChosenFromSalamandersSkin() {
        ModelBendsPlayer skin = new ModelBendsPlayer();
        skin.initModern();
        skin.setSlim(false);
        skin.ssb$setSlim(true);
        assertEquals(false, skin.isSlim());
        skin.setSlim(true);
        skin.ssb$setSlim(false);
        assertEquals(true, skin.isSlim());
    }

    @Test
    void seamAdjustmentPreservesArmorInflationOnBothSides() {
        for (float inflation : new float[] {0.0f, 0.5f, 1.0f}) {
            ModelBendsPlayer model = new ModelBendsPlayer(inflation);
            for (ModelRenderer part : new ModelRenderer[] {
                model.bipedRightArm, model.bipedLeftArm, model.bipedRightLeg, model.bipedLeftLeg
            }) {
                ModelBoxBends box = box(part);
                assertEquals(4.02 + inflation * 2, extent(box, 0), EPSILON);
                assertEquals(6.0 + inflation * 2, extent(box, 1), EPSILON);
                assertEquals(4.02 + inflation * 2, extent(box, 2), EPSILON);
                // Inflation must not change the texture's pixel dimensions.
                assertEquals(4.0f, box.originalResX);
                assertEquals(6.0f, box.originalResY);
                assertEquals(4.0f, box.originalResZ);
            }
            assertEquals(4.0 + inflation * 2, extent(box(model.bipedRightForeArm), 0), EPSILON);
            assertEquals(4.0 + inflation * 2, extent(box(model.bipedLeftForeArm), 0), EPSILON);
        }
    }

    @Test
    void armorEnclosesSsbSleevesAndPantsRatherThanSittingInsideThem() {
        ModelBendsPlayer skin = new ModelBendsPlayer();
        skin.ssb$set64x();
        for (boolean slim : new boolean[] {false, true}) {
            skin.initModern();
            skin.setSlim(slim);
            skin.prepareModernOverlays();
            ModelBendsPlayer chestplate = new ModelBendsPlayer(1.0f);
            assertEncloses(chestplate.bipedRightArm, skin.getRightArmWear());
            assertEncloses(chestplate.bipedLeftArm, skin.getLeftArmWear());
            assertEncloses(chestplate.bipedBody, skin.getBodyWear());
            ModelBendsPlayer leggings = new ModelBendsPlayer(0.5f);
            assertEncloses(leggings.bipedRightLeg, skin.getRightLegWear());
            assertEncloses(leggings.bipedLeftLeg, skin.getLeftLegWear());
        }
    }

    private static void assertEncloses(ModelRenderer armor, ModelRenderer overlay) {
        for (int axis = 0; axis < 3; ++axis) {
            assertTrue(bound(box(armor), axis, false) < bound(box(overlay), axis, false));
            assertTrue(bound(box(armor), axis, true) > bound(box(overlay), axis, true));
        }
    }

    private static ModelBoxBends box(ModelRenderer part) {
        return (ModelBoxBends)part.cubeList.get(0);
    }

    private static double extent(ModelBoxBends box, int axis) {
        return bound(box, axis, true) - bound(box, axis, false);
    }

    private static double bound(ModelBoxBends box, int axis, boolean maximum) {
        double result = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
        for (PositionTextureVertex vertex : box.vertices) {
            double value = axis == 0 ? vertex.vector3D.xCoord
                : axis == 1 ? vertex.vector3D.yCoord : vertex.vector3D.zCoord;
            result = maximum ? Math.max(result, value) : Math.min(result, value);
        }
        return result;
    }
}
