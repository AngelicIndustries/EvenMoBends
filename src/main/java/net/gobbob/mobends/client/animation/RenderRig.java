package net.gobbob.mobends.client.animation;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.client.model.ModelRenderer;

/** The animation rig plus independent skin layers, so armor/skin visibility survives geometry baking. */
public final class RenderRig implements AnimatedModel {
    private final AnimatedModel source;
    private final List<String> bones;
    private final Map<String, ModelRendererBends> parts = new LinkedHashMap<>();
    private final Map<String, String> parents = new LinkedHashMap<>();
    private final Map<ModelRenderer, String> names = new IdentityHashMap<>();

    public RenderRig(AnimatedModel source) {
        this.source = source;
        this.bones = new ArrayList<>(source.rigBones());
        for (String bone : this.bones) {
            ModelRendererBends part = source.rigPart(bone);
            this.parts.put(bone, part);
            this.parents.put(bone, source.rigParent(bone));
            if (part != null) this.names.put(part, bone);
        }
        for (String bone : new ArrayList<>(this.bones)) children(bone, this.parts.get(bone));
    }

    private void children(String parent, ModelRendererBends part) {
        if (part == null || part.childModels == null) return;
        int index = 0;
        for (ModelRenderer child : part.childModels) {
            if (this.names.containsKey(child)) continue;
            String name = parent + "_layer" + index++;
            ModelRendererBends layer = (ModelRendererBends)child;
            this.names.put(child, name);
            this.bones.add(name);
            this.parts.put(name, layer);
            this.parents.put(name, parent);
            children(name, layer);
        }
    }

    /** Export hidden variants too: their visibility belongs to the pose, never the shared geometry. */
    public com.google.gson.JsonObject export(String identifier) {
        Map<ModelRendererBends, boolean[]> visibility = new IdentityHashMap<>();
        for (ModelRendererBends part : this.parts.values()) if (part != null) {
            visibility.put(part, new boolean[] {part.showModel, part.isHidden});
            part.showModel = true;
            part.isHidden = false;
        }
        try {
            return PlayerRig.exportGeometry(this, identifier);
        } finally {
            visibility.forEach((part, flags) -> { part.showModel = flags[0]; part.isHidden = flags[1]; });
        }
    }

    public boolean descendant(String bone, String ancestor) {
        for (String name = bone; name != null; name = rigParent(name)) if (ancestor.equals(name)) return true;
        return false;
    }

    public String name(ModelRenderer part) { return this.names.get(part); }

    @Override public List<String> rigBones() { return this.bones; }
    @Override public String rigParent(String bone) { return this.parents.get(bone); }
    @Override public ModelRendererBends rigPart(String bone) { return this.parts.get(bone); }
    @Override public String rigAimed(String bone) { return this.source.rigAimed(bone); }
    @Override public String rigAim(String bone) { return this.source.rigAim(bone); }
    @Override public String rigHoldingArm(String bone) { return this.source.rigHoldingArm(bone); }
    @Override public ItemBone rigItem(String bone) { return this.source.rigItem(bone); }
    @Override public SmoothVector3f rootOffset() { return this.source.rootOffset(); }
    @Override public SmoothVector3f rootRotation() { return this.source.rootRotation(); }
    @Override public float headPitch() { return this.source.headPitch(); }
    @Override public float headYaw() { return this.source.headYaw(); }
    @Override public float limbSwing() { return this.source.limbSwing(); }
    @Override public float limbSwingAmount() { return this.source.limbSwingAmount(); }
}
