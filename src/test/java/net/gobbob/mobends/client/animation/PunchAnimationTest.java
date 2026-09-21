package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.animation.BoneAnimation;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import net.gobbob.mobends.animation.player.Animation_Attack_Punch;
import net.gobbob.mobends.animation.player.Animation_Attack_PunchStance;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.data.Data_Player;
import net.gobbob.mobends.util.SmoothVector3f;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

/** The Blockbench punch must pose the player like the original Java punch it replaces. */
class PunchAnimationTest {
    private static final String FILE = "/assets/mobends/geckolib/animations/player/punch.animation.json";
    private static final float DEG = (float)(Math.PI / 180.0);
    private static final float[] PITCHES = {-60, -20, 0, 35, 80};
    private static final float[] YAWS = {-40, 0, 25};

    @Test
    void everyAnimatedBoneExistsInTheRig() throws Exception {
        for (Animation animation : animations().animations().values()) {
            for (BoneAnimation track : animation.boneAnimations()) {
                String id = track.boneName();
                boolean known = PlayerRig.ROOT.equals(id);
                for (Bone bone : Bone.values()) known |= bone.id.equals(id);
                assertTrue(known, animation.name() + " animates unknown bone " + id);
            }
        }
    }

    /** Standing: stance, the old punch at its peak, then back to the stance for the rest of the 3 s. */
    @Test
    void standingPunchMatchesTheJavaPunchAndStance() throws Exception {
        for (boolean right : new boolean[] {true, false}) {
            String name = "animation.mobends.player.punch_" + (right ? "right" : "left");
            Bone arm = right ? Bone.RIGHT_ARM : Bone.LEFT_ARM;
            for (float pitch : PITCHES) {
                for (float yaw : YAWS) {
                    ModelBendsPlayer stance = model(pitch, yaw);
                    Animation_Attack_PunchStance.animate(null, stance, data(right, false));
                    ModelBendsPlayer punch = model(pitch, yaw);
                    Animation_Attack_Punch.animate(null, punch, data(right, false));
                    String at = ", pitch " + pitch + " yaw " + yaw;
                    for (double seconds : new double[] {0, 1.1, 2.0, 3.0}) {
                        assertSamePose(stance, played(name, right, false, pitch, yaw, seconds), arm, name + " at " + seconds + "s" + at);
                    }
                    for (double seconds : new double[] {0.45, 0.5}) {
                        assertSamePose(punch, played(name, right, false, pitch, yaw, seconds), arm, name + " at " + seconds + "s" + at);
                    }
                }
            }
        }
    }

    @Test
    void movingPunchMatchesTheJavaPunchAtItsPeak() throws Exception {
        for (boolean right : new boolean[] {true, false}) {
            String name = "animation.mobends.player.punch_" + (right ? "right" : "left") + "_moving";
            for (float pitch : PITCHES) {
                for (float yaw : YAWS) {
                    ModelBendsPlayer punch = model(pitch, yaw);
                    Animation_Attack_Punch.animate(null, punch, data(right, true));
                    for (double seconds : new double[] {0.45, 0.5}) {
                        assertSamePose(punch, played(name, right, true, pitch, yaw, seconds), right ? Bone.RIGHT_ARM : Bone.LEFT_ARM,
                            name + " at " + seconds + "s, pitch " + pitch + " yaw " + yaw);
                    }
                }
            }
        }
    }

    /** The punch is an animation, not a pose: mid-way the fist is between guard and full extension. */
    @Test
    void punchMovesOverTime() throws Exception {
        float guard = rightForeArmX(0);
        float mid = rightForeArmX(0.05);
        float out = rightForeArmX(0.3);
        assertEquals(-80, guard, 1e-3f);
        assertEquals(0, out, 1e-3f);
        assertTrue(mid > guard + 1 && mid < out - 1, "forearm mid-punch at " + mid);
    }

