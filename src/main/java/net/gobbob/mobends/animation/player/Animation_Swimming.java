package net.gobbob.mobends.animation.player;

import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;

public class Animation_Swimming
extends net.gobbob.mobends.animation.Animation {
    @Override
    public String getName() {
        return "swimming";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        ModelBendsPlayer model = (ModelBendsPlayer)argModel;
        Data_Player data = (Data_Player)argData;
        play(model, data, data.motion.x != 0.0f || data.motion.z != 0.0f);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/swim");

    /**
     * Plays the Blockbench swim from {@code player/swim.animation.json}: {@code swim} / {@code swim_idle} for the
     * upper body and {@code swim_legs} / {@code swim_idle_legs} for the kick, which loops at its own speed. While the
     * hand holds something, a {@code _holding} version of each is used if the file has one.
     * A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data, boolean moving) {
        String upper = moving ? "swim" : "swim_idle";
        boolean holding = BendsAnimator.isHolding(data);
        loop(model, data, upper, "swim_upper", holding);
        loop(model, data, upper + "_legs", "swim_legs", holding);
    }

    private static boolean loop(ModelBendsPlayer model, Data_Player data, String name, String slot, boolean holding) {
        String animation = BendsAnimator.variant(GeckoLibResources.getBakedAnimations().getAnimations(FILE, null),
            "animation.mobends.player." + name, holding);
        Animation loaded = BendsAnimator.find(FILE, animation);
        if (loaded == null) return false;
        double seconds = loaded.length() > 0 ? (data.ticks / 20.0) % loaded.length() : 0;
        // Blend when switching between moving and idle or empty and holding, as the Java swim eased between them.
        return BendsAnimator.play(model, data, FILE, animation, seconds, slot, 0.5);
    }
}
