package net.gobbob.mobends.compat;

import java.lang.reflect.Field;
import java.util.function.IntConsumer;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;
import org.lwjgl.opengl.GL11;

/**
 * Draconic Evolution puts in its own 3D armor models (OBJ meshes hung on ModelBiped parts) from a SetArmorModel
 * handler, posed like vanilla. They are drawn on the bones instead, at the scale DE draws each part at: a mesh is
 * sized by the scale it is first drawn at. They hug the body, made for skins without the outer layer, which would
 * poke through: it is hidden under them.
 */
public final class DraconicEvolutionCompat {
    private static final String MODELS = "com.brandon3055.draconicevolution.client.model.";
    /** Head, body, arms, legs. */
    private static final float[] SCALES = {1.0f / 13.0f, 1.0f / 15.0f, 1.0f / 15.0f, 1.0f / 16.0f};
    private static ObjScale armScale;
    private static boolean scaleFailureLogged;

    private DraconicEvolutionCompat() {}

    /** The part scales of a Draconic Evolution armor model, or null for any other model. */
    public static float[] armorScales(ModelBase model) {
        if (model == null) return null;
        String name = model.getClass().getName();
        return name.equals(MODELS + "ModelDraconicArmor") || name.equals(MODELS + "ModelWyvernArmor") ? SCALES : null;
    }

    /**
     * On the render thread, before drawing DE armor. Armored Arms may have drawn the shared right arm at 1/16
     * first, baking that scale into its OBJ display list. Pin both arm meshes to DE's 1/15 and discard any list
     * compiled at another scale; passing 1/15 to render alone cannot change an existing list.
     */
    public static void prepareArmor(ModelBiped model) {
        if (armorScales(model) == null || scaleFailureLogged) return;
        try {
            prepareArm(model.bipedRightArm);
            prepareArm(model.bipedLeftArm);
        } catch (ReflectiveOperationException | LinkageError e) {
            scaleFailureLogged = true;
            BendsLogger.log("Could not correct Draconic Evolution arm mesh scale: " + e, BendsLogger.ERROR);
        }
    }

    private static void prepareArm(ModelRenderer part) throws ReflectiveOperationException {
        if (part == null) return;
        if (part.getClass().getName().equals(MODELS + "ModelRenderOBJ")) {
            if (armScale == null) armScale = new ObjScale(part.getClass(), list -> GL11.glDeleteLists(list, 1));
            armScale.prepare(part, SCALES[2]);
        }
        if (part.childModels != null) {
            for (Object child : part.childModels) prepareArm((ModelRenderer)child);
        }
    }

    /** DE's own fields, not the similarly named private display-list fields inherited from ModelRenderer. */
    static final class ObjScale {
        private final Field scale;
        private final Field compiled;
        private final Field displayList;
        private final IntConsumer deleteList;

        ObjScale(Class<?> type, IntConsumer deleteList) throws NoSuchFieldException {
            this.scale = type.getDeclaredField("scale");
            this.compiled = type.getDeclaredField("compiled");
            this.displayList = type.getDeclaredField("displayList");
            this.scale.setAccessible(true);
            this.compiled.setAccessible(true);
            this.displayList.setAccessible(true);
            this.deleteList = deleteList;
        }

        void prepare(Object mesh, float expected) throws IllegalAccessException {
            if (this.scale.getFloat(mesh) == expected) return;
            if (this.compiled.getBoolean(mesh)) {
                this.deleteList.accept(this.displayList.getInt(mesh));
                this.compiled.setBoolean(mesh, false);
                this.displayList.setInt(mesh, 0);
            }
            this.scale.setFloat(mesh, expected);
        }
    }

    /** Whether the armor worn in render pass {@code pass} (0 helmet to 3 boots) is drawn with such a model. */
    public static boolean hugs(EntityPlayer player, int pass) {
        ItemStack stack = player.inventory.armorItemInSlot(3 - pass);
        return stack != null && armorScales(ForgeHooksClient.getArmorModel(player, stack, pass, null)) != null;
    }
}