    /** Bone positions move the part's pivot, e.g. a hand detached from the elbow mid-punch. */
    @Test
    void bonePositionsMoveParts() throws Exception {
        String json = "{\"format_version\": \"1.8.0\", \"animations\": {\"animation.test\": {\"animation_length\": 0.5,"
            + " \"bones\": {\"rightForeArm\": {\"position\": {\"0.0\": [0, 0, 0], \"0.4\": [1, 2, 11], \"0.5\": [0, 0, 0]}}}}}}";
        Animation animation = new GeckoLibGsonLoader().loadAnimations(new java.io.StringReader(json)).getAnimation("animation.test");
        ModelBendsPlayer model = model(0, 0);
        PlayerAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, 0.4, PlayerAnimator.context(model, null)));
        org.lwjgl.util.vector.Vector3f position = ((ModelRendererBends)model.bipedRightForeArm).position.vFinal;
        assertEquals(1, position.x, 1e-4f);
        assertEquals(-2, position.y, 1e-4f, "Bedrock up is model -Y");
        assertEquals(11, position.z, 1e-4f);
    }

    /** Moving bones follow their keyframes; bones that only hold a pose ease into it. */
    @Test
    void movingBonesFollowAndHeldBonesEase() throws Exception {
        ModelBendsPlayer model = played("animation.mobends.player.punch_right", true, false, 0, 0, 0.05);
        assertEquals(PlayerAnimator.FOLLOW_RATE, ((ModelRendererBends)model.bipedBody).rotation.smoothness.y, 1e-6f);
        assertEquals(PlayerAnimator.FOLLOW_RATE, ((ModelRendererBends)model.bipedRightArm).rotation.smoothness.z, 1e-6f);
        assertEquals(PlayerAnimator.EASE_RATE, ((ModelRendererBends)model.bipedRightLeg).rotation.smoothness.x, 1e-6f);
        assertEquals(PlayerAnimator.EASE_RATE, ((ModelRendererBends)model.bipedLeftArm).rotation.smoothness.x, 1e-6f);
    }

    private static float rightForeArmX(double seconds) throws Exception {
        ModelBendsPlayer model = played("animation.mobends.player.punch_right", true, false, 0, 0, seconds);
        return ((ModelRendererBends)model.bipedRightForeArm).rotation.vFinal.x;
    }

    private static ModelBendsPlayer played(String name, boolean right, boolean moving, float pitch, float yaw, double seconds)
        throws Exception {
        ModelBendsPlayer model = model(pitch, yaw);
        play(model, data(right, moving), name, seconds);
        return model;
    }

    /**
     * Every target angle matches. The punching arm got its aim from a pre-rotation, which the Blockbench punch
     * expresses with an aim bone folded into the arm's rotation, so there the arm's whole orientation must match.
     */
    private static void assertSamePose(ModelBendsPlayer java, ModelBendsPlayer file, Bone aimedArm, String context) {
        for (Bone bone : Bone.values()) {
            ModelRendererBends expected = bone.part(java);
            ModelRendererBends actual = bone.part(file);
            if (expected == null) continue;
            if (bone == aimedArm) {
                Matrix4f want = orientation(expected);
                Matrix4f got = orientation(actual);
                assertTrue(want.equals(got, 1e-4f), context + ": " + bone.id + " orientation\n" + got + "expected\n" + want);
                continue;
            }
            assertTargets(expected.rotation, actual.rotation, context + ": " + bone.id + " rotation");
            assertTargets(expected.pre_rotation, actual.pre_rotation, context + ": " + bone.id + " pre-rotation");
        }
        assertTargets(java.renderRotation, file.renderRotation, context + ": renderRotation");
        assertTargets(java.renderOffset, file.renderOffset, context + ": renderOffset");
    }

    /** A part's target orientation: pre-rotation (-Y, X, Z) then rotation (Z, Y, X), as ModelRendererBends renders. */
    private static Matrix4f orientation(ModelRendererBends part) {
        org.lwjgl.util.vector.Vector3f pre = part.pre_rotation.vFinal;
        org.lwjgl.util.vector.Vector3f rot = part.rotation.vFinal;
        return new Matrix4f()
            .rotateY(-pre.y * DEG).rotateX(pre.x * DEG).rotateZ(pre.z * DEG)
            .rotateZ(rot.z * DEG).rotateY(rot.y * DEG).rotateX(rot.x * DEG);
    }

    private static void assertTargets(SmoothVector3f expected, SmoothVector3f actual, String context) {
        assertEquals(expected.vFinal.x, actual.vFinal.x, 1e-3f, context + " x");
        assertEquals(expected.vFinal.y, actual.vFinal.y, 1e-3f, context + " y");
        assertEquals(expected.vFinal.z, actual.vFinal.z, 1e-3f, context + " z");
    }

    private static void play(ModelBendsPlayer model, Data_Player data, String name, double seconds) throws Exception {
        Animation animation = animations().getAnimation(name);
        assertNotNull(animation, name);
        PlayerAnimator.apply(model, animation,
            AnimationProcessor.evaluate(animation, seconds, PlayerAnimator.context(model, data)));
    }

    private static BakedAnimations animations() throws Exception {
        try (Reader reader = new InputStreamReader(PunchAnimationTest.class.getResourceAsStream(FILE), StandardCharsets.UTF_8)) {
            return new GeckoLibGsonLoader().loadAnimations(reader);
        }
    }

    private static ModelBendsPlayer model(float pitch, float yaw) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.headRotationX = pitch;
        model.headRotationY = yaw;
        return model;
    }

    /** Data_Player's constructor needs a running world; the punch only reads motion and the fist. */
    private static Data_Player data(boolean rightFist, boolean moving) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Data_Player data = (Data_Player)((Unsafe)field.get(null)).allocateInstance(Data_Player.class);
        data.motion = new org.lwjgl.util.vector.Vector3f(moving ? 0.1f : 0, 0, 0);
        data.fistPunchArm = rightFist;
        return data;
    }
}
