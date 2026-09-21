package net.gobbob.mobends.client.animation;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
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
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.util.BendsLogger;
import net.gobbob.mobends.util.SmoothVector3f;
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

    /** Smoothing rate for keyframed motion: about one frame behind at 60 fps, none at 20. */
    static final float FOLLOW_RATE = 2.0f;
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
        Animation animation = find(file, name);
        if (animation == null) return false;
        apply(model, animation, AnimationProcessor.evaluate(animation, seconds, context(model, data)));
        return true;
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
                default: return 0;
            }
        };
    }

    static void apply(ModelBendsPlayer model, Animation animation, Map<String, BoneSnapshot> pose) {
        Map<String, BoneAnimation> tracks = new HashMap<String, BoneAnimation>();
        for (BoneAnimation track : animation.boneAnimations()) {
            if (pose.containsKey(track.boneName())) tracks.put(track.boneName(), track);
        }
        for (BoneAnimation track : tracks.values()) {
            BoneSnapshot snapshot = pose.get(track.boneName());
            float rate = rate(track);
            if (PlayerRig.ROOT.equals(track.boneName())) {
                applyRoot(model, track, snapshot, rate);
                continue;
            }
            Bone bone = Bone.byId(track.boneName());
            if (bone == null || bone.aimed() != null) continue; // Aim bones are folded into their arm below.
            ModelRendererBends part = bone.part(model);
            KeyframeStack rotation = track.rotationKeyFrames();
            if (animates(rotation)) clearPreRotation(part, rate);
            setRotation(part.rotation, rotation, snapshot, rate);
            KeyframeStack scale = track.scaleKeyFrames();
            if (has(scale.xKeyframes())) part.scaleX = snapshot.getScaleX();
            if (has(scale.yKeyframes())) part.scaleY = snapshot.getScaleY();
            if (has(scale.zKeyframes())) part.scaleZ = snapshot.getScaleZ();
        }
        for (Bone aim : new Bone[] {Bone.RIGHT_ARM_AIM, Bone.LEFT_ARM_AIM}) {
            BoneAnimation aimTrack = tracks.get(aim.id);
            if (aimTrack == null || !animates(aimTrack.rotationKeyFrames())) continue;
            Bone arm = aim.aimed();
            BoneAnimation armTrack = tracks.get(arm.id);
            float rate = Math.max(rate(aimTrack), armTrack == null ? EASE_RATE : rate(armTrack));
            applyAimedArm(arm.part(model), aimTrack, pose.get(aim.id), armTrack, pose.get(arm.id), rate);
        }
    }

    /** Folds the aim bone's rotation into the arm: one rotation equal to aim, then arm. */
    private static void applyAimedArm(ModelRendererBends part, BoneAnimation aimTrack, BoneSnapshot aimSnapshot,
        BoneAnimation armTrack, BoneSnapshot armSnapshot, float rate) {
        float[] aim = degrees(aimTrack.rotationKeyFrames(), aimSnapshot, new float[3]);
        // Arm axes the animation leaves out keep what earlier animations set.
        float[] target = {part.rotation.vFinal.x, part.rotation.vFinal.y, part.rotation.vFinal.z};
        float[] arm = armTrack == null ? target : degrees(armTrack.rotationKeyFrames(), armSnapshot, target);
        // Of the equivalent angle sets, take the one nearest where the arm is now.
        float[] now = {part.rotation.vSmooth.x, part.rotation.vSmooth.y, part.rotation.vSmooth.z};
        float[] combined = EulerZYX.angles(
            EulerZYX.multiply(EulerZYX.matrix(aim[0], aim[1], aim[2]), EulerZYX.matrix(arm[0], arm[1], arm[2])), now);
        clearPreRotation(part, rate);
        part.rotation.setSmoothX(combined[0], rate);
        part.rotation.setSmoothY(combined[1], rate);
        part.rotation.setSmoothZ(combined[2], rate);
    }

    /** Authored rotations replace procedural pre-rotations, which Blockbench bones don't have. */
    private static void clearPreRotation(ModelRendererBends part, float rate) {
        part.pre_rotation.setSmoothX(0, rate);
        part.pre_rotation.setSmoothY(0, rate);
        part.pre_rotation.setSmoothZ(0, rate);
    }

    /** Keyframed axes in part degrees; others are taken from {@code fallback}. */
    private static float[] degrees(KeyframeStack keyframes, BoneSnapshot snapshot, float[] fallback) {
        if (snapshot == null) return fallback;
        if (has(keyframes.xKeyframes())) fallback[0] = -snapshot.getRotX() * RAD_TO_DEG;
        if (has(keyframes.yKeyframes())) fallback[1] = -snapshot.getRotY() * RAD_TO_DEG;
        if (has(keyframes.zKeyframes())) fallback[2] = snapshot.getRotZ() * RAD_TO_DEG;
        return fallback;
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
        if (has(keyframes.xKeyframes())) target.setSmoothX(-snapshot.getRotX() * RAD_TO_DEG, rate);
        if (has(keyframes.yKeyframes())) target.setSmoothY(-snapshot.getRotY() * RAD_TO_DEG, rate);
        if (has(keyframes.zKeyframes())) target.setSmoothZ(snapshot.getRotZ() * RAD_TO_DEG, rate);
    }

    /** Follow a bone that moves; ease into a bone that only holds a pose. */
    static float rate(BoneAnimation track) {
        for (KeyframeStack stack : new KeyframeStack[] {
            track.rotationKeyFrames(), track.positionKeyFrames(), track.scaleKeyFrames()}) {
            for (Keyframe[] axis : new Keyframe[][] {stack.xKeyframes(), stack.yKeyframes(), stack.zKeyframes()}) {
                if (axis != null && axis.length > 1) return FOLLOW_RATE;
            }
        }
        return EASE_RATE;
    }

    private static boolean animates(KeyframeStack stack) {
        return has(stack.xKeyframes()) || has(stack.yKeyframes()) || has(stack.zKeyframes());
    }

    private static boolean has(Keyframe[] keyframes) {
        return keyframes != null && keyframes.length > 0;
    }
}
