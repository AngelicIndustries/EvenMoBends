package net.gobbob.mobends;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.io.File;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.CommonProxy;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.settings.SettingsBoolean;
import net.gobbob.mobends.settings.SettingsNode;
import net.minecraftforge.common.config.Configuration;

@Mod(modid=MoBends.MODID, name=MoBends.MODNAME, version=MoBends.VERSION, dependencies="required-after:salamander@[0.0.3,);after:etfuturum;after:SpecialMobs;after:VillageNames;after:wildmobsmod",
    acceptableRemoteVersions="*", guiFactory="net.gobbob.mobends.client.gui.MoBendsGuiFactory",
    customProperties = {
        @Mod.CustomProperty(k = "iconFile", v = "logo.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/mobends/background.png")
    })
public class MoBends {
    public static final String MODID = "mobends";
    public static final String MODNAME = "Even Mo' Bends";
    public static final String VERSION = Tags.VERSION;
    @SidedProxy(serverSide="net.gobbob.mobends.CommonProxy", clientSide="net.gobbob.mobends.client.ClientProxy")
    public static CommonProxy proxy;
    @Mod.Instance(value="mobends")
    public static MoBends instance;
    public static File configFile;
    /** The config, kept loaded: the config screen edits it in place. */
    public static Configuration config;
    public static int refreshModel;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        proxy.preInit(config);
        config.save();
    }

    public static void saveConfig() {
        for (int i = 0; i < AnimatedEntity.animatedEntities.length; ++i) {
            config.get(net.gobbob.mobends.config.MobSwitches.CATEGORY, AnimatedEntity.animatedEntities[i].id, false).setValue(AnimatedEntity.animatedEntities[i].animate);
        }
        config.get("general", "Sword Trail", true).setValue(((SettingsBoolean)SettingsNode.getSetting((String)"swordTrail")).data);
        PlayerAnimationConfig.save(config);
        config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // On both sides: the integrated server runs beside the client proxy.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new net.gobbob.mobends.event.EventHandler_MobSwing());
        proxy.init();
    }

    static {
        refreshModel = 0;
    }
}
