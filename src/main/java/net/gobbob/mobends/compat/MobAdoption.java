package net.gobbob.mobends.compat;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.ReflectionHelper;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.client.renderer.entity.RenderBendsAdoptedSpider;
import net.gobbob.mobends.client.renderer.entity.RenderBendsAdoptedZombie;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSpider;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSpider;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;

/**
 * Other mods' spiders and zombies drawn the vanilla way, whatever the mod: a renderer that only picks the texture, the
 * size and a spider's eyes around the vanilla model. Mo' Bends draws those with its rig and asks the mod's renderer for
 * the rest. A renderer that draws more (its own model pass, items, effects) or a model with parts of its own keeps its
 * mob.
 */
public final class MobAdoption {
    /** The General setting; off, every mod keeps its mobs. */
    public static boolean enabled = true;
    /** Entity names (as /summon takes them) or class names left to their mod. */
    public static Set<String> excluded = Collections.emptySet();

    /** The kinds of mob Mo' Bends animates, by the Animate switch each follows. */
    public enum Family {
        ZOMBIE("zombie"), SPIDER("spider"), CAVE_SPIDER("cave_spider");

        public final String id;

        Family(String id) {
            this.id = id;
        }

        static Family of(Class<?> type) {
            if (EntityCaveSpider.class.isAssignableFrom(type)) return CAVE_SPIDER;
            if (EntitySpider.class.isAssignableFrom(type)) return SPIDER;
            if (EntityZombie.class.isAssignableFrom(type)) return ZOMBIE;
            return null;
        }

        boolean animated() {
            for (AnimatedEntity entity : AnimatedEntity.animatedEntities) {
                if (entity.id.equals(this.id)) return entity.animate;
            }
            return false;
        }
    }

    /** Adopted entity classes and the mod renderers they had, put back when the settings change. */
    private static final Map<Class<? extends Entity>, Render> ORIGINALS = new LinkedHashMap<>();
    /** Per kind: the mods whose mobs Mo' Bends animates too, and the mobs their mods keep ("Zomgus (Wild Mobs)"). */
    private static final Map<Family, Set<String>> ADOPTED_MODS = new LinkedHashMap<>();
    private static final Map<Family, Set<String>> KEPT_MODS = new LinkedHashMap<>();
    private static boolean scanned;

    /**
     * Vanilla's drawing steps a renderer may not change for Mo' Bends to take its mob: it would draw more than a
     * texture, a size and eyes. MCP and SRG names.
     */
    private static final Set<String> DRAWS = new HashSet<>(Arrays.asList(
        "doRender", "func_76986_a", "renderModel", "func_77036_a", "renderEquippedItems", "func_77029_c",
        "rotateCorpse", "func_77043_a", "renderLivingAt", "func_77039_a", "inheritRenderPass", "func_77035_b",
        "passSpecialRender", "func_77033_b", "renderArrowsStuckInEntity", "func_85093_e", "func_82408_c",
        "handleRotationFloat", "func_77044_a", "getColorMultiplier", "func_77030_a"));
    /** And for zombies, their armor and overlay passes too: Mo' Bends draws armor on its own rig. */
    private static final Set<String> ZOMBIE_DRAWS = new HashSet<>(Arrays.asList(
        "shouldRenderPass", "func_77032_a", "func_82421_b"));

    private static final Method TEXTURE = ReflectionHelper.findMethod(Render.class, null,
        new String[] {"getEntityTexture", "func_110775_a"}, Entity.class);
    private static final Method PRE_RENDER = ReflectionHelper.findMethod(RendererLivingEntity.class, null,
        new String[] {"preRenderCallback", "func_77041_b"}, EntityLivingBase.class, float.class);
    private static final Method RENDER_PASS = ReflectionHelper.findMethod(RendererLivingEntity.class, null,
        new String[] {"shouldRenderPass", "func_77032_a"}, EntityLivingBase.class, int.class, float.class);
    private static final Field SHADOW = ReflectionHelper.findField(Render.class, "shadowSize", "field_76989_e");
    private static final Field MAIN_MODEL = ReflectionHelper.findField(RendererLivingEntity.class, "mainModel", "field_77045_g");

    private MobAdoption() {}

    /** Once every mod's renderer is in place: from the first client tick. */
    public static void scanOnce() {
        if (scanned) return;
        scanned = true;
        apply();
    }

