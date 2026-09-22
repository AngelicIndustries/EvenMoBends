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
