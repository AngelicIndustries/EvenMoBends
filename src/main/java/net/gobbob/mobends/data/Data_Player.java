package net.gobbob.mobends.data;

import java.util.ArrayList;
import java.util.List;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.renderer.SwordTrail;
import net.gobbob.mobends.client.animation.HandAction;
import net.gobbob.mobends.compat.BackhandCompat;
import net.gobbob.mobends.compat.EtFuturumRequiemCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;

public class Data_Player
extends EntityData {
    public static List<Data_Player> dataList = new ArrayList<Data_Player>();
    public ModelRendererBends head;
    public ModelRendererBends headwear;
    public ModelRendererBends body;
    public ModelRendererBends rightArm;
    public ModelRendererBends leftArm;
    public ModelRendererBends rightLeg;
    public ModelRendererBends leftLeg;
    public ModelRendererBends ears;
    public ModelRendererBends cloak;
    public ModelRendererBends rightForeArm;
    public ModelRendererBends leftForeArm;
    public ModelRendererBends rightForeLeg;
    public ModelRendererBends leftForeLeg;
    public SmoothVector3f renderOffset = new SmoothVector3f();
    public SmoothVector3f renderRotation = new SmoothVector3f();
    public SmoothVector3f renderItemRotation = new SmoothVector3f();
    public final ItemBone rightItem = new ItemBone(false);
    public final ItemBone rightItemFree = new ItemBone(true);
    public final ItemBone leftItem = new ItemBone(false);
    public final ItemBone leftItemFree = new ItemBone(true);
    public SwordTrail swordTrail = new SwordTrail();
    public SwordTrail offhandTrail = new SwordTrail();
    public boolean sprintJumpLeg = false;
    public boolean fistPunchArm = false;
    /** Counts empty-hand punches (main and off hand), so a punch thrown again restarts rather than continues. */
    public int punches = 0;
    public int offhandPunches = 0;
    /**
     * When each fist (right, left) last started a punch, the last punch of each hand seen by the animation, and
     * whether it starts by pulling the fist back in.
     */
    public final float[] fistPunchedAt = {-1000.0f, -1000.0f};
    public int seenPunches = -1, seenOffhandPunches = -1;
    public boolean punchRetracts, offhandPunchRetracts;
    /**
     * What each hand's last swing was for, as it started (see HandAction): whether that is known (only for the local
     * player), a use such as placing a block, or breaking a block.
     */
    public boolean swingKnown, swingIsUse, swingBreaks;
    public boolean offhandSwingKnown, offhandSwingIsUse, offhandSwingBreaks;
    public int currentAttack = 0;
    /** Backhand's off hand: ticks since it last started swinging, and its place in its own sword combo (0 none). */
    public float ticksAfterOffhandPunch = 60.0f;
    /** Ticks since the off hand last started any swing (mining or chopping swings keep restarting it). */
    public float ticksAfterOffhandSwing = 60.0f;
    public int currentOffhandAttack = 0;
    private float lastOffhandSwingProgress;

    /** The diving lean along the travel direction, eased (q.mobends_travel_pitch). */
    public float divingTravelPitch;
    /** The water swim (not Aqua Acrobatics') played this frame. */
    public boolean swimming;
    /** How far the drawn model reached ahead of and behind the player this frame, in blocks along the facing. */
    public float reachFront, reachBack;
    /** The same when last lying in the stroke: what lying down needs, while the swim is upright. */
    public float strokeFront = 1.3f, strokeBack = 0.7f;
    /** How far back the swimming model is drawn, away from blocks ahead; eased. */
    public float swimShift;
    /** No room here to lie in the stroke: the swim stays upright. */
    public boolean swimBoxedIn;

    public Data_Player(int argEntityID) {
        super(argEntityID);
    }

    private final RemotePlayerLanding remoteLanding = new RemotePlayerLanding();

    @Override
    public boolean calcOnGround() {
        boolean grounded = super.calcOnGround();
        if (this.getEntity() instanceof EntityOtherPlayerMP) {
            EntityOtherPlayerMP player = (EntityOtherPlayerMP)this.getEntity();
            boolean eligible = player.isEntityAlive() && !player.isPlayerSleeping() && !player.isInWater()
                && !player.isOnLadder() && !player.isRiding() && !this.isFlying(player)
                && !EtFuturumRequiemCompat.isElytraFlying(player);
            this.remoteLanding.update(player, eligible, box -> player.worldObj.func_147461_a(box));
            grounded |= this.remoteLanding.isGrounded();
        }
        return grounded;
    }

    /** World-space drawing correction only: never changes the player's position, hitbox, or packets. */
    public double landingRenderOffset(float partialTicks) {
        return this.remoteLanding.renderOffset(partialTicks);
    }

    public boolean hasLandingContact() {
        return this.remoteLanding.isGrounded();
    }

    public void syncModelInfo(ModelBendsPlayer argModel) {
        if (this.head == null) {
            this.head = new ModelRendererBends(argModel);
        }
        this.head.sync((ModelRendererBends)argModel.bipedHead);
        if (this.headwear == null) {
            this.headwear = new ModelRendererBends(argModel);
        }
        this.headwear.sync((ModelRendererBends)argModel.bipedHeadwear);
        if (this.body == null) {
            this.body = new ModelRendererBends(argModel);
        }
        this.body.sync((ModelRendererBends)argModel.bipedBody);
        if (this.rightArm == null) {
            this.rightArm = new ModelRendererBends(argModel);
        }
        this.rightArm.sync((ModelRendererBends)argModel.bipedRightArm);
        if (this.leftArm == null) {
            this.leftArm = new ModelRendererBends(argModel);
        }
        this.leftArm.sync((ModelRendererBends)argModel.bipedLeftArm);
        if (this.rightLeg == null) {
            this.rightLeg = new ModelRendererBends(argModel);
        }
        this.rightLeg.sync((ModelRendererBends)argModel.bipedRightLeg);
        if (this.leftLeg == null) {
            this.leftLeg = new ModelRendererBends(argModel);
        }
        this.leftLeg.sync((ModelRendererBends)argModel.bipedLeftLeg);
        if (this.rightForeArm == null) {
            this.rightForeArm = new ModelRendererBends(argModel);
        }
        this.rightForeArm.sync((ModelRendererBends)argModel.bipedRightForeArm);
        if (this.leftForeArm == null) {
            this.leftForeArm = new ModelRendererBends(argModel);
        }
        this.leftForeArm.sync((ModelRendererBends)argModel.bipedLeftForeArm);
        if (this.rightForeLeg == null) {
            this.rightForeLeg = new ModelRendererBends(argModel);
        }
        this.rightForeLeg.sync((ModelRendererBends)argModel.bipedRightForeLeg);
        if (this.leftForeLeg == null) {
            this.leftForeLeg = new ModelRendererBends(argModel);
        }
        this.leftForeLeg.sync((ModelRendererBends)argModel.bipedLeftForeLeg);
        this.renderOffset.set(argModel.renderOffset);
        this.renderRotation.set(argModel.renderRotation);
        this.renderItemRotation.set(argModel.renderItemRotation);
        this.rightItem.set(argModel.rightItem);
        this.rightItemFree.set(argModel.rightItemFree);
        this.leftItem.set(argModel.leftItem);
        this.leftItemFree.set(argModel.leftItemFree);
        this.swordTrail = argModel.swordTrail;
        this.offhandTrail = argModel.offhandTrail;
    }

    public static void add(Data_Player argData) {
        dataList.add(argData);
    }

    public static Data_Player get(int argEntityID) {
        for (int i = 0; i < dataList.size(); ++i) {
            if (Data_Player.dataList.get((int)i).entityID != argEntityID) continue;
            return dataList.get(i);
        }
        Data_Player newData = new Data_Player(argEntityID);
        if (Minecraft.getMinecraft().theWorld.getEntityByID(argEntityID) != null) {
            dataList.add(newData);
        }
        return newData;
    }

    /** Whether {@code player} is in creative flight: its own ability, which other players' clients are never sent. */
    public boolean isFlying(EntityPlayer player) {
        if (player == Minecraft.getMinecraft().thePlayer) return player.capabilities.isFlying;
        return this.steadyAirTicks >= STEADY_FLIGHT_TICKS && this.aloftTicks >= ALOFT_FLIGHT_TICKS;
    }

    /** A remote landing can last only one packet interval; do not spend all of it holding the airborne pose. */
    public boolean shouldAnimateAirborne(boolean groundedInWater, boolean remote) {
        return !this.isOnGround() || !remote && !groundedInWater && this.ticksAfterTouchdown < 2.0f;
    }

    /** As above, for any player. */
    public static boolean isFlying(EntityPlayer player, boolean fallback) {
        Data_Player data = get(player.getEntityId());
        return data != null ? data.isFlying(player) : fallback;
    }

    /**
     * Another player flies when it stays well off the ground without falling: gravity changes the vertical speed by
     * about 0.08 a tick even at a jump's top, while flight holds it steady, still or rising and sinking. Its position
     * arrives rounded and smoothed, so it may float a hair above the ground standing, and a jump's top can look level
     * for a few ticks: it must be clear of the ground for longer than a jump lasts.
     */
    private void sampleFlight(EntityPlayer player) {
        if (player.ticksExisted == this.flightSampleTick) return;
        this.flightSampleTick = player.ticksExisted;
        double speed = player.posY - player.lastTickPosY;
        boolean aloft = !this.isOnGround() && !player.isInWater() && !player.isOnLadder() && !player.isRiding() && !this.bouncedOn(player.ticksExisted)
            && player.worldObj.func_147461_a(player.boundingBox.copy().addCoord(0.0, -GROUND_MARGIN, 0.0)).isEmpty();
        this.aloftTicks = aloft ? this.aloftTicks + 1 : 0;
        boolean steady = aloft && speed > -1.0 && Math.abs(speed - this.lastVerticalSpeed) < 0.04;
        this.steadyAirTicks = steady ? this.steadyAirTicks + 1 : 0;
        this.lastVerticalSpeed = speed;
    }

    /** Ticks of steady vertical speed, and of being clear of the ground (a jump lasts about 12), before it is flight. */
    private static final int STEADY_FLIGHT_TICKS = 4, ALOFT_FLIGHT_TICKS = 20;
    /** How far above the ground another player still counts as standing, in blocks. */
    private static final double GROUND_MARGIN = 0.1;
    private int flightSampleTick = Integer.MIN_VALUE;
    private double lastVerticalSpeed;
    private int steadyAirTicks, aloftTicks;

    @Override
    public void update(float argPartialTicks) {
        super.update(argPartialTicks);
        if (this.ticksAfterPunch > 20.0f) {
            this.currentAttack = 0;
        }
        if (this.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)this.getEntity();
            this.sampleFlight(player);
            // A swing starts when the progress leaves 0, or drops back while swinging: Backhand interpolates a held
            // swing's restart up through 1 rather than back through 0, so it never reads 0 between held swings.
            float progress = BackhandCompat.getOffSwingProgress(player, argPartialTicks);
            if (progress > 0.0f && (this.lastOffhandSwingProgress <= 0.0f || progress < this.lastOffhandSwingProgress - 0.3f)) {
                this.onOffhandPunch(BackhandCompat.getOffhandItem(player) != null);
            }
            this.lastOffhandSwingProgress = progress;
        }
        this.ticksAfterOffhandPunch += this.ticksPerFrame;
        this.ticksAfterOffhandSwing += this.ticksPerFrame;
        if (this.ticksAfterOffhandPunch > 20.0f) {
            this.currentOffhandAttack = 0;
        }
    }

    /** The off hand started a swing: an empty hand punches, anything else goes through the combo like the main hand. */
    private void onOffhandPunch(boolean holding) {
        EntityPlayer player = (EntityPlayer)this.getEntity();
        this.offhandSwingKnown = HandAction.isKnown(player);
        this.offhandSwingIsUse = HandAction.isUse(player);
        this.offhandSwingBreaks = HandAction.isBreakingWithOffhand(player);
        this.ticksAfterOffhandSwing = 0.0f;
        if (!holding) {
            this.ticksAfterOffhandPunch = 0.0f;
            ++this.offhandPunches;
        } else if (this.currentOffhandAttack == 0) {
            this.currentOffhandAttack = 1;
            this.ticksAfterOffhandPunch = 0.0f;
        } else if (this.ticksAfterOffhandPunch > 6.0f && this.ticksAfterOffhandPunch < 15.0f) {
            this.currentOffhandAttack = this.currentOffhandAttack == 1 ? 2
                : this.currentOffhandAttack == 2 ? (this.getEntity().isRiding() ? 1 : 3) : 1;
            this.ticksAfterOffhandPunch = 0.0f;
        }
    }

    @Override
    public void onLiftoff() {
        super.onLiftoff();
        this.sprintJumpLeg = !this.sprintJumpLeg;
    }

    @Override
    public void onPunch() {
        if (this.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)this.getEntity();
            this.swingKnown = HandAction.isKnown(player);
            this.swingIsUse = HandAction.isUse(player);
            this.swingBreaks = HandAction.isBreakingBlock(player);
        }
        if (this.getEntity().getHeldItem() != null) {
            if (this.ticksAfterPunch > 6.0f) {
                if (this.currentAttack == 0) {
                    this.currentAttack = 1;
                    this.ticksAfterPunch = 0.0f;
                } else if (this.ticksAfterPunch < 15.0f) {
                    if (this.currentAttack == 1) {
                        this.currentAttack = 2;
                    } else if (this.currentAttack == 2) {
                        this.currentAttack = this.getEntity().isRiding() ? 1 : 3;
                    } else if (this.currentAttack == 3) {
                        this.currentAttack = 1;
                    }
                    this.ticksAfterPunch = 0.0f;
                }
            }
        } else {
            this.fistPunchArm = !this.fistPunchArm;
            this.ticksAfterPunch = 0.0f;
            ++this.punches;
        }
    }
}

