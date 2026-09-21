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
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
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
            tracks.put(channel, keyframes(recorded.get(channel), periodTicks / 20.0, samples, templates.get(channel)));
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

    /** A looping animation from "bone property" to JSON track. */
    static String animation(String name, double seconds, Map<String, String> tracks) {
        Map<String, Map<String, String>> bones = new LinkedHashMap<String, Map<String, String>>();
        for (Map.Entry<String, String> track : tracks.entrySet()) {
            String bone = track.getKey().substring(0, track.getKey().indexOf(' '));
            String property = track.getKey().substring(track.getKey().indexOf(' ') + 1);
            bones.computeIfAbsent(bone, b -> new LinkedHashMap<String, String>()).put(property, track.getValue());
        }
        StringBuilder json = new StringBuilder();
        json.append(String.format(Locale.ROOT, "\t\t\"animation.mobends.player.%s\": {\n\t\t\t\"loop\": true,\n"
            + "\t\t\t\"animation_length\": %.4f,\n\t\t\t\"bones\": {\n", name, seconds));
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
            track.append(first ? "" : ", ").append(String.format(Locale.ROOT, "\"%.4f\": [%s, %s, %s]",
                seconds * k / samples, axis(v[0], template, 0), axis(v[1], template, 1), axis(v[2], template, 2)));
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
        return "\"" + template[axis].replace("%s", number(value)) + "\"";
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

    /** The rendered orientation of every part, the whole-body rotation, the held item and the body offset. */
    private static final class Snapshot {
        private final List<Matrix4f> parts = new ArrayList<Matrix4f>();
        private final Vector3f root, item, offset;

        Snapshot(ModelBendsPlayer model) {
            for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
                if (bone.part(model) != null) this.parts.add(orientation(bone.part(model)));
            }
            this.root = new Vector3f(model.renderRotation.vSmooth);
            this.item = new Vector3f(model.rightItem.rotation.vSmooth.lengthSquared() > 0
                ? model.rightItem.rotation.vSmooth : model.renderItemRotation.vSmooth);
            this.offset = new Vector3f(model.renderOffset.vSmooth);
        }

        /** Degrees per part, then root, item, and the offset in fifths of a pixel. */
        float[] differences(Snapshot other) {
            float[] result = new float[this.parts.size() + 3];
            for (int i = 0; i < this.parts.size(); ++i) result[i] = angleBetween(this.parts.get(i), other.parts.get(i));
            result[this.parts.size()] = maxDifference(this.root, other.root);
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

    private static float maxDifference(Vector3f a, Vector3f b) {
        return Math.max(Math.abs(a.x - b.x), Math.max(Math.abs(a.y - b.y), Math.abs(a.z - b.z)));
    }
}
