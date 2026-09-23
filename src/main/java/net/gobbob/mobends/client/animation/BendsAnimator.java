package net.gobbob.mobends.client.animation;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ControllerState;
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
import net.gobbob.mobends.compat.BackhandCompat;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.data.EntityData;
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
public final class BendsAnimator {
    public static final ResourceLocation PUNCH = new ResourceLocation(MoBends.MODID, "player/punch");

    /** Smoothing rate for keyframed motion: none, the parts are exactly where the keyframes say. */
    static final float FOLLOW_RATE = 1000.0f;
    /** The fastest a blend-in eases: exact at 60 fps, before handing over to {@link #FOLLOW_RATE}. */
    static final float BLEND_END_RATE = 3.0f;
    /** Smoothing rate for held poses, as most procedural animations use. */
    static final float EASE_RATE = 0.3f;

    private static final float RAD_TO_DEG = (float)(180.0 / Math.PI);
    private static final Set<String> reportedMissing = new HashSet<String>();

    private BendsAnimator() {}

    /**
     * Applies an animation from the given file at {@code seconds}.
     *
     * @return false if the file or animation is missing, so the caller can fall back to its Java pose.
     */
    public static boolean play(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds) {
        return play(model, data, file, name, seconds, null, 0);
    }

    /**
     * As {@link #play(ModelBendsPlayer, Data_Player, ResourceLocation, String, double)}, blending in over
     * {@code blendSeconds} whenever {@code slot} starts playing this animation, e.g. when swimming switches from
     * moving to idle: bones ease toward it at first, then follow its keyframes.
     */
    public static boolean play(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds, String slot, double blendSeconds) {
        return play(model, data, file, name, seconds, slot, blendSeconds, 1);
    }

    /**
     * As above, applied at {@code weight}: each channel the animation sets moves that far from what earlier
     * animations set this frame toward the animation's value (e.g. a kick over a still pose, by speed).
     */
    public static boolean play(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds, String slot, double blendSeconds, float weight) {
        return play(model, data, file, name, seconds, slot, blendSeconds, weight, null);
    }

    /** As above, leaving out the bones in {@code without} (e.g. the root while riding, where the mount places the body). */
    public static boolean play(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds, String slot, double blendSeconds, float weight, Set<String> without) {
        return play(model, data, file, name, seconds, slot, blendSeconds, weight, without, false);
    }

    /** As above; {@code mirror} plays it on the other side (see {@link #mirror}). */
    public static boolean play(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds, String slot, double blendSeconds, float weight, Set<String> without, boolean mirror) {
        Animation animation = find(file, name);
        if (animation == null) return false;
        float maxRate = FOLLOW_RATE;
        if (slot != null && data != null && blendSeconds > 0) {
            float blended = (float)(data.ticksPlaying(slot, file + "#" + name) / (blendSeconds * 20));
            if (blended < 1) maxRate = blendRate(blended);
        }
        apply(model, animation, AnimationProcessor.evaluate(animation, seconds, context(model, data, mirror)), maxRate, weight, without,
            null, mirror);
        return true;
    }

    /**
     * Plays an animation that starts mid-motion (e.g. a sword swing), timed by the caller, in {@code slot}: where
     * the parts were when it started fades out over {@code fadeSeconds} ({@link Inertia}), so it neither snaps to its
     * first frame nor slows its motion down, and held poses are in place by then. Leaves out the bones in
     * {@code without} (may be null).
     *
     * @return false if the file or animation is missing.
     */
    public static boolean playOnce(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds, String slot, double fadeSeconds, Set<String> without) {
        return playOnce(model, data, file, name, seconds, slot, fadeSeconds, without, false);
    }

    /** As above; {@code mirror} plays it on the other side (see {@link #mirror}), e.g. for the off hand. */
    public static boolean playOnce(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds, String slot, double fadeSeconds, Set<String> without, boolean mirror) {
        return playOnce(model, data, file, name, seconds, slot, fadeSeconds, without, mirror, 0);
    }

