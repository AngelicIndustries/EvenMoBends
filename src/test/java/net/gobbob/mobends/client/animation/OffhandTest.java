package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import java.nio.file.Paths;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector3f;

/** With something in the off hand, main-hand animations leave the left arm to what it was doing. */
class OffhandTest {
    /** Played mirrored, an animation puts each part where its other-side part goes, reflected across the body's middle. */
    @Test
    void mirroredAnimationsAreReflections() throws Exception {
        String dir = "src/main/resources/assets/mobends/geckolib/animations/player/";
        for (String[] which : new String[][] {{"sword", "sword_1"}, {"sword", "sword_3"}, {"axe", "axe_swing"}, {"punch", "punch_right"}}) {
            Animation animation = AnimationBaking.load(Paths.get(dir + which[0] + ".animation.json"))
                .getAnimation("animation.mobends.player." + which[1]);
            for (double seconds : new double[] {0, 0.1, 0.2, 0.4}) {
                ModelBendsPlayer plain = AnimationBaking.model(0, 0), mirrored = AnimationBaking.model(0, 0);
                AnimationBaking.frame(plain, m -> BendsAnimator.apply(m, animation,
                    AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(m, null)), 1000, 1, null, null, false), 1000);
                AnimationBaking.frame(mirrored, m -> BendsAnimator.apply(m, animation,
                    AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(m, null)), 1000, 1, null, null, true), 1000);
                org.joml.Matrix4f reflect = new org.joml.Matrix4f().scale(-1, 1, 1);
                for (Bone bone : Bone.values()) {
                    if (bone.part(plain) == null) continue;
                    Bone other = Bone.byId(BendsAnimator.mirrorName(bone.id));
                    org.joml.Matrix4f expected = new org.joml.Matrix4f(reflect).mul(AnimationBaking.orientation(bone.part(plain))).mul(reflect);
                    float off = AnimationBaking.angleBetween(expected, AnimationBaking.orientation(other.part(mirrored)));
                    org.junit.jupiter.api.Assertions.assertTrue(off < 0.1f, which[1] + " at " + seconds + ": " + other.id + " off by " + off);
                    ModelRendererBends a = bone.part(plain), b = other.part(mirrored);
                    assertEquals(-a.position.vSmooth.x, b.position.vSmooth.x, 1e-4, which[1] + ": " + other.id + " position");
                }
                assertEquals(-plain.renderRotation.vSmooth.y, mirrored.renderRotation.vSmooth.y, 1e-3, which[1] + ": root turn");
                assertEquals(-plain.renderOffset.vSmooth.x, mirrored.renderOffset.vSmooth.x, 1e-4, which[1] + ": root offset");
            }
        }
    }

    /**
     * With the off hand full, every punch is the right fist's. Punching again while the fist is out pulls it back
     * to the punch's start before punching, rather than snapping back to it in one frame.
     */
    @Test
    void repeatedPunchesWithOneFistPullBackFirst() throws Exception {
        Animation punch = AnimationBaking.load(Paths.get("src/main/resources/assets/mobends/geckolib/animations/player/punch.animation.json"))
            .getAnimation("animation.mobends.player.punch_right");
        float single = punching(punch, 1000, true, null), snapping = punching(punch, 4, false, null);
        float[] back = {0};
        float retracting = punching(punch, 4, true, back);
        System.out.println("worst arm turn per frame: one punch " + single + ", every 4 ticks snapping back " + snapping
            + ", pulling back " + retracting + "; farthest from the start pose as a punch begins " + back[0]);
        // Pulling back is quick, a little quicker than the punch itself, but nothing like snapping back.
        org.junit.jupiter.api.Assertions.assertTrue(retracting <= single * 1.5f, "punching every 4 ticks turns the arm " + retracting
            + " degrees in a frame, one punch at most " + single);
        org.junit.jupiter.api.Assertions.assertTrue(back[0] < 3, "the fist is " + back[0] + " degrees from the punch's start as it punches");
    }

    /**
     * Punches every {@code every} ticks for 2 seconds, as Animation_Attack plays them ({@code pullBack}) or restarting
     * each at its first frame: the most the arm turns in one frame. {@code back} gets how far the arm is from the
     * punch's starting pose when a strike begins.
     */
    private static float punching(Animation punch, float every, boolean pullBack, float[] back) throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        net.gobbob.mobends.data.Data_Player data = (net.gobbob.mobends.data.Data_Player)((sun.misc.Unsafe)field.get(null))
            .allocateInstance(net.gobbob.mobends.data.Data_Player.class);
        data.ticksPerFrame = AnimationBaking.TICKS_PER_FRAME;
        ModelBendsPlayer start = AnimationBaking.model(0, 0);
        AnimationBaking.frame(start, m -> BendsAnimator.apply(m, punch,
            AnimationProcessor.evaluate(punch, 0, BendsAnimator.context(m, null)), 1000, 1), 1000);
        org.joml.Matrix4f startArm = arm(start);
        ModelBendsPlayer model = AnimationBaking.model(0, 0);
        float worst = 0;
        org.joml.Matrix4f last = null;
        boolean[] retracts = new boolean[1];
        for (float t = 0; t < 40; t += AnimationBaking.TICKS_PER_FRAME) {
            final float now = t;
            int instance = (int)(t / every);
            float since = t - instance * every;
            retracts[0] = pullBack && instance > 0 && every < net.gobbob.mobends.animation.player.Animation_Attack.FIST_OUT_TICKS;
            data.ticks = t;
            AnimationBaking.frame(model, m -> BendsAnimator.playOnce(m, data, punch, "punch@" + (pullBack ? instance : 0),
                pullBack ? net.gobbob.mobends.animation.player.Animation_Attack.punchSeconds(since, retracts[0]) : since / 20.0, "punch",
                pullBack ? net.gobbob.mobends.animation.player.Animation_Attack.punchFade(retracts[0]) : 0, null));
            org.joml.Matrix4f arm = arm(model);
            if (last != null && t > 1) worst = Math.max(worst, AnimationBaking.angleBetween(last, arm));
            last = arm;
            float strike = retracts[0] ? net.gobbob.mobends.animation.player.Animation_Attack.RETRACT_TICKS : 0;
            if (back != null && instance > 0 && Math.abs(since - strike) < AnimationBaking.TICKS_PER_FRAME / 2) {
                back[0] = Math.max(back[0], AnimationBaking.angleBetween(startArm, arm));
            }
        }
        return worst;
    }

    /** The whole arm's orientation: upper arm, then forearm. */
    private static org.joml.Matrix4f arm(ModelBendsPlayer model) {
        return AnimationBaking.orientation(Bone.RIGHT_ARM.part(model)).mul(AnimationBaking.orientation(Bone.RIGHT_FORE_ARM.part(model)));
    }

    /**
     * The off hand's stance and its swings while the main hand leads the body: the mirrored animation, left arm only.
     * Everything else stays as it was; the left arm takes the right arm's pose, reflected.
     */
    @Test
    void offhandArmOnlyAnimationsMoveOnlyTheLeftArm() throws Exception {
        String dir = "src/main/resources/assets/mobends/geckolib/animations/player/";
        for (String name : new String[] {"sword_stance", "sword_2"}) {
            Animation animation = AnimationBaking.load(Paths.get(dir + "sword.animation.json")).getAnimation("animation.mobends.player." + name);
            ModelBendsPlayer plain = AnimationBaking.model(0, 0), armOnly = AnimationBaking.model(0, 0), untouched = AnimationBaking.model(0, 0);
            AnimationBaking.frame(plain, m -> BendsAnimator.apply(m, animation,
                AnimationProcessor.evaluate(animation, 0.1, BendsAnimator.context(m, null)), 1000, 1, null, null, false), 1000);
            AnimationBaking.frame(armOnly, m -> BendsAnimator.apply(m, animation,
                AnimationProcessor.evaluate(animation, 0.1, BendsAnimator.context(m, null)), 1000, 1, BendsAnimator.ALL_BUT_LEFT_ARM,
                null, true), 1000);
            AnimationBaking.frame(untouched, m -> {}, 1000);
            org.joml.Matrix4f reflect = new org.joml.Matrix4f().scale(-1, 1, 1);
            for (Bone bone : Bone.values()) {
                if (bone.part(plain) == null) continue;
                org.joml.Matrix4f actual = AnimationBaking.orientation(bone.part(armOnly));
                org.joml.Matrix4f expected = bone == Bone.LEFT_ARM || bone == Bone.LEFT_FORE_ARM
                    ? new org.joml.Matrix4f(reflect).mul(AnimationBaking.orientation(Bone.byId(BendsAnimator.mirrorName(bone.id)).part(plain))).mul(reflect)
                    : AnimationBaking.orientation(bone.part(untouched));
                float off = AnimationBaking.angleBetween(expected, actual);
                org.junit.jupiter.api.Assertions.assertTrue(off < 0.1f, name + ": " + bone.id + " off by " + off);
            }
            assertEquals(untouched.renderRotation.vSmooth, armOnly.renderRotation.vSmooth, name + ": root turned");
        }
    }

    /** Played mirrored (for the off hand), an animation's head still follows the player's look, not its reflection. */
    @Test
    void mirroredAnimationsStillLookWhereThePlayerLooks() throws Exception {
        String dir = "src/main/resources/assets/mobends/geckolib/animations/player/";
        Animation swing = AnimationBaking.load(Paths.get(dir + "mine.animation.json")).getAnimation("animation.mobends.player.mine_swing");
        for (float yaw : new float[] {-40, 25}) {
            ModelBendsPlayer plain = AnimationBaking.model(10, yaw), mirrored = AnimationBaking.model(10, -yaw);
            // The same swing, played for the other hand by a player looking the other way: exact reflections.
            AnimationBaking.frame(plain, m -> BendsAnimator.apply(m, swing, AnimationProcessor.evaluate(swing, 0.2,
                BendsAnimator.context(m, null, false)), 1000, 1, null, null, false), 1000);
            AnimationBaking.frame(mirrored, m -> BendsAnimator.apply(m, swing, AnimationProcessor.evaluate(swing, 0.2,
                BendsAnimator.context(m, null, true)), 1000, 1, null, null, true), 1000);
            org.joml.Matrix4f reflect = new org.joml.Matrix4f().scale(-1, 1, 1);
            org.joml.Matrix4f expected = new org.joml.Matrix4f(reflect).mul(AnimationBaking.orientation(Bone.HEAD.part(plain))).mul(reflect);
            float off = AnimationBaking.angleBetween(expected, AnimationBaking.orientation(Bone.HEAD.part(mirrored)));
            org.junit.jupiter.api.Assertions.assertTrue(off < 0.1f, "looking " + yaw + ": the mirrored head is off by " + off);
        }
    }

    @Test
    void mainHandAnimationsLeaveAnOccupiedOffhandArmAlone() throws Exception {
        String dir = "src/main/resources/assets/mobends/geckolib/animations/player/";
        Animation[] animations = {
            AnimationBaking.load(Paths.get(dir + "punch.animation.json")).getAnimation("animation.mobends.player.punch_right"),
            AnimationBaking.load(Paths.get(dir + "sword.animation.json")).getAnimation("animation.mobends.player.sword_1"),
            AnimationBaking.load(Paths.get(dir + "sword.animation.json")).getAnimation("animation.mobends.player.sword_stance"),
            AnimationBaking.load(Paths.get(dir + "axe.animation.json")).getAnimation("animation.mobends.player.axe_swing")};
        for (Animation animation : animations) {
            ModelBendsPlayer held = AnimationBaking.model(0, 0), free = AnimationBaking.model(0, 0);
            for (ModelBendsPlayer model : new ModelBendsPlayer[] {held, free}) {
                // The off-hand arm holding a torch out.
                Bone.LEFT_ARM.part(model).rotation.setSmooth(new Vector3f(-40, 10, 0), 1000);
                Bone.LEFT_FORE_ARM.part(model).rotation.setSmooth(new Vector3f(-30, 0, 0), 1000);
            }
            BendsAnimator.apply(held, animation, AnimationProcessor.evaluate(animation, 0.1, BendsAnimator.context(held, null)),
                BendsAnimator.FOLLOW_RATE, 1, BendsAnimator.LEFT_ARM);
            BendsAnimator.apply(free, animation, AnimationProcessor.evaluate(animation, 0.1, BendsAnimator.context(free, null)),
                BendsAnimator.FOLLOW_RATE, 1, null);
            String name = animation.name();
            for (Bone bone : new Bone[] {Bone.LEFT_ARM, Bone.LEFT_FORE_ARM}) {
                ModelRendererBends part = bone.part(held);
                assertEquals(bone == Bone.LEFT_ARM ? new Vector3f(-40, 10, 0) : new Vector3f(-30, 0, 0), part.rotation.vFinal,
                    name + " moved " + bone.id);
                assertEquals(new Vector3f(0, 0, 0), part.aim.vFinal, name + " aimed " + bone.id);
            }
            // With the off hand free, the same animation does use the left arm.
            assertNotEquals(new Vector3f(-40, 10, 0), Bone.LEFT_ARM.part(free).rotation.vFinal, name + " never used the left arm");
        }
    }
}
