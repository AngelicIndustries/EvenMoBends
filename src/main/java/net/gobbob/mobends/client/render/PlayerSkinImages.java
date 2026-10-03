package net.gobbob.mobends.client.render;

import cpw.mods.fml.relauncher.ReflectionHelper;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.ITextureObject;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import org.fentanylsolutions.salamander.cem.client.CemSkinImage;

/** Read original skin pixels for the editor and Blockbench export, without generated expression textures. */
public final class PlayerSkinImages {
    private static boolean wawelReflectionResolved;
    private static Method wawelGetCachedImage;
    private PlayerSkinImages() {}

    public static BufferedImage copyPlayerSkin(AbstractClientPlayer player) {
        if (player == null) return null;
        ResourceLocation location = player.getLocationSkin();
        ITextureObject texture = Minecraft.getMinecraft().getTextureManager().getTexture(location);
        return CemSkinImage.copy(getSourceImage(location, texture));
    }

    private static BufferedImage getSourceImage(ResourceLocation location, ITextureObject textureObject) {
        BufferedImage image = textureObject instanceof org.fentanylsolutions.salamander.cem.client.CemSkinTexture
            ? ((org.fentanylsolutions.salamander.cem.client.CemSkinTexture)textureObject).salamander$skinImage() : null;
        if (image == null) image = getBufferedImageField(textureObject);
        if (image != null) {
            return image;
        }

        image = getWawelCachedImage(location);
        if (image != null) {
            return image;
        }

        if (textureObject instanceof DynamicTexture) {
            int[] pixels = ((DynamicTexture)textureObject).getTextureData();
            int side = (int)Math.sqrt(pixels.length);
            if (side * side == pixels.length && side >= 64) {
                BufferedImage dynamicImage = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
                dynamicImage.setRGB(0, 0, side, side, pixels, 0, side);
                return dynamicImage;
            }
        }

        if (textureObject == null) {
            return null;
        }
        IResource resource = null;
        try {
            resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            InputStream input = resource.getInputStream();
            try {
                image = ImageIO.read(input);
            } finally {
                input.close();
            }
            return image == null ? null : image;
        } catch (IOException ignored) {
            return null;
        }
    }

    private static BufferedImage getBufferedImageField(ITextureObject textureObject) {
        if (textureObject == null) {
            return null;
        }
        if (textureObject instanceof ThreadDownloadImageData) {
            try {
                return ReflectionHelper.getPrivateValue(
                    ThreadDownloadImageData.class,
                    (ThreadDownloadImageData)textureObject,
                    "bufferedImage",
                    "field_110560_d");
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        Class<?> type = textureObject.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("bufferedImage");
                field.setAccessible(true);
                Object value = field.get(textureObject);
                return value instanceof BufferedImage ? (BufferedImage)value : null;
            } catch (ReflectiveOperationException ignored) {
                type = type.getSuperclass();
            }
        }
        return null;
    }

    private static BufferedImage getWawelCachedImage(ResourceLocation location) {
        if (!wawelReflectionResolved) {
            wawelReflectionResolved = true;
            try {
                Class<?> loader = Class.forName("org.fentanylsolutions.wawelauth.client.render.LocalTextureLoader");
                wawelGetCachedImage = loader.getMethod("getCachedImage", ResourceLocation.class);
            } catch (ReflectiveOperationException ignored) {
                wawelGetCachedImage = null;
            }
        }
        if (wawelGetCachedImage == null) {
            return null;
        }
        try {
            Object image = wawelGetCachedImage.invoke(null, location);
            return image instanceof BufferedImage ? (BufferedImage)image : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

}
