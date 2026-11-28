package net.gobbob.mobends.client.render;

import java.nio.FloatBuffer;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import baubles.api.BaublesApi;
import baubles.api.expanded.BaubleExpandedSlots;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.gobbob.mobends.api.WornLanterns;

/**
 * Lanterns worn in a Baubles Expanded belt slot ({@link WornLanterns}), hooked by their chain to the front of the belt's
 * right side. Et Futurum draws its own only through Botania's bauble renderer, so without Botania they were invisible;
 * with Botania, that drawing steps aside for this one. The hook rides the body part, so a torso Mo' Bends bends carries
 * it along, but the lantern hangs down from it. It swings forward with the right thigh as if the thigh pushed it, leans
 * back a little when the player moves, and eases after both, so it trails and settles.
 */
public final class WornLanternRenderer {

    /** Of a block: the hanging lantern, chain included, is then a third of a leg long. */
    private static final float SIZE = 0.4f;
    /** Pixels below the neck: the bottom of the torso, where a belt sits. */
    private static final float BELT = 12.0f;
    /** The body's half width and depth in pixels, and how much a chestplate or leggings widen it. */
    private static final float BODY_SIDE = 4.0f, BODY_FRONT = 2.0f, CHESTPLATE = 1.0f, LEGGINGS = 0.5f;
    /** Pixels in from the side where the hook sits on the belt's front. */
    private static final float HOOK_IN = 1.0f;
    /** Degrees it leans out at rest. */
    private static final float REST_OUT = 6.0f;
    /**
     * The thigh pivots behind the hook, so raised by an angle its front comes this much further forward per squared
     * radian than the lantern turned as far: so much more it turns, plus a margin, to stay in front.
     */
    private static final float PUSH_EXTRA = 0.31f, PUSH_MARGIN = 2.0f, MAX_FORWARD = 70.0f;
    /** Degrees out per degree the thigh is raised, so a raised knee doesn't catch it, and at most. */
    private static final float PUSH_OUT = 0.3f, MAX_OUT = 20.0f;
    /** Degrees it leans back per block per tick of movement, and at most. */
    private static final float LEAN = 30.0f, MAX_LEAN = 8.0f;
    /** How fast it falls back and settles (1/s); the thigh pushes it at once. */
    private static final float FALL = 6.0f;
    /** Supernova tints whole entities by the colored light where they stand, on texture unit 2. */
    private static final boolean SUPERNOVA = Loader.isModLoaded("supernova");

    private final LanternAccess access = new LanternAccess();
    private final RenderBlocks blocks = new RenderBlocks(access);
    private final FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
    private final float[] model = new float[16], leg = new float[16], hook = new float[16];
    private final Map<EntityPlayer, Swing> swings = new WeakHashMap<>();
    private int[] beltSlots;
    /** The General setting: drawn at all. */
    public static boolean enabled = true;

