package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.AnimationProcessor;
import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.animation.Animation;
import com.geckolib.cache.animation.BakedAnimations;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.renderer.base.GeoModelRenderer;
import com.geckolib.renderer.base.GeoRenderTransforms;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** Both strokes keep a folded pull and an extended reach while clearing the head. */
class HoldingSwimAnimationTest {
    @Test
    void returningHandsDoNotMakeAnExtraOutwardDetour() throws Exception {
        for (BakedAnimations file : files()) {
            String base = file.getAnimation("animation.mobends.player.swim") != null ? "swim" : "dive";
            for (String suffix : new String[] {"", "_holding"}) {
                Animation animation = file.getAnimation("animation.mobends.player." + base + suffix);
                for (boolean slim : new boolean[] {false, true}) {
                    ModelBendsPlayer model = new ModelBendsPlayer();
                    model.ssb$setSlim(slim);
                    BakedGeoModel rig = rig(model);
                    for (String bone : new String[] {"rightItem", "leftItem"}) {
                        float nearest = Float.POSITIVE_INFINITY;
                        // The recovery crosses the loop seam and ends at the front reach.
                        for (int step = 0; step <= 100; ++step) {
                            double phase = (0.70 + step * 0.0035) % 1;
                            GeoRenderTransforms pose = frames(rig, animation, animation.length() * phase, 0, 0);
                            Vector3f grip = new Matrix4f(pose.getBone("body").get().modelSpaceMatrix()).invert()
                                .mul(pose.getBone(bone).get().modelSpaceMatrix()).getTranslation(new Vector3f());
                            float distance = Math.abs(grip.x) * 16;
                            // Allow only sub-pixel keyframe rounding, not a second outward sweep.
                            assertTrue(distance <= nearest + 0.03f, animation.name() + " " + bone
                                + " reverses during recovery: phase=" + phase + " slim=" + slim);
                            nearest = Math.min(nearest, distance);
                        }
                    }
                }
            }
        }
    }

    @Test
    void diveElbowsNeverBendBackwardAndOpenTheJoint() throws Exception {
        BakedAnimations file = AnimationBaking.load(DiveAnimationTest.FILE);
        for (String suffix : new String[] {"", "_holding"}) {
            Animation animation = file.getAnimation("animation.mobends.player.dive" + suffix);
            for (float yaw : new float[] {0, -75, 75}) {
                for (int step = 0; step < 240; ++step) {
                    for (String bone : new String[] {"rightForeArm", "leftForeArm"}) {
                        float elbow = (float)Math.toDegrees(-AnimationProcessor.evaluate(animation,
                            animation.length() * step / 240,
                            variable -> variable.equals("query.mobends_head_yaw") ? yaw : 0).get(bone).getRotX());
                        // The forearm's hinge is on its back edge and its top face is hidden.
                        // Positive X pulls the open front edge away from the upper arm.
                        assertTrue(elbow <= 0, animation.name() + " " + bone + " opens at frame=" + step);
                    }
                }
            }
        }
    }

    @Test
    void inwardRecoveryDoesNotLockTheElbowsStraight() throws Exception {
        for (BakedAnimations file : files()) {
            String base = file.getAnimation("animation.mobends.player.swim") != null ? "swim" : "dive";
            for (String suffix : new String[] {"", "_holding"}) {
                Animation animation = file.getAnimation("animation.mobends.player." + base + suffix);
                for (float yaw : new float[] {0, -75, 75}) {
                    // The inward return crosses the loop boundary before the next outward pull.
                    for (double phase : new double[] {0.9, 0.975, 0.025, 0.075}) {
                        for (String bone : new String[] {"rightForeArm", "leftForeArm"}) {
                            float elbow = (float)Math.toDegrees(-AnimationProcessor.evaluate(animation,
                                animation.length() * phase,
                                variable -> variable.equals("query.mobends_head_yaw") ? yaw : 0).get(bone).getRotX());
                            assertTrue(elbow < -5, animation.name() + " " + bone
                                + " straightens before the inward return ends: phase=" + phase + " yaw=" + yaw);
                        }
                    }
                }
            }
        }
    }

