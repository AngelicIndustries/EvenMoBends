package net.gobbob.mobends.client.model.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.AnimatedModel;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.animation.PlayerRig;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.data.Data_Spider;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.opengl.GL11;

public class ModelBendsSpider
extends ModelSpider implements AnimatedModel {
    public ModelRenderer spiderHead;
    public ModelRenderer spiderNeck;
    public ModelRenderer spiderBody;
    public ModelRenderer spiderLeg1;
    public ModelRenderer spiderLeg2;
    public ModelRenderer spiderLeg3;
    public ModelRenderer spiderLeg4;
    public ModelRenderer spiderLeg5;
    public ModelRenderer spiderLeg6;
    public ModelRenderer spiderLeg7;
    public ModelRenderer spiderLeg8;
    public ModelRendererBends spiderForeLeg1;
    public ModelRendererBends spiderForeLeg2;
    public ModelRendererBends spiderForeLeg3;
    public ModelRendererBends spiderForeLeg4;
    public ModelRendererBends spiderForeLeg5;
    public ModelRendererBends spiderForeLeg6;
    public ModelRendererBends spiderForeLeg7;
    public ModelRendererBends spiderForeLeg8;
    public SmoothVector3f renderOffset = new SmoothVector3f();
    public SmoothVector3f renderRotation = new SmoothVector3f();
    public float headRotationX;
    public float headRotationY;
    public float limbSwing;
    public float limbSwingAmount;

    public static final ResourceLocation WALK = new ResourceLocation(MoBends.MODID, "spider/walk");

    /**
     * The rig: head, neck and body, and eight legs, {@code rightLeg1} to {@code rightLeg4} and {@code leftLeg1} to
     * {@code leftLeg4} from front to back, each under an aim bone that turns it forward or back and with a foreleg.
     */
    private static final List<String> BONES;
    private static final int[] RIGHT_LEGS = {7, 5, 3, 1}, LEFT_LEGS = {8, 6, 4, 2};

    static {
        List<String> bones = new ArrayList<String>(Arrays.asList("body", "neck", "head"));
        for (int i = 1; i <= 4; ++i) {
            for (String side : new String[] {"right", "left"}) {
                bones.add(side + "Leg" + i + "Aim");
                bones.add(side + "Leg" + i);
                bones.add(side + "ForeLeg" + i);
            }
        }
        BONES = Collections.unmodifiableList(bones);
    }

    @Override
    public List<String> rigBones() {
        return BONES;
    }

    @Override
    public String rigParent(String bone) {
        if (bone.contains("ForeLeg")) return bone.replace("ForeLeg", "Leg");
        if (bone.endsWith("Aim")) return PlayerRig.ROOT;
        if (bone.contains("Leg")) return bone + "Aim";
        return BONES.contains(bone) ? PlayerRig.ROOT : null;
    }

    @Override
    public ModelRendererBends rigPart(String bone) {
        switch (bone) {
            case "head": return (ModelRendererBends)this.spiderHead;
            case "neck": return (ModelRendererBends)this.spiderNeck;
            case "body": return (ModelRendererBends)this.spiderBody;
            default:
        }
        if (bone.endsWith("Aim") || !bone.contains("Leg")) return null;
        boolean right = bone.startsWith("right"), fore = bone.contains("ForeLeg");
        int index;
        try {
            index = Integer.parseInt(bone.substring(bone.length() - 1));
        } catch (NumberFormatException e) {
            return null;
        }
        if (index < 1 || index > 4) return null;
        int leg = (right ? RIGHT_LEGS : LEFT_LEGS)[index - 1];
        return (ModelRendererBends)(fore ? this.foreLegs()[leg - 1] : this.legs()[leg - 1]);
    }

    @Override
    public String rigAimed(String bone) {
        return bone.endsWith("Aim") && BONES.contains(bone) ? bone.substring(0, bone.length() - 3) : null;
    }

    @Override
    public String rigAim(String bone) {
        return bone.contains("Leg") && !bone.contains("ForeLeg") && !bone.endsWith("Aim") ? bone + "Aim" : null;
    }

    /** The legs, spiderLeg1 to spiderLeg8. */
    public ModelRenderer[] legs() {
        return new ModelRenderer[] {this.spiderLeg1, this.spiderLeg2, this.spiderLeg3, this.spiderLeg4,
            this.spiderLeg5, this.spiderLeg6, this.spiderLeg7, this.spiderLeg8};
    }

    public ModelRendererBends[] foreLegs() {
        return new ModelRendererBends[] {this.spiderForeLeg1, this.spiderForeLeg2, this.spiderForeLeg3, this.spiderForeLeg4,
            this.spiderForeLeg5, this.spiderForeLeg6, this.spiderForeLeg7, this.spiderForeLeg8};
    }

    /** The spider's whole-body transform (resting at 0), applied by its renderer. */
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
        return this.limbSwing;
    }

    @Override
    public float limbSwingAmount() {
        return this.limbSwingAmount;
    }

    public ModelBendsSpider() {
        float f = 0.0f;
        int b0 = 15;
        float legLength = 12.0f;
        float foreLegLength = 15.0f;
        float legRatio = 1.0f;
        float foreLegRatio = 1.0f;
        this.spiderHead = new ModelRendererBends(this, 32, 4);
        this.spiderHead.addBox(-4.0f, -4.0f, -8.0f, 8, 8, 8, f);
        this.spiderHead.setRotationPoint(0.0f, b0, -3.0f);
        this.spiderNeck = new ModelRendererBends(this, 0, 0);
        this.spiderNeck.addBox(-3.0f, -3.0f, -3.0f, 6, 6, 6, f);
        this.spiderNeck.setRotationPoint(0.0f, b0, 0.0f);
        this.spiderBody = new ModelRendererBends(this, 0, 12);
        this.spiderBody.addBox(-5.0f, -4.0f, -6.0f, 10, 8, 12, f);
        this.spiderBody.setRotationPoint(0.0f, b0, 9.0f);
        this.spiderLeg1 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg1.addBox(-7.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg1.setRotationPoint(-4.0f, b0, 2.0f);
        ((ModelRendererBends)this.spiderLeg1).offsetBox_Add(8.0f - legLength, -0.01f, -0.01f).resizeBox(legLength, 2.02f, 2.02f).updateVertices();
        this.spiderLeg2 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg2.addBox(-1.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg2.setRotationPoint(4.0f, b0, 2.0f);
        ((ModelRendererBends)this.spiderLeg2).offsetBox_Add(0.0f, -0.01f, -0.01f).resizeBox(legLength, 2.02f, 2.02f).updateVertices();
        this.spiderLeg3 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg3.addBox(-7.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg3.setRotationPoint(-4.0f, b0, 1.0f);
        ((ModelRendererBends)this.spiderLeg3).offsetBox_Add(8.0f - legLength * legRatio, -0.01f, -0.01f).resizeBox(legLength * legRatio, 2.02f, 2.02f).updateVertices();
        this.spiderLeg4 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg4.addBox(-1.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg4.setRotationPoint(4.0f, b0, 1.0f);
        ((ModelRendererBends)this.spiderLeg4).offsetBox_Add(0.0f, -0.01f, -0.01f).resizeBox(legLength * legRatio, 2.02f, 2.02f).updateVertices();
        this.spiderLeg5 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg5.addBox(-7.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg5.setRotationPoint(-4.0f, b0, 0.0f);
        ((ModelRendererBends)this.spiderLeg5).offsetBox_Add(8.0f - legLength * legRatio, -0.01f, -0.01f).resizeBox(legLength * legRatio, 2.02f, 2.02f).updateVertices();
        this.spiderLeg6 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg6.addBox(-1.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg6.setRotationPoint(4.0f, b0, 0.0f);
        ((ModelRendererBends)this.spiderLeg6).offsetBox_Add(0.0f, -0.01f, -0.01f).resizeBox(legLength * legRatio, 2.02f, 2.02f).updateVertices();
        this.spiderLeg7 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg7.addBox(-7.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg7.setRotationPoint(-4.0f, b0, -1.0f);
        ((ModelRendererBends)this.spiderLeg7).offsetBox_Add(8.0f - legLength, -0.01f, -0.01f).resizeBox(legLength, 2.02f, 2.02f).updateVertices();
        this.spiderLeg8 = new ModelRendererBends(this, 18, 0);
        this.spiderLeg8.addBox(-1.0f, -1.0f, -1.0f, 8, 2, 2, f);
        this.spiderLeg8.setRotationPoint(4.0f, b0, -1.0f);
        ((ModelRendererBends)this.spiderLeg8).offsetBox_Add(0.0f, -0.01f, -0.01f).resizeBox(legLength, 2.02f, 2.02f).updateVertices();
        this.spiderForeLeg1 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg1.addBox(-foreLegLength, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg1.setRotationPoint(-legLength + 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg1.resizeBox(foreLegLength, 2.0f, 2.0f).updateVertices();
        this.spiderForeLeg2 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg2.addBox(0.0f, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg2.setRotationPoint(legLength - 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg2.resizeBox(foreLegLength, 2.0f, 2.0f).updateVertices();
        this.spiderForeLeg3 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg3.addBox(-foreLegLength * foreLegRatio, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg3.setRotationPoint(-legLength * legRatio + 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg3.resizeBox(foreLegLength * foreLegRatio, 2.0f, 2.0f).updateVertices();
        this.spiderForeLeg4 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg4.addBox(0.0f, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg4.setRotationPoint(legLength * legRatio - 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg4.resizeBox(foreLegLength * foreLegRatio, 2.0f, 2.0f).updateVertices();
        this.spiderForeLeg5 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg5.addBox(-foreLegLength * foreLegRatio, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg5.setRotationPoint(-legLength * legRatio + 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg5.resizeBox(foreLegLength * foreLegRatio, 2.0f, 2.0f).updateVertices();
        this.spiderForeLeg6 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg6.addBox(0.0f, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg6.setRotationPoint(legLength * legRatio - 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg6.resizeBox(foreLegLength * foreLegRatio, 2.0f, 2.0f).updateVertices();
        this.spiderForeLeg7 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg7.addBox(-foreLegLength, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg7.setRotationPoint(-legLength + 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg7.resizeBox(foreLegLength, 2.0f, 2.0f).updateVertices();
        this.spiderForeLeg8 = new ModelRendererBends(this, 18, 0);
        this.spiderForeLeg8.addBox(0.0f, 0.0f, -1.0f, 8, 2, 2, f);
        this.spiderForeLeg8.setRotationPoint(legLength - 1.0f, -1.0f, 0.0f);
        this.spiderForeLeg8.resizeBox(foreLegLength, 2.0f, 2.0f).updateVertices();
        this.spiderLeg1.addChild(this.spiderForeLeg1);
        this.spiderLeg2.addChild(this.spiderForeLeg2);
        this.spiderLeg3.addChild(this.spiderForeLeg3);
        this.spiderLeg4.addChild(this.spiderForeLeg4);
        this.spiderLeg5.addChild(this.spiderForeLeg5);
        this.spiderLeg6.addChild(this.spiderForeLeg6);
        this.spiderLeg7.addChild(this.spiderForeLeg7);
        this.spiderLeg8.addChild(this.spiderForeLeg8);
    }

    private net.gobbob.mobends.client.animation.BendsGeometry geometry;

    public net.gobbob.mobends.client.animation.BendsGeometry geometry() {
        if (this.geometry == null) this.geometry = new net.gobbob.mobends.client.animation.BendsGeometry(this, "spider");
        return this.geometry;
    }

    @Override
    public void render(Entity p_78088_1_, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float p_78088_7_) {
        this.setRotationAngles(p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, p_78088_7_, p_78088_1_);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)0.0f, (float)0.2f, (float)0.0f);
        this.geometry().render(p_78088_7_);
        GL11.glPopMatrix();
    }

    @Override
    public void setRotationAngles(float p_78087_1_, float p_78087_2_, float p_78087_3_, float p_78087_4_, float p_78087_5_, float p_78087_6_, Entity argEntity) {
        if (Minecraft.getMinecraft().theWorld == null) {
            return;
        }
        if (Minecraft.getMinecraft().theWorld.isRemote && Minecraft.getMinecraft().isGamePaused()) {
            return;
        }
        Data_Spider data = Data_Spider.get(argEntity.getEntityId());
        this.headRotationX = p_78087_5_;
        // 1.7.10 passes head minus body yaw unwrapped, whole turns apart; blends would spin through them.
        this.headRotationY = MathHelper.wrapAngleTo180_float(p_78087_4_);
        ((ModelRendererBends)this.spiderHead).sync(data.spiderHead);
        ((ModelRendererBends)this.spiderNeck).sync(data.spiderNeck);
        ((ModelRendererBends)this.spiderBody).sync(data.spiderBody);
        ((ModelRendererBends)this.spiderLeg1).sync(data.spiderLeg1);
        ((ModelRendererBends)this.spiderLeg2).sync(data.spiderLeg2);
        ((ModelRendererBends)this.spiderLeg3).sync(data.spiderLeg3);
        ((ModelRendererBends)this.spiderLeg4).sync(data.spiderLeg4);
        ((ModelRendererBends)this.spiderLeg5).sync(data.spiderLeg5);
        ((ModelRendererBends)this.spiderLeg6).sync(data.spiderLeg6);
        ((ModelRendererBends)this.spiderLeg7).sync(data.spiderLeg7);
        ((ModelRendererBends)this.spiderLeg8).sync(data.spiderLeg8);
        this.spiderForeLeg1.sync(data.spiderForeLeg1);
        this.spiderForeLeg2.sync(data.spiderForeLeg2);
        this.spiderForeLeg3.sync(data.spiderForeLeg3);
        this.spiderForeLeg4.sync(data.spiderForeLeg4);
        this.spiderForeLeg5.sync(data.spiderForeLeg5);
        this.spiderForeLeg6.sync(data.spiderForeLeg6);
        this.spiderForeLeg7.sync(data.spiderForeLeg7);
        this.spiderForeLeg8.sync(data.spiderForeLeg8);
        this.renderOffset.set(data.renderOffset);
        this.renderRotation.set(data.renderRotation);
        if (Data_Spider.get(argEntity.getEntityId()).canBeUpdated()) {
            ((ModelRendererBends)this.spiderHead).resetScale();
            ((ModelRendererBends)this.spiderNeck).resetScale();
            ((ModelRendererBends)this.spiderBody).resetScale();
            ((ModelRendererBends)this.spiderLeg1).resetScale();
            ((ModelRendererBends)this.spiderLeg2).resetScale();
            ((ModelRendererBends)this.spiderLeg3).resetScale();
            ((ModelRendererBends)this.spiderLeg4).resetScale();
            ((ModelRendererBends)this.spiderLeg5).resetScale();
            ((ModelRendererBends)this.spiderLeg6).resetScale();
            ((ModelRendererBends)this.spiderLeg7).resetScale();
            ((ModelRendererBends)this.spiderLeg8).resetScale();
            this.spiderForeLeg1.resetScale();
            this.spiderForeLeg2.resetScale();
            this.spiderForeLeg3.resetScale();
            this.spiderForeLeg4.resetScale();
            this.spiderForeLeg5.resetScale();
            this.spiderForeLeg6.resetScale();
            this.spiderForeLeg7.resetScale();
            this.spiderForeLeg8.resetScale();
            this.limbSwing = p_78087_1_;
            this.limbSwingAmount = p_78087_2_;
            this.renderOffset.setSmooth(new Vector3f(0.0f, 0.0f, 0.0f), 0.5f);
            this.renderRotation.setSmooth(new Vector3f(0.0f, 0.0f, 0.0f), 0.5f);
            // spider/walk.animation.json: the legs' stride, timed by the limb swing, and its swing layer by how
            // strongly they swing. Without it the spider isn't posed (the animator logs the missing file).
            BendsAnimator.playStride(this, data, WALK, "walk", "spider");
            ((ModelRendererBends)this.spiderHead).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderNeck).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderBody).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg1).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg2).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg3).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg4).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg5).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg6).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg7).update(data.ticksPerFrame);
            ((ModelRendererBends)this.spiderLeg8).update(data.ticksPerFrame);
            this.spiderForeLeg1.update(data.ticksPerFrame);
            this.spiderForeLeg2.update(data.ticksPerFrame);
            this.spiderForeLeg3.update(data.ticksPerFrame);
            this.spiderForeLeg4.update(data.ticksPerFrame);
            this.spiderForeLeg5.update(data.ticksPerFrame);
            this.spiderForeLeg6.update(data.ticksPerFrame);
            this.spiderForeLeg7.update(data.ticksPerFrame);
            this.spiderForeLeg8.update(data.ticksPerFrame);
            this.renderOffset.update(data.ticksPerFrame);
            this.renderRotation.update(data.ticksPerFrame);
            this.renderRotation.wrapTurns(360.0f);
            data.updatedThisFrame = true;
        }
        Data_Spider.get(argEntity.getEntityId()).syncModelInfo(this);
    }
}

