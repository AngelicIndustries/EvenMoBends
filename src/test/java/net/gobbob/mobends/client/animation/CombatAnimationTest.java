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
import java.util.Set;
import java.util.function.BiConsumer;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo0;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo1;
import net.gobbob.mobends.animation.player.Animation_Attack_Combo2;
import net.gobbob.mobends.animation.player.Animation_Attack_Stance;
import net.gobbob.mobends.animation.player.Animation_Blocking;
import net.gobbob.mobends.animation.player.Animation_Sprint;
import net.gobbob.mobends.animation.player.Animation_Stand;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.PlayerBlockingCompat;
import org.junit.jupiter.api.Test;

/**
 * The sword swings, their stances and blocking were baked from their Java animations as they look in game, and must
 * keep matching them. Run with {@code MOBENDS_BAKE_COMBAT=1} to bake them again.
 */
class CombatAnimationTest {
    private static final Path DIR = Paths.get("src/main/resources/assets/mobends/geckolib/animations/player");
    /** A swing lasts until 10 ticks after the click, when the stance takes over. */
    private static final float SWING_TICKS = 10;
    private static final float[][] LOOKS = {{0, 0}, {20, -30}};
    private static final Set<String> RIDING_MASK = Collections.singleton(PlayerRig.ROOT);

