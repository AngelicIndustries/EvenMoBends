package net.gobbob.mobends.client.model.entity;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import org.lwjgl.opengl.GL11;

/**
 * Wild Mobs' zomgus on the zombie's rig: its mushrooms grow from the head, the right arm and the left forearm, where
 * Wild Mobs' own model places them, and follow those bones as they bend.
 */
public class ModelBendsZomgus
extends ModelBendsZombie {
    /** Holds the mushrooms, so they stay out of this model's own parts. */
    private final ModelBase mushrooms = new ModelBase() {};
    private final ModelRenderer[] onHead;
    private final ModelRenderer[] onRightArm;
    private final ModelRenderer[] onLeftForeArm;

    public ModelBendsZomgus() {
        super(0.0f, 0.0f, 64, 64);
        this.mushrooms.textureWidth = 64;
        this.mushrooms.textureHeight = 64;
        // In the frames of the bones they hang on. Wild Mobs hangs the left ones on the whole arm, at y 7: on the rig,
        // that is the forearm, from its pivot.
        this.onHead = new ModelRenderer[] {
            this.mushroom(25, 5, 5, -1.0f, -8.0f, 1.0f, 0.0f, 0.7853982f, 0.0f),
            this.mushroom(25, 5, 5, -1.0f, -8.0f, 1.0f, 0.0f, -0.7853982f, 0.0f)};
        this.onRightArm = new ModelRenderer[] {
            this.mushroom(25, 5, 4, -1.0f, 4.0f, -2.0f, 1.570796f, 0.0f, -0.7853982f),
            this.mushroom(25, 5, 4, -1.0f, 4.0f, -2.0f, 1.570796f, 0.0f, 0.7853982f)};
        ModelRenderer foreArm = this.bipedLeftForeArm;
        float x = 1.0f - foreArm.rotationPointX, y = 7.0f - foreArm.rotationPointY, z = -2.0f - foreArm.rotationPointZ;
        this.onLeftForeArm = new ModelRenderer[] {
            this.mushroom(36, 3, 3, x, y, z, 1.570796f, 0.0f, -0.7853982f),
            this.mushroom(36, 3, 3, x, y, z, 1.570796f, 0.0f, 0.7853982f)};
    }

    /** One of Wild Mobs' flat mushrooms: a plane rising from its pivot, textured from the top row of its skin. */
    private ModelRenderer mushroom(int u, int width, int height, float x, float y, float z, float rx, float ry, float rz) {
        ModelRenderer plane = new ModelRenderer(this.mushrooms, u, 0);
        plane.addBox(-width / 2.0f, -height, 0.0f, width, height, 0);
        plane.setRotationPoint(x, y, z);
        plane.rotateAngleX = rx;
        plane.rotateAngleY = ry;
        plane.rotateAngleZ = rz;
        return plane;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSpeed, float age, float headYaw, float headPitch, float scale) {
        super.render(entity, limbSwing, limbSpeed, age, headYaw, headPitch, scale);
        GL11.glPushMatrix();
        try {
            if (this.isChild) {
                // As ModelBendsZombie draws a child: everything at half size, the head half as big again.
                GL11.glScalef(0.5f, 0.5f, 0.5f);
                GL11.glTranslatef(0.0f, 24.0f * scale, 0.0f);
            }
            this.drawOn(this.bipedHead, this.onHead, scale, this.isChild ? 1.5f : 1.0f);
            this.drawOn(this.bipedRightArm, this.onRightArm, scale, 1.0f);
            this.drawOn(this.bipedLeftForeArm, this.onLeftForeArm, scale, 1.0f);
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** Draws parts in a bone's frame as it is posed now, the bone scaled by {@code grow} for it. */
    private void drawOn(ModelRenderer part, ModelRenderer[] parts, float scale, float grow) {
        if (part.isHidden || !part.showModel) return;
        String bone = this.geometry().rig.name(part);
        if (bone == null) return;
        ModelRendererBends bends = (ModelRendererBends)part;
        GL11.glPushMatrix();
        try {
            bends.scaleX *= grow;
            bends.scaleY *= grow;
            bends.scaleZ *= grow;
            try {
                this.geometry().attach(bone, scale);
            } finally {
                bends.scaleX /= grow;
                bends.scaleY /= grow;
                bends.scaleZ /= grow;
            }
            for (ModelRenderer mushroom : parts) mushroom.render(scale);
        } finally {
            GL11.glPopMatrix();
        }
    }
}
