package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class EulerZYXTest {
    private static final float DEG = (float)(Math.PI / 180.0);

    @Test
    void matrixMatchesGlRotationOrder() {
        Random random = new Random(7);
        for (int i = 0; i < 200; ++i) {
            float x = random.nextFloat() * 720 - 360, y = random.nextFloat() * 720 - 360, z = random.nextFloat() * 720 - 360;
            Matrix4f gl = new Matrix4f().rotateZ(z * DEG).rotateY(y * DEG).rotateX(x * DEG);
            double[] m = EulerZYX.matrix(x, y, z);
            for (int row = 0; row < 3; ++row) {
                for (int col = 0; col < 3; ++col) {
                    assertEquals(gl.get(col, row), m[row * 3 + col], 1e-5);
                }
            }
        }
    }

    @Test
    void anglesRebuildTheSameRotation() {
        Random random = new Random(11);
        for (int i = 0; i < 500; ++i) {
            double[] m = EulerZYX.matrix(random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180);
            float[] reference = {random.nextFloat() * 720 - 360, random.nextFloat() * 720 - 360, random.nextFloat() * 720 - 360};
            float[] a = EulerZYX.angles(m, reference);
            assertArrayEquals(m, EulerZYX.matrix(a[0], a[1], a[2]), 1e-4);
        }
    }

    @Test
    void gimbalLockStillRebuildsTheRotation() {
        for (float y : new float[] {90, -90}) {
            double[] m = EulerZYX.matrix(30, y, 50);
            float[] a = EulerZYX.angles(m, new float[] {0, 0, 50});
            assertArrayEquals(m, EulerZYX.matrix(a[0], a[1], a[2]), 1e-4);
            assertEquals(50, a[2], 1e-3, "keeps the reference z in gimbal lock");
        }
    }

    @Test
    void prefersTheAnglesNearestTheReference() {
        // (x, y, z) and (x + 180, 180 - y, z + 180) are the same rotation; stay near where the part is.
        float[] a = EulerZYX.angles(EulerZYX.matrix(170, 10, 0), new float[] {175, 5, 0});
        assertArrayEquals(new float[] {170, 10, 0}, a, 1e-3f);
        float[] b = EulerZYX.angles(EulerZYX.matrix(170, 10, 0), new float[] {-10, 170, 180});
        assertArrayEquals(new float[] {-10, 170, 180}, b, 1e-3f);
        // Across the +-180 seam it continues instead of wrapping back.
        float[] c = EulerZYX.angles(EulerZYX.matrix(-175, 0, 0), new float[] {170, 0, 0});
        assertArrayEquals(new float[] {185, 0, 0}, c, 1e-3f);
    }
}
