package net.gobbob.mobends.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

import cpw.mods.fml.relauncher.FMLLaunchHandler;

/** Mixins into other mods, chosen by which of them are loaded. */
@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.mobends.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        List<String> mixins = new ArrayList<>();
        // Et Futurum draws worn lanterns through Botania too: Mo' Bends draws them instead
        if (FMLLaunchHandler.side().isClient() && loadedMods.contains("etfuturum") && loadedMods.contains("Botania")) {
            mixins.add("etfuturum.MixinItemBlockLanternBotania");
        }
        return mixins;
    }
}
