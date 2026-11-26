package net.gobbob.mobends.client.animation;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * Loops one animation on the local player, on top of everything else, for testing it in game; and another on one more
 * entity, for a menu's preview.
 */
public final class AnimationPreview {
    private static ResourceLocation file;
    private static String name;
    private static long startMillis;

    private AnimationPreview() {}

    /**
     * Starts looping {@code name} from {@code file}. The name may be the full animation name or its last part,
     * e.g. {@code item_spin} for {@code animation.mobends.player.item_spin}.
     *
     * @return the full animation name, or null if the file has no such animation.
     */
    public static String start(ResourceLocation file, String name) {
        String resolved = resolve(GeckoLibResources.getBakedAnimations().getAnimations(file, null), name);
        if (resolved == null) return null;
        AnimationPreview.file = file;
        AnimationPreview.name = resolved;
        AnimationPreview.startMillis = Minecraft.getSystemTime();
        return resolved;
    }

    public static void stop() {
        file = null;
        name = null;
    }

    public static boolean isPlaying() {
        return file != null;
    }

    /** A second loop for one other entity, such as a menu's preview player, apart from the command's. */
    private static Entity loopEntity;
    private static ResourceLocation loopFile;
    private static String loopName;
    private static long loopStartMillis;

    /** Starts looping {@code name} from {@code file} on {@code entity}; null if the file has no such animation. */
    public static String loopOn(Entity entity, ResourceLocation file, String name) {
        String resolved = resolve(GeckoLibResources.getBakedAnimations().getAnimations(file, null), name);
        if (resolved == null) return null;
        loopEntity = entity;
        loopFile = file;
        loopName = resolved;
        loopStartMillis = Minecraft.getSystemTime();
        return resolved;
    }

    public static void stopLoop(Entity entity) {
        if (loopEntity == entity) loopEntity = null;
    }

    public static void apply(ModelBendsPlayer model, Data_Player data, Entity entity) {
        if (entity != null && entity == loopEntity) {
            play(model, data, loopFile, loopName, loopStartMillis);
            return;
        }
        if (file == null || entity != Minecraft.getMinecraft().thePlayer) return;
        play(model, data, file, name, startMillis);
    }

    private static void play(ModelBendsPlayer model, Data_Player data, ResourceLocation file, String name, long startMillis) {
        Animation animation = BendsAnimator.find(file, name);
        if (animation == null) return; // Removed by a resource reload; keep the command state until stopped.
        double seconds = (Minecraft.getSystemTime() - startMillis) / 1000.0;
        if (animation.length() > 0) seconds %= animation.length();
        BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(model, data)));
    }

    static String resolve(BakedAnimations animations, String name) {
        if (animations.getAnimation(name) != null) return name;
        for (String candidate : animations.animations().keySet()) {
            if (candidate.endsWith("." + name)) return candidate;
        }
        return null;
    }
}