    /** Puts back what was adopted, then adopts again as the settings say now; nothing before the first scan. */
    public static void reapply() {
        if (scanned) apply();
    }

    private static void apply() {
        Map<Class<? extends Entity>, Render> map = RenderManager.instance.entityRenderMap;
        map.putAll(ORIGINALS);
        ORIGINALS.clear();
        ADOPTED_MODS.clear();
        KEPT_MODS.clear();
        for (Map.Entry<Class<? extends Entity>, Render> entry : new ArrayList<>(map.entrySet())) {
            Class<? extends Entity> type = entry.getKey();
            Render render = entry.getValue();
            Family family = type == null ? null : Family.of(type);
            // Vanilla's own mobs are Mo' Bends' already, or left vanilla by their Animate switch.
            if (family == null || render == null || type.getName().startsWith("net.minecraft.")
                || render.getClass().getName().startsWith("net.gobbob.mobends.")) continue;
            String mod = modName(type);
            if (!enabled || !family.animated() || excluded(type) || !adoptable(family, render)) {
                kinds(KEPT_MODS, family).add(entityName(type) + " (" + mod + ")");
                continue;
            }
            Render adopted = family == Family.ZOMBIE ? new RenderBendsAdoptedZombie((RenderBiped)render)
                : new RenderBendsAdoptedSpider((RenderSpider)render);
            adopted.setRenderManager(RenderManager.instance);
            map.put(type, adopted);
            ORIGINALS.put(type, render);
            kinds(ADOPTED_MODS, family).add(mod);
        }
        BendsLogger.log("Animating " + ORIGINALS.size() + " other mods' spiders and zombies", BendsLogger.INFO);
    }

    private static Set<String> kinds(Map<Family, Set<String>> map, Family family) {
        Set<String> set = map.get(family);
        if (set == null) map.put(family, set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        return set;
    }

    /** The mods whose mobs of this kind Mo' Bends animates too, sorted. */
    public static List<String> adoptedMods(String id) {
        return mods(ADOPTED_MODS, id);
    }

    /** The mobs of this kind their mods draw their own way, with the mod's name, sorted. */
    public static List<String> keptMobs(String id) {
        return mods(KEPT_MODS, id);
    }

    private static List<String> mods(Map<Family, Set<String>> map, String id) {
        for (Family family : Family.values()) {
            if (family.id.equals(id)) {
                Set<String> set = map.get(family);
                return set == null ? Collections.<String>emptyList() : new ArrayList<>(set);
            }
        }
        return Collections.emptyList();
    }

    private static boolean excluded(Class<?> type) {
        if (excluded.isEmpty()) return false;
        Object name = EntityList.classToStringMapping.get(type);
        return name != null && excluded.contains(name) || excluded.contains(type.getName())
            || excluded.contains(type.getSimpleName());
    }

    /**
     * Whether Mo' Bends can draw what this renderer draws: the vanilla model of that kind, and the renderer changes
     * only the texture, the size and (for spiders) the eye pass.
     */
    static boolean adoptable(Family family, Render render) {
        boolean zombie = family == Family.ZOMBIE;
        if (zombie ? !(render instanceof RenderBiped) : !(render instanceof RenderSpider)) return false;
        for (Class<?> type = render.getClass(); type != null && !type.getName().startsWith("net.minecraft."); type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (DRAWS.contains(method.getName()) || zombie && ZOMBIE_DRAWS.contains(method.getName())) return false;
            }
        }
        ModelBase model = mainModel(render);
        if (model == null) return false;
        ModelBase vanilla = zombie ? new ModelZombie() : new ModelSpider();
        if (zombie ? !(model instanceof ModelBiped) : !(model instanceof ModelSpider)) return false;
        return sameGeometry(model, vanilla);
    }

    /** The same texture size and boxes: no parts of its own, nor resized ones. */
    static boolean sameGeometry(ModelBase model, ModelBase vanilla) {
        if (model.textureWidth != vanilla.textureWidth || model.textureHeight != vanilla.textureHeight) return false;
        return boxes(model).equals(boxes(vanilla));
    }

