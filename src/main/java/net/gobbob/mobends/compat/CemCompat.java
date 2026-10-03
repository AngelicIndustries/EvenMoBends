package net.gobbob.mobends.compat;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.fentanylsolutions.salamander.cem.client.CemBinding;
import org.fentanylsolutions.salamander.cem.client.CemModelParts;
import org.fentanylsolutions.salamander.cem.client.CemPlayers;
import org.fentanylsolutions.salamander.cem.client.CemResources;
import org.fentanylsolutions.salamander.cem.client.CemRuntime;
import org.fentanylsolutions.salamander.cem.client.CemTargets;

/** Ask the same loader/variant selector as Salamander's renderer, including config and invalid-pack fallback. */
public final class CemCompat {
    private static final ModelSpider SPIDER = new ModelSpider();
    private static final Map<ModelBase, CemBinding> BINDINGS = new IdentityHashMap<>();

    private CemCompat() {}

    public static boolean hasModel(Entity entity) {
        return hasModel(entity, SPIDER, null);
    }

    public static boolean hasModel(Entity entity, ModelBase model, ResourceLocation texture) {
        if (entity == null || !(model instanceof CemModelParts)) return false;
        if (entity instanceof AbstractClientPlayer && model instanceof ModelBiped) {
            CemPlayers.register((ModelBiped)model);
            if (WawelAuthCompat.isModernSkinSupportEnabled())
                CemPlayers.skinModel((AbstractClientPlayer)entity, WawelAuthCompat.isSlim((AbstractClientPlayer)entity));
        }
        Map<String, ModelRenderer> parts = ((CemModelParts)model).salamander$cemParts();
        CemBinding binding = BINDINGS.get(model);
        if (binding == null || !binding.matches(parts)) {
            binding = new CemBinding(model, parts);
            BINDINGS.put(model, binding);
        }
        ResourceLocation previous = CemRuntime.texture();
        try {
            CemRuntime.texture(texture);
            return selects(entity, binding, CemTargets.candidates(entity, model, texture));
        } finally {
            CemRuntime.texture(previous);
        }
    }

    static boolean selects(Object subject, CemBinding binding, List<String> candidates) {
        return CemResources.INSTANCE.select(candidates, binding, subject) != null;
    }
}
