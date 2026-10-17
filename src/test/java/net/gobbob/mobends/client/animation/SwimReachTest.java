package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertTrue;
import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.junit.jupiter.api.Test;

/** The lying strokes reach far past the standing hitbox the game keeps in water (0.3 ahead); the upright tread barely. */
class SwimReachTest {
    @Test
    void lyingStrokesReachPastTheHitboxAndTheTreadDoesNot() throws Exception {
        BakedAnimations swim = AnimationBaking.load(SwimAnimationTest.FILE), dive = AnimationBaking.load(DiveAnimationTest.FILE);
        for (String name : new String[] {"player", "player_slim"}) {
            BakedGeoModel baked = BendsGeometryTest.load(name);
            ModelBendsPlayer model = (ModelBendsPlayer)BendsGeometryTest.models().get(name);
            float[] lying = reach(model, baked, swim.getAnimation("animation.mobends.player.swim"));
            float[] diving = reach(model, baked, dive.getAnimation("animation.mobends.player.dive"));
            float[] tread = reach(model, baked, swim.getAnimation("animation.mobends.player.swim_idle"));
            assertTrue(lying[0] > 1.0f && lying[0] < 1.6f && lying[1] > 0.5f && lying[1] < 0.9f, name + " swim " + lying[0] + "/" + lying[1]);
            assertTrue(diving[0] > 1.0f && diving[0] < 1.6f && diving[1] > 0.5f && diving[1] < 0.9f, name + " dive " + diving[0] + "/" + diving[1]);
            assertTrue(tread[0] < 0.8f && tread[1] < 0.6f, name + " tread " + tread[0] + "/" + tread[1]);
        }
    }

    /** The farthest reach ahead and behind over the loop. */
    private static float[] reach(ModelBendsPlayer model, BakedGeoModel baked, Animation animation) {
        float[] most = new float[2], now = new float[2];
        for (int step = 0; step < 24; ++step) {
            double seconds = animation.length() * step / 24;
            AnimationBaking.frame(model, m -> BendsAnimator.apply(m, animation,
                AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(m, null)), 1000, 1), 1000);
            model.reach(baked, now);
            most[0] = Math.max(most[0], now[0]);
            most[1] = Math.max(most[1], now[1]);
        }
        return most;
    }
}
