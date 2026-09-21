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

    @Test
    void punchesMatchTheJavaPunch() throws Exception {
        for (boolean right : new boolean[] {true, false}) {
            for (boolean moving : new boolean[] {false, true}) {
                String name = "animation.mobends.player." + (right ? "punch_right" : "punch_left") + (moving ? "_moving" : "");
                for (float pitch : PITCHES) {
                    for (float yaw : YAWS) {
                        ModelBendsPlayer java = model(pitch, yaw);
                        Animation_Attack_Punch.animate(null, java, data(right, moving));
                        for (double seconds : new double[] {0, 0.2, 0.49}) {
                            ModelBendsPlayer file = model(pitch, yaw);
                            play(file, data(right, moving), name, seconds);
                            assertSamePose(java, file, right ? Bone.RIGHT_ARM : Bone.LEFT_ARM,
                                name + " at " + seconds + "s, pitch " + pitch + " yaw " + yaw);
                        }
                    }
                }
            }
        }
    }

    @Test
    void stanceMatchesTheJavaStance() throws Exception {
        for (float pitch : PITCHES) {
            for (float yaw : YAWS) {
                ModelBendsPlayer java = model(pitch, yaw);
                Animation_Attack_PunchStance.animate(null, java, data(true, false));
                for (double seconds : new double[] {0, 1.2, 2.49}) {
                    ModelBendsPlayer file = model(pitch, yaw);
                    play(file, data(true, false), "animation.mobends.player.punch_stance", seconds);
                    assertSamePose(java, file, null, "stance at " + seconds + "s, pitch " + pitch + " yaw " + yaw);
                }
            }
        }
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
            AnimationProcessor.evaluate(animation, seconds, PlayerAnimator.context(model, data)), bone -> 0.3f);
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
