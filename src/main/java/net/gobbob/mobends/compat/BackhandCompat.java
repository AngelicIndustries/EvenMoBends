package net.gobbob.mobends.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import xonin.backhand.api.core.BackhandUtils;

/** Backhand's off hand. Without Backhand the off hand is always empty. */
public final class BackhandCompat {
    private BackhandCompat() {}

    /** The item in the player's off hand, or null. */
    public static ItemStack getOffhandItem(EntityPlayer player) {
        if (player == null || !CompatibilityPolicy.isBackhandLoaded()) return null;
        return Api.getOffhandItem(player);
    }

    public static boolean isOffhandHolding(EntityPlayer player) {
        return getOffhandItem(player) != null;
    }

    /** Only loaded with Backhand present. */
    private static final class Api {
        static ItemStack getOffhandItem(EntityPlayer player) {
            return BackhandUtils.getOffhandItem(player);
        }
    }
}