    private static List<String> boxes(ModelBase model) {
        List<String> boxes = new ArrayList<>();
        for (Object part : model.boxList) {
            for (Object box : ((ModelRenderer)part).cubeList) {
                ModelBox cube = (ModelBox)box;
                boxes.add(round(cube.posX2 - cube.posX1) + "x" + round(cube.posY2 - cube.posY1) + "x" + round(cube.posZ2 - cube.posZ1));
            }
        }
        Collections.sort(boxes);
        return boxes;
    }

    private static String round(float value) {
        return String.valueOf(Math.round(value * 100.0f) / 100.0f);
    }

    /** The model a mod's renderer draws its mob with, or null. */
    public static ModelBase mainModel(Render render) {
        try {
            return render instanceof RendererLivingEntity ? (ModelBase)MAIN_MODEL.get(render) : null;
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    /** The mod a mob comes from, by its class's package, else by its entity name's prefix. */
    static String modName(Class<?> type) {
        String name = type.getName();
        String pkg = name.lastIndexOf('.') < 0 ? "" : name.substring(0, name.lastIndexOf('.'));
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            if (mod.getOwnedPackages().contains(pkg)) return mod.getName();
        }
        Object entity = EntityList.classToStringMapping.get(type);
        String id = entity == null ? "" : entity.toString();
        if (id.indexOf('.') > 0) {
            ModContainer mod = Loader.instance().getIndexedModList().get(id.substring(0, id.indexOf('.')));
            return mod != null ? mod.getName() : id.substring(0, id.indexOf('.'));
        }
        return type.getSimpleName();
    }

    /** A mob's name as the game shows it, else its entity name without the mod's prefix. */
    static String entityName(Class<?> type) {
        Object entity = EntityList.classToStringMapping.get(type);
        if (entity == null) return type.getSimpleName();
        String id = entity.toString();
        String key = "entity." + id + ".name";
        String shown = net.minecraft.util.StatCollector.translateToLocal(key);
        if (!shown.equals(key)) return shown;
        return id.indexOf('.') >= 0 ? id.substring(id.indexOf('.') + 1) : id;
    }

    private static final Set<Class<?>> FAILED = new HashSet<>();

    /** The mod renderer's texture for this mob, or {@code fallback} if asking it fails. */
    public static ResourceLocation texture(Render render, Entity entity, ResourceLocation fallback) {
        try {
            ResourceLocation texture = (ResourceLocation)TEXTURE.invoke(render, entity);
            return texture != null ? texture : fallback;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed(render, e);
            return fallback;
        }
    }

    /** The mod renderer's size and any other GL setup it does before the model, then its shadow size. */
    public static void preRender(RendererLivingEntity render, EntityLivingBase entity, float partialTicks) {
        try {
            PRE_RENDER.invoke(render, entity, partialTicks);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed(render, e);
        }
    }

    /** The mod renderer's extra pass, a spider's eyes, as it binds and sets it up; -1 for none. */
    public static int renderPass(RendererLivingEntity render, EntityLivingBase entity, int pass, float partialTicks) {
        try {
            return (Integer)RENDER_PASS.invoke(render, entity, pass, partialTicks);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed(render, e);
            return -1;
        }
    }

    /** The mod renderer's shadow size, which some set as they draw. */
    public static float shadow(Render render, float fallback) {
        try {
            return SHADOW.getFloat(render);
        } catch (IllegalAccessException e) {
            return fallback;
        }
    }

    /**
     * Runs the mod model's own setLivingAnimations when it has one, for what it does to GL, such as Twilight Forest's
     * lich minions' tint. Vanilla's models have none worth running.
     */
    public static void livingAnimations(ModelBase model, EntityLivingBase entity, float partialTicks) {
        if (model == null || model.getClass().getName().startsWith("net.minecraft.")) return;
        try {
            float swing = entity.prevLimbSwingAmount + (entity.limbSwingAmount - entity.prevLimbSwingAmount) * partialTicks;
            model.setLivingAnimations(entity, entity.limbSwing - entity.limbSwingAmount * (1.0f - partialTicks), swing, partialTicks);
        } catch (RuntimeException e) {
            failed(model.getClass(), e);
        }
    }

    private static void failed(Object source, Exception e) {
        Class<?> type = source instanceof Class ? (Class<?>)source : source.getClass();
        if (FAILED.add(type)) BendsLogger.log("Asking " + type.getName() + " how to draw its mob failed: " + e, BendsLogger.ERROR);
    }
}
