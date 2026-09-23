package net.gobbob.mobends.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import xonin.backhand.api.core.BackhandUtils;
import xonin.backhand.api.core.IBackhandPlayer;

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

    /** Whether the player is drawing a bow held in the off hand. */
    public static boolean isDrawingOffhandBow(EntityPlayer player) {
        if (player == null || !CompatibilityPolicy.isBackhandLoaded() || player.getItemInUseCount() <= 0) return false;
        ItemStack offhand = getOffhandItem(player);
        return offhand != null && offhand.getItemUseAction() == EnumAction.bow && Api.isOffhandItemInUse(player);
    }

    /** Whether Backhand counts the item as a tool (what it breaks blocks with from the off hand). */
    public static boolean isTool(ItemStack stack) {
        return stack != null && CompatibilityPolicy.isBackhandLoaded() && Api.isTool(stack);
    }

    /** How far through its swing the off hand is, 0 when it isn't swinging. */
    public static float getOffSwingProgress(EntityPlayer player, float partialTicks) {
        if (player == null || !CompatibilityPolicy.isBackhandLoaded()) return 0;
        return Api.getOffSwingProgress(player, partialTicks);
    }

    /** Only loaded with Backhand present. */
    private static final class Api {
        static ItemStack getOffhandItem(EntityPlayer player) {
            return BackhandUtils.getOffhandItem(player);
        }

        static boolean isTool(ItemStack stack) {
            return BackhandUtils.isItemTool(stack.getItem());
        }

        static boolean isOffhandItemInUse(EntityPlayer player) {
            return player instanceof IBackhandPlayer && ((IBackhandPlayer)player).isOffhandItemInUse();
        }

        static float getOffSwingProgress(EntityPlayer player, float partialTicks) {
            return player instanceof IBackhandPlayer ? ((IBackhandPlayer)player).getOffSwingProgress(partialTicks) : 0;
        }
    }
}
