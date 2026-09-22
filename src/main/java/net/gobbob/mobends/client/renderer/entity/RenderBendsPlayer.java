package net.gobbob.mobends.client.renderer.entity;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.player.Animation_Rowing;
import net.gobbob.mobends.client.animation.PlayerRig;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.render.BlinkingTextures;
import net.gobbob.mobends.client.renderer.SwordTrail;
import net.gobbob.mobends.compat.AquaAcrobaticsCompat;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat.BoatState;
import net.gobbob.mobends.compat.SimpleSkinBackportCompat;
import net.gobbob.mobends.compat.WawelAuthCompat;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.customarmor.CustomArmor;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.settings.SettingsBoolean;
import net.gobbob.mobends.settings.SettingsNode;
import net.minecraft.block.Block;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.tileentity.TileEntitySkullRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.opengl.GL11;

public class RenderBendsPlayer
extends RenderPlayer {
    private static final float MODEL_SCALE = 0.0625f;
    public int refreshModel = 0;
    private final ModelRenderer eyebrowOverlay;
    private final ModelBiped wawelFirstPersonModel = new ModelBiped();
    private final ModelBiped ssbFirstPersonModel = new ModelBiped();

    public RenderBendsPlayer() {
        this.mainModel = WawelAuthCompat.createPlayerModel(0.0f);
        this.modelBipedMain = (ModelBendsPlayer)this.mainModel;
        this.modelArmorChestplate = new ModelBendsPlayer(1.0f);
        this.modelArmor = new ModelBendsPlayer(0.5f);
        this.eyebrowOverlay = new ModelRenderer((ModelBiped)this.mainModel, 0, 0);
        this.eyebrowOverlay.setTextureSize(64, 64);
        this.eyebrowOverlay.addBox(-4.0f, -8.0f, -4.0f, 8, 8, 8, 0.02f);
    }

    @Override
    protected ResourceLocation getEntityTexture(AbstractClientPlayer player) {
        ResourceLocation normalTexture = super.getEntityTexture(player);
        return BlinkingTextures.forPlayer(player, normalTexture);
    }

    @Override
    protected void renderModel(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float age,
        float headYaw, float headPitch, float scale) {
        super.renderModel(entity, limbSwing, limbSwingAmount, age, headYaw, headPitch, scale);
        if (!(entity instanceof AbstractClientPlayer) || entity.isInvisible()) {
            return;
        }
        AbstractClientPlayer player = (AbstractClientPlayer)entity;
        ModelBendsPlayer model = (ModelBendsPlayer)this.mainModel;
        for (int group = 1; group <= 2; ++group) {
            ResourceLocation brows = BlinkingTextures.eyebrowTexture(player, player.getLocationSkin(), group);
            if (brows == null) {
                continue;
            }
            this.bindTexture(brows);
            GL11.glPushMatrix();
            model.bipedBody.postRender(MODEL_SCALE);
            model.bipedHead.postRender(MODEL_SCALE);
            float idle = (float)Math.sin((player.ticksExisted + group * 17) / (20.0 + group * 3.0)) * 0.04f;
            GL11.glTranslatef(0.0f, idle, -0.002f);
            this.eyebrowOverlay.render(MODEL_SCALE);
            GL11.glPopMatrix();
        }
    }

    @Override
    protected int shouldRenderPass(AbstractClientPlayer p_77032_1_, int p_77032_2_, float p_77032_3_) {
        Item item;
        ItemStack itemstack = p_77032_1_.inventory.armorItemInSlot(3 - p_77032_2_);
        RenderPlayerEvent.SetArmorModel event = new RenderPlayerEvent.SetArmorModel(p_77032_1_, this, 3 - p_77032_2_, p_77032_3_, itemstack);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.result != -1) {
            return event.result;
        }
        if (itemstack != null && (item = itemstack.getItem()) instanceof ItemArmor) {
            ItemArmor itemarmor = (ItemArmor)item;
            ResourceLocation texture = RenderBiped.getArmorResource(p_77032_1_, itemstack, p_77032_2_, null);
            this.bindTexture(texture);
            ModelBiped modelbiped = p_77032_2_ == 2 ? this.modelArmor : this.modelArmorChestplate;
            modelbiped.bipedHead.showModel = p_77032_2_ == 0;
            modelbiped.bipedHeadwear.showModel = p_77032_2_ == 0;
            modelbiped.bipedBody.showModel = p_77032_2_ == 1 || p_77032_2_ == 2;
            modelbiped.bipedRightArm.showModel = p_77032_2_ == 1;
            modelbiped.bipedLeftArm.showModel = p_77032_2_ == 1;
            modelbiped.bipedRightLeg.showModel = p_77032_2_ == 2 || p_77032_2_ == 3;
            modelbiped.bipedLeftLeg.showModel = p_77032_2_ == 2 || p_77032_2_ == 3;
            modelbiped = ForgeHooksClient.getArmorModel(p_77032_1_, itemstack, p_77032_2_, modelbiped);
            this.setRenderPassModel(modelbiped);
            modelbiped.onGround = this.mainModel.onGround;
            modelbiped.isRiding = this.mainModel.isRiding;
            modelbiped.isChild = this.mainModel.isChild;
            modelbiped = CustomArmor.get((ModelBiped)modelbiped, (String)texture.getResourcePath(), (float)(p_77032_2_ == 2 ? 0.5f : 1.0f)).armorModel;
            int j = itemarmor.getColor(itemstack);
            if (j != -1) {
                float f1 = (float)(j >> 16 & 0xFF) / 255.0f;
                float f2 = (float)(j >> 8 & 0xFF) / 255.0f;
                float f3 = (float)(j & 0xFF) / 255.0f;
                GL11.glColor3f((float)f1, (float)f2, (float)f3);
                if (itemstack.isItemEnchanted()) {
                    return 31;
                }
                return 16;
            }
            GL11.glColor3f((float)1.0f, (float)1.0f, (float)1.0f);
            if (itemstack.isItemEnchanted()) {
                return 15;
            }
            return 1;
        }
        return -1;
    }

    @Override
    public void renderFirstPersonArm(EntityPlayer p_82441_1_) {
        float f = 1.0f;
        GL11.glColor3f((float)f, (float)f, (float)f);

        if (p_82441_1_ instanceof AbstractClientPlayer && WawelAuthCompat.isModernSkinSupportEnabled()) {
            ModelBiped previousModel = this.modelBipedMain;
            this.modelBipedMain = this.wawelFirstPersonModel;
            try {
                super.renderFirstPersonArm(p_82441_1_);
            } finally {
                this.modelBipedMain = previousModel;
            }
            return;
        }

        // SSB's hand event updates modelBipedMain, not the straight arm drawn here.
        // Use its converter only on a vanilla model, never on ModelRendererBends boxes.
        ModelBiped model = SimpleSkinBackportCompat.prepareFirstPersonModel(this.ssbFirstPersonModel, p_82441_1_)
            ? this.ssbFirstPersonModel : new ModelBiped();
        model.onGround = 0.0f;
        model.setRotationAngles(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0625f, p_82441_1_);
        model.bipedRightArm.render(0.0625f);
    }

    private float interpolateRotation(float p_77034_1_, float p_77034_2_, float p_77034_3_) {
        float f3;
        for (f3 = p_77034_2_ - p_77034_1_; f3 < -180.0f; f3 += 360.0f) {
        }
        while (f3 >= 180.0f) {
            f3 -= 360.0f;
        }
        return p_77034_1_ + p_77034_3_ * f3;
    }

    protected void rotateSuperCorpse(EntityLivingBase p_77043_1_, float p_77043_2_, float p_77043_3_, float p_77043_4_) {
        GL11.glRotatef((float)(180.0f - p_77043_3_), (float)0.0f, (float)1.0f, (float)0.0f);
        if (p_77043_1_.deathTime > 0) {
            float f3 = ((float)p_77043_1_.deathTime + p_77043_4_ - 1.0f) / 20.0f * 1.6f;
            if ((f3 = MathHelper.sqrt_float(f3)) > 1.0f) {
                f3 = 1.0f;
            }
            GL11.glRotatef((float)(f3 * this.getDeathMaxRotation(p_77043_1_)), (float)0.0f, (float)0.0f, (float)1.0f);
        } else {
            String s = EnumChatFormatting.getTextWithoutFormattingCodes(p_77043_1_.getCommandSenderName());
            if (!(!s.equals("Dinnerbone") && !s.equals("Grumm") || p_77043_1_ instanceof EntityPlayer && ((EntityPlayer)p_77043_1_).getHideCape())) {
                GL11.glTranslatef((float)0.0f, (float)(p_77043_1_.height + 0.1f), (float)0.0f);
                GL11.glRotatef((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            }
        }
    }

    @Override
    public void doRender(AbstractClientPlayer p_76986_1_, double p_76986_2_, double p_76986_4_, double p_76986_6_, float p_76986_8_, float p_76986_9_) {
        if (this.refreshModel != MoBends.refreshModel) {
            this.mainModel = WawelAuthCompat.createPlayerModel(0.0f);
            this.modelBipedMain = (ModelBendsPlayer)this.mainModel;
            this.modelArmorChestplate = new ModelBendsPlayer(1.0f);
            this.modelArmor = new ModelBendsPlayer(0.5f);
            this.refreshModel = MoBends.refreshModel;
        }
        super.doRender(p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
    }

    @Override
    protected void rotateCorpse(AbstractClientPlayer argPlayer, float p_77043_2_, float p_77043_3_, float p_77043_4_) {
        if (argPlayer.isEntityAlive() && argPlayer.isPlayerSleeping()) {
            GL11.glRotatef((float)argPlayer.getBedOrientationInDegrees(), (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glRotatef((float)this.getDeathMaxRotation(argPlayer), (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glRotatef((float)270.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else {
            BoatState boat = Animation_Rowing.getBoatState(argPlayer, p_77043_4_);
            if (boat != null) {
                // Keep the seated body facing the boat even with EFR's yaw lock disabled.
                // Do not apply last frame's global swimming/riding offsets to this pose.
                super.rotateCorpse(argPlayer, p_77043_2_, Animation_Rowing.boatYaw(boat, p_77043_4_), p_77043_4_);
                GL11.glTranslatef(0.0f, Animation_Rowing.seatLift(boat.raft), 0.0f);
                return;
            }
            super.rotateCorpse(argPlayer, p_77043_2_, p_77043_3_, p_77043_4_);
            if (AquaAcrobaticsCompat.getState(argPlayer, p_77043_4_).isActive()) {
                // AA's RenderPlayer mixin has already rotated and positioned the whole player.
                // Suppress our transform even on entry, before setRotationAngles updates the model,
                // and through AA's stand-up blend so last frame's pose cannot add a second tilt.
                return;
            }
            ((ModelBendsPlayer)this.modelBipedMain).updateWithEntityData(argPlayer);
            ((ModelBendsPlayer)this.modelBipedMain).postRender(0.0625f);
        }
    }

    @Override
    protected void renderEquippedItems(AbstractClientPlayer argPlayer, float argPartialTicks) {
        ItemStack itemstack1;
        float f2;
        RenderPlayerEvent.Specials.Pre event = new RenderPlayerEvent.Specials.Pre(argPlayer, this, argPartialTicks);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return;
        }
        GL11.glColor3f((float)1.0f, (float)1.0f, (float)1.0f);
        ModelBendsPlayer trailModel = this.modelBipedMain instanceof ModelBendsPlayer ? (ModelBendsPlayer)this.modelBipedMain : null;
        SwordTrail trail = trailModel != null && ((SettingsBoolean)SettingsNode.getSetting((String)"swordTrail")).data
            ? trailModel.swordTrail : null;
        this.trailFrame = trail != null ? trail.modelview() : null;
        ItemStack itemstack = argPlayer.inventory.armorItemInSlot(3);
        if (itemstack != null && event.renderHelmet) {
            float f1;
            GL11.glPushMatrix();
            this.modelBipedMain.bipedBody.postRender(0.0625f);
            this.modelBipedMain.bipedHead.postRender(0.0625f);
            if (itemstack.getItem() instanceof ItemBlock) {
                boolean is3D;
                IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(itemstack, IItemRenderer.ItemRenderType.EQUIPPED);
                boolean bl = is3D = customRenderer != null && customRenderer.shouldUseRenderHelper(IItemRenderer.ItemRenderType.EQUIPPED, itemstack, IItemRenderer.ItemRendererHelper.BLOCK_3D);
                if (is3D || RenderBlocks.renderItemIn3d(Block.getBlockFromItem(itemstack.getItem()).getRenderType())) {
                    f1 = 0.625f;
                    GL11.glTranslatef((float)0.0f, (float)-0.25f, (float)0.0f);
                    GL11.glRotatef((float)90.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                    GL11.glScalef((float)f1, (float)(-f1), (float)(-f1));
                }
                this.renderManager.itemRenderer.renderItem(argPlayer, itemstack, 0);
            } else if (itemstack.getItem() == Items.skull) {
                f1 = 1.0625f;
                GL11.glScalef((float)f1, (float)(-f1), (float)(-f1));
                GameProfile gameprofile = null;
                if (itemstack.hasTagCompound()) {
                    NBTTagCompound nbttagcompound = itemstack.getTagCompound();
                    if (nbttagcompound.hasKey("SkullOwner", 10)) {
                        gameprofile = NBTUtil.func_152459_a(nbttagcompound.getCompoundTag("SkullOwner"));
                    } else if (nbttagcompound.hasKey("SkullOwner", 8) && !StringUtils.isNullOrEmpty(nbttagcompound.getString("SkullOwner"))) {
                        gameprofile = new GameProfile((UUID)null, nbttagcompound.getString("SkullOwner"));
                    }
                }
                TileEntitySkullRenderer.field_147536_b.func_152674_a(-0.5f, 0.0f, -0.5f, 1, 180.0f, itemstack.getItemDamage(), gameprofile);
            }
            GL11.glPopMatrix();
        }
        if (argPlayer.getCommandSenderName().equals("deadmau5") && argPlayer.func_152123_o()) {
            this.bindTexture(argPlayer.getLocationSkin());
            for (int j = 0; j < 2; ++j) {
                float f9 = argPlayer.prevRotationYaw + (argPlayer.rotationYaw - argPlayer.prevRotationYaw) * argPartialTicks - (argPlayer.prevRenderYawOffset + (argPlayer.renderYawOffset - argPlayer.prevRenderYawOffset) * argPartialTicks);
                float f10 = argPlayer.prevRotationPitch + (argPlayer.rotationPitch - argPlayer.prevRotationPitch) * argPartialTicks;
                GL11.glPushMatrix();
                GL11.glRotatef((float)f9, (float)0.0f, (float)1.0f, (float)0.0f);
                GL11.glRotatef((float)f10, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glTranslatef((float)(0.375f * (float)(j * 2 - 1)), (float)0.0f, (float)0.0f);
                GL11.glTranslatef((float)0.0f, (float)-0.375f, (float)0.0f);
                GL11.glRotatef((float)(-f10), (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)(-f9), (float)0.0f, (float)1.0f, (float)0.0f);
                f2 = 1.3333334f;
                GL11.glScalef((float)f2, (float)f2, (float)f2);
                this.modelBipedMain.renderEars(0.0625f);
                GL11.glPopMatrix();
            }
        }
        boolean flag = argPlayer.func_152122_n();
        boolean bl = flag = event.renderCape && flag;
        if (flag && !argPlayer.isInvisible() && !argPlayer.getHideCape()) {
            this.bindTexture(argPlayer.getLocationCape());
            GL11.glPushMatrix();
            this.modelBipedMain.bipedBody.postRender(0.0625f);
            GL11.glTranslatef((float)0.0f, (float)-0.75f, (float)0.125f);
            double d3 = argPlayer.field_71091_bM + (argPlayer.field_71094_bP - argPlayer.field_71091_bM) * (double)argPartialTicks - (argPlayer.prevPosX + (argPlayer.posX - argPlayer.prevPosX) * (double)argPartialTicks);
            double d4 = argPlayer.field_71096_bN + (argPlayer.field_71095_bQ - argPlayer.field_71096_bN) * (double)argPartialTicks - (argPlayer.prevPosY + (argPlayer.posY - argPlayer.prevPosY) * (double)argPartialTicks);
            double d0 = argPlayer.field_71097_bO + (argPlayer.field_71085_bR - argPlayer.field_71097_bO) * (double)argPartialTicks - (argPlayer.prevPosZ + (argPlayer.posZ - argPlayer.prevPosZ) * (double)argPartialTicks);
            float f4 = argPlayer.prevRenderYawOffset + (argPlayer.renderYawOffset - argPlayer.prevRenderYawOffset) * argPartialTicks;
            double d1 = MathHelper.sin(f4 * (float)Math.PI / 180.0f);
            double d2 = -MathHelper.cos(f4 * (float)Math.PI / 180.0f);
            float f5 = (float)d4 * 10.0f;
            if (f5 < -6.0f) {
                f5 = -6.0f;
            }
            if (f5 > 32.0f) {
                f5 = 32.0f;
            }
            float f6 = (float)(d3 * d1 + d0 * d2) * 100.0f;
            float f7 = (float)(d3 * d2 - d0 * d1) * 100.0f;
            if (f6 < 0.0f) {
                f6 = 0.0f;
            }
            float f8 = argPlayer.prevCameraYaw + (argPlayer.cameraYaw - argPlayer.prevCameraYaw) * argPartialTicks;
            f5 += MathHelper.sin((argPlayer.prevDistanceWalkedModified + (argPlayer.distanceWalkedModified - argPlayer.prevDistanceWalkedModified) * argPartialTicks) * 6.0f) * 32.0f * f8;
            if (argPlayer.isSneaking()) {
                f5 += 25.0f;
            }
            boolean creativeFlying = PlayerAnimationConfig.isEnabled("flying")
                && argPlayer.capabilities.isFlying
                && !argPlayer.onGround
                && !EtFuturumRequiemCompat.isElytraFlying(argPlayer);
            if (creativeFlying) {
                float flutter = MathHelper.sin((argPlayer.ticksExisted + argPartialTicks) * 0.15f) * 2.0f;
                float sideSway = MathHelper.clamp_float(f7 * 0.15f, -8.0f, 8.0f);
                GL11.glRotatef(8.0f + flutter, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(sideSway, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(-sideSway, 0.0f, 1.0f, 0.0f);
            } else {
                GL11.glRotatef((float)(6.0f + f6 / 2.0f + f5), (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)(f7 / 2.0f), (float)0.0f, (float)0.0f, (float)1.0f);
                GL11.glRotatef((float)(-f7 / 2.0f), (float)0.0f, (float)1.0f, (float)0.0f);
            }
            GL11.glRotatef((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            this.modelBipedMain.renderCloak(0.0625f);
            GL11.glPopMatrix();
        }
        if ((itemstack1 = argPlayer.inventory.getCurrentItem()) != null && event.renderItem) {
            if (argPlayer.fishEntity != null) {
                itemstack1 = new ItemStack(Items.stick);
            }
            ModelBendsPlayer bends = this.modelBipedMain instanceof ModelBendsPlayer ? (ModelBendsPlayer)this.modelBipedMain : null;
            if (bends == null || bends.rightItem.isVisible()) {
                GL11.glPushMatrix();
                this.modelBipedMain.bipedRightArm.postRender(0.0625f);
                GL11.glTranslatef(0.0f, PlayerRig.GRIP_DROP * 0.0625f, 0.0f);
                if (bends != null) {
                    bends.rightItem.apply(0.0625f);
                    GL11.glRotatef((float)bends.renderItemRotation.vSmooth.x, (float)1.0f, (float)0.0f, (float)0.0f);
                    GL11.glRotatef((float)bends.renderItemRotation.vSmooth.y, (float)0.0f, (float)-1.0f, (float)0.0f);
                    GL11.glRotatef((float)bends.renderItemRotation.vSmooth.z, (float)0.0f, (float)0.0f, (float)1.0f);
                }
                this.renderHeldItem(argPlayer, itemstack1, trail != null && trail.takeSample() ? trail : null);
                GL11.glPopMatrix();
            }
            if (bends != null && bends.rightItemFree.isVisible()) {
                // A free item hangs from the model root, at the resting grip, so it can leave the hand.
                GL11.glPushMatrix();
                float[] grip = PlayerRig.restPivot(bends, PlayerRig.Bone.RIGHT_ITEM_FREE);
                GL11.glTranslatef(grip[0] * 0.0625f, grip[1] * 0.0625f, grip[2] * 0.0625f);
                bends.rightItemFree.apply(0.0625f);
                this.renderHeldItem(argPlayer, itemstack1, null);
                GL11.glPopMatrix();
            }
        }
        if (trail != null) trail.render();
        MinecraftForge.EVENT_BUS.post(new RenderPlayerEvent.Specials.Post(argPlayer, this, argPartialTicks));
    }

    /** The modelview the player model draws in, while drawing its equipment with the sword trail on. */
    private float[] trailFrame;

    /** Vanilla's per-item placement from the grip, then the draw; {@code trail} (may be null) samples the blade. */
    private void renderHeldItem(AbstractClientPlayer argPlayer, ItemStack itemstack1, SwordTrail trail) {
        IItemRenderer customRenderer;
        boolean is3D;
        float f2;
        GL11.glTranslatef((float)-0.0625f, (float)-0.125f, (float)0.0625f);
        EnumAction enumaction = null;
        if (argPlayer.getItemInUseCount() > 0) {
            enumaction = itemstack1.getItemUseAction();
        }
        boolean bl2 = is3D = (customRenderer = MinecraftForgeClient.getItemRenderer(itemstack1, IItemRenderer.ItemRenderType.EQUIPPED)) != null && customRenderer.shouldUseRenderHelper(IItemRenderer.ItemRenderType.EQUIPPED, itemstack1, IItemRenderer.ItemRendererHelper.BLOCK_3D);
        if (is3D || itemstack1.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(itemstack1.getItem()).getRenderType())) {
            f2 = 0.5f;
            GL11.glTranslatef((float)0.0f, (float)0.1875f, (float)-0.3125f);
            GL11.glRotatef((float)20.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glScalef((float)(-(f2 *= 0.75f)), (float)(-f2), (float)f2);
        } else if (itemstack1.getItem() == Items.bow) {
            f2 = 0.625f;
            GL11.glTranslatef((float)0.0f, (float)0.125f, (float)0.3125f);
            GL11.glRotatef((float)-20.0f, (float)0.0f, (float)1.0f, (float)0.0f);
            GL11.glScalef((float)f2, (float)(-f2), (float)f2);
            GL11.glRotatef((float)-100.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else if (itemstack1.getItem().isFull3D()) {
            f2 = 0.625f;
            if (itemstack1.getItem().shouldRotateAroundWhenRendering()) {
                GL11.glRotatef((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
                GL11.glTranslatef((float)0.0f, (float)-0.125f, (float)0.0f);
            }
            // Backhand may be using the other hand, and Just A Shield may suppress
            // sword blocking. Respect their final per-hand flag, not just the use timer.
            if (this.modelBipedMain.heldItemRight == 3 && enumaction == EnumAction.block) {
                GL11.glTranslatef((float)0.05f, (float)0.0f, (float)-0.1f);
                GL11.glRotatef((float)-50.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                GL11.glRotatef((float)-10.0f, (float)1.0f, (float)0.0f, (float)0.0f);
                GL11.glRotatef((float)-60.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            }
            GL11.glTranslatef((float)0.0f, (float)0.1875f, (float)0.0f);
            GL11.glScalef((float)f2, (float)(-f2), (float)f2);
            GL11.glRotatef((float)-100.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glRotatef((float)45.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        } else {
            f2 = 0.375f;
            GL11.glTranslatef((float)0.25f, (float)0.1875f, (float)-0.1875f);
            GL11.glScalef((float)f2, (float)f2, (float)f2);
            GL11.glRotatef((float)60.0f, (float)0.0f, (float)0.0f, (float)1.0f);
            GL11.glRotatef((float)-90.0f, (float)1.0f, (float)0.0f, (float)0.0f);
            GL11.glRotatef((float)20.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        }
        if (trail != null && this.trailFrame != null) trail.sample(this.trailFrame, trail.modelview());
        if (itemstack1.getItem().requiresMultipleRenderPasses()) {
            for (int k = 0; k < itemstack1.getItem().getRenderPasses(itemstack1.getItemDamage()); ++k) {
                int i = itemstack1.getItem().getColorFromItemStack(itemstack1, k);
                float f12 = (float)(i >> 16 & 0xFF) / 255.0f;
                float f3 = (float)(i >> 8 & 0xFF) / 255.0f;
                float f4 = (float)(i & 0xFF) / 255.0f;
                GL11.glColor4f((float)f12, (float)f3, (float)f4, (float)1.0f);
                this.renderManager.itemRenderer.renderItem(argPlayer, itemstack1, k);
            }
        } else {
            int k = itemstack1.getItem().getColorFromItemStack(itemstack1, 0);
            float f11 = (float)(k >> 16 & 0xFF) / 255.0f;
            float f12 = (float)(k >> 8 & 0xFF) / 255.0f;
            float f3 = (float)(k & 0xFF) / 255.0f;
            GL11.glColor4f((float)f11, (float)f12, (float)f3, (float)1.0f);
            this.renderManager.itemRenderer.renderItem(argPlayer, itemstack1, 0);
        }
    }
}
