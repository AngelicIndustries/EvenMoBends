package net.gobbob.mobends.client.model.entity;

import net.gobbob.mobends.client.animation.BipedRigModel;
import net.gobbob.mobends.client.animation.BendsGeometry;
import net.gobbob.mobends.compat.WawelAuthCompat;

import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.animation.player.Animation_AquaAcrobatics;
import net.minecraft.item.ItemStack;
import net.gobbob.mobends.compat.BackhandCompat;
import net.gobbob.mobends.client.animation.HandAction;
import net.gobbob.mobends.animation.player.Animation_Bow;
import net.gobbob.mobends.animation.player.Animation_Eating;
import net.gobbob.mobends.animation.player.Animation_Attack;
import net.gobbob.mobends.animation.player.Animation_Blocking;
import net.gobbob.mobends.animation.player.Animation_Rowing;
import net.gobbob.mobends.animation.player.Animation_Swimming;
import net.gobbob.mobends.client.animation.AnimationPreview;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.model.ModelBoxBends;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.ModelRendererBends_SeperatedChild;
import net.gobbob.mobends.client.renderer.SwordTrail;
import net.gobbob.mobends.compat.AquaAcrobaticsCompat;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat.BoatState;
import net.gobbob.mobends.compat.PlayerBlockingCompat;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemPickaxe;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.renderer.base.GeoVertexConsumer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector3f;

