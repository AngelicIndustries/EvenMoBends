package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import java.nio.file.Paths;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.junit.jupiter.api.Test;

class RemoteLandingPoseTest {
    @Test
    void touchdownEasesTheLegsOverTwoTicksAtDifferentFrameRates() throws Exception {
        Animation stand = AnimationBaking.load(Paths.get(
            "src/main/resources/assets/mobends/geckolib/animations/player/stand.animation.json"))
            .getAnimation("animation.mobends.player.stand");
        for (int frames : new int[] {1, 3, 7, 12}) {
            ModelBendsPlayer model = new ModelBendsPlayer();
            ModelRendererBends[] legs = {(ModelRendererBends)model.bipedRightLeg, (ModelRendererBends)model.bipedLeftLeg,
                (ModelRendererBends)model.bipedRightForeLeg, (ModelRendererBends)model.bipedLeftForeLeg};
            for (int i = 0; i < legs.length; i++) legs[i].rotation.setX(i < 2 ? -30 : 35);
            for (int frame = 0; frame < frames * 2; frame++) {
                BendsAnimator.apply(model, stand, AnimationProcessor.evaluate(stand, 0, BendsAnimator.context(model, null)),
                    BendsAnimator.blendRate(frame / (float)frames / 5), 1);
                model.settleLanding();
                for (ModelRendererBends leg : legs) leg.update(1.0f / frames);
                if (frame == 0 && frames > 1) {
                    assertTrue(legs[2].rotation.getX() > 15, "The knee must not snap straight on the first frame");
                }
            }
            for (int i = 0; i < legs.length; i++) {
                assertEquals(i < 2 ? 0 : 4, legs[i].rotation.getX(), 2.0f, "Landing at " + frames * 20 + " FPS");
            }
        }
    }
}
