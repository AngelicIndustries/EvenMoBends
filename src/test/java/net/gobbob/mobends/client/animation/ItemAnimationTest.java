package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.animation.BoneAnimation;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.renderer.base.GeoModelRenderer;
import com.geckolib.renderer.base.GeoRenderTransforms;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

/** The shipped item test animations: a spin in the hand, and a throw that leaves and returns to the hand. */
class ItemAnimationTest {
    private static final Path ANIMATIONS = Paths.get("src/main/resources/assets/mobends/geckolib/animations");
    private static final String ITEM_TEST = "player/item_test.animation.json";

    @Test
    void everyShippedAnimationUsesRigBones() throws Exception {
        for (Path file : animationFiles()) {
            for (Animation animation : load(file).animations().values()) {
                for (BoneAnimation track : animation.boneAnimations()) {
                    String id = track.boneName();
                    assertTrue(PlayerRig.ROOT.equals(id) || Bone.byId(id) != null,
                        file + ": " + animation.name() + " animates unknown bone " + id);
                }
            }
        }
    }

    /** At release and catch, the free item is exactly where the hand holds it. */
    @Test
    void throwHandsOffSeamlessly() throws Exception {
        Animation animation = itemTest().getAnimation("animation.mobends.player.item_throw");
        for (boolean slim : new boolean[] {false, true}) {
            for (double seconds : new double[] {0.3, 1.3}) {
                GeoRenderTransforms frames = rigPose(slim, animation, seconds);
                Matrix4f hand = frames.getBone("rightItem").get().modelSpaceMatrix();
                Matrix4f free = frames.getBone("rightItemFree").get().modelSpaceMatrix();
                assertTrue(hand.equals(free, 1e-4f),
                    (slim ? "slim" : "classic") + " at " + seconds + "s\nhand\n" + hand + "free\n" + free);
            }
            Matrix4f release = rigPose(slim, animation, 0.3).getBone("rightItemFree").get().modelSpaceMatrix();
            Matrix4f apex = rigPose(slim, animation, 0.8).getBone("rightItemFree").get().modelSpaceMatrix();
            assertEquals(40 / 16f, apex.m31() - release.m31(), 1e-4f, "rises 40 px");
        }
    }

    @Test
    void throwShowsTheItemInTheHandOrInTheAirButNotBoth() throws Exception {
        Animation animation = itemTest().getAnimation("animation.mobends.player.item_throw");
        for (double seconds : new double[] {0.1, 0.29, 1.31, 1.5}) {
            ModelBendsPlayer model = played(animation, seconds);
            assertTrue(model.rightItem.isVisible(), "in hand at " + seconds);
            assertFalse(model.rightItemFree.isVisible(), "not in the air at " + seconds);
        }
        for (double seconds : new double[] {0.31, 0.8, 1.29}) {
            ModelBendsPlayer model = played(animation, seconds);
            assertFalse(model.rightItem.isVisible(), "not in hand at " + seconds);
            assertTrue(model.rightItemFree.isVisible(), "in the air at " + seconds);
        }
    }

    @Test
    void spinTurnsAndKeepsTurningAcrossTheLoop() throws Exception {
        Animation animation = itemTest().getAnimation("animation.mobends.player.item_spin");
        assertEquals(180, played(animation, 0.25).rightItem.rotation.vFinal.x, 1e-3f);
        // 0.49 s -> 0.01 s across the loop is 352.8 -> 7.2 degrees: 14.4 degrees forward, not 345.6 back.
        ModelBendsPlayer model = played(animation, 0.49);
        model.rightItem.update(1); // Settle on the target.
        float before = model.rightItem.rotation.vSmooth.x;
        apply(model, animation, 0.01);
        assertEquals(14.4f, model.rightItem.rotation.vFinal.x - before, 1e-3f, "wraps forward, not back through 0");
    }

