package net.gobbob.mobends.client.renderer.entity;

import net.gobbob.mobends.data.Data_Player;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Keeps a swimming player's model out of the blocks ahead and behind. Swimming, the game keeps the standing hitbox,
 * while the model lies along the facing and reaches well past it, so against a wall it would sink in. It is drawn
 * back as far as the water behind allows; where even that is not room enough, the swim stays upright.
 */
final class SwimRoom {
    /** The body's band above the feet that must stay out of blocks, in blocks. */
    private static final double LOW = 0.2, HIGH = 1.0;
    /** Free space is sampled this far, in steps this long, with a probe this wide (blocks). */
    private static final float MAX = 2.0f, STEP = 1.0f / 32.0f, HALF_WIDTH = 1.0f / 64.0f;
    /** Lying down lacking this much room makes the swim upright; lacking only this little lets it lie down again. */
    static final float GIVE_UP = 0.2f, RESUME = 0.05f;
    /** The shift eases in about this many ticks. */
    private static final float EASE_TICKS = 2.0f;

    private SwimRoom() {}

    /** Per frame, before the player's model is drawn: eases {@link Data_Player#swimShift} and decides upright. */
    static void update(EntityPlayer player, Data_Player data, float bodyYaw, float partialTicks) {
        float target = 0.0f;
        if (data.swimming) {
            double x = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
            double y = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks - player.yOffset;
            double z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
            float yaw = bodyYaw * (float)Math.PI / 180.0f;
            double dx = -MathHelper.sin(yaw), dz = MathHelper.cos(yaw);
            float ahead = free(player.worldObj, x, y, z, dx, dz);
            float behind = free(player.worldObj, x, y, z, -dx, -dz);
            target = shift(data.reachFront, data.reachBack, ahead, behind);
            float lacking = lacking(data.strokeFront, data.strokeBack, ahead, behind);
            if (lacking > GIVE_UP) data.swimBoxedIn = true;
            else if (lacking < RESUME) data.swimBoxedIn = false;
        } else {
            data.swimBoxedIn = false;
        }
        float ease = 1.0f - (float)Math.exp(-Math.max(0.0f, data.ticksPerFrame) / EASE_TICKS);
        data.swimShift += (target - data.swimShift) * ease;
    }

    /** How far back to draw a model reaching {@code front} and {@code back} with this much free space ahead and behind. */
    static float shift(float front, float back, float ahead, float behind) {
        return Math.max(0.0f, Math.min(front - ahead, behind - back));
    }

    /** How much room lying in the stroke lacks, drawn back as far as it can be. */
    static float lacking(float strokeFront, float strokeBack, float ahead, float behind) {
        return strokeFront - ahead - Math.max(0.0f, behind - strokeBack);
    }

    /** How far the body's band can go from the feet along (dx, dz) before it meets a block's collision box. */
    private static float free(World world, double x, double y, double z, double dx, double dz) {
        for (float d = 0.0f; d < MAX; d += STEP) {
            double px = x + dx * d, pz = z + dz * d;
            AxisAlignedBB probe = AxisAlignedBB.getBoundingBox(px - HALF_WIDTH, y + LOW, pz - HALF_WIDTH,
                px + HALF_WIDTH, y + HIGH, pz + HALF_WIDTH);
            if (!world.func_147461_a(probe).isEmpty()) return d;
        }
        return MAX;
    }
}
