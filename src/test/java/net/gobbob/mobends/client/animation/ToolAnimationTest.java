package net.gobbob.mobends.client.animation;

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
import java.util.function.BiConsumer;
import net.gobbob.mobends.animation.player.Animation_Axe;
import net.gobbob.mobends.animation.player.Animation_Bow;
import net.gobbob.mobends.animation.player.Animation_Mining;
import net.gobbob.mobends.animation.player.Animation_Stand;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.junit.jupiter.api.Test;

/**
 * The bow, mining and axe were baked from their Java animations as they look in game, and must keep matching them.
 * Run with {@code MOBENDS_BAKE_TOOLS=1} to bake them again.
 */
class ToolAnimationTest {
    private static final Path DIR = Paths.get("src/main/resources/assets/mobends/geckolib/animations/player");
    /** One tool swing, looping on the player's age: 1.8 swings a second. */
    private static final double SWING_PERIOD = 20 / 1.8;
    /** How long the aim is baked for after the draw: long enough for it to settle. */
    private static final float AIM_TICKS = 20;
    private static final float[][] LOOKS = {{0, 0}, {20, -30}, {-40, 15}};

    // --- Baking ---------------------------------------------------------------------------------------------------

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_TOOLS"))) return;
        // Mining turns the arm out a little more or less as it swings, following the look nonlinearly: baked looking
        // ahead (within a degree for most looks).
        Map<String, String[]> mineOverrides = Collections.singletonMap("rightArm rotation", new String[] {null, "%s", null});
        write("mine",
            pose("mine", (m, t) -> Animation_Mining.apply(m, 0, false)),
            loop("mine_swing", (m, t) -> Animation_Mining.apply(m, t, true), mineOverrides));
        // Chopping, the left hand holds the handle below the right (AxeGrip) instead of the Java's loose arm.
        write("axe",
            axe("axe", 20, (m, t) -> Animation_Axe.apply(m, 0, false)),
            axe("axe_swing", SWING_PERIOD, (m, t) -> Animation_Axe.apply(m, t, true)));

