package net.gobbob.mobends.data;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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

    public boolean calcOnGround() {
        Entity entity = Minecraft.getMinecraft().theWorld.getEntityByID(this.entityID);
        if (entity == null) {
            return false;
        }
        return entity.onGround
            || !entity.worldObj.getCollidingBoundingBoxes(
                entity,
                entity.boundingBox.addCoord(0.0, -0.001, 0.0)).isEmpty();
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
        if (calculatedOnGround && !this.onGround) {
            this.onTouchdown();
            this.onGround = true;
        } else if (!calculatedOnGround && this.onGround) {
            this.onLiftoff();
            this.onGround = false;
        }
        if (this.getEntity().swingProgress > 0.0f) {
            if (!this.alreadyPunched) {
                this.onPunch();
                this.alreadyPunched = true;
            }
        } else {
            this.alreadyPunched = false;
        }
        if (!this.isOnGround()) {
            this.ticksAfterLiftoff += this.ticksPerFrame;
        }
        if (this.isOnGround()) {
            this.ticksAfterTouchdown += this.ticksPerFrame;
        }
        this.ticksAfterPunch += this.ticksPerFrame;
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
