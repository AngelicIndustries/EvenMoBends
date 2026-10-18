package net.gobbob.mobends.data;

import java.util.List;
import java.util.function.Function;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.util.AxisAlignedBB;

/** Restores floor contact which remote position interpolation can cut out between consecutive jumps. */
final class RemotePlayerLanding {
    private static final double GROUND_MARGIN = 1.0 / 16.0;
    // The final descending packet of a normal jump can be 3/32 block above the floor; the next is already rising.
    private static final double LANDING_MARGIN = 1.0 / 8.0;
    private int sampleTick = Integer.MIN_VALUE;
    private int receivedY;
    private double previousOffset, offset;
    private double pendingFloor = Double.NaN;
    private boolean grounded;

    void update(EntityOtherPlayerMP player, boolean eligible, Function<AxisAlignedBB, List<AxisAlignedBB>> collisions) {
        if (!eligible) {
            this.previousOffset = this.offset = 0;
            this.pendingFloor = Double.NaN;
            this.grounded = false;
            this.sampleTick = player.ticksExisted;
            return;
        }
        if (this.sampleTick == player.ticksExisted) return;
        boolean consecutive = this.sampleTick == player.ticksExisted - 1;
        boolean wasGrounded = consecutive && this.grounded;
        double finishingFloor = consecutive ? this.pendingFloor : Double.NaN;
        this.pendingFloor = Double.NaN;
        boolean heldLanding = this.grounded && this.receivedY == player.serverPosY;
        this.receivedY = player.serverPosY;
        this.previousOffset = consecutive ? this.offset : 0;
        this.sampleTick = player.ticksExisted;
        this.offset = 0;
        this.grounded = false;
        // A teleport must not carry the previous landing's render offset to its destination.
        if (Math.abs(player.posY - player.lastTickPosY) > 1
            || Math.abs(player.posX - player.lastTickPosX) > 1
            || Math.abs(player.posZ - player.lastTickPosZ) > 1) {
            this.previousOffset = 0;
            return;
        }
        double previousFeet = player.boundingBox.minY - (player.posY - player.lastTickPosY) + this.previousOffset;
        // A short landing packet can put the next jump well ahead of the drawn feet. Release the correction at
        // no more than a normal jump's initial rise, rather than replacing a fast stomp with a fast upward snap.
        if (this.previousOffset < 0) this.offset = Math.min(0, previousFeet + 0.42 - player.boundingBox.minY);
        AxisAlignedBB received = player.boundingBox.getOffsetBoundingBox(
            player.serverPosX / 32.0 - player.posX, player.serverPosY / 32.0 - player.posY,
            player.serverPosZ / 32.0 - player.posZ);
        boolean descending = player.posY - player.lastTickPosY < -0.02;
        double floor = support(received, descending || heldLanding ? LANDING_MARGIN : GROUND_MARGIN, collisions);
        if (Double.isNaN(floor) && !Double.isNaN(finishingFloor)) {
            // Finish the second half of touchdown even if the next rising packet has already arrived. Both X/Z
            // footprints must still have the same support; a departed ledge must not pull the player down.
            double support = support(received.getOffsetBoundingBox(0, finishingFloor - received.minY, 0),
                GROUND_MARGIN, collisions);
            if (Math.abs(support - finishingFloor) < 0.001) floor = finishingFloor;
        }
        if (Double.isNaN(floor) || Math.abs(player.boundingBox.minY - floor) > 1) return;
        // The received X/Z can already be over a ledge. Lower the displayed player only if the same surface also
        // supports its displayed X/Z. Use collision tops, not full-block Y, so slabs and stairs retain their height.
        AxisAlignedBB displayedAtFloor = player.boundingBox.getOffsetBoundingBox(0, floor - player.boundingBox.minY, 0);
        double displayedFloor = support(displayedAtFloor, GROUND_MARGIN, collisions);
        if (Double.isNaN(displayedFloor) || Math.abs(displayedFloor - floor) > 0.001) return;
        this.grounded = true;
        if (!wasGrounded && previousFeet - floor > GROUND_MARGIN) {
            // Spend two ticks descending to the surface. The previous version removed all interpolation lag in
            // one tick (about 0.93 block in the capture), which looked like an abrupt stomp.
            this.offset = (previousFeet + floor) * 0.5 - player.boundingBox.minY;
            this.pendingFloor = floor;
        } else {
            this.offset = floor - player.boundingBox.minY;
        }
    }

    boolean isGrounded() {
        return this.grounded;
    }

    /** Interpolate the correction with the same partial tick as the entity's rendered position, including release. */
    double renderOffset(float partialTicks) {
        double partial = Math.max(0, Math.min(1, partialTicks));
        return this.previousOffset + (this.offset - this.previousOffset) * partial;
    }

    private static double support(AxisAlignedBB bounds, double below,
        Function<AxisAlignedBB, List<AxisAlignedBB>> collisions) {
        AxisAlignedBB probe = AxisAlignedBB.getBoundingBox(bounds.minX, bounds.minY - below, bounds.minZ,
            bounds.maxX, bounds.minY + GROUND_MARGIN, bounds.maxZ);
        double floor = Double.NaN;
        for (AxisAlignedBB box : collisions.apply(probe)) {
            if (box.maxY > bounds.minY + GROUND_MARGIN || box.maxY < bounds.minY - below
                || box.maxX <= bounds.minX || box.minX >= bounds.maxX || box.maxZ <= bounds.minZ || box.minZ >= bounds.maxZ) continue;
            if (Double.isNaN(floor) || box.maxY > floor) floor = box.maxY;
        }
        return floor;
    }
}
