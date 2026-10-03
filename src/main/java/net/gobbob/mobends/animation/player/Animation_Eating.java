package net.gobbob.mobends.animation.player;

import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.animation.Animation;
import net.gobbob.mobends.client.animation.BendsAnimator;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.BackhandCompat;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** A hand-only overlay: walking, sneaking and the other hand continue underneath it. */
public final class Animation_Eating extends Animation {
    public static final ResourceLocation FILE = new ResourceLocation(MoBends.MODID, "player/eating");
    public static final String NAME = "animation.mobends.player.eating";
    public static final double FADE = .15;

    @Override public String getName() { return "eating"; }

    @Override
    public void animate(EntityLivingBase entity, ModelBase model, EntityData data) {
        EntityPlayer player = (EntityPlayer)entity;
        int hand = hand(player);
        if (hand != 0) play((ModelBendsPlayer)model, (Data_Player)data,
            player.getItemInUseDuration() + ((ModelBendsPlayer)model).partialTicks(), hand == 2);
    }

    /** 0 = no consumption, 1 = main hand, 2 = Backhand off hand. Merely holding food never triggers it. */
    public static int hand(EntityPlayer player) {
        if (player == null || !PlayerAnimationConfig.isEnabled("eating")) return 0;
        ItemStack item = player.getItemInUse();
        return hand(item == null ? null : item.getItemUseAction(), player.getItemInUseCount(), BackhandCompat.isOffhandItemInUse(player));
    }

    public static int hand(EnumAction action, int remainingTicks, boolean offhand) {
        return remainingTicks > 0 && (action == EnumAction.eat || action == EnumAction.drink) ? (offhand ? 2 : 1) : 0;
    }

    public static void play(ModelBendsPlayer model, Data_Player data, float useTicks, boolean offhand) {
        BendsAnimator.playOnce(model, data, FILE, NAME, useTicks / 20.0, offhand ? "eatingLeft" : "eatingRight", FADE, null, offhand);
    }
}
