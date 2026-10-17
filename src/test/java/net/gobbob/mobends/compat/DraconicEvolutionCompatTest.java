package net.gobbob.mobends.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DraconicEvolutionCompatTest {
    private static final float ARM_SCALE = 1.0f / 15.0f;

    @Test
    void repairsFirstPersonCompiledMeshAndReusesTheCorrectedList() throws Exception {
        List<Integer> deleted = new ArrayList<>();
        DraconicEvolutionCompat.ObjScale api = new DraconicEvolutionCompat.ObjScale(ObjMesh.class, deleted::add);
        ObjMesh right = new ObjMesh();
        right.render(1.0f / 16.0f);
        int wrongList = right.displayList;
        assertEquals(1.0f / 16.0f, right.bakedScale);

        api.prepare(right, ARM_SCALE);

        assertEquals(Arrays.asList(wrongList), deleted);
        assertFalse(right.compiled);
        assertEquals(0, right.displayList);
        assertEquals(ARM_SCALE, right.scale);
        assertTrue(((NativePart)right).compiled, "Must not change the parent class's display-list fields");
        assertEquals(99, ((NativePart)right).displayList);
        right.render(ARM_SCALE);
        int correctedList = right.displayList;
        assertEquals(ARM_SCALE, right.bakedScale);

        // Even a later first-person call must use the corrected mesh without another allocation/deletion.
        right.render(1.0f / 16.0f);
        api.prepare(right, ARM_SCALE);
        assertEquals(ARM_SCALE, right.bakedScale);
        assertEquals(correctedList, right.displayList);
        assertTrue(right.compiled);
        assertEquals(Arrays.asList(wrongList), deleted);
        assertEquals(2, right.compilations);
    }

    @Test
    void pinsAnUncompiledArmBeforeEitherRendererCanChooseTheScale() throws Exception {
        List<Integer> deleted = new ArrayList<>();
        DraconicEvolutionCompat.ObjScale api = new DraconicEvolutionCompat.ObjScale(ObjMesh.class, deleted::add);
        ObjMesh arm = new ObjMesh();
        api.prepare(arm, ARM_SCALE);
        assertTrue(deleted.isEmpty());
        assertFalse(arm.compiled);
        arm.render(1.0f / 16.0f);
        assertEquals(ARM_SCALE, arm.bakedScale);
    }

    @Test
    void leavesAnAlreadyCorrectLeftArmListAlone() throws Exception {
        List<Integer> deleted = new ArrayList<>();
        DraconicEvolutionCompat.ObjScale api = new DraconicEvolutionCompat.ObjScale(ObjMesh.class, deleted::add);
        ObjMesh left = new ObjMesh();
        left.render(ARM_SCALE);
        int list = left.displayList;
        api.prepare(left, ARM_SCALE);
        assertTrue(deleted.isEmpty());
        assertTrue(left.compiled);
        assertEquals(list, left.displayList);
        assertEquals(1, left.compilations);
    }

    private static class NativePart {
        private boolean compiled = true;
        private int displayList = 99;
    }

    // DE's scale caching contract, without requiring its optional mod or an OpenGL context in unit tests.
    private static final class ObjMesh extends NativePart {
        public float scale;
        private boolean compiled;
        private int displayList;
        private float bakedScale;
        private int compilations;

        void render(float requestedScale) {
            if (this.compiled) return;
            if (this.scale == 0) this.scale = requestedScale;
            this.bakedScale = this.scale;
            this.displayList = ++this.compilations;
            this.compiled = true;
        }
    }
}
