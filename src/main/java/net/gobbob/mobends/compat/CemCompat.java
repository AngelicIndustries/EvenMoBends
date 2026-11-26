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
import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.client.CemModelParts;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.client.CemResources;
import industries.angelic.salamander.cem.client.CemRuntime;
import industries.angelic.salamander.cem.client.CemTargets;

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

    /** The resource pack whose model draws the spider in Mo' Bends' place, or null; see {@link #source}. */
    public static String source(Entity entity) {
        return source(entity, SPIDER, null);
    }

    /**
     * The resource pack whose model draws {@code entity} in Mo' Bends' place, or null when none does. Salamanders
     * without the API name no pack: "a resource pack" stands for it.
     */
    public static String source(Entity entity, ModelBase model, ResourceLocation texture) {
        if (!hasModel(entity, model, texture)) return null;
        String pack = net.gobbob.mobends.compat.salamander.PackFaces.modelSource(entity, model, texture);
        return pack != null ? pack : net.minecraft.client.resources.I18n.format("mobends.menu.override.somePack");
    }

    static boolean selects(Object subject, CemBinding binding, List<String> candidates) {
        return CemResources.INSTANCE.select(candidates, binding, subject) != null;
    }
}
