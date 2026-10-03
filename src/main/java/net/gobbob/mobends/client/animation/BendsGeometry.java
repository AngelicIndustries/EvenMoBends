package net.gobbob.mobends.client.animation;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.base.GeoModelRenderer;
import com.geckolib.renderer.base.GeoRenderer;
import java.nio.FloatBuffer;
import java.util.function.Predicate;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/** Salamander owns the baked mesh and its transforms; native parts are compatibility/pose adapters only. */
public final class BendsGeometry {
    private static final float DEG = (float)(Math.PI / 180);
    private final ResourceLocation resource;
    public final RenderRig rig;
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);

    public BendsGeometry(AnimatedModel model, String name) {
        this.resource = new ResourceLocation("mobends", "geckolib/models/" + name + ".geo.json");
        this.rig = new RenderRig(model);
        if (model instanceof BipedRigModel) {
            bridgeArm((BipedRigModel)model, true);
            bridgeArm((BipedRigModel)model, false);
        }
    }

    private void bridgeArm(BipedRigModel model, boolean right) {
        net.minecraft.client.model.ModelRenderer arm = right ? model.biped().bipedRightArm : model.biped().bipedLeftArm;
        net.minecraft.client.model.ModelRenderer forearm = model.foreArm(right);
        if (arm instanceof net.gobbob.mobends.client.model.ModelRendererBends_SeperatedChild)
            ((net.gobbob.mobends.client.model.ModelRendererBends_SeperatedChild)arm).geometryAttachment = scale -> {
                attach(right ? "rightForeArm" : "leftForeArm", scale);
                // Vanilla/Backhand expect the bent hand frame translated back to the shoulder.
                GL11.glTranslatef(-forearm.rotationPointX * scale, -forearm.rotationPointY * scale, -forearm.rotationPointZ * scale);
            };
    }

    private BakedGeoModel model() {
        // Look up each draw: Salamander atomically replaces its cache on resource reload.
        return GeckoLibResources.getBakedModels().getModel(this.resource);
    }

    public ModelPose pose(BakedGeoModel baked) {
        ModelPose pose = ModelPose.create(baked);
        for (String name : this.rig.rigBones()) {
            BoneSnapshot bone = pose.get(name).orElse(null);
            if (bone == null) continue;
            String aimed = this.rig.rigAimed(name);
            ModelRendererBends part = this.rig.rigPart(aimed == null ? name : aimed);
            ItemBone item = this.rig.rigItem(name);
            if (part != null) {
                boolean hasAim = this.rig.rigAim(name) != null;
                if (aimed != null || !hasAim) {
                    translation(bone, part.position, part.offsetX, part.offsetY, part.offsetZ);
                    double[] before = EulerZYX.multiply(
                        EulerZYX.matrix(part.aim.getX(), part.aim.getY(), part.aim.getZ()),
                        EulerZYX.multiply(EulerZYX.matrix(0, -part.pre_rotation.getY(), 0),
                            EulerZYX.multiply(EulerZYX.matrix(part.pre_rotation.getX(), 0, 0),
                                EulerZYX.matrix(0, 0, part.pre_rotation.getZ()))));
                    if (aimed == null) before = EulerZYX.multiply(before,
                        EulerZYX.matrix(part.rotation.getX(), part.rotation.getY(), part.rotation.getZ()));
                    float[] angles = EulerZYX.angles(before, new float[3]);
                    bone.setRotation(-angles[0] * DEG, -angles[1] * DEG, angles[2] * DEG);
                } else rotation(bone, part.rotation);
                if (aimed == null) {
                    bone.setScale(part.scaleX, part.scaleY, part.scaleZ);
                    boolean hidden = part.isHidden || !part.showModel;
                    bone.skipRender(hidden).skipChildrenRender(hidden && !part.showChildIfHidden);
                }
            } else if (item != null) {
                rotation(bone, item.rotation);
                translation(bone, item.position, 0, 0, 0);
                bone.setScale(item.scaleX, item.scaleY, item.scaleZ);
            }
        }
        return pose;
    }

    private static void rotation(BoneSnapshot bone, SmoothVector3f rotation) {
        bone.setRotation(-rotation.getX() * DEG, -rotation.getY() * DEG, rotation.getZ() * DEG);
    }

    private static void translation(BoneSnapshot bone, SmoothVector3f position, float x, float y, float z) {
        bone.setTranslation(position.getX() + x * 16, -position.getY() - y * 16, position.getZ() + z * 16);
    }

    public void render(float scale) { render(scale, name -> true); }

    public void render(float scale, Predicate<String> visible) {
        BakedGeoModel baked = model();
        ModelPose pose = pose(baked);
        this.buffer.clear();
        GL11.glGetFloat(GL11.GL_CURRENT_COLOR, this.buffer);
        float r = this.buffer.get(0), g = this.buffer.get(1), b = this.buffer.get(2), a = this.buffer.get(3);
        GL11.glPushMatrix();
        try {
            GL11.glTranslatef(0, 24 * scale, 0);
            GL11.glScalef(-16 * scale, -16 * scale, 16 * scale);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            try {
                GeoModelRenderer.render(baked, pose, bone -> visible.test(bone.name()),
                    new GeoRenderer.TessellatorVertexConsumer(tessellator), r, g, b, a);
            } finally {
                tessellator.draw();
            }
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** Native-coordinate frame at a Salamander bone, for held items, capes and mod-provided layers. */
    public Matrix4f transform(BakedGeoModel baked, String bone, float scale) {
        Matrix4f frame = GeoModelRenderer.captureTransforms(baked, pose(baked)).getBone(bone)
            .orElseThrow(() -> new IllegalArgumentException("Missing attachment bone " + bone)).modelSpaceMatrix();
        return new Matrix4f().translate(0, 24 * scale, 0).scale(-16 * scale, -16 * scale, 16 * scale)
            .mul(frame).scale(-1, -1, 1);
    }

    public void attach(String bone, float scale) {
        this.buffer.clear();
        transform(model(), bone, scale).get(this.buffer);
        GL11.glMultMatrix(this.buffer);
    }
}
