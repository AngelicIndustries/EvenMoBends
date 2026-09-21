package net.gobbob.mobends.client.animation;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.animation.BoneAnimation;
import com.geckolib.cache.animation.Keyframe;
import com.geckolib.cache.animation.KeyframeStack;
import com.geckolib.loading.math.MolangContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.gobbob.mobends.MoBends;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.util.BendsLogger;
import net.gobbob.mobends.util.SmoothVector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Plays Blockbench/GeckoLib animations on the Mo' Bends player model through Salamander.
 *
 * <p>Bones with motion (more than one keyframe) follow their keyframes closely, so what plays in Blockbench plays in
 * game. Bones that only hold a pose ease into it at Mo' Bends' usual rate, which blends an animation in from
 * whatever the player was doing. Only channels an animation keyframes are applied: bones and axes it leaves out
 * keep whatever the animations before it set.
 */
public final class PlayerAnimator {
    public static final ResourceLocation PUNCH = new ResourceLocation(MoBends.MODID, "player/punch");

    /** Smoothing rate for keyframed motion: none, the parts are exactly where the keyframes say. */
    static final float FOLLOW_RATE = 1000.0f;
    /** The fastest a blend-in eases: exact at 60 fps, before handing over to {@link #FOLLOW_RATE}. */
    static final float BLEND_END_RATE = 3.0f;
    /** Smoothing rate for held poses, as most procedural animations use. */
    static final float EASE_RATE = 0.3f;

    private static final float RAD_TO_DEG = (float)(180.0 / Math.PI);
    private static final Set<String> reportedMissing = new HashSet<String>();

    private PlayerAnimator() {}

    /**
     * Applies an animation from the given file at {@code seconds}.
     *
     * @return false if the file or animation is missing, so the caller can fall back to its Java pose.
     */
    public static boolean play(ModelBendsPlayer model, Data_Player data, ResourceLocation file, String name,
        double seconds) {
        return play(model, data, file, name, seconds, null, 0);
    }

    /**
     * As {@link #play(ModelBendsPlayer, Data_Player, ResourceLocation, String, double)}, blending in over
     * {@code blendSeconds} whenever {@code slot} starts playing this animation, e.g. when swimming switches from
     * moving to idle: bones ease toward it at first, then follow its keyframes.
     */
    public static boolean play(ModelBendsPlayer model, Data_Player data, ResourceLocation file, String name,
        double seconds, String slot, double blendSeconds) {
        Animation animation = find(file, name);
        if (animation == null) return false;
        float maxRate = FOLLOW_RATE;
        if (slot != null && data != null && blendSeconds > 0) {
            float blended = (float)(data.ticksPlaying(slot, file + "#" + name) / (blendSeconds * 20));
            if (blended < 1) maxRate = blendRate(blended);
        }
        apply(model, animation, AnimationProcessor.evaluate(animation, seconds, context(model, data)), maxRate);
        return true;
    }

    /** From the usual ease rate toward following keyframes, over a blend's progress from 0 to 1. */
    static float blendRate(float progress) {
        return progress >= 1 ? FOLLOW_RATE : EASE_RATE + (BLEND_END_RATE - EASE_RATE) * progress * progress;
    }

    public static Animation find(ResourceLocation file, String name) {
        Animation animation = GeckoLibResources.getBakedAnimations().getAnimations(file, null).getAnimation(name);
        if (animation == null && reportedMissing.add(file + "#" + name)) {
            BendsLogger.log("Missing animation " + name + " in " + file + "; using the built-in pose", BendsLogger.INFO);
        }
        return animation;
    }

    /** Molang queries available to player animations. */
    public static MolangContext context(ModelBendsPlayer model, Data_Player data) {
        return variable -> {
            switch (variable) {
                case "query.mobends_head_pitch": return model.headRotationX;
                case "query.mobends_head_yaw": return model.headRotationY;
                case "query.mobends_ticks": return data == null ? 0 : data.ticks;
                case "query.mobends_ticks_after_punch": return data == null ? 0 : data.ticksAfterPunch;
                case "query.mobends_holding": return isHolding(data) ? 1 : 0;
                default: return 0;
            }
        };
    }