public class ModelBendsPlayer
extends ModelBiped implements BipedRigModel {
    private static final int UPPER_SEGMENT_HEIGHT = 6;

    public ModelRenderer bipedRightForeArm;
    public ModelRenderer bipedLeftForeArm;
    public ModelRenderer bipedRightForeLeg;
    public ModelRenderer bipedLeftForeLeg;
    public SmoothVector3f renderOffset = new SmoothVector3f();
    public SmoothVector3f renderRotation = new SmoothVector3f();
    public SmoothVector3f renderItemRotation = new SmoothVector3f();
    /** Held-item transforms from the rig's item bones: in the hand, and free of it. */
    public final ItemBone rightItem = new ItemBone(false);
    public final ItemBone rightItemFree = new ItemBone(true);
    public final ItemBone leftItem = new ItemBone(false);
    public final ItemBone leftItemFree = new ItemBone(true);
    public SwordTrail swordTrail = new SwordTrail();
    /** Backhand's off-hand sword's trail. */
    public SwordTrail offhandTrail = new SwordTrail();
    public float headRotationX;
    public float headRotationY;
    public float armSwing;
    public float armSwingAmount;
    private final float modelScale;
    private final float modelYOffset;
    private float partialTicks;
    private BendsGeometry geometry;
    private String geometryName;

    public String geometryName() {
        if (this.modelScale == 1) return "player_armor_outer";
        if (this.modelScale == 0.5f) return "player_armor_inner";
        return this.modern ? (this.slim ? "player_slim" : "player") : "player_legacy";
    }

    public BendsGeometry geometry() {
        String name = geometryName();
        if (!name.equals(this.geometryName)) {
            this.geometryName = name;
            this.geometry = new BendsGeometry(this, name);
        }
        return this.geometry;
    }

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
    public boolean holdsItems() {
        return true;
    }

    @Override
    public ItemBone rigItem(String bone) {
        switch (bone) {
            case "rightItem": return this.rightItem;
            case "rightItemFree": return this.rightItemFree;
            case "leftItem": return this.leftItem;
            case "leftItemFree": return this.leftItemFree;
            default: return null;
        }
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

    /** The fraction of a tick this frame is past the last tick. */
    public float partialTicks() {
        return this.partialTicks;
    }

    private boolean modern;
    private boolean slim;
    /** The arms this model was built with. Ears puts its own arms into any player renderer's model each draw. */
    private ModelRenderer ownRightArm;
    private ModelRenderer ownLeftArm;

    private ModelRendererBends bodyWear;
    private ModelRendererBends rightLegWear;
    private ModelRendererBends rightForeLegWear;
    private ModelRendererBends leftLegWear;
    private ModelRendererBends leftForeLegWear;

    private ModelRendererBends classicRightArm;
    private ModelRendererBends classicRightForeArm;
    private ModelRendererBends classicLeftArm;
    private ModelRendererBends classicLeftForeArm;
    private ModelRendererBends slimRightArm;
    private ModelRendererBends slimRightForeArm;
    private ModelRendererBends slimLeftArm;
    private ModelRendererBends slimLeftForeArm;

    private ModelRendererBends classicRightArmWear;
    private ModelRendererBends classicRightForeArmWear;
    private ModelRendererBends classicLeftArmWear;
    private ModelRendererBends classicLeftForeArmWear;
    private ModelRendererBends slimRightArmWear;
    private ModelRendererBends slimRightForeArmWear;
    private ModelRendererBends slimLeftArmWear;
    private ModelRendererBends slimLeftForeArmWear;

    public ModelBendsPlayer() {
        this(0.0f);
    }

    public ModelBendsPlayer(float p_i1148_1_) {
        this(p_i1148_1_, 0.0f, 64, 32);
    }

    public ModelBendsPlayer(float p_i1149_1_, float p_i1149_2_, int p_i1149_3_, int p_i1149_4_) {
        this.modelScale = p_i1149_1_;
        this.modelYOffset = p_i1149_2_;
        this.textureWidth = p_i1149_3_;
        this.textureHeight = p_i1149_4_;
        this.bipedCloak = new ModelRendererBends(this, 0, 0);
        this.bipedCloak.addBox(-5.0f, 0.0f, -1.0f, 10, 16, 1, p_i1149_1_);
        this.bipedEars = new ModelRendererBends(this, 24, 0);
        this.bipedEars.addBox(-3.0f, -6.0f, -1.0f, 6, 6, 1, p_i1149_1_);
        this.bipedHead = new ModelRendererBends(this, 0, 0);
        this.bipedHead.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, p_i1149_1_);
        this.bipedHead.setRotationPoint(0.0f, 0.0f + p_i1149_2_ - 12.0f, 0.0f);
        this.bipedHeadwear = new ModelRendererBends(this, 32, 0);
        this.bipedHeadwear.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, p_i1149_1_ + 0.5f);
        this.bipedHeadwear.setRotationPoint(0.0f, 0.0f, 0.0f);
        this.bipedBody = new ModelRendererBends(this, 16, 16).setShowChildIfHidden(true);
        this.bipedBody.addBox(-4.0f, -12.0f, -2.0f, 8, 12, 4, p_i1149_1_);
        this.bipedBody.setRotationPoint(0.0f, 0.0f + p_i1149_2_ + 12.0f, 0.0f);
        this.bipedRightArm = new ModelRendererBends_SeperatedChild(this, 40, 16).setMother((ModelRendererBends)this.bipedBody);
        this.bipedRightArm.addBox(-3.0f, -2.0f, -2.0f, 4, 6, 4, p_i1149_1_);
        this.bipedRightArm.setRotationPoint(-5.0f, 2.0f + p_i1149_2_ - 12.0f, 0.0f);
        this.bipedLeftArm = new ModelRendererBends_SeperatedChild(this, 40, 16).setMother((ModelRendererBends)this.bipedBody);
        this.bipedLeftArm.mirror = true;
        this.bipedLeftArm.addBox(-1.0f, -2.0f, -2.0f, 4, 6, 4, p_i1149_1_);
        this.bipedLeftArm.setRotationPoint(5.0f, 2.0f + p_i1149_2_ - 12.0f, 0.0f);
        this.ownRightArm = this.bipedRightArm;
        this.ownLeftArm = this.bipedLeftArm;
        this.bipedRightLeg = new ModelRendererBends(this, 0, 16);
        this.bipedRightLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, p_i1149_1_);
        this.bipedRightLeg.setRotationPoint(-1.9f, 12.0f + p_i1149_2_, 0.0f);
        this.bipedLeftLeg = new ModelRendererBends(this, 0, 16);
        this.bipedLeftLeg.mirror = true;
        this.bipedLeftLeg.addBox(-2.0f, 0.0f, -2.0f, 4, 6, 4, p_i1149_1_);
        this.bipedLeftLeg.setRotationPoint(1.9f, 12.0f + p_i1149_2_, 0.0f);
        this.bipedRightForeArm = new ModelRendererBends(this, 40, 22);
        this.bipedRightForeArm.addBox(0.0f, 0.0f, -4.0f, 4, 6, 4, p_i1149_1_);
        this.bipedRightForeArm.setRotationPoint(-3.0f, 4.0f, 2.0f);
        ((ModelRendererBends)this.bipedRightForeArm).getBox().offsetTextureQuad(this.bipedRightForeArm, 3, 0.0f, -6.0f);
        ((ModelRendererBends)this.bipedRightForeArm).getBox().hideQuad(ModelBoxBends.TOP);
        this.bipedLeftForeArm = new ModelRendererBends(this, 40, 22);
        this.bipedLeftForeArm.mirror = true;
        this.bipedLeftForeArm.addBox(0.0f, 0.0f, -4.0f, 4, 6, 4, p_i1149_1_);
        this.bipedLeftForeArm.setRotationPoint(-1.0f, 4.0f, 2.0f);
        ((ModelRendererBends)this.bipedLeftForeArm).getBox().offsetTextureQuad(this.bipedRightForeArm, 3, 0.0f, -6.0f);
        ((ModelRendererBends)this.bipedLeftForeArm).getBox().hideQuad(ModelBoxBends.TOP);
        this.bipedRightForeLeg = new ModelRendererBends(this, 0, 22);
        this.bipedRightForeLeg.addBox(-2.0f, 0.0f, 0.0f, 4, 6, 4, p_i1149_1_);
        this.bipedRightForeLeg.setRotationPoint(0.0f, 6.0f, -2.0f);
        ((ModelRendererBends)this.bipedRightForeLeg).getBox().offsetTextureQuad(this.bipedRightForeLeg, 3, 0.0f, -6.0f);
        ((ModelRendererBends)this.bipedRightForeLeg).getBox().hideQuad(ModelBoxBends.TOP);
        this.bipedLeftForeLeg = new ModelRendererBends(this, 0, 22);
        this.bipedLeftForeLeg.mirror = true;
        this.bipedLeftForeLeg.addBox(-2.0f, 0.0f, 0.0f, 4, 6, 4, p_i1149_1_);
        this.bipedLeftForeLeg.setRotationPoint(0.0f, 6.0f, -2.0f);
        ((ModelRendererBends)this.bipedLeftForeLeg).getBox().offsetTextureQuad(this.bipedLeftForeLeg, 3, 0.0f, -6.0f);
        ((ModelRendererBends)this.bipedLeftForeLeg).getBox().hideQuad(ModelBoxBends.TOP);
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
        // Expand the existing vertices: rebuilding from uninflated box dimensions
        // discards armor thickness and lets modern skin overlays protrude through it.
        separateUpperJoint((ModelRendererBends)this.bipedRightArm);
        separateUpperJoint((ModelRendererBends)this.bipedLeftArm);
        separateUpperJoint((ModelRendererBends)this.bipedRightLeg);
        separateUpperJoint((ModelRendererBends)this.bipedLeftLeg);
    }

    @Override
    public void render(Entity argEntity, float p_78088_2_, float p_78088_3_, float p_78088_4_, float p_78088_5_, float p_78088_6_, float p_78088_7_) {
        this.setRotationAngles(p_78088_2_, p_78088_3_, p_78088_4_, p_78088_5_, p_78088_6_, p_78088_7_, argEntity);
        if (argEntity instanceof EntityPlayer) {
            Data_Player data = Data_Player.get(argEntity.getEntityId());
            if (data != null && data.swimming) this.measureSwimReach(data);
        }
        this.prepareModernOverlays();
        if (this.modern && WawelAuthCompat.isLoaded()) {
            // Wawel Auth draws the overlay layers, flat or as voxels, on this model's joints.
            WawelAuthCompat.renderWithLayers(this, argEntity, p_78088_7_);
            return;
        }
        this.renderGeometry(p_78088_7_);
    }

    /** For SwimRoom on the next frame: how far this player's pose reaches, and while lying in the stroke. */
    private void measureSwimReach(Data_Player data) {
        float[] reach = new float[2];
        this.reach(null, reach);
        data.reachFront = reach[0];
        data.reachBack = reach[1];
        if (Math.abs(this.renderRotation.getX()) > 45.0f) {
            data.strokeFront = reach[0];
            data.strokeBack = reach[1];
        }
    }

    /**
     * How far this model as posed reaches ahead of ({@code out[0]}) and behind ({@code out[1]}) the player's position,
     * in blocks along the facing, drawn as RenderBendsPlayer draws it. {@code baked} null for the game's geometry.
     */
    public void reach(BakedGeoModel baked, float[] out) {
        float deg = (float)(Math.PI / 180.0);
        // Salamander's model blocks to the frame rotateCorpse leaves, facing -Z: postRender's offset and turn,
        // RendererLivingEntity's flip, RenderPlayer's scale and the drop to the feet, then the geometry's own frame.
        Matrix4f frame = new Matrix4f()
            .translate(this.renderOffset.vSmooth.x * 0.0625f, this.renderOffset.vSmooth.y * 0.0625f, this.renderOffset.vSmooth.z * 0.0625f)
            .rotateX(-this.renderRotation.getX() * deg).rotateY(-this.renderRotation.getY() * deg).rotateZ(this.renderRotation.getZ() * deg)
            .scale(-0.9375f, -0.9375f, 0.9375f).translate(0.0f, -1.5078125f, 0.0f)
            .translate(0.0f, 1.5f, 0.0f).scale(-1.0f, -1.0f, 1.0f);
        out[0] = 0.0f;
        out[1] = 0.0f;
        GeoVertexConsumer along = (x, y, z, u, v, nx, ny, nz, r, g, b, a) -> {
            float ahead = -(frame.m02() * x + frame.m12() * y + frame.m22() * z + frame.m32());
            out[0] = Math.max(out[0], ahead);
            out[1] = Math.max(out[1], -ahead);
        };
        if (baked == null) this.geometry().vertices(along);
        else this.geometry().vertices(baked, along);
    }

    /** The rigged model including its own overlay boxes, grouped like ModelBiped for children. */
    public void renderGeometry(float scale) {
        if (this.isChild) {
            float f6 = 2.0f;
            GL11.glPushMatrix();
            GL11.glScalef((float)(1.5f / f6), (float)(1.5f / f6), (float)(1.5f / f6));
            GL11.glTranslatef((float)0.0f, (float)(16.0f * scale), (float)0.0f);
            this.geometry().render(scale, name -> this.geometry().rig.descendant(name, "head"));
            GL11.glPopMatrix();
            GL11.glPushMatrix();
            GL11.glScalef((float)(1.0f / f6), (float)(1.0f / f6), (float)(1.0f / f6));
            GL11.glTranslatef((float)0.0f, (float)(24.0f * scale), (float)0.0f);
            this.geometry().render(scale, name -> !this.geometry().rig.descendant(name, "head"));
            GL11.glPopMatrix();
        } else {
            this.geometry().render(scale);
        }
    }

    /** Which overlay boxes a draw shows. */
    public static final class WearState {
        public final boolean hat;
        public final boolean jacket;
        public final boolean rightSleeve;
        public final boolean leftSleeve;
        public final boolean rightPants;
        public final boolean leftPants;

        WearState(boolean hat, boolean jacket, boolean rightSleeve, boolean leftSleeve, boolean rightPants,
            boolean leftPants) {
            this.hat = hat;
            this.jacket = jacket;
            this.rightSleeve = rightSleeve;
            this.leftSleeve = leftSleeve;
            this.rightPants = rightPants;
            this.leftPants = leftPants;
        }
    }

    /** Hides the overlay boxes for a draw whose layers another renderer draws; returns which were shown. */
    public WearState hideWear() {
        WearState shown = new WearState(
            this.bipedHeadwear.showModel,
            this.modern && this.bodyWear.showModel,
            this.modern && this.getRightArmWear().showModel,
            this.modern && this.getLeftArmWear().showModel,
            this.modern && this.rightLegWear.showModel,
            this.modern && this.leftLegWear.showModel);
        this.bipedHeadwear.showModel = false;
        if (this.modern) {
            this.bodyWear.showModel = false;
            this.setRightSleeveVisible(false);
            this.setLeftSleeveVisible(false);
            this.setRightPantsVisible(false);
            this.setLeftPantsVisible(false);
        }
        return shown;
    }

    public void restoreWear(WearState shown) {
        this.bipedHeadwear.showModel = shown.hat;
        if (this.modern) {
            this.bodyWear.showModel = shown.jacket;
            this.setRightSleeveVisible(shown.rightSleeve);
            this.setLeftSleeveVisible(shown.leftSleeve);
            this.setRightPantsVisible(shown.rightPants);
            this.setLeftPantsVisible(shown.leftPants);
        }
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float swing, float amount, float partialTicks) {
        super.setLivingAnimations(entity, swing, amount, partialTicks);
        this.partialTicks = partialTicks;
    }

    public float getPartialTicks() {
        return this.partialTicks;
    }

    @Override
    public void setRotationAngles(float argSwingTime, float argSwingAmount, float argArmSway, float argHeadY, float argHeadX, float argNr6, Entity argEntity) {
        this.bipedRightArm = this.ownRightArm;
        this.bipedLeftArm = this.ownLeftArm;
        if (Minecraft.getMinecraft().theWorld == null) {
            return;
        }
        if (Minecraft.getMinecraft().theWorld.isRemote && Minecraft.getMinecraft().isGamePaused()) {
            return;
        }
        Data_Player data = Data_Player.get(argEntity.getEntityId());
        this.armSwing = argSwingTime;
        this.armSwingAmount = argSwingAmount;
        this.headRotationX = argHeadX;
        // 1.7.10 passes head minus body yaw unwrapped, whole turns apart; blends would spin through them.
        this.headRotationY = MathHelper.wrapAngleTo180_float(argHeadY);
        // Inventory and menu previews face forward. GUI drawing happens outside the world's render passes.
        if (MinecraftForgeClient.getRenderPass() == -1) {
            this.headRotationY = 0.0f;
        }
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
        this.renderItemRotation.set(data.renderItemRotation);
        this.setItemBones(data);
        this.swordTrail = data.swordTrail;
        this.offhandTrail = data.offhandTrail;
        if (Data_Player.get(argEntity.getEntityId()).canBeUpdated()) {
            this.renderOffset.setSmooth(new Vector3f(0.0f, -1.0f, 0.0f), 0.5f);
            this.renderRotation.setSmooth(new Vector3f(0.0f, 0.0f, 0.0f), 0.5f);
            this.renderItemRotation.setSmooth(new Vector3f(0.0f, 0.0f, 0.0f), 0.5f);
            this.rightItem.resetScale();
            this.rightItemFree.resetScale();
            this.leftItem.resetScale();
            this.leftItemFree.resetScale();
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
            EntityPlayer player = (EntityPlayer)argEntity;
            AquaAcrobaticsCompat.State aqua = AquaAcrobaticsCompat.getState(player, this.partialTicks);
            BoatState boat = Animation_Rowing.getBoatState(player, this.partialTicks);
            boolean rowing = false;
            boolean elytraFlying = EtFuturumRequiemCompat.isElytraFlying(player);
            boolean creativeFlying = player.capabilities.isFlying && !data.isOnGround();
            boolean touchingWater = this.isTouchingWater(argEntity);
            boolean groundedInWater = touchingWater && data.isOnGround();
            data.swimming = false;
            if (boat != null) {
                rowing = this.animatePlayer("rowing", argEntity, data);
                if (!rowing) this.animatePlayer("riding", argEntity, data);
            } else if ((elytraFlying || creativeFlying) && this.animatePlayer("flying", argEntity, data)) {
                // Flight takes precedence over the ordinary airborne animations.
            } else if (argEntity.isRiding()) {
                this.animatePlayer("riding", argEntity, data);
            } else if (aqua.isActive()) {
                // AA owns the pose, including crawling on dry land and swimming along the bottom.
                Animation_AquaAcrobatics.apply(this, aqua.blend, aqua.inWater, argSwingTime, argSwingAmount,
                    PlayerAnimationConfig.isEnabled(aqua.inWater ? "swimming" : "crawling"));
            } else if (touchingWater && !groundedInWater) {
                if (aqua.available) {
                    // With AA, being in water alone does not mean the player is swimming horizontally.
                    if (PlayerAnimationConfig.isEnabled("swimming")) Animation_Swimming.play(this, data, false);
                } else {
                    data.swimming = true;
                    if (data.swimBoxedIn) {
                        // No room to lie in the stroke here (see SwimRoom): tread water upright.
                        if (PlayerAnimationConfig.isEnabled("swimming")) Animation_Swimming.play(this, data, false);
                    } else if (!this.shouldAnimateDiving(argEntity, data) || !this.animatePlayer("diving", argEntity, data)) {
                        this.animatePlayer("swimming", argEntity, data);
                    }
                }
            } else if (player.isOnLadder() && this.animatePlayer("climbing", argEntity, data)) {
                // Climbing takes precedence over the ordinary airborne animations.
            } else if (!data.isOnGround() || !groundedInWater && data.ticksAfterTouchdown < 2.0f) {
                if (!this.shouldAnimateFalling(argEntity, data) || !this.animatePlayer("falling", argEntity, data)) {
                    this.animatePlayer("jump", argEntity, data);
                }
            } else {
                if (Data_Player.get((int)argEntity.getEntityId()).motion.x == 0.0f & Data_Player.get((int)argEntity.getEntityId()).motion.z == 0.0f) {
                    this.animatePlayer("stand", argEntity, data);
                } else if (argEntity.isSprinting()) {
                    if (!this.animatePlayer("sprint", argEntity, data)) {
                        this.animatePlayer("walk", argEntity, data);
                    }
                } else {
                    this.animatePlayer("walk", argEntity, data);
                }
                if (argEntity.isSneaking()) {
                    this.animatePlayer("sneak", argEntity, data);
                }
            }
            int blockingHands = PlayerBlockingCompat.getBlockingHands(player, this);
            int eatingHand = Animation_Eating.hand(player);
            if (rowing && boat.driver) {
                // Both hands are occupied by paddles, even while coasting or holding a tool.
            } else if (eatingHand == 1 || (blockingHands & PlayerBlockingCompat.MAIN_HAND) != 0) {
                // Do not let a recent sword swing/stance compete with main-hand blocking.
            } else if (aqua.isActive() && !player.isSwingInProgress && player.getItemInUseCount() == 0) {
                // Keep weapon recovery stances from replacing the swimming/crawling arm cycle.
            } else if (BackhandCompat.isDrawingOffhandBow(player)) {
                // Backhand draws a bow in the off hand: the bow arm is the left one.
                if (PlayerAnimationConfig.isEnabled("bow")) Animation_Bow.play(this, data, player.getItemInUseDuration() + this.partialTicks, true);
            } else if (this.aimedBow) {
                this.animatePlayer("bow", argEntity, data);
            } else {
                this.animatePlayer(mainHandAnimation(player, data), argEntity, data);
            }
            if (!(rowing && boat.driver) && eatingHand != 2 && (blockingHands & PlayerBlockingCompat.OFF_HAND) == 0) {
                Animation_Attack.playOffhand(player, this, data);
            }
            if (!(rowing && boat.driver)) {
                Animation_Blocking.play(this, data, blockingHands);
                if (eatingHand != 0) Animation_Eating.play(this, data, player.getItemInUseDuration() + this.partialTicks, eatingHand == 2);
            }
            AnimationPreview.apply(this, data, argEntity);
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
            this.renderItemRotation.update(data.ticksPerFrame);
            this.rightItem.update(data.ticksPerFrame);
            this.rightItemFree.update(data.ticksPerFrame);
            this.leftItem.update(data.ticksPerFrame);
            this.leftItemFree.update(data.ticksPerFrame);
            this.swordTrail.update(data.ticksPerFrame);
            this.offhandTrail.update(data.ticksPerFrame);
            data.updatedThisFrame = true;
        }
        Data_Player.get(argEntity.getEntityId()).syncModelInfo(this);
    }

    private boolean animatePlayer(String animationName, Entity entity, Data_Player data) {
        if (!PlayerAnimationConfig.isEnabled(animationName)) {
            return false;
        }
        Animation animation = AnimatedEntity.getByEntity(entity).get(animationName);
        if (animation == null) {
            return false;
        }
        animation.animate((EntityLivingBase)entity, this, data);
        return true;
    }

    /**
     * What the main hand's item does: a pickaxe mines; a block mines while breaking or placing (other players' swings
     * with a block count as placing, as we can't tell) and otherwise attacks; an axe chops while breaking and in the
     * stance after (the last swing broke a block), and otherwise swings like a sword; anything else attacks.
     */
    private static String mainHandAnimation(EntityPlayer player, Data_Player data) {
        ItemStack held = player.getCurrentEquippedItem();
        if (held == null) return "attack";
        boolean breaking = HandAction.isBreakingBlock(player);
        if (held.getItem() instanceof ItemPickaxe) return "mining";
        if (Block.getBlockFromItem(held.getItem()) != Blocks.air) {
            boolean attacking = data.swingKnown && !data.swingIsUse && !data.swingBreaks && data.ticksAfterPunch < 60.0f;
            return breaking || !attacking ? "mining" : "attack";
        }
        if (held.getItem() instanceof ItemAxe) {
            return breaking || data.swingBreaks && data.ticksAfterPunch < 60.0f ? "axe" : "attack";
        }
        return "attack";
    }

    private boolean shouldAnimateFalling(Entity entity, Data_Player data) {
        return !data.isOnGround()
            && data.ticksAfterLiftoff > 3.0f
            && data.motion.y < -0.08f
            && entity.fallDistance >= 3.0f;
    }

    private boolean shouldAnimateDiving(Entity entity, Data_Player data) {
        double horizontalSpeedSquared = data.motion.x * data.motion.x + data.motion.z * data.motion.z;
        return entity.isInsideOfMaterial(Material.water)
            && (horizontalSpeedSquared > 0.0004 || Math.abs(data.motion.y) > 0.02);
    }

    private boolean isTouchingWater(Entity entity) {
        return entity.isInWater()
            || entity.worldObj.isMaterialInBB(entity.boundingBox.contract(0.001, 0.001, 0.001), Material.water);
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
        GL11.glRotatef((float)this.renderItemRotation.vSmooth.x, (float)1.0f, (float)0.0f, (float)0.0f);
        GL11.glRotatef((float)this.renderItemRotation.vSmooth.y, (float)0.0f, (float)-1.0f, (float)0.0f);
        GL11.glRotatef((float)this.renderItemRotation.vSmooth.z, (float)0.0f, (float)0.0f, (float)1.0f);
    }

    private void setItemBones(Data_Player data) {
        this.rightItem.set(data.rightItem);
        this.rightItemFree.set(data.rightItemFree);
        this.leftItem.set(data.leftItem);
        this.leftItemFree.set(data.leftItemFree);
    }

    public void updateWithEntityData(AbstractClientPlayer argPlayer) {
        Data_Player data = Data_Player.get(argPlayer.getEntityId());
        if (data != null) {
            this.renderOffset.set(data.renderOffset);
            this.renderRotation.set(data.renderRotation);
            this.renderItemRotation.set(data.renderItemRotation);
            this.setItemBones(data);
        }
    }

    /**
     * Overrides SimpleSkinBackport's duck method once its ModelBiped mixin is applied.
     * Its generic box conversion loses our segmented geometry, UVs and hidden joint faces.
     * Keep these signatures free of optional API types so the model also loads without SSB.
     */
    public void ssb$set64x() {
        this.initModern();
    }

    public void ssb$setSlim(boolean slim) {
        // Models recreated by the settings GUI miss SSB's startup-only texture conversion.
        this.initModern();
        this.setSlim(slim);
    }

    /**
     * Overrides WawelAuth's interface method after its ModelBiped mixin is applied.
     * The segmented Mo' Bends hierarchy is retained instead of being replaced with vanilla limbs.
     */
    public void initModern() {
        if (this.modern) {
            return;
        }

        this.textureWidth = 64;
        this.textureHeight = 64;

        replaceBox((ModelRendererBends)this.bipedHead, 0, 0, false, -4.0f, -8.0f, -4.0f, 8, 8, 8, this.modelScale);
        replaceBox((ModelRendererBends)this.bipedHeadwear, 32, 0, false, -4.0f, -8.0f, -4.0f, 8, 8, 8, this.modelScale + 0.5f);
        replaceBox((ModelRendererBends)this.bipedBody, 16, 16, false, -4.0f, -12.0f, -2.0f, 8, 12, 4, this.modelScale);
        replaceBox((ModelRendererBends)this.bipedRightLeg, 0, 16, false, -2.0f, 0.0f, -2.0f, 4, 6, 4, this.modelScale);
        replaceBox((ModelRendererBends)this.bipedLeftLeg, 16, 48, false, -2.0f, 0.0f, -2.0f, 4, 6, 4, this.modelScale);
        replaceLowerBox((ModelRendererBends)this.bipedRightForeLeg, 0, 16, false, -2.0f, 0.0f, 0.0f, 4, 6, 4, this.modelScale);
        replaceLowerBox((ModelRendererBends)this.bipedLeftForeLeg, 16, 48, false, -2.0f, 0.0f, 0.0f, 4, 6, 4, this.modelScale);

        clearBox((ModelRendererBends)this.bipedRightArm);
        clearBox((ModelRendererBends)this.bipedRightForeArm);
        clearBox((ModelRendererBends)this.bipedLeftArm);
        clearBox((ModelRendererBends)this.bipedLeftForeArm);

        this.classicRightArm = createBox(40, 16, false, -3.0f, -2.0f, -2.0f, 4, 6, 4, this.modelScale, false);
        this.classicRightForeArm = createBox(40, 16, false, 0.0f, 0.0f, -4.0f, 4, 6, 4, this.modelScale, true);
        this.classicLeftArm = createBox(32, 48, false, -1.0f, -2.0f, -2.0f, 4, 6, 4, this.modelScale, false);
        this.classicLeftForeArm = createBox(32, 48, false, 0.0f, 0.0f, -4.0f, 4, 6, 4, this.modelScale, true);
        this.slimRightArm = createBox(40, 16, false, -2.0f, -2.0f, -2.0f, 3, 6, 4, this.modelScale, false);
        this.slimRightForeArm = createBox(40, 16, false, 0.0f, 0.0f, -4.0f, 3, 6, 4, this.modelScale, true);
        this.slimLeftArm = createBox(32, 48, false, -1.0f, -2.0f, -2.0f, 3, 6, 4, this.modelScale, false);
        this.slimLeftForeArm = createBox(32, 48, false, 0.0f, 0.0f, -4.0f, 3, 6, 4, this.modelScale, true);

        separateUpperJoint(this.classicRightArm);
        separateUpperJoint(this.classicLeftArm);
        separateUpperJoint(this.slimRightArm);
        separateUpperJoint(this.slimLeftArm);
        separateUpperJoint((ModelRendererBends)this.bipedRightLeg);
        separateUpperJoint((ModelRendererBends)this.bipedLeftLeg);

        float overlay = this.modelScale + 0.25f;
        this.bodyWear = createBox(16, 32, false, -4.0f, -12.0f, -2.0f, 8, 12, 4, overlay, false);
        this.rightLegWear = createBox(0, 32, false, -2.0f, 0.0f, -2.0f, 4, 6, 4, overlay, false);
        this.rightForeLegWear = createBox(0, 32, false, -2.0f, 0.0f, 0.0f, 4, 6, 4, overlay, true);
        this.leftLegWear = createBox(0, 48, false, -2.0f, 0.0f, -2.0f, 4, 6, 4, overlay, false);
        this.leftForeLegWear = createBox(0, 48, false, -2.0f, 0.0f, 0.0f, 4, 6, 4, overlay, true);

        this.classicRightArmWear = createBox(40, 32, false, -3.0f, -2.0f, -2.0f, 4, 6, 4, overlay, false);
        this.classicRightForeArmWear = createBox(40, 32, false, 0.0f, 0.0f, -4.0f, 4, 6, 4, overlay, true);
        this.classicLeftArmWear = createBox(48, 48, false, -1.0f, -2.0f, -2.0f, 4, 6, 4, overlay, false);
        this.classicLeftForeArmWear = createBox(48, 48, false, 0.0f, 0.0f, -4.0f, 4, 6, 4, overlay, true);
        this.slimRightArmWear = createBox(40, 32, false, -2.0f, -2.0f, -2.0f, 3, 6, 4, overlay, false);
        this.slimRightForeArmWear = createBox(40, 32, false, 0.0f, 0.0f, -4.0f, 3, 6, 4, overlay, true);
        this.slimLeftArmWear = createBox(48, 48, false, -1.0f, -2.0f, -2.0f, 3, 6, 4, overlay, false);
        this.slimLeftForeArmWear = createBox(48, 48, false, 0.0f, 0.0f, -4.0f, 3, 6, 4, overlay, true);

        separateUpperJoint(this.classicRightArmWear);
        separateUpperJoint(this.classicLeftArmWear);
        separateUpperJoint(this.slimRightArmWear);
        separateUpperJoint(this.slimLeftArmWear);
        separateUpperJoint(this.rightLegWear);
        separateUpperJoint(this.leftLegWear);

        this.bipedBody.addChild(this.bodyWear);
        this.bipedRightLeg.addChild(this.rightLegWear);
        this.bipedRightForeLeg.addChild(this.rightForeLegWear);
        this.bipedLeftLeg.addChild(this.leftLegWear);
        this.bipedLeftForeLeg.addChild(this.leftForeLegWear);

        addArmChildren((ModelRendererBends)this.bipedRightArm, this.classicRightArm, this.slimRightArm, this.classicRightArmWear, this.slimRightArmWear);
        addArmChildren((ModelRendererBends)this.bipedRightForeArm, this.classicRightForeArm, this.slimRightForeArm, this.classicRightForeArmWear, this.slimRightForeArmWear);
        addArmChildren((ModelRendererBends)this.bipedLeftArm, this.classicLeftArm, this.slimLeftArm, this.classicLeftArmWear, this.slimLeftArmWear);
        addArmChildren((ModelRendererBends)this.bipedLeftForeArm, this.classicLeftForeArm, this.slimLeftForeArm, this.classicLeftForeArmWear, this.slimLeftForeArmWear);

        this.modern = true;
        this.setSlim(false);
    }

    public void setSlim(boolean slim) {
        if (!this.modern) {
            return;
        }

        this.slim = slim;
        this.classicRightArm.showModel = !slim;
        this.classicRightForeArm.showModel = !slim;
        this.classicLeftArm.showModel = !slim;
        this.classicLeftForeArm.showModel = !slim;
        this.slimRightArm.showModel = slim;
        this.slimRightForeArm.showModel = slim;
        this.slimLeftArm.showModel = slim;
        this.slimLeftForeArm.showModel = slim;

        this.classicRightArmWear.showModel = !slim;
        this.classicLeftArmWear.showModel = !slim;
        this.slimRightArmWear.showModel = slim;
        this.slimLeftArmWear.showModel = slim;

        float armY = 2.0f + this.modelYOffset - 12.0f + (slim ? 0.5f : 0.0f);
        this.bipedRightArm.rotationPointY = armY;
        this.bipedLeftArm.rotationPointY = armY;
        this.bipedRightForeArm.rotationPointX = slim ? -2.0f : -3.0f;
    }

    public boolean isModern() {
        return this.modern;
    }

    public boolean isSlim() {
        return this.slim;
    }

    public ModelRenderer getBodyWear() {
        return this.bodyWear;
    }

    public ModelRenderer getRightArmWear() {
        return this.slim ? this.slimRightArmWear : this.classicRightArmWear;
    }

    public ModelRenderer getLeftArmWear() {
        return this.slim ? this.slimLeftArmWear : this.classicLeftArmWear;
    }

    public ModelRenderer getRightLegWear() {
        return this.rightLegWear;
    }

    public ModelRenderer getLeftLegWear() {
        return this.leftLegWear;
    }

    public void prepareModernOverlays() {
        if (!this.modern) {
            return;
        }

        this.classicRightForeArmWear.showModel = !this.slim && this.classicRightArmWear.showModel;
        this.classicLeftForeArmWear.showModel = !this.slim && this.classicLeftArmWear.showModel;
        this.slimRightForeArmWear.showModel = this.slim && this.slimRightArmWear.showModel;
        this.slimLeftForeArmWear.showModel = this.slim && this.slimLeftArmWear.showModel;
        this.rightForeLegWear.showModel = this.rightLegWear.showModel;
        this.leftForeLegWear.showModel = this.leftLegWear.showModel;
    }

    private void setRightSleeveVisible(boolean visible) {
        this.classicRightArmWear.showModel = !this.slim && visible;
        this.classicRightForeArmWear.showModel = !this.slim && visible;
        this.slimRightArmWear.showModel = this.slim && visible;
        this.slimRightForeArmWear.showModel = this.slim && visible;
    }

    private void setLeftSleeveVisible(boolean visible) {
        this.classicLeftArmWear.showModel = !this.slim && visible;
        this.classicLeftForeArmWear.showModel = !this.slim && visible;
        this.slimLeftArmWear.showModel = this.slim && visible;
        this.slimLeftForeArmWear.showModel = this.slim && visible;
    }

    private void setRightPantsVisible(boolean visible) {
        this.rightLegWear.showModel = visible;
        this.rightForeLegWear.showModel = visible;
    }

    private void setLeftPantsVisible(boolean visible) {
        this.leftLegWear.showModel = visible;
        this.leftForeLegWear.showModel = visible;
    }

    private void addArmChildren(ModelRendererBends parent, ModelRendererBends classicPart, ModelRendererBends slimPart,
        ModelRendererBends classicWear, ModelRendererBends slimWear) {
        parent.addChild(classicPart);
        parent.addChild(slimPart);
        parent.addChild(classicWear);
        parent.addChild(slimWear);
    }

    private void separateUpperJoint(ModelRendererBends renderer) {
        renderer.getBox().expandXZ(0.01f);
    }

    private ModelRendererBends createBox(int textureX, int textureY, boolean mirror, float x, float y, float z,
        int width, int height, int depth, float scale, boolean lowerSegment) {
        ModelRendererBends renderer = new ModelRendererBends(this, textureX, textureY);
        renderer.setTextureSize(64, 64);
        renderer.mirror = mirror;
        renderer.addBox(x, y, z, width, height, depth, scale);
        if (lowerSegment) {
            offsetLowerSegmentUvs(renderer);
            if (scale > 0.0f) {
                renderer.getBox().insetMinY(scale * 2.0f);
            }
        }
        return renderer;
    }

    private void replaceBox(ModelRendererBends renderer, int textureX, int textureY, boolean mirror, float x,
        float y, float z, int width, int height, int depth, float scale) {
        renderer.setTextureSize(64, 64);
        renderer.txOffsetX = textureX;
        renderer.txOffsetY = textureY;
        renderer.mirror = mirror;
        renderer.cubeList.clear();
        renderer.addBox(x, y, z, width, height, depth, scale);
        renderer.compiled = false;
    }

    private void replaceLowerBox(ModelRendererBends renderer, int textureX, int textureY, boolean mirror, float x,
        float y, float z, int width, int height, int depth, float scale) {
        replaceBox(renderer, textureX, textureY, mirror, x, y, z, width, height, depth, scale);
        offsetLowerSegmentUvs(renderer);
    }

    private void clearBox(ModelRendererBends renderer) {
        renderer.cubeList.clear();
        renderer.compiled = false;
    }

    private void offsetLowerSegmentUvs(ModelRendererBends renderer) {
        ModelBoxBends box = renderer.getBox();
        box.hideQuad(ModelBoxBends.TOP);
        box.offsetTextureQuad(renderer, ModelBoxBends.LEFT, 0.0f, UPPER_SEGMENT_HEIGHT);
        box.offsetTextureQuad(renderer, ModelBoxBends.RIGHT, 0.0f, UPPER_SEGMENT_HEIGHT);
        box.offsetTextureQuad(renderer, ModelBoxBends.FRONT, 0.0f, UPPER_SEGMENT_HEIGHT);
        box.offsetTextureQuad(renderer, ModelBoxBends.BACK, 0.0f, UPPER_SEGMENT_HEIGHT);
    }
}
