package net.gobbob.mobends.client.model;

import java.nio.DoubleBuffer;
import java.util.Arrays;
import net.gobbob.mobends.client.animation.BendsGeometry;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * Draws another mod's armor model on this mod's bones: each of its biped parts, with whatever hangs from them, where
 * the limb it stands for is posed instead of in vanilla's pose. A limb's part bends with it: what lies above the elbow
 * or knee rides the upper bone and the rest the lower one, cut apart at the joint like a mitred pipe, on the plane
 * halfway between the two, so the halves meet without a gap however far the joint bends.
 */
public final class BonedArmorModel extends ModelBiped {
    private ModelBiped armor;
    private ModelBendsPlayer pose;
    /** The scale the armor draws its head, body, arms and legs at; null for the scale it is drawn with. */
    private float[] scales;
    /** Whether the armor picks and places its parts in its own {@code render}, which then runs with nothing drawn. */
    private boolean rendered;
    private final float[] drawScales = new float[4];
    private final DoubleBuffer plane = BufferUtils.createDoubleBuffer(4);

    /** This adapter, now drawing {@code armor} on {@code pose}'s bones; {@code scales} as for {@link #scales}. */
    public BonedArmorModel wrap(ModelBiped armor, ModelBendsPlayer pose, float[] scales) {
        this.armor = armor;
        this.pose = pose;
        this.scales = scales;
        this.rendered = false;
        return this;
    }

    /**
     * This adapter, now drawing an armor model from {@code getArmorModel} on {@code pose}'s bones. Such models often set
     * up their parts for the slot in {@code render}, so that runs first, drawing nothing.
     */
    public BonedArmorModel wrapRendered(ModelBiped armor, ModelBendsPlayer pose) {
        this.armor = armor;
        this.pose = pose;
        this.scales = null;
        this.rendered = true;
        return this;
    }

