package net.gobbob.mobends.client.animation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;

/**
 * A biped with Mo' Bends' fore-limbs (the player, the zombie), rigged as {@link PlayerRig}: body, neck and head,
 * arms with their aim bones and forearms, legs and forelegs, and item bones where the model holds items.
 */
public interface BipedRigModel extends AnimatedModel {
    ModelBiped biped();

    ModelRenderer foreArm(boolean right);

    ModelRenderer foreLeg(boolean right);

    /** Whether the model has the item bones. */
    default boolean holdsItems() {
        return false;
    }

    @Override
    default List<String> rigBones() {
        List<String> bones = new ArrayList<String>();
        for (Bone bone : Bone.values()) if (this.holdsItems() || bone.holdingArm() == null) bones.add(bone.id);
        return Collections.unmodifiableList(bones);
    }

    @Override
    default String rigParent(String bone) {
        Bone b = Bone.byId(bone);
        return b == null ? null : b.parent;
    }

    @Override
    default ModelRendererBends rigPart(String bone) {
        Bone b = Bone.byId(bone);
        return b == null ? null : b.part(this);
    }

    @Override
    default String rigAimed(String bone) {
        Bone b = Bone.byId(bone);
        return b == null || b.aimed() == null ? null : b.aimed().id;
    }

    @Override
    default String rigAim(String bone) {
        Bone b = Bone.byId(bone);
        return b == null || b.aim() == null ? null : b.aim().id;
    }

    @Override
    default String rigHoldingArm(String bone) {
        Bone b = Bone.byId(bone);
        return !this.holdsItems() || b == null || b.holdingArm() == null ? null : b.holdingArm().id;
    }
}
