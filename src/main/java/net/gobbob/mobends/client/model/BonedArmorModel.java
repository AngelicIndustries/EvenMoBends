package net.gobbob.mobends.client.model;

import java.nio.DoubleBuffer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.gobbob.mobends.client.animation.BendsGeometry;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

/**
 * Draws another mod's armor model on this mod's bones: each of its biped parts, with whatever hangs from them, where
 * the limb it stands for is posed instead of in vanilla's pose. A limb's part bends with it: what lies above the elbow
 * or knee rides the upper bone and the rest the lower one, cut apart at the joint like a mitred pipe, on the plane
 * halfway between the two, so the halves meet without a gap however far the joint bends.
 */
public final class BonedArmorModel extends ModelBiped {
    /** Armor models known to draw themselves in several passes, by class: Avaritia's infinity armor. */
    private static final Set<String> SELF_DRAWING = new HashSet<String>(
        Arrays.asList("fox.spiteful.avaritia.render.ModelArmorInfinity"));
    private ModelBiped armor;
    private ModelBendsPlayer pose;
    /** The scale the armor draws its head, body, arms and legs at; null for the scale it is drawn with. */
    private float[] scales;
    /** Whether the armor picks and places its parts in its own {@code render}, which then runs with nothing drawn. */
    private boolean rendered;
    private final float[] drawScales = new float[4];
    private final DoubleBuffer plane = BufferUtils.createDoubleBuffer(4);
    /** The side of a joint the limb piece being drawn keeps, while cutting one; else null. */
    private Cut cut;

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
        if (this.rendered && drawsItself(this.armor)) {
            this.renderItself(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scale);
            return;
        }
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
     * Whether {@code armor} draws itself in several passes, such as a shader overlay over its base and glowing parts,
     * which drawing its parts once would lose. Such armor renders itself, its parts riding the bones.
     */
    static boolean drawsItself(ModelBiped armor) {
        return SELF_DRAWING.contains(armor.getClass().getName());
    }

    /**
     * The armor's own render, every pass of it, with the parts of it and of the bipeds it draws along (such as an
     * overlay) standing in for their bones: each part it draws is drawn on its bone, as {@link #render} draws them.
     */
    private void renderItself(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float scale) {
        List<OnBone> standIns = new ArrayList<OnBone>();
        this.standIn(this.armor, standIns);
        for (Field field : this.armor.getClass().getDeclaredFields()) {
            if (!ModelBiped.class.isAssignableFrom(field.getType()) || Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object inner = field.get(this.armor);
                if (inner != this.armor && inner instanceof ModelBiped) this.standIn((ModelBiped)inner, standIns);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // A biped it keeps out of reach draws where it puts it
            }
        }
        boolean offset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -10.0f);
        try {
            this.armor.render(entity, limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch, scale);
        } finally {
            GL11.glPolygonOffset(0.0f, 0.0f);
            if (!offset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            for (int i = standIns.size() - 1; i >= 0; i--) standIns.get(i).restore();
        }
    }

    /** Puts stand-ins in place of {@code model}'s parts, collected so they can be put back. */
    private void standIn(ModelBiped model, List<OnBone> standIns) {
        model.bipedHead = this.standIn(model, model.bipedHead, Part.HEAD, standIns, 0);
        model.bipedHeadwear = this.standIn(model, model.bipedHeadwear, Part.HEAD, standIns, 1);
        model.bipedBody = this.standIn(model, model.bipedBody, Part.BODY, standIns, 2);
        model.bipedRightArm = this.standIn(model, model.bipedRightArm, Part.RIGHT_ARM, standIns, 3);
        model.bipedLeftArm = this.standIn(model, model.bipedLeftArm, Part.LEFT_ARM, standIns, 4);
        model.bipedRightLeg = this.standIn(model, model.bipedRightLeg, Part.RIGHT_LEG, standIns, 5);
        model.bipedLeftLeg = this.standIn(model, model.bipedLeftLeg, Part.LEFT_LEG, standIns, 6);
    }

    private ModelRenderer standIn(ModelBiped model, ModelRenderer part, Part which, List<OnBone> standIns, int slot) {
        if (part == null || part instanceof OnBone) return part;
        OnBone standIn = new OnBone(model, part, which, slot);
        standIns.add(standIn);
        return standIn;
    }

    /**
     * Stands in for one of an armor's parts while it renders itself: the visibility its passes give the stand-in goes to
     * the real part, drawn on its bone.
     */
    private final class OnBone extends ModelRenderer {
        private final ModelBiped model;
        private final ModelRenderer part;
        private final Part which;
        private final int slot;
        private final boolean showModel0, isHidden0;

        OnBone(ModelBiped model, ModelRenderer part, Part which, int slot) {
            super(model);
            model.boxList.remove(this);
            this.model = model;
            this.part = part;
            this.which = which;
            this.slot = slot;
            this.showModel = this.showModel0 = part.showModel;
            this.isHidden = this.isHidden0 = part.isHidden;
        }

