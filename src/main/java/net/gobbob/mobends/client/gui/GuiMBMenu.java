package net.gobbob.mobends.client.gui;

import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.ClientProxy;
import net.gobbob.mobends.settings.SettingsBoolean;
import net.gobbob.mobends.settings.SettingsNode;
import net.gobbob.mobends.util.Color;
import net.gobbob.mobends.util.Draw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

/**
 * The Mo' Bends menu: the animated mobs (each with a preview and whether to animate it), the settings, and the blink
 * editor. Animations themselves are Blockbench files, edited outside the game.
 */
public class GuiMBMenu
extends GuiScreen {
    public static final ResourceLocation menuTitleTexture = new ResourceLocation("mobends", "textures/gui/menuTitle.png");
    public static final ResourceLocation displayBGTexture = new ResourceLocation("mobends", "textures/gui/displayBG.png");
    public float titleTransitionState = 0.0f;
    public boolean titleTransition = true;
    public float[] buttonPositions = new float[AnimatedEntity.animatedEntities.length];
    public float buttonRevealState = 0.0f;
    public float leftBgState = 0.0f;
    public float presetWindowState = 0.0f;
    public float previewRotation = 0.0f;
    public boolean customizeWindow;
    public boolean settingsWindow;
    public int animatedEntityID;

    public GuiMBMenu() {
        Keyboard.enableRepeatEvents((boolean)true);
        this.titleTransition = true;
        this.titleTransitionState = 0.0f;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        if (this.customizeWindow | this.settingsWindow) {
            this.buttonList.add(new GuiButton(0, 10, this.height - 30, 60, 20, I18n.format("mobends.gui.back")));
        } else {
            this.buttonList.add(new GuiButton(1, -90 + (int)(this.leftBgState * 100.0f), this.height - 30, 60, 20, I18n.format("mobends.menu.settings")));
            this.buttonList.add(new GuiButton(6, this.width / 2 - 80, this.height - 30, 160, 20, I18n.format("mobends.blink.title")));
        }
        if (this.settingsWindow) {
            for (int i = 0; i < SettingsNode.settings.length; ++i) {
                if (!(SettingsNode.settings[i] instanceof SettingsBoolean)) continue;
                this.buttonList.add(new GuiToggleButton(10 + i, (int)((float)this.width + this.presetWindowState * -500.0f + 20.0f), 50 + i * 25, ((SettingsBoolean)SettingsNode.settings[i]).data).setTitle(I18n.format("mobends.settings." + SettingsNode.settings[i].id), 100));
            }
        }
        if (this.customizeWindow) {
            this.buttonList.add(new GuiToggleButton(2, (int)((float)this.width + this.presetWindowState * -500.0f + 10.0f), 163, AnimatedEntity.animatedEntities[this.animatedEntityID].animate).setTitle(I18n.format("mobends.menu.animate"), 88));
        }
        for (int i = 0; i < AnimatedEntity.animatedEntities.length; ++i) {
            this.buttonList.add(new GuiButton(100 + i, (int)(this.buttonPositions[i] - 80.0f + this.presetWindowState * -100.0f), 70 + i * 25, 80, 20, I18n.format("mobends.entity." + AnimatedEntity.animatedEntities[i].id)));
        }
    }

    @Override
    protected void keyTyped(char par1, int par2) {
        if (par2 == Keyboard.KEY_ESCAPE) this.close();
    }

    public void close() {
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents((boolean)false);
        MoBends.saveConfig();
    }

    @Override
    public void updateScreen() {
        this.initGui();
        this.previewRotation += 2.0f;
    }

    @Override
    protected void actionPerformed(GuiButton par1GuiButton) {
        if (par1GuiButton.id >= 100) {
            this.animatedEntityID = par1GuiButton.id - 100;
            this.customizeWindow = true;
        } else if (par1GuiButton.id >= 10) {
            int s = 0;
            for (int i = 0; i < SettingsNode.settings.length; ++i) {
                if (!(SettingsNode.settings[i] instanceof SettingsBoolean)) continue;
                if (s == par1GuiButton.id - 10) {
                    ((SettingsBoolean)SettingsNode.settings[i]).data = !((SettingsBoolean)SettingsNode.settings[i]).data;
                    break;
                }
                ++s;
            }
        }
        switch (par1GuiButton.id) {
            case 0: {
                this.customizeWindow = false;
                this.settingsWindow = false;
                break;
            }
            case 1: {
                this.settingsWindow = true;
                break;
            }
            case 2: {
                AnimatedEntity.animatedEntities[this.animatedEntityID].animate = !AnimatedEntity.animatedEntities[this.animatedEntityID].animate;
                break;
            }
            case 6: {
                Minecraft.getMinecraft().displayGuiScreen(new GuiBlinkSkinEditor(this));
            }
        }
    }

    @Override
    public void drawScreen(int par1, int par2, float par3) {
        GL11.glDisable((int)2896);
        GL11.glEnable((int)3042);
        if (this.titleTransition) {
            this.titleTransitionState += (128.0f - this.titleTransitionState) / 5.0f;
        } else {
            this.titleTransitionState += (100.0f - this.titleTransitionState) / 7.0f;
            for (float i = 0.0f; i < (float)this.buttonPositions.length; i += 1.0f) {
                if (!(this.buttonRevealState >= i / (float)this.buttonPositions.length)) continue;
                int n = (int)i;
                this.buttonPositions[n] = this.buttonPositions[n] + (90.0f - this.buttonPositions[(int)i]) / 4.0f;
            }
            this.buttonRevealState += 0.05f;
            this.leftBgState += (1.0f - this.leftBgState) / 4.0f;
        }
        if (this.titleTransitionState > 126.0f) {
            this.titleTransition = false;
        }
        this.presetWindowState = this.customizeWindow | this.settingsWindow ? (this.presetWindowState += (1.0f - this.presetWindowState) / 4.0f) : (this.presetWindowState += (0.0f - this.presetWindowState) / 4.0f);
        GL11.glPushMatrix();
        Minecraft.getMinecraft().renderEngine.bindTexture(ClientProxy.texture_NULL);
        Draw.rectangle_xgradient(-this.leftBgState * -100.0f - 100.0f + this.presetWindowState * -100.0f, 0.0f, 100.0f, this.height, new Color(0.0f, 0.0f, 0.0f, 0.7f), new Color(0.0f, 0.0f, 0.0f, 0.3f));
        GL11.glColor4f((float)0.0f, (float)0.0f, (float)0.0f, (float)0.5f);
        Draw.rectangle(-this.leftBgState * -105.0f - 105.0f + this.presetWindowState * -105.0f + 100.0f, 0.0f, 5.0f, this.height);
        GL11.glPopMatrix();
        GL11.glColor3f((float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.renderEngine.bindTexture(menuTitleTexture);
        Draw.rectangle((float)(this.width / 2 - 64) + this.presetWindowState * (float)(-this.width / 2 + 80), this.titleTransitionState - 100.0f, 128.0f, 64.0f);
        if (this.presetWindowState > 0.0f) {
            GL11.glPushMatrix();
            Minecraft.getMinecraft().renderEngine.bindTexture(ClientProxy.texture_NULL);
            GL11.glColor4f((float)0.0f, (float)0.0f, (float)0.0f, (float)0.5f);
            float varWidth = 500.0f;
            Draw.rectangle((float)this.width + this.presetWindowState * -varWidth, 0.0f, varWidth, 20.0f);
            Draw.rectangle((float)this.width + this.presetWindowState * -varWidth, 25.0f, varWidth, this.height);
            GL11.glPopMatrix();
        }
        if (this.presetWindowState > 0.0f & this.customizeWindow) {
            this.displayCustomizeWindow();
        }
        if (this.presetWindowState > 0.0f & this.settingsWindow) {
            String title = I18n.format("mobends.menu.settings");
            this.drawString(this.fontRendererObj, title, (int)((float)this.width + this.presetWindowState * -500.0f + 250.0f - (float)(this.fontRendererObj.getStringWidth(title) / 2)), 5, 0xFFFFFF);
        }
        super.drawScreen(par1, par2, par3);
    }

    public void renderLivingEntity(int argX, int argY, int scale, EntityLivingBase par5EntityLivingBase) {
        GL11.glEnable((int)2903);
        GL11.glPushMatrix();
        GL11.glTranslatef((float)argX, (float)argY, (float)50.0f);
        GL11.glScalef((float)(-scale), (float)scale, (float)scale);
        GL11.glRotatef((float)180.0f, (float)0.0f, (float)0.0f, (float)1.0f);
        GL11.glRotatef((float)this.previewRotation, (float)0.0f, (float)1.0f, (float)0.0f);
        float f2 = par5EntityLivingBase.renderYawOffset;
        float f3 = par5EntityLivingBase.rotationYaw;
        float f4 = par5EntityLivingBase.rotationPitch;
        float f5 = par5EntityLivingBase.prevRotationYawHead;
        float f6 = par5EntityLivingBase.rotationYawHead;
        GL11.glRotatef((float)135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        GL11.glColor3f((float)1.0f, (float)1.0f, (float)1.0f);
        RenderHelper.enableStandardItemLighting();
        GL11.glRotatef((float)-135.0f, (float)0.0f, (float)1.0f, (float)0.0f);
        par5EntityLivingBase.renderYawOffset = 0.0f;
        par5EntityLivingBase.rotationYaw = 0.0f;
        par5EntityLivingBase.rotationPitch = 0.0f;
        par5EntityLivingBase.rotationYawHead = par5EntityLivingBase.rotationYaw;
        par5EntityLivingBase.prevRotationYawHead = par5EntityLivingBase.rotationYaw;
        GL11.glTranslatef((float)0.0f, (float)par5EntityLivingBase.yOffset, (float)0.0f);
        RenderManager.instance.playerViewY = 180.0f;
        par5EntityLivingBase.moveForward = 1.0f;
        RenderManager.instance.renderEntityWithPosYaw(par5EntityLivingBase, 0.0, 0.0, 0.0, 0.0f, 1.0f);
        par5EntityLivingBase.renderYawOffset = f2;
        par5EntityLivingBase.rotationYaw = f3;
        par5EntityLivingBase.rotationPitch = f4;
        par5EntityLivingBase.prevRotationYawHead = f5;
        par5EntityLivingBase.rotationYawHead = f6;
        GL11.glPopMatrix();
        RenderHelper.disableStandardItemLighting();
        GL11.glDisable((int)32826);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable((int)3553);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return true;
    }

    /** A mob's page: its preview, whether to animate it, and where its animations are edited. */
    public void displayCustomizeWindow() {
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        String title = I18n.format("mobends.menu.customization");
        this.drawCenteredString(this.fontRendererObj, title, (int)((float)this.width + this.presetWindowState * -500.0f + 250.0f), 5, 0xFFFFFF);
        GL11.glPushMatrix();
        GL11.glColor4f((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        this.mc.renderEngine.bindTexture(displayBGTexture);
        Draw.rectangle((float)this.width + this.presetWindowState * -500.0f + 10.0f, 35.0f, 128.0f, 128.0f);
        GL11.glPopMatrix();
        AnimatedEntity entity = AnimatedEntity.animatedEntities[this.animatedEntityID];
        if (entity.id.equals("player")) {
            this.renderLivingEntity((int)((float)this.width + this.presetWindowState * -500.0f + 10.0f + 64.0f), 150, 50, Minecraft.getMinecraft().thePlayer);
        } else {
            entity.entity.worldObj = Minecraft.getMinecraft().theWorld;
            this.renderLivingEntity((int)((float)this.width + this.presetWindowState * -500.0f + 10.0f + 64.0f), 150, 50, (EntityLivingBase)entity.entity);
        }
        int textX = (int)((float)this.width + this.presetWindowState * -500.0f + 150.0f);
        String rig = entity.id.equals("player") ? "" : " " + (entity.id.equals("cave_spider") ? "spider" : entity.id);
        this.drawString(this.fontRendererObj, I18n.format("mobends.menu.blockbench"), textX, 40, 0xFFFFFF);
        this.drawString(this.fontRendererObj, "/evenmobends blockbench" + rig, textX, 55, 0xAAAAAA);
        String warning = I18n.format("mobends.menu.restart");
        this.drawString(this.fontRendererObj, warning, (int)((float)this.width + this.presetWindowState * -500.0f + 20.0f), this.height - 20, 0xFFFFFF);
    }
}
