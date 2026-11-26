package net.gobbob.mobends.config;

import java.util.HashMap;
import java.util.Map;

import net.gobbob.mobends.AnimatedEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

/**
 * Which mobs Mo' Bends animates, one switch per entity: the Animate category of mobends.cfg. Vanilla's player,
 * zombie, spider and cave spider keep the keys they always had; every other mob is keyed by its entity name, as
 * /summon takes it. A mob without an entry is animated. Renderers ask as they draw, so a switch takes effect at once.
 */
public final class MobSwitches {
    /** Lowercase: Configuration.get lowercases category names, getCategory doesn't. */
    public static final String CATEGORY = "animate";

    private static final Map<String, Boolean> SWITCHES = new HashMap<>();
    /** Each entity class's key, and its switch, as last asked. */
    private static final Map<Class<?>, String> KEYS = new HashMap<>();
    private static final Map<Class<?>, Boolean> CACHE = new HashMap<>();

    private MobSwitches() {}

    /** Reads every switch in the config, and points vanilla's Animate fields at theirs. */
    public static void load(Configuration config) {
        SWITCHES.clear();
        CACHE.clear();
        config.getCategory(CATEGORY).setComment(
            "Which mobs Mo' Bends animates, by entity name. The Animations menu in the config screen sets them.");
        for (AnimatedEntity entity : AnimatedEntity.animatedEntities) {
            entity.animate = config.get(CATEGORY, entity.id, true).getBoolean();
        }
        for (Map.Entry<String, Property> entry : config.getCategory(CATEGORY).entrySet()) {
            SWITCHES.put(entry.getKey(), entry.getValue().getBoolean(true));
        }
    }

    /** Whether Mo' Bends animates this mob; true for anything without a switch. */
    public static boolean animates(Entity entity) {
        return entity == null || animates(entity.getClass());
    }

    public static boolean animates(Class<?> type) {
        Boolean cached = CACHE.get(type);
        if (cached == null) {
            Boolean value = SWITCHES.get(key(type));
            CACHE.put(type, cached = value == null || value);
        }
        return cached;
    }

    public static boolean animates(String key) {
        Boolean value = SWITCHES.get(key);
        return value == null || value;
    }

    /** Switches a mob on or off, and writes the config. */
    public static void set(Configuration config, String key, boolean on) {
        SWITCHES.put(key, on);
        CACHE.clear();
        config.get(CATEGORY, key, true).setValue(on);
        for (AnimatedEntity entity : AnimatedEntity.animatedEntities) {
            if (entity.id.equals(key)) entity.animate = on;
        }
        config.save();
    }

    /**
     * A mob class's switch: vanilla's own classes by their old keys, a mod's by its entity name. A class that isn't
     * registered itself (a player stand-in, a mod's helper subclass) follows the nearest registered one it extends.
     */
    public static String key(Class<?> type) {
        String key = KEYS.get(type);
        if (key != null) return key;
        if (EntityPlayer.class.isAssignableFrom(type)) key = "player";
        for (Class<?> c = type; key == null && c != null && Entity.class.isAssignableFrom(c); c = c.getSuperclass()) {
            if (c == EntityZombie.class) key = "zombie";
            else if (c == EntityCaveSpider.class) key = "cave_spider";
            else if (c == EntitySpider.class) key = "spider";
            else {
                Object name = EntityList.classToStringMapping.get(c);
                if (name != null) key = name.toString();
            }
        }
        if (key == null) key = type.getName();
        KEYS.put(type, key);
        return key;
    }
}