        // Aiming, the bow arm follows the look down but rises no higher than 120 degrees.
        Map<String, String[]> aimOverrides = Collections.singletonMap("leftArmAim rotation",
            new String[] {"math.max(q.mobends_head_pitch + %s, -120)", null, null});
        AnimationBaking.Derived draw = AnimationBaking.derive(ToolAnimationTest::draw, null, 0, 3, 6, 9.9f);
        AnimationBaking.Derived aim = AnimationBaking.derive(ToolAnimationTest::aim, aimOverrides, 0, 2, 4, 10);
        write("bow",
            AnimationBaking.bakeOnce("bow_draw", Animation_Bow.DRAW_TICKS, draw.channels, draw.templates,
                (m, t) -> draw(m, 0f), ToolAnimationTest::draw),
            AnimationBaking.bakeOnce("bow_aim", AIM_TICKS, aim.channels, aim.templates, ToolAnimationTest::drawLeadIn,
                ToolAnimationTest::aim));
    }

    private static String pose(String name, BiConsumer<ModelBendsPlayer, Float> java) {
        AnimationBaking.Derived derived = AnimationBaking.derive(java, null, 0);
        return AnimationBaking.bake(name, 20, derived.channels, derived.templates, new LinkedHashMap<String, String>(), java);
    }

    private static String loop(String name, BiConsumer<ModelBendsPlayer, Float> java, Map<String, String[]> overrides) {
        AnimationBaking.Derived derived = AnimationBaking.derive(java, overrides, 0, 2, 4, 6, 8, 10);
        return AnimationBaking.bake(name, SWING_PERIOD, derived.channels, derived.templates, new LinkedHashMap<String, String>(), java);
    }

    /** The Java axe for everything but the left arm, which holds the handle wherever the right hand takes the axe. */
    private static String axe(String name, double period, BiConsumer<ModelBendsPlayer, Float> java) {
        AnimationBaking.Derived derived = AnimationBaking.derive(java, null, 0, 2, 4, 6, 8, 10);
        for (String channel : LEFT_ARM) {
            derived.channels.remove(channel);
            derived.templates.remove(channel);
        }
        AxeGrip grip = new AxeGrip();
        AxeGrip.Solution[] now = new AxeGrip.Solution[1];
        derived.channels.put(LEFT_ARM[0], m -> (now[0] = grip.solve(m)).aim);
        derived.channels.put(LEFT_ARM[1], m -> AxeGrip.LEFT_ARM_ROTATION.clone());
        derived.channels.put(LEFT_ARM[2], m -> new float[] {now[0].bend, 0, 0});
        derived.channels.put("leftArmAim position", m -> now[0].shoulder);
        return AnimationBaking.bake(name, period, derived.channels, derived.templates, new LinkedHashMap<String, String>(), java);
    }

    private static final String[] LEFT_ARM = {"leftArmAim rotation", "leftArm rotation", "leftForeArm rotation"};

    private static void draw(ModelBendsPlayer model, Float ticks) {
        Animation_Bow.apply(model, ticks);
    }

    private static void aim(ModelBendsPlayer model, Float ticks) {
        Animation_Bow.apply(model, Animation_Bow.DRAW_TICKS + ticks);
    }

    /** Holding the draw's first frame, then drawing: the 400 ticks before the aim. */
    private static void drawLeadIn(ModelBendsPlayer model, Float ticks) {
        Animation_Bow.apply(model, Math.max(0, ticks - (400 - Animation_Bow.DRAW_TICKS)));
    }

    private static void write(String file, String... animations) throws IOException {
        Files.write(DIR.resolve(file + ".animation.json"), AnimationBaking.file(animations).getBytes(StandardCharsets.UTF_8));
    }

    // --- Parity ---------------------------------------------------------------------------------------------------

    @Test
    void miningMatches() throws Exception {
        BakedAnimations file = load("mine");
        assertTool(file, "mine", Animation_Mining::apply);
    }

    @Test
    void axeMatches() throws Exception {
        BakedAnimations file = load("axe");
        assertTool(file, "axe", Animation_Axe::apply, "leftArm", "leftForeArm");
    }

    /** Holding the axe and chopping, the left hand stays on the handle, a hand's width below the right. */
    @Test
    void handsJoinOnTheAxe() throws Exception {
        BakedAnimations file = load("axe");
        Animation stand = load("stand").getAnimation("animation.mobends.player.stand");
        Animation hold = file.getAnimation("animation.mobends.player.axe");
        Animation swing = file.getAnimation("animation.mobends.player.axe_swing");
        for (boolean swinging : new boolean[] {false, true}) {
            ModelBendsPlayer model = AnimationBaking.model(15, -20);
            float worstOff = 0, worstAlong = 0;
            for (float t = 0; t < 700; t += AnimationBaking.TICKS_PER_FRAME) {
                final float now = t;
                AnimationBaking.frame(model, m -> {
                    apply(m, stand, (now / 20.0) % stand.length());
                    apply(m, hold, 0);
                    if (swinging) apply(m, swing, (now / 20.0) % swing.length());
                });
                if (t < 500) continue;
                org.joml.Vector3f hand = AxeGrip.leftHand(model), target = AxeGrip.leftTarget(model);
                org.joml.Vector3f[] line = AxeGrip.handle(model);
                org.joml.Vector3f fromLine = new org.joml.Vector3f(hand).sub(line[0]);
                fromLine.sub(new org.joml.Vector3f(line[1]).mul(fromLine.dot(line[1])));
                worstOff = Math.max(worstOff, fromLine.length() * 16);
                worstAlong = Math.max(worstAlong, Math.abs(new org.joml.Vector3f(hand).sub(target).dot(line[1])) * 16);
            }
            String what = swinging ? "chopping" : "holding the axe";
            assertTrue(worstOff < 0.75f, what + ": the left hand is " + worstOff + " px off the handle");
            assertTrue(worstAlong < 1, what + ": the left hand is " + worstAlong + " px from its place along the handle");
        }
    }

    interface Tool {
        void apply(ModelBendsPlayer model, float ticks, boolean swinging);
    }

    /** Holding the tool while standing, and swinging it, over the Java stand and the ported one. */
    private static void assertTool(BakedAnimations file, String name, Tool java, String... authoredParts) throws IOException {
        Animation stand = load("stand").getAnimation("animation.mobends.player.stand");
        Animation hold = file.getAnimation("animation.mobends.player." + name);
        Animation swing = file.getAnimation("animation.mobends.player." + name + "_swing");
        for (boolean swinging : new boolean[] {false, true}) {
            for (float[] look : LOOKS) {
                String[] where = {""};
                float worst = AnimationBaking.compare(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
                    (m, t) -> {
                        Animation_Stand.apply(m, t);
                        java.apply(m, t, swinging);
                    }, (m, t) -> {
                        apply(m, stand, (t / 20.0) % stand.length());
                        apply(m, hold, 0);
                        if (swinging) apply(m, swing, (t / 20.0) % swing.length());
                    }, where, authoredParts);
                assertTrue(worst < 1.5f, name + (swinging ? " swinging" : "") + ", look " + look[0] + "/" + look[1] + ": differs by "
                    + worst + " degrees (" + where[0] + ")");
            }
        }
    }

    @Test
    void bowMatches() throws Exception {
        BakedAnimations file = load("bow");
        Animation draw = file.getAnimation("animation.mobends.player.bow_draw");
        Animation aim = file.getAnimation("animation.mobends.player.bow_aim");
        for (float[] look : LOOKS) {
            String[] where = {""};
            float drawing = AnimationBaking.compareOnce(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
                (m, t) -> draw(m, 0f), (m, t) -> apply(m, draw, 0), ToolAnimationTest::draw, (m, t) -> apply(m, draw, t / 20.0),
                Animation_Bow.DRAW_TICKS, 0, where);
            assertTrue(drawing < 1.5f, "bow_draw, look " + look[0] + "/" + look[1] + ": differs by " + drawing + " degrees (" + where[0] + ")");
            // Aiming, the body turns to the look and the bow arm follows it down to its limit: the file follows the
            // look from the first frame (in game the switch fades, see PlayerAnimator.playOnce), and limits the arm as
            // it eases rather than where it eases to. The Java eases into both, so looking away from straight ahead,
            // compare once that has settled.
            float aiming = AnimationBaking.compareOnce(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
                ToolAnimationTest::drawLeadIn,
                (m, t) -> apply(m, draw, Math.max(0, t - (400 - Animation_Bow.DRAW_TICKS)) / 20.0),
                ToolAnimationTest::aim, (m, t) -> apply(m, aim, t / 20.0), 2 * AIM_TICKS, look[0] == 0 && look[1] == 0 ? 0 : 20, where);
            assertTrue(aiming < 1.5f, "bow_aim, look " + look[0] + "/" + look[1] + ": differs by " + aiming + " degrees (" + where[0] + ")");
        }
    }

    private static void apply(ModelBendsPlayer model, Animation animation, double seconds) {
        PlayerAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, seconds, PlayerAnimator.context(model, null)),
            PlayerAnimator.FOLLOW_RATE, 1);
    }

    private static BakedAnimations load(String file) throws IOException {
        return AnimationBaking.load(DIR.resolve(file + ".animation.json"));
    }
}
