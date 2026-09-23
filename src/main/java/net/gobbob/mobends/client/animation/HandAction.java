package net.gobbob.mobends.client.animation;

import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import java.util.Map;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

/**
 * What a player's swing is for: breaking a block, using the item (e.g. placing a block) or attacking. Breaking is
 * known for everyone, from the block damage the world shows; using versus attacking only for the local player, from
 * the key that started it (other players' swings don't say).
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

    /** Whether we know what the player's swing was for: only for the local player. */
    public static boolean isKnown(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return minecraft != null && player != null && player == minecraft.thePlayer;
    }

    /** Whether the local player's swing is a use (e.g. placing a block) rather than an attack. */
    public static boolean isUse(EntityPlayer player) {
        Minecraft minecraft = Minecraft.getMinecraft();
        return isKnown(player) && minecraft.gameSettings.keyBindUseItem.getIsKeyPressed()
            && !minecraft.gameSettings.keyBindAttack.getIsKeyPressed();
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
