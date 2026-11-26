package net.gobbob.mobends.compat.salamander;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.renderer.entity.RenderBendsPlayer;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

/**
 * What other mods draw on a player's back from the model's root, such as Et Futurum's elytra, follows this mod's bent
 * body: Salamander's body frames (0.0.4 or newer) ask this mod for the body's frame while it draws the player. Reached
 * by reflection, so this mod still runs with older Salamanders, where the elytra keeps vanilla's crouch.
 */
public final class BodyFrame {

    private BodyFrame() {}

    public static void register() {
        try {
            Class<?> frames = Class.forName("industries.angelic.salamander.api.player.BodyFrames");
            Class<?> provider = Class.forName("industries.angelic.salamander.api.player.BodyFrames$Provider");
            Method register = frames.getMethod("register", provider);
            Object proxy = Proxy.newProxyInstance(BodyFrame.class.getClassLoader(), new Class<?>[] { provider },
                (self, method, args) -> {
                    switch (method.getName()) {
                        case "apply":
                            return apply((EntityPlayer)args[0], (Float)args[1]);
                        case "equals":
                            return self == args[0];
                        case "hashCode":
                            return System.identityHashCode(self);
                        case "toString":
                            return "Mo' Bends' body frame";
                        default:
                            return null;
                    }
                });
            register.invoke(null, proxy);
        } catch (ClassNotFoundException olderSalamander) {
            // Salamander 0.0.3 and older: no body frames.
        } catch (ReflectiveOperationException | LinkageError e) {
            BendsLogger.log("Could not register Mo' Bends' body frame with Salamander: " + e, BendsLogger.ERROR);
        }
    }

    /** The body bone's frame, back at the neck where vanilla's body pivots, while this mod draws {@code player}. */
    static boolean apply(EntityPlayer player, float scale) {
        Render render = RenderManager.instance.getEntityRenderObject(player);
        if (!(render instanceof RenderBendsPlayer) || ((RenderBendsPlayer)render).drawing() != player) return false;
        ModelBendsPlayer model = ((RenderBendsPlayer)render).bendsModel();
        if (model == null || !(model.bipedBody instanceof ModelRendererBends)) return false;
        ModelRendererBends body = (ModelRendererBends)model.bipedBody;
        body.postRender(scale);
        GL11.glTranslatef(-body.rotationPointX * scale, -body.rotationPointY * scale, -body.rotationPointZ * scale);
        return true;
    }
}