        @Override
        public void render(float scale) {
            this.part.showModel = this.showModel;
            this.part.isHidden = this.isHidden;
            BonedArmorModel.this.part(this.part, this.which, scale);
        }

        @Override
        public void renderWithRotation(float scale) {
            this.render(scale);
        }

        @Override
        public void postRender(float scale) {
            this.part.postRender(scale);
        }

        /** The real part back in its field, as visible as it was. */
        void restore() {
            this.part.showModel = this.showModel0;
            this.part.isHidden = this.isHidden0;
            switch (this.slot) {
                case 0: this.model.bipedHead = this.part; break;
                case 1: this.model.bipedHeadwear = this.part; break;
                case 2: this.model.bipedBody = this.part; break;
                case 3: this.model.bipedRightArm = this.part; break;
                case 4: this.model.bipedLeftArm = this.part; break;
                case 5: this.model.bipedRightLeg = this.part; break;
                default: this.model.bipedLeftLeg = this.part; break;
            }
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

    /**
     * {@code part}, something another mod hangs on a player, drawn on {@code pose}'s bone for {@code which}, whole,
     * in the frame the player's model is drawn in. {@code local}, run first, places it from the part's vanilla pivot,
     * as that mod places it from the vanilla model; with {@code ownPose} the part keeps its own pivot and turn, such as
     * a wing's flap, else the bone's pose stands in for it.
     */
    public void drawAttached(ModelBendsPlayer pose, ModelRenderer part, Part which, Runnable local, boolean ownPose,
        float scale) {
        if (part == null || part.isHidden || !part.showModel) return;
        this.pose = pose;
        BendsGeometry geometry = pose.geometry();
        Vector3f rest = geometry.restPivot(which.bone);
        GL11.glPushMatrix();
        try {
            geometry.attach(which.bone, 0.0625f);
            GL11.glTranslatef(which.x * scale - rest.x * 0.0625f, which.y * scale - rest.y * 0.0625f, which.z * scale - rest.z * 0.0625f);
            if (local != null) local.run();
            if (ownPose) part.render(scale);
            else this.draw(part, scale);
        } finally {
            GL11.glPopMatrix();
        }
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
        // GL's clip plane cuts the limb, but a shader drawing the armor, such as an armor's own overlay, may ignore it:
        // then the boxes are cut here, and the plane only cuts other mods' pieces.
        boolean shader = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) != 0;
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        try {
            this.clip(down, at, -1.0f);
            GL11.glPushMatrix();
            geometry.attach(bone, 0.0625f);
            GL11.glTranslatef(offsetX, offsetY, offsetZ);
            this.cut = shader ? Cut.of(new Matrix4f(upperFrame).translate(offsetX, offsetY, offsetZ), down, at, -1.0f) : null;
            this.draw(part, scale, jointY, cut);
            GL11.glPopMatrix();
            this.clip(down, at, 1.0f);
            GL11.glPushMatrix();
            geometry.attach(lower, 0.0625f);
            GL11.glTranslatef(offsetX - joint.x, offsetY - joint.y, offsetZ - joint.z);
            this.cut = shader ? Cut.of(
                new Matrix4f(lowerFrame).translate(offsetX - joint.x, offsetY - joint.y, offsetZ - joint.z), down, at, 1.0f)
                : null;
            this.draw(part, scale, jointY, cut);
            GL11.glPopMatrix();
        } finally {
            this.cut = null;
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
            Cut cut = share == Share.CUT && this.cut != null ? this.cut.moved(part.offsetX, part.offsetY, part.offsetZ) : null;
            if (part.cubeList != null) for (Object box : part.cubeList) {
                ModelBox cube = (ModelBox)box;
                if (share(cube.posY1, cube.posY2, jointY) != share) continue;
                if (cut == null || !cut.draw(cube, scale)) cube.render(Tessellator.instance, scale);
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
    /**
     * One side of a joint's cut, in a piece's own frame: the boxes of a limb's armor are cut there, keeping what lies on
     * that side, so each bone draws its half when a shader drawing it ignores GL's clip planes.
     */
    static final class Cut {
        private static Field quads;
        /** The plane's normal and offset in the piece's frame: a point v is kept where normal . v + offset >= 0. */
        final Vector3f normal;
        final float offset;

        private Cut(Vector3f normal, float offset) {
            this.normal = normal;
            this.offset = offset;
        }

        /**
         * The side of the plane {@code down . p = at} in the frame the limb is drawn from, {@code side} -1 above it and 1
         * below, seen from a piece drawn with {@code frame}.
         */
        static Cut of(Matrix4f frame, Vector3f down, float at, float side) {
            Vector3f normal = frame.transpose3x3(new Matrix3f()).transform(new Vector3f(down)).mul(side);
            float offset = side * (down.dot(frame.getTranslation(new Vector3f())) - at);
            return new Cut(normal, offset);
        }

        /** As seen from inside this frame, moved by (x, y, z). */
        Cut moved(float x, float y, float z) {
            return new Cut(this.normal, this.offset + this.normal.x * x + this.normal.y * y + this.normal.z * z);
        }

        float distance(float x, float y, float z) {
            return this.normal.x * x + this.normal.y * y + this.normal.z * z + this.offset;
        }

        /** Draws the kept part of the box, as {@link ModelBox#render} draws it whole; false if its faces are out of reach. */
        boolean draw(ModelBox box, float scale) {
            TexturedQuad[] faces = faces(box);
            if (faces == null) return false;
            Tessellator tessellator = Tessellator.instance;
            float[] x = new float[8], y = new float[8], z = new float[8], u = new float[8], v = new float[8];
            for (TexturedQuad face : faces) {
                if (face == null || face.nVertices < 3) continue;
                int count = this.keep(face, scale, x, y, z, u, v);
                if (count < 3) continue;
                // The face's normal as vanilla's TexturedQuad works it out: (v2 - v1) x (v0 - v1)
                Vec3 p0 = face.vertexPositions[0].vector3D, p1 = face.vertexPositions[1].vector3D, p2 = face.vertexPositions[2].vector3D;
                double ax = p0.xCoord - p1.xCoord, ay = p0.yCoord - p1.yCoord, az = p0.zCoord - p1.zCoord;
                double bx = p2.xCoord - p1.xCoord, by = p2.yCoord - p1.yCoord, bz = p2.zCoord - p1.zCoord;
                double nx = by * az - bz * ay, ny = bz * ax - bx * az, nz = bx * ay - by * ax;
                double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (length < 1.0e-4) length = 1.0;
                tessellator.startDrawingQuads();
                tessellator.setNormal((float)(nx / length), (float)(ny / length), (float)(nz / length));
                // A fan of quads, a triangle repeating its last corner
                for (int i = 1; i + 1 < count; i += 2) {
                    int last = Math.min(i + 2, count - 1);
                    tessellator.addVertexWithUV(x[0], y[0], z[0], u[0], v[0]);
                    tessellator.addVertexWithUV(x[i], y[i], z[i], u[i], v[i]);
                    tessellator.addVertexWithUV(x[i + 1], y[i + 1], z[i + 1], u[i + 1], v[i + 1]);
                    tessellator.addVertexWithUV(x[last], y[last], z[last], u[last], v[last]);
                }
                tessellator.draw();
            }
            return true;
        }

        /** The face's corners on the kept side, scaled, with new corners where its edges cross the plane. */
        private int keep(TexturedQuad face, float scale, float[] x, float[] y, float[] z, float[] u, float[] v) {
            int count = 0;
            int n = Math.min(face.nVertices, face.vertexPositions.length);
            for (int i = 0; i < n && count < 7; i++) {
                PositionTextureVertex from = face.vertexPositions[i], to = face.vertexPositions[(i + 1) % n];
                float x0 = (float)from.vector3D.xCoord * scale, y0 = (float)from.vector3D.yCoord * scale, z0 = (float)from.vector3D.zCoord * scale;
                float x1 = (float)to.vector3D.xCoord * scale, y1 = (float)to.vector3D.yCoord * scale, z1 = (float)to.vector3D.zCoord * scale;
                float d0 = this.distance(x0, y0, z0), d1 = this.distance(x1, y1, z1);
                if (d0 >= 0.0f) {
                    x[count] = x0; y[count] = y0; z[count] = z0; u[count] = from.texturePositionX; v[count] = from.texturePositionY;
                    count++;
                }
                if ((d0 >= 0.0f) != (d1 >= 0.0f)) {
                    float t = d0 / (d0 - d1);
                    x[count] = x0 + (x1 - x0) * t; y[count] = y0 + (y1 - y0) * t; z[count] = z0 + (z1 - z0) * t;
                    u[count] = from.texturePositionX + (to.texturePositionX - from.texturePositionX) * t;
                    v[count] = from.texturePositionY + (to.texturePositionY - from.texturePositionY) * t;
                    count++;
                }
            }
            return count;
        }

        private static TexturedQuad[] faces(ModelBox box) {
            try {
                if (quads == null) {
                    Field field;
                    try {
                        field = ModelBox.class.getDeclaredField("field_78254_i");
                    } catch (NoSuchFieldException e) {
                        field = ModelBox.class.getDeclaredField("quadList");
                    }
                    field.setAccessible(true);
                    quads = field;
                }
                return (TexturedQuad[])quads.get(box);
            } catch (ReflectiveOperationException | RuntimeException e) {
                return null;
            }
        }
    }

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
        LEFT_HAND("leftForeArm", null, 5.0f, 2.0f, 0.0f),
        /** A leg part worn on the foot, such as a boot: the whole of it rides the shin. */
        RIGHT_FOOT("rightForeLeg", null, -1.9f, 12.0f, 0.0f),
        LEFT_FOOT("leftForeLeg", null, 1.9f, 12.0f, 0.0f);

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
