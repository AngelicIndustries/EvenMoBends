package net.gobbob.mobends.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.compat.MobAdoption;
import net.gobbob.mobends.config.MobSwitches;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.world.World;

/**
 * The mobs the menu lists: vanilla's first, then every other mod's spider and zombie, by mod. Each is one Mo' Bends
 * animates (its switch says whether), or one its mod draws: left to it by the General setting, or drawn its own way.
 */
final class MobRoster {

    enum Kind {
        ANIMATED, SETTING_OFF, OWN_DRAWING
    }

    static final class Entry {
        /** Its switch's key in the Animate category; see {@link MobSwitches}. */
        final String key;
        final String name;
        final String mod;
        /** Its entity class, or null for the player. */
        final Class<? extends Entity> type;
        /** Vanilla's own entry for the player, zombie, spider and cave spider, else null. */
        final AnimatedEntity vanilla;
        /** The rig it takes: player, zombie, spider or cave_spider. */
        final String family;
        final Kind kind;
        /** The mod whose renderer draws it, when Mo' Bends doesn't. */
        final String keeper;
        private EntityLivingBase preview;
        private boolean previewFailed;
        /** How much its renderer scales it, measured once; see {@link MobAdoption#renderScale}. */
        private float renderScale = -1.0f;

        Entry(String key, String name, String mod, Class<? extends Entity> type, AnimatedEntity vanilla, String family,
            Kind kind, String keeper) {
            this.key = key;
            this.name = name;
            this.mod = mod;
            this.type = type;
            this.vanilla = vanilla;
            this.family = family;
            this.kind = kind;
            this.keeper = keeper;
        }

        boolean player() {
            return "player".equals(this.family);
        }

        boolean spider() {
            return "spider".equals(this.family) || "cave_spider".equals(this.family);
        }

        boolean animated() {
            return this.kind == Kind.ANIMATED && MobSwitches.animates(this.key);
        }

        /** A mob to show on its page; null when it couldn't be made or drawn here. */
        EntityLivingBase preview() {
            if (this.previewFailed) return null;
            if (this.player()) return MenuPreview.showcasePlayer();
            if (this.preview == null && this.vanilla != null && this.vanilla.entity instanceof EntityLivingBase)
                this.preview = (EntityLivingBase)this.vanilla.entity;
            if (this.preview == null) {
                try {
                    World world = Minecraft.getMinecraft().theWorld != null ? Minecraft.getMinecraft().theWorld
                        : PreviewWorld.INSTANCE;
                    Entity entity = EntityList.createEntityByName(this.key, world);
                    if (entity instanceof EntityLivingBase) this.preview = (EntityLivingBase)entity;
                } catch (Throwable failed) {
                    // A mob that needs more of a world than the menu has.
                }
                if (this.preview == null) this.previewFailed = true;
            }
            return this.preview;
        }

        /** How much the renderer drawing its preview scales it, measured the first time. */
        float renderScale(EntityLivingBase shown) {
            if (this.renderScale < 0.0f)
                this.renderScale = MobAdoption.renderScale(RenderManager.instance.getEntityRenderObject(shown), shown);
            return this.renderScale;
        }

        /** Drawing it failed once: show the placeholder from now on. */
        void previewFailed() {
            this.previewFailed = true;
        }
    }

    private MobRoster() {}

    /** Everything to list, vanilla's first, then by mod and name. */
    static List<Entry> build() {
        List<Entry> entries = new ArrayList<>();
        String vanilla = I18n.format("mobends.menu.vanilla");
        for (AnimatedEntity entity : AnimatedEntity.animatedEntities) {
            boolean player = entity.id.equals("player");
            Class<? extends Entity> type = player ? null : entity.entityClass;
            Render render = player ? null : RenderManager.instance.getEntityClassRenderObject(type);
            boolean ours = player || bends(render);
            entries.add(new Entry(entity.id, I18n.format("mobends.entity." + entity.id), vanilla, type, entity,
                entity.id, ours ? Kind.ANIMATED : Kind.OWN_DRAWING, ours ? null : owner(render)));
        }
        Render pigman = RenderManager.instance.getEntityClassRenderObject(EntityPigZombie.class);
        entries.add(new Entry(MobSwitches.key(EntityPigZombie.class), MobAdoption.entityName(EntityPigZombie.class),
            vanilla, EntityPigZombie.class, null, "zombie", bends(pigman) ? Kind.ANIMATED : Kind.OWN_DRAWING,
            bends(pigman) ? null : owner(pigman)));

        Map<String, List<Entry>> byMod = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Object value : EntityList.stringToClassMapping.entrySet()) {
            Map.Entry<?, ?> mapping = (Map.Entry<?, ?>)value;
            if (!(mapping.getKey() instanceof String) || !(mapping.getValue() instanceof Class)) continue;
            @SuppressWarnings("unchecked")
            Class<? extends Entity> type = (Class<? extends Entity>)mapping.getValue();
            MobAdoption.Family family = MobAdoption.Family.of(type);
            if (family == null || !EntityLiving.class.isAssignableFrom(type) || type.getName().startsWith("net.minecraft.")) continue;
            Render render = RenderManager.instance.getEntityClassRenderObject(type);
            Kind kind = bends(render) ? Kind.ANIMATED
                : MobAdoption.status(type) == MobAdoption.Status.SETTING_OFF ? Kind.SETTING_OFF : Kind.OWN_DRAWING;
            String mod = MobAdoption.modName(type);
            List<Entry> list = byMod.get(mod);
            if (list == null) byMod.put(mod, list = new ArrayList<>());
            list.add(new Entry(MobSwitches.key(type), MobAdoption.entityName(type), mod, type, null, family.id, kind,
                kind == Kind.ANIMATED ? null : owner(render)));
        }
        for (List<Entry> list : byMod.values()) {
            // The ones Mo' Bends animates first, then those their mod keeps, each by name.
            Collections.sort(list, Comparator.comparing((Entry entry) -> entry.kind != Kind.ANIMATED)
                .thenComparing(entry -> entry.name, String.CASE_INSENSITIVE_ORDER));
            entries.addAll(list);
        }
        return entries;
    }

    private static boolean bends(Render render) {
        return render != null && render.getClass().getName().startsWith("net.gobbob.mobends.");
    }

    /** The mod a renderer comes from, by its package, else its class's name. */
    static String owner(Render render) {
        if (render == null) return "?";
        String type = render.getClass().getName();
        if (type.startsWith("net.minecraft.")) return "Minecraft";
        String pkg = type.lastIndexOf('.') < 0 ? "" : type.substring(0, type.lastIndexOf('.'));
        for (ModContainer mod : Loader.instance().getActiveModList()) {
            if (mod.getOwnedPackages().contains(pkg)) return mod.getName();
        }
        return render.getClass().getSimpleName();
    }
}
