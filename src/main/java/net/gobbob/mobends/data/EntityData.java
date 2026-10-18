package net.gobbob.mobends.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.AxisAlignedBB;
import java.util.function.Predicate;
import org.lwjgl.util.vector.Vector3f;

public class EntityData {
    public int entityID;
    public String entityType;
    public ModelBase model;
    public Vector3f position = new Vector3f();
    public Vector3f motion_prev = new Vector3f();
    public Vector3f motion = new Vector3f();
    public float ticks = 0.0f;
    public float ticksPerFrame = 0.0f;
    public float lastTicks = 0.0f;
    public float lastTicksPerFrame = 0.0f;
    public boolean updatedThisFrame = false;
    public float ticksAfterLiftoff = 0.0f;
    public float ticksAfterTouchdown = 0.0f;
    public float ticksAfterPunch = 0.0f;
    public boolean alreadyPunched = false;
    public boolean onGround;

    private java.util.Map<String, String> slotAnimations;
    private java.util.Map<String, float[]> slotTicks;
    /**
     * Ticks since {@code animation} started playing in {@code slot}. It restarts when the slot switches animation,
     * or when the slot wasn't played for a moment.
     */
    public float ticksPlaying(String slot, String animation) {
        if (this.slotAnimations == null) {
            this.slotAnimations = new java.util.HashMap<String, String>();
            this.slotTicks = new java.util.HashMap<String, float[]>();
        }
        float[] startAndLast = this.slotTicks.get(slot);
        if (startAndLast == null || !animation.equals(this.slotAnimations.get(slot)) || this.ticks - startAndLast[1] > 2.0f) {
            startAndLast = new float[] {this.ticks, this.ticks};
            this.slotTicks.put(slot, startAndLast);
            this.slotAnimations.put(slot, animation);
        }
        startAndLast[1] = this.ticks;
        return this.ticks - startAndLast[0];
    }

    private java.util.Map<String, net.gobbob.mobends.client.animation.Inertia> slotInertia;

    /** The inertialization state of {@code slot}. */
    public net.gobbob.mobends.client.animation.Inertia inertia(String slot) {
        if (this.slotInertia == null) this.slotInertia = new java.util.HashMap<String, net.gobbob.mobends.client.animation.Inertia>();
        return this.slotInertia.computeIfAbsent(slot, s -> new net.gobbob.mobends.client.animation.Inertia());
    }

    public EntityData(int argEntityID) {
        this.entityID = argEntityID;
        this.entityType = Minecraft.getMinecraft().theWorld.getEntityByID(argEntityID) != null ? Minecraft.getMinecraft().theWorld.getEntityByID(argEntityID).getCommandSenderName() : "NULL";
        this.model = null;
    }

    public boolean canBeUpdated() {
        return !this.updatedThisFrame;
    }

    /** How far above the ground another entity still stands, in blocks: twice the rounding of its sent position. */
    private static final double REMOTE_GROUND_MARGIN = 1.0 / 16.0;
    private boolean receivedOnGround;

    /** Whether the last ground check found support at a remote player's received, unsmoothed position. */
    public boolean isReceivedOnGround() {
        return this.receivedOnGround;
    }

    public boolean calcOnGround() {
        Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(this.entityID);
        if (entity == null) {
            this.receivedOnGround = false;
            return false;
        }
        return this.calcOnGround(entity, entity == Minecraft.getMinecraft().thePlayer,
            box -> !entity.worldObj.getCollidingBoundingBoxes(entity, box).isEmpty());
    }

    boolean calcOnGround(Entity entity, boolean local, Predicate<AxisAlignedBB> collides) {
        // Other entities' positions arrive rounded to 1/32 block and smoothed: standing, they can float a hair above it.
        double margin = local ? 0.001 : REMOTE_GROUND_MARGIN;
        // Remote players interpolate a target over three ticks, but receive another every two ticks. During bunny
        // hops the displayed position can turn upward before ever reaching the landing target. Test the received
        // position as well, at its own X/Z (so a ledge below the displayed position cannot support that target).
        this.receivedOnGround = !local && entity instanceof EntityOtherPlayerMP && collides.test(
            entity.boundingBox.getOffsetBoundingBox(entity.serverPosX / 32.0 - entity.posX,
                entity.serverPosY / 32.0 - entity.posY, entity.serverPosZ / 32.0 - entity.posZ)
                .addCoord(0.0, -margin, 0.0));
        return entity.onGround || this.receivedOnGround
            || collides.test(entity.boundingBox.addCoord(0.0, -margin, 0.0));
    }

