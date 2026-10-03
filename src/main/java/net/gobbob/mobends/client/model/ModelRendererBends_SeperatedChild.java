package net.gobbob.mobends.client.model;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.minecraft.client.model.ModelBase;
import org.lwjgl.opengl.GL11;

public class ModelRendererBends_SeperatedChild
extends ModelRendererBends {
    ModelRendererBends momModel;
    ModelRendererBends seperatedModel;
    /** Runs at the end of {@link #postRender}, while set: something drawn from this limb goes where it says. */
    public Runnable afterPostRender;
    /** Compatibility entry point used by vanilla equipment renderers and Backhand. */
    public java.util.function.Consumer<Float> geometryAttachment;

    public ModelRendererBends_SeperatedChild(ModelBase argModel) {
        super(argModel);
    }

    public ModelRendererBends_SeperatedChild(ModelBase argModel, String argName) {
        super(argModel, argName);
    }

    public ModelRendererBends_SeperatedChild(ModelBase argModel, int argTexOffsetX, int argTexOffsetY) {
        super(argModel, argTexOffsetX, argTexOffsetY);
    }

    public ModelRendererBends_SeperatedChild setMother(ModelRendererBends argMom) {
        this.momModel = argMom;
        return this;
    }

    public ModelRendererBends_SeperatedChild setSeperatedPart(ModelRendererBends argPart) {
        this.seperatedModel = argPart;
        return this;
    }

    @Override
    public void postRender(float p_78794_1_) {
        if (this.geometryAttachment != null) {
            this.geometryAttachment.accept(p_78794_1_);
            if (this.afterPostRender != null) this.afterPostRender.run();
            return;
        }
        this.updateBends(p_78794_1_);
        this.momModel.postRender(p_78794_1_);
        if (!this.isHidden && this.showModel) {
            if (this.rotateAngleX == 0.0f && this.rotateAngleY == 0.0f && this.rotateAngleZ == 0.0f) {
                if (this.pivotX() != 0.0f || this.pivotY() != 0.0f || this.pivotZ() != 0.0f) {
                    GL11.glTranslatef((float)(this.pivotX() * p_78794_1_), (float)(this.pivotY() * p_78794_1_), (float)(this.pivotZ() * p_78794_1_));
                    this.applyAim();
                    GL11.glRotatef((float)(-this.pre_rotation.getY()), (float)0.0f, (float)1.0f, (float)0.0f);
                    GL11.glRotatef((float)this.pre_rotation.getX(), (float)1.0f, (float)0.0f, (float)0.0f);
                    GL11.glRotatef((float)this.pre_rotation.getZ(), (float)0.0f, (float)0.0f, (float)1.0f);
                }
            } else {
                GL11.glTranslatef((float)(this.pivotX() * p_78794_1_), (float)(this.pivotY() * p_78794_1_), (float)(this.pivotZ() * p_78794_1_));
                this.applyAim();
                GL11.glRotatef((float)(-this.pre_rotation.getY()), (float)0.0f, (float)1.0f, (float)0.0f);
                GL11.glRotatef((float)this.pre_rotation.getX(), (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)this.pre_rotation.getZ(), (float)0.0f, (float)0.0f, (float)1.0f);
                if (this.rotateAngleZ != 0.0f) {
                    GL11.glRotatef((float)(this.rotateAngleZ * 57.295776f), (float)0.0f, (float)0.0f, (float)1.0f);
                }
                if (this.rotateAngleY != 0.0f) {
                    GL11.glRotatef((float)(this.rotateAngleY * 57.295776f), (float)0.0f, (float)1.0f, (float)0.0f);
                }
                if (this.rotateAngleX != 0.0f) {
                    GL11.glRotatef((float)(this.rotateAngleX * 57.295776f), (float)1.0f, (float)0.0f, (float)0.0f);
                }
            }
        }
        this.seperatedModel.postRender(p_78794_1_);
        GL11.glTranslatef((float)(-this.seperatedModel.rotationPointX * p_78794_1_), (float)(-this.seperatedModel.rotationPointY * p_78794_1_), (float)(-this.seperatedModel.rotationPointZ * p_78794_1_));
        if (this.afterPostRender != null) this.afterPostRender.run();
    }
}