    /** Whether {@code model} is one this adapter can draw on the bones: another mod's biped, not one of this mod's. */
    public static boolean adapts(Object model) {
        return model instanceof ModelBiped && !(model instanceof ModelBendsPlayer) && !(model instanceof BonedArmorModel);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        this.armor.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float scale) {
        float[] scales = this.scales;
        if (scales == null) {
            Arrays.fill(this.drawScales, scale);
            scales = this.drawScales;
        }
        if (this.rendered) {
            GL11.glPushAttrib(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            try {
                GL11.glColorMask(false, false, false, false);
                GL11.glDepthMask(false);
                this.armor.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scale);
            } finally {
                GL11.glPopAttrib();
            }
        }
        // Such armor can sit a hair outside the skin; pulled toward the eye, it wins where the depth buffer can't tell.
        boolean offset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -10.0f);
        try {
            this.part(this.armor.bipedHead, Part.HEAD, scales[0]);
            this.part(this.armor.bipedHeadwear, Part.HEAD, scales[0]);
            this.part(this.armor.bipedBody, Part.BODY, scales[1]);
            this.part(this.armor.bipedRightArm, Part.RIGHT_ARM, scales[2]);
            this.part(this.armor.bipedLeftArm, Part.LEFT_ARM, scales[2]);
            this.part(this.armor.bipedRightLeg, Part.RIGHT_LEG, scales[3]);
            this.part(this.armor.bipedLeftLeg, Part.LEFT_LEG, scales[3]);
        } finally {
            GL11.glPolygonOffset(0.0f, 0.0f);
            if (!offset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }

    /**
     * {@code part} of another mod's biped drawn on {@code pose}'s bone for {@code which}, in the frame the player's model
     * is drawn in, as {@link #render} draws an armor's.
     */
    public void drawOn(ModelBendsPlayer pose, ModelRenderer part, Part which, float scale) {
        this.pose = pose;
        this.part(part, which, scale);
    }

    private void part(ModelRenderer part, Part which, float scale) {
        this.part(part, which.bone, which.lower, which.x, which.y, which.z, scale);
    }

    /**
     * {@code part}, whose vanilla pivot is (x, y, z), on {@code bone}, and below the joint on {@code lower} (null for
     * none), whose pivot is the joint.
     */
    private void part(ModelRenderer part, String bone, String lower, float x, float y, float z, float scale) {
        if (part == null || part.isHidden || !part.showModel) return;
        BendsGeometry geometry = this.pose.geometry();
        Vector3f rest = geometry.restPivot(bone);
        // From the bone's pivot to the part's own, in blocks.
        float offsetX = x * scale - rest.x * 0.0625f, offsetY = y * scale - rest.y * 0.0625f, offsetZ = z * scale - rest.z * 0.0625f;
        if (lower == null) {
            GL11.glPushMatrix();
            geometry.attach(bone, 0.0625f);
            GL11.glTranslatef(offsetX, offsetY, offsetZ);
            this.draw(part, scale);
            GL11.glPopMatrix();
            return;
        }
        Matrix4f upperFrame = geometry.transform(bone, 0.0625f), lowerFrame = geometry.transform(lower, 0.0625f);
        // Where the lower bone hangs from the upper one as posed, in the upper bone's frame (blocks).
        Vector3f joint = new Matrix4f(upperFrame).invert().mul(lowerFrame).getTranslation(new Vector3f());
        // The cut, in this frame: through the joint, facing halfway between the two bones' directions down the limb.
        Vector3f down = upperFrame.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).normalize()
            .add(lowerFrame.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f)).normalize());
        if (down.lengthSquared() < 1.0e-6f) down = upperFrame.transformDirection(new Vector3f(0.0f, 1.0f, 0.0f));
        down.normalize();
        float at = down.dot(lowerFrame.getTranslation(new Vector3f()));
        // Another mod's own kind of part draws its own way: it can only be cut as a whole.
        Share cut = part.getClass() == ModelRenderer.class ? Share.CUT : Share.ALL;
        // The joint along the part, in its pixels: pieces clear of it, or small, ride one bone whole.
        float jointY = (joint.y - offsetY) / scale;
        if (cut == Share.CUT) {
            GL11.glPushMatrix();
            geometry.attach(bone, 0.0625f);
            GL11.glTranslatef(offsetX, offsetY, offsetZ);
            this.draw(part, scale, jointY, Share.UPPER);
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            geometry.attach(lower, 0.0625f);
            GL11.glTranslatef(offsetX - joint.x, offsetY - joint.y, offsetZ - joint.z);
            this.draw(part, scale, jointY, Share.LOWER);
            GL11.glPopMatrix();
        }
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        try {
            this.clip(down, at, -1.0f);
            GL11.glPushMatrix();
            geometry.attach(bone, 0.0625f);
            GL11.glTranslatef(offsetX, offsetY, offsetZ);
            this.draw(part, scale, jointY, cut);
            GL11.glPopMatrix();
            this.clip(down, at, 1.0f);
            GL11.glPushMatrix();
            geometry.attach(lower, 0.0625f);
            GL11.glTranslatef(offsetX - joint.x, offsetY - joint.y, offsetZ - joint.z);
            this.draw(part, scale, jointY, cut);
            GL11.glPopMatrix();
        } finally {
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
        }
    }

    /** Which bone a piece of a limb's part rides: the upper or lower one whole, both cut at the joint, or all of it. */
    enum Share {
        UPPER, LOWER, CUT, ALL
    }

    /** A piece shorter than this, in pixels, rides one bone whole. */
    private static final float MIN_CUT_LENGTH = 6.0f;
    /** A piece reaching less than this past the joint, in pixels, rides the bone it mostly lies on. */
    private static final float MIN_CUT_REACH = 2.0f;

    /**
     * The share of a piece spanning {@code lo} to {@code hi} down the limb, against the joint at {@code jointY}: only
     * long pieces well across the joint are cut. Cutting a small one, such as a pauldron's edge, would leave a sliver
     * on the other bone.
     */
    static Share share(float lo, float hi, float jointY) {
        float above = jointY - lo, below = hi - jointY;
        if (below <= 0.0f) return Share.UPPER;
        if (above <= 0.0f) return Share.LOWER;
        if (hi - lo >= MIN_CUT_LENGTH && above >= MIN_CUT_REACH && below >= MIN_CUT_REACH) return Share.CUT;
        return (lo + hi) * 0.5f <= jointY ? Share.UPPER : Share.LOWER;
    }

    /** The pieces of {@code part} in {@code share}, unposed: its boxes one by one, its children whole. */
    private void draw(ModelRenderer part, float scale, float jointY, Share share) {
        if (share == Share.ALL) {
            this.draw(part, scale);
            return;
        }
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(part.offsetX, part.offsetY, part.offsetZ);
            if (part.cubeList != null) for (Object box : part.cubeList) {
                ModelBox cube = (ModelBox)box;
                if (share(cube.posY1, cube.posY2, jointY) == share) cube.render(Tessellator.instance, scale);
            }
            if (part.childModels != null) for (Object child : part.childModels) {
                ModelRenderer piece = (ModelRenderer)child;
                float[] range = range(piece, scale);
                Share own = piece.getClass() == ModelRenderer.class ? share(range[0], range[1], jointY) : Share.CUT;
                if (own == share) piece.render(scale);
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** How far down its parent's limb a child part's boxes reach, in pixels: from, to. */
    private static float[] range(ModelRenderer child, float scale) {
        Matrix4f frame = new Matrix4f()
            .translate(child.offsetX / scale + child.rotationPointX, child.offsetY / scale + child.rotationPointY,
                child.offsetZ / scale + child.rotationPointZ)
            .rotateZ(child.rotateAngleZ).rotateY(child.rotateAngleY).rotateX(child.rotateAngleX);
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
        Vector3f corner = new Vector3f();
        if (child.cubeList != null) for (Object box : child.cubeList) {
            ModelBox cube = (ModelBox)box;
            for (int i = 0; i < 8; i++) {
                frame.transformPosition(corner.set((i & 1) == 0 ? cube.posX1 : cube.posX2,
                    (i & 2) == 0 ? cube.posY1 : cube.posY2, (i & 4) == 0 ? cube.posZ1 : cube.posZ2));
                lo = Math.min(lo, corner.y);
                hi = Math.max(hi, corner.y);
            }
        }
        if (lo > hi) lo = hi = frame.getTranslation(corner).y;
        return new float[] {lo, hi};
    }

    /** Keeps what lies where {@code side} * (normal . p - at) is at least 0, in the current frame. */
    private void clip(Vector3f normal, float at, float side) {
        this.plane.clear();
        this.plane.put(side * normal.x).put(side * normal.y).put(side * normal.z).put(-side * at).flip();
        GL11.glClipPlane(GL11.GL_CLIP_PLANE0, this.plane);
    }

    /** {@code part} unposed, here: its pivot and turn are this frame's. */
    private void draw(ModelRenderer part, float scale) {
        float pointX = part.rotationPointX, pointY = part.rotationPointY, pointZ = part.rotationPointZ;
        float angleX = part.rotateAngleX, angleY = part.rotateAngleY, angleZ = part.rotateAngleZ;
        part.setRotationPoint(0.0f, 0.0f, 0.0f);
        part.rotateAngleX = part.rotateAngleY = part.rotateAngleZ = 0.0f;
        try {
            part.render(scale);
        } finally {
            part.setRotationPoint(pointX, pointY, pointZ);
            part.rotateAngleX = angleX;
            part.rotateAngleY = angleY;
            part.rotateAngleZ = angleZ;
        }
    }

    /** A vanilla biped part: the bone it rides, the bone below its joint, and its vanilla pivot. */
    public enum Part {
        HEAD("head", null, 0.0f, 0.0f, 0.0f),
        BODY("body", null, 0.0f, 0.0f, 0.0f),
        RIGHT_ARM("rightArm", "rightForeArm", -5.0f, 2.0f, 0.0f),
        LEFT_ARM("leftArm", "leftForeArm", 5.0f, 2.0f, 0.0f),
        RIGHT_LEG("rightLeg", "rightForeLeg", -1.9f, 12.0f, 0.0f),
        LEFT_LEG("leftLeg", "leftForeLeg", 1.9f, 12.0f, 0.0f),
        /** An arm part worn on the hand, such as a glove: the whole of it rides the forearm. */
        RIGHT_HAND("rightForeArm", null, -5.0f, 2.0f, 0.0f),
        LEFT_HAND("leftForeArm", null, 5.0f, 2.0f, 0.0f);

        final String bone;
        final String lower;
        final float x, y, z;

        Part(String bone, String lower, float x, float y, float z) {
            this.bone = bone;
            this.lower = lower;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
