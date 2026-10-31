package net.gobbob.mobends.compat;

import java.lang.reflect.Field;
import java.nio.FloatBuffer;
import java.util.Map;
import java.util.WeakHashMap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.gobbob.mobends.client.model.BonedArmorModel;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.renderer.CapeSway;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * The Aether draws its accessories after the player, from its own copy of the player's transforms, on models of its
 * own posed like vanilla: on a bending player they float off the limbs. Their parts are swapped for stand-ins. While
 * the Aether draws a player this mod drew this frame, they draw the real part on that player's bones; anywhere else,
 * such as the first-person glove, they draw it as before.
 * <ul>
 * <li>Gloves ride the forearms whole, the pendant the body; the shield follows every bone, cut at the joints.
 * <li>The cape hangs from the body bone, swung as this mod's own cape swings, then shrunk as the Aether shrinks it.
 * <li>Wings hang from the body bone and keep their flap.
 * <li>The halo floats above the head bone; agility boots ride the shins, with their wings.
 * </ul>
 */
public final class AetherCompat {
    private static final String RENDERER = "com.gildedgames.the_aether.client.renders.entity.PlayerAetherRenderer";
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final Map<Entity, Drawn> DRAWN = new WeakHashMap<Entity, Drawn>();
    private static final BonedArmorModel BONES = new BonedArmorModel();
    /** The halo, as the Aether lifts it above the head, in blocks. */
    private static final Placement HALO = drawn -> GL11.glTranslatef(0.0f, -0.65f, 0.0f);
    /** Agility boots and their wings, as the Aether lifts them, in blocks. */
    private static final Placement BOOTS = drawn -> GL11.glTranslatef(0.0f, -0.121875f, 0.0f);
    /** What the Aether draws all its accessories at, about the model's origin: a little inside armor. */
    private static final float SHRINK = 0.9375f;

    /**
     * An agility boot, whose wings are painted on its sides, where boots worn as armor are: shrunk and lifted as the
     * Aether draws it, from its pivot 1.8 pixels out where a leg's is 1.9, so they show from just inside the armor
     * instead of fighting it. {@code side}: -1 right, 1 left.
     */
    private static Placement boot(float side) {
        float x = (1.8f * SHRINK - 1.9f) * side, y = (12.0f - 0.121875f * 16.0f) * SHRINK - 12.0f;
        return drawn -> {
            GL11.glTranslatef(x * 0.0625f, y * 0.0625f, 0.0f);
            GL11.glScalef(SHRINK, SHRINK, SHRINK);
        };
    }

    /** The cape from the top of the back: vanilla's swing, then the Aether's narrower, thinner cape. */
    private static final Placement CAPE = drawn -> {
        GL11.glTranslatef(0.0f, 0.0f, 0.125f);
        if (drawn.entity instanceof EntityPlayer) CapeSway.of((EntityPlayer)drawn.entity, drawn.pose.partialTicks()).apply();
        GL11.glRotatef(180.0f, 0.0f, 1.0f, 0.0f);
        GL11.glTranslatef(0.0f, 0.015625f, -0.0625f);
        GL11.glScalef(0.8f, 0.9375f, 0.234375f);
    };
    private static boolean installed;
    private static long frame;
    /** The player whose accessories the Aether is drawing, if this mod drew it. */
    private static Drawn current;

    private AetherCompat() {}

    public static void install() {
        if (installed || !Loader.isModLoaded("aether_legacy")) return;
        try {
            Class<?> renderer = Class.forName(RENDERER);
            Object instance = renderer.getMethod("instance").invoke(null);
            ModelBiped misc = (ModelBiped)field(renderer, instance, "modelMisc");
            anchorBiped(misc, true);
            anchorBiped((ModelBiped)field(renderer, instance, "modelGlow"), false);
            misc.bipedCloak = new Anchored(misc, misc.bipedCloak, BonedArmorModel.Part.BODY, CAPE, true);
            Object wings = field(renderer, instance, "modelWings");
            anchor(wings, "wingLeft", BonedArmorModel.Part.BODY, null, true);
            anchor(wings, "wingRight", BonedArmorModel.Part.BODY, null, true);
            Object halo = field(renderer, instance, "modelHalo");
            for (String part : new String[] {"halo1", "halo2", "halo3", "halo4"})
                anchor(halo, part, BonedArmorModel.Part.HEAD, HALO, false);
            Object boots = field(renderer, instance, "modelAgilityBoots");
            anchor(boots, "boot_R", BonedArmorModel.Part.RIGHT_FOOT, boot(-1.0f), false);
            anchor(boots, "boot_L", BonedArmorModel.Part.LEFT_FOOT, boot(1.0f), false);
            // The boots also draw their wings on their own, from where the Aether's model has its origin.
            for (String part : new String[] {"wing1_R", "wing2_R", "wing1_L", "wing2_L"})
                anchor(boots, part, BonedArmorModel.Part.BODY, BOOTS, true);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            BendsLogger.log("The Aether's accessories stay where it puts them: " + e, BendsLogger.ERROR);
            return;
        }
        AetherCompat handler = new AetherCompat();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
        installed = true;
        BendsLogger.log("Aether detected: its accessories follow the bones.", BendsLogger.INFO);
    }

    private static Object field(Class<?> type, Object instance, String name) throws ReflectiveOperationException {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(instance);
    }