    // --- Baking ---------------------------------------------------------------------------------------------------

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_COMBAT"))) return;
        write("sword",
            swing("sword_1", 1, false), swing("sword_1_moving", 1, true),
            swing("sword_2", 2, false), swing("sword_2_moving", 2, true),
            swing("sword_3", 3, false),
            pose("sword_stance", (m, t) -> Animation_Attack_Stance.apply(m, false, false)),
            // The ported sprint turns the head against the body's sway on the neck; the stance's head replaces that.
            pose("sword_stance_sprint", (m, t) -> Animation_Attack_Stance.apply(m, true, true),
                Collections.singletonMap("neck rotation", AnimationBaking.hold("0", "0", "0"))));
        write("block",
            pose("block_right", (m, t) -> Animation_Blocking.apply(m, PlayerBlockingCompat.MAIN_HAND)),
            pose("block_left", (m, t) -> Animation_Blocking.apply(m, PlayerBlockingCompat.OFF_HAND)));
    }

    /**
     * A swing as it follows the swings before it in the combo ({@link #CLICK_TICKS} apart), so it eases out of the
     * last one as the Java does; the first from its own first frame.
     */
    private static String swing(String name, int attack, boolean moving) {
        BiConsumer<ModelBendsPlayer, Float> java = combo(attack, moving, false);
        AnimationBaking.Derived derived = AnimationBaking.derive(java, null, 0, 2.5f, 5, 7.5f, 9.9f);
        return AnimationBaking.bakeOnce(name, SWING_TICKS, derived.channels, derived.templates,
            leadIn(attack, (a, m, t) -> combo(a, moving, false).accept(m, t)), java);
    }

    /** Clicking through the combo: the next swing starts this many ticks after the last. */
    private static final float CLICK_TICKS = 7;

    interface Swing {
        void play(int attack, ModelBendsPlayer model, float ticks);
    }

    /**
     * The 400 ticks before swing {@code attack}: holding swing 1's first frame, then each swing before it for
     * {@link #CLICK_TICKS}.
     */
    private static BiConsumer<ModelBendsPlayer, Float> leadIn(int attack, Swing swing) {
        float start = 400 - CLICK_TICKS * (attack - 1);
        return (m, t) -> {
            if (t < start) {
                swing.play(1, m, 0);
            } else {
                int before = (int)((t - start) / CLICK_TICKS);
                swing.play(before + 1, m, t - start - before * CLICK_TICKS);
            }
        };
    }

    private static String pose(String name, BiConsumer<ModelBendsPlayer, Float> java) {
        return pose(name, java, new LinkedHashMap<String, String>());
    }

    /** A held pose, plus {@code fixed} tracks ("bone property" to JSON track). */
    private static String pose(String name, BiConsumer<ModelBendsPlayer, Float> java, Map<String, String> fixed) {
        AnimationBaking.Derived derived = AnimationBaking.derive(java, null, 0);
        return AnimationBaking.bake(name, 20, derived.channels, derived.templates, fixed, java);
    }

    private static BiConsumer<ModelBendsPlayer, Float> combo(int attack, boolean moving, boolean riding) {
        return (m, t) -> {
            if (attack == 1) Animation_Attack_Combo0.apply(m, t, moving, riding);
            else if (attack == 2) Animation_Attack_Combo1.apply(m, t, moving, riding);
            else Animation_Attack_Combo2.apply(m, t);
        };
    }

    private static void write(String file, String... animations) throws IOException {
        Files.write(DIR.resolve(file + ".animation.json"), AnimationBaking.file(animations).getBytes(StandardCharsets.UTF_8));
    }

    // --- Parity ---------------------------------------------------------------------------------------------------

    @Test
    void swingsMatch() throws Exception {
        BakedAnimations file = load("sword");
        for (float[] look : LOOKS) {
            for (int attack = 1; attack <= 3; ++attack) {
                for (boolean moving : attack < 3 ? new boolean[] {false, true} : new boolean[] {false}) {
                    String name = "sword_" + attack + (moving ? "_moving" : "");
                    String suffix = moving ? "_moving" : "";
                    Animation animation = file.getAnimation("animation.mobends.player." + name);
                    assertOnce(name, look, leadIn(attack, (a, m, t) -> combo(a, moving, false).accept(m, t)),
                        leadIn(attack, (a, m, t) -> once(m, file.getAnimation("animation.mobends.player.sword_" + a + suffix), t, null)),
                        combo(attack, moving, false), (m, t) -> once(m, animation, t, null));
                }
            }
            // Riding: the moving swing, leaving the root to the mount.
            for (int attack = 1; attack <= 2; ++attack) {
                Animation animation = file.getAnimation("animation.mobends.player.sword_" + attack + "_moving");
                assertOnce("sword_" + attack + " riding", look, leadIn(attack, (a, m, t) -> combo(a, true, true).accept(m, t)),
                    leadIn(attack, (a, m, t) -> once(m, file.getAnimation("animation.mobends.player.sword_" + a + "_moving"), t, RIDING_MASK)),
                    combo(attack, true, true), (m, t) -> once(m, animation, t, RIDING_MASK));
            }
        }
    }

    /**
     * Clicking through the combo, from standing or the stance, and settling back into the stance, as it plays in
     * game (swings fading in from where the body was, the stance blending in). Each swing was baked out of the one
     * before it, so switches within the combo stay close; entering it from elsewhere fades differently from the
     * Java's eases for a few ticks.
     */
    /**
     * Clicking through the combo, from standing or the stance, and settling back into the stance, as it plays in game
     * (swings fading in from where the body was, the stance blending in). Each swing was baked out of the one before
     * it, 7 ticks in; at other switches the fade eases differently from the Java (the held sword turning into the grip
     * most), within a few ticks, and the stance ends up matching.
     */
    @Test
    void comboChainStaysClose() throws Exception {
        for (float click : new float[] {7, 10}) {
            for (boolean fromStance : new boolean[] {false, true}) {
                String what = "combo clicked every " + click + " ticks from " + (fromStance ? "the stance" : "standing");
                String[] where = {""};
                float through = chain(click, fromStance, 0, 4 * click + 3, where);
                assertTrue(through < 45, what + ": differs by " + through + " degrees (" + where[0] + ")");
                float settled = chain(click, fromStance, 4 * click + 40, 4 * click + 60, where);
                assertTrue(settled < 1.5f, what + ", back in the stance: differs by " + settled + " degrees (" + where[0] + ")");
            }
        }
    }

    /** Swings 1, 2, 3, 1 every {@code click} ticks, then the stance; compared from {@code from} to {@code to} ticks. */
    private static float chain(float click, boolean fromStance, float from, float to, String[] where) throws Exception {
        BakedAnimations file = load("sword");
        Animation stand = load("stand").getAnimation("animation.mobends.player.stand");
        Animation stance = file.getAnimation("animation.mobends.player.sword_stance");
        int[] order = {1, 2, 3, 1};
        net.gobbob.mobends.data.Data_Player data = data();
        data.ticksPerFrame = AnimationBaking.TICKS_PER_FRAME;
        BiConsumer<ModelBendsPlayer, Float> java = (m, t) -> {
            Animation_Stand.apply(m, 400 + t);
            int k = (int)(t / click);
            if (k >= order.length) Animation_Attack_Stance.apply(m, false, false);
            else combo(order[k], false, false).accept(m, t - k * click);
        };
        BiConsumer<ModelBendsPlayer, Float> baked = (m, t) -> {
            data.ticks = 400 + t;
            loop(m, stand, 400 + t);
            int k = (int)(t / click);
            if (k >= order.length) {
                float progress = data.ticksPlaying("attack", "stance") / (float)(BendsAnimator.LOCOMOTION_BLEND * 20);
                BendsAnimator.apply(m, stance, AnimationProcessor.evaluate(stance, 0, BendsAnimator.context(m, data)),
                    BendsAnimator.blendRate(progress), 1);
            } else {
                Animation swing = file.getAnimation("animation.mobends.player.sword_" + order[k]);
                BendsAnimator.playOnce(m, data, swing, "sword_" + order[k], (t - k * click) / 20.0, "attack",
                    net.gobbob.mobends.animation.player.Animation_Attack.SWING_FADE, null);
            }
        };
        BiConsumer<ModelBendsPlayer, Float> before = (m, t) -> {
            Animation_Stand.apply(m, t);
            if (fromStance) Animation_Attack_Stance.apply(m, false, false);
        };
        BiConsumer<ModelBendsPlayer, Float> bakedBefore = (m, t) -> {
            data.ticks = t;
            loop(m, stand, t);
            if (fromStance) pose(m, stance);
        };
        return AnimationBaking.compareOnce(AnimationBaking.model(10, -20), AnimationBaking.model(10, -20),
            before, bakedBefore, java, baked, to, from, where);
    }

    private static net.gobbob.mobends.data.Data_Player data() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (net.gobbob.mobends.data.Data_Player)((sun.misc.Unsafe)field.get(null))
            .allocateInstance(net.gobbob.mobends.data.Data_Player.class);
    }

    @Test
    void stancesMatch() throws Exception {
        BakedAnimations file = load("sword");
        Animation still = file.getAnimation("animation.mobends.player.sword_stance");
        Animation sprint = file.getAnimation("animation.mobends.player.sword_stance_sprint");
        Animation stand = load("stand").getAnimation("animation.mobends.player.stand");
        for (float[] look : LOOKS) {
            assertLoop("sword_stance", look, (m, t) -> {
                Animation_Stand.apply(m, t);
                Animation_Attack_Stance.apply(m, false, false);
            }, (m, t) -> {
                loop(m, stand, t);
                pose(m, still);
            });
            assertLoop("sword_stance_sprint", look, (m, t) -> {
                sprinting(m, t);
                Animation_Attack_Stance.apply(m, true, true);
            }, (m, t) -> {
                sprintFile(m, t);
                pose(m, sprint);
            });
        }
    }

    @Test
    void blockingMatches() throws Exception {
        BakedAnimations file = load("block");
        Animation right = file.getAnimation("animation.mobends.player.block_right");
        Animation left = file.getAnimation("animation.mobends.player.block_left");
        for (float[] look : LOOKS) {
            assertLoop("blocking, both hands", look, (m, t) -> {
                sprinting(m, t);
                Animation_Blocking.apply(m, PlayerBlockingCompat.MAIN_HAND | PlayerBlockingCompat.OFF_HAND);
            }, (m, t) -> {
                sprintFile(m, t);
                pose(m, right);
                pose(m, left);
            });
        }
    }

    /** Sprinting, the limbs swinging fully: the Java sprint, and below the ported one, as each plays in game. */
    private static void sprinting(ModelBendsPlayer model, float ticks) {
        model.armSwing = ticks;
        model.armSwingAmount = 1;
        Animation_Sprint.apply(model);
    }

    private static BakedAnimations sprintFile;

    private static void sprintFile(ModelBendsPlayer model, float ticks) {
        model.armSwing = ticks;
        model.armSwingAmount = 1;
        try {
            if (sprintFile == null) sprintFile = load("sprint");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // As BendsAnimator.playStride.
        double stride = ticks * 0.6662 / (2 * Math.PI);
        stride -= Math.floor(stride);
        for (String name : new String[] {"sprint", "sprint_swing"}) {
            Animation animation = sprintFile.getAnimation("animation.mobends.player." + name);
            BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, stride * animation.length(),
                BendsAnimator.context(model, null)), BendsAnimator.FOLLOW_RATE, 1);
        }
    }

    private static void loop(ModelBendsPlayer model, Animation animation, float ticks) {
        BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, (ticks / 20.0) % animation.length(),
            BendsAnimator.context(model, null)), BendsAnimator.FOLLOW_RATE, 1);
    }

    private static void assertLoop(String what, float[] look, BiConsumer<ModelBendsPlayer, Float> java,
        BiConsumer<ModelBendsPlayer, Float> baked) {
        String[] where = {""};
        float worst = AnimationBaking.compare(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
            java, baked, where);
        assertTrue(worst < 1.5f, what + ", look " + look[0] + "/" + look[1] + ": differs by " + worst + " degrees (" + where[0] + ")");
    }

    /** Each side leading in its own way (Java, or the ported animations), as baked. */
    private static void assertOnce(String what, float[] look, BiConsumer<ModelBendsPlayer, Float> javaBefore,
        BiConsumer<ModelBendsPlayer, Float> bakedBefore, BiConsumer<ModelBendsPlayer, Float> java,
        BiConsumer<ModelBendsPlayer, Float> baked) {
        String[] where = {""};
        float worst = AnimationBaking.compareOnce(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
            javaBefore, bakedBefore, java, baked, SWING_TICKS, 0, where);
        assertTrue(worst < 1.5f, what + ", look " + look[0] + "/" + look[1] + ": differs by " + worst + " degrees (" + where[0] + ")");
    }

    /** As Animation_Attack plays a swing: timed from the click, no blend in. */
    private static void once(ModelBendsPlayer model, Animation animation, float ticks, Set<String> without) {
        BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, ticks / 20.0, BendsAnimator.context(model, null)),
            BendsAnimator.FOLLOW_RATE, 1, without);
    }

    private static void pose(ModelBendsPlayer model, Animation animation) {
        BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, 0, BendsAnimator.context(model, null)),
            BendsAnimator.FOLLOW_RATE, 1);
    }

    private static BakedAnimations load(String file) throws IOException {
        return AnimationBaking.load(DIR.resolve(file + ".animation.json"));
    }
}
