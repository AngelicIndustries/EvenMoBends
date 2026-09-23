package net.gobbob.mobends.client.animation;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import java.util.Map;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.gobbob.mobends.compat.BackhandCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * What a player's swing is for: breaking a block, using the item (e.g. placing a block) or attacking. Breaking is
 * known for everyone, from the block damage the world shows; using versus attacking only for the local player, from
 * the right clicks Forge reports (other players' swings don't say).
 */
public final class HandAction {
    private static Field damagedBlocks;
    private static boolean failed;

    private HandAction() {}

    /** Whether the player is breaking a block (or, the local player, hitting one, which covers instant breaks). */
    public static boolean isBreakingBlock(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (player == null || minecraft == null) return false;
        if (player == minecraft.thePlayer && minecraft.gameSettings.keyBindAttack.getIsKeyPressed()
            && minecraft.objectMouseOver != null && minecraft.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return true;
        }
        Map<?, ?> damaged = damagedBlocks(minecraft.renderGlobal);
        return damaged != null && damaged.containsKey(player.getEntityId());
    }

    /**
     * Whether the player is breaking a block with Backhand's off hand: the local player holding right click on a
     * block with a tool there (as Backhand breaks), or anyone breaking while the main hand isn't swinging.
     */
    public static boolean isBreakingWithOffhand(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (player == null || minecraft == null) return false;
        ItemStack offhand = BackhandCompat.getOffhandItem(player);
        if (player == minecraft.thePlayer && offhand != null && BackhandCompat.isTool(offhand)
            && minecraft.gameSettings.keyBindUseItem.getIsKeyPressed() && minecraft.objectMouseOver != null
            && minecraft.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            return true;
        }
        Map<?, ?> damaged = damagedBlocks(minecraft.renderGlobal);
        return damaged != null && damaged.containsKey(player.getEntityId()) && player.swingProgress <= 0;
    }

    /** Whether we know what the player's swing was for: only for the local player. */
    public static boolean isKnown(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && player != null && player == minecraft.thePlayer;
    }

    /** The local player's age when a right click last used an item or placed a block. */
    private static int lastUseTick = Integer.MIN_VALUE / 2;

    /**
     * Whether the local player's swing is a use (e.g. placing a block) rather than an attack: a right click acted
     * within the last couple of ticks. (The use key alone can be pressed and released within one tick.)
     */
    public static boolean isUse(EntityPlayer player) {
        if (!isKnown(player)) return false;
        int since = player.ticksExisted - lastUseTick;
        return since >= 0 && since <= 2;
    }

    /** Notes the local player's right clicks, which Forge reports on the client as they act. */
    public static final class Listener {
        @SubscribeEvent
        public void onInteract(PlayerInteractEvent event) {
            if (event.entityPlayer != null && event.entityPlayer.worldObj.isRemote
                && event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) {
                lastUseTick = event.entityPlayer.ticksExisted;
            }
        }
    }

    private static Map<?, ?> damagedBlocks(RenderGlobal renderGlobal) {
        if (renderGlobal == null || failed) return null;
        try {
            if (damagedBlocks == null) damagedBlocks = ReflectionHelper.findField(RenderGlobal.class, "field_72738_E", "damagedBlocks");
            return (Map<?, ?>)damagedBlocks.get(renderGlobal);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            BendsLogger.log("Could not read which blocks are being broken: " + e, BendsLogger.ERROR);
            return null;
        }
    }
}