    /** {@code gloves}: the model's arms are only drawn as gloves, the rest of it as a whole. */
    private static void anchorBiped(ModelBiped model, boolean gloves) {
        model.bipedHead = new Anchored(model, model.bipedHead, BonedArmorModel.Part.HEAD, null, false);
        model.bipedHeadwear = new Anchored(model, model.bipedHeadwear, BonedArmorModel.Part.HEAD, null, false);
        model.bipedBody = new Anchored(model, model.bipedBody, BonedArmorModel.Part.BODY, null, false);
        // Gloves cover the forearm and hand: cut at the elbow, the cuff's edge would stay on the upper arm. The
        // shield covers the whole arm and is cut at the elbow like armor.
        BonedArmorModel.Part right = gloves ? BonedArmorModel.Part.RIGHT_HAND : BonedArmorModel.Part.RIGHT_ARM;
        BonedArmorModel.Part left = gloves ? BonedArmorModel.Part.LEFT_HAND : BonedArmorModel.Part.LEFT_ARM;
        model.bipedRightArm = new Anchored(model, model.bipedRightArm, right, null, false);
        model.bipedLeftArm = new Anchored(model, model.bipedLeftArm, left, null, false);
        model.bipedRightLeg = new Anchored(model, model.bipedRightLeg, BonedArmorModel.Part.RIGHT_LEG, null, false);
        model.bipedLeftLeg = new Anchored(model, model.bipedLeftLeg, BonedArmorModel.Part.LEFT_LEG, null, false);
    }

    /** Swaps the part in {@code model}'s public field {@code name} for a stand-in. */
    private static void anchor(Object model, String name, BonedArmorModel.Part which, Placement place, boolean ownPose)
        throws ReflectiveOperationException {
        Field field = model.getClass().getField(name);
        field.set(model, new Anchored((ModelBase)model, (ModelRenderer)field.get(model), which, place, ownPose));
    }

    /** {@code entity}'s model is drawn now, on {@code pose}: the frame it is drawn in, for its accessories. */
    public static void drawn(Entity entity, ModelBendsPlayer pose) {
        if (!installed || entity == null || pose == null) return;
        MATRIX.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        float[] matrix = new float[16];
        MATRIX.get(matrix);
        DRAWN.put(entity, new Drawn(frame, entity, matrix, pose));
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beforeAccessories(RenderLivingEvent.Post event) {
        Drawn drawn = DRAWN.get(event.entity);
        current = drawn != null && drawn.frame == frame ? drawn : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void afterAccessories(RenderLivingEvent.Post event) {
        current = null;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        frame++;
        current = null;
    }

    /** Where an accessory goes from its part's vanilla pivot, as the Aether places it from the vanilla model. */
    private interface Placement {
        void place(Drawn drawn);
    }

    private static final class Drawn {
        final long frame;
        final Entity entity;
        final float[] matrix;
        final ModelBendsPlayer pose;

        Drawn(long frame, Entity entity, float[] matrix, ModelBendsPlayer pose) {
            this.frame = frame;
            this.entity = entity;
            this.matrix = matrix;
            this.pose = pose;
        }
    }

    /** Stands in for one of the Aether's model parts, which keeps its boxes and children. */
    private static final class Anchored extends ModelRenderer {
        private final ModelRenderer part;
        private final BonedArmorModel.Part which;
        private final Placement place;
        /** Whether the part keeps the pivot and turn the Aether gives it, rather than taking the bone's. */
        private final boolean ownPose;

        Anchored(ModelBase model, ModelRenderer part, BonedArmorModel.Part which, Placement place, boolean ownPose) {
            super(model);
            model.boxList.remove(this);
            this.part = part;
            this.which = which;
            this.place = place;
            this.ownPose = ownPose;
            this.setRotationPoint(part.rotationPointX, part.rotationPointY, part.rotationPointZ);
            this.rotateAngleX = part.rotateAngleX;
            this.rotateAngleY = part.rotateAngleY;
            this.rotateAngleZ = part.rotateAngleZ;
            this.offsetX = part.offsetX;
            this.offsetY = part.offsetY;
            this.offsetZ = part.offsetZ;
            this.showModel = part.showModel;
            this.isHidden = part.isHidden;
            this.mirror = part.mirror;
        }

        /** The pose and visibility the Aether gave this stand-in, onto the real part. */
        private void pose() {
            this.part.setRotationPoint(this.rotationPointX, this.rotationPointY, this.rotationPointZ);
            this.part.rotateAngleX = this.rotateAngleX;
            this.part.rotateAngleY = this.rotateAngleY;
            this.part.rotateAngleZ = this.rotateAngleZ;
            this.part.offsetX = this.offsetX;
            this.part.offsetY = this.offsetY;
            this.part.offsetZ = this.offsetZ;
            this.part.showModel = this.showModel;
            this.part.isHidden = this.isHidden;
        }

        @Override
        public void render(float scale) {
            this.pose();
            final Drawn drawn = current;
            if (drawn == null) {
                this.part.render(scale);
                return;
            }
            GL11.glPushMatrix();
            try {
                MATRIX.clear();
                MATRIX.put(drawn.matrix).flip();
                GL11.glLoadMatrix(MATRIX);
                if (this.place == null && !this.ownPose) {
                    BONES.drawOn(drawn.pose, this.part, this.which, scale);
                } else {
                    Runnable local = this.place == null ? null : () -> this.place.place(drawn);
                    BONES.drawAttached(drawn.pose, this.part, this.which, local, this.ownPose, scale);
                }
            } finally {
                GL11.glPopMatrix();
            }
        }

        @Override
        public void renderWithRotation(float scale) {
            if (current == null) {
                this.pose();
                this.part.renderWithRotation(scale);
                return;
            }
            this.render(scale);
        }

        @Override
        public void postRender(float scale) {
            this.pose();
            this.part.postRender(scale);
        }
    }
}
