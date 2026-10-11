package net.gobbob.mobends.compat.wawelauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import net.gobbob.mobends.client.model.ModelBoxBends;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelRenderer;

import org.fentanylsolutions.wawelauth.api.skin.LayerSegment;
import org.junit.jupiter.api.Test;

class WawelAuthLayersTest {

    @Test
    void slicesMatchTheModelsOwnWearBoxes() throws Exception {
        ModelBendsPlayer model = new ModelBendsPlayer(0.0f);
        model.initModern();

        assertSameBox(model.bipedHeadwear, WawelAuthLayers.HAT);
        assertSameBox(wear(model, "bodyWear"), WawelAuthLayers.JACKET);
        assertSameBox(wear(model, "classicRightArmWear"), WawelAuthLayers.RIGHT_SLEEVE_UPPER);
        assertSameBox(wear(model, "classicRightForeArmWear"), WawelAuthLayers.RIGHT_SLEEVE_LOWER);
        assertSameBox(wear(model, "slimRightArmWear"), WawelAuthLayers.RIGHT_SLEEVE_UPPER_SLIM);
        assertSameBox(wear(model, "slimRightForeArmWear"), WawelAuthLayers.RIGHT_SLEEVE_LOWER_SLIM);
        assertSameBox(wear(model, "classicLeftArmWear"), WawelAuthLayers.LEFT_SLEEVE_UPPER);
        assertSameBox(wear(model, "classicLeftForeArmWear"), WawelAuthLayers.LEFT_SLEEVE_LOWER);
        assertSameBox(wear(model, "slimLeftArmWear"), WawelAuthLayers.LEFT_SLEEVE_UPPER_SLIM);
        assertSameBox(wear(model, "slimLeftForeArmWear"), WawelAuthLayers.LEFT_SLEEVE_LOWER_SLIM);
        assertSameBox(wear(model, "rightLegWear"), WawelAuthLayers.RIGHT_PANTS_UPPER);
        assertSameBox(wear(model, "rightForeLegWear"), WawelAuthLayers.RIGHT_PANTS_LOWER);
        assertSameBox(wear(model, "leftLegWear"), WawelAuthLayers.LEFT_PANTS_UPPER);
        assertSameBox(wear(model, "leftForeLegWear"), WawelAuthLayers.LEFT_PANTS_LOWER);
    }

    @Test
    void limbHalvesShareOneBoxAndMeetAtTheJoint() {
        assertHalves(WawelAuthLayers.RIGHT_SLEEVE_UPPER, WawelAuthLayers.RIGHT_SLEEVE_LOWER);
        assertHalves(WawelAuthLayers.LEFT_SLEEVE_UPPER_SLIM, WawelAuthLayers.LEFT_SLEEVE_LOWER_SLIM);
        assertHalves(WawelAuthLayers.RIGHT_PANTS_UPPER, WawelAuthLayers.RIGHT_PANTS_LOWER);
    }

    @Test
    void hidingTheWearRestoresWhatWasShown() {
        ModelBendsPlayer model = new ModelBendsPlayer(0.0f);
        model.initModern();
        model.getBodyWear().showModel = false;

        ModelBendsPlayer.WearState shown = model.hideWear();
        assertTrue(shown.hat);
        assertFalse(shown.jacket);
        assertTrue(shown.rightSleeve);
        assertFalse(model.bipedHeadwear.showModel);
        assertFalse(model.getRightArmWear().showModel);

        model.restoreWear(shown);
        assertTrue(model.bipedHeadwear.showModel);
        assertFalse(model.getBodyWear().showModel);
        assertTrue(model.getRightArmWear().showModel);
        assertTrue(model.getLeftLegWear().showModel);
    }

    private static ModelRenderer wear(ModelBendsPlayer model, String field) throws Exception {
        Field wear = ModelBendsPlayer.class.getDeclaredField(field);
        wear.setAccessible(true);
        return (ModelRenderer)wear.get(model);
    }

    /** The slice covers exactly the wear box, before inflation, with the same texture origin. */
    private static void assertSameBox(ModelRenderer wear, LayerSegment segment) {
        ModelBoxBends box = ((ModelRendererBends)wear).getBox();
        String at = segment.toString();
        assertEquals(box.offsetX, segment.originX(), 1.0e-6f, at);
        assertEquals(box.offsetY, segment.originY() + segment.rowStart(), 1.0e-6f, at);
        assertEquals(box.offsetZ, segment.originZ(), 1.0e-6f, at);
        assertEquals(box.resX, segment.width(), 1.0e-6f, at);
        assertEquals(box.resY, segment.rowEnd() - segment.rowStart(), 1.0e-6f, at);
        assertEquals(box.resZ, segment.depth(), 1.0e-6f, at);
        assertEquals(box.txOffsetX, segment.u(), 1.0e-6f, at);
        assertEquals(box.txOffsetY, segment.v(), 1.0e-6f, at);
    }

    private static void assertHalves(LayerSegment upper, LayerSegment lower) {
        assertEquals(upper.part(), lower.part());
        assertEquals(upper.u(), lower.u());
        assertEquals(upper.v(), lower.v());
        assertEquals(upper.width(), lower.width());
        assertEquals(upper.height(), lower.height());
        assertEquals(upper.rowEnd(), lower.rowStart());
        assertTrue(upper.hasTopCap());
        assertTrue(lower.hasBottomCap());
        assertFalse(upper.hasBottomCap());
        assertFalse(lower.hasTopCap());
    }
}
