package net.gobbob.mobends.client.model;

import java.nio.DoubleBuffer;
import net.gobbob.mobends.client.animation.BendsGeometry;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
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
    /** The scale the armor draws its head, body, arms and legs at. */
    private float[] scales;
    private final DoubleBuffer plane = BufferUtils.createDoubleBuffer(4);

    /** This adapter, now drawing {@code armor} on {@code pose}'s bones; {@code scales} as for {@link #scales}. */
    public BonedArmorModel wrap(ModelBiped armor, ModelBendsPlayer pose, float[] scales) {
        this.armor = armor;
        this.pose = pose;
        this.scales = scales;
        return this;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float headYaw, float headPitch, float scale) {
        // Such armor can sit a hair outside the skin; pulled toward the eye, it wins where the depth buffer can't tell.
        boolean offset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-1.0f, -10.0f);
        try {
            this.part(this.armor.bipedHead, "head", null, 0.0f, 0.0f, 0.0f, this.scales[0]);
            this.part(this.armor.bipedHeadwear, "head", null, 0.0f, 0.0f, 0.0f, this.scales[0]);
            this.part(this.armor.bipedBody, "body", null, 0.0f, 0.0f, 0.0f, this.scales[1]);
            this.part(this.armor.bipedRightArm, "rightArm", "rightForeArm", -5.0f, 2.0f, 0.0f, this.scales[2]);
            this.part(this.armor.bipedLeftArm, "leftArm", "leftForeArm", 5.0f, 2.0f, 0.0f, this.scales[2]);
            this.part(this.armor.bipedRightLeg, "rightLeg", "rightForeLeg", -1.9f, 12.0f, 0.0f, this.scales[3]);
            this.part(this.armor.bipedLeftLeg, "leftLeg", "leftForeLeg", 1.9f, 12.0f, 0.0f, this.scales[3]);
        } finally {
            GL11.glPolygonOffset(0.0f, 0.0f);
            if (!offset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
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
        GL11.glEnable(GL11.GL_CLIP_PLANE0);
        try {
            this.clip(down, at, -1.0f);
            GL11.glPushMatrix();
            geometry.attach(bone, 0.0625f);
            GL11.glTranslatef(offsetX, offsetY, offsetZ);
            this.draw(part, scale);
            GL11.glPopMatrix();
            this.clip(down, at, 1.0f);
            GL11.glPushMatrix();
            geometry.attach(lower, 0.0625f);
            GL11.glTranslatef(offsetX - joint.x, offsetY - joint.y, offsetZ - joint.z);
            this.draw(part, scale);
            GL11.glPopMatrix();
        } finally {
            GL11.glDisable(GL11.GL_CLIP_PLANE0);
        }
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
}
