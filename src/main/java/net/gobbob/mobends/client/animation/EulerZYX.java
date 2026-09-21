package net.gobbob.mobends.client.animation;

/**
 * Rotations in the order {@link net.gobbob.mobends.client.model.ModelRendererBends} and Bedrock bones use:
 * {@code R = Rz(z) * Ry(y) * Rx(x)}, angles in degrees, matrices row-major.
 */
public final class EulerZYX {
    private EulerZYX() {}

    public static double[] matrix(double x, double y, double z) {
        double cx = Math.cos(Math.toRadians(x)), sx = Math.sin(Math.toRadians(x));
        double cy = Math.cos(Math.toRadians(y)), sy = Math.sin(Math.toRadians(y));
        double cz = Math.cos(Math.toRadians(z)), sz = Math.sin(Math.toRadians(z));
        return new double[] {
            cz * cy, cz * sy * sx - sz * cx, cz * sy * cx + sz * sx,
            sz * cy, sz * sy * sx + cz * cx, sz * sy * cx - cz * sx,
            -sy, cy * sx, cy * cx};
    }

    public static double[] multiply(double[] a, double[] b) {
        double[] m = new double[9];
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                m[row * 3 + col] = a[row * 3] * b[col] + a[row * 3 + 1] * b[3 + col] + a[row * 3 + 2] * b[6 + col];
            }
        }
        return m;
    }

    /**
     * Angles {x, y, z} of a rotation matrix. A rotation has two sets of angles (and any multiple of 360 degrees);
     * this returns the one nearest {@code reference}, so smoothing toward it never takes the long way round.
     */
    public static float[] angles(double[] m, float[] reference) {
        double sinY = Math.max(-1, Math.min(1, -m[6]));
        double y1 = Math.toDegrees(Math.asin(sinY));
        double[][] candidates = {solve(m, y1, reference), solve(m, 180 - y1, reference)};
        double[] best = null;
        double bestDistance = Double.MAX_VALUE;
        for (double[] candidate : candidates) {
            double distance = 0;
            for (int axis = 0; axis < 3; ++axis) {
                candidate[axis] = nearest(candidate[axis], reference[axis]);
                distance += (candidate[axis] - reference[axis]) * (candidate[axis] - reference[axis]);
            }
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return new float[] {(float)best[0], (float)best[1], (float)best[2]};
    }

    private static double[] solve(double[] m, double y, float[] reference) {
        double cy = Math.cos(Math.toRadians(y));
        if (Math.abs(cy) > 1e-6) {
            double x = Math.toDegrees(Math.atan2(m[7] / cy, m[8] / cy));
            double z = Math.toDegrees(Math.atan2(m[3] / cy, m[0] / cy));
            return new double[] {x, y, z};
        }
        // Gimbal lock: only x - z (y = 90) or x + z (y = -90) is defined; keep z where it was.
        double z = reference[2];
        double x = Math.sin(Math.toRadians(y)) > 0
            ? z + Math.toDegrees(Math.atan2(m[1], m[2]))
            : Math.toDegrees(Math.atan2(-m[1], -m[2])) - z;
        return new double[] {x, y, z};
    }

    private static double nearest(double angle, double reference) {
        return angle + 360 * Math.rint((reference - angle) / 360);
    }
}
