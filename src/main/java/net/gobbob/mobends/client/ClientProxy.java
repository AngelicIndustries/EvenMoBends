package net.gobbob.mobends.client;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.CommonProxy;
import net.gobbob.mobends.client.animation.CommandEvenMoBends;
import net.gobbob.mobends.client.render.BlinkingTextures;
import net.gobbob.mobends.client.renderer.entity.RenderBlinkingPig;
import net.gobbob.mobends.compat.CompatibilityPolicy;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.config.BlinkConfig;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.event.EventHandler_DataUpdate;
import net.gobbob.mobends.event.EventHandler_Keyboard;
import net.gobbob.mobends.settings.SettingsBoolean;
import net.gobbob.mobends.settings.SettingsNode;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.passive.EntityPig;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;

public class ClientProxy
extends CommonProxy {
    public static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    public static final ResourceLocation texture_NULL = new ResourceLocation("mobends", "textures/white.png");
    public static final ResourceLocation GOBLIN_CAPE = new ResourceLocation("mobends", "textures/goblinCape.png");

    @Override
    public void preInit(Configuration config) {
        CompatibilityPolicy.logActiveMode();
        BlinkConfig.load(config);
        PlayerAnimationConfig.load(config);
        for (int i = 0; i < AnimatedEntity.animatedEntities.length; ++i) {
            AnimatedEntity.animatedEntities[i].animate = config.get("Animate", AnimatedEntity.animatedEntities[i].id, true).getBoolean();
        }
        AnimatedEntity.registerRendering();
        ClientRegistry.registerKeyBinding(EventHandler_Keyboard.key_Menu);
        MinecraftForge.EVENT_BUS.register(new EventHandler_DataUpdate());
        MinecraftForge.EVENT_BUS.register(new net.gobbob.mobends.client.animation.HandAction.Listener());
        FMLCommonHandler.instance().bus().register(new EventHandler_DataUpdate());
        FMLCommonHandler.instance().bus().register(new EventHandler_Keyboard());
        ((SettingsBoolean)SettingsNode.getSetting((String)"swordTrail")).data = config.get("General", "Sword Trail", true).getBoolean();
        BlinkingTextures.registerReloadListener();
    }

    @Override
    public void init() {
        // EFR installs its Technoblade pig renderer during init. Register after it so blinking remains active,
        // while retaining EFR's crown render pass when that mod is present.
        RenderingRegistry.registerEntityRenderingHandler(EntityPig.class, createPigRenderer());
        EtFuturumRequiemCompat.registerBoatRenderers();
        // EFR renders its own zombie villagers, also from init: animate them as zombies, after it.
        for (AnimatedEntity entity : AnimatedEntity.animatedEntities) {
            if (entity.id.equals("zombie") && entity.animate) EtFuturumRequiemCompat.registerZombieVillagerRenderer();
        }
        ClientCommandHandler.instance.registerCommand(new CommandEvenMoBends());
    }

    private static Render createPigRenderer() {
        if (Loader.isModLoaded("etfuturum")) {
            try {
                return (Render)Class.forName(
                    "net.gobbob.mobends.client.renderer.entity.RenderBlinkingPigEtFuturum")
                    .getDeclaredConstructor()
                    .newInstance();
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Keep pig blinking usable if EFR or its renderer API differs from the tested version.
            }
        }
        return new RenderBlinkingPig();
    }
}
