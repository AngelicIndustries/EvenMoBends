package net.gobbob.mobends.compat;

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
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

/**
 * The Aether draws gloves, pendants and shields after the player, from its own copy of the player's transforms, on a
 * biped of its own posed like vanilla: on a bending player they float off the limbs. That biped's parts are swapped
 * for stand-ins. While the Aether draws a player this mod drew this frame, they draw the real part on that player's
 * bones, cut at the joints like armor; anywhere else, such as the first-person glove, they draw it as before.
 * <p>
 * Capes, wings, halos and agility boots are drawn on other models and stay as the Aether places them.
 */
public final class AetherCompat {
    private static final String RENDERER = "com.gildedgames.the_aether.client.renders.entity.PlayerAetherRenderer";
    private static final String[] MODELS = {"modelMisc", "modelGlow"};
    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
    private static final Map<Entity, Drawn> DRAWN = new WeakHashMap<Entity, Drawn>();
    private static final BonedArmorModel BONES = new BonedArmorModel();
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
            for (String name : MODELS) {
                Object model = renderer.getField(name).get(instance);
                if (model instanceof ModelBiped) anchor((ModelBiped)model);
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            BendsLogger.log("The Aether's accessories stay where it puts them: " + e, BendsLogger.ERROR);
            return;
        }
        AetherCompat handler = new AetherCompat();
        MinecraftForge.EVENT_BUS.register(handler);
        FMLCommonHandler.instance().bus().register(handler);
        installed = true;
        BendsLogger.log("Aether detected: its gloves, pendants and shields follow the bones.", BendsLogger.INFO);
    }

    private static void anchor(ModelBiped model) {
        model.bipedHead = new Anchored(model, model.bipedHead, BonedArmorModel.Part.HEAD);
        model.bipedHeadwear = new Anchored(model, model.bipedHeadwear, BonedArmorModel.Part.HEAD);
        model.bipedBody = new Anchored(model, model.bipedBody, BonedArmorModel.Part.BODY);
        // Gloves cover the forearm and hand: cut at the elbow, the cuff's edge would stay on the upper arm.
        model.bipedRightArm = new Anchored(model, model.bipedRightArm, BonedArmorModel.Part.RIGHT_HAND);
        model.bipedLeftArm = new Anchored(model, model.bipedLeftArm, BonedArmorModel.Part.LEFT_HAND);
        model.bipedRightLeg = new Anchored(model, model.bipedRightLeg, BonedArmorModel.Part.RIGHT_LEG);
        model.bipedLeftLeg = new Anchored(model, model.bipedLeftLeg, BonedArmorModel.Part.LEFT_LEG);
    }

    /** {@code entity}'s model is drawn now, on {@code pose}: the frame it is drawn in, for its accessories. */
    public static void drawn(Entity entity, ModelBendsPlayer pose) {
        if (!installed || entity == null || pose == null) return;
        MATRIX.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX);
        float[] matrix = new float[16];
        MATRIX.get(matrix);
        DRAWN.put(entity, new Drawn(frame, matrix, pose));
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

    private static final class Drawn {
        final long frame;
        final float[] matrix;
        final ModelBendsPlayer pose;

        Drawn(long frame, float[] matrix, ModelBendsPlayer pose) {
            this.frame = frame;
            this.matrix = matrix;
            this.pose = pose;
        }
    }

    /** Stands in for one of the Aether biped's parts, which keeps its boxes. */
    private static final class Anchored extends ModelRenderer {
        private final ModelRenderer part;
        private final BonedArmorModel.Part which;

        Anchored(ModelBiped model, ModelRenderer part, BonedArmorModel.Part which) {
            super(model);
            model.boxList.remove(this);
            this.part = part;
            this.which = which;
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
            Drawn drawn = current;
            if (drawn == null) {
                this.part.render(scale);
                return;
            }
            GL11.glPushMatrix();
            try {
                MATRIX.clear();
                MATRIX.put(drawn.matrix).flip();
                GL11.glLoadMatrix(MATRIX);
                BONES.drawOn(drawn.pose, this.part, this.which, scale);
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
