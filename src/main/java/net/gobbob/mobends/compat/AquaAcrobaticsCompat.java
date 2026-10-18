package net.gobbob.mobends.compat;

import java.lang.reflect.Method;

import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.entity.player.EntityPlayer;

/** Reads AA's authoritative pose without requiring Aqua Acrobatics at runtime. */
public final class AquaAcrobaticsCompat {
    private static boolean resolved;
    private static boolean failureLogged;
    private static PoseApi api;

    private AquaAcrobaticsCompat() {}

    public static State getState(EntityPlayer player, float partialTicks) {
        if (!CompatibilityPolicy.isAquaAcrobaticsLoaded()) return State.UNAVAILABLE;
        try {
            if (!resolved) {
                resolved = true;
                api = new PoseApi(Class.forName("com.fuzs.aquaacrobatics.entity.player.IPlayerResizeable"));
            }
            if (api == null) return State.UNAVAILABLE;
            boolean otherPose = !player.isEntityAlive() || player.isPlayerSleeping() || player.isRiding()
                || Data_Player.isFlying(player, player.capabilities.isFlying) || EtFuturumRequiemCompat.isElytraFlying(player);
            return api.read(player, partialTicks, player.isInWater(), otherPose);
        } catch (ReflectiveOperationException | LinkageError e) {
            if (!failureLogged) {
                failureLogged = true;
                BendsLogger.log("Could not read Aqua Acrobatics pose: " + e, BendsLogger.ERROR);
            }
            return State.UNAVAILABLE;
        }
    }

    public static final class State {
        public static final State UNAVAILABLE = new State(false, false, false, 0.0f);
        public final boolean available;
        public final boolean prone;
        public final boolean inWater;
        public final float blend;

        private State(boolean available, boolean prone, boolean inWater, float blend) {
            this.available = available;
            this.prone = prone;
            this.inWater = inWater;
            this.blend = Float.isFinite(blend) ? Math.max(0.0f, Math.min(1.0f, blend)) : 0.0f;
        }

        public boolean isActive() {
            // Keep ownership through the entire stand-up transition, not just while the flag is set.
            return this.prone || this.blend > 0.0f;
        }

        static State fromPose(String pose, float blend, boolean inWater, boolean otherPose) {
            // AA's isActuallySwimming() also includes Elytra flight. Do not treat that as crawling.
            if (otherPose || "FALL_FLYING".equals(pose) || "SLEEPING".equals(pose) || "DYING".equals(pose)) {
                return new State(true, false, inWater, 0.0f);
            }
            return new State(true, "SWIMMING".equals(pose), inWater, blend);
        }
    }

    /** Only optional duck methods are reflected; their names do not change in an obfuscated client. */
    static final class PoseApi {
        private final Class<?> playerType;
        private final Method getPose;
        private final Method getSwimAnimation;

        PoseApi(Class<?> playerType) throws NoSuchMethodException {
            this.playerType = playerType;
            this.getPose = playerType.getMethod("getPose");
            this.getSwimAnimation = playerType.getMethod("getSwimAnimation", Float.TYPE);
        }

        State read(Object player, float partialTicks, boolean inWater, boolean otherPose)
            throws ReflectiveOperationException {
            if (!this.playerType.isInstance(player)) return State.UNAVAILABLE;
            Object pose = this.getPose.invoke(player);
            if (!(pose instanceof Enum<?>)) return State.UNAVAILABLE;
            float blend = ((Number)this.getSwimAnimation.invoke(player, partialTicks)).floatValue();
            return State.fromPose(((Enum<?>)pose).name(), blend, inWater, otherPose);
        }
    }
}