    /** After a long spin, switching to an animation that doesn't turn the item eases it back by half a turn at most. */
    @Test
    void switchingAwayFromASpinDoesNotUnwindIt() throws Exception {
        Animation spin = itemTest().getAnimation("animation.mobends.player.item_spin");
        Animation thrown = itemTest().getAnimation("animation.mobends.player.item_throw");
        ModelBendsPlayer model = new ModelBendsPlayer();
        float ticksPerFrame = 20 / 60f;
        for (double seconds = 0; seconds < 5; seconds += 1 / 60.0) {
            apply(model, spin, seconds % spin.length());
            model.rightItem.update(ticksPerFrame);
        }
        float turned = 0;
        float previous = model.rightItem.rotation.vSmooth.x;
        assertTrue(Math.abs(previous) <= 180, "kept within half a turn while spinning: " + previous);
        for (double seconds = 0; seconds < 1; seconds += 1 / 60.0) {
            apply(model, thrown, seconds);
            model.rightItem.update(ticksPerFrame);
            turned += Math.abs(model.rightItem.rotation.vSmooth.x - previous);
            previous = model.rightItem.rotation.vSmooth.x;
        }
        assertTrue(turned <= 180.001f, "eased back " + turned + " degrees");
        assertEquals(0, previous, 0.5f, "settled at rest");
    }

    @Test
    void wrappingKeepsProceduralAnglesButFoldsLongSpins() {
        net.gobbob.mobends.util.SmoothVector3f angle = new net.gobbob.mobends.util.SmoothVector3f();
        angle.vSmooth.set(-300, 350, 7270);
        angle.wrapTurns(360);
        assertEquals(-300, angle.vSmooth.x, 1e-3f, "within a turn: left alone");
        assertEquals(350, angle.vSmooth.y, 1e-3f);
        assertEquals(70, angle.vSmooth.z, 1e-3f, "20 turns and 70 degrees: folded");
        angle.wrapTurns(180);
        assertEquals(60, angle.vSmooth.x, 1e-3f);
        assertEquals(-10, angle.vSmooth.y, 1e-3f);
    }

    @Test
    void itemsAreVisibleOnlyInTheHandByDefault() {
        ModelBendsPlayer model = new ModelBendsPlayer();
        assertTrue(model.rightItem.isVisible());
        assertFalse(model.rightItemFree.isVisible());
    }

    private static GeoRenderTransforms rigPose(boolean slim, Animation animation, double seconds) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.ssb$set64x();
        model.ssb$setSlim(slim);
        BakedGeoModel baked = new GeckoLibGsonLoader()
            .loadModel(new ResourceLocation("mobends", "geckolib/models/player.geo.json"), PlayerRig.exportGeometry(model, "geometry.test"));
        ModelPose pose = ModelPose.create(baked);
        Map<String, BoneSnapshot> snapshots = AnimationProcessor.evaluate(animation, seconds, variable -> 0);
        pose.apply(snapshots, false);
        // Compare where the item is, whichever of the two is showing at this instant.
        pose.get("rightItem").get().setScale(1, 1, 1);
        pose.get("rightItemFree").get().setScale(1, 1, 1);
        return GeoModelRenderer.captureTransforms(baked, pose);
    }

    private static ModelBendsPlayer played(Animation animation, double seconds) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        apply(model, animation, seconds);
        return model;
    }

    private static void apply(ModelBendsPlayer model, Animation animation, double seconds) {
        model.rightItem.resetScale();
        model.rightItemFree.resetScale();
        BendsAnimator.apply(model, animation, AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(model, null)));
    }

    private static BakedAnimations itemTest() throws Exception {
        BakedAnimations animations = load(ANIMATIONS.resolve(ITEM_TEST));
        assertNotNull(animations.getAnimation("animation.mobends.player.item_throw"));
        return animations;
    }

    private static BakedAnimations load(Path file) throws Exception {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return new GeckoLibGsonLoader().loadAnimations(reader);
        }
    }

    private static List<Path> animationFiles() throws Exception {
        List<Path> files = new ArrayList<Path>();
        try (Stream<Path> walk = Files.walk(ANIMATIONS)) {
            walk.filter(path -> path.toString().endsWith(".animation.json")).forEach(files::add);
        }
        assertTrue(files.size() >= 2);
        return files;
    }
}
