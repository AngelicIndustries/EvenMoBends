package net.gobbob.mobends.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EntityDataClockTest {
    @Test
    void respawningKeepsTheClockRunningForward() {
        EntityData.Clock clock = new EntityData.Clock();
        // The first read counts from 0, as the player's age did.
        assertEquals(5000.0f, clock.advance(5000.0));
        assertEquals(0.5f, clock.advance(5000.5));
        // A respawn: the new player's age starts over.
        assertEquals(0.0f, clock.advance(0.0));
        assertEquals(5000.5f, clock.ticks());
        assertEquals(1.25f, clock.advance(1.25));
        assertEquals(5001.75f, clock.ticks());
    }
}
