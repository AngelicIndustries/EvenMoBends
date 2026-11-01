package net.gobbob.mobends.compat.adventurebackpack;

import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import com.darkona.adventurebackpack.client.render.RendererWearableEquipped;
import com.darkona.adventurebackpack.common.Constants;
import com.darkona.adventurebackpack.item.IBackWearableItem;
import com.darkona.adventurebackpack.item.ItemCoalJetpack;
import com.darkona.adventurebackpack.item.ItemCopterPack;
import com.darkona.adventurebackpack.playerProperties.BackpackProperty;
import com.darkona.adventurebackpack.proxy.ClientProxy;
import com.darkona.adventurebackpack.util.Wearing;

import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.AdventureBackpackCompat;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Adventure Backpack's renderer for what players wear on their backs, drawing on the body bone of players this mod
 * draws. Its models place the wearable from their own body, which they pose as vanilla does, sneaking included: that
 * pose is undone on the bone, which already carries this mod's.
 */
public final class BackWearableRenderer extends RendererWearableEquipped {
    /** The scale Adventure Backpack draws its models at. */
    private static final float SCALE = 0.0625f;
    private static final float DEGREES = 180.0f / (float)Math.PI;

    public static void install() {
        ClientProxy.rendererWearableEquipped = new BackWearableRenderer();
    }

    @Override
    public void render(Entity entity, double x, double y, double z, float rotX, float rotY, float rotZ, float yaw,
        float pitch) {
        ModelBendsPlayer pose = AdventureBackpackCompat.pose(entity);
        ModelBiped model = pose == null ? null : model(entity);
        if (model == null) {
            super.render(entity, x, y, z, rotX, rotY, rotZ, yaw, pitch);
            return;
        }
        GL11.glPushMatrix();
        try {
            // From the model's root to the body bone, at the vanilla body's pivot.
            Vector3f rest = pose.geometry().restPivot("body");
            pose.geometry().attach("body", SCALE);
            GL11.glTranslatef(-rest.x * SCALE, -rest.y * SCALE, -rest.z * SCALE);
            undoBody(model, entity, rotX, rotY, rotZ);
            super.render(entity, x, y, z, rotX, rotY, rotZ, yaw, pitch);
        } finally {
            GL11.glPopMatrix();
        }
    }

    /** Whether {@code player} flies on what it wears: a copter pack that is running, or a coal jetpack giving thrust. */
    public static boolean flies(EntityPlayer player) {
        ItemStack wearable = wearable(player);
        if (wearable == null || !wearable.hasTagCompound()) return false;
        NBTTagCompound state = wearable.getTagCompound().getCompoundTag(Constants.TAG_WEARABLE_COMPOUND);
        if (wearable.getItem() instanceof ItemCopterPack) {
            return state.getByte(Constants.Copter.TAG_STATUS) != ItemCopterPack.OFF_MODE;
        }
        return wearable.getItem() instanceof ItemCoalJetpack && state.getBoolean("inUse");
    }

    /** What {@code player} wears on its back, or null. */
    private static ItemStack wearable(EntityPlayer player) {
        return BackpackProperty.get(player) == null ? null : Wearing.getWearingWearable(player);
    }

    /** The model Adventure Backpack draws what {@code entity} wears on its back with, or null when it wears nothing. */
    private static ModelBiped model(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return null;
        ItemStack wearable = wearable((EntityPlayer)entity);
        if (wearable == null || !(wearable.getItem() instanceof IBackWearableItem)) return null;
        return ((IBackWearableItem)wearable.getItem()).getWearableModel(wearable.copy());
    }

    /**
     * Undoes the body transform {@code model} is about to draw the wearable with: posed as its draw poses it, then
     * moved to its body's pivot and offset and turned by its angles, as each of its models does.
     */
    private static void undoBody(ModelBiped model, Entity entity, float rotX, float rotY, float rotZ) {
        ModelRenderer body = model.bipedBody;
        body.rotateAngleX = rotX;
        body.rotateAngleY = rotY;
        body.rotateAngleZ = rotZ;
        model.isSneak = entity.isSneaking();
        model.setRotationAngles(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, SCALE, entity);
        GL11.glRotatef(-body.rotateAngleX * DEGREES, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(-body.rotateAngleY * DEGREES, 0.0f, 1.0f, 0.0f);
        GL11.glRotatef(-body.rotateAngleZ * DEGREES, 0.0f, 0.0f, 1.0f);
        GL11.glTranslatef(-body.rotationPointX * SCALE - body.offsetX, -body.rotationPointY * SCALE - body.offsetY,
            -body.rotationPointZ * SCALE - body.offsetZ);
    }
}
