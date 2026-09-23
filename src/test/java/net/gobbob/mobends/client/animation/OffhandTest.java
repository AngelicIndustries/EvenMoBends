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
                AnimationBaking.frame(plain, m -> PlayerAnimator.apply(m, animation,
                    AnimationProcessor.evaluate(animation, seconds, PlayerAnimator.context(m, null)), 1000, 1, null, null, false), 1000);
                AnimationBaking.frame(mirrored, m -> PlayerAnimator.apply(m, animation,
                    AnimationProcessor.evaluate(animation, seconds, PlayerAnimator.context(m, null)), 1000, 1, null, null, true), 1000);
                org.joml.Matrix4f reflect = new org.joml.Matrix4f().scale(-1, 1, 1);
                for (Bone bone : Bone.values()) {
                    if (bone.part(plain) == null) continue;
                    Bone other = Bone.byId(PlayerAnimator.mirrorName(bone.id));
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
     * With the off hand full, every punch is the right fist's: punching again mid-punch fades from where the arm is
     * instead of snapping back to the punch's first frame, so the arm never jumps more than a punch moves it anyway.
     */
    @Test
    void repeatedPunchesWithOneFistDontSnapBack() throws Exception {
        Animation punch = AnimationBaking.load(Paths.get("src/main/resources/assets/mobends/geckolib/animations/player/punch.animation.json"))
            .getAnimation("animation.mobends.player.punch_right");
        float single = worstJump(punch, 1000, true), restarted = worstJump(punch, 4, false), faded = worstJump(punch, 4, true);
        System.out.println("worst arm jump per frame: one punch " + single + ", every 4 ticks snapping " + restarted + ", fading " + faded);
        org.junit.jupiter.api.Assertions.assertTrue(faded <= single + 1, "punching every 4 ticks jumps " + faded
            + " degrees in a frame, one punch at most " + single);
    }

    /** The most the right arm turns in one frame, punching every {@code every} ticks for 2 seconds. */
    private static float worstJump(Animation punch, float every, boolean fade) throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        net.gobbob.mobends.data.Data_Player data = (net.gobbob.mobends.data.Data_Player)((sun.misc.Unsafe)field.get(null))
            .allocateInstance(net.gobbob.mobends.data.Data_Player.class);
        data.ticksPerFrame = AnimationBaking.TICKS_PER_FRAME;
        ModelBendsPlayer model = AnimationBaking.model(0, 0);
        float worst = 0;
        org.joml.Matrix4f last = null;
        for (float t = 0; t < 40; t += AnimationBaking.TICKS_PER_FRAME) {
            final float now = t;
            int instance = (int)(t / every);
            data.ticks = t;
            AnimationBaking.frame(model, m -> PlayerAnimator.playOnce(m, data, punch, "punch@" + (fade ? instance : 0),
                (now - instance * every) / 20.0, "punch", fade ? 0.15 : 0, null));
            org.joml.Matrix4f arm = AnimationBaking.orientation(Bone.RIGHT_ARM.part(model))
                .mul(AnimationBaking.orientation(Bone.RIGHT_FORE_ARM.part(model)));
            if (last != null && t > 1) worst = Math.max(worst, AnimationBaking.angleBetween(last, arm));
            last = arm;
        }
        return worst;
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
            PlayerAnimator.apply(held, animation, AnimationProcessor.evaluate(animation, 0.1, PlayerAnimator.context(held, null)),
                PlayerAnimator.FOLLOW_RATE, 1, PlayerAnimator.LEFT_ARM);
            PlayerAnimator.apply(free, animation, AnimationProcessor.evaluate(animation, 0.1, PlayerAnimator.context(free, null)),
                PlayerAnimator.FOLLOW_RATE, 1, null);
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
