package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import net.gobbob.mobends.animation.player.Animation_Swimming;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector3f;

/**
 * The Blockbench swim ({@code player/swim.animation.json}) was baked from the Java swim as it looks in game (after
 * Mo' Bends' smoothing), and must keep matching it. Run with {@code MOBENDS_BAKE_SWIM=1} to bake it again.
 */
class SwimAnimationTest {
    static final Path FILE = Paths.get("src/main/resources/assets/mobends/geckolib/animations/player/swim.animation.json");
    private static final float TICKS_PER_FRAME = 20 / 60f;
    private static final float DEG = (float)(Math.PI / 180.0);
    /** Upper body and legs loop at unrelated speeds (ticks * speed in radians), so each is its own animation. */
    private static final float MOVING_ARMS = 0.1625f, MOVING_LEGS = 0.4625f, IDLE_ARMS = 0.0825f, IDLE_LEGS = 0.2625f;

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_SWIM"))) return;
        StringBuilder json = new StringBuilder("{\n\t\"format_version\": \"1.8.0\",\n\t\"animations\": {\n");
        json.append(bake("swim", true, MOVING_ARMS, upper(true))).append(",\n");
        json.append(bake("swim_legs", true, MOVING_LEGS, legs())).append(",\n");
        json.append(bake("swim_idle", false, IDLE_ARMS, upper(false))).append(",\n");
        json.append(bake("swim_idle_legs", false, IDLE_LEGS, legs())).append("\n\t}\n}\n");
        Files.write(FILE, json.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void swimMatchesTheJavaSwim() throws Exception {
        for (boolean moving : new boolean[] {true, false}) {
            for (float[] look : new float[][] {{0, 0}, {35, -20}, {-50, 40}}) {
                assertSameMotion(moving, look[0], look[1]);
            }
        }
    }

    /** Plays both swims side by side for several seconds and compares every part, frame by frame. */
    private static void assertSameMotion(boolean moving, float pitch, float yaw) throws Exception {
        BakedAnimations file = load();
        String upper = moving ? "swim" : "swim_idle";
        Animation upperAnimation = file.getAnimation("animation.mobends.player." + upper);
        Animation legsAnimation = file.getAnimation("animation.mobends.player." + upper + "_legs");
        ModelBendsPlayer java = model(pitch, yaw);
        ModelBendsPlayer baked = model(pitch, yaw);
        float worst = 0;
        String where = "";
        for (float ticks = 0; ticks < 600; ticks += TICKS_PER_FRAME) {
            final float now = ticks;
            frame(java, model -> Animation_Swimming.apply(model, now, moving));
            frame(baked, model -> {
                for (Animation animation : new Animation[] {upperAnimation, legsAnimation}) {
                    double seconds = (now / 20.0) % animation.length();
                    PlayerAnimator.apply(model, animation,
                        AnimationProcessor.evaluate(animation, seconds, PlayerAnimator.context(model, null)));
                }
            });
            if (ticks < 200) continue; // Let both settle into the stroke.
            for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
                ModelRendererBends a = bone.part(java);
                if (a == null) continue;
                float angle = angleBetween(orientation(a), orientation(bone.part(baked)));
                if (angle > worst) {
                    worst = angle;
                    where = bone.id + " at tick " + ticks;
                }
            }
            worst = Math.max(worst, check(java.renderRotation.vSmooth, baked.renderRotation.vSmooth, "root rotation", ticks));
            worst = Math.max(worst, check(java.renderItemRotation.vSmooth, baked.rightItem.rotation.vSmooth, "item", ticks));
            float offset = Math.abs(java.renderOffset.vSmooth.z - baked.renderOffset.vSmooth.z);
            assertTrue(offset < 0.2f, "root offset differs by " + offset + " px at tick " + ticks);
        }
        assertTrue(worst < 1.5f, (moving ? "moving" : "idle") + " swim, look " + pitch + "/" + yaw
            + ": differs by " + worst + " degrees (" + where + ")");
    }

    private static float check(Vector3f expected, Vector3f actual, String what, float ticks) {
        float worst = Math.max(Math.abs(expected.x - actual.x), Math.max(Math.abs(expected.y - actual.y), Math.abs(expected.z - actual.z)));
        assertTrue(worst < 1.5f, what + " differs by " + worst + " degrees at tick " + ticks);
        return 0;
    }