    /** Whether the player's main hand holds something (false outside a running world). */
    public static boolean isHolding(Data_Player data) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (data == null || minecraft == null || minecraft.theWorld == null) return false;
        EntityLivingBase entity = data.getEntity();
        return entity instanceof EntityPlayer && ((EntityPlayer)entity).getCurrentEquippedItem() != null;
    }

    /**
     * {@code name + "_holding"} when the hand holds something and {@code animations} has that variant, else
     * {@code name}: an animation can have a version for an occupied hand without every animation needing one.
     */
    public static String variant(BakedAnimations animations, String name, boolean holding) {
        return holding && animations.getAnimation(name + "_holding") != null ? name + "_holding" : name;
    }

    static void apply(ModelBendsPlayer model, Animation animation, Map<String, BoneSnapshot> pose) {
        apply(model, animation, pose, FOLLOW_RATE);
    }

    static void apply(ModelBendsPlayer model, Animation animation, Map<String, BoneSnapshot> pose, float maxRate) {
        Map<String, BoneAnimation> tracks = new HashMap<String, BoneAnimation>();
        for (BoneAnimation track : animation.boneAnimations()) {
            if (pose.containsKey(track.boneName())) tracks.put(track.boneName(), track);
        }
        for (BoneAnimation track : tracks.values()) {
            BoneSnapshot snapshot = pose.get(track.boneName());
            float rate = rate(track, maxRate);
            if (PlayerRig.ROOT.equals(track.boneName())) {
                applyRoot(model, track, snapshot, rate);
                continue;
            }
            Bone bone = Bone.byId(track.boneName());
            if (bone == null || bone.aimed() != null) continue; // Aim bones drive their part's aim below.
            if (bone.holdingArm() != null) {
                applyItem(itemBone(model, bone), bone.parent.equals(PlayerRig.ROOT), track, snapshot, rate);
                continue;
            }
            ModelRendererBends part = bone.part(model);
            KeyframeStack rotation = track.rotationKeyFrames();
            if (animates(rotation)) clearPreRotation(part, rate);
            setRotation(part.rotation, rotation, snapshot, rate);
            if (bone.aim() == null || !tracks.containsKey(bone.aim().id)) setPosition(part, track, snapshot, rate);
            KeyframeStack scale = track.scaleKeyFrames();
            if (has(scale.xKeyframes())) part.scaleX = snapshot.getScaleX();
            if (has(scale.yKeyframes())) part.scaleY = snapshot.getScaleY();
            if (has(scale.zKeyframes())) part.scaleZ = snapshot.getScaleZ();
        }
        for (Bone aim : new Bone[] {Bone.NECK, Bone.RIGHT_ARM_AIM, Bone.LEFT_ARM_AIM}) {
            BoneAnimation aimTrack = tracks.get(aim.id);
            if (aimTrack == null) continue;
            Bone limb = aim.aimed();
            BoneAnimation limbTrack = tracks.get(limb.id);
            float rate = Math.max(rate(aimTrack, maxRate), limbTrack == null ? 0 : rate(limbTrack, maxRate));
            applyAim(limb.part(model), aimTrack, pose.get(aim.id), limbTrack, pose.get(limb.id), rate);
        }
    }

    /**
     * An aim bone turns the part at its pivot before the part's own rotation. Positions compose: the aim's offset,
     * plus the part's offset turned by the aim.
     */
    private static void applyAim(ModelRendererBends part, BoneAnimation aimTrack, BoneSnapshot aimSnapshot,
        BoneAnimation partTrack, BoneSnapshot partSnapshot, float rate) {
        if (animates(aimTrack.rotationKeyFrames())) {
            clearPreRotation(part, rate);
            setRotation(part.aim, aimTrack.rotationKeyFrames(), aimSnapshot, rate);
        }
        KeyframeStack partPosition = partTrack == null ? KeyframeStack.EMPTY : partTrack.positionKeyFrames();
        if (animates(aimTrack.positionKeyFrames()) || animates(partPosition)) {
            float[] aim = {part.aim.vFinal.x, part.aim.vFinal.y, part.aim.vFinal.z};
            double[] turn = EulerZYX.matrix(aim[0], aim[1], aim[2]);
            float[] offset = modelPosition(aimTrack.positionKeyFrames(), aimSnapshot, new float[3]);
            float[] own = modelPosition(partPosition, partSnapshot, new float[3]);
            for (int row = 0; row < 3; ++row) {
                offset[row] += (float)(turn[row * 3] * own[0] + turn[row * 3 + 1] * own[1] + turn[row * 3 + 2] * own[2]);
            }
            part.position.setSmoothX(offset[0], rate);
            part.position.setSmoothY(offset[1], rate);
            part.position.setSmoothZ(offset[2], rate);
        }
    }

    /** A bone's translation moves the part's pivot: Bedrock (x, y, z) pixels is model (x, -y, z). */
    private static void setPosition(ModelRendererBends part, BoneAnimation track, BoneSnapshot snapshot, float rate) {
        KeyframeStack position = track.positionKeyFrames();
        if (has(position.xKeyframes())) part.position.setSmoothX(snapshot.getTranslateX(), rate);
        if (has(position.yKeyframes())) part.position.setSmoothY(-snapshot.getTranslateY(), rate);
        if (has(position.zKeyframes())) part.position.setSmoothZ(snapshot.getTranslateZ(), rate);
    }

    private static float[] modelPosition(KeyframeStack keyframes, BoneSnapshot snapshot, float[] fallback) {
        if (snapshot == null) return fallback;
        if (has(keyframes.xKeyframes())) fallback[0] = snapshot.getTranslateX();
        if (has(keyframes.yKeyframes())) fallback[1] = -snapshot.getTranslateY();
        if (has(keyframes.zKeyframes())) fallback[2] = snapshot.getTranslateZ();
        return fallback;
    }

    /**
     * Item bones: rotation and position as for parts; scale shows or hides the item there. An animated free bone
     * shows the item unless its scale is keyed to 0.
     */
    private static void applyItem(ItemBone item, boolean free, BoneAnimation track, BoneSnapshot snapshot, float rate) {
        setRotation(item.rotation, track.rotationKeyFrames(), snapshot, rate);
        KeyframeStack position = track.positionKeyFrames();
        if (has(position.xKeyframes())) item.position.setSmoothX(snapshot.getTranslateX(), rate);
        if (has(position.yKeyframes())) item.position.setSmoothY(-snapshot.getTranslateY(), rate);
        if (has(position.zKeyframes())) item.position.setSmoothZ(snapshot.getTranslateZ(), rate);
        KeyframeStack scale = track.scaleKeyFrames();
        if (free) {
            item.scaleX = item.scaleY = item.scaleZ = 1;
        }
        if (has(scale.xKeyframes())) item.scaleX = snapshot.getScaleX();
        if (has(scale.yKeyframes())) item.scaleY = snapshot.getScaleY();
        if (has(scale.zKeyframes())) item.scaleZ = snapshot.getScaleZ();
    }

    private static ItemBone itemBone(ModelBendsPlayer model, Bone bone) {
        switch (bone) {
            case RIGHT_ITEM: return model.rightItem;
            case RIGHT_ITEM_FREE: return model.rightItemFree;
            case LEFT_ITEM: return model.leftItem;
            case LEFT_ITEM_FREE: return model.leftItemFree;
            default: throw new IllegalArgumentException(bone.id);
        }
    }

    /** Authored rotations replace procedural pre-rotations, which Blockbench bones don't have. */
    private static void clearPreRotation(ModelRendererBends part, float rate) {
        part.pre_rotation.setSmoothX(0, rate);
        part.pre_rotation.setSmoothY(0, rate);
        part.pre_rotation.setSmoothZ(0, rate);
    }

    /** {@code root} drives the whole-body transform: rotation to renderRotation, position to renderOffset. */
    private static void applyRoot(ModelBendsPlayer model, BoneAnimation track, BoneSnapshot snapshot, float rate) {
        setRotation(model.renderRotation, track.rotationKeyFrames(), snapshot, rate);
        KeyframeStack position = track.positionKeyFrames();
        if (has(position.xKeyframes())) model.renderOffset.setSmoothX(-snapshot.getTranslateX(), rate);
        if (has(position.yKeyframes())) model.renderOffset.setSmoothY(snapshot.getTranslateY(), rate);
        if (has(position.zKeyframes())) model.renderOffset.setSmoothZ(snapshot.getTranslateZ(), rate);
    }

    /** Salamander stores Bedrock rotations in radians with X and Y negated; parts use Bedrock degrees. */
    private static void setRotation(SmoothVector3f target, KeyframeStack keyframes, BoneSnapshot snapshot, float rate) {
        if (has(keyframes.xKeyframes())) target.setSmoothX(nearest(-snapshot.getRotX() * RAD_TO_DEG, target.vSmooth.x), rate);
        if (has(keyframes.yKeyframes())) target.setSmoothY(nearest(-snapshot.getRotY() * RAD_TO_DEG, target.vSmooth.y), rate);
        if (has(keyframes.zKeyframes())) target.setSmoothZ(nearest(snapshot.getRotZ() * RAD_TO_DEG, target.vSmooth.z), rate);
    }

    /**
     * The same single-axis angle, give or take whole turns, closest to where the part is now, so a looping spin
     * wrapping from 360 back to 0 degrees keeps turning forward instead of smoothing back.
     */
    static float nearest(float angle, float current) {
        return angle + 360.0f * Math.round((current - angle) / 360.0f);
    }

    /**
     * Follow a bone that moves (several keyframes, or Molang such as following the look); ease into a bone that only
     * holds a pose. Never faster than {@code maxRate}, which blends a newly started animation in.
     */
    static float rate(BoneAnimation track, float maxRate) {
        return Math.min(maxRate, moves(track) ? FOLLOW_RATE : EASE_RATE);
    }

    private static boolean moves(BoneAnimation track) {
        if (!track.getUsedVariables().isEmpty()) return true;
        for (KeyframeStack stack : new KeyframeStack[] {
            track.rotationKeyFrames(), track.positionKeyFrames(), track.scaleKeyFrames()}) {
            for (Keyframe[] axis : new Keyframe[][] {stack.xKeyframes(), stack.yKeyframes(), stack.zKeyframes()}) {
                if (axis != null && axis.length > 1) return true;
            }
        }
        return false;
    }

    private static boolean animates(KeyframeStack stack) {
        return has(stack.xKeyframes()) || has(stack.yKeyframes()) || has(stack.zKeyframes());
    }

    private static boolean has(Keyframe[] keyframes) {
        return keyframes != null && keyframes.length > 0;
    }
}