    /**
     * As above, for the {@code instance}th time it is thrown: a new instance starts over (and fades in) even when it
     * is the same animation as before, e.g. one punch after another with the same fist.
     */
    public static boolean playOnce(AnimatedModel model, EntityData data, ResourceLocation file, String name,
        double seconds, String slot, double fadeSeconds, Set<String> without, boolean mirror, int instance) {
        Animation animation = find(file, name);
        if (animation == null) return false;
        playOnce(model, data, animation, file + "#" + name + "@" + instance, seconds, slot, fadeSeconds, without, mirror);
        return true;
    }

    static void playOnce(AnimatedModel model, EntityData data, Animation animation, String key, double seconds,
        String slot, double fadeSeconds, Set<String> without) {
        playOnce(model, data, animation, key, seconds, slot, fadeSeconds, without, false);
    }

    static void playOnce(AnimatedModel model, EntityData data, Animation animation, String key, double seconds,
        String slot, double fadeSeconds, Set<String> without, boolean mirror) {
        Inertia inertia = null;
        if (data != null && fadeSeconds > 0) {
            inertia = data.inertia(slot);
            float elapsed = data.ticksPlaying(slot, key);
            if (elapsed <= 0) {
                // The offsets from where the animation was a frame ago, which is where the fade starts.
                inertia.capture();
                double previous = Math.max(0, seconds - data.ticksPerFrame / 20.0);
                apply(model, animation, AnimationProcessor.evaluate(animation, previous, context(model, data, mirror)), FOLLOW_RATE, 1,
                    without, inertia, mirror);
            }
            inertia.fade((float)((elapsed + data.ticksPerFrame) / (fadeSeconds * 20)));
            if (!inertia.active()) inertia = null;
        }
        apply(model, animation, AnimationProcessor.evaluate(animation, seconds, context(model, data, mirror)), FOLLOW_RATE, 1,
            without, inertia, mirror);
    }

    /**
     * The left arm's bones (and its items): an animation plays without them while the off hand holds something, so
     * a swing or a stance in the main hand doesn't pull the torch or shield arm along.
     */
    public static final Set<String> LEFT_ARM = java.util.Collections.unmodifiableSet(new HashSet<String>(java.util.Arrays.asList(
        Bone.LEFT_ARM_AIM.id, Bone.LEFT_ARM.id, Bone.LEFT_FORE_ARM.id, Bone.LEFT_ITEM.id, Bone.LEFT_ITEM_FREE.id)));

    /** The right arm's bones: an off-hand action plays without them while the main hand holds something. */
    public static final Set<String> RIGHT_ARM = java.util.Collections.unmodifiableSet(new HashSet<String>(java.util.Arrays.asList(
        Bone.RIGHT_ARM_AIM.id, Bone.RIGHT_ARM.id, Bone.RIGHT_FORE_ARM.id, Bone.RIGHT_ITEM.id, Bone.RIGHT_ITEM_FREE.id)));

    /** Every bone but {@link #LEFT_ARM}'s: an animation played without them only moves the left arm. */
    public static final Set<String> ALL_BUT_LEFT_ARM;

    static {
        Set<String> others = new HashSet<String>();
        others.add(PlayerRig.ROOT);
        for (Bone bone : Bone.values()) if (!LEFT_ARM.contains(bone.id)) others.add(bone.id);
        ALL_BUT_LEFT_ARM = java.util.Collections.unmodifiableSet(others);
    }

    /** {@code arm} plus {@code others} (may be null). */
    public static Set<String> union(Set<String> arm, Set<String> others) {
        if (others == null) return arm;
        Set<String> both = new HashSet<String>(arm);
        both.addAll(others);
        return both;
    }

    /** {@link #LEFT_ARM} if the off hand holds something, else none; plus {@code others} (may be null). */
    public static Set<String> freeHandsOnly(EntityData data, Set<String> others) {
        return isOffhandHolding(data) ? union(LEFT_ARM, others) : others;
    }

