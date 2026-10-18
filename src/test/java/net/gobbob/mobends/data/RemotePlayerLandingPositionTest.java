package net.gobbob.mobends.data;

import static org.junit.jupiter.api.Assertions.*;
import static net.gobbob.mobends.data.RemotePlayerLandingTest.allocate;
import static net.gobbob.mobends.data.RemotePlayerLandingTest.position;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.util.AxisAlignedBB;
import org.junit.jupiter.api.Test;

class RemotePlayerLandingPositionTest {
    @Test
    void latestCaptureReachesTheFloorBetweenEveryPairOfJumps() throws Exception {
        for (double floor : new double[] {63, 63.5}) {
            EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
            Data_Player data = allocate(Data_Player.class);
            RemotePlayerLanding landing = new RemotePlayerLanding();
            EntityData.BounceTracker bounce = new EntityData.BounceTracker();
            Function<AxisAlignedBB, List<AxisAlignedBB>> collisions = floor(floor);
            data.ticksPerFrame = 0.25f;
            int contacts = 0, samples = 0, syntheticBounces = 0;
            boolean previouslyGrounded = true;
            double previousRenderedY = Double.NaN;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream("remote-bunnyhop-3.csv"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("#")) continue;
                    String[] values = line.split(",");
                    int tick = Integer.parseInt(values[0]);
                    double y = Double.parseDouble(values[1]) + floor - 63;
                    double received = Double.parseDouble(values[2]) + floor - 63;
                    double dy = Double.parseDouble(values[3]);
                    position(player, 0, y, 0);
                    player.lastTickPosY = y - dy;
                    player.ticksExisted = tick;
                    player.serverPosY = (int)Math.round(received * 32);
                    landing.update(player, true, collisions);
                    boolean grounded = data.calcOnGround(player, false, box -> !collisions.apply(box).isEmpty())
                        || landing.isGrounded();
                    if (bounce.sample(tick, dy, !grounded)) syntheticBounces++;
                    if (grounded && !previouslyGrounded) {
                        contacts++;
                        assertTrue(dy < 0, "Touchdown must happen on descent, not halfway up the next hop");
                    }
                    if (!Double.isNaN(previousRenderedY)) {
                        assertEquals(previousRenderedY, drawnY(player, landing, 0), 0.000002, "Continuous tick boundary");
                    }
                    for (int frame = 1; frame <= 4; frame++) {
                        data.updateGround(grounded);
                        double partial = frame / 4.0;
                        assertTrue(drawnY(player, landing, (float)partial) >= floor - 0.000002, "Never below the floor");
                        assertEquals(!grounded, data.shouldAnimateAirborne(false, true));
                        // Repeated render and armor frames must not advance the correction a second time.
                        double offset = landing.renderOffset((float)partial);
                        landing.update(player, true, collisions);
                        assertEquals(offset, landing.renderOffset((float)partial));
                    }
                    if (landing.isGrounded() && previouslyGrounded) assertEquals(floor, drawnY(player, landing, 1), 0.000002);
                    if (!Double.isNaN(previousRenderedY)) {
                        assertTrue(previousRenderedY - drawnY(player, landing, 1) < 0.5,
                            "The old one-tick correction dropped about 0.93 block at touchdown");
                    }
                    assertEquals(y, player.posY, "Rendering must not move the entity");
                    assertEquals(y, player.boundingBox.minY, "Rendering must not move its hitbox");
                    previousRenderedY = drawnY(player, landing, 1);
                    previouslyGrounded = grounded;
                    samples++;
                }
            }
            assertEquals(185, samples);
            assertEquals(11, contacts);
            assertEquals(0, syntheticBounces, "All landings should precede the old late bounce detector");
        }
    }

    @Test
    void contactInterpolatesIntoTheFloorAndReleasesIntoTheNextJump() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        RemotePlayerLanding landing = new RemotePlayerLanding();
        position(player, 0, 63.649306, 0);
        player.lastTickPosY = 63.927084;
        player.serverPosY = (int)(63.09375 * 32);
        player.ticksExisted = 1;
        landing.update(player, true, floor(63));
        assertEquals(player.lastTickPosY, drawnY(player, landing, 0), 0.000001);
        double halfwayDown = (player.lastTickPosY + 63) / 2;
        assertEquals(halfwayDown, drawnY(player, landing, 1), 0.000001);
        assertEquals((player.lastTickPosY + halfwayDown) / 2, drawnY(player, landing, 0.5f), 0.000001);
        player.ticksExisted = 2;
        // First finish the second landing tick so its saved offset matches lastTickPosY.
        position(player, 0, 63.371528, 0);
        player.lastTickPosY = 63.649306;
        player.serverPosY = (int)(63.09375 * 32);
        landing.update(player, true, floor(63));
        assertEquals(halfwayDown, drawnY(player, landing, 0), 0.000001);
        assertEquals(63, drawnY(player, landing, 1), 0.000001);
        player.ticksExisted = 3;
        position(player, 0, 63.383102, 0);
        player.lastTickPosY = 63.371528;
        player.serverPosY = (int)(63.40625 * 32);
        landing.update(player, true, floor(63));
        assertFalse(landing.isGrounded());
        assertEquals(63, drawnY(player, landing, 0), 0.000001);
        assertEquals(player.posY, drawnY(player, landing, 1), 0.000001);
    }

    @Test
    void aDelayedPacketKeepsContactButHoveringAndWallsDoNotCreateIt() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        RemotePlayerLanding landing = new RemotePlayerLanding();
        position(player, 0, 63.09375, 0);
        player.lastTickPosY = player.posY;
        player.serverPosY = (int)(player.posY * 32);
        landing.update(player, true, floor(63));
        assertFalse(landing.isGrounded(), "Hovering above the existing ground margin is not a landing");
        player.ticksExisted++;
        player.lastTickPosY = 63.4;
        landing.update(player, true, floor(63));
        assertTrue(landing.isGrounded());
        player.ticksExisted++;
        player.lastTickPosY = player.posY;
        landing.update(player, true, floor(63));
        assertTrue(landing.isGrounded(), "Do not float up again while waiting for the next packet");
        landing = new RemotePlayerLanding();
        player.lastTickPosY = 63.4;
        landing.update(player, true, box -> Collections.singletonList(AxisAlignedBB.getBoundingBox(-1, 62, -1, 1, 65, 1)));
        assertFalse(landing.isGrounded(), "A wall beside the feet must not be used as a floor");
    }

    @Test
    void oneTickLandingPacketStillTouchesDownWithoutAnUpwardSnap() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        RemotePlayerLanding landing = new RemotePlayerLanding();
        double previousY = 63.66;
        double[] positions = {63.44, 63.54, 63.65, 63.82};
        double[] received = {63, 63.75, 63.75, 64.15625};
        double previousDrawnY = previousY;
        for (int tick = 0; tick < positions.length; tick++) {
            position(player, 0, positions[tick], 0);
            player.lastTickPosY = previousY;
            player.serverPosY = (int)(received[tick] * 32);
            player.ticksExisted = tick;
            landing.update(player, true, floor(63));
            assertEquals(previousDrawnY, drawnY(player, landing, 0), 0.000001);
            double shown = drawnY(player, landing, 1);
            assertTrue(shown - previousDrawnY <= 0.420001, "Release must not snap upward to the ahead-of-pose packet");
            assertTrue(previousDrawnY - shown <= 0.330001, "Spread the landing over two ticks");
            if (tick == 0) assertEquals(63.33, shown, 0.000001);
            if (tick == 1) assertEquals(63, shown, 0.000001, "Contact survives the next rising packet");
            if (tick == 3) assertEquals(player.posY, shown, 0.000001, "Correction releases completely");
            previousY = player.posY;
            previousDrawnY = shown;
        }
    }

    @Test
    void ledgesMissingGroundAndExcludedMovementDoNotLowerThePlayer() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        position(player, 0, 63.4, 0);
        player.lastTickPosY = 63.6;
        player.serverPosY = (int)(63.09375 * 32);
        RemotePlayerLanding landing = new RemotePlayerLanding();
        landing.update(player, true, box -> Collections.emptyList());
        assertFalse(landing.isGrounded());
        assertEquals(0, landing.renderOffset(1));
        player.serverPosX = 4 * 32;
        player.ticksExisted++;
        landing.update(player, true, collisions(AxisAlignedBB.getBoundingBox(3, 62, -1, 5, 63, 1)));
        assertFalse(landing.isGrounded(), "The received floor must also exist under the displayed player");
        player.serverPosX = 0;
        player.ticksExisted++;
        landing.update(player, true, floor(63));
        assertTrue(landing.isGrounded());
        player.ticksExisted++;
        landing.update(player, false, floor(63));
        assertFalse(landing.isGrounded(), "Flight, water, riding, sleeping and ladders disable correction");
        assertEquals(0, landing.renderOffset(0));
        assertEquals(0, landing.renderOffset(1));
    }

    @Test
    void teleportOrSkippedTicksDiscardThePreviousLandingOffset() throws Exception {
        EntityOtherPlayerMP player = allocate(EntityOtherPlayerMP.class);
        position(player, 0, 63.4, 0);
        player.lastTickPosY = 63.6;
        player.serverPosY = (int)(63.09375 * 32);
        RemotePlayerLanding landing = new RemotePlayerLanding();
        landing.update(player, true, floor(63));
        assertTrue(landing.isGrounded());
        player.ticksExisted++;
        position(player, 20, 63.4, 0);
        player.serverPosX = 20 * 32;
        landing.update(player, true, floor(63));
        assertFalse(landing.isGrounded());
        assertEquals(0, landing.renderOffset(0));
        assertEquals(0, landing.renderOffset(1));
        player.lastTickPosX = player.posX;
        player.ticksExisted++;
        landing.update(player, true, floor(63));
        assertTrue(landing.isGrounded());
        player.ticksExisted += 10;
        player.lastTickPosY = player.posY;
        position(player, 20, 64, 0);
        player.serverPosY = 64 * 32;
        landing.update(player, true, floor(63));
        assertEquals(0, landing.renderOffset(0));
        assertEquals(0, landing.renderOffset(1));
    }

    private static double drawnY(EntityOtherPlayerMP player, RemotePlayerLanding landing, float partial) {
        return player.lastTickPosY + (player.posY - player.lastTickPosY) * partial + landing.renderOffset(partial);
    }

    private static Function<AxisAlignedBB, List<AxisAlignedBB>> floor(double y) {
        return collisions(AxisAlignedBB.getBoundingBox(-100, y - 1, -100, 100, y, 100));
    }

    private static Function<AxisAlignedBB, List<AxisAlignedBB>> collisions(AxisAlignedBB... boxes) {
        return probe -> {
            java.util.ArrayList<AxisAlignedBB> found = new java.util.ArrayList<>();
            for (AxisAlignedBB box : boxes) if (box.intersectsWith(probe)) found.add(box);
            return found;
        };
    }
}