    private WornLanternRenderer() {
        blocks.renderAllFaces = true;
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new WornLanternRenderer());
    }

    @SubscribeEvent
    public void onSpecials(RenderPlayerEvent.Specials.Post event) {
        EntityPlayer player = event.entityPlayer;
        if (!enabled || player.isInvisible()) return;
        IInventory baubles = BaublesApi.getBaubles(player);
        if (baubles == null) return;
        // Assigned while mods load, so asked on the first draw
        if (beltSlots == null)
            beltSlots = BaubleExpandedSlots.getIndexesOfAssignedSlotsOfType(BaubleExpandedSlots.beltType);
        for (int slot : beltSlots) {
            ItemStack stack = slot < baubles.getSizeInventory() ? baubles.getStackInSlot(slot) : null;
            int meta = WornLanterns.hangingMeta(stack);
            if (meta >= 0) {
                draw(event, player, stack, meta);
                return;
            }
        }
    }

    private void draw(RenderPlayerEvent event, EntityPlayer player, ItemStack stack, int meta) {
        Block block = Block.getBlockFromItem(stack.getItem());
        if (block == null || block == Blocks.air) return;
        boolean chestplate = player.getCurrentArmor(2) != null, leggings = player.getCurrentArmor(1) != null;
        float grow = chestplate ? CHESTPLATE : leggings ? LEGGINGS : 0.0f;
        float brightnessX = OpenGlHelper.lastBrightnessX, brightnessY = OpenGlHelper.lastBrightnessY;
        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GL11.glPushMatrix();
        read(model);
        ModelBiped biped = event.renderer.modelBipedMain;
        GL11.glPushMatrix();
        biped.bipedRightLeg.postRender(0.0625f);
        read(leg);
        GL11.glPopMatrix();
        ModelRenderer body = biped.bipedBody;
        body.postRender(0.0625f);
        // Back to the neck, where vanilla's body pivots: Mo' Bends pivots it at the waist
        GL11.glTranslatef(
            -body.rotationPointX * 0.0625f,
            -body.rotationPointY * 0.0625f,
            -body.rotationPointZ * 0.0625f);
        // The hook on the belt's front, near its right side; the model's right is -x and its front -z
        GL11.glTranslatef(-(BODY_SIDE + grow - HOOK_IN) * 0.0625f, BELT * 0.0625f, -(BODY_FRONT + grow) * 0.0625f);
        // Hanging from it as the model stands, whatever the body's pitch or bend
        read(hook);
        Swing swing = swings.get(player);
        if (swing == null) swings.put(player, swing = new Swing());
        swing.update(player, thighRaised());
        for (int column = 0; column < 3; column++) System.arraycopy(model, column * 4, hook, column * 4, 3);
        buffer.clear();
        buffer.put(hook)
            .flip();
        GL11.glLoadMatrix(buffer);
        // Back is +x rotation (the bottom to +z), out is +z rotation (the bottom to -x)
        GL11.glRotatef(-swing.forward, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(swing.out, 0.0f, 0.0f, 1.0f);
        // Upright, the top of its chain on the hook
        GL11.glRotatef(180.0f, 1.0f, 0.0f, 0.0f);
        GL11.glScalef(SIZE, SIZE, SIZE);
        GL11.glTranslatef(-0.5f, -1.0f, -0.5f);
        Minecraft.getMinecraft().renderEngine.bindTexture(TextureMap.locationBlocksTexture);
        if (lighting) GL11.glDisable(GL11.GL_LIGHTING);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0f, 240.0f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        access.block = block;
        access.meta = meta;
        // It lights itself, so its colors stay its own
        boolean tinted = SUPERNOVA && untint();
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        blocks.renderBlockByRenderType(block, 0, 0, 0);
        tessellator.draw();
        if (tinted) retint();
        if (lighting) GL11.glEnable(GL11.GL_LIGHTING);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, brightnessX, brightnessY);
        GL11.glPopMatrix();
    }

    /**
     * Degrees the thigh is raised forward against the body: its down axis in the body's frame (the hook's, whose axes
     * are the body part's). A body leaning over, as flying or swimming, then doesn't read as a raised knee.
     */
    private float thighRaised() {
        // The body's axes are square to each other: dotting with them turns an eye-space direction into the body's
        float x = leg[4], y = leg[5], z = leg[6];
        float down = hook[4] * x + hook[5] * y + hook[6] * z;
        float front = -(hook[8] * x + hook[9] * y + hook[10] * z);
        return Math.max(0.0f, (float) Math.toDegrees(Math.atan2(front, down)));
    }

    /** Turns Supernova's tint off, if it was on. */
    private static boolean untint() {
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2);
        boolean tinted = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
        if (tinted) GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        return tinted;
    }

    private static void retint() {
        OpenGlHelper.setActiveTexture(GL13.GL_TEXTURE2);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private void read(float[] into) {
        buffer.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, buffer);
        buffer.get(into);
    }

    /** A lantern's angles in degrees, each easing toward where the thigh and the player's movement put it. */
    private static final class Swing {

        float forward, out = REST_OUT;
        private long lastNanos = System.nanoTime();

        /** Never behind the thigh: pushed at once, framerate or not, and only eased back as the thigh falls. */
        void update(EntityPlayer player, float thigh) {
            long now = System.nanoTime();
            float fall = Math.min(1.0f, Math.min(0.1f, (now - lastNanos) / 1.0e9f) * FALL);
            lastNanos = now;
            // Leaning back by the speed it moves forward at, against the way the body faces, in blocks per tick
            float yaw = (float) Math.toRadians(player.renderYawOffset);
            float speed = (float) (-(player.posX - player.prevPosX) * MathHelper.sin(yaw)
                + (player.posZ - player.prevPosZ) * MathHelper.cos(yaw));
            forward += (-MathHelper.clamp_float(speed * LEAN, -MAX_LEAN, MAX_LEAN) - forward) * fall;
            out += (REST_OUT - out) * fall;
            if (thigh <= 0.0f) return;
            float radians = (float) Math.toRadians(thigh);
            float pushed = thigh + (float) Math.toDegrees(PUSH_EXTRA * radians * radians) + PUSH_MARGIN;
            forward = Math.max(forward, Math.min(MAX_FORWARD, pushed));
            out = Math.max(out, Math.min(MAX_OUT, REST_OUT + thigh * PUSH_OUT));
        }
    }

    /** A world holding one hanging lantern at the origin, fully lit, for its block renderer. */
    private static final class LanternAccess implements IBlockAccess {

        Block block = Blocks.air;
        int meta;

        private static boolean origin(int x, int y, int z) {
            return x == 0 && y == 0 && z == 0;
        }

        @Override
        public Block getBlock(int x, int y, int z) {
            return origin(x, y, z) ? block : Blocks.air;
        }

        @Override
        public TileEntity getTileEntity(int x, int y, int z) {
            return null;
        }

        @Override
        public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
            return 15 << 20 | 15 << 4;
        }

        @Override
        public int getBlockMetadata(int x, int y, int z) {
            return origin(x, y, z) ? meta : 0;
        }

        @Override
        public int isBlockProvidingPowerTo(int x, int y, int z, int side) {
            return 0;
        }

        @Override
        public boolean isAirBlock(int x, int y, int z) {
            return !origin(x, y, z);
        }

        @Override
        public BiomeGenBase getBiomeGenForCoords(int x, int z) {
            return BiomeGenBase.plains;
        }

        @Override
        public int getHeight() {
            return 256;
        }

        @Override
        public boolean extendedLevelsInChunkCache() {
            return false;
        }

        @Override
        public boolean isSideSolid(int x, int y, int z, ForgeDirection side, boolean _default) {
            return false;
        }
    }
}