    /** Whether the player's off hand holds something (Backhand; false outside a running world). */
    public static boolean isOffhandHolding(EntityData data) {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (data == null || minecraft == null || minecraft.theWorld == null) return false;
        EntityLivingBase entity = data.getEntity();
        return entity instanceof EntityPlayer && BackhandCompat.isOffhandHolding((EntityPlayer)entity);
    }

    /** How long switching between looping locomotion animations blends, as their Java smoothing eased them. */
    static final double LOCOMOTION_BLEND = 0.25;

    /**
     * Loops {@code animation.mobends.player.<name>} from {@code file} on the player's age, in {@code slot}.
     *
     * @return false if the file or animation is missing.
     */
    public static boolean playLoop(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot) {
        return playLoop(model, data, file, name, slot, 1);
    }

    /** As above, applied at {@code weight}. */
    public static boolean playLoop(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot,
        float weight) {
        return playLoop(model, data, file, name, slot, weight, null);
    }

    /** As above, leaving out the bones in {@code without} (may be null). */
    public static boolean playLoop(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot,
        float weight, Set<String> without) {
        return playLoop(model, data, file, name, slot, weight, without, false);
    }

    /** As above; {@code mirror} plays it on the other side (see {@link #mirror}). */
    public static boolean playLoop(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot,
        float weight, Set<String> without, boolean mirror) {
        String animation = "animation.mobends.player." + name;
        Animation loaded = find(file, animation);
        if (loaded == null) return false;
        double seconds = loaded.length() > 0 ? (data.ticks / 20.0) % loaded.length() : 0;
        return play(model, data, file, animation, seconds, slot, LOCOMOTION_BLEND, weight, without, mirror);
    }

    /** Plays a held pose (the animation at 0 s) from {@code file}, blending in when {@code slot} switches to it. */
    public static boolean playPose(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot) {
        return playPose(model, data, file, name, slot, 1);
    }

    /** As above, applied at {@code weight}. */
    public static boolean playPose(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot,
        float weight) {
        return playPose(model, data, file, name, slot, weight, null);
    }

    /** As above, leaving out the bones in {@code without} (may be null). */
    public static boolean playPose(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot,
        float weight, Set<String> without) {
        return playPose(model, data, file, name, slot, weight, without, false);
    }

    /** As above; {@code mirror} plays it on the other side (see {@link #mirror}). */
    public static boolean playPose(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot,
        float weight, Set<String> without, boolean mirror) {
        return play(model, data, file, "animation.mobends.player." + name, 0, slot, LOCOMOTION_BLEND, weight, without, mirror);
    }

    /**
     * Plays {@code <name>} and, over it, {@code <name>_swing} weighted by how strongly the limbs swing, both at the
     * stride's phase: one animation loop per stride (vanilla's limb swing times 0.6662 radians), so the animation keeps
     * pace with the walk whatever its length.
     *
     * @return false if the file or either animation is missing.
     */
    public static boolean playStride(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot) {
        String main = "animation.mobends.player." + name, swing = main + "_swing";
        Animation mainAnimation = find(file, main), swingAnimation = find(file, swing);
        if (mainAnimation == null || swingAnimation == null) return false;
        double stride = model.limbSwing() * 0.6662 / (2 * Math.PI);
        stride -= Math.floor(stride);
        play(model, data, file, main, stride * mainAnimation.length(), slot, LOCOMOTION_BLEND);
        play(model, data, file, swing, stride * swingAnimation.length(), slot + "_swing", LOCOMOTION_BLEND,
            Math.max(0, Math.min(1, model.limbSwingAmount())));
        return true;
    }

