package net.gobbob.mobends;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.io.File;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.CommonProxy;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.config.BlinkConfig;
import net.gobbob.mobends.pack.BendsPack;
import net.gobbob.mobends.settings.SettingsBoolean;
import net.gobbob.mobends.settings.SettingsNode;
import net.minecraftforge.common.config.Configuration;

@Mod(modid=MoBends.MODID, name=MoBends.MODNAME, version=MoBends.VERSION, dependencies="required-after:salamander;after:etfuturum")
public class MoBends {
    public static final String MODID = "mobends";
    public static final String MODNAME = "Even Mo' Bends";
    public static final String VERSION = Tags.VERSION;
    @SidedProxy(serverSide="net.gobbob.mobends.CommonProxy", clientSide="net.gobbob.mobends.client.ClientProxy")
    public static CommonProxy proxy;
    @Mod.Instance(value="mobends")
    public static MoBends instance;
    public static File configFile;
    public static int refreshModel;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        configFile = event.getSuggestedConfigurationFile();
        Configuration config = new Configuration(event.getSuggestedConfigurationFile());
        config.load();
        proxy.preInit(config);
        config.save();
    }

    public static void saveConfig() {
        Configuration config = new Configuration(configFile);
        config.load();
        for (int i = 0; i < AnimatedEntity.animatedEntities.length; ++i) {
            config.get("Animate", AnimatedEntity.animatedEntities[i].id, false).setValue(AnimatedEntity.animatedEntities[i].animate);
        }
        config.get("General", "Sword Trail", true).setValue(((SettingsBoolean)SettingsNode.getSetting((String)"swordTrail")).data);
        config.get("General", "Current Pack", true).setValue(BendsPack.currentPack);
        BlinkConfig.save(config);
        PlayerAnimationConfig.save(config);
        config.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    static {
        refreshModel = 0;
    }
}