    @Test
    void holdingLoopsCloseAndKeepTheOriginalStrokePeriod() throws Exception {
        for (BakedAnimations file : files()) {
            String base = file.getAnimation("animation.mobends.player.swim") != null ? "swim" : "dive";
            Animation original = file.getAnimation("animation.mobends.player." + base);
            Animation holding = file.getAnimation(original.name() + "_holding");
            assertNotNull(holding);
            assertEquals(original.length(), holding.length());
            assertEquals(holding.name(), BendsAnimator.variant(file, original.name(), true));
            assertEquals(original.name(), BendsAnimator.variant(file, original.name(), false));
            ModelBendsPlayer model = new ModelBendsPlayer();
            BakedGeoModel rig = rig(model);
            GeoRenderTransforms first = frames(rig, holding, 0, 0, 0);
            GeoRenderTransforms last = frames(rig, holding, holding.length(), 0, 0);
            for (PlayerRig.Bone bone : PlayerRig.Bone.values()) {
                assertTrue(first.getBone(bone.id).get().modelSpaceMatrix().equals(
                    last.getBone(bone.id).get().modelSpaceMatrix(), 0.0001f), holding.name() + " loop seam: " + bone.id);
            }
        }
    }

    @Test
    void swordAndBlockClearTheHeadThroughoutHoldingStrokes() throws Exception {
        List<Vector3f> sword = new ArrayList<>();
        BufferedImage icon = ImageIO.read(getClass().getResourceAsStream("/assets/minecraft/textures/items/iron_sword.png"));
        for (int v = 0; v < 16; ++v) {
            for (int u = 0; u < 16; ++u) {
                if ((icon.getRGB(u, v) >>> 24) == 0) continue;
                for (int corner = 0; corner < 8; ++corner) {
                    double[] p = BlockbenchExport.SWORD.transform(15 - u + (corner & 1),
                        15 - v + ((corner >> 1) & 1), -((corner >> 2) & 1));
                    sword.add(new Vector3f((float)p[0], (float)p[1], (float)p[2]));
                }
            }
        }
        List<Vector3f> block = new ArrayList<>();
        for (int corner = 0; corner < 8; ++corner) {
            double[] p = BlockbenchExport.BLOCK.transform((corner & 1) == 0 ? -8 : 8,
                (corner & 2) == 0 ? -8 : 8, (corner & 4) == 0 ? -8 : 8);
            block.add(new Vector3f((float)p[0], (float)p[1], (float)p[2]));
        }
        for (BakedAnimations file : files()) {
            String base = file.getAnimation("animation.mobends.player.swim") != null ? "swim" : "dive";
            Animation animation = file.getAnimation("animation.mobends.player." + base + "_holding");
            for (boolean slim : new boolean[] {false, true}) {
                ModelBendsPlayer model = new ModelBendsPlayer();
                model.ssb$set64x();
                model.ssb$setSlim(slim);
                BakedGeoModel rig = rig(model);
                for (float pitch : new float[] {-85, -45, 0, 45, 85}) {
                    for (float yaw : new float[] {-75, -40, 0, 40, 75}) {
                        for (int step = 0; step < 120; ++step) {
                            GeoRenderTransforms pose = frames(rig, animation, animation.length() * step / 120, pitch, yaw);
                            // Salamander's bone-local blocks -> the game's bone-local pixels (X/Y mirrored).
                            Matrix4f hand = new Matrix4f().scale(-16, -16, 16)
                                .mul(pose.getBone("head").get().modelSpaceMatrix().invert())
                                .mul(pose.getBone("rightItem").get().modelSpaceMatrix())
                                .scale(-1 / 16f, -1 / 16f, 1 / 16f);
                            String where = base + " slim=" + slim + " pitch=" + pitch + " yaw=" + yaw + " frame=" + step;
                            for (int i = 0; i < sword.size(); i += 8) assertClear(hand, sword.subList(i, i + 8), where + " sword");
                            assertClear(hand, block, where + " block");
                        }
                    }
                }
            }
        }
    }

