package net.gobbob.mobends.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.MovementInput;
import net.minecraft.world.World;

import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.Data_Spider;
import net.gobbob.mobends.data.Data_Zombie;
import net.gobbob.mobends.data.EntityData;

/**
 * Draws the menu's previews with or without a world. Without one, as when the menu opens from the mods list, the mob
 * goes into {@link PreviewWorld}, and a stand-in for the player with the session's profile is the viewer, as renderers
 * expect a player to be there. The stand-in is also the player the menu shows, in a world too.
 */
final class MenuPreview {

    private static EntityClientPlayerMP standIn;

    private MenuPreview() {}

    /** The menu's player, whose animations it plays: always the stand-in, never the one in the world. */
    static EntityLivingBase showcasePlayer() {
        return standIn(Minecraft.getMinecraft());
    }

    private static EntityClientPlayerMP standIn(Minecraft minecraft) {
        if (standIn == null) {
            standIn = new EntityClientPlayerMP(minecraft, PreviewWorld.INSTANCE, minecraft.getSession(), null,
                new StatFileWriter());
            // Drawn as the player too: the renderer asks it whether it sneaks, which reads its movement input.
            standIn.movementInput = new MovementInput();
        }
        return standIn;
    }

    /**
     * A preview is outside the world, whose updates never reach its animation data: its model would pose it once a
     * tick, or once ever without a world. Let it pose every frame, so animations play smoothly.
     */
    private static void poseEveryFrame(EntityLivingBase entity) {
        if (EntityData.inWorld(entity)) return;
        int id = entity.getEntityId();
        EntityData data = entity instanceof EntityPlayer ? Data_Player.get(id)
            : entity instanceof EntityZombie ? Data_Zombie.get(id) : entity instanceof EntitySpider ? Data_Spider.get(id)
            : null;
        if (data != null) data.updatedThisFrame = false;
    }

    /** Runs {@code draw} with {@code entity} in a world and a player to view it. */
    static void draw(EntityLivingBase entity, Runnable draw) {
        Minecraft minecraft = Minecraft.getMinecraft();
        poseEveryFrame(entity);
        if (minecraft.theWorld != null) {
            if (entity.worldObj == null) entity.worldObj = minecraft.theWorld;
            draw.run();
            return;
        }
        RenderManager manager = RenderManager.instance;
        World world = manager.worldObj;
        EntityClientPlayerMP player = minecraft.thePlayer;
        EntityLivingBase view = minecraft.renderViewEntity;
        EntityLivingBase living = manager.livingPlayer;
        net.minecraft.entity.Entity viewing = manager.field_147941_i;
        World entityWorld = entity.worldObj;
        EntityClientPlayerMP standIn = standIn(minecraft);
        try {
            entity.worldObj = PreviewWorld.INSTANCE;
            minecraft.thePlayer = standIn;
            minecraft.renderViewEntity = standIn;
            manager.cacheActiveRenderInfo(PreviewWorld.INSTANCE, minecraft.getTextureManager(), minecraft.fontRenderer,
                standIn, standIn, minecraft.gameSettings, 1.0f);
            draw.run();
        } finally {
            entity.worldObj = entityWorld;
            minecraft.thePlayer = player;
            minecraft.renderViewEntity = view;
            manager.worldObj = world;
            manager.livingPlayer = living;
            manager.field_147941_i = viewing;
        }
    }
}
