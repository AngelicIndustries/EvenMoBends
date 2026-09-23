package net.gobbob.mobends.data;

import java.util.ArrayList;
import java.util.List;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.renderer.SwordTrail;
import net.gobbob.mobends.compat.BackhandCompat;
import net.minecraft.entity.player.EntityPlayer;
import net.gobbob.mobends.data.EntityData;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.client.Minecraft;

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
    public boolean sprintJumpLeg = false;
    public boolean fistPunchArm = false;
    /** Counts empty-hand punches (main and off hand), so a punch thrown again restarts rather than continues. */
    public int punches = 0;
    public int offhandPunches = 0;
    public int currentAttack = 0;
    /** Backhand's off hand: ticks since it last started swinging, and its place in its own sword combo (0 none). */
    public float ticksAfterOffhandPunch = 60.0f;
    public int currentOffhandAttack = 0;
    private boolean alreadyOffhandPunched;

    /** The diving lean along the travel direction, eased (q.mobends_travel_pitch). */
    public float divingTravelPitch;
    private java.util.Map<String, String> slotAnimations;
    private java.util.Map<String, float[]> slotTicks;

    /**
     * Ticks since {@code animation} started playing in {@code slot}. It restarts when the slot switches animation,
     * or when the slot wasn't played for a moment.
     */
    public float ticksPlaying(String slot, String animation) {
        if (this.slotAnimations == null) {
            this.slotAnimations = new java.util.HashMap<String, String>();
            this.slotTicks = new java.util.HashMap<String, float[]>();
        }
        float[] startAndLast = this.slotTicks.get(slot);
        if (startAndLast == null || !animation.equals(this.slotAnimations.get(slot)) || this.ticks - startAndLast[1] > 2.0f) {
            startAndLast = new float[] {this.ticks, this.ticks};
            this.slotTicks.put(slot, startAndLast);
            this.slotAnimations.put(slot, animation);
        }
        startAndLast[1] = this.ticks;
        return this.ticks - startAndLast[0];
    }

    private java.util.Map<String, net.gobbob.mobends.client.animation.Inertia> slotInertia;

    /** The inertialization state of {@code slot}. */
    public net.gobbob.mobends.client.animation.Inertia inertia(String slot) {
        if (this.slotInertia == null) this.slotInertia = new java.util.HashMap<String, net.gobbob.mobends.client.animation.Inertia>();
        return this.slotInertia.computeIfAbsent(slot, s -> new net.gobbob.mobends.client.animation.Inertia());
    }

    public Data_Player(int argEntityID) {
        super(argEntityID);
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

    @Override
    public void update(float argPartialTicks) {
        super.update(argPartialTicks);
        if (this.ticksAfterPunch > 20.0f) {
            this.currentAttack = 0;
        }
        if (this.getEntity() instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer)this.getEntity();
            if (BackhandCompat.getOffSwingProgress(player, argPartialTicks) > 0.0f) {
                if (!this.alreadyOffhandPunched) {
                    this.onOffhandPunch(BackhandCompat.getOffhandItem(player) != null);
                    this.alreadyOffhandPunched = true;
                }
            } else {
                this.alreadyOffhandPunched = false;
            }
        }
        this.ticksAfterOffhandPunch += this.ticksPerFrame;
        if (this.ticksAfterOffhandPunch > 20.0f) {
            this.currentOffhandAttack = 0;
        }
    }

    /** The off hand started a swing: an empty hand punches, anything else goes through the combo like the main hand. */
    private void onOffhandPunch(boolean holding) {
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

