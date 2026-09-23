package net.gobbob.mobends.client.animation;

import static net.gobbob.mobends.client.animation.AnimationBaking.KEEP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsSpider;
import net.minecraft.util.MathHelper;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

/**
 * The spider's walk ({@code spider/walk.animation.json}) was baked from its original Java walk, kept below as the
 * reference, and must keep matching it. Run with {@code MOBENDS_BAKE_SPIDER=1} to bake it again.
 */
class SpiderAnimationTest {
    private static final Path DIR = Paths.get("src/main/resources/assets/mobends/geckolib/animations/spider");
    /** One loop of the walk: a stride of the limb swing (0.6662 radians per unit), as BendsAnimator.playStride times it. */
    private static final double STRIDE = 2 * Math.PI / 0.6662;

    @Test
    void committedRigFileIsUpToDate() throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().create()
            .toJson(PlayerRig.exportGeometry(new ModelBendsSpider(), "geometry.mobends.spider")) + "\n";
        Path file = Paths.get("blockbench/spider.geo.json");
        if ("1".equals(System.getenv("MOBENDS_UPDATE_RIG"))) Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        String committed = Files.exists(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
        assertEquals(committed, json, "spider.geo.json is stale; regenerate with MOBENDS_UPDATE_RIG=1 ./gradlew test");
    }

    /** The rig names each part once, and the legs from front to back, right and left. */
    @Test
    void rigCoversEveryPart() {
        ModelBendsSpider model = new ModelBendsSpider();
        java.util.Set<Object> parts = new java.util.HashSet<Object>();
        for (String bone : model.rigBones()) {
            ModelRendererBends part = model.rigPart(bone);
            if (part != null) assertTrue(parts.add(part), bone + " drives a part twice");
        }
        assertEquals(3 + 16, parts.size());
        // Front legs are nearest the head (most negative z), right legs on the right (negative x).
        assertTrue(model.rigPart("rightLeg1").rotationPointZ < model.rigPart("rightLeg4").rotationPointZ);
        assertTrue(model.rigPart("rightLeg1").rotationPointX < 0 && model.rigPart("leftLeg1").rotationPointX > 0);
    }

