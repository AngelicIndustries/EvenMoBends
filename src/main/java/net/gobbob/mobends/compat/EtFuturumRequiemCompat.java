package net.gobbob.mobends.compat;

import ganymedes01.etfuturum.api.elytra.IElytraPlayer;
import ganymedes01.etfuturum.entities.EntityNewBoat;
import ganymedes01.etfuturum.entities.EntityNewBoatSeat;
import java.util.Map;
import java.util.WeakHashMap;
import net.gobbob.mobends.AnimatedEntity;
import net.gobbob.mobends.client.renderer.entity.RenderRowingBoatEtFuturum;
import net.gobbob.mobends.config.PlayerAnimationConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

/** Optional Et Futurum Requiem state access, isolated from normal class loading. */
public final class EtFuturumRequiemCompat {
    private static boolean boatRenderersRegistered;

    private EtFuturumRequiemCompat() {}

    public static boolean isElytraFlying(EntityPlayer player) {
        return CompatibilityPolicy.isEtFuturumRequiemLoaded()
            && Api.isElytraFlying(player);
    }

    public static BoatState getBoatState(EntityPlayer player, float partialTicks) {
        return CompatibilityPolicy.isEtFuturumRequiemLoaded() ? Api.getBoatState(player, partialTicks) : null;
    }

    /** Et Futurum's zombie villagers render as Mo' Bends zombies while zombies are animated. */
    public static void registerZombieVillagerRenderer() {
        if (CompatibilityPolicy.isEtFuturumRequiemLoaded()) Api.registerZombieVillagerRenderer();
    }

    /** An Et Futurum zombie villager's profession (its villager type), or -1. */
    public static int getZombieVillagerType(Entity entity) {
        return CompatibilityPolicy.isEtFuturumRequiemLoaded() ? Api.getZombieVillagerType(entity) : -1;
    }

    public static void registerBoatRenderers() {
        if (CompatibilityPolicy.isEtFuturumRequiemLoaded() && CompatibilityPolicy.arePlayerAnimationsEnabled()) {
            Api.registerBoatRenderers();
            boatRenderersRegistered = true;
        }
    }

    public static EtFuturumRowingState.Pose getPaddlePose(Entity boat, float partialTicks) {
        return boatRenderersRegistered ? Api.getPaddlePose(boat, partialTicks) : null;
    }

    /** No EFR types escape the optional API boundary. Paddle 0 is on the player's left. */
    public static final class BoatState {
        public final Entity boat;
        public final boolean driver;
        public final boolean raft;
        public final EtFuturumRowingState.Pose paddles;
        public final float rockingDegrees;

        private BoatState(Entity boat, boolean driver, boolean raft, EtFuturumRowingState.Pose paddles, float rockingDegrees) {
            this.boat = boat;
            this.driver = driver;
            this.raft = raft;
            this.paddles = paddles;
            this.rockingDegrees = rockingDegrees;
        }

        public float paddlePhase(int side) {
            return side == 0 ? this.paddles.leftPhase : this.paddles.rightPhase;
        }
    }

    /** This class is only loaded after FML confirms that Et Futurum Requiem is present. */
    private static final class Api {
        private static final Map<Entity, EtFuturumRowingState> ROWING_STATES = new WeakHashMap<>();

        private Api() {}

        private static void registerBoatRenderers() {
            RenderRowingBoatEtFuturum.register();
        }

        private static void registerZombieVillagerRenderer() {
            cpw.mods.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(
                ganymedes01.etfuturum.entities.EntityZombieVillager.class,
                new net.gobbob.mobends.client.renderer.entity.RenderBendsZombieVillagerEtFuturum());
        }

        private static int getZombieVillagerType(Entity entity) {
            return entity instanceof ganymedes01.etfuturum.entities.EntityZombieVillager
                ? ((ganymedes01.etfuturum.entities.EntityZombieVillager)entity).getType() : -1;
        }

        private static boolean hasBendsDriver(Entity entity) {
            if (!(entity instanceof EntityNewBoat) || !PlayerAnimationConfig.isEnabled("riding")
                || !PlayerAnimationConfig.isEnabled("rowing")) return false;
            EntityNewBoat boat = (EntityNewBoat)entity;
            Entity driver = boat.getControllingPassenger();
            if (!(driver instanceof EntityPlayer) || !driver.isEntityAlive()) return false;
            AnimatedEntity animated = AnimatedEntity.getByEntity(driver);
            return animated != null && animated.animate;
        }

        private static EtFuturumRowingState.Pose getPaddlePose(Entity entity, float partialTicks) {
            if (!hasBendsDriver(entity)) {
                ROWING_STATES.remove(entity);
                return null;
            }
            EntityNewBoat boat = (EntityNewBoat)entity;
            EtFuturumRowingState state = ROWING_STATES.get(boat);
            if (state == null) {
                state = new EtFuturumRowingState();
                ROWING_STATES.put(boat, state);
            }
            return state.sample((double)boat.ticksExisted + partialTicks,
                boat.getPaddleState(0), boat.getRowingTime(0, partialTicks),
                boat.getPaddleState(1), boat.getRowingTime(1, partialTicks));
        }

        private static boolean isElytraFlying(EntityPlayer player) {
            return player instanceof IElytraPlayer
                && ((IElytraPlayer)player).etfu$isElytraFlying();
        }

        private static BoatState getBoatState(EntityPlayer player, float partialTicks) {
            Entity mount = player.ridingEntity;
            EntityNewBoat boat = mount instanceof EntityNewBoat ? (EntityNewBoat)mount
                : mount instanceof EntityNewBoatSeat ? ((EntityNewBoatSeat)mount).getBoat() : null;
            if (boat == null) return null;
            float time = boat.getTimeSinceHit() - partialTicks;
            float damage = Math.max(0.0f, boat.getDamageTaken() - partialTicks);
            float rocking = time > 0.0f
                ? MathHelper.sin(time) * time * damage / 10.0f * boat.getForwardDirection() : 0.0f;
            EtFuturumRowingState.Pose paddles = EtFuturumRequiemCompat.getPaddlePose(boat, partialTicks);
            if (paddles == null) {
                paddles = EtFuturumRowingState.Pose.unmodified(boat.getRowingTime(0, partialTicks), boat.getRowingTime(1, partialTicks));
            }
            return new BoatState(boat, boat.getControllingPassenger() == player, boat.isRaft(), paddles, rocking);
        }
    }
}
