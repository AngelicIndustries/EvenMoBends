package net.gobbob.mobends.settings;

import net.gobbob.mobends.settings.SettingsBoolean;

public abstract class SettingsNode {
    public static SettingsNode[] settings = new SettingsNode[]{new SettingsBoolean("swordTrail", "Sword Trail").setupDefault(true)};
    public String id;
    public String displayName;

    public SettingsNode() {
        this.id = "NULL";
        this.displayName = "NULL";
    }

    public SettingsNode(String argID, String argDisplayName) {
        this.id = argID;
        this.displayName = argDisplayName;
    }

    public static SettingsNode getSetting(String argID) {
        for (int i = 0; i < settings.length; ++i) {
            if (!SettingsNode.settings[i].id.equalsIgnoreCase(argID)) continue;
            return settings[i];
        }
        return null;
    }
}