    public boolean isOnGround() {
        return this.onGround;
    }

    public void update(float argPartialTicks) {
        if (this.getEntity() == null) {
            return;
        }
        this.ticksPerFrame = (float)Minecraft.getMinecraft().thePlayer.ticksExisted + argPartialTicks - this.ticks;
        this.ticks = (float)Minecraft.getMinecraft().thePlayer.ticksExisted + argPartialTicks;
        this.updatedThisFrame = false;
        boolean calculatedOnGround = this.calcOnGround();
        this.bounced(this.getEntity(), calculatedOnGround);
        // When even the landing packet was skipped, show its inferred touchdown for one client tick. Resetting
        // touchdown and liftoff together never leaves the airborne branch. Keep the result for every render frame
        // in this tick; the next tick starts the new jump if there is still no support.
        this.updateGround(calculatedOnGround || this.bouncedOn(this.getEntity().ticksExisted));
        if (this.getEntity().swingProgress > 0.0f) {
            if (!this.alreadyPunched) {
                this.onPunch();
                this.alreadyPunched = true;
            }
        } else {
            this.alreadyPunched = false;
        }
        this.ticksAfterPunch += this.ticksPerFrame;
    }

    void updateGround(boolean calculatedOnGround) {
        if (calculatedOnGround && !this.onGround) {
            this.onTouchdown();
            this.onGround = true;
        } else if (!calculatedOnGround && this.onGround) {
            this.onLiftoff();
            this.onGround = false;
        }
        if (!this.isOnGround()) {
            this.ticksAfterLiftoff += this.ticksPerFrame;
        }
        if (this.isOnGround()) {
            this.ticksAfterTouchdown += this.ticksPerFrame;
        }
    }

    /**
     * Whether another entity turned from falling to rising this tick: it landed and took off again. Its position
     * arrives every few ticks, smoothed, so a jump's one tick on the ground can pass between two samples.
     */
    private boolean bounced(Entity entity, boolean grounded) {
        if (entity == null) return false;
        boolean eligible = entity != Minecraft.getMinecraft().thePlayer && !grounded && !entity.isInWater()
            && !entity.isRiding() && !(entity instanceof EntityLivingBase && ((EntityLivingBase)entity).isOnLadder());
        boolean bounced = this.bounce.sample(entity.ticksExisted, entity.posY - entity.lastTickPosY, eligible);
        this.lastBounce = bounced ? entity.ticksExisted : this.lastBounce;
        return bounced;
    }

    /** Whether {@link #bounced} saw a landing on the given tick. */
    public boolean bouncedOn(int tick) {
        return this.lastBounce == tick;
    }

    private final BounceTracker bounce = new BounceTracker();
    private int lastBounce = Integer.MIN_VALUE;

    /** Tracks a significant descent until a significant ascent, across interpolation's near-zero samples. */
    static final class BounceTracker {
        private static final double SPEED = 0.02;
        private int sampleTick = Integer.MIN_VALUE;
        private boolean descending;

        boolean sample(int tick, double speed, boolean eligible) {
            if (!eligible) this.descending = false;
            if (tick == this.sampleTick) return false;
            this.sampleTick = tick;
            if (!eligible) return false;
            if (speed < -SPEED) {
                this.descending = true;
            } else if (speed > SPEED) {
                boolean bounced = this.descending;
                this.descending = false;
                return bounced;
            }
            // Observed bunny hops turn -0.277778 -> +0.011574 -> +0.201775. The small rise must not erase the fall.
            return false;
        }
    }

    public EntityLivingBase getEntity() {
        if (Minecraft.getMinecraft().theWorld.getEntityByID(this.entityID) instanceof EntityLivingBase) {
            return (EntityLivingBase)Minecraft.getMinecraft().theWorld.getEntityByID(this.entityID);
        }
        return null;
    }

    public void onTouchdown() {
        this.ticksAfterTouchdown = 0.0f;
    }

    public void onLiftoff() {
        this.ticksAfterLiftoff = 0.0f;
    }

    public void onPunch() {
        this.ticksAfterPunch = 0.0f;
    }
}
