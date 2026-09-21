package net.gobbob.mobends.client.animation;

import static net.gobbob.mobends.client.animation.AnimationBaking.KEEP;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import net.gobbob.mobends.animation.player.Animation_Sneak;
import net.gobbob.mobends.animation.player.Animation_Sprint;
import net.gobbob.mobends.animation.player.Animation_Stand;
import net.gobbob.mobends.animation.player.Animation_Walk;
import net.gobbob.mobends.client.animation.AnimationBaking.Channel;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.junit.jupiter.api.Test;

/**
 * Stand, walk, sprint and sneak were baked from their Java animations as they look in game, and must keep matching
 * them. Walk, sprint and sneak are timed by the stride (vanilla's limb swing, which advances by the limb swing amount
 * each tick), their limbs' swing played over them by that amount. Run with {@code MOBENDS_BAKE_MOVEMENT=1} to bake.
 */
class MovementAnimationTest {
    private static final Path DIR = Paths.get("src/main/resources/assets/mobends/geckolib/animations/player");
    /** Typical limb swing amounts: walking, sprinting, sneaking (vanilla: distance per tick times 4, at most 1). */
    private static final float WALK = 0.86f, SPRINT = 1.0f, SNEAK = 0.26f;
    private static final String LOOK = AnimationBaking.hold("\"q.mobends_head_pitch\"", "\"q.mobends_head_yaw\"", "0");
    private static final Bone[] LIMBS = {Bone.RIGHT_ARM, Bone.LEFT_ARM, Bone.RIGHT_LEG, Bone.LEFT_LEG};
    private static final Bone[] FORE_LIMBS = {Bone.RIGHT_FORE_ARM, Bone.LEFT_FORE_ARM, Bone.RIGHT_FORE_LEG, Bone.LEFT_FORE_LEG};

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_MOVEMENT"))) return;
        // Stand: breathing and arm sway on the player's age (cos(ticks / 10)).
        Map<String, Channel> stand = new LinkedHashMap<String, Channel>();
        stand.put("body rotation", m -> xyz(Bone.BODY.part(m).rotation.vSmooth));
        for (Bone bone : LIMBS) stand.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vSmooth));
        for (Bone bone : FORE_LIMBS) stand.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vSmooth));
        Map<String, String> standFixed = new LinkedHashMap<String, String>();
        standFixed.put("head rotation", LOOK);
        write("stand", AnimationBaking.bake("stand", 20 * Math.PI, stand, standFixed, (m, t) -> Animation_Stand.apply(m, t)));

        write("walk", strideAnimations("walk", WALK, 0.1f, 10, (m, t) -> Animation_Walk.apply(m), false));
        write("sprint", strideAnimations("sprint", SPRINT, 0.3f, 20, (m, t) -> Animation_Sprint.apply(m), true));
        String[] sneakWalk = strideAnimations("sneak_walk", SNEAK, 0.1f, 10, (m, t) -> {
            Animation_Walk.apply(m);
            Animation_Sneak.apply(m);
        }, false);
        write("sneak", sneak(), sneakWalk[0], sneakWalk[1]);
    }

    /**
     * {@code name} (baked with the limbs not swinging: the body's sway, the knees' and elbows' steps, the head) and
     * {@code name_swing} (the arms and legs at full swing). The body leans into head turns: {@code leanPerDegree} of the
     * head's yaw, up to {@code lean} degrees.
     */
    private static String[] strideAnimations(String name, float amount, float leanPerDegree, float lean,
        BiConsumer<ModelBendsPlayer, Float> java, boolean rootMoves) {
        Map<String, Channel> main = new LinkedHashMap<String, Channel>();
        Map<String, String[]> templates = new LinkedHashMap<String, String[]>();
        if (rootMoves) main.put("root position", m -> new float[] {-m.renderOffset.vSmooth.x, m.renderOffset.vSmooth.y, m.renderOffset.vSmooth.z});
        main.put("body rotation", m -> xyz(Bone.BODY.part(m).rotation.vSmooth));
        templates.put("body rotation", new String[] {null, null,
            "-math.clamp(q.mobends_head_yaw * " + leanPerDegree + ", -" + (int)lean + ", " + (int)lean + ")"});
        // The head counters the body's sway: its turn on the neck, its tilt around the look.
        main.put("neck rotation", m -> new float[] {0, Bone.HEAD.part(m).rotation.vSmooth.y, 0});
        main.put("head rotation", m -> new float[] {Bone.HEAD.part(m).rotation.vSmooth.x, 0, 0});
        templates.put("head rotation", new String[] {"q.mobends_head_pitch + %s", "q.mobends_head_yaw", "0"});
        for (Bone bone : LIMBS) main.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vSmooth));
        for (Bone bone : FORE_LIMBS) main.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vSmooth));
        Map<String, Channel> swing = new LinkedHashMap<String, Channel>();
        for (Bone bone : LIMBS) swing.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vSmooth));
        double period = stridePeriod(amount);
        return new String[] {
            AnimationBaking.bake(name, period, main, templates, new LinkedHashMap<String, String>(), stride(java, amount, 0)),
            AnimationBaking.bake(name + "_swing", period, swing, new LinkedHashMap<String, String[]>(),
                new LinkedHashMap<String, String>(), stride(java, amount, 1))};
    }

    /**
     * The crouch over standing: only the axes the Java sneak sets, the rest kept for the stand underneath. The stride
     * is frozen while standing, so it holds the sneak's targets at the phase the player stopped at.
     */
    private static String sneak() {
        Map<String, Channel> main = new LinkedHashMap<String, Channel>();
        Map<String, String[]> templates = new LinkedHashMap<String, String[]>();
        main.put("body rotation", m -> xyz(Bone.BODY.part(m).rotation.vFinal));
        templates.put("body rotation", new String[] {null, KEEP, KEEP});
        main.put("head rotation", m -> new float[] {Bone.HEAD.part(m).rotation.vFinal.x, 0, 0});
        templates.put("head rotation", new String[] {"q.mobends_head_pitch + %s", KEEP, KEEP});
        for (Bone bone : LIMBS) {
            main.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vFinal));
            boolean arm = bone == Bone.RIGHT_ARM || bone == Bone.LEFT_ARM;
            templates.put(bone.id + " rotation", arm ? new String[] {null, KEEP, KEEP} : new String[] {null, KEEP, null});
        }
        for (Bone bone : FORE_LIMBS) {
            main.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vFinal));
            templates.put(bone.id + " rotation", new String[] {null, KEEP, KEEP});
        }
        return AnimationBaking.bakePose("sneak", stridePeriod(SNEAK), main, templates,
            stride((m, t) -> Animation_Sneak.apply(m), SNEAK, 0));
    }

    /** Ticks per stride at a limb swing amount: vanilla adds the amount to the limb swing every tick. */
    private static double stridePeriod(float amount) {
        return 2 * Math.PI / (0.6662 * amount);
    }

    /** Runs a Java stride animation with the limbs advancing at {@code cadence} and swinging by {@code amount}. */
    private static BiConsumer<ModelBendsPlayer, Float> stride(BiConsumer<ModelBendsPlayer, Float> java, float cadence, float amount) {
        return (model, ticks) -> {
            model.armSwing = ticks * cadence;
            model.armSwingAmount = amount;
            java.accept(model, ticks);
        };
    }

    private static void write(String file, String... animations) throws IOException {
        Files.write(DIR.resolve(file + ".animation.json"), AnimationBaking.file(animations).getBytes(StandardCharsets.UTF_8));
    }

    // --- Parity ---------------------------------------------------------------------------------------------------

    @Test
    void standMatchesTheJavaStand() throws Exception {
        Animation stand = load("stand").getAnimation("animation.mobends.player.stand");
        for (float[] look : new float[][] {{0, 0}, {30, -40}}) {
            assertMatches("stand", look, (m, t) -> Animation_Stand.apply(m, t), (m, t) -> loop(m, stand, t));
        }
    }

    @Test
    void walkAndSprintMatchTheJavaOnes() throws Exception {
        BakedAnimations walk = load("walk"), sprint = load("sprint");
        for (float[] look : new float[][] {{0, 0}, {20, 30}, {-30, -60}}) {
            assertMatches("walk", look, walking((m, t) -> Animation_Walk.apply(m), WALK),
                walking((m, t) -> stride(m, walk, "walk"), WALK));
            assertMatches("sprint", look, walking((m, t) -> Animation_Sprint.apply(m), SPRINT),
                walking((m, t) -> stride(m, sprint, "sprint"), SPRINT));
        }
    }

    @Test
    void sneakMatchesOverWalkAndStand() throws Exception {
        BakedAnimations walk = load("walk"), sneak = load("sneak");
        Animation stand = load("stand").getAnimation("animation.mobends.player.stand");
        for (float[] look : new float[][] {{0, 0}, {25, 20}}) {
            assertMatches("sneak walking", look,
                walking((m, t) -> {
                    Animation_Walk.apply(m);
                    Animation_Sneak.apply(m);
                }, SNEAK),
                walking((m, t) -> {
                    stride(m, walk, "walk");
                    stride(m, sneak, "sneak_walk");
                }, SNEAK));
            // Stopped at different points of the stride, which the crouch then holds.
            for (float stoppedAt : new float[] {0, 2.3f, 5.1f, 8.4f}) {
                assertMatches("sneak standing at limb swing " + stoppedAt, look,
                    (m, t) -> {
                        m.armSwing = stoppedAt;
                        m.armSwingAmount = 0;
                        Animation_Stand.apply(m, t);
                        Animation_Sneak.apply(m);
                    },
                    (m, t) -> {
                        m.armSwing = stoppedAt;
                        m.armSwingAmount = 0;
                        loop(m, stand, t);
                        pose(m, sneak.getAnimation("animation.mobends.player.sneak"));
                    });
            }
        }
    }

    private static void assertMatches(String what, float[] look, BiConsumer<ModelBendsPlayer, Float> java,
        BiConsumer<ModelBendsPlayer, Float> baked) {
        String[] where = {""};
        float worst = AnimationBaking.compare(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
            java, baked, where);
        assertTrue(worst < 1.5f, what + ", look " + look[0] + "/" + look[1] + ": differs by " + worst + " degrees (" + where[0] + ")");
    }

    /** Walking: the limb swing advances by {@code amount} per tick and swings by it. */
    private static BiConsumer<ModelBendsPlayer, Float> walking(BiConsumer<ModelBendsPlayer, Float> animate, float amount) {
        return (model, ticks) -> {
            model.armSwing = ticks * amount;
            model.armSwingAmount = amount;
            animate.accept(model, ticks);
        };
    }

    /** As PlayerAnimator.playStride. */
    private static void stride(ModelBendsPlayer model, BakedAnimations file, String name) {
        Animation main = file.getAnimation("animation.mobends.player." + name);
        Animation swing = file.getAnimation("animation.mobends.player." + name + "_swing");
        double stride = model.armSwing * 0.6662 / (2 * Math.PI);
        stride -= Math.floor(stride);
        PlayerAnimator.apply(model, main, AnimationProcessor.evaluate(main, stride * main.length(), PlayerAnimator.context(model, null)));
        PlayerAnimator.apply(model, swing, AnimationProcessor.evaluate(swing, stride * swing.length(), PlayerAnimator.context(model, null)),
            PlayerAnimator.FOLLOW_RATE, model.armSwingAmount);
    }

    /** As PlayerAnimator.playStridePose. */
    private static void pose(ModelBendsPlayer model, Animation animation) {
        double stride = model.armSwing * 0.6662 / (2 * Math.PI);
        stride -= Math.floor(stride);
        PlayerAnimator.apply(model, animation,
            AnimationProcessor.evaluate(animation, stride * animation.length(), PlayerAnimator.context(model, null)));
    }

    private static void loop(ModelBendsPlayer model, Animation animation, float ticks) {
        PlayerAnimator.apply(model, animation,
            AnimationProcessor.evaluate(animation, (ticks / 20.0) % animation.length(), PlayerAnimator.context(model, null)));
    }

    private static BakedAnimations load(String file) throws IOException {
        return AnimationBaking.load(DIR.resolve(file + ".animation.json"));
    }
}
