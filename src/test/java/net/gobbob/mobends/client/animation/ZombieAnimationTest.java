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
import java.util.function.BiConsumer;
import net.gobbob.mobends.animation.zombie.Animation_Stand;
import net.gobbob.mobends.animation.zombie.Animation_Walk;
import net.gobbob.mobends.client.animation.AnimationBaking.Channel;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.model.entity.ModelBendsZombie;
import org.junit.jupiter.api.Test;

/**
 * The zombie's stand and walk were baked from its Java animations and must keep matching them. The zombie has the
 * player's rig without item bones, so both are baked and compared on the player's test model; a last check plays
 * them on the zombie's own model. Run with {@code MOBENDS_BAKE_ZOMBIE=1} to bake them again.
 */
class ZombieAnimationTest {
    private static final Path DIR = Paths.get("src/main/resources/assets/mobends/geckolib/animations/zombie");
    /** A zombie's typical limb swing amount, walking. */
    private static final float CADENCE = 0.7f;
    private static final Bone[] ARMS_AND_LEGS = {Bone.RIGHT_ARM, Bone.LEFT_ARM, Bone.RIGHT_LEG, Bone.LEFT_LEG};

    @Test
    void committedRigFileIsUpToDate() throws IOException {
        String json = new GsonBuilder().setPrettyPrinting().create()
            .toJson(PlayerRig.exportGeometry(new ModelBendsZombie(), "geometry.mobends.zombie")) + "\n";
        Path file = Paths.get("blockbench/zombie.geo.json");
        if ("1".equals(System.getenv("MOBENDS_UPDATE_RIG"))) Files.write(file, json.getBytes(StandardCharsets.UTF_8));
        String committed = Files.exists(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
        assertEquals(committed, json, "zombie.geo.json is stale; regenerate with MOBENDS_UPDATE_RIG=1 ./gradlew test");
    }

    // --- Baking ---------------------------------------------------------------------------------------------------

    @Test
    void bakeWhenAsked() throws IOException {
        if (!"1".equals(System.getenv("MOBENDS_BAKE_ZOMBIE"))) return;
        AnimationBaking.Derived stand = AnimationBaking.derive((m, t) -> Animation_Stand.apply(m), null, 0);
        write("stand", AnimationBaking.bake("stand", 20, stand.channels, stand.templates, new LinkedHashMap<String, String>(),
            (m, t) -> Animation_Stand.apply(m)));

        // The walk with the limbs still (the body's bob and sway, the knees' and elbows' steps, the head), and the arms
        // and legs at full swing over it, both a stride long.
        BiConsumer<ModelBendsPlayer, Float> still = stride(0), swinging = stride(1);
        AnimationBaking.Derived walk = AnimationBaking.derive(still, null, 0, 3, 6, 9);
        AnimationBaking.Derived full = AnimationBaking.derive(swinging, null, 0, 3, 6, 9);
        Map<String, Channel> swing = new LinkedHashMap<String, Channel>();
        Map<String, String[]> swingTemplates = new LinkedHashMap<String, String[]>();
        for (Bone bone : ARMS_AND_LEGS) {
            swing.put(bone.id + " rotation", full.channels.get(bone.id + " rotation"));
            swingTemplates.put(bone.id + " rotation", full.templates.get(bone.id + " rotation"));
        }
        Map<String, String> armsUp = new LinkedHashMap<String, String>();
        armsUp.put("rightArm rotation", AnimationBaking.hold("-120", "\"" + KEEP + "\"", "\"" + KEEP + "\""));
        armsUp.put("leftArm rotation", AnimationBaking.hold("-120", "\"" + KEEP + "\"", "\"" + KEEP + "\""));
        double period = 2 * Math.PI / (0.6662 * CADENCE);
        write("walk",
            AnimationBaking.bake("walk", period, walk.channels, walk.templates, new LinkedHashMap<String, String>(), still),
            AnimationBaking.bake("walk_swing", period, swing, swingTemplates, new LinkedHashMap<String, String>(), swinging),
            AnimationBaking.animation("walk_arms_up", 1, armsUp));
    }

    /** The Java walk with the limbs advancing at the zombie's cadence and swinging by {@code amount}. */
    private static BiConsumer<ModelBendsPlayer, Float> stride(float amount) {
        return (model, ticks) -> {
            model.armSwing = ticks * CADENCE;
            model.armSwingAmount = amount;
            Animation_Walk.apply(model, false);
        };
    }

    private static void write(String file, String... animations) throws IOException {
        Files.createDirectories(DIR);
        String json = AnimationBaking.file(animations).replace("animation.mobends.player.", "animation.mobends.zombie.");
        Files.write(DIR.resolve(file + ".animation.json"), json.getBytes(StandardCharsets.UTF_8));
    }

    // --- Parity ---------------------------------------------------------------------------------------------------

    @Test
    void standMatches() throws Exception {
        Animation stand = load("stand").getAnimation("animation.mobends.zombie.stand");
        for (float[] look : new float[][] {{0, 0}, {25, -40}}) {
            assertMatches("stand", look, (m, t) -> Animation_Stand.apply(m), (m, t) -> apply(m, stand, 0, 1));
        }
    }

    @Test
    void walkMatches() throws Exception {
        BakedAnimations file = load("walk");
        for (boolean armsUp : new boolean[] {false, true}) {
            for (float amount : new float[] {0.3f, CADENCE, 1}) {
                for (float[] look : new float[][] {{0, 0}, {20, 35}}) {
                    assertMatches("walk" + (armsUp ? " reaching" : "") + " swinging " + amount, look, (m, t) -> {
                        m.armSwing = t * CADENCE;
                        m.armSwingAmount = amount;
                        Animation_Walk.apply(m, armsUp);
                    }, (m, t) -> {
                        m.armSwing = t * CADENCE;
                        m.armSwingAmount = amount;
                        walk(m, file, armsUp);
                    });
                }
            }
        }
    }

    /** Played on the zombie's own model, the files pose it exactly as the player's test model. */
    @Test
    void theZombieModelPlaysTheSame() throws Exception {
        playsLikeThePlayerModel(new ModelBendsZombie());
    }

    /** Et Futurum's zombie villager, reshaped on the zombie's rig, animates the same as well. */
    @Test
    void etFuturumZombieVillagersPlayTheSame() throws Exception {
        playsLikeThePlayerModel(new net.gobbob.mobends.client.model.entity.ModelBendsZombieVillagerEtFuturum());
    }

    /** Each of its boxes reads Et Futurum's zombie villager texture where Et Futurum's own model does. */
    @Test
    void etFuturumZombieVillagerBoxesUseItsTextureLayout() {
        net.gobbob.mobends.client.model.entity.ModelBendsZombieVillagerEtFuturum model =
            new net.gobbob.mobends.client.model.entity.ModelBendsZombieVillagerEtFuturum();
        assertTexture(model.bipedHead, 0, 0, 0);
        assertTexture(model.bipedBody, 0, 16, 20);
        assertTexture(model.bipedBody, 1, 0, 38);
        for (boolean right : new boolean[] {true, false}) {
            assertTexture(right ? model.bipedRightArm : model.bipedLeftArm, 0, 44, 38);
            assertTexture(model.foreArm(right), 0, 44, 44);
            assertTexture(right ? model.bipedRightLeg : model.bipedLeftLeg, 0, 0, 22);
            assertTexture(model.foreLeg(right), 0, 0, 28);
        }
    }

    private static void assertTexture(net.minecraft.client.model.ModelRenderer part, int box, int u, int v) {
        net.gobbob.mobends.client.model.ModelBoxBends cube = (net.gobbob.mobends.client.model.ModelBoxBends)part.cubeList.get(box);
        assertEquals(u + "," + v, (int)cube.txOffsetX + "," + (int)cube.txOffsetY, "texture of box " + box);
    }

    private static void playsLikeThePlayerModel(ModelBendsZombie zombie) throws Exception {
        BakedAnimations file = load("walk");
        ModelBendsPlayer player = AnimationBaking.model(15, -20);
        zombie.headRotationX = 15;
        zombie.headRotationY = -20;
        for (float t = 0; t < 60; t += AnimationBaking.TICKS_PER_FRAME) {
            player.armSwing = zombie.armSwing = t * CADENCE;
            player.armSwingAmount = zombie.armSwingAmount = CADENCE;
            AnimationBaking.frame(player, m -> walk(m, file, true));
            zombieFrame(zombie, file);
        }
        for (Bone bone : Bone.values()) {
            ModelRendererBends a = bone.part(player), b = bone.part(zombie);
            if (a == null) continue;
            assertEquals(a.rotation.vSmooth, b.rotation.vSmooth, bone.id + " rotation");
            assertEquals(a.aim.vSmooth, b.aim.vSmooth, bone.id + " aim");
        }
        assertEquals(player.renderOffset.vSmooth, zombie.renderOffset.vSmooth, "root offset");
    }

    /** One frame of the zombie model, as ModelBendsZombie's own bookkeeping does it. */
    private static void zombieFrame(ModelBendsZombie zombie, BakedAnimations file) {
        zombie.renderOffset.setSmooth(new org.lwjgl.util.vector.Vector3f(0, -1, 0), 0.5f);
        zombie.renderRotation.setSmooth(new org.lwjgl.util.vector.Vector3f(0, 0, 0), 0.5f);
        walk(zombie, file, true);
        for (Bone bone : Bone.values()) if (bone.part(zombie) != null) bone.part(zombie).update(AnimationBaking.TICKS_PER_FRAME);
        zombie.renderOffset.update(AnimationBaking.TICKS_PER_FRAME);
        zombie.renderRotation.update(AnimationBaking.TICKS_PER_FRAME);
    }

    /** As Animation_Walk plays it: the stride's two layers, and the reach over them. */
    private static void walk(AnimatedModel model, BakedAnimations file, boolean armsUp) {
        double stride = model.limbSwing() * 0.6662 / (2 * Math.PI);
        stride -= Math.floor(stride);
        Animation main = file.getAnimation("animation.mobends.zombie.walk"), swing = file.getAnimation("animation.mobends.zombie.walk_swing");
        apply(model, main, stride * main.length(), 1);
        apply(model, swing, stride * swing.length(), model.limbSwingAmount());
        if (armsUp) apply(model, file.getAnimation("animation.mobends.zombie.walk_arms_up"), 0, 1);
    }

    private static void assertMatches(String what, float[] look, BiConsumer<ModelBendsPlayer, Float> java,
        BiConsumer<ModelBendsPlayer, Float> baked) {
        String[] where = {""};
        float worst = AnimationBaking.compare(AnimationBaking.model(look[0], look[1]), AnimationBaking.model(look[0], look[1]),
            java, baked, where);
        assertTrue(worst < 1.5f, what + ", look " + look[0] + "/" + look[1] + ": differs by " + worst + " degrees (" + where[0] + ")");
    }

    private static void apply(AnimatedModel model, Animation animation, double seconds, float weight) {
        BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(model, null)),
            BendsAnimator.FOLLOW_RATE, weight);
    }

    private static BakedAnimations load(String file) throws IOException {
        return AnimationBaking.load(DIR.resolve(file + ".animation.json"));
    }
}
