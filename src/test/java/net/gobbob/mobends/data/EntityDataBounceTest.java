package net.gobbob.mobends.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class EntityDataBounceTest {
    @Test
    void capturedBunnyHopsRestartTheJumpClockEveryCycle() throws Exception {
        EntityData.BounceTracker tracker = new EntityData.BounceTracker();
        int samples = 0, bounces = 0, airborneTicks = 0, longestJump = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
            getClass().getResourceAsStream("remote-bunnyhop.csv"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) continue;
                String[] fields = line.split(",");
                int tick = Integer.parseInt(fields[0]);
                double speed = Double.parseDouble(fields[1]);
                boolean grounded = Boolean.parseBoolean(fields[2]);
                boolean bounced = tracker.sample(tick, speed, !grounded);
                if (bounced) bounces++;
                if (grounded || bounced) airborneTicks = 0;
                if (!grounded) airborneTicks++;
                longestJump = Math.max(longestJump, airborneTicks);
                // Multiple render frames must not report a second landing for the same client tick.
                assertFalse(tracker.sample(tick, speed, !grounded));
                assertFalse(tracker.sample(tick, speed, !grounded));
                samples++;
            }
        }
        assertEquals(1200, samples);
        assertEquals(95, bounces, "The original adjacent-sample check detected only two bounces in this capture");
        assertEquals(14, longestJump, "The original jump clock ran to 589 ticks and held the last keyframe");
    }

    @Test
    void remembersDescentAcrossSmallPositiveAndZeroSpeeds() {
        EntityData.BounceTracker tracker = new EntityData.BounceTracker();
        assertFalse(tracker.sample(1, -0.277778, true));
        assertFalse(tracker.sample(2, 0.011574, true));
        assertFalse(tracker.sample(3, 0, true));
        assertFalse(tracker.sample(4, 0.011574, true));
        assertTrue(tracker.sample(5, 0.201775, true));
        assertFalse(tracker.sample(6, 0.201775, true));
    }

    @Test
    void apexAndSmallPositionJitterDoNotCountAsLandings() {
        EntityData.BounceTracker tracker = new EntityData.BounceTracker();
        double[] speeds = {0.2, 0.14, 0.01, 0, -0.01, 0.01, 0, -0.14, -0.27};
        for (int tick = 0; tick < speeds.length; tick++) assertFalse(tracker.sample(tick, speeds[tick], true));
        tracker = new EntityData.BounceTracker();
        for (int tick = 0; tick < 100; tick++) assertFalse(tracker.sample(tick, tick % 2 == 0 ? -0.01 : 0.01, true));
    }

    @Test
    void groundWaterLadderRidingOrLocalPlayerClearPendingDescent() {
        EntityData.BounceTracker tracker = new EntityData.BounceTracker();
        assertFalse(tracker.sample(1, -0.2, true));
        assertFalse(tracker.sample(2, 0, false));
        assertFalse(tracker.sample(3, 0.2, true));
        assertFalse(tracker.sample(4, -0.2, false));
        assertFalse(tracker.sample(5, 0.2, true));
    }
}
