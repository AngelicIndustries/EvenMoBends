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
import net.gobbob.mobends.animation.player.Animation_Climbing;
import net.gobbob.mobends.animation.player.Animation_Falling;
import net.gobbob.mobends.animation.player.Animation_Flying;
import net.gobbob.mobends.animation.player.Animation_Jump;
import net.gobbob.mobends.animation.player.Animation_Riding;
import net.gobbob.mobends.animation.player.Animation_Sprint;
import net.gobbob.mobends.animation.player.Animation_Stand;
import net.gobbob.mobends.animation.player.Animation_Walk;
import net.gobbob.mobends.client.animation.AnimationBaking.Channel;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.junit.jupiter.api.Test;

/**
 * Fall, climb, fly, ride and jump were baked from their Java animations as they look in game, and must keep matching
 * them. Run with {@code MOBENDS_BAKE_AIR=1} to bake them again.
 */
class AirAnimationTest {
    private static final Path DIR = Paths.get("src/main/resources/assets/mobends/geckolib/animations/player");
    private static final Bone[] PARTS = {Bone.BODY, Bone.RIGHT_ARM, Bone.LEFT_ARM, Bone.RIGHT_FORE_ARM, Bone.LEFT_FORE_ARM,
        Bone.RIGHT_LEG, Bone.LEFT_LEG, Bone.RIGHT_FORE_LEG, Bone.LEFT_FORE_LEG};
    private static final Bone[] LIMBS = {Bone.RIGHT_ARM, Bone.LEFT_ARM, Bone.RIGHT_FORE_ARM, Bone.LEFT_FORE_ARM,
        Bone.RIGHT_LEG, Bone.LEFT_LEG, Bone.RIGHT_FORE_LEG, Bone.LEFT_FORE_LEG};
    private static final float WALK = 0.86f, JUMP_TICKS = 40;

    // --- Baking ---------------------------------------------------------------------------------------------------

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_AIR"))) return;
        write("fall", AnimationBaking.bake("fall", 2 * Math.PI / 0.1, parts(PARTS), look("q.mobends_head_pitch"),
            (m, t) -> Animation_Falling.apply(m, t)));

        write("climb",
            AnimationBaking.bake("climb", 2 * Math.PI / 0.32, parts(PARTS), look("q.mobends_head_pitch - 5"),
                (m, t) -> Animation_Climbing.apply(m, t, 0)),
            AnimationBaking.bake("climb_move", 2 * Math.PI / 0.32, parts(LIMBS), new LinkedHashMap<String, String>(),
                (m, t) -> Animation_Climbing.apply(m, t, 0.12f)));

        write("fly",
            flight("fly", false, 0), flight("fly_forward", false, 0.4f),
            flight("fly_elytra", true, 0), flight("fly_elytra_forward", true, 0.4f));

        write("ride",
            ride("ride", false, 0, null), ride("ride_move", true, 0, null),
            ride("ride_move_jiggle", true, 1, new Bone[] {Bone.BODY}));

