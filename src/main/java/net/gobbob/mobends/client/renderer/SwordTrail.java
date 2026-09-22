package net.gobbob.mobends.client.renderer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import net.gobbob.mobends.client.ClientProxy;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * The trail a sword leaves through a swing. Each frame a swing asks for it ({@link #add}), the blade is sampled where
 * the held item is actually drawn, so the trail follows whatever the animation does with the arm and the item.
 * Samples are kept in the player model's frame and drawn from there.
 */
public class SwordTrail {
    /** The blade of a vanilla sword icon, guard to tip, in ItemRenderer's 2D item frame (x and y run against u and v). */
    private static final float[] GUARD = {12 / 16f, 5 / 16f, -1 / 32f}, TIP = {1 / 16f, 15 / 16f, -1 / 32f};
    private static final float LIFETIME = 5.0f;

    public List<TrailPart> trailPartList = new ArrayList<TrailPart>();
    private boolean sampling;
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

    public void reset() {
        this.trailPartList.clear();
    }

    /** A swing wants the blade traced this frame. */
    public void add(ModelBendsPlayer model) {
        this.sampling = true;
    }

    /** Whether the blade should be sampled as the item is drawn this frame; asks again next frame. */
    public boolean takeSample() {
        boolean sample = this.sampling;
        this.sampling = false;
        return sample;
    }

    /** The current GL modelview, column-major. */
    public float[] modelview() {
        this.buffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, this.buffer);
        float[] m = new float[16];
        this.buffer.get(m);
        return m;
    }

    /**
     * Records the blade from the modelview where the held item is placed (just before ItemRenderer draws it), relative
     * to {@code modelFrame}, the modelview the player model draws in.
     */
    public void sample(float[] modelFrame, float[] itemPlacement) {
        // ItemRenderer.renderItem's placement of a 2D item in the hand.
        float[] icon = Affine.multiply(itemPlacement, Affine.translation(0, -0.3f, 0));
        icon = Affine.multiply(icon, Affine.scale(1.5f));
        icon = Affine.multiply(icon, Affine.rotation(50, 0, 1, 0));
        icon = Affine.multiply(icon, Affine.rotation(335, 0, 0, 1));
        icon = Affine.multiply(icon, Affine.translation(-0.9375f, -0.0625f, 0));
        float[] relative = Affine.multiply(Affine.invert(modelFrame), icon);
        TrailPart part = new TrailPart();
        part.guard = Affine.transform(relative, GUARD);
        part.tip = Affine.transform(relative, TIP);
        this.trailPartList.add(part);
    }

    /** Draws the trail; the current modelview must be the frame the player model draws in. */
    public void render() {
        if (this.trailPartList.size() < 2) return;
        Minecraft.getMinecraft().renderEngine.bindTexture(ClientProxy.texture_NULL);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        for (int i = 1; i < this.trailPartList.size(); ++i) {
            TrailPart older = this.trailPartList.get(i - 1), newer = this.trailPartList.get(i);
            vertex(older, true);
            vertex(older, false);
            vertex(newer, false);
            vertex(newer, true);
        }
        GL11.glEnd();
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_CULL_FACE);
    }

    /** As a sample ages it fades and narrows toward the middle of the blade. */
    private static void vertex(TrailPart part, boolean guard) {
        float alpha = Math.max(0, 1 - part.ticksExisted / LIFETIME);
        GL11.glColor4f(1, 1, 1, alpha);
        float[] end = guard ? part.guard : part.tip;
        float[] mid = {(part.guard[0] + part.tip[0]) / 2, (part.guard[1] + part.tip[1]) / 2, (part.guard[2] + part.tip[2]) / 2};
        GL11.glVertex3f(mid[0] + (end[0] - mid[0]) * alpha, mid[1] + (end[1] - mid[1]) * alpha, mid[2] + (end[2] - mid[2]) * alpha);
    }

    public void update(float ticksPerFrame) {
        for (TrailPart part : this.trailPartList) part.ticksExisted += ticksPerFrame;
        this.trailPartList.removeIf(part -> part.ticksExisted > LIFETIME);
    }

    public static class TrailPart {
        float[] guard;
        float[] tip;
        float ticksExisted;
    }

    /** Column-major 4x4 affine matrices, as GL keeps them. */
    static final class Affine {
        private Affine() {}

        static float[] identity() {
            return new float[] {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        }

        static float[] translation(float x, float y, float z) {
            float[] m = identity();
            m[12] = x;
            m[13] = y;
            m[14] = z;
            return m;
        }

        static float[] scale(float s) {
            float[] m = identity();
            m[0] = m[5] = m[10] = s;
            return m;
        }

        /** As glRotatef: {@code degrees} about the unit axis (x, y, z). */
        static float[] rotation(float degrees, float x, float y, float z) {
            double a = Math.toRadians(degrees);
            float c = (float)Math.cos(a), s = (float)Math.sin(a), t = 1 - c;
            float[] m = identity();
            m[0] = t * x * x + c;
            m[1] = t * x * y + s * z;
            m[2] = t * x * z - s * y;
            m[4] = t * x * y - s * z;
            m[5] = t * y * y + c;
            m[6] = t * y * z + s * x;
            m[8] = t * x * z + s * y;
            m[9] = t * y * z - s * x;
            m[10] = t * z * z + c;
            return m;
        }

        static float[] multiply(float[] a, float[] b) {
            float[] m = new float[16];
            for (int col = 0; col < 4; ++col) {
                for (int row = 0; row < 4; ++row) {
                    float sum = 0;
                    for (int k = 0; k < 4; ++k) sum += a[k * 4 + row] * b[col * 4 + k];
                    m[col * 4 + row] = sum;
                }
            }
            return m;
        }

        /** The inverse of an affine matrix (any invertible linear part, e.g. with GL's scales and mirrors). */
        static float[] invert(float[] m) {
            float a = m[0], b = m[4], c = m[8], d = m[1], e = m[5], f = m[9], g = m[2], h = m[6], k = m[10];
            float det = a * (e * k - f * h) - b * (d * k - f * g) + c * (d * h - e * g);
            float[] r = identity();
            r[0] = (e * k - f * h) / det;
            r[4] = (c * h - b * k) / det;
            r[8] = (b * f - c * e) / det;
            r[1] = (f * g - d * k) / det;
            r[5] = (a * k - c * g) / det;
            r[9] = (c * d - a * f) / det;
            r[2] = (d * h - e * g) / det;
            r[6] = (b * g - a * h) / det;
            r[10] = (a * e - b * d) / det;
            for (int row = 0; row < 3; ++row) {
                r[12 + row] = -(r[row] * m[12] + r[4 + row] * m[13] + r[8 + row] * m[14]);
            }
            return r;
        }

        static float[] transform(float[] m, float[] p) {
            return new float[] {
                m[0] * p[0] + m[4] * p[1] + m[8] * p[2] + m[12],
                m[1] * p[0] + m[5] * p[1] + m[9] * p[2] + m[13],
                m[2] * p[0] + m[6] * p[1] + m[10] * p[2] + m[14]};
        }
    }
}
