package net.gobbob.mobends.client.animation;

import java.util.List;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.util.SmoothVector3f;

/**
 * A Mo' Bends model that Blockbench animations can drive ({@link BendsAnimator}): its rig's bones and what each one
 * drives. Bones are named as in the model's geometry file; {@code root} (the whole-body transform) is implicit.
 */
public interface AnimatedModel {
    /** The rig's bones, parents before children, without {@code root}. */
    List<String> rigBones();

    /** A bone's parent ({@code root} for top-level bones). */
    String rigParent(String bone);

    /** The part a bone drives, or null for bones without one (aim and item bones, or no such bone). */
    ModelRendererBends rigPart(String bone);

    /** For an aim bone, the bone whose part it turns before that part's own rotation; else null. */
    default String rigAimed(String bone) {
        return null;
    }

    /** The aim bone above a bone, or null. */
    default String rigAim(String bone) {
        return null;
    }

    /** For an item bone, the arm whose hand holds its item; else null. */
    default String rigHoldingArm(String bone) {
        return null;
    }

    /** An item bone's transform, or null. */
    default ItemBone rigItem(String bone) {
        return null;
    }

    /** {@code root}'s position (as {@code (-x, y, z)}; resting at y = -1) and rotation. */
    SmoothVector3f rootOffset();

    SmoothVector3f rootRotation();

    /** Where the entity looks, relative to its body, in degrees. */
    float headPitch();

    float headYaw();

    /** Vanilla's limb swing (distance walked) and how strongly the limbs swing (0 to 1). */
    float limbSwing();

    float limbSwingAmount();
}
