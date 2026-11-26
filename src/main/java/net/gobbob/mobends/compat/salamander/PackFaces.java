package net.gobbob.mobends.compat.salamander;

import java.lang.reflect.Method;
import net.gobbob.mobends.client.animation.BendsGeometry;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Player packs that only animate the face, such as Just Expressions: Salamander draws the pack's face on Mo' Bends'
 * head, while Mo' Bends keeps animating the body. A pack that covers the body too, such as Fresh Moves, replaces the
 * player as before. Salamander decides which a pack is, through its CEM API, which is reached by reflection: Mo' Bends
 * builds against Salamanders without it, and with one of those every pack replaces the player.
 */
public final class PackFaces {
    /** The General setting: combine face-only player packs with Mo' Bends' body. */
    public static boolean combine = true;

    private static final Method COVERAGE, SOURCE, DRAW, EXCLUDE, MODEL_SOURCE;

    static {
        Method coverage = null, source = null, draw = null, exclude = null, modelSource = null;
        try {
            Class<?> api = Class.forName("industries.angelic.salamander.api.cem.CemApi");
            coverage = api.getMethod("playerPackCoverage", Entity.class);
            source = api.getMethod("playerPackSource", Entity.class);
            draw = api.getMethod("drawPlayerPackFace", Entity.class, float.class, float.class, float.class, float.class,
                float.class, float.class);
            exclude = api.getMethod("excludeModel", ModelBase.class);
            modelSource = api.getMethod("packModelSource", Object.class, ModelBase.class, ResourceLocation.class);
        } catch (ReflectiveOperationException | LinkageError olderSalamander) {
            coverage = source = draw = exclude = modelSource = null;
        }
        COVERAGE = coverage;
        SOURCE = source;
        DRAW = draw;
        EXCLUDE = exclude;
        MODEL_SOURCE = modelSource;
    }

    public enum Cover {
        NONE, FACE, BODY
    }

    private PackFaces() {}

    /** Whether this Salamander tells face-only packs from the others. */
    public static boolean available() {
        return COVERAGE != null;
    }

    /** What the player's pack model covers; null when this Salamander can't tell. */
    public static Cover cover(Entity player) {
        Object coverage = call(COVERAGE, player);
        return coverage == null ? null : Cover.valueOf(((Enum<?>)coverage).name());
    }

    /** Whether the player's pack face draws on Mo' Bends' head now: a face-only pack, and the setting on. */
    public static boolean combines(Entity player) {
        return combine && cover(player) == Cover.FACE;
    }

    /** The resource pack the player's pack model comes from, or null. */
    public static String source(Entity player) {
        return (String)call(SOURCE, player);
    }

    /** The resource pack whose model would draw {@code subject} with this model and texture, or null. */
    public static String modelSource(Object subject, ModelBase model, ResourceLocation texture) {
        return (String)call(MODEL_SOURCE, subject, model, texture);
    }

    /** Keeps Salamander's pack models off a model Mo' Bends draws itself, whose head may take a pack's face. */
    public static void exclude(ModelBase model) {
        call(EXCLUDE, model);
    }

    /**
     * Draws the face of a face-only player pack on the model's head, after the model drew: the head bone's frame is
     * the vanilla head's, the face's own animations are the pack's. Head yaw and pitch as the model turned its head.
     */
    public static void draw(ModelBendsPlayer model, Entity entity, float limbSwing, float limbSpeed, float age,
        float headYaw, float headPitch, float scale) {
        if (!combine || DRAW == null || !(entity instanceof AbstractClientPlayer)) return;
        if (model.bipedHead.isHidden || !model.bipedHead.showModel) return;
        BendsGeometry geometry = model.geometry();
        String bone = geometry.rig.name(model.bipedHead);
        if (bone == null) return;
        GL11.glPushMatrix();
        try {
            if (model.isChild) {
                // As ModelBendsPlayer draws a child's head.
                GL11.glScalef(0.75f, 0.75f, 0.75f);
                GL11.glTranslatef(0.0f, 16.0f * scale, 0.0f);
            }
            geometry.attach(bone, scale);
            call(DRAW, entity, limbSwing, limbSpeed, age, headYaw, headPitch, scale);
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static Object call(Method method, Object... arguments) {
        if (method == null) return null;
        try {
            return method.invoke(null, arguments);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
            if (cause instanceof RuntimeException) throw (RuntimeException)cause;
            if (cause instanceof Error) throw (Error)cause;
            throw new IllegalStateException(cause);
        }
    }
}
