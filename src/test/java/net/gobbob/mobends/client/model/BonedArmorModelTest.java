package net.gobbob.mobends.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class BonedArmorModelTest {

    /** A vanilla arm box runs from 2 above the shoulder pivot to 10 below; the elbow is at 4. */
    private static final float ELBOW = 4.0f;

    @Test
    void onlyLongPiecesAcrossTheJointAreCut() {
        assertEquals(BonedArmorModel.Share.CUT, BonedArmorModel.share(-2.0f, 10.0f, ELBOW));
        assertEquals(BonedArmorModel.Share.UPPER, BonedArmorModel.share(-3.0f, 1.0f, ELBOW));
        assertEquals(BonedArmorModel.Share.LOWER, BonedArmorModel.share(6.0f, 10.0f, ELBOW));
    }

    @Test
    void piecesBarelyAcrossTheJointRideTheBoneTheyMostlyLieOn() {
        // A pauldron reaching a pixel past the elbow, and a cuff starting a pixel above it.
        assertEquals(BonedArmorModel.Share.UPPER, BonedArmorModel.share(-3.0f, 5.0f, ELBOW));
        assertEquals(BonedArmorModel.Share.LOWER, BonedArmorModel.share(3.0f, 10.0f, ELBOW));
        // A short branch across the joint goes whole with its middle.
        assertEquals(BonedArmorModel.Share.UPPER, BonedArmorModel.share(1.0f, 5.0f, ELBOW));
        assertEquals(BonedArmorModel.Share.LOWER, BonedArmorModel.share(3.5f, 7.5f, ELBOW));
    }

    @Test
    void theCutKeepsEachSideOfTheJointInThePiecesOwnFrame() {
        // The joint plane y = 0.25 (blocks), facing down the limb; the piece is drawn half a block lower.
        Vector3f down = new Vector3f(0.0f, 1.0f, 0.0f);
        Matrix4f frame = new Matrix4f().translate(0.0f, 0.5f, 0.0f);
        BonedArmorModel.Cut upper = BonedArmorModel.Cut.of(frame, down, 0.25f, -1.0f);
        BonedArmorModel.Cut lower = BonedArmorModel.Cut.of(frame, down, 0.25f, 1.0f);
        // The piece's own y = -0.25 lies on the plane; above it is the upper bone's, below the lower one's.
        assertEquals(0.0f, upper.distance(0.0f, -0.25f, 0.0f), 1.0e-6f);
        assertEquals(0.1f, upper.distance(0.0f, -0.35f, 0.0f), 1.0e-6f);
        assertEquals(-0.1f, lower.distance(0.0f, -0.35f, 0.0f), 1.0e-6f);
        assertEquals(0.1f, lower.distance(0.0f, -0.15f, 0.0f), 1.0e-6f);
        // Moving into a frame 0.1 lower moves the plane up by as much there.
        assertEquals(0.0f, upper.moved(0.0f, 0.1f, 0.0f).distance(0.0f, -0.35f, 0.0f), 1.0e-6f);
    }

    @Test
    void theCutFollowsAScaledAndMirroredFrame() {
        // Like a bone frame: pixels to blocks, mirrored on x and y.
        Vector3f down = new Vector3f(0.0f, 1.0f, 0.0f);
        Matrix4f frame = new Matrix4f().scale(-0.0625f, -0.0625f, 0.0625f);
        BonedArmorModel.Cut upper = BonedArmorModel.Cut.of(frame, down, 0.0f, -1.0f);
        // y is flipped: the piece's +y lies above the plane (model y < 0), on the upper side.
        assertEquals(true, upper.distance(0.0f, 4.0f, 0.0f) > 0.0f);
        assertEquals(true, upper.distance(0.0f, -4.0f, 0.0f) < 0.0f);
    }
}
