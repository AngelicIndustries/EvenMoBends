package net.gobbob.mobends.client.renderer.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/** A swimming model lying 1.3 blocks ahead of the feet and 0.7 behind, as the strokes do, against walls. */
class SwimRoomTest {
    private static final float FRONT = 1.3f, BACK = 0.7f;

    @Test
    void openWaterLeavesTheModelWhereItIs() {
        assertEquals(0, SwimRoom.shift(FRONT, BACK, 2, 2), 1e-6);
        assertTrue(SwimRoom.lacking(FRONT, BACK, 2, 2) < SwimRoom.RESUME);
    }

    @Test
    void aWallAheadDrawsTheBodyBackIntoTheWaterBehind() {
        // Touching the wall: the hitbox keeps the feet 0.3 from it.
        assertEquals(1.0f, SwimRoom.shift(FRONT, BACK, 0.3f, 2), 1e-6);
        assertTrue(SwimRoom.lacking(FRONT, BACK, 0.3f, 2) < SwimRoom.RESUME);
        // Some way off, only the overlap.
        assertEquals(0.3f, SwimRoom.shift(FRONT, BACK, 1.0f, 2), 1e-6);
    }

    @Test
    void aDeadEndTooShortToLieInStaysUpright() {
        // A one-block nook: walls 0.3 ahead and 0.7 behind.
        assertEquals(0, SwimRoom.shift(FRONT, BACK, 0.3f, 0.7f), 1e-6);
        assertTrue(SwimRoom.lacking(FRONT, BACK, 0.3f, 0.7f) > SwimRoom.GIVE_UP);
        // Upright, the tread's smaller reach still moves back what room there is.
        assertEquals(0.2f, SwimRoom.shift(0.5f, 0.3f, 0.3f, 0.7f), 1e-6);
    }

    @Test
    void aBlockBehindIsNeverPushedInto() {
        assertEquals(0, SwimRoom.shift(FRONT, BACK, 0.3f, 0.5f), 1e-6);
    }
}