    /**
     * Flat items take the strokes' forward tilt, which keeps them off the head, but not the side turn that swings a blade
     * out: on a flat item it sticks out from the hand like a flipper.
     */
    @Test
    void flatItemsSkipTheSideTurnAndClearTheHeadThroughoutHoldingStrokes() throws Exception {
        BufferedImage icon = ImageIO.read(getClass().getResourceAsStream("/assets/minecraft/textures/items/apple.png"));
        List<Vector3f> texels = new ArrayList<>();
        for (int v = 0; v < 16; ++v) {
            for (int u = 0; u < 16; ++u) {
                if ((icon.getRGB(u, v) >>> 24) == 0) continue;
                for (int corner = 0; corner < 8; ++corner) texels.add(FLAT.transformPosition(new Vector3f(15 - u + (corner & 1),
                    15 - v + ((corner >> 1) & 1), -((corner >> 2) & 1))));
            }
        }
        for (BakedAnimations file : files()) {
            String base = file.getAnimation("animation.mobends.player.swim") != null ? "swim" : "dive";
            Animation animation = file.getAnimation("animation.mobends.player." + base + "_holding");
            for (String name : new String[] {"player", "player_slim"}) {
                BakedGeoModel baked = BendsGeometryTest.load(name);
                ModelBendsPlayer model = (ModelBendsPlayer)BendsGeometryTest.models().get(name);
                for (float pitch : new float[] {-85, -45, 0, 45, 85}) {
                    for (float yaw : new float[] {-75, -40, 0, 40, 75}) {
                        for (int step = 0; step < 48; ++step) {
                            double seconds = animation.length() * step / 48;
                            model.headRotationX = pitch;
                            model.headRotationY = yaw;
                            AnimationBaking.frame(model, m -> BendsAnimator.apply(m, animation, AnimationProcessor.evaluate(animation, seconds,
                                variable -> variable.endsWith("holding_flat") ? 1 : BendsAnimator.context(m, null).resolve(variable)), 1000, 1), 1000);
                            String where = base + " " + name + " pitch=" + pitch + " yaw=" + yaw + " frame=" + step;
                            for (ItemBone item : new ItemBone[] {model.rightItem, model.leftItem}) {
                                assertEquals(0, item.rotation.getY(), 1e-3, where);
                                assertEquals(0, item.rotation.getZ(), 1e-3, where);
                            }
                            // Item pixels -> head pixels.
                            Matrix4f head = new Matrix4f().scale(16).mul(model.geometry().transform(baked, "head", 1 / 16f).invert())
                                .mul(model.geometry().transform(baked, "rightItem", 1 / 16f))
                                .translate(model.rightItem.fistPivot(false, model.isSlim(), new Vector3f()).div(16)).scale(1 / 16f);
                            for (int i = 0; i < texels.size(); i += 8) assertClear(head, texels.subList(i, i + 8), where + " flat item");
                        }
                    }
                }
            }
        }
    }