    /** One rendered frame of ModelBendsPlayer's animation bookkeeping, as in setRotationAngles. */
    private static void frame(ModelBendsPlayer model, Consumer<ModelBendsPlayer> animate) {
        model.renderOffset.setSmooth(new org.lwjgl.util.vector.Vector3f(0, -1, 0), 0.5f);
        model.renderRotation.setSmooth(new org.lwjgl.util.vector.Vector3f(0, 0, 0), 0.5f);
        model.renderItemRotation.setSmooth(new org.lwjgl.util.vector.Vector3f(0, 0, 0), 0.5f);
        model.rightItem.resetScale();
        animate.accept(model);
        for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
            if (bone.part(model) != null) bone.part(model).update(TICKS_PER_FRAME);
        }
        model.renderOffset.update(TICKS_PER_FRAME);
        model.renderRotation.update(TICKS_PER_FRAME);
        model.renderRotation.wrapTurns(360);
        model.renderItemRotation.update(TICKS_PER_FRAME);
        model.rightItem.update(TICKS_PER_FRAME);
    }

    private static ModelBendsPlayer model(float pitch, float yaw) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.headRotationX = pitch;
        model.headRotationY = yaw;
        return model;
    }

    /** As rendered: aim (Z, Y, X), pre-rotation (-Y, X, Z), rotation (Z, Y, X). */
    private static Matrix4f orientation(ModelRendererBends part) {
        Vector3f aim = part.aim.vSmooth, pre = part.pre_rotation.vSmooth, rot = part.rotation.vSmooth;
        return new Matrix4f()
            .rotateZ(aim.z * DEG).rotateY(aim.y * DEG).rotateX(aim.x * DEG)
            .rotateY(-pre.y * DEG).rotateX(pre.x * DEG).rotateZ(pre.z * DEG)
            .rotateZ(rot.z * DEG).rotateY(rot.y * DEG).rotateX(rot.x * DEG);
    }

    private static float angleBetween(Matrix4f a, Matrix4f b) {
        Matrix4f relative = new Matrix4f(a).transpose().mul(b);
        double cos = (relative.m00() + relative.m11() + relative.m22() - 1) / 2;
        return (float)Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }

    private static BakedAnimations load() throws IOException {
        try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
            return new GeckoLibGsonLoader().loadAnimations(reader);
        }
    }

    // --- Baking ---------------------------------------------------------------------------------------------

    /** A rig channel read from the Java model: bone, "rotation"/"position", and its three axes. */
    private interface Channel {
        float[] read(ModelBendsPlayer model);
    }

    private static Map<String, Channel> upper(boolean moving) {
        Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        channels.put("root rotation", m -> new float[] {m.renderRotation.vSmooth.x, 0, 0});
        // renderOffset is the root position as (-x, y, z); its resting y is -1.
        channels.put("root position", m -> new float[] {-m.renderOffset.vSmooth.x, m.renderOffset.vSmooth.y, m.renderOffset.vSmooth.z});
        channels.put("body rotation", m -> xyz(((ModelRendererBends)m.bipedBody).rotation.vSmooth));
        // The head tilts back with the neck (its pre-rotation) and follows the look with its own rotation.
        channels.put("neck rotation", m -> new float[] {((ModelRendererBends)m.bipedHead).pre_rotation.vSmooth.x, 0, 0});
        channels.put("head rotation", m -> null);
        for (String side : new String[] {"right", "left"}) {
            // Arms: pre-rotation Y turns the stroke sideways (aim); pre Z spreads it, which sits inside as the arm's Z.
            channels.put(side + "ArmAim rotation", m -> new float[] {0, -arm(m, side).pre_rotation.vSmooth.y, 0});
            channels.put(side + "Arm rotation", m -> {
                ModelRendererBends arm = arm(m, side);
                return new float[] {arm.rotation.vSmooth.x, arm.rotation.vSmooth.y, arm.pre_rotation.vSmooth.z + arm.rotation.vSmooth.z};
            });
            channels.put(side + "ForeArm rotation", m -> xyz(((ModelRendererBends)(side.equals("right") ? m.bipedRightForeArm : m.bipedLeftForeArm)).rotation.vSmooth));
        }
        if (moving) channels.put("rightItem rotation", m -> xyz(m.renderItemRotation.vSmooth));
        return channels;
    }

    private static Map<String, Channel> legs() {
        Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        channels.put("rightLeg rotation", m -> xyz(((ModelRendererBends)m.bipedRightLeg).rotation.vSmooth));
        channels.put("rightForeLeg rotation", m -> xyz(((ModelRendererBends)m.bipedRightForeLeg).rotation.vSmooth));
        channels.put("leftLeg rotation", m -> xyz(((ModelRendererBends)m.bipedLeftLeg).rotation.vSmooth));
        channels.put("leftForeLeg rotation", m -> xyz(((ModelRendererBends)m.bipedLeftForeLeg).rotation.vSmooth));
        return channels;
    }

    private static ModelRendererBends arm(ModelBendsPlayer model, String side) {
        return (ModelRendererBends)(side.equals("right") ? model.bipedRightArm : model.bipedLeftArm);
    }

    private static float[] xyz(Vector3f v) {
        return new float[] {v.x, v.y, v.z};
    }

    /** Runs the Java swim until it settles, then samples one loop of each channel about every 0.1 s. */
    private static String bake(String name, boolean moving, float speed, Map<String, Channel> channels) {
        double period = 2 * Math.PI / speed; // ticks
        int samples = (int)Math.round(period * 2); // every half tick, simplified to keyframes below
        ModelBendsPlayer model = model(0, 0);
        Map<String, List<float[]>> recorded = new LinkedHashMap<String, List<float[]>>();
        for (String channel : channels.keySet()) recorded.put(channel, new ArrayList<float[]>());
        double start = Math.ceil(600 / period) * period; // Settled, and on the loop's phase 0.
        int next = 0;
        float previousTicks = 0;
        Map<String, float[]> previous = new LinkedHashMap<String, float[]>();
        for (float ticks = 0; next <= samples; ticks += TICKS_PER_FRAME) {
            final float now = ticks;
            frame(model, m -> Animation_Swimming.apply(m, now, moving));
            Map<String, float[]> values = new LinkedHashMap<String, float[]>();
            for (Map.Entry<String, Channel> channel : channels.entrySet()) values.put(channel.getKey(), channel.getValue().read(model));
            while (next <= samples && ticks >= start + period * next / samples) {
                double at = start + period * next / samples;
                double f = ticks == previousTicks ? 1 : (at - previousTicks) / (ticks - previousTicks);
                for (String channel : channels.keySet()) {
                    float[] a = previous.get(channel), b = values.get(channel);
                    if (b == null) continue;
                    float[] v = new float[3];
                    for (int i = 0; i < 3; ++i) v[i] = a == null ? b[i] : (float)(a[i] + (b[i] - a[i]) * f);
                    recorded.get(channel).add(v);
                }
                ++next;
            }
            previous = values;
            previousTicks = ticks;
        }

        Map<String, Map<String, String>> bones = new LinkedHashMap<String, Map<String, String>>();
        for (String channel : channels.keySet()) {
            String bone = channel.substring(0, channel.indexOf(' '));
            String property = channel.substring(channel.indexOf(' ') + 1);
            String track = bone.equals("head")
                ? "{\"0.0\": [\"q.mobends_head_pitch\", \"q.mobends_head_yaw\", 0]}"
                : keyframes(recorded.get(channel), period / 20.0, samples);
            bones.computeIfAbsent(bone, b -> new LinkedHashMap<String, String>()).put(property, track);
        }
        StringBuilder json = new StringBuilder();
        json.append(String.format(Locale.ROOT, "\t\t\"animation.mobends.player.%s\": {\n\t\t\t\"loop\": true,\n"
            + "\t\t\t\"animation_length\": %.4f,\n\t\t\t\"bones\": {\n", name, period / 20.0));
        int b = 0;
        for (Map.Entry<String, Map<String, String>> bone : bones.entrySet()) {
            json.append("\t\t\t\t\"").append(bone.getKey()).append("\": {");
            int p = 0;
            for (Map.Entry<String, String> property : bone.getValue().entrySet()) {
                json.append(p++ > 0 ? ", " : "").append('"').append(property.getKey()).append("\": ").append(property.getValue());
            }
            json.append(++b < bones.size() ? "},\n" : "}\n");
        }
        return json.append("\t\t\t}\n\t\t}").toString();
    }

    /**
     * One keyframe if the channel holds still. Otherwise the fewest samples that keep the linear keyframes within
     * {@link #TOLERANCE} degrees of the recorded motion (Ramer-Douglas-Peucker), always keeping both loop ends.
     */
    private static String keyframes(List<float[]> values, double seconds, int samples) {
        boolean still = true;
        for (float[] v : values) {
            for (int i = 0; i < 3; ++i) still &= Math.abs(v[i] - values.get(0)[i]) < 0.01f;
        }
        boolean[] keep = new boolean[values.size()];
        keep[0] = true;
        if (!still) {
            keep[values.size() - 1] = true;
            simplify(values, 0, values.size() - 1, keep);
        }
        StringBuilder track = new StringBuilder("{");
        boolean first = true;
        for (int k = 0; k < values.size(); ++k) {
            if (!keep[k]) continue;
            float[] v = values.get(k);
            track.append(first ? "" : ", ").append(String.format(Locale.ROOT, "\"%.4f\": [%s, %s, %s]",
                seconds * k / samples, number(v[0]), number(v[1]), number(v[2])));
            first = false;
        }
        return track.append('}').toString();
    }

    private static final float TOLERANCE = 0.5f;

    private static void simplify(List<float[]> values, int from, int to, boolean[] keep) {
        int worst = -1;
        float worstError = TOLERANCE;
        for (int k = from + 1; k < to; ++k) {
            float f = (k - from) / (float)(to - from);
            for (int i = 0; i < 3; ++i) {
                float line = values.get(from)[i] + (values.get(to)[i] - values.get(from)[i]) * f;
                float error = Math.abs(values.get(k)[i] - line);
                if (error > worstError) {
                    worstError = error;
                    worst = k;
                }
            }
        }
        if (worst < 0) return;
        keep[worst] = true;
        simplify(values, from, worst, keep);
        simplify(values, worst, to, keep);
    }

    private static String number(float value) {
        float rounded = Math.round(value * 100) / 100f;
        return rounded == (int)rounded ? Integer.toString((int)rounded) : Float.toString(rounded);
    }
}
