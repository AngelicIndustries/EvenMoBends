package net.gobbob.mobends.client.animation;

import static net.gobbob.mobends.client.animation.AnimationBaking.xyz;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import net.gobbob.mobends.animation.player.Animation_Diving;
import net.gobbob.mobends.client.animation.AnimationBaking.Channel;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.data.Data_Player;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

/**
 * The Blockbench dive ({@code player/dive.animation.json}) was baked from the Java dive as it looks in game, and must
 * keep matching it outside the deliberately reauthored arms, for any travel direction and speed.
 */
class DiveAnimationTest {
    static final Path FILE = Paths.get("src/main/resources/assets/mobends/geckolib/animations/player/dive.animation.json");
    private static final double ARMS_PERIOD = 2 * Math.PI / 0.24, LEGS_PERIOD = 2 * Math.PI / (0.24 * 1.8);
    /** Full speed, level: the kick at full strength and no lean beyond the resting 80 degrees. */
    private static final float[] BAKE_MOTION = {0.25f, 0, 0};

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_DIVE"))) return;
        Map<String, Channel> upper = new LinkedHashMap<String, Channel>();
        // renderOffset is the root position as (-x, y, z); its resting y is -1.
        upper.put("root position", m -> new float[] {-m.renderOffset.vSmooth.x, m.renderOffset.vSmooth.y, m.renderOffset.vSmooth.z});
        upper.put("body rotation", m -> xyz(((ModelRendererBends)m.bipedBody).rotation.vSmooth));
        upper.put("neck rotation", m -> new float[] {((ModelRendererBends)m.bipedHead).pre_rotation.vSmooth.x, 0, 0});
        AnimationBaking.putArms(upper);
        Map<String, String> upperFixed = new LinkedHashMap<String, String>();
        // The body leans along the travel direction; the head keeps looking where the player looks.
        upperFixed.put("root rotation", AnimationBaking.hold("\"80 + q.mobends_travel_pitch\"", "0", "0"));
        upperFixed.put("head rotation", AnimationBaking.hold(
            "\"q.mobends_head_pitch - 0.45 * (80 + q.mobends_travel_pitch)\"", "\"q.mobends_head_yaw\"", "0"));

        Map<String, Channel> legs = new LinkedHashMap<String, Channel>();
        AnimationBaking.putLegs(legs);
        Map<String, String> still = new LinkedHashMap<String, String>();
        still.put("rightLeg rotation", AnimationBaking.hold("0", "0", "3"));
        still.put("rightForeLeg rotation", AnimationBaking.hold("8", "0", "0"));
        still.put("leftLeg rotation", AnimationBaking.hold("0", "0", "-3"));
        still.put("leftForeLeg rotation", AnimationBaking.hold("8", "0", "0"));

        Files.write(FILE, AnimationBaking.file(
            AnimationBaking.bake("dive", ARMS_PERIOD, upper, upperFixed, DiveAnimationTest::java),
            AnimationBaking.bake("dive_legs", LEGS_PERIOD, legs, new LinkedHashMap<String, String>(), DiveAnimationTest::java),
            AnimationBaking.animation("dive_legs_still", LEGS_PERIOD / 20, still)).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void uneditedDivePartsMatchTheJavaDive() throws Exception {
        BakedAnimations file = AnimationBaking.load(FILE);
        Animation upper = file.getAnimation("animation.mobends.player.dive");
        Animation legs = file.getAnimation("animation.mobends.player.dive_legs");
        Animation still = file.getAnimation("animation.mobends.player.dive_legs_still");
        float[][] motions = {{0.15f, 0, 0.05f}, {0.1f, -0.08f, 0}, {0.06f, 0.09f, 0.03f}, {0.25f, 0, 0}};
        for (float[] motion : motions) {
            Data_Player data = data();
            data.divingTravelPitch = Animation_Diving.travelPitch(motion[0], motion[1], motion[2]);
            float kick = Animation_Diving.moving(motion[0], motion[1], motion[2]);
            for (float[] look : new float[][] {{0, 0}, {30, -25}}) {
                String[] where = {""};
                float worst = AnimationBaking.compare(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
                    (model, ticks) -> Animation_Diving.apply(model, ticks, motion[0], motion[1], motion[2]),
                    (model, ticks) -> {
                        play(model, data, upper, ticks, 1);
                        play(model, data, still, ticks, 1);
                        play(model, data, legs, ticks, kick);
                    }, where, "rightArm", "rightForeArm", "leftArm", "leftForeArm");
                assertTrue(worst < 1.5f, String.format("motion %s/%s/%s, look %s/%s: differs by %s degrees (%s)",
                    motion[0], motion[1], motion[2], look[0], look[1], worst, where[0]));
            }
        }
    }

    private static void play(net.gobbob.mobends.client.model.entity.ModelBendsPlayer model, Data_Player data,
        Animation animation, float ticks, float weight) {
        double seconds = (ticks / 20.0) % animation.length();
        BendsAnimator.apply(model, animation,
            AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(model, data)), BendsAnimator.FOLLOW_RATE, weight);
    }

    private static void java(net.gobbob.mobends.client.model.entity.ModelBendsPlayer model, Float ticks) {
        Animation_Diving.apply(model, ticks, BAKE_MOTION[0], BAKE_MOTION[1], BAKE_MOTION[2]);
    }

    private static Data_Player data() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Data_Player)((Unsafe)field.get(null)).allocateInstance(Data_Player.class);
    }
}