    /** An item bone turn pivots a flat item at the fist; the strokes' blade tilt (8 to 102 degrees) makes a wide sweep. */
    @Test
    void aTurnedFlatItemStaysInTheFist() throws Exception {
        Vector3f iconCenter = FLAT.transformPosition(new Vector3f(8, 8, -0.5f));
        for (BakedAnimations file : files()) {
            String base = file.getAnimation("animation.mobends.player.swim") != null ? "swim" : "dive";
            Animation animation = file.getAnimation("animation.mobends.player." + base + "_holding");
            for (String name : new String[] {"player", "player_slim"}) {
                BakedGeoModel baked = BendsGeometryTest.load(name);
                ModelBendsPlayer model = (ModelBendsPlayer)BendsGeometryTest.models().get(name);
                float nearest = Float.POSITIVE_INFINITY, farthest = 0, leastTurn = Float.POSITIVE_INFINITY, mostTurn = 0;
                for (int step = 0; step < 48; ++step) {
                    double seconds = animation.length() * step / 48;
                    AnimationBaking.frame(model, m -> BendsAnimator.apply(m, animation,
                        AnimationProcessor.evaluate(animation, seconds, BendsAnimator.context(m, null)), 1000, 1), 1000);
                    leastTurn = Math.min(leastTurn, model.rightItem.rotation.getX());
                    mostTurn = Math.max(mostTurn, model.rightItem.rotation.getX());
                    // Native pixels: the held item as drawn, and the fist, the forearm's far 4 px.
                    Matrix4f item = new Matrix4f().scale(16).mul(model.geometry().transform(baked, "rightItem", 1 / 16f))
                        .translate(model.rightItem.fistPivot(false, model.isSlim(), new Vector3f()).div(16)).scale(1 / 16f);
                    float distance = item.transformPosition(new Vector3f(iconCenter)).distance(fist(model, baked));
                    nearest = Math.min(nearest, distance);
                    farthest = Math.max(farthest, distance);
                }
                assertTrue(mostTurn - leastTurn > 80, base + " sweeps too little to test: " + leastTurn + " .. " + mostTurn);
                // A flat item is held by its edge, its middle about 5 px from the fist, whatever the turn.
                assertTrue(farthest - nearest < 0.1f && farthest < 6, base + " " + name + " flat item leaves the fist: "
                    + nearest + " .. " + farthest + " px");
            }
        }
    }

    /** A flat item's placement from the grip, in pixels: RenderBendsPlayer's, then ItemRenderer's for an equipped icon. */
    private static final Matrix4f FLAT = new Matrix4f().translate(-1, -2, 1).translate(4, 3, -3).scale(0.375f)
        .rotateZ((float)Math.toRadians(60)).rotateX((float)Math.toRadians(-90)).rotateZ((float)Math.toRadians(20))
        .translate(0, -0.3f * 16, 0).scale(1.5f).rotateY((float)Math.toRadians(50)).rotateZ((float)Math.toRadians(335))
        .translate(-0.9375f * 16, -0.0625f * 16, 0);

