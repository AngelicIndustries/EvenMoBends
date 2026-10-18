package net.gobbob.mobends.client.renderer.entity;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.player.Animation_Rowing;
import net.gobbob.mobends.client.animation.PlayerRig;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.CemCompat;
import net.gobbob.mobends.compat.DraconicEvolutionCompat;
import net.gobbob.mobends.client.model.BonedArmorModel;
import net.gobbob.mobends.client.model.ModelRendererBends_SeperatedChild;
import net.gobbob.mobends.client.renderer.SwordTrail;
import net.gobbob.mobends.compat.BackhandCompat;
import net.gobbob.mobends.compat.AquaAcrobaticsCompat;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat.BoatState;
import net.gobbob.mobends.compat.SimpleSkinBackportCompat;
import net.gobbob.mobends.compat.WaveyCapesCompat;
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
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public class RenderBendsPlayer
extends RenderPlayer {
    private static final float MODEL_SCALE = 0.0625f;
    public int refreshModel = 0;
    private final RenderPlayer packRenderer = new RenderPlayer();
    private final ModelBendsPlayer firstPersonModel = new ModelBendsPlayer(0);
    /** The model RenderPlayer's constructor installed; with Wawel Auth, its player model, which adds the sleeve. */
    private final ModelBiped wawelFirstPersonModel = this.modelBipedMain;
    private final ModelBiped ssbFirstPersonModel = new ModelBiped();
    /** Wavey Capes' cape layer for this renderer, made on first use. */
    private Object waveyCape;
    /** Draws another mod's armor model on the bones, for the render pass at hand. */
    private final BonedArmorModel bonedArmor = new BonedArmorModel();

    public RenderBendsPlayer() {
        this.mainModel = WawelAuthCompat.createPlayerModel(0.0f);
        this.modelBipedMain = (ModelBendsPlayer)this.mainModel;
        this.modelArmorChestplate = new ModelBendsPlayer(1.0f);
        this.modelArmor = new ModelBendsPlayer(0.5f);
    }

    @Override
    protected int shouldRenderPass(AbstractClientPlayer p_77032_1_, int p_77032_2_, float p_77032_3_) {
        Item item;
        ItemStack itemstack = p_77032_1_.inventory.armorItemInSlot(3 - p_77032_2_);
        RenderPlayerEvent.SetArmorModel event = new RenderPlayerEvent.SetArmorModel(p_77032_1_, this, 3 - p_77032_2_, p_77032_3_, itemstack);
        MinecraftForge.EVENT_BUS.post(event);
        if (event.result != -1) {
            float[] scales = DraconicEvolutionCompat.armorScales(this.renderPassModel);
            if (scales != null && this.modelBipedMain instanceof ModelBendsPlayer) {
                // Draconic Evolution put in its own armor model, posed like vanilla: draw it on the bones.
                DraconicEvolutionCompat.prepareArmor((ModelBiped)this.renderPassModel);
                this.setRenderPassModel(this.bonedArmor.wrap((ModelBiped)this.renderPassModel, (ModelBendsPlayer)this.modelBipedMain, scales));
            }
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
    protected void renderModel(EntityLivingBase entity, float swing, float swingAmount, float age, float headYaw, float headPitch, float scale) {
        // Armor drawn on the bones hugs the body: the skin's outer layer would poke through it, so it is hidden there.
        ModelBendsPlayer model = this.modelBipedMain instanceof ModelBendsPlayer ? (ModelBendsPlayer)this.modelBipedMain : null;
        ModelBendsPlayer.WearState shown = null;
        if (model != null && entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)entity;
            boolean hat = DraconicEvolutionCompat.hugs(player, 0), torso = DraconicEvolutionCompat.hugs(player, 1);
            boolean legs = DraconicEvolutionCompat.hugs(player, 2);
            if (hat || torso || legs) shown = model.coverWear(hat, torso, legs);
        }
        try {
            super.renderModel(entity, swing, swingAmount, age, headYaw, headPitch, scale);
        } finally {
            if (shown != null) model.restoreWear(shown);
        }
    }

    @Override
    public void renderFirstPersonArm(EntityPlayer p_82441_1_) {
        if (p_82441_1_ instanceof AbstractClientPlayer && CemCompat.hasModel(p_82441_1_,
            this.packRenderer.modelBipedMain, ((AbstractClientPlayer)p_82441_1_).getLocationSkin())) {
            this.packRenderer.setRenderManager(this.renderManager);
            this.packRenderer.renderFirstPersonArm(p_82441_1_);
            return;
        }
        float f = 1.0f;
        GL11.glColor3f((float)f, (float)f, (float)f);

        if (p_82441_1_ instanceof AbstractClientPlayer && WawelAuthCompat.isLoaded()) {
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
        if (SimpleSkinBackportCompat.prepareFirstPersonModel(this.ssbFirstPersonModel, p_82441_1_)) {
            this.ssbFirstPersonModel.onGround = 0.0f;
            this.ssbFirstPersonModel.setRotationAngles(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, MODEL_SCALE, p_82441_1_);
            this.ssbFirstPersonModel.bipedRightArm.render(MODEL_SCALE);
        } else {
            this.firstPersonModel.geometry().render(MODEL_SCALE,
                name -> this.firstPersonModel.geometry().rig.descendant(name, "rightArm"));
        }
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
        if (CemCompat.hasModel(p_76986_1_, this.packRenderer.modelBipedMain, p_76986_1_.getLocationSkin())) {
            this.packRenderer.setRenderManager(this.renderManager);
            this.packRenderer.doRender(p_76986_1_, p_76986_2_, p_76986_4_, p_76986_6_, p_76986_8_, p_76986_9_);
            return;
        }
        if (this.refreshModel != MoBends.refreshModel) {
            this.mainModel = WawelAuthCompat.createPlayerModel(0.0f);
            this.modelBipedMain = (ModelBendsPlayer)this.mainModel;
            this.modelArmorChestplate = new ModelBendsPlayer(1.0f);
            this.modelArmor = new ModelBendsPlayer(0.5f);
            this.refreshModel = MoBends.refreshModel;
        }
        if (WawelAuthCompat.isLoaded() && this.modelBipedMain instanceof ModelBendsPlayer) {
            ((ModelBendsPlayer)this.modelBipedMain).setSlim(WawelAuthCompat.isSlim(p_76986_1_));
        }
        Data_Player data = Data_Player.get(p_76986_1_.getEntityId());
        double landingOffset = data.landingRenderOffset(p_76986_9_);
        super.doRender(p_76986_1_, p_76986_2_, p_76986_4_ + landingOffset, p_76986_6_, p_76986_8_, p_76986_9_);
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
            Data_Player data = Data_Player.get(argPlayer.getEntityId());
            if (data != null) {
                // Lying in the water, draw the body back from a wall ahead (-Z is the facing here).
                SwimRoom.update(argPlayer, data, p_77043_3_, p_77043_4_);
                if (data.swimShift > 0.001f) GL11.glTranslatef(0.0f, 0.0f, data.swimShift);
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
            ((ModelBendsPlayer)this.modelBipedMain).geometry().attach("head", MODEL_SCALE);
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
            boolean wavey = WaveyCapesCompat.drawsCape();
            if (!wavey) this.bindTexture(argPlayer.getLocationCape());
            GL11.glPushMatrix();
            ((ModelBendsPlayer)this.modelBipedMain).geometry().attach("body", MODEL_SCALE);
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
            Data_Player flight = Data_Player.get(argPlayer.getEntityId());
            boolean creativeFlying = PlayerAnimationConfig.isEnabled("flying")
                && flight != null && flight.isFlying(argPlayer)
                && !flight.isOnGround()
                && !EtFuturumRequiemCompat.isElytraFlying(argPlayer);
            if (wavey) {
                // Wavey Capes cancels renderCloak and draws its own cape from vanilla's cape matrix: it undoes
                // vanilla's swing, clamped as it clamps it, then swings the cape itself.
                f6 = Math.min(f6, 150.0f);
                f7 = MathHelper.clamp_float(f7, -20.0f, 20.0f);
                GL11.glRotatef(6.0f + f6 / 2.0f + f5, 1.0f, 0.0f, 0.0f);
                GL11.glRotatef(f7 / 2.0f, 0.0f, 0.0f, 1.0f);
                GL11.glRotatef(-f7 / 2.0f, 0.0f, 1.0f, 0.0f);
                GL11.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
                if (this.waveyCape == null) this.waveyCape = WaveyCapesCompat.createLayer(this, this.modelBipedMain);
                WaveyCapesCompat.render(this.waveyCape, argPlayer, argPartialTicks);
                GL11.glPopMatrix();
            } else if (creativeFlying) {
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
            if (!wavey) {
                GL11.glRotatef((float)180.0f, (float)0.0f, (float)1.0f, (float)0.0f);
                this.modelBipedMain.renderCloak(0.0625f);
                GL11.glPopMatrix();
            }
        }
        if ((itemstack1 = argPlayer.inventory.getCurrentItem()) != null && event.renderItem) {
            if (argPlayer.fishEntity != null) {
                itemstack1 = new ItemStack(Items.stick);
            }
            ModelBendsPlayer bends = this.modelBipedMain instanceof ModelBendsPlayer ? (ModelBendsPlayer)this.modelBipedMain : null;
            if (bends == null || bends.rightItem.isVisible()) {
                GL11.glPushMatrix();
                if (bends != null) {
                    bends.geometry().attach("rightItem", MODEL_SCALE);
                    if (isDrawnFlat(itemstack1)) {
                        Vector3f pivot = bends.rightItem.fistPivot(false, bends.isSlim(), new Vector3f());
                        GL11.glTranslatef(pivot.x * MODEL_SCALE, pivot.y * MODEL_SCALE, pivot.z * MODEL_SCALE);
                    }
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
                bends.geometry().attach("rightItemFree", MODEL_SCALE);
                this.renderHeldItem(argPlayer, itemstack1, null);
                GL11.glPopMatrix();
            }
        }
        if (trail != null) trail.render();
        // Backhand draws the off-hand item from the left arm here; put it at the leftItem bone.
        ModelRendererBends_SeperatedChild leftArm = trailModel != null && trailModel.bipedLeftArm instanceof ModelRendererBends_SeperatedChild
            && BackhandCompat.getOffhandItem(argPlayer) != null ? (ModelRendererBends_SeperatedChild)trailModel.bipedLeftArm : null;
        SwordTrail offhandTrail = trail != null && trailModel.offhandTrail.takeSample() ? trailModel.offhandTrail : null;
        if (leftArm != null) leftArm.afterPostRender = () -> this.placeOffhandItem(trailModel, BackhandCompat.getOffhandItem(argPlayer), offhandTrail);
        // A handler that throws mid-draw must not leave its matrices to the rest of the frame.
        int depth = GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH);
        try {
            MinecraftForge.EVENT_BUS.post(new RenderPlayerEvent.Specials.Post(argPlayer, this, argPartialTicks));
        } finally {
            if (leftArm != null) leftArm.afterPostRender = null;
            while (GL11.glGetInteger(GL11.GL_MODELVIEW_STACK_DEPTH) > depth) GL11.glPopMatrix();
        }
        if (trail != null) trailModel.offhandTrail.render();
    }

    /**
     * After the left arm's postRender, for Backhand's off-hand item: moves to the grip and the leftItem bone, then
     * mirrored like the right hand's placement, and undoes the offsets Backhand applies next for vanilla's arm (it
     * moves by minus the arm's pivot, mirrors, moves by (-x, y, -z) of it, then 7 px down), so the item lands as the
     * right hand's would, mirrored.
     */
    private void placeOffhandItem(ModelBendsPlayer model, ItemStack item, SwordTrail trail) {
        float s = 0.0625f;
        ModelRenderer arm = model.bipedLeftArm;
        GL11.glTranslatef(0.0f, PlayerRig.GRIP_DROP * s, 0.0f);
        model.leftItem.apply(s);
        if (item != null && isDrawnFlat(item)) {
            Vector3f pivot = model.leftItem.fistPivot(true, model.isSlim(), new Vector3f());
            GL11.glTranslatef(pivot.x * s, pivot.y * s, pivot.z * s);
        }
        GL11.glScalef(-1.0f, 1.0f, 1.0f);
        // The right hand's (-1, -2, 1) px from the grip: here the item's placement starts, as in the right hand.
        GL11.glTranslatef(-s, -2.0f * s, s);
        if (trail != null && this.trailFrame != null) trail.sampleFromHand(this.trailFrame, trail.modelview());
        // Backhand's (-1, 7, 1) px undone.
        GL11.glTranslatef(s, -7.0f * s, -s);
        GL11.glTranslatef(arm.rotationPointX * s, -arm.rotationPointY * s, arm.rotationPointZ * s);
        GL11.glScalef(-1.0f, 1.0f, 1.0f);
        GL11.glTranslatef(arm.rotationPointX * s, arm.rotationPointY * s, arm.rotationPointZ * s);
    }

    /** The modelview the player model draws in, while drawing its equipment with the sword trail on. */
    private float[] trailFrame;

    /**
     * Whether {@link #renderHeldItem} draws the item as a flat icon beside the grip: not a 3D block, a bow or a
     * full-3D item such as a sword or tool, whose grip is in the fist. Item bone turns pivot such an item at the fist.
     */
    public static boolean isDrawnFlat(ItemStack itemstack) {
        IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(itemstack, IItemRenderer.ItemRenderType.EQUIPPED);
        if (customRenderer != null && customRenderer.shouldUseRenderHelper(IItemRenderer.ItemRenderType.EQUIPPED, itemstack,
            IItemRenderer.ItemRendererHelper.BLOCK_3D)) return false;
        if (itemstack.getItem() instanceof ItemBlock && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(itemstack.getItem()).getRenderType())) return false;
        return itemstack.getItem() != Items.bow && !itemstack.getItem().isFull3D();
    }

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
