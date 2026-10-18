package net.gobbob.mobends.data;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

class RemotePlayerLandingTest {
    @Test
    void landingTargetSurvivesInterruptedVanillaInterpolation() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        Data_Player data = allocate(Data_Player.class);
        data.ticksPerFrame = 1;
        double y = 0.663194;
        // Two ticks toward the floor, then the next jump replaces that target. Vanilla needs three ticks to arrive.
        double[] targets = {0, 0, 0.75, 0.75};
        for (int tick = 0; tick < targets.length; tick++) {
            y += (targets[tick] - y) / (tick % 2 == 0 ? 3 : 2);
            position(player, 0, y, 0);
            player.serverPosY = (int)(targets[tick] * 32);
            assertTrue(y > 1.0 / 16, "The original ground check misses every sample");
            boolean grounded = data.calcOnGround(player, false, box -> box.minY < 0);
            data.updateGround(grounded);
            assertEquals(tick < 2, data.isReceivedOnGround());
            assertEquals(tick >= 2, data.shouldAnimateAirborne(false, true));
        }
        assertEquals(2, data.ticksAfterLiftoff);
    }

    @Test
    void secondCaptureShowsATouchdownForEveryInferredBounce() throws Exception {
        Data_Player data = allocate(Data_Player.class);
        EntityData.BounceTracker tracker = new EntityData.BounceTracker();
        data.ticksPerFrame = 0.25f;
        int samples = 0, landings = 0;
        boolean previousGround = true;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            getClass().getResourceAsStream("remote-bunnyhop-2.csv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] fields = line.split(",");
                int tick = Integer.parseInt(fields[0]);
                double speed = Double.parseDouble(fields[1]);
                boolean ground = Boolean.parseBoolean(fields[2]);
                boolean bounce = tracker.sample(tick, speed, !ground);
                if (bounce) landings++;
                // The runtime retains bouncedOn(entityTick) across all render frames in the same tick.
                for (int frame = 0; frame < 4; frame++) {
                    data.updateGround(ground || bounce);
                    if (bounce) {
                        assertFalse(data.shouldAnimateAirborne(false, true), "Landing at " + tick + "/" + frame);
                        assertEquals((frame + 1) * 0.25f, data.ticksAfterTouchdown);
                    } else if (!ground && previousGround) {
                        assertTrue(data.shouldAnimateAirborne(false, true));
                        assertEquals((frame + 1) * 0.25f, data.ticksAfterLiftoff, "Next jump restarts once");
                    }
                }
                previousGround = ground || bounce;
                samples++;
            }
        }
        assertEquals(911, samples);
        assertEquals(68, landings);
    }

    @Test
    void receivedSupportUsesReceivedHorizontalPositionAndDoesNotAffectLocalPlayer() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        Data_Player data = allocate(Data_Player.class);
        position(player, 0, 0.5, 0);
        player.serverPosX = 4 * 32;
        assertFalse(data.calcOnGround(player, false, box -> box.minY < 0 && box.minX < 1 && box.maxX > -1));
        player.serverPosX = 0;
        assertTrue(data.calcOnGround(player, false, box -> box.minY < 0));
        assertFalse(data.calcOnGround(player, true, box -> box.minY < 0), "Local player must ignore serverPos");
        assertFalse(data.isReceivedOnGround());
    }

    @Test
    void existingGroundMarginsAndLocalLandingTimingArePreserved() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        Data_Player data = allocate(Data_Player.class);
        player.serverPosY = 10 * 32;
        position(player, 0, 0.04, 0);
        assertTrue(data.calcOnGround(player, false, box -> box.minY < 0));
        assertFalse(data.calcOnGround(player, true, box -> box.minY < 0));
        data.onGround = true;
        data.ticksAfterTouchdown = 0.25f;
        assertTrue(data.shouldAnimateAirborne(false, false));
        assertFalse(data.shouldAnimateAirborne(true, false));
        assertFalse(data.shouldAnimateAirborne(false, true));
        data.ticksAfterTouchdown = 2;
        assertFalse(data.shouldAnimateAirborne(false, false));
    }

    static void position(EntityOtherPlayerMP player, double x, double y, double z) throws Exception {
        player.posX = x;
        player.posY = y;
        player.posZ = z;
        Field bounds = net.minecraft.entity.Entity.class.getField("boundingBox");
        bounds.setAccessible(true);
        bounds.set(player, AxisAlignedBB.getBoundingBox(x - 0.3, y, z - 0.3, x + 0.3, y + 1.8, z + 0.3));
    }

    /** These tests supply collision boxes directly and need neither a running world nor an OpenGL context. */
    static <T> T allocate(Class<T> type) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return type.cast(((Unsafe)field.get(null)).allocateInstance(type));
    }
}
