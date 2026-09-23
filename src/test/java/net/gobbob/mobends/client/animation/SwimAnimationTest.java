package net.gobbob.mobends.client.animation;

import static net.gobbob.mobends.client.animation.AnimationBaking.xyz;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.gobbob.mobends.animation.player.Animation_Swimming;
import net.gobbob.mobends.client.animation.AnimationBaking.Channel;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import org.junit.jupiter.api.Test;

/**
 * The Blockbench swim ({@code player/swim.animation.json}) was baked from the Java swim as it looks in game, and must
 * keep matching it outside the deliberately reauthored moving arms. Never re-bake over authored changes.
 */
class SwimAnimationTest {
    static final Path FILE = Paths.get("src/main/resources/assets/mobends/geckolib/animations/player/swim.animation.json");
    /** Upper body and legs loop at unrelated speeds (ticks * speed in radians), so each is its own animation. */
    private static final float MOVING_ARMS = 0.1625f, MOVING_LEGS = 0.4625f, IDLE_ARMS = 0.0825f, IDLE_LEGS = 0.2625f;
    private static final String LOOK = AnimationBaking.hold("\"q.mobends_head_pitch\"", "\"q.mobends_head_yaw\"", "0");

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_SWIM"))) return;
        Files.write(FILE, AnimationBaking.file(
            bake("swim", true, MOVING_ARMS, upper(true)),
            bake("swim_legs", true, MOVING_LEGS, legs()),
            bake("swim_idle", false, IDLE_ARMS, upper(false)),
            bake("swim_idle_legs", false, IDLE_LEGS, legs())).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void idleAndUneditedMovingPartsMatchTheJavaSwim() throws Exception {
        BakedAnimations file = AnimationBaking.load(FILE);
        for (boolean moving : new boolean[] {true, false}) {
            String upper = moving ? "swim" : "swim_idle";
            Animation upperAnimation = file.getAnimation("animation.mobends.player." + upper);
            Animation legsAnimation = file.getAnimation("animation.mobends.player." + upper + "_legs");
            for (float[] look : new float[][] {{0, 0}, {35, -20}, {-50, 40}}) {
                String[] where = {""};
                float worst = AnimationBaking.compare(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
                    (model, ticks) -> Animation_Swimming.apply(model, ticks, moving),
                    (model, ticks) -> {
                        for (Animation animation : new Animation[] {upperAnimation, legsAnimation}) {
                            double seconds = (ticks / 20.0) % animation.length();
                            BendsAnimator.apply(model, animation,
                                AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(model, null)));
                        }
                    }, where, moving ? new String[] {"rightArm", "rightForeArm", "leftArm", "leftForeArm"} : new String[0]);
                assertTrue(worst < 1.5f, upper + ", look " + look[0] + "/" + look[1] + ": differs by " + worst
                    + " degrees (" + where[0] + ")");
            }
        }
    }

    private static String bake(String name, boolean moving, float speed, Map<String, Channel> channels) {
        Map<String, String> fixed = channels.containsKey("neck rotation")
            ? Collections.singletonMap("head rotation", LOOK) : Collections.<String, String>emptyMap();
        return AnimationBaking.bake(name, 2 * Math.PI / speed, channels, fixed,
            (model, ticks) -> Animation_Swimming.apply(model, ticks, moving));
    }

    private static Map<String, Channel> upper(boolean moving) {
        Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        channels.put("root rotation", m -> new float[] {m.renderRotation.vSmooth.x, 0, 0});
        // renderOffset is the root position as (-x, y, z); its resting y is -1.
        channels.put("root position", m -> new float[] {-m.renderOffset.vSmooth.x, m.renderOffset.vSmooth.y, m.renderOffset.vSmooth.z});
        channels.put("body rotation", m -> xyz(((ModelRendererBends)m.bipedBody).rotation.vSmooth));
        // The head tilts back with the neck (its pre-rotation) and follows the look with its own rotation.
        channels.put("neck rotation", m -> new float[] {((ModelRendererBends)m.bipedHead).pre_rotation.vSmooth.x, 0, 0});
        Map<String, Channel> arms = new LinkedHashMap<String, Channel>();
        AnimationBaking.putArms(arms);
        for (Bone side : new Bone[] {Bone.RIGHT_ARM, Bone.LEFT_ARM}) {
            Bone foreArm = side == Bone.RIGHT_ARM ? Bone.RIGHT_FORE_ARM : Bone.LEFT_FORE_ARM;
            channels.put(side.aim().id + " rotation", arms.get(side.aim().id + " rotation"));
            channels.put(side.id + " rotation", arms.get(side.id + " rotation"));
            channels.put(foreArm.id + " rotation", arms.get(foreArm.id + " rotation"));
        }
        if (moving) channels.put("rightItem rotation", m -> xyz(m.renderItemRotation.vSmooth));
        return channels;
    }

    private static Map<String, Channel> legs() {
        Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        AnimationBaking.putLegs(channels);
        return channels;
    }
}
