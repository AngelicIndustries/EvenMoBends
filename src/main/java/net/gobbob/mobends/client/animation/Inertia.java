package net.gobbob.mobends.client.animation;

import java.util.HashMap;
import java.util.Map;

/**
 * Inertialization for an animation that starts mid-motion (e.g. a sword swing out of a stance): on its first frame,
 * how far each channel it drives is from where the part shows, faded out over the start of the animation. The
 * animation keeps its shape from the first frame, without snapping to it.
 */
public final class Inertia {
    private final Map<String, Float> offsets = new HashMap<String, Float>();
    private boolean capturing;
    private float remaining;

    /** On the animation's first frame: the next {@link #apply} calls record each channel's offset. */
    void capture() {
        this.offsets.clear();
        this.capturing = true;
        this.remaining = 1;
    }

    /** @param progress how far through the fade, counting from the frame before the animation started. */
    void fade(float progress) {
        this.capturing = false;
        float p = Math.max(0, Math.min(1, progress));
        this.remaining = 1 - p * p * (3 - 2 * p);
    }

    boolean active() {
        return this.remaining > 0;
    }

    /** {@code value} for channel {@code key}, plus what is left of the part's offset from it when the animation started. */
    float apply(String key, float value, float shown) {
        if (this.capturing) this.offsets.put(key, shown - value);
        Float offset = this.offsets.get(key);
        return offset == null ? value : value + offset * this.remaining;
    }
}
