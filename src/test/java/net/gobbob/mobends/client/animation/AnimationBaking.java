package net.gobbob.mobends.client.animation;

import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.util.SmoothVector3f;
import org.joml.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

/**
 * Bakes a Java procedural animation into Blockbench keyframes as it looks in game (after Mo' Bends' smoothing), and
 * compares played animations with their Java originals. Shared by the ported animations' tests.
 */
final class AnimationBaking {
    static final float TICKS_PER_FRAME = 20 / 60f;
    private static final float DEG = (float)(Math.PI / 180.0);
    /** Keyframes are kept only where needed to stay this close (degrees) to the recorded motion. */
    private static final float TOLERANCE = 0.5f;

    /** A rig channel ("bone property") read from the Java model's smoothed state. */
    interface Channel {
        float[] read(ModelBendsPlayer model);
    }

    private AnimationBaking() {}

    /** One rendered frame at 60 fps. */
    static void frame(ModelBendsPlayer model, Consumer<ModelBendsPlayer> animate) {
        frame(model, animate, TICKS_PER_FRAME);
    }

    /** One rendered frame of ModelBendsPlayer's animation bookkeeping, as in setRotationAngles. */
    static void frame(ModelBendsPlayer model, Consumer<ModelBendsPlayer> animate, float ticksPerFrame) {
        model.renderOffset.setSmooth(new Vector3f(0, -1, 0), 0.5f);
        model.renderRotation.setSmooth(new Vector3f(0, 0, 0), 0.5f);
        model.renderItemRotation.setSmooth(new Vector3f(0, 0, 0), 0.5f);
        model.rightItem.resetScale();
        animate.accept(model);
        for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
            if (bone.part(model) != null) bone.part(model).update(ticksPerFrame);
        }
        model.renderOffset.update(ticksPerFrame);
        model.renderRotation.update(ticksPerFrame);
        model.renderRotation.wrapTurns(360);
        model.renderItemRotation.update(ticksPerFrame);
        model.rightItem.update(ticksPerFrame);
    }

    static ModelBendsPlayer model(float pitch, float yaw) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.headRotationX = pitch;
        model.headRotationY = yaw;
        return model;
    }

    // --- Channels -------------------------------------------------------------------------------------------------

    static float[] xyz(Vector3f v) {
        return new float[] {v.x, v.y, v.z};
    }

    static ModelRendererBends part(ModelBendsPlayer model, PlayerRig.Bone bone) {
        return bone.part(model);
    }

    /**
     * Arms stroking sideways: pre-rotation Y turns the arm (its aim bone), pre-rotation Z spreads it inside that, so
     * it becomes the arm's own Z. Only exact while the pre-rotation X and the rotation Y stay 0.
     */
    static void putArms(Map<String, Channel> channels) {
        for (PlayerRig.Bone arm : new PlayerRig.Bone[] {PlayerRig.Bone.RIGHT_ARM, PlayerRig.Bone.LEFT_ARM}) {
            channels.put(arm.aim().id + " rotation", m -> new float[] {0, -arm.part(m).pre_rotation.vSmooth.y, 0});
            channels.put(arm.id + " rotation", m -> {
                ModelRendererBends part = arm.part(m);
                return new float[] {part.rotation.vSmooth.x, part.rotation.vSmooth.y, part.pre_rotation.vSmooth.z + part.rotation.vSmooth.z};
            });
        }
        for (PlayerRig.Bone foreArm : new PlayerRig.Bone[] {PlayerRig.Bone.RIGHT_FORE_ARM, PlayerRig.Bone.LEFT_FORE_ARM}) {
            channels.put(foreArm.id + " rotation", m -> xyz(foreArm.part(m).rotation.vSmooth));
        }
    }

    static void putLegs(Map<String, Channel> channels) {
        for (PlayerRig.Bone leg : new PlayerRig.Bone[] {
            PlayerRig.Bone.RIGHT_LEG, PlayerRig.Bone.RIGHT_FORE_LEG, PlayerRig.Bone.LEFT_LEG, PlayerRig.Bone.LEFT_FORE_LEG}) {
            channels.put(leg.id + " rotation", m -> xyz(leg.part(m).rotation.vSmooth));
        }
    }

    // --- Rig channels derived from a Java animation --------------------------------------------------------------

    private static final float SENTINEL = -12345.0f;

    /** A channel read from the Java model, and which of its axes an animation set (on a {@link #sentinelModel}). */
    private interface Source {
        float[] read(ModelBendsPlayer model, boolean target);

        boolean[] set(ModelBendsPlayer model);
    }

    private static Vector3f value(SmoothVector3f vector, boolean target) {
        return target ? vector.vFinal : vector.vSmooth;
    }

    /** One of the Java model's vectors, times {@code signs} per axis. */
    private static Source vector(Function<ModelBendsPlayer, SmoothVector3f> vector, float x, float y, float z) {
        return new Source() {
            public float[] read(ModelBendsPlayer model, boolean target) {
                Vector3f v = value(vector.apply(model), target);
                return new float[] {x * v.x, y * v.y, z * v.z};
            }

            public boolean[] set(ModelBendsPlayer model) {
                Vector3f v = vector.apply(model).vFinal;
                return new boolean[] {v.x != SENTINEL, v.y != SENTINEL, v.z != SENTINEL};
            }
        };
    }

    /**
     * A part's pre-rotation (turning -Y, X, then Z) as the Euler angles of its aim bone, which turns just before it
     * at the same pivot: exact, and leaves the part's own rotation to the animations before. Set whole if at all.
     */
    private static Source aim(PlayerRig.Bone part) {
        return new Source() {
            public float[] read(ModelBendsPlayer model, boolean target) {
                Vector3f pre = value(part.part(model).pre_rotation, target);
                double[] m = EulerZYX.multiply(EulerZYX.multiply(EulerZYX.matrix(0, -pre.y, 0), EulerZYX.matrix(pre.x, 0, 0)),
                    EulerZYX.matrix(0, 0, pre.z));
                return EulerZYX.angles(m, new float[3]);
            }

            public boolean[] set(ModelBendsPlayer model) {
                Vector3f v = part.part(model).pre_rotation.vFinal;
                boolean any = v.x != SENTINEL || v.y != SENTINEL || v.z != SENTINEL;
                return new boolean[] {any, any, any};
            }
        };
    }

    /** Every Blockbench channel ("bone property") the Java model can drive. */
    private static Map<String, Source> rigSources() {
        Map<String, Source> channels = new LinkedHashMap<String, Source>();
        channels.put("root rotation", vector(m -> m.renderRotation, 1, 1, 1));
        // renderOffset is the root position as (-x, y, z).
        channels.put("root position", vector(m -> m.renderOffset, -1, 1, 1));
        for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
            if (bone.part(new ModelBendsPlayer()) == null) continue;
            channels.put(bone.id + " rotation", vector(m -> bone.part(m).rotation, 1, 1, 1));
            channels.put((bone.aim() != null ? bone.aim().id : bone.id + " (unsupported pre-rotation)") + " rotation", aim(bone));
        }
        // The Java turns the item about X, then -Y, then Z; for one axis at a time that is the item bone's (x, -y, z).
        channels.put("rightItem rotation", vector(m -> m.renderItemRotation, 1, -1, 1));
        return channels;
    }

    /** Baking channels and templates derived from a Java animation; see {@link #derive}. */
    static final class Derived {
        final Map<String, Channel> channels = new LinkedHashMap<String, Channel>();
        final Map<String, String[]> templates = new LinkedHashMap<String, String[]>();
    }

    /**
     * The channels a Java animation drives, from what it sets at {@code ticks}: axes it never sets are left to the
     * animations before it ({@link #KEEP}), and axes that follow the look get it as Molang (e.g.
     * {@code "q.mobends_head_yaw + %s"}), so bake with the player looking straight ahead. {@code overrides} replace
     * derived templates per axis (non-null entries), e.g. for an axis following the look nonlinearly.
     */
    static Derived derive(BiConsumer<ModelBendsPlayer, Float> animate, Map<String, String[]> overrides, float... ticks) {
        Derived derived = new Derived();
        for (Map.Entry<String, Source> channel : rigSources().entrySet()) {
            String name = channel.getKey();
            Source source = channel.getValue();
            boolean[] set = new boolean[3];
            float[][] pitch = new float[ticks.length][], yaw = new float[ticks.length][];
            for (int k = 0; k < ticks.length; ++k) {
                ModelBendsPlayer probe = sentinelModel();
                animate.accept(probe, ticks[k]);
                boolean[] setNow = source.set(probe);
                for (int i = 0; i < 3; ++i) set[i] |= setNow[i];
                float[] still = target(animate, source, ticks[k], 0, 0);
                float[] up = target(animate, source, ticks[k], 20, 0), turned = target(animate, source, ticks[k], 0, 20);
                float[] both = target(animate, source, ticks[k], -30, -30);
                pitch[k] = new float[3];
                yaw[k] = new float[3];
                for (int i = 0; i < 3; ++i) {
                    pitch[k][i] = Math.round((up[i] - still[i]) / 20 * 100) / 100f;
                    yaw[k][i] = Math.round((turned[i] - still[i]) / 20 * 100) / 100f;
                    boolean linear = Math.abs(both[i] - (still[i] - 30 * pitch[k][i] - 30 * yaw[k][i])) < 0.01f;
                    boolean steady = pitch[k][i] == pitch[0][i] && yaw[k][i] == yaw[0][i];
                    if (set[i] && (!linear || !steady) && !overridden(overrides, name, i)) {
                        throw new IllegalStateException(name + " axis " + i + " follows the look nonlinearly; give an override");
                    }
                }
                if (name.contains("unsupported") && (still[0] != 0 || still[1] != 0 || still[2] != 0)) {
                    throw new IllegalStateException(name + " at " + ticks[k]);
                }
            }
            // Zeroing an unsupported pre-rotation is fine: parts only pre-rotate in the Java.
            if (name.contains("unsupported") || !(set[0] || set[1] || set[2])) continue;
            String[] template = new String[3];
            for (int i = 0; i < 3; ++i) {
                if (!set[i]) template[i] = KEEP;
                else if (pitch[0][i] != 0 || yaw[0][i] != 0) template[i] = look(pitch[0][i], yaw[0][i]);
                if (overridden(overrides, name, i)) template[i] = overrides.get(name)[i];
            }
            derived.channels.put(name, m -> source.read(m, false));
            derived.templates.put(name, template);
        }
        return derived;
    }

    private static boolean overridden(Map<String, String[]> overrides, String channel, int axis) {
        return overrides != null && overrides.containsKey(channel) && overrides.get(channel)[axis] != null;
    }

    private static float[] target(BiConsumer<ModelBendsPlayer, Float> animate, Source source, float ticks, float pitch, float yaw) {
        ModelBendsPlayer model = model(pitch, yaw);
        animate.accept(model, ticks);
        return source.read(model, true);
    }

    private static String look(float pitch, float yaw) {
        StringBuilder molang = new StringBuilder();
        for (Object[] query : new Object[][] {{"q.mobends_head_pitch", pitch}, {"q.mobends_head_yaw", yaw}}) {
            float k = (Float)query[1];
            if (k == 0) continue;
            molang.append(molang.length() == 0 ? (k < 0 ? "-" : "") : (k < 0 ? " - " : " + ")).append(query[0]);
            if (Math.abs(k) != 1) molang.append(" * ").append(number(Math.abs(k)));
        }
        return molang.append(" + %s").toString();
    }

    /** A fresh model with every target the animations set planted with a sentinel, to see what an animation sets. */
    private static ModelBendsPlayer sentinelModel() {
        ModelBendsPlayer model = model(0, 0);
        List<SmoothVector3f> vectors = new ArrayList<SmoothVector3f>();
        vectors.add(model.renderRotation);
        vectors.add(model.renderOffset);
        vectors.add(model.renderItemRotation);
        for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
            if (bone.part(model) == null) continue;
            vectors.add(bone.part(model).rotation);
            vectors.add(bone.part(model).pre_rotation);
        }
        for (SmoothVector3f vector : vectors) vector.vFinal = new Vector3f(SENTINEL, SENTINEL, SENTINEL);
        return model;
    }

    // --- Baking ---------------------------------------------------------------------------------------------------

    /**
     * Runs {@code animate(model, ticks)} until it settles, records one loop of each channel every eighth of a tick, writes
     * the fewest keyframes that stay within 0.5 degrees of it as a looping animation. It runs at 480 fps, so targets the Java switches on a threshold switch when they
     * should rather than on whichever 60 fps frame crosses it. {@code fixed} tracks ("bone property" to a JSON track) are written as given, e.g.
     * Molang that follows the player's input.
     */
    static String bake(String name, double periodTicks, Map<String, Channel> channels, Map<String, String> fixed,
        BiConsumer<ModelBendsPlayer, Float> animate) {
        return bake(name, periodTicks, channels, new LinkedHashMap<String, String[]>(), fixed, animate);
    }

    /** Leaves an axis to the animations before this one. */
    static final String KEEP = "q.mobends_keep";

    /**
     * As above, with per-axis {@code templates} for some channels: an axis template containing {@code %s} wraps the
     * recorded number in Molang (e.g. {@code "q.mobends_head_pitch + %s"}); one without is written as it is (e.g. a
     * Molang expression, or {@link #KEEP}).
     */
    static String bake(String name, double periodTicks, Map<String, Channel> channels, Map<String, String[]> templates,
        Map<String, String> fixed, BiConsumer<ModelBendsPlayer, Float> animate) {
        // Every eighth of a tick, so the keyframes can land on the corner where a Java target switches.
        int samples = (int)Math.round(periodTicks * 8);
        ModelBendsPlayer model = model(0, 0);
        Map<String, List<float[]>> recorded = new LinkedHashMap<String, List<float[]>>();
        for (String channel : channels.keySet()) recorded.put(channel, new ArrayList<float[]>());
        double start = Math.ceil(600 / periodTicks) * periodTicks; // Settled, and on the loop's phase 0.
        int next = 0;
        float previousTicks = 0;
        Map<String, float[]> previous = new LinkedHashMap<String, float[]>();
        float step = TICKS_PER_FRAME / 8;
        for (float ticks = 0; next <= samples; ticks += step) {
            final float now = ticks;
            frame(model, m -> animate.accept(m, now), step);
            Map<String, float[]> values = new LinkedHashMap<String, float[]>();
            for (Map.Entry<String, Channel> channel : channels.entrySet()) values.put(channel.getKey(), channel.getValue().read(model));
            while (next <= samples && ticks >= start + periodTicks * next / samples) {
                double at = start + periodTicks * next / samples;
                double f = ticks == previousTicks ? 1 : (at - previousTicks) / (ticks - previousTicks);
                for (String channel : channels.keySet()) {
                    float[] a = previous.get(channel), b = values.get(channel);
                    float[] v = new float[3];
                    for (int i = 0; i < 3; ++i) v[i] = a == null ? b[i] : (float)(a[i] + (b[i] - a[i]) * f);
                    recorded.get(channel).add(v);
                }
                ++next;
            }
            previous = values;
            previousTicks = ticks;
        }
        Map<String, String> tracks = new LinkedHashMap<String, String>();
        for (String channel : channels.keySet()) {
            tracks.put(channel, keyframes(unwrap(channel, recorded.get(channel)), periodTicks / 20.0, samples, templates.get(channel)));
        }
        tracks.putAll(fixed);
        return animation(name, periodTicks / 20.0, tracks);
    }

    /**
     * Bakes a pose held at a frozen phase (e.g. a crouch while standing still): at each phase across the loop, the
     * Java animation's targets, which the parts settle on when the phase stops moving. {@code channels} read targets
     * ({@code vFinal}), not the smoothed state.
     */
    static String bakePose(String name, double periodTicks, Map<String, Channel> channels, Map<String, String[]> templates,
        BiConsumer<ModelBendsPlayer, Float> animate) {
        int samples = (int)Math.round(periodTicks * 8);
        Map<String, List<float[]>> recorded = new LinkedHashMap<String, List<float[]>>();
        for (String channel : channels.keySet()) recorded.put(channel, new ArrayList<float[]>());
        for (int k = 0; k <= samples; ++k) {
            ModelBendsPlayer model = model(0, 0);
            animate.accept(model, (float)(periodTicks * k / samples));
            for (Map.Entry<String, Channel> channel : channels.entrySet()) recorded.get(channel.getKey()).add(channel.getValue().read(model));
        }
        Map<String, String> tracks = new LinkedHashMap<String, String>();
        for (String channel : channels.keySet()) {
            tracks.put(channel, keyframes(recorded.get(channel), periodTicks / 20.0, samples, templates.get(channel)));
        }
        return animation(name, periodTicks / 20.0, tracks);
    }

    /**
     * Bakes a loop from what an animation sets at each sample (its targets), on models from {@code factory}: for
     * animations that set their parts outright rather than easing into them (e.g. the spider's), on any rig.
     */
    static <M> String bakeTargets(String name, double periodTicks, Map<String, Function<M, float[]>> channels,
        Map<String, String[]> templates, java.util.function.Supplier<M> factory, BiConsumer<M, Float> animate) {
        int samples = (int)Math.round(periodTicks * 8);
        Map<String, List<float[]>> recorded = new LinkedHashMap<String, List<float[]>>();
        for (String channel : channels.keySet()) recorded.put(channel, new ArrayList<float[]>());
        for (int k = 0; k <= samples; ++k) {
            M model = factory.get();
            animate.accept(model, (float)(periodTicks * k / samples));
            for (Map.Entry<String, Function<M, float[]>> channel : channels.entrySet()) {
                recorded.get(channel.getKey()).add(channel.getValue().apply(model));
            }
        }
        Map<String, String> tracks = new LinkedHashMap<String, String>();
        for (String channel : channels.keySet()) {
            tracks.put(channel, keyframes(unwrap(channel, recorded.get(channel)), periodTicks / 20.0, samples, templates.get(channel)));
        }
        return animation(name, periodTicks / 20.0, tracks);
    }

    /**
     * Bakes something that happens once (e.g. a jump): {@code before} settles for 400 ticks, then {@code animate}
     * runs for {@code durationTicks} (its ticks count from the start) and is recorded every eighth of a tick as an
     * animation that holds its last frame.
     */
    static String bakeOnce(String name, double durationTicks, Map<String, Channel> channels, Map<String, String[]> templates,
        BiConsumer<ModelBendsPlayer, Float> before, BiConsumer<ModelBendsPlayer, Float> animate) {
        int samples = (int)Math.round(durationTicks * 8);
        float step = TICKS_PER_FRAME / 8;
        ModelBendsPlayer model = model(0, 0);
        for (float ticks = 0; ticks < 400; ticks += step) {
            final float now = ticks;
            frame(model, m -> before.accept(m, now), step);
        }
        Map<String, List<float[]>> recorded = new LinkedHashMap<String, List<float[]>>();
        for (String channel : channels.keySet()) recorded.put(channel, new ArrayList<float[]>());
        int next = 0;
        for (float ticks = 0; next <= samples; ticks += step) {
            final float now = ticks;
            frame(model, m -> animate.accept(m, now), step);
            // Frames land on the samples: an eighth of a tick is exactly one step.
            while (next <= samples && ticks + step / 2 >= durationTicks * next / samples) {
                for (Map.Entry<String, Channel> channel : channels.entrySet()) {
                    recorded.get(channel.getKey()).add(channel.getValue().read(model));
                }
                ++next;
            }
        }
        Map<String, String> tracks = new LinkedHashMap<String, String>();
        for (String channel : channels.keySet()) {
            tracks.put(channel, keyframes(unwrap(channel, recorded.get(channel)), durationTicks / 20.0, samples, templates.get(channel)));
        }
        return animation(name, durationTicks / 20.0, tracks, "\"hold_on_last_frame\"");
    }

    /**
     * A rotation recorded as it turns past a half turn (e.g. a spin, which the Java wraps back into one turn)
     * continues in whole turns, so the keyframes turn the same way instead of sweeping back.
     */
    private static List<float[]> unwrap(String channel, List<float[]> values) {
        if (!channel.endsWith(" rotation")) return values;
        for (int k = 1; k < values.size(); ++k) {
            float[] previous = values.get(k - 1), v = values.get(k);
            for (int i = 0; i < 3; ++i) v[i] += 360.0f * Math.round((previous[i] - v[i]) / 360.0f);
        }
        return values;
    }

    /** A looping animation from "bone property" to JSON track. */
    static String animation(String name, double seconds, Map<String, String> tracks) {
        return animation(name, seconds, tracks, "true");
    }

    static String animation(String name, double seconds, Map<String, String> tracks, String loop) {
        Map<String, Map<String, String>> bones = new LinkedHashMap<String, Map<String, String>>();
        for (Map.Entry<String, String> track : tracks.entrySet()) {
            String bone = track.getKey().substring(0, track.getKey().indexOf(' '));
            String property = track.getKey().substring(track.getKey().indexOf(' ') + 1);
            bones.computeIfAbsent(bone, b -> new LinkedHashMap<String, String>()).put(property, track.getValue());
        }
        StringBuilder json = new StringBuilder();
        json.append(String.format(Locale.ROOT, "\t\t\"animation.mobends.player.%s\": {\n\t\t\t\"loop\": %s,\n"
            + "\t\t\t\"animation_length\": %s,\n\t\t\t\"bones\": {\n", name, loop, time(seconds)));
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
     * Seconds to 4 decimals, or 6 where 4 would be off (e.g. a loop of 20 / 1.8 ticks, which would drift out of phase
     * over many loops).
     */
    private static String time(double seconds) {
        String rounded = String.format(Locale.ROOT, "%.4f", seconds);
        return Math.abs(Double.parseDouble(rounded) - seconds) < 1e-7 ? rounded : String.format(Locale.ROOT, "%.6f", seconds);
    }

    static String file(String... animations) {
        return "{\n\t\"format_version\": \"1.8.0\",\n\t\"animations\": {\n" + String.join(",\n", animations) + "\n\t}\n}\n";
    }

    /** A single keyframe at 0 holding {@code x, y, z} (numbers or Molang strings, as JSON). */
    static String hold(String x, String y, String z) {
        return "{\"0.0\": [" + x + ", " + y + ", " + z + "]}";
    }

    /**
     * One keyframe if the channel holds still. Otherwise the fewest samples that keep the linear keyframes within
     * {@link #TOLERANCE} degrees of the recorded motion (Ramer-Douglas-Peucker), always keeping both loop ends.
     */
    private static String keyframes(List<float[]> values, double seconds, int samples, String[] template) {
        // Axes written as a fixed expression don't need keyframes of their own.
        for (float[] v : values) {
            for (int i = 0; i < 3; ++i) if (template != null && template[i] != null && !template[i].contains("%s")) v[i] = 0;
        }
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
            track.append(first ? "" : ", ").append(String.format(Locale.ROOT, "\"%s\": [%s, %s, %s]",
                time(seconds * k / samples), axis(v[0], template, 0), axis(v[1], template, 1), axis(v[2], template, 2)));
            first = false;
        }
        return track.append('}').toString();
    }

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

    private static String axis(float value, String[] template, int axis) {
        if (template == null || template[axis] == null) return number(value);
        String molang = template[axis];
        if (molang.equals("%s")) return number(value);
        if (number(value).equals("0") && molang.endsWith(" + %s")) molang = molang.substring(0, molang.length() - 5);
        return "\"" + molang.replace("%s", number(value)) + "\"";
    }

    static String number(float value) {
        float rounded = Math.round(value * 100) / 100f;
        if (rounded == 0) rounded = 0;
        return rounded == (int)rounded ? Integer.toString((int)rounded) : Float.toString(rounded);
    }

    // --- Comparing ------------------------------------------------------------------------------------------------

    static BakedAnimations load(Path file) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return new GeckoLibGsonLoader().loadAnimations(reader);
        }
    }

    /** As rendered: aim (Z, Y, X), pre-rotation (-Y, X, Z), rotation (Z, Y, X). */
    static Matrix4f orientation(ModelRendererBends part) {
        Vector3f aim = part.aim.vSmooth, pre = part.pre_rotation.vSmooth, rot = part.rotation.vSmooth;
        return new Matrix4f()
            .rotateZ(aim.z * DEG).rotateY(aim.y * DEG).rotateX(aim.x * DEG)
            .rotateY(-pre.y * DEG).rotateX(pre.x * DEG).rotateZ(pre.z * DEG)
            .rotateZ(rot.z * DEG).rotateY(rot.y * DEG).rotateX(rot.x * DEG);
    }

    static float angleBetween(Matrix4f a, Matrix4f b) {
        Matrix4f relative = new Matrix4f(a).transpose().mul(b);
        double cos = (relative.m00() + relative.m11() + relative.m22() - 1) / 2;
        return (float)Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, cos))));
    }

    /**
     * Plays {@code java} and {@code baked} side by side after a settling time and returns the worst difference in
     * degrees of any part, the whole-body rotation and the held item's rotation (and the body offset, in fifths of a
     * pixel). The baked animation plays at 60 fps; the Java one runs at 480 fps, like the bake, so a target it switches
     * on a threshold switches at that moment rather than on whichever 60 fps frame crosses it.
     */
    static float compare(ModelBendsPlayer javaModel, ModelBendsPlayer bakedModel, BiConsumer<ModelBendsPlayer, Float> java,
        BiConsumer<ModelBendsPlayer, Float> baked, String[] where, String... authoredParts) {
        java.util.Set<Integer> excluded = new java.util.HashSet<>();
        int count = new Snapshot(javaModel).differences(new Snapshot(bakedModel)).length;
        for (int i = 0; i < count; ++i) {
            for (String name : authoredParts) if (Snapshot.name(i).equals(name)) excluded.add(i);
        }
        float worst = 0;
        float step = TICKS_PER_FRAME / 8;
        float javaTicks = 0;
        // Settle for 500 ticks: some Java parts ease very slowly (the sneak's elbows at 0.01 per tick).
        for (float ticks = 0; ticks < 1100; ticks += TICKS_PER_FRAME) {
            final float now = ticks;
            while (javaTicks <= ticks + step / 2) {
                final float javaNow = javaTicks;
                frame(javaModel, m -> java.accept(m, javaNow), step);
                javaTicks += step;
            }
            frame(bakedModel, m -> baked.accept(m, now));
            if (ticks < 500) continue;
            float[] differences = new Snapshot(javaModel).differences(new Snapshot(bakedModel));
            for (int i = 0; i < differences.length; ++i) {
                if (!excluded.contains(i) && differences[i] > worst) {
                    worst = differences[i];
                    where[0] = Snapshot.name(i) + " at tick " + ticks;
                }
            }
        }
        return worst;
    }

    /**
     * Compares something that happens once: {@code before} settles both models for 400 ticks, then {@code java} and
     * {@code baked} run for {@code durationTicks} (ticks counting from the start), compared every frame. As in
     * {@link #compare}, the Java side runs at 480 fps.
     */
    static float compareOnce(ModelBendsPlayer javaModel, ModelBendsPlayer bakedModel, BiConsumer<ModelBendsPlayer, Float> before,
        BiConsumer<ModelBendsPlayer, Float> java, BiConsumer<ModelBendsPlayer, Float> baked, double durationTicks, String[] where) {
        return compareOnce(javaModel, bakedModel, before, java, baked, durationTicks, 0, where);
    }

    /** As above, comparing only from {@code fromTicks}. */
    static float compareOnce(ModelBendsPlayer javaModel, ModelBendsPlayer bakedModel, BiConsumer<ModelBendsPlayer, Float> before,
        BiConsumer<ModelBendsPlayer, Float> java, BiConsumer<ModelBendsPlayer, Float> baked, double durationTicks, double fromTicks,
        String[] where) {
        return compareOnce(javaModel, bakedModel, before, before, java, baked, durationTicks, fromTicks, where);
    }

    /** As above, each side with its own lead-up (e.g. each holding its animation's first frame). */
    static float compareOnce(ModelBendsPlayer javaModel, ModelBendsPlayer bakedModel, BiConsumer<ModelBendsPlayer, Float> javaBefore,
        BiConsumer<ModelBendsPlayer, Float> bakedBefore, BiConsumer<ModelBendsPlayer, Float> java, BiConsumer<ModelBendsPlayer, Float> baked,
        double durationTicks, double fromTicks, String[] where) {
        float step = TICKS_PER_FRAME / 8;
        for (float ticks = 0; ticks < 400; ticks += step) {
            final float now = ticks;
            frame(javaModel, m -> javaBefore.accept(m, now), step);
        }
        // Both lead-ups at 480 fps, so the two models start from the same state.
        for (float ticks = 0; ticks < 400; ticks += step) {
            final float now = ticks;
            frame(bakedModel, m -> bakedBefore.accept(m, now), step);
        }
        float worst = 0;
        float javaTicks = 0;
        for (float ticks = 0; ticks < durationTicks; ticks += TICKS_PER_FRAME) {
            final float now = ticks;
            while (javaTicks <= ticks + step / 2) {
                final float javaNow = javaTicks;
                frame(javaModel, m -> java.accept(m, javaNow), step);
                javaTicks += step;
            }
            frame(bakedModel, m -> baked.accept(m, now));
            // The first frame is the switch itself: channels the animation doesn't set start settling there, a frame
            // at 60 fps against an eighth of one on the Java side.
            if (ticks == 0 || ticks < fromTicks) continue;
            float[] differences = new Snapshot(javaModel).differences(new Snapshot(bakedModel));
            for (int i = 0; i < differences.length; ++i) {
                if (differences[i] > worst) {
                    worst = differences[i];
                    where[0] = Snapshot.name(i) + " at tick " + ticks;
                }
            }
        }
        return worst;
    }

    /** The rendered orientation of every part, the whole-body rotation, the held item and the body offset. */
    private static final class Snapshot {
        private final List<Matrix4f> parts = new ArrayList<Matrix4f>();
        private final Vector3f root, item, offset;

        Snapshot(ModelBendsPlayer model) {
            for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
                if (bone.part(model) != null) this.parts.add(orientation(bone.part(model)));
            }
            this.root = new Vector3f(model.renderRotation.vSmooth);
            // The Java turns the item about -Y; the item bone about +Y.
            Vector3f java = model.renderItemRotation.vSmooth;
            this.item = model.rightItem.rotation.vSmooth.lengthSquared() > 0
                ? new Vector3f(model.rightItem.rotation.vSmooth) : new Vector3f(java.x, -java.y, java.z);
            this.offset = new Vector3f(model.renderOffset.vSmooth);
        }

        /** Degrees per part, then root, item, and the offset in fifths of a pixel. */
        float[] differences(Snapshot other) {
            float[] result = new float[this.parts.size() + 3];
            for (int i = 0; i < this.parts.size(); ++i) result[i] = angleBetween(this.parts.get(i), other.parts.get(i));
            result[this.parts.size()] = maxAngleDifference(this.root, other.root);
            result[this.parts.size() + 1] = maxDifference(this.item, other.item);
            result[this.parts.size() + 2] = maxDifference(this.offset, other.offset) * 5;
            return result;
        }

        static String name(int index) {
            List<String> names = new ArrayList<String>();
            for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
                if (bone.part(new ModelBendsPlayer()) != null) names.add(bone.id);
            }
            names.add("root rotation");
            names.add("held item");
            names.add("root offset (fifths of a pixel)");
            return names.get(index);
        }
    }

    /** Per axis, give or take whole turns (the Java wraps a spin back into one turn where a file keeps turning). */
    private static float maxAngleDifference(Vector3f a, Vector3f b) {
        float worst = 0;
        for (float d : new float[] {a.x - b.x, a.y - b.y, a.z - b.z}) {
            worst = Math.max(worst, Math.abs(d - 360.0f * Math.round(d / 360.0f)));
        }
        return worst;
    }

    private static float maxDifference(Vector3f a, Vector3f b) {
        return Math.max(Math.abs(a.x - b.x), Math.max(Math.abs(a.y - b.y), Math.abs(a.z - b.z)));
    }
}
