package net.gobbob.mobends.client.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
