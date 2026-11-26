package net.gobbob.mobends.client.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.DummyConfigElement;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.GuiConfigEntries;
import cpw.mods.fml.client.config.IConfigElement;
import net.gobbob.mobends.MoBends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

/**
 * Even Mo' Bends' settings in the mods list: a button into the animation menu, where the player's animations are
 * switched on and off too, then the General category of its config.
 */
public class MoBendsGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraft) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return ConfigGui.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class ConfigGui extends GuiConfig {

        public ConfigGui(GuiScreen parent) {
            super(parent, elements(), MoBends.MODID, false, false, MoBends.MODNAME);
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        private static List<IConfigElement> elements() {
            List<IConfigElement> elements = new ArrayList<IConfigElement>();
            elements.add(new DummyConfigElement.DummyCategoryElement("animations", "mobends.config.animations",
                AnimationsEntry.class));
            elements.add(new ConfigElement(MoBends.config.getCategory("general")));
            return elements;
        }
    }

    /** A category button that opens the animation menu instead of a page of settings. */
    public static class AnimationsEntry extends GuiConfigEntries.CategoryEntry {

        public AnimationsEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement<?> configElement) {
            super(owningScreen, owningEntryList, configElement);
        }

        @Override
        protected GuiScreen buildChildScreen() {
            return new GuiMBMenu(this.owningScreen);
        }
    }
}
