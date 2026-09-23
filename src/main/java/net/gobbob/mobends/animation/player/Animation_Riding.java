package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.minecraft.util.ResourceLocation;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;

public class Animation_Riding
extends Animation {
    @Override
    public String getName() {
        return "riding";
    }

    @Override
    public void animate(EntityLivingBase argEntity, ModelBase argModel, EntityData argData) {
        play((ModelBendsPlayer)argModel, (Data_Player)argData);
    }

    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/ride");

    /**
     * Plays {@code player/ride.animation.json}: {@code ride} sitting still; moving, {@code ride_move} with
     * {@code ride_move_jiggle} (looping on the player's age) over it as strongly as the limbs swing.
     * A missing animation is skipped (the animator logs it).
     */
    public static void play(ModelBendsPlayer model, Data_Player data) {
        boolean moving = !(data.motion.x == 0.0f & data.motion.z == 0.0f);
        if (moving) {
            BendsAnimator.playPose(model, data, FILE, "ride_move", "ride");
            BendsAnimator.playLoop(model, data, FILE, "ride_move_jiggle", "ride_jiggle", Math.max(0, Math.min(1, model.armSwingAmount)));
        } else {
            BendsAnimator.playPose(model, data, FILE, "ride", "ride");
        }
    }
}
