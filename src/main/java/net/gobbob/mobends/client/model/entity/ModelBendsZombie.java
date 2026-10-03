package net.gobbob.mobends.client.model.entity;

import net.gobbob.mobends.client.animation.BipedRigModel;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.ModelRendererBends_SeperatedChild;
import net.gobbob.mobends.data.Data_Zombie;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

@SideOnly(value=Side.CLIENT)
public class ModelBendsZombie
extends ModelBiped implements BipedRigModel {
    private float geometryInflate;
    private net.gobbob.mobends.client.animation.BendsGeometry geometry;

    public String geometryName() {
        if (this.geometryInflate == 1) return "zombie_armor_outer";
        if (this.geometryInflate == 0.5f) return "zombie_armor_inner";
        if (this instanceof ModelBendsZombieVillagerEtFuturum) return "zombie_villager_modern";
        if (this instanceof ModelBendsZombieVillager) return "zombie_villager";
        return "zombie";
    }

    public net.gobbob.mobends.client.animation.BendsGeometry geometry() {
        if (this.geometry == null) this.geometry = new net.gobbob.mobends.client.animation.BendsGeometry(this, geometryName());
        return this.geometry;
    }

    public ModelRenderer bipedRightForeArm;
    public ModelRenderer bipedLeftForeArm;
    public ModelRenderer bipedRightForeLeg;
    public ModelRenderer bipedLeftForeLeg;
    public SmoothVector3f renderOffset = new SmoothVector3f();
    public SmoothVector3f renderRotation = new SmoothVector3f();
    public float headRotationX;
    public float headRotationY;
    public float armSwing;
    public float armSwingAmount;

    @Override
    public ModelBiped biped() {
        return this;
    }

    @Override
    public ModelRenderer foreArm(boolean right) {
        return right ? this.bipedRightForeArm : this.bipedLeftForeArm;
    }

    @Override
    public ModelRenderer foreLeg(boolean right) {
        return right ? this.bipedRightForeLeg : this.bipedLeftForeLeg;
    }

    @Override
    public SmoothVector3f rootOffset() {
        return this.renderOffset;
    }

    @Override
    public SmoothVector3f rootRotation() {
        return this.renderRotation;
    }

    @Override
    public float headPitch() {
        return this.headRotationX;
    }

    @Override
    public float headYaw() {
        return this.headRotationY;
    }

    @Override
    public float limbSwing() {
        return this.armSwing;
    }

    @Override
    public float limbSwingAmount() {
        return this.armSwingAmount;
    }

    public ModelBendsZombie() {
        this(0.0f, false);
    }

    public ModelBendsZombie(float p_i1168_1_, boolean p_i1168_2_) {
        this(p_i1168_1_, 0.0f, 64, p_i1168_2_ ? 32 : 64);
    }

    protected ModelBendsZombie(float p_i1167_1_, float p_i1167_2_, int p_i1167_3_, int p_i1167_4_) {
        this.geometryInflate = p_i1167_1_;
        this.textureWidth = p_i1167_3_;
        this.textureHeight = p_i1167_4_;
        this.bipedCloak = new ModelRendererBends(this, 0, 0);
        this.bipedCloak.addBox(-5.0f, 0.0f, -1.0f, 10, 16, 1, p_i1167_1_);
        this.bipedEars = new ModelRendererBends(this, 24, 0);
        this.bipedEars.addBox(-3.0f, -6.0f, -1.0f, 6, 6, 1, p_i1167_1_);
        this.bipedHead = new ModelRendererBends(this, 0, 0);
        this.bipedHead.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, p_i1167_1_);
        this.bipedHead.setRotationPoint(0.0f, 0.0f + p_i1167_2_ - 12.0f, 0.0f);
        this.bipedHeadwear = new ModelRendererBends(this, 32, 0);
        this.bipedHeadwear.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, p_i1167_1_ + 0.5f);
        this.bipedHeadwear.setRotationPoint(0.0f, 0.0f, 0.0f);
        this.bipedBody = new ModelRendererBends(this, 16, 16).setShowChildIfHidden(true);
        this.bipedBody.addBox(-4.0f, -12.0f, -2.0f, 8, 12, 4, p_i1167_1_);
        this.bipedBody.setRotationPoint(0.0f, 0.0f + p_i1167_2_ + 12.0f, 0.0f);
        this.bipedRightArm = new ModelRendererBends_SeperatedChild(this, 40, 16).setMother((ModelRendererBends)this.bipedBody);
        this.bipedRightArm.addBox(-3.0f, -2.0f, -2.0f, 4, 6, 4, p_i1167_1_);
        this.bipedRightArm.setRotationPoint(-5.0f, 2.0f + p_i1167_2_ - 12.0f, 0.0f);
        this.bipedLeftArm = new ModelRendererBends_SeperatedChild(this, 40, 16).setMother((ModelRendererBends)this.bipedBody);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0f, -2.0f, -2.0f, 4, 6, 4, p_i1167_1_);
        this.bipedLeftArm.setRotationPoint(5.0f, 2.0f + p_i1167_2_ - 12.0f, 0.0f);
        this.bipedRightLeg = new ModelRendererBends(this, 0, 16);
        this.bipedRightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, p_i1167_1_);
        this.bipedRightLeg.setRotationPoint(-1.9f, 12.0f + p_i1167_2_, 0.0f);
        this.bipedLeftLeg = new ModelRendererBends(this, 0, 16);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, p_i1167_1_);
        this.bipedLeftLeg.setRotationPoint(1.9f, 12.0f + p_i1167_2_, 0.0f);
        this.bipedRightForeArm = new ModelRendererBends(this, 40, 22);
        this.bipedRightForeArm.addBox(0.0f, 0.0f, -4.0f, 4, 6, 4, p_i1167_1_);
        this.bipedRightForeArm.setRotationPoint(-3.0f, 4.0f, 2.0f);
        ((ModelRendererBends)this.bipedRightForeArm).getBox().offsetTextureQuad(this.bipedRightForeArm, 3, 0.0f, -6.0f);
        this.bipedLeftForeArm = new ModelRendererBends(this, 40, 22);
        this.bipedLeftForeArm.mirror = true;
        this.bipedLeftForeArm.addBox(0.0f, 0.0f, -4.0f, 4, 6, 4, p_i1167_1_);
        this.bipedLeftForeArm.setRotationPoint(-1.0f, 4.0f, 2.0f);
        ((ModelRendererBends)this.bipedLeftForeArm).getBox().offsetTextureQuad(this.bipedRightForeArm, 3, 0.0f, -6.0f);
        this.bipedRightForeLeg = new ModelRendererBends(this, 0, 22);
        this.bipedRightForeLeg.addBox(-2.0f, 0.0f, 0.0f, 4, 6, 4, p_i1167_1_);
        this.bipedRightForeLeg.setRotationPoint(0.0f, 6.0f, -2.0f);
        ((ModelRendererBends)this.bipedRightForeLeg).getBox().offsetTextureQuad(this.bipedRightForeLeg, 3, 0.0f, -6.0f);
        this.bipedLeftForeLeg = new ModelRendererBends(this, 0, 22);
        this.bipedLeftForeLeg.mirror = true;
        this.bipedLeftForeLeg.addBox(-2.0f, 0.0f, 0.0f, 4, 6, 4, p_i1167_1_);
        this.bipedLeftForeLeg.setRotationPoint(0.0f, 6.0f, -2.0f);
        ((ModelRendererBends)this.bipedLeftForeLeg).getBox().offsetTextureQuad(this.bipedLeftForeLeg, 3, 0.0f, -6.0f);
        this.bipedBody.addChild(this.bipedHead);
        this.bipedBody.addChild(this.bipedRightArm);
        this.bipedBody.addChild(this.bipedLeftArm);
        this.bipedHead.addChild(this.bipedHeadwear);
        this.bipedRightArm.addChild(this.bipedRightForeArm);
        this.bipedLeftArm.addChild(this.bipedLeftForeArm);
        this.bipedRightLeg.addChild(this.bipedRightForeLeg);
        this.bipedLeftLeg.addChild(this.bipedLeftForeLeg);
        ((ModelRendererBends_SeperatedChild)this.bipedRightArm).setSeperatedPart((ModelRendererBends)this.bipedRightForeArm);
        ((ModelRendererBends_SeperatedChild)this.bipedLeftArm).setSeperatedPart((ModelRendererBends)this.bipedLeftForeArm);
        ((ModelRendererBends)this.bipedRightArm).offsetBox_Add(-0.01f, 0.0f, -0.01f).resizeBox(4.02f, 6.0f, 4.02f).updateVertices();
        ((ModelRendererBends)this.bipedLeftArm).offsetBox_Add(-0.01f, 0.0f, -0.01f).resizeBox(4.02f, 6.0f, 4.02f).updateVertices();
        ((ModelRendererBends)this.bipedRightLeg).offsetBox_Add(-0.01f, 0.0f, -0.01f).resizeBox(4.02f, 6.0f, 4.02f).updateVertices();
        ((ModelRendererBends)this.bipedLeftLeg).offsetBox_Add(-0.01f, 0.0f, -0.01f).resizeBox(4.02f, 6.0f, 4.02f).updateVertices();
    }

    @Override
    public void render(Entity argEntity, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float p_78088_7_) {
        this.setRotationAngles(p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, p_78088_7_, argEntity);
        if (this.isChild) {
            float f6 = 2.0f;
            ((ModelRendererBends)this.bipedHead).scaleX *= 1.5f;
            ((ModelRendererBends)this.bipedHead).scaleY *= 1.5f;
            ((ModelRendererBends)this.bipedHead).scaleZ *= 1.5f;
            GL11.glPushMatrix();
            GL11.glScalef((float)(1.0f / f6), (float)(1.0f / f6), (float)(1.0f / f6));
            GL11.glTranslatef((float)0.0f, (float)(24.0f * p_78088_7_), (float)0.0f);
            this.geometry().render(p_78088_7_);
            GL11.glPopMatrix();
            ((ModelRendererBends)this.bipedHead).scaleX /= 1.5f;
            ((ModelRendererBends)this.bipedHead).scaleY /= 1.5f;
            ((ModelRendererBends)this.bipedHead).scaleZ /= 1.5f;
        } else {
            this.geometry().render(p_78088_7_);
        }
    }

    @Override
    public void setRotationAngles(float argSwingTime, float argSwingAmount, float argArmSway, float argHeadY, float argHeadX, float argNr6, Entity argEntity) {
        if (Minecraft.getMinecraft().theWorld == null) {
            return;
        }
        if (Minecraft.getMinecraft().theWorld.isRemote) {
            // empty if block
        }
        Data_Zombie data = Data_Zombie.get(argEntity.getEntityId());
        this.armSwing = argSwingTime;
        this.armSwingAmount = argSwingAmount;
        this.headRotationX = argHeadX;
        this.headRotationY = argHeadY;
        ((ModelRendererBends)this.bipedHead).sync(data.head);
        ((ModelRendererBends)this.bipedHeadwear).sync(data.headwear);
        ((ModelRendererBends)this.bipedBody).sync(data.body);
        ((ModelRendererBends)this.bipedRightArm).sync(data.rightArm);
        ((ModelRendererBends)this.bipedLeftArm).sync(data.leftArm);
        ((ModelRendererBends)this.bipedRightLeg).sync(data.rightLeg);
        ((ModelRendererBends)this.bipedLeftLeg).sync(data.leftLeg);
        ((ModelRendererBends)this.bipedRightForeArm).sync(data.rightForeArm);
        ((ModelRendererBends)this.bipedLeftForeArm).sync(data.leftForeArm);
        ((ModelRendererBends)this.bipedRightForeLeg).sync(data.rightForeLeg);
        ((ModelRendererBends)this.bipedLeftForeLeg).sync(data.leftForeLeg);
        this.renderOffset.set(data.renderOffset);
        this.renderRotation.set(data.renderRotation);
        if (Data_Zombie.get(argEntity.getEntityId()).canBeUpdated()) {
            this.renderOffset.setSmooth(new Vector3f(0.0f, -1.0f, 0.0f), 0.5f);
            this.renderRotation.setSmooth(new Vector3f(0.0f, 0.0f, 0.0f), 0.5f);
            ((ModelRendererBends)this.bipedHead).resetScale();
            ((ModelRendererBends)this.bipedHeadwear).resetScale();
            ((ModelRendererBends)this.bipedBody).resetScale();
            ((ModelRendererBends)this.bipedRightArm).resetScale();
            ((ModelRendererBends)this.bipedLeftArm).resetScale();
            ((ModelRendererBends)this.bipedRightLeg).resetScale();
            ((ModelRendererBends)this.bipedLeftLeg).resetScale();
            ((ModelRendererBends)this.bipedRightForeArm).resetScale();
            ((ModelRendererBends)this.bipedLeftForeArm).resetScale();
            ((ModelRendererBends)this.bipedRightForeLeg).resetScale();
            ((ModelRendererBends)this.bipedLeftForeLeg).resetScale();
            if (Data_Zombie.get((int)argEntity.getEntityId()).motion.x == 0.0f & Data_Zombie.get((int)argEntity.getEntityId()).motion.z == 0.0f) {
                AnimatedEntity.getByEntity(argEntity).get("stand").animate((EntityLivingBase)argEntity, this, Data_Zombie.get(argEntity.getEntityId()));
            } else {
                AnimatedEntity.getByEntity(argEntity).get("walk").animate((EntityLivingBase)argEntity, this, Data_Zombie.get(argEntity.getEntityId()));
            }
            net.gobbob.mobends.animation.zombie.Animation_Swing.play(this, data, (EntityLivingBase)argEntity);
            ((ModelRendererBends)this.bipedHead).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedHeadwear).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedBody).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedLeftArm).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedRightArm).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedLeftLeg).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedRightLeg).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedLeftForeArm).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedRightForeArm).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedLeftForeLeg).update(data.ticksPerFrame);
            ((ModelRendererBends)this.bipedRightForeLeg).update(data.ticksPerFrame);
            this.renderOffset.update(data.ticksPerFrame);
            this.renderRotation.update(data.ticksPerFrame);
            this.renderRotation.wrapTurns(360.0f);
            data.swordTrail.update(data.ticksPerFrame);
            data.updatedThisFrame = true;
        }
        Data_Zombie.get(argEntity.getEntityId()).syncModelInfo(this);
    }

    public void postRender(float argScale) {
        GL11.glTranslatef((float)(this.renderOffset.vSmooth.x * argScale), (float)(this.renderOffset.vSmooth.y * argScale), (float)(this.renderOffset.vSmooth.z * argScale));
        GL11.glRotatef((float)(-this.renderRotation.getX()), (float)1.0f, (float)0.0f, (float)0.0f);
        GL11.glRotatef((float)(-this.renderRotation.getY()), (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glRotatef((float)this.renderRotation.getZ(), (float)0.0f, (float)0.0f, (float)1.0f);
    }

    public void postRenderArm(float argScale) {
        this.bipedRightArm.postRender(argScale);
        this.bipedRightForeArm.postRender(argScale);
        GL11.glTranslatef((float)(2.0f * argScale), (float)(4.0f * argScale), (float)(2.0f * argScale));
    }

    public void updateWithEntityData(AbstractClientPlayer argPlayer) {
        Data_Zombie data = Data_Zombie.get(argPlayer.getEntityId());
        if (data != null) {
            this.renderOffset.set(data.renderOffset);
            this.renderRotation.set(data.renderRotation);
        }
    }
}

