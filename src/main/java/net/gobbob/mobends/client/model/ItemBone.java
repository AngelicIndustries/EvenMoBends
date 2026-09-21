package net.gobbob.mobends.client.model;

import net.gobbob.mobends.util.SmoothVector3f;
import org.lwjgl.opengl.GL11;

/**
 * An animated held-item transform, driven by an item bone of the Blockbench rig. It is applied at the grip, before
 * vanilla's per-item placement, so an item's usual look in the hand is its rest pose.
 */
public final class ItemBone {
    /** Degrees, applied Z, Y, X like {@link ModelRendererBends#rotation}. */
    public final SmoothVector3f rotation = new SmoothVector3f();
    /** Model pixels, before the rotation. */
    public final SmoothVector3f position = new SmoothVector3f();
    public float scaleX;
    public float scaleY;
    public float scaleZ;
    private final boolean free;

    /** @param free true for a bone the item can leave the hand with, hidden unless an animation shows it. */
    public ItemBone(boolean free) {
        this.free = free;
        this.resetScale();
    }

    public boolean isVisible() {
        return this.scaleX != 0 && this.scaleY != 0 && this.scaleZ != 0;
    }

    /** Per frame, before animations: the hand holds its item, a free item stays hidden. */
    public void resetScale() {
        float scale = this.free ? 0 : 1;
        this.scaleX = scale;
        this.scaleY = scale;
        this.scaleZ = scale;
    }

    public void update(float ticksPerFrame) {
        this.rotation.update(ticksPerFrame);
        this.position.update(ticksPerFrame);
    }

    public void set(ItemBone other) {
        this.rotation.set(other.rotation);
        this.position.set(other.position);
        this.scaleX = other.scaleX;
        this.scaleY = other.scaleY;
        this.scaleZ = other.scaleZ;
    }

    /** Moves the current GL matrix from the grip into the item bone's frame. */
    public void apply(float scale) {
        GL11.glTranslatef(this.position.getX() * scale, this.position.getY() * scale, this.position.getZ() * scale);
        GL11.glRotatef(this.rotation.getZ(), 0, 0, 1);
        GL11.glRotatef(this.rotation.getY(), 0, 1, 0);
        GL11.glRotatef(this.rotation.getX(), 1, 0, 0);
        GL11.glScalef(this.scaleX, this.scaleY, this.scaleZ);
    }
}