    /**
     * Plays {@code <name>} at the stride's phase, without a swing layer (e.g. the sneak crouch over standing).
     *
     * @return false if the file or animation is missing.
     */
    public static boolean playStridePose(AnimatedModel model, EntityData data, ResourceLocation file, String name, String slot) {
        String animation = "animation.mobends.player." + name;
        Animation loaded = find(file, animation);
        if (loaded == null) return false;
        double stride = model.limbSwing() * 0.6662 / (2 * Math.PI);
        stride -= Math.floor(stride);
        return play(model, data, file, animation, stride * loaded.length(), slot, LOCOMOTION_BLEND);
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

    /** Molang queries available to animations. */
    public static MolangContext context(AnimatedModel model, EntityData data) {
        return context(model, data, false);
    }

    /**
     * As above, for an animation played {@code mirror}ed: the look's yaw is given reflected, so that mirroring the
     * pose turns the head back to where the player actually looks.
     */
    static MolangContext context(AnimatedModel model, EntityData data, boolean mirror) {
        return variable -> {
            switch (variable) {
                case "query.mobends_head_pitch": return model.headPitch();
                case "query.mobends_head_yaw": return mirror ? -model.headYaw() : model.headYaw();
                case "query.mobends_ticks": return data == null ? 0 : data.ticks;
                case "query.mobends_ticks_after_punch": return data == null ? 0 : data.ticksAfterPunch;
                case "query.mobends_holding": return isHolding(data) ? 1 : 0;
                case "query.mobends_offhand_holding": return isOffhandHolding(data) ? 1 : 0;
                case "query.mobends_travel_pitch": return data instanceof Data_Player ? ((Data_Player)data).divingTravelPitch : 0;
                // An axis set to q.mobends_keep is left to the animations before this one (e.g. a sneak over a walk).
                case "query.mobends_keep": return Double.NaN;
                default: return 0;
            }
        };
    }

    /** Whether the player's main hand holds something (false outside a running world). */
    public static boolean isHolding(EntityData data) {
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

    static void apply(AnimatedModel model, Animation animation, Map<String, BoneSnapshot> pose) {
        apply(model, animation, pose, FOLLOW_RATE, 1);
    }

    static void apply(AnimatedModel model, Animation animation, Map<String, BoneSnapshot> pose, float maxRate,
        float weight) {
        apply(model, animation, pose, maxRate, weight, null);
    }

    static void apply(AnimatedModel model, Animation animation, Map<String, BoneSnapshot> pose, float maxRate,
        float weight, Set<String> without) {
        apply(model, animation, pose, maxRate, weight, without, null);
    }

    static void apply(AnimatedModel model, Animation animation, Map<String, BoneSnapshot> pose, float maxRate,
        float weight, Set<String> without, Inertia inertia) {
        apply(model, animation, pose, maxRate, weight, without, inertia, false);
    }

    /**
     * As above; {@code mirror} plays the animation on the other side (right and left bones swapped, mirrored across
     * the body's middle), e.g. a right-hand swing for the off hand. {@code without} names bones as they end up.
     */
    static void apply(AnimatedModel model, Animation animation, Map<String, BoneSnapshot> pose, float maxRate,
        float weight, Set<String> without, Inertia inertia, boolean mirror) {
        if (weight <= 0) return;
        if (mirror) pose = mirror(pose);
        Map<String, BoneAnimation> tracks = new HashMap<String, BoneAnimation>();
        for (BoneAnimation track : animation.boneAnimations()) {
            String name = mirror ? mirrorName(track.boneName()) : track.boneName();
            if (pose.containsKey(name) && (without == null || !without.contains(name))) tracks.put(name, track);
        }
        for (Map.Entry<String, BoneAnimation> entry : tracks.entrySet()) {
            String name = entry.getKey();
            BoneAnimation track = entry.getValue();
            BoneSnapshot snapshot = pose.get(name);
            float rate = rate(track, maxRate);
            if (weight < 1) rate = Math.max(rate, currentRate(model, name));
            if (PlayerRig.ROOT.equals(name)) {
                applyRoot(model, track, snapshot, maxRate, weight, inertia);
                continue;
            }
            if (model.rigAimed(name) != null) continue; // Aim bones drive their part's aim below.
            ItemBone item = model.rigItem(name);
            if (item != null) {
                applyItem(item, name, PlayerRig.ROOT.equals(model.rigParent(name)), track, snapshot, maxRate, weight, inertia);
                continue;
            }
            ModelRendererBends part = model.rigPart(name);
            if (part == null) continue;
            KeyframeStack rotation = track.rotationKeyFrames();
            if (animates(rotation)) clearPreRotation(part, rate);
            setRotation(part.rotation, rotation, snapshot, maxRate, weight, inertia, name + " rotation");
            String aim = model.rigAim(name);
            if (aim == null || !tracks.containsKey(aim)) setPosition(part, name, track, snapshot, maxRate, weight, inertia);
            KeyframeStack scale = track.scaleKeyFrames();
            if (has(scale.xKeyframes())) part.scaleX = mix(part.scaleX, snapshot.getScaleX(), weight);
            // (A kept scale axis stays at what earlier animations set, like rotations and positions.)
            if (has(scale.yKeyframes())) part.scaleY = mix(part.scaleY, snapshot.getScaleY(), weight);
            if (has(scale.zKeyframes())) part.scaleZ = mix(part.scaleZ, snapshot.getScaleZ(), weight);
        }
        for (Map.Entry<String, BoneAnimation> entry : tracks.entrySet()) {
            String aim = entry.getKey(), limb = model.rigAimed(aim);
            if (limb == null) continue;
            BoneAnimation aimTrack = entry.getValue(), limbTrack = tracks.get(limb);
            float rate = Math.max(rate(aimTrack, maxRate), limbTrack == null ? 0 : rate(limbTrack, maxRate));
            applyAim(model.rigPart(limb), aim, aimTrack, pose.get(aim), limbTrack, pose.get(limb), rate, maxRate, weight, inertia);
        }
    }

    /** The bone on the other side ({@code rightArm} and {@code leftArm}, ...); others are their own mirror. */
    static String mirrorName(String name) {
        if (name.startsWith("right")) return "left" + name.substring(5);
        if (name.startsWith("left")) return "right" + name.substring(4);
        return name;
    }

    /**
     * A pose mirrored across the body's middle, the X axis: each bone takes the other side's values, turned
     * (x, -y, -z) and moved (-x, y, z). (Salamander keeps X and Y rotations negated, which doesn't change that.)
     */
    static Map<String, BoneSnapshot> mirror(Map<String, BoneSnapshot> pose) {
        Map<String, BoneSnapshot> mirrored = new HashMap<String, BoneSnapshot>();
        for (Map.Entry<String, BoneSnapshot> entry : pose.entrySet()) {
            BoneSnapshot snapshot = entry.getValue().copy();
            snapshot.setRotation(snapshot.getRotX(), -snapshot.getRotY(), -snapshot.getRotZ());
            snapshot.setTranslateX(-snapshot.getTranslateX());
            mirrored.put(mirrorName(entry.getKey()), snapshot);
        }
        return mirrored;
    }

    /**
     * An aim bone turns the part at its pivot before the part's own rotation. Positions compose: the aim's offset,
     * plus the part's offset turned by the aim.
     */
    private static void applyAim(ModelRendererBends part, String aimId, BoneAnimation aimTrack, BoneSnapshot aimSnapshot,
        BoneAnimation partTrack, BoneSnapshot partSnapshot, float rate, float maxRate, float weight, Inertia inertia) {
        if (animates(aimTrack.rotationKeyFrames())) {
            clearPreRotation(part, rate);
            setRotation(part.aim, aimTrack.rotationKeyFrames(), aimSnapshot, maxRate, weight, inertia, aimId + " rotation");
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
            part.position.setSmoothX(mix(part.position.vFinal.x, offset[0], weight), rate);
            part.position.setSmoothY(mix(part.position.vFinal.y, offset[1], weight), rate);
            part.position.setSmoothZ(mix(part.position.vFinal.z, offset[2], weight), rate);
        }
    }

    /** A bone's translation moves the part's pivot: Bedrock (x, y, z) pixels is model (x, -y, z). */
    private static void setPosition(ModelRendererBends part, String boneId, BoneAnimation track, BoneSnapshot snapshot,
        float maxRate, float weight, Inertia inertia) {
        setTranslation(part.position, track.positionKeyFrames(), snapshot.getTranslateX(), -snapshot.getTranslateY(),
            snapshot.getTranslateZ(), maxRate, weight, inertia, boneId + " position");
    }

    /** {@code to} at {@code weight}; NaN (q.mobends_keep) leaves {@code from} as it is. */
    private static float mix(float from, float to, float weight) {
        if (Float.isNaN(to)) return from;
        return weight >= 1 ? to : from + (to - from) * weight;
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
    private static void applyItem(ItemBone item, String boneId, boolean free, BoneAnimation track, BoneSnapshot snapshot,
        float maxRate, float weight, Inertia inertia) {
        setRotation(item.rotation, track.rotationKeyFrames(), snapshot, maxRate, weight, inertia, boneId + " rotation");
        setTranslation(item.position, track.positionKeyFrames(), snapshot.getTranslateX(), -snapshot.getTranslateY(),
            snapshot.getTranslateZ(), maxRate, weight, inertia, boneId + " position");
        KeyframeStack scale = track.scaleKeyFrames();
        if (free) {
            item.scaleX = item.scaleY = item.scaleZ = 1;
        }
        if (has(scale.xKeyframes())) item.scaleX = mix(item.scaleX, snapshot.getScaleX(), 1);
        if (has(scale.yKeyframes())) item.scaleY = mix(item.scaleY, snapshot.getScaleY(), 1);
        if (has(scale.zKeyframes())) item.scaleZ = mix(item.scaleZ, snapshot.getScaleZ(), 1);
    }

    /** Authored rotations replace procedural pre-rotations, which Blockbench bones don't have. */
    private static void clearPreRotation(ModelRendererBends part, float rate) {
        part.pre_rotation.setSmoothX(0, rate);
        part.pre_rotation.setSmoothY(0, rate);
        part.pre_rotation.setSmoothZ(0, rate);
    }

    /** {@code root} drives the whole-body transform: rotation to renderRotation, position to renderOffset. */
    private static void applyRoot(AnimatedModel model, BoneAnimation track, BoneSnapshot snapshot, float maxRate,
        float weight, Inertia inertia) {
        setRotation(model.rootRotation(), track.rotationKeyFrames(), snapshot, maxRate, weight, inertia, "root rotation");
        setTranslation(model.rootOffset(), track.positionKeyFrames(), -snapshot.getTranslateX(), snapshot.getTranslateY(),
            snapshot.getTranslateZ(), maxRate, weight, inertia, "root position");
    }

    /**
     * Salamander stores Bedrock rotations in radians with X and Y negated; parts use Bedrock degrees. {@code inertia}
     * (may be null) fades channel {@code key} in from where it was.
     */
    private static void setRotation(SmoothVector3f target, KeyframeStack keyframes, BoneSnapshot snapshot, float maxRate,
        float weight, Inertia inertia, String key) {
        // Taking the nearest whole turn keeps a spin going forward; a partial blend mixes the angles as authored.
        boolean unwrap = weight >= 1;
        float[] angles = {-snapshot.getRotX() * RAD_TO_DEG, -snapshot.getRotY() * RAD_TO_DEG, snapshot.getRotZ() * RAD_TO_DEG};
        float[] shown = {target.vSmooth.x, target.vSmooth.y, target.vSmooth.z};
        float[] finals = {target.vFinal.x, target.vFinal.y, target.vFinal.z};
        float[] smoothness = {target.smoothness.x, target.smoothness.y, target.smoothness.z};
        Keyframe[][] axes = {keyframes.xKeyframes(), keyframes.yKeyframes(), keyframes.zKeyframes()};
        for (int i = 0; i < 3; ++i) {
            if (!has(axes[i])) continue;
            float rate = inertia != null ? FOLLOW_RATE : axisRate(axes[i], maxRate, weight, smoothness[i]);
            // Only motion unwraps: a held pose eases there the way the angle reads, as the Java poses do.
            float angle = unwrap && rate >= FOLLOW_RATE ? nearest(angles[i], shown[i]) : angles[i];
            set(target, i, mix(finals[i], inertial(inertia, key, i, angle, shown[i], rate), weight), rate);
        }
    }

    /** A position (x, y, z) in the target's space; {@code inertia} as for rotations. */
    private static void setTranslation(SmoothVector3f target, KeyframeStack keyframes, float x, float y, float z,
        float maxRate, float weight, Inertia inertia, String key) {
        float[] values = {x, y, z};
        float[] shown = {target.vSmooth.x, target.vSmooth.y, target.vSmooth.z};
        float[] finals = {target.vFinal.x, target.vFinal.y, target.vFinal.z};
        float[] smoothness = {target.smoothness.x, target.smoothness.y, target.smoothness.z};
        Keyframe[][] axes = {keyframes.xKeyframes(), keyframes.yKeyframes(), keyframes.zKeyframes()};
        for (int i = 0; i < 3; ++i) {
            if (!has(axes[i])) continue;
            float rate = inertia != null ? FOLLOW_RATE : axisRate(axes[i], maxRate, weight, smoothness[i]);
            set(target, i, mix(finals[i], inertial(inertia, key, i, values[i], shown[i], rate), weight), rate);
        }
    }

    /**
     * While an animation fades in ({@link #playOnce}), every axis follows it, fading out where it started; otherwise
     * nothing changes.
     */
    private static float inertial(Inertia inertia, String key, int axis, float value, float shown, float rate) {
        if (inertia == null || rate < FOLLOW_RATE || Float.isNaN(value)) return value;
        return inertia.apply(key + " " + axis, value, shown);
    }

    private static void set(SmoothVector3f target, int axis, float value, float rate) {
        if (axis == 0) target.setSmoothX(value, rate);
        else if (axis == 1) target.setSmoothY(value, rate);
        else target.setSmoothZ(value, rate);
    }

    private static final Map<Keyframe[], Boolean> AXIS_MOVES = java.util.Collections.synchronizedMap(
        new java.util.WeakHashMap<Keyframe[], Boolean>());

    /**
     * An axis whose keyframes all hold one value holds a pose and eases into it; one that changes, or follows Molang,
     * follows its keyframes. A partial layer never slows an axis an earlier layer set this frame.
     */
    private static float axisRate(Keyframe[] keyframes, float maxRate, float weight, float current) {
        Boolean moves = AXIS_MOVES.get(keyframes);
        if (moves == null) {
            moves = axisMoves(keyframes);
            AXIS_MOVES.put(keyframes, moves);
        }
        float rate = Math.min(maxRate, moves ? FOLLOW_RATE : EASE_RATE);
        return weight < 1 ? Math.max(rate, current) : rate;
    }

    private static boolean axisMoves(Keyframe[] keyframes) {
        Double held = null;
        for (Keyframe keyframe : keyframes) {
            if (!keyframe.getUsedVariables().isEmpty()) return true;
            for (double value : new double[] {keyframe.startValue().get(new ControllerState()), keyframe.endValue().get(new ControllerState())}) {
                if (held == null) held = value;
                else if (Math.abs(value - held) > 1e-4) return true;
            }
        }
        return false;
    }

    /**
     * The same single-axis angle, give or take whole turns, closest to where the part is now, so a looping spin
     * wrapping from 360 back to 0 degrees keeps turning forward instead of smoothing back.
     */
    static float nearest(float angle, float current) {
        return angle + 360.0f * Math.round((current - angle) / 360.0f);
    }

    /**
     * How fast what earlier animations set on a bone this frame is followed: a partial layer mixed over it shouldn't
     * slow it down (e.g. a held forward-flight pose blended over a hover that follows its keyframes).
     */
    private static float currentRate(AnimatedModel model, String boneId) {
        SmoothVector3f rotation;
        if (PlayerRig.ROOT.equals(boneId)) {
            rotation = model.rootRotation();
        } else if (model.rigAimed(boneId) != null) {
            rotation = model.rigPart(model.rigAimed(boneId)).aim;
        } else if (model.rigItem(boneId) != null) {
            rotation = model.rigItem(boneId).rotation;
        } else if (model.rigPart(boneId) != null) {
            rotation = model.rigPart(boneId).rotation;
        } else {
            return 0;
        }
        return Math.min(rotation.smoothness.x, Math.min(rotation.smoothness.y, rotation.smoothness.z));
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
