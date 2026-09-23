package net.gobbob.mobends.event;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import java.io.IOException;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.gui.GuiMBMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

public class EventHandler_Keyboard {
    public static final KeyBinding key_Menu = new KeyBinding("Mo'Bends Menu", 34, "GobBob's Mods");

    @SubscribeEvent
    public void onKeyPressed(InputEvent.KeyInputEvent event) throws IOException {
        if (key_Menu.getIsKeyPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiMBMenu());
            ++MoBends.refreshModel;
        }
    }
}