    /** The center of the right forearm's far 4 px, in native pixels, from its drawn skin cube (wide or slim). */
    private static Vector3f fist(ModelBendsPlayer model, BakedGeoModel baked) {
        Matrix4f forearm = new Matrix4f().scale(16).mul(model.geometry().transform(baked, "rightForeArm", 1 / 16f)).scale(1 / 16f);
        Matrix4f toForearm = forearm.invert(new Matrix4f());
        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY), max = new Vector3f(Float.NEGATIVE_INFINITY);
        // Salamander's model blocks -> native pixels (X/Y mirrored, Y from the neck) -> the forearm's frame.
        GeoModelRenderer.render(baked, model.geometry().pose(baked),
            bone -> bone.name().equals("rightForeArm_layer0") || bone.name().equals("rightForeArm_layer1"),
            (x, y, z, u, v, nx, ny, nz, r, g, b, a) -> {
                Vector3f p = toForearm.transformPosition(new Vector3f(-x * 16, 24 - y * 16, z * 16));
                min.min(p);
                max.max(p);
            }, 1, 1, 1, 1);
        // The hand is the end away from the elbow, at the bottom of the hanging arm.
        return forearm.transformPosition(new Vector3f((min.x + max.x) / 2, max.y - 2, (min.z + max.z) / 2));
    }

    @Test
    void emptyAndHoldingHandsClearTheHeadWithAFoldedPullAndExtendedReach() throws Exception {
        for (BakedAnimations file : files()) {
            String base = file.getAnimation("animation.mobends.player.swim") != null ? "swim" : "dive";
            for (String suffix : new String[] {"", "_holding"}) {
                Animation animation = file.getAnimation("animation.mobends.player." + base + suffix);
                float mostBent = 0, mostExtended = -180;
                for (int step = 0; step < 120; ++step) {
                    float elbow = (float)Math.toDegrees(-AnimationProcessor.evaluate(animation,
                        animation.length() * step / 120, variable -> 0).get("rightForeArm").getRotX());
                    mostBent = Math.min(mostBent, elbow);
                    mostExtended = Math.max(mostExtended, elbow);
                }
                assertTrue(mostBent < -35 && mostExtended > -5, animation.name() + " needs a folded pull and extended reach");
                for (boolean slim : new boolean[] {false, true}) {
                    ModelBendsPlayer model = new ModelBendsPlayer();
                    model.ssb$set64x();
                    model.ssb$setSlim(slim);
                    BakedGeoModel rig = rig(model);
                    // The hand occupies the distal half of the forearm, including its sleeve.
                    List<Vector3f> fist = new ArrayList<>();
                    for (int corner = 0; corner < 8; ++corner) fist.add(new Vector3f(
                        (corner & 1) == 0 ? -0.25f : (slim ? 3.25f : 4.25f),
                        (corner & 2) == 0 ? 3 : 6.25f, (corner & 4) == 0 ? -4.25f : 0.25f));
                    for (float pitch : new float[] {-85, -45, 0, 45, 85}) {
                        for (float yaw : new float[] {-75, 0, 75}) {
                            for (int step = 0; step < 120; ++step) {
                                GeoRenderTransforms pose = frames(rig, animation, animation.length() * step / 120, pitch, yaw);
                                for (String bone : new String[] {"rightForeArm", "leftForeArm"}) {
                                    Matrix4f hand = new Matrix4f().scale(-16, -16, 16)
                                        .mul(pose.getBone("head").get().modelSpaceMatrix().invert())
                                        .mul(pose.getBone(bone).get().modelSpaceMatrix()).scale(-1 / 16f, -1 / 16f, 1 / 16f);
                                    assertClear(hand, fist, animation.name() + " " + bone + " slim=" + slim
                                        + " pitch=" + pitch + " yaw=" + yaw + " frame=" + step);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** Exact box/box separating-axis check for each extruded sword texel or the held block. */
    private static void assertClear(Matrix4f transform, List<Vector3f> box, String where) {
        Vector3f[] points = new Vector3f[8];
        for (int i = 0; i < 8; ++i) points[i] = transform.transformPosition(new Vector3f(box.get(i))).add(0, 4, 0);
        Vector3f[] headAxes = {new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1)};
        List<Vector3f> axes = new ArrayList<>();
        for (Vector3f axis : headAxes) axes.add(axis);
        for (int corner : new int[] {1, 2, 4}) {
            Vector3f edge = new Vector3f(points[corner]).sub(points[0]);
            axes.add(edge);
            for (Vector3f axis : headAxes) axes.add(new Vector3f(edge).cross(axis));
        }
        boolean intersects = true;
        for (Vector3f axis : axes) {
            if (axis.lengthSquared() < 1e-8f) continue;
            float min = Float.POSITIVE_INFINITY, max = Float.NEGATIVE_INFINITY;
            for (Vector3f point : points) { min = Math.min(min, point.dot(axis)); max = Math.max(max, point.dot(axis)); }
            float radius = 4.5f * (Math.abs(axis.x) + Math.abs(axis.y) + Math.abs(axis.z));
            if (min >= radius || max <= -radius) { intersects = false; break; }
        }
        assertFalse(intersects, where);
    }

    private static BakedAnimations[] files() throws Exception {
        return new BakedAnimations[] {AnimationBaking.load(SwimAnimationTest.FILE), AnimationBaking.load(DiveAnimationTest.FILE)};
    }

    private static BakedGeoModel rig(ModelBendsPlayer model) {
        return new GeckoLibGsonLoader().loadModel(new ResourceLocation("mobends", "geckolib/models/player.geo.json"),
            PlayerRig.exportGeometry(model, "geometry.holding_test"));
    }

    private static GeoRenderTransforms frames(BakedGeoModel rig, Animation animation, double seconds, float pitch, float yaw) {
        ModelPose pose = ModelPose.create(rig);
        pose.apply(AnimationProcessor.evaluate(animation, seconds, variable -> {
            if (variable.equals("query.mobends_head_pitch")) return pitch;
            if (variable.equals("query.mobends_head_yaw")) return yaw;
            return 0;
        }), false);
        return GeoModelRenderer.captureTransforms(rig, pose);
    }
}
