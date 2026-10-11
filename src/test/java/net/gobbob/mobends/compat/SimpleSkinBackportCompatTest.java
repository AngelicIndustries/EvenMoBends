package net.gobbob.mobends.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.gobbob.mobends.client.model.ModelBoxBends;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelRenderer;

import org.junit.jupiter.api.Test;

class SimpleSkinBackportCompatTest {
    @Test
    void leavesLegacyModelsAloneUntilSsbCallsThem() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        assertFalse(model.isModern());
        assertFalse(model.isSlim());
        assertEquals(32, model.textureHeight);
        assertEquals(8.0f, box(model.bipedBody).resX);
        assertEquals(12.0f, box(model.bipedBody).resY);
    }

    @Test
    void modernCallbackPreservesSegmentedHierarchyAndBodyDimensions() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        ModelRenderer body = model.bipedBody;
        ModelRenderer arm = model.bipedRightArm;
        ModelRenderer forearm = model.bipedRightForeArm;
        ((ModelRendererBends)body).compiled = true;

        model.ssb$set64x();

        assertTrue(model.isModern());
        assertEquals(64, model.textureHeight);
        assertSame(body, model.bipedBody);
        assertSame(arm, model.bipedRightArm);
        assertSame(forearm, model.bipedRightForeArm);
        assertTrue(arm.childModels.contains(forearm));
        assertFalse(((ModelRendererBends)body).compiled);
        assertEquals(-4.0f, box(body).offsetX);
        assertEquals(-12.0f, box(body).offsetY);
        assertEquals(-2.0f, box(body).offsetZ);
        assertEquals(8.0f, box(body).resX);
        assertEquals(12.0f, box(body).resY);
        assertEquals(4.0f, box(body).resZ);
        assertBendsHierarchy(body);
        assertBendsHierarchy(model.bipedRightLeg);
        assertBendsHierarchy(model.bipedLeftLeg);
        assertEquals(16.0f, box(model.bipedLeftLeg).txOffsetX);
        assertEquals(48.0f, box(model.bipedLeftLeg).txOffsetY);
        assertEquals(32, model.bipedCloak.textureHeight);
    }

    @Test
    void slimCallbackInitializesRefreshedModelsAndSwitchesBothArmSegmentsWithoutRebuilding() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.ssb$setSlim(true);
        assertTrue(model.isModern());
        assertTrue(model.isSlim());
        assertArmWidths(model, 3);
        assertEquals(-2.0f, model.bipedRightForeArm.rotationPointX);
        int parts = model.boxList.size();
        ModelRenderer slimSleeve = model.getRightArmWear();
        ModelBoxBends torso = box(model.bipedBody);

        model.ssb$set64x();
        assertTrue(model.isSlim(), "Repeated initialization must not reset the current skin model");
        for (int i = 0; i < 3; ++i) {
            model.ssb$setSlim(false);
            assertFalse(model.isSlim());
            assertArmWidths(model, 4);
            assertEquals(-3.0f, model.bipedRightForeArm.rotationPointX);
            model.ssb$setSlim(true);
            assertArmWidths(model, 3);
        }
        assertSame(slimSleeve, model.getRightArmWear());
        assertSame(torso, box(model.bipedBody));
        assertEquals(parts, model.boxList.size());
    }

    @Test
    void firstPersonBridgeInitializesBeforeSettingSlimAndRefreshesPlayerState() throws Exception {
        SimpleSkinBackportCompat.SkinApi api = new SimpleSkinBackportCompat.SkinApi(ModelCallbacks.class, ArmsState.class);
        FirstPersonModel model = new FirstPersonModel();
        assertTrue(api.prepare(model, (ArmsState)() -> true));
        assertTrue(api.prepare(model, (ArmsState)() -> false));
        assertEquals(Arrays.asList("modern", "slim:true", "modern", "slim:false"), model.calls);
    }

    @Test
    void duckInterfaceDispatchUsesInheritedBendsCallbacks() throws Exception {
        SimpleSkinBackportCompat.SkinApi api = new SimpleSkinBackportCompat.SkinApi(ModelCallbacks.class, ArmsState.class);
        SsbPlayerModel model = new SsbPlayerModel();
        assertTrue(api.prepare(model, (ArmsState)() -> true));
        assertTrue(model.isModern());
        assertArmWidths(model, 3);
        assertEquals(12.0f, box(model.bipedBody).resY);
    }

    @Test
    void firstPersonBridgeRejectsUntransformedObjectsWithoutMutatingTheModel() throws Exception {
        SimpleSkinBackportCompat.SkinApi api = new SimpleSkinBackportCompat.SkinApi(ModelCallbacks.class, ArmsState.class);
        FirstPersonModel model = new FirstPersonModel();
        assertFalse(api.prepare(new Object(), (ArmsState)() -> true));
        assertFalse(api.prepare(model, new Object()));
        assertFalse(api.prepare(model, null));
        assertTrue(model.calls.isEmpty());
    }

    private static void assertArmWidths(ModelBendsPlayer model, int width) {
        model.prepareModernOverlays();
        for (ModelRenderer segment : new ModelRenderer[] {
            model.bipedRightArm, model.bipedRightForeArm, model.bipedLeftArm, model.bipedLeftForeArm
        }) {
            assertTrue(segment.cubeList.isEmpty(), "Arm roots must remain animation pivots, not vanilla cubes");
            int visible = 0;
            for (Object entry : segment.childModels) {
                ModelRenderer child = (ModelRenderer)entry;
                if (child.showModel && !child.cubeList.isEmpty()) {
                    ++visible;
                    assertEquals((float)width, box(child).originalResX);
                    assertEquals(6.0f, box(child).originalResY);
                    assertEquals(64.0f, child.textureHeight);
                }
            }
            assertEquals(2, visible, "Each segment needs exactly one base and one sleeve");
        }
    }

    private static ModelBoxBends box(ModelRenderer renderer) {
        return assertInstanceOf(ModelBoxBends.class, renderer.cubeList.get(0));
    }

    private static void assertBendsHierarchy(ModelRenderer renderer) {
        assertInstanceOf(ModelRendererBends.class, renderer);
        // Empty arm roots are animation pivots; only their child meshes sample the texture.
        for (Object box : renderer.cubeList) {
            assertEquals(64.0f, renderer.textureHeight);
            assertInstanceOf(ModelBoxBends.class, box);
        }
        if (renderer.childModels != null) {
            for (Object child : renderer.childModels) {
                assertBendsHierarchy((ModelRenderer)child);
            }
        }
    }

    // The optional mod's two duck interfaces, represented without loading its mixins in unit tests.
    public interface ModelCallbacks {
        void ssb$set64x();
        void ssb$setSlim(boolean slim);
    }

    public interface ArmsState {
        boolean ssb$isSlim();
    }

    // In game SSB adds this interface to ModelBiped, inherited by our model.
    public static class SsbPlayerModel extends ModelBendsPlayer implements ModelCallbacks {}

    public static class FirstPersonModel implements ModelCallbacks {
        final List<String> calls = new ArrayList<>();

        public void ssb$set64x() {
            this.calls.add("modern");
        }

        public void ssb$setSlim(boolean slim) {
            this.calls.add("slim:" + slim);
        }
    }
}
