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