        Map<String, String[]> jumpTemplates = headTemplates("q.mobends_head_yaw");
        Map<String, Channel> jump = withHead(parts(PARTS));
        Map<String, String[]> sprintTemplates = headTemplates("q.mobends_head_yaw + %s");
        Map<String, Channel> legLayer = new LinkedHashMap<String, Channel>();
        Map<String, String[]> legTemplates = new LinkedHashMap<String, String[]>();
        for (Bone bone : new Bone[] {Bone.RIGHT_LEG, Bone.LEFT_LEG, Bone.RIGHT_FORE_LEG, Bone.LEFT_FORE_LEG,
            Bone.RIGHT_FORE_ARM, Bone.LEFT_FORE_ARM}) {
            legLayer.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vSmooth));
            legTemplates.put(bone.id + " rotation", new String[] {null, KEEP, KEEP});
        }
        Map<String, Channel> legSwing = new LinkedHashMap<String, Channel>();
        for (Bone bone : new Bone[] {Bone.RIGHT_LEG, Bone.LEFT_LEG}) legSwing.put(bone.id + " rotation", legLayer.get(bone.id + " rotation"));
        double stride = 2 * Math.PI / (0.6662 * WALK);
        write("jump",
            AnimationBaking.bakeOnce("jump", JUMP_TICKS, jump, jumpTemplates, AirAnimationTest::standing, AirAnimationTest::javaJump),
            AnimationBaking.bake("jump_walk", stride, legLayer, legTemplates, new LinkedHashMap<String, String>(), airborneStride(0)),
            AnimationBaking.bake("jump_walk_swing", stride, legSwing, legTemplates, new LinkedHashMap<String, String>(), airborneStride(1)),
            AnimationBaking.bakeOnce("jump_sprint_right", JUMP_TICKS, jump, sprintTemplates, AirAnimationTest::sprinting, sprintJump(true)),
            AnimationBaking.bakeOnce("jump_sprint_left", JUMP_TICKS, jump, sprintTemplates, AirAnimationTest::sprinting, sprintJump(false)));
    }

    /** Hovering ({@code speed} 0) or flying forward: the root's tilt and offset, every part, and the head. */
    private static String flight(String name, boolean elytra, float speed) {
        Map<String, Channel> channels = rootAndParts(PARTS);
        String head = elytra ? "-45" : speed > 0 ? "q.mobends_head_pitch - 35" : "q.mobends_head_pitch";
        return AnimationBaking.bake(name, 2 * Math.PI / 0.15, channels, look(head),
            (m, t) -> Animation_Flying.apply(m, t, speed, 0, 0, elytra));
    }

    /** Riding: the offset, every part (or {@code only} these), and the head countering the body's lean. */
    private static String ride(String name, boolean moving, float swing, Bone[] only) {
        Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        channels.put("root position", m -> new float[] {-m.renderOffset.vSmooth.x, m.renderOffset.vSmooth.y, m.renderOffset.vSmooth.z});
        channels.putAll(parts(only != null ? only : PARTS));
        channels.putAll(withHead(new LinkedHashMap<String, Channel>()));
        return AnimationBaking.bake(name, 2 * Math.PI / 0.6, channels, headTemplates("q.mobends_head_yaw"),
            new LinkedHashMap<String, String>(), (m, t) -> {
                m.armSwingAmount = swing;
                Animation_Riding.apply(m, t, moving);
            });
    }

    private static Map<String, Channel> parts(Bone[] bones) {
        Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        for (Bone bone : bones) channels.put(bone.id + " rotation", m -> xyz(bone.part(m).rotation.vSmooth));
        return channels;
    }

    private static Map<String, Channel> rootAndParts(Bone[] bones) {
        Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        channels.put("root rotation", m -> xyz(m.renderRotation.vSmooth));
        channels.put("root position", m -> new float[] {-m.renderOffset.vSmooth.x, m.renderOffset.vSmooth.y, m.renderOffset.vSmooth.z});
        channels.putAll(parts(bones));
        return channels;
    }

    /** The head's own rotation, recorded with the player looking straight ahead and written around the look. */
    private static Map<String, Channel> withHead(Map<String, Channel> channels) {
        channels.put("head rotation", m -> new float[] {Bone.HEAD.part(m).rotation.vSmooth.x, Bone.HEAD.part(m).rotation.vSmooth.y, 0});
        return channels;
    }

    private static Map<String, String[]> headTemplates(String yaw) {
        Map<String, String[]> templates = new LinkedHashMap<String, String[]>();
        templates.put("head rotation", new String[] {"q.mobends_head_pitch + %s", yaw, "0"});
        return templates;
    }

    private static Map<String, String> look(String pitch) {
        Map<String, String> fixed = new LinkedHashMap<String, String>();
        fixed.put("head rotation", AnimationBaking.hold("\"" + pitch + "\"", "\"q.mobends_head_yaw\"", "0"));
        return fixed;
    }

    private static void write(String file, String... animations) throws IOException {
        Files.write(DIR.resolve(file + ".animation.json"), AnimationBaking.file(animations).getBytes(StandardCharsets.UTF_8));
    }

    // --- The Java jumps, from liftoff -----------------------------------------------------------------------------

    /** Vanilla's vertical speed during a jump, per tick: 0.42, then (v - 0.08) * 0.98. */
    static float jumpSpeed(float ticks) {
        float speed = 0.42f;
        for (int tick = 0; tick < (int)ticks; ++tick) speed = (speed - 0.08f) * 0.98f;
        return speed;
    }

    private static void standing(ModelBendsPlayer model, Float ticks) {
        Animation_Stand.apply(model, ticks);
    }

    private static void javaJump(ModelBendsPlayer model, Float ticks) {
        Animation_Jump.apply(model, ticks, false, false, false, jumpSpeed(ticks));
    }

    /** Sprinting up to the jump: the limbs swing fully, a stride per 2 pi / 0.6662 ticks. */
    private static void sprinting(ModelBendsPlayer model, Float ticks) {
        model.armSwing = ticks;
        model.armSwingAmount = 1;
        Animation_Sprint.apply(model);
    }

    private static BiConsumer<ModelBendsPlayer, Float> sprintJump(boolean rightLeg) {
        return (model, ticks) -> {
            model.armSwing = 400 + ticks;
            model.armSwingAmount = 1;
            Animation_Jump.apply(model, ticks, true, true, rightLeg, jumpSpeed(ticks));
        };
    }

    /** Walking through the air long after liftoff: the legs keep striding at the walking cadence. */
    private static BiConsumer<ModelBendsPlayer, Float> airborneStride(float amount) {
        return (model, ticks) -> {
            model.armSwing = ticks * WALK;
            model.armSwingAmount = amount;
            Animation_Jump.apply(model, 1000, true, false, false, 0);
        };
    }

    // --- Parity ---------------------------------------------------------------------------------------------------

    @Test
    void fallMatches() throws Exception {
        Animation fall = load("fall").getAnimation("animation.mobends.player.fall");
        for (float[] look : new float[][] {{0, 0}, {30, -40}}) {
            assertLoop("fall", look, (m, t) -> Animation_Falling.apply(m, t), (m, t) -> loop(m, fall, t, 1));
        }
    }

    @Test
    void climbMatches() throws Exception {
        BakedAnimations file = load("climb");
        for (float speed : new float[] {0, 0.06f, 0.12f, -0.2f}) {
            float stride = Animation_Climbing.stride(speed);
            assertLoop("climb at " + speed, new float[] {20, 15}, (m, t) -> Animation_Climbing.apply(m, t, speed), (m, t) -> {
                pose(m, file.getAnimation("animation.mobends.player.climb"), 1);
                loop(m, file.getAnimation("animation.mobends.player.climb_move"), t, stride);
            });
        }
    }

    @Test
    void flightMatches() throws Exception {
        BakedAnimations file = load("fly");
        for (boolean elytra : new boolean[] {false, true}) {
            String hover = elytra ? "fly_elytra" : "fly";
            for (float speed : new float[] {0, 0.175f, 0.5f}) {
                float moving = Animation_Flying.moving(speed, 0, 0);
                assertLoop(hover + " at " + speed, new float[] {-20, 30}, (m, t) -> Animation_Flying.apply(m, t, speed, 0, 0, elytra),
                    (m, t) -> {
                        loop(m, file.getAnimation("animation.mobends.player." + hover), t, 1);
                        pose(m, file.getAnimation("animation.mobends.player." + hover + "_forward"), moving);
                    });
            }
        }
    }

    @Test
    void rideMatches() throws Exception {
        BakedAnimations file = load("ride");
        assertLoop("ride", new float[] {10, 20}, (m, t) -> Animation_Riding.apply(m, t, false),
            (m, t) -> pose(m, file.getAnimation("animation.mobends.player.ride"), 1));
        for (float swing : new float[] {0, 0.05f, 0.3f}) {
            assertLoop("ride moving, swing " + swing, new float[] {10, 20}, (m, t) -> {
                m.armSwingAmount = swing;
                Animation_Riding.apply(m, t, true);
            }, (m, t) -> {
                m.armSwingAmount = swing;
                pose(m, file.getAnimation("animation.mobends.player.ride_move"), 1);
                loop(m, file.getAnimation("animation.mobends.player.ride_move_jiggle"), t, swing);
            });
        }
    }

    @Test
    void jumpsMatch() throws Exception {
        BakedAnimations file = load("jump");
        Animation jump = file.getAnimation("animation.mobends.player.jump");
        for (float[] look : new float[][] {{0, 0}, {20, 30}}) {
            assertOnce("standing jump", look, AirAnimationTest::standing, AirAnimationTest::javaJump,
                (m, t) -> once(m, jump, t));
            for (boolean right : new boolean[] {true, false}) {
                Animation sprint = file.getAnimation("animation.mobends.player.jump_sprint_" + (right ? "right" : "left"));
                assertOnce("sprint jump, " + (right ? "right" : "left") + " leg", look, AirAnimationTest::sprinting,
                    sprintJump(right), (m, t) -> {
                        m.armSwing = 400 + t;
                        once(m, sprint, t);
                    });
            }
            // Walking into a jump: the legs keep the stride. What the walk left behind (the arms' swing, the body's
            // sway) eases out at the usual rate rather than the Java jump's own (within 3 degrees); compare once it
            // has settled, 0.75 s in.
            BiConsumer<ModelBendsPlayer, Float> walking = (m, t) -> {
                m.armSwing = t * WALK;
                m.armSwingAmount = WALK;
                Animation_Walk.apply(m);
            };
            assertOnce("walking jump", look, 15, walking, (m, t) -> {
                m.armSwing = (400 + t) * WALK;
                m.armSwingAmount = WALK;
                Animation_Jump.apply(m, t, true, false, false, jumpSpeed(t));
            }, (m, t) -> {
                m.armSwing = (400 + t) * WALK;
                m.armSwingAmount = WALK;
                once(m, jump, t);
                stride(m, file, "jump_walk");
            });
        }
    }

    private static void assertLoop(String what, float[] look, BiConsumer<ModelBendsPlayer, Float> java,
        BiConsumer<ModelBendsPlayer, Float> baked) {
        String[] where = {""};
        float worst = AnimationBaking.compare(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
            java, baked, where);
        assertTrue(worst < 1.5f, what + ", look " + look[0] + "/" + look[1] + ": differs by " + worst + " degrees (" + where[0] + ")");
    }

    private static void assertOnce(String what, float[] look, BiConsumer<ModelBendsPlayer, Float> before,
        BiConsumer<ModelBendsPlayer, Float> java, BiConsumer<ModelBendsPlayer, Float> baked) {
        assertOnce(what, look, 0, before, java, baked);
    }

    private static void assertOnce(String what, float[] look, float fromTicks, BiConsumer<ModelBendsPlayer, Float> before,
        BiConsumer<ModelBendsPlayer, Float> java, BiConsumer<ModelBendsPlayer, Float> baked) {
        String[] where = {""};
        float worst = AnimationBaking.compareOnce(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
            before, java, baked, JUMP_TICKS, fromTicks, where);
        assertTrue(worst < 1.5f, what + ", look " + look[0] + "/" + look[1] + ": differs by " + worst + " degrees (" + where[0] + ")");
    }

    private static void loop(ModelBendsPlayer model, Animation animation, float ticks, float weight) {
        apply(model, animation, (ticks / 20.0) % animation.length(), weight);
    }

    private static void pose(ModelBendsPlayer model, Animation animation, float weight) {
        apply(model, animation, 0, weight);
    }

    /** As Animation_Jump plays it: blending in over its first {@link Animation_Jump#BLEND} seconds. */
    private static void once(ModelBendsPlayer model, Animation animation, float ticks) {
        float progress = (float)(ticks / (Animation_Jump.BLEND * 20));
        PlayerAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, ticks / 20.0, PlayerAnimator.context(model, null)),
            progress < 1 ? PlayerAnimator.blendRate(progress) : PlayerAnimator.FOLLOW_RATE, 1);
    }

    /** As PlayerAnimator.playStride. */
    private static void stride(ModelBendsPlayer model, BakedAnimations file, String name) {
        Animation main = file.getAnimation("animation.mobends.player." + name);
        Animation swing = file.getAnimation("animation.mobends.player." + name + "_swing");
        double stride = model.armSwing * 0.6662 / (2 * Math.PI);
        stride -= Math.floor(stride);
        apply(model, main, stride * main.length(), 1);
        apply(model, swing, stride * swing.length(), model.armSwingAmount);
    }

    private static void apply(ModelBendsPlayer model, Animation animation, double seconds, float weight) {
        PlayerAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, seconds, PlayerAnimator.context(model, null)),
            PlayerAnimator.FOLLOW_RATE, weight);
    }

    private static BakedAnimations load(String file) throws IOException {
        return AnimationBaking.load(DIR.resolve(file + ".animation.json"));
    }
}
