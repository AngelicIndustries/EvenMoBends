package net.gobbob.mobends.compat;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.util.ResourceLocation;
import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.client.CemResources;
import industries.angelic.salamander.config.CemConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Exercises the actual dependency, including parsing and reloads; no invented reflection API. */
class CemCompatTest {
    private final Resources resources = new Resources();
    private final CemBinding binding;
    private final Object subject = new Object();

    CemCompatTest() {
        ModelSpider model = new ModelSpider();
        binding = new CemBinding(model, Collections.singletonMap("head", model.spiderHead));
    }

    @AfterEach
    void restore() {
        CemConfig.enabled = true;
        resources.files.clear();
        CemResources.INSTANCE.onResourceManagerReload(resources);
    }

    private boolean selects() { return CemCompat.selects(subject, binding, Collections.singletonList("spider")); }

    @Test
    void validPackTakesOverAndReloadGivesTheEntityBack() {
        CemResources.INSTANCE.onResourceManagerReload(resources);
        assertFalse(selects());
        resources.files.put("optifine/cem/spider.jem", "{\"textureSize\":[64,32],\"models\":[{\"part\":\"head\",\"id\":\"head\",\"boxes\":[{\"textureOffset\":[0,0],\"coordinates\":[-4,-4,-4,8,8,8]}]}]}");
        CemResources.INSTANCE.onResourceManagerReload(resources);
        assertTrue(selects());
        CemConfig.enabled = false;
        assertFalse(selects());
        CemConfig.enabled = true;
        assertTrue(selects());
        resources.files.clear();
        CemResources.INSTANCE.onResourceManagerReload(resources);
        assertFalse(selects());
    }

    @Test
    void malformedPackDoesNotSuppressBends() {
        resources.files.put("optifine/cem/spider.jem", "broken JSON");
        CemResources.INSTANCE.onResourceManagerReload(resources);
        assertFalse(selects());
    }

    private static final class Resources implements IResourceManager {
        final Map<String, String> files = new HashMap<>();
        @Override public Set<String> getResourceDomains() { return Collections.singleton("minecraft"); }
        @Override public IResource getResource(ResourceLocation location) throws FileNotFoundException {
            String value = files.get(location.getResourcePath());
            if (value == null) throw new FileNotFoundException(location.toString());
            return new IResource() {
                @Override public java.io.InputStream getInputStream() { return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8)); }
                @Override public boolean hasMetadata() { return false; }
                @Override public IMetadataSection getMetadata(String name) { return null; }
            };
        }
        @Override public List<IResource> getAllResources(ResourceLocation location) throws FileNotFoundException {
            return Collections.singletonList(getResource(location));
        }
    }
}
