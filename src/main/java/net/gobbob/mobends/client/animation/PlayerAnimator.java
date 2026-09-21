package net.gobbob.mobends.client.animation;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.GeckoLibResources;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BoneAnimation;
import com.geckolib.cache.animation.Keyframe;
import com.geckolib.cache.animation.KeyframeStack;
import com.geckolib.loading.math.MolangContext;
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
 * <p>An animation gives the target pose; the model's usual smoothing eases the parts toward it, so authored
 * animations blend with the procedural ones. Only channels an animation keyframes are applied: bones and
 * axes it leaves out keep whatever the animations before it set.
 */
public final class PlayerAnimator {
    public static final ResourceLocation PUNCH = new ResourceLocation(MoBends.MODID, "player/punch");

    private static final float RAD_TO_DEG = (float)(180.0 / Math.PI);
    private static final Set<String> reportedMissing = new HashSet<String>();

    /** Per-bone smoothing rate, as in {@link SmoothVector3f#setSmoothX(float, float)}. */
    public interface Smoothing {
        float rate(String bone);
    }

    private PlayerAnimator() {}

    /**
     * Applies an animation from the given file at {@code seconds}.
     *
     * @return false if the file or animation is missing, so the caller can fall back to its Java pose.
     */
    public static boolean play(ModelBendsPlayer model, Data_Player data, ResourceLocation file, String name,
        double seconds, Smoothing smoothing) {
        Animation animation = find(file, name);
        if (animation == null) return false;
        apply(model, animation, AnimationProcessor.evaluate(animation, seconds, context(model, data)), smoothing);
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

    static void apply(ModelBendsPlayer model, Animation animation, Map<String, BoneSnapshot> pose, Smoothing smoothing) {
        for (BoneAnimation track : animation.boneAnimations()) {
            BoneSnapshot snapshot = pose.get(track.boneName());
            if (snapshot == null) continue;
            float rate = smoothing.rate(track.boneName());
            if (PlayerRig.ROOT.equals(track.boneName())) {
                applyRoot(model, track, snapshot, rate);
                continue;
            }
            Bone bone = bone(track.boneName());
            if (bone == null) continue;
            ModelRendererBends part = bone.part(model);
            KeyframeStack rotation = track.rotationKeyFrames();
            if (animates(rotation)) {
                // Authored rotations replace procedural pre-rotations, which Blockbench bones don't have.
                part.pre_rotation.setSmoothX(0, rate);
                part.pre_rotation.setSmoothY(0, rate);
                part.pre_rotation.setSmoothZ(0, rate);
            }
            setRotation(part.rotation, rotation, snapshot, rate);
            KeyframeStack scale = track.scaleKeyFrames();
            if (has(scale.xKeyframes())) part.scaleX = snapshot.getScaleX();
            if (has(scale.yKeyframes())) part.scaleY = snapshot.getScaleY();
            if (has(scale.zKeyframes())) part.scaleZ = snapshot.getScaleZ();
        }
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

    private static boolean animates(KeyframeStack stack) {
        return has(stack.xKeyframes()) || has(stack.yKeyframes()) || has(stack.zKeyframes());
    }

    private static boolean has(Keyframe[] keyframes) {
        return keyframes != null && keyframes.length > 0;
    }

    private static Bone bone(String id) {
        for (Bone bone : Bone.values()) {
            if (bone.id.equals(id)) return bone;
        }
        return null;
    }
}