    // --- Baking ---------------------------------------------------------------------------------------------------

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_SPIDER"))) return;
        Map<String, Function<ModelBendsSpider, float[]>> still = new LinkedHashMap<String, Function<ModelBendsSpider, float[]>>();
        Map<String, String[]> templates = new LinkedHashMap<String, String[]>();
        still.put("head rotation", m -> new float[] {0, 0, 0});
        templates.put("head rotation", new String[] {"q.mobends_head_pitch", "q.mobends_head_yaw", KEEP});
        still.put("body rotation", m -> xyz(m.rigPart("body").rotation.vFinal));
        templates.put("body rotation", new String[] {null, KEEP, KEEP});
        Map<String, Function<ModelBendsSpider, float[]>> swing = new LinkedHashMap<String, Function<ModelBendsSpider, float[]>>();
        for (String side : new String[] {"right", "left"}) {
            for (int i = 1; i <= 4; ++i) {
                String leg = side + "Leg" + i, fore = side + "ForeLeg" + i;
                // The Java turns a leg forward or back with its pre-rotation (-Y), then lifts it (Z).
                Function<ModelBendsSpider, float[]> aim = m -> new float[] {0, -m.rigPart(leg).pre_rotation.vFinal.y, 0};
                Function<ModelBendsSpider, float[]> lift = m -> new float[] {0, 0, m.rigPart(leg).rotation.vFinal.z};
                still.put(leg + "Aim rotation", aim);
                still.put(leg + " rotation", lift);
                swing.put(leg + "Aim rotation", aim);
                swing.put(leg + " rotation", lift);
                templates.put(leg + " rotation", new String[] {KEEP, KEEP, null});
                still.put(fore + " rotation", m -> new float[] {0, 0, m.rigPart(fore).rotation.vFinal.z});
                templates.put(fore + " rotation", new String[] {KEEP, KEEP, null});
            }
        }
        String json = AnimationBaking.file(
            AnimationBaking.bakeTargets("walk", STRIDE, still, templates, ModelBendsSpider::new, (m, t) -> java(m, t, 0)),
            AnimationBaking.bakeTargets("walk_swing", STRIDE, swing, templates, ModelBendsSpider::new, (m, t) -> java(m, t, 1)));
        Files.createDirectories(DIR);
        Files.write(DIR.resolve("walk.animation.json"),
            json.replace("animation.mobends.player.", "animation.mobends.spider.").getBytes(StandardCharsets.UTF_8));
    }

    private static float[] xyz(org.lwjgl.util.vector.Vector3f v) {
        return new float[] {v.x, v.y, v.z};
    }

    /** The original Java spider walk, the reference the Blockbench walk was baked from. */
    static void java(ModelBendsSpider model, float limbSwing, float amount) {
        float f9 = -(MathHelper.cos(limbSwing * 0.6662f * 2.0f + 0.0f) * 0.4f) * amount;
        float f10 = -(MathHelper.cos(limbSwing * 0.6662f * 2.0f + (float)Math.PI) * 0.4f) * amount;
        float f11 = -(MathHelper.cos(limbSwing * 0.6662f * 2.0f + 1.5707964f) * 0.4f) * amount;
        float f12 = -(MathHelper.cos(limbSwing * 0.6662f * 2.0f + 4.712389f) * 0.4f) * amount;
        float f13 = Math.abs(MathHelper.sin(limbSwing * 0.6662f + 0.0f) * 0.4f) * amount;
        float f14 = Math.abs(MathHelper.sin(limbSwing * 0.6662f + (float)Math.PI) * 0.4f) * amount;
        float f15 = Math.abs(MathHelper.sin(limbSwing * 0.6662f + 1.5707964f) * 0.4f) * amount;
        float f16 = Math.abs(MathHelper.sin(limbSwing * 0.6662f + 4.712389f) * 0.4f) * amount;
        ((ModelRendererBends)model.spiderHead).rotation.setY(model.headRotationY);
        ((ModelRendererBends)model.spiderHead).rotation.setX(model.headRotationX);
        float f6 = 0.7853982f;
        float sm = 40.0f;
        ((ModelRendererBends)model.spiderLeg1).rotation.setZ(sm + f13 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg2).rotation.setZ(-sm - f13 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg3).rotation.setZ(sm + f14 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg4).rotation.setZ(-sm - f14 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg5).rotation.setZ(sm + f15 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg6).rotation.setZ(-sm - f15 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg7).rotation.setZ(sm + f16 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg8).rotation.setZ(-sm - f16 / (float)Math.PI * 180.0f);
        float f7 = -0.0f;
        float f8 = 0.3926991f;
        ((ModelRendererBends)model.spiderLeg1).pre_rotation.setY(-70.0f + f9 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg2).pre_rotation.setY(70.0f - f9 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg3).pre_rotation.setY(-40.0f + f10 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg4).pre_rotation.setY(40.0f - f10 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg5).pre_rotation.setY(40.0f + f11 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg6).pre_rotation.setY(-40.0f - f11 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg7).pre_rotation.setY(70.0f + f12 / (float)Math.PI * 180.0f);
        ((ModelRendererBends)model.spiderLeg8).pre_rotation.setY(-70.0f - f12 / (float)Math.PI * 180.0f);
        float foreBend = 89.0f;
        model.spiderForeLeg1.rotation.setSmoothZ(-foreBend);
        model.spiderForeLeg2.rotation.setSmoothZ(foreBend);
        model.spiderForeLeg3.rotation.setSmoothZ(-foreBend);
        model.spiderForeLeg4.rotation.setSmoothZ(foreBend);
        model.spiderForeLeg5.rotation.setSmoothZ(-foreBend);
        model.spiderForeLeg6.rotation.setSmoothZ(foreBend);
        model.spiderForeLeg7.rotation.setSmoothZ(-foreBend);
        model.spiderForeLeg8.rotation.setSmoothZ(foreBend);
        ((ModelRendererBends)model.spiderBody).rotation.setX(-30.0f);

    }

    // --- Parity ---------------------------------------------------------------------------------------------------

    @Test
    void walkMatches() throws Exception {
        BakedAnimations file = AnimationBaking.load(DIR.resolve("walk.animation.json"));
        Animation still = file.getAnimation("animation.mobends.spider.walk"), swing = file.getAnimation("animation.mobends.spider.walk_swing");
        for (float amount : new float[] {0, 0.4f, 1}) {
            for (float[] look : new float[][] {{0, 0}, {20, -35}}) {
                ModelBendsSpider java = spider(look), baked = spider(look);
                float worst = 0;
                String where = "";
                for (float t = 0; t < 400; t += AnimationBaking.TICKS_PER_FRAME) {
                    float limbSwing = t * 0.8f;
                    frame(java, m -> java(m, limbSwing, amount));
                    frame(baked, m -> {
                        m.limbSwing = limbSwing;
                        m.limbSwingAmount = amount;
                        // As BendsAnimator.playStride.
                        double stride = limbSwing * 0.6662 / (2 * Math.PI);
                        stride -= Math.floor(stride);
                        apply(m, still, stride * still.length(), 1);
                        apply(m, swing, stride * swing.length(), amount);
                    });
                    if (t < 200) continue;
                    for (String bone : java.rigBones()) {
                        ModelRendererBends a = java.rigPart(bone), b = baked.rigPart(bone);
                        if (a == null) continue;
                        float off = AnimationBaking.angleBetween(orientation(a), orientation(b));
                        if (off > worst) {
                            worst = off;
                            where = bone + " at tick " + t;
                        }
                    }
                }
                assertTrue(worst < 1.5f, "walk swinging " + amount + ", look " + look[0] + "/" + look[1] + ": differs by "
                    + worst + " degrees (" + where + ")");
            }
        }
    }

    private static ModelBendsSpider spider(float[] look) {
        ModelBendsSpider model = new ModelBendsSpider();
        model.headRotationX = look[0];
        model.headRotationY = look[1];
        return model;
    }

    private static Matrix4f orientation(ModelRendererBends part) {
        return AnimationBaking.orientation(part);
    }

    /** A frame of the spider model's bookkeeping, as its setRotationAngles does it. */
    private static void frame(ModelBendsSpider model, java.util.function.Consumer<ModelBendsSpider> animate) {
        animate.accept(model);
        for (String bone : model.rigBones()) if (model.rigPart(bone) != null) model.rigPart(bone).update(AnimationBaking.TICKS_PER_FRAME);
    }

    private static void apply(ModelBendsSpider model, Animation animation, double seconds, float weight) {
        BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(model, null)),
            BendsAnimator.FOLLOW_RATE, weight);
    }
}
