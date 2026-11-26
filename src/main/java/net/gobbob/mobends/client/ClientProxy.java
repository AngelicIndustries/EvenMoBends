package net.gobbob.mobends.client;

import cpw.mods.fml.common.FMLCommonHandler;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.CommonProxy;
import net.gobbob.mobends.client.animation.CommandEvenMoBends;
import net.gobbob.mobends.compat.AdventureBackpackCompat;
import net.gobbob.mobends.compat.AetherCompat;
import net.gobbob.mobends.compat.CompatibilityPolicy;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.compat.MobAdoption;
import net.gobbob.mobends.compat.SpecialMobsCompat;
import net.gobbob.mobends.compat.VillageNamesCompat;
import net.gobbob.mobends.compat.WildMobsCompat;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.event.EventHandler_DataUpdate;
import net.gobbob.mobends.settings.SettingsBoolean;
import net.gobbob.mobends.settings.SettingsNode;
import net.minecraft.util.ResourceLocation;
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
        PlayerAnimationConfig.load(config);
        dropStrayCategories(config);
        net.gobbob.mobends.config.MobSwitches.load(config);
        AnimatedEntity.registerRendering();
        MinecraftForge.EVENT_BUS.register(new EventHandler_DataUpdate());
        MinecraftForge.EVENT_BUS.register(new net.gobbob.mobends.client.animation.HandAction.Listener());
        FMLCommonHandler.instance().bus().register(new EventHandler_DataUpdate());
        readGeneral(config);
        FMLCommonHandler.instance().bus().register(this);
    }

    /**
     * Configuration.get lowercases category names but getCategory doesn't: builds before 0.0.4's release asked for
     * "General" and "Animate" by getCategory, read those empty categories and wrote them back. Drop them.
     */
    private static void dropStrayCategories(Configuration config) {
        for (String stray : new String[] {"General", "Animate"}) {
            if (config.hasCategory(stray) && config.getCategory(stray).isEmpty()) {
                config.removeCategory(config.getCategory(stray));
            }
        }
    }

    /** The General category, as the config screen shows it. */
    private static void readGeneral(Configuration config) {
        config.getCategory("general").setLanguageKey("mobends.config.general");
        ((SettingsBoolean)SettingsNode.getSetting("swordTrail")).data = config.get("general", "Sword Trail", true)
            .setLanguageKey("mobends.settings.swordTrail").getBoolean();
        net.gobbob.mobends.compat.salamander.PackFaces.combine = config.get("general", "Combine Face Packs", true,
            "Resource packs that only animate the player's face, such as Just Expressions, draw it on Mo' Bends' head "
                + "while Mo' Bends animates the body. Off, such a pack replaces the player.")
            .setLanguageKey("mobends.config.general.combineFaces").getBoolean();
        MobAdoption.enabled = config.get("general", "Animate Other Mods' Mobs", true,
            "Other mods' spiders and zombies drawn with the vanilla model take Mo' Bends' animations, keeping their own "
                + "texture, size and eyes.")
            .setLanguageKey("mobends.config.general.adoptMobs").getBoolean();
        // Mobs left alone are switched off in the menu now, each on its own (see MobSwitches).
        if (config.getCategory("general").containsKey("Mobs Left Alone")) {
            for (String name : config.get("general", "Mobs Left Alone", new String[0]).getStringList()) {
                net.gobbob.mobends.config.MobSwitches.set(config, name, false);
            }
            config.getCategory("general").remove("Mobs Left Alone");
        }
    }

    /** The config screen edited the loaded config: take its values and write it. */
    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void onConfigChanged(cpw.mods.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!event.modID.equals(net.gobbob.mobends.MoBends.MODID)) return;
        readGeneral(net.gobbob.mobends.MoBends.config);
        PlayerAnimationConfig.load(net.gobbob.mobends.MoBends.config);
        net.gobbob.mobends.MoBends.config.save();
        MobAdoption.reapply();
    }

    /**
     * Other mods' renderers are all in place once the game runs (FML fills the render map after the last mod's
     * events): the first tick looks for spiders and zombies to animate.
     */
    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void onClientTick(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) {
        MobAdoption.scanOnce();
    }

    @Override
    public void init() {
        EtFuturumRequiemCompat.registerBoatRenderers();
        AetherCompat.install();
        AdventureBackpackCompat.install();
        // EFR renders its own zombie villagers and husks, also from init: animate them as zombies, after it.
        EtFuturumRequiemCompat.registerZombieRenderers();
        // Special Mobs too, also from init: its zombies and spiders animate as theirs do, after it.
        SpecialMobsCompat.registerRenderers();
        // And Village Names, whose modern zombie skins come with a zombie renderer of its own.
        VillageNamesCompat.registerRenderers();
        // And Wild Mobs' zomgus, whose mushrooms keep adoption away: drawn with them on the rig.
        WildMobsCompat.registerRenderers();
        ClientCommandHandler.instance.registerCommand(new CommandEvenMoBends());
    }

}
