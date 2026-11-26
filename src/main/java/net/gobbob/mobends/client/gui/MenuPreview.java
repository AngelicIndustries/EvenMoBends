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
     * How fast the stand-in seems to move as its animation plays, in blocks a tick: along where it faces, and up
     * (falling is negative). For its cape; see {@link #trailCape}.
     */
    private static float forward, up;
    private static final long START = System.nanoTime();

    /** Vanilla's speeds for the animation the stand-in plays, by its name; still for anything not moving. */
    static void motion(String animation) {
        String name = animation == null ? "" : animation.substring(animation.lastIndexOf('.') + 1);
        float[] speed = SPEEDS.get(name);
        forward = speed == null ? 0.0f : speed[0];
        up = speed == null ? 0.0f : speed[1];
    }

    private static final java.util.Map<String, float[]> SPEEDS = new java.util.HashMap<String, float[]>();
    static {
        // Blocks a tick, as a player moves while vanilla plays these: walking 4.3 m/s, sprinting 5.6, sneaking 1.3,
        // swimming 2.2, flying 10.9, climbing a ladder 2.4 up, falling well along, riding a horse at a trot.
        Object[][] speeds = {
            {"walk", 0.216f, 0.0f}, {"walk_swing", 0.216f, 0.0f}, {"sprint", 0.28f, 0.0f}, {"sprint_swing", 0.28f, 0.0f},
            {"sneak_walk", 0.065f, 0.0f}, {"sneak_walk_swing", 0.065f, 0.0f}, {"swim", 0.11f, 0.0f},
            {"swim_legs", 0.11f, 0.0f}, {"dive", 0.11f, 0.0f}, {"fly_forward", 0.55f, 0.0f},
            {"fly_elytra_forward", 0.8f, 0.0f}, {"climb_move", 0.0f, 0.118f}, {"jump", 0.1f, 0.2f}, {"fall", 0.0f, -0.6f},
            {"ride_move", 0.3f, 0.0f}, {"sword_1_moving", 0.216f, 0.0f}, {"sword_2_moving", 0.216f, 0.0f},
            {"sword_stance_sprint", 0.28f, 0.0f}};
        for (Object[] speed : speeds) SPEEDS.put((String)speed[0], new float[] {(Float)speed[1], (Float)speed[2]});
    }

    /**
     * The stand-in never ticks, so what a cape reads to swing stays where it began: give it what a player moving at
     * the animation's speed would have. Vanilla's chase point closes a quarter of its gap a tick, so at a steady speed
     * it trails three ticks' travel behind; the stride runs on in real time, and the view bobs as walking makes it.
     * Capes and elytra drawn by Mo' Bends, vanilla, Salamander or Et Futurum all read these.
     */
    private static void trailCape(EntityClientPlayerMP player) {
        double ticks = (System.nanoTime() - START) / 5.0e7;
        player.prevPosX = player.posX;
        player.prevPosY = player.posY;
        player.prevPosZ = player.posZ;
        // The menu draws it facing south (+Z): the chase point trails north of it, and above it while it falls.
        player.field_71091_bM = player.field_71094_bP = player.posX;
        player.field_71096_bN = player.field_71095_bQ = player.posY - up * 3.0;
        player.field_71097_bO = player.field_71085_bR = player.posZ - forward * 3.0;
        player.prevCameraYaw = player.cameraYaw = Math.min(0.1f, forward);
        player.prevDistanceWalkedModified = player.distanceWalkedModified = (float)(ticks * forward * 0.6);
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
        if (entity == standIn) trailCape(standIn);
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
