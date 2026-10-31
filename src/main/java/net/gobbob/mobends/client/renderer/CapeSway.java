package net.gobbob.mobends.client.renderer;

import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

/**
 * Vanilla's cape swing, as RenderPlayer works it out from where the cape trails behind the player: lifted by the
 * fall or the climb, by walking speed and stride, pushed back by speed, swung sideways by turning. Creative flight
 * flutters it instead. For this mod's own cape and for other mods' capes hung from the body bone.
 */
public final class CapeSway {
    /** Lifted, in degrees: falling, climbing, stride and sneaking. */
    public final float lift;
    /** Pushed back by speed, in degrees, never forward. */
    public final float back;
    /** Swung sideways by turning, in degrees. */
    public final float side;
    public final boolean creativeFlying;
    private final float age;

    private CapeSway(float lift, float back, float side, boolean creativeFlying, float age) {
        this.lift = lift;
        this.back = back;
        this.side = side;
        this.creativeFlying = creativeFlying;
        this.age = age;
    }

    public static CapeSway of(EntityPlayer player, float partialTicks) {
        double dx = player.field_71091_bM + (player.field_71094_bP - player.field_71091_bM) * (double)partialTicks - (player.prevPosX + (player.posX - player.prevPosX) * (double)partialTicks);
        double dy = player.field_71096_bN + (player.field_71095_bQ - player.field_71096_bN) * (double)partialTicks - (player.prevPosY + (player.posY - player.prevPosY) * (double)partialTicks);
        double dz = player.field_71097_bO + (player.field_71085_bR - player.field_71097_bO) * (double)partialTicks - (player.prevPosZ + (player.posZ - player.prevPosZ) * (double)partialTicks);
        float yaw = player.prevRenderYawOffset + (player.renderYawOffset - player.prevRenderYawOffset) * partialTicks;
        double sin = MathHelper.sin(yaw * (float)Math.PI / 180.0f);
        double cos = -MathHelper.cos(yaw * (float)Math.PI / 180.0f);
        float lift = MathHelper.clamp_float((float)dy * 10.0f, -6.0f, 32.0f);
        float back = Math.max(0.0f, (float)(dx * sin + dz * cos) * 100.0f);
        float side = (float)(dx * cos - dz * sin) * 100.0f;
        float bob = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * partialTicks;
        lift += MathHelper.sin((player.prevDistanceWalkedModified + (player.distanceWalkedModified - player.prevDistanceWalkedModified) * partialTicks) * 6.0f) * 32.0f * bob;
        if (player.isSneaking()) lift += 25.0f;
        Data_Player flight = Data_Player.get(player.getEntityId());
        boolean creativeFlying = PlayerAnimationConfig.isEnabled("flying")
            && flight != null && flight.isFlying(player)
            && !flight.isOnGround()
            && !EtFuturumRequiemCompat.isElytraFlying(player);
        return new CapeSway(lift, back, side, creativeFlying, player.ticksExisted + partialTicks);
    }

    /** Turns the cape matrix at the top of the back by this swing, as vanilla does, or by the flight's flutter. */
    public void apply() {
        if (this.creativeFlying) {
            float flutter = MathHelper.sin(this.age * 0.15f) * 2.0f;
            float sideSway = MathHelper.clamp_float(this.side * 0.15f, -8.0f, 8.0f);
            GL11.glRotatef(8.0f + flutter, 1.0f, 0.0f, 0.0f);
            GL11.glRotatef(sideSway, 0.0f, 0.0f, 1.0f);
            GL11.glRotatef(-sideSway, 0.0f, 1.0f, 0.0f);
            return;
        }
        GL11.glRotatef(6.0f + this.back / 2.0f + this.lift, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(this.side / 2.0f, 0.0f, 0.0f, 1.0f);
        GL11.glRotatef(-this.side / 2.0f, 0.0f, 1.0f, 0.0f);
    }
}
