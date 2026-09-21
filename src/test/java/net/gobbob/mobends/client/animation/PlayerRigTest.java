package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.renderer.base.GeoModelRenderer;
import com.geckolib.renderer.base.GeoRenderTransforms;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.ModelBoxBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

/**
 * Proves the Blockbench rig is the in-game model: Salamander's baked and posed mesh must match the
 * Mo' Bends parts vertex for vertex, UVs included.
 */
class PlayerRigTest {
    private static final Path RIG_DIR = Paths.get("blockbench");
    private static final float DEG = (float)(Math.PI / 180.0);

    @Test
    void committedRigFilesAreUpToDate() throws IOException {
        boolean update = "1".equals(System.getenv("MOBENDS_UPDATE_RIG"));
        for (boolean slim : new boolean[] {false, true}) {
            String name = slim ? "player_slim.geo.json" : "player.geo.json";
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(export(slim)) + "\n";
            Path file = RIG_DIR.resolve(name);
            if (update) {
                Files.createDirectories(RIG_DIR);
                Files.write(file, json.getBytes(StandardCharsets.UTF_8));
            }
            String committed = Files.exists(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
            assertEquals(committed, json, name + " is stale; regenerate with MOBENDS_UPDATE_RIG=1 ./gradlew test");
        }
    }

    @Test
    void restMeshMatchesModel() {
        for (boolean slim : new boolean[] {false, true}) {
            assertSameMesh(slim, new EnumMap<Bone, float[]>(Bone.class), true);
        }
    }

    @Test
    void posedMeshMatchesModel() {
        Random random = new Random(1234);
        for (boolean slim : new boolean[] {false, true}) {
            for (int trial = 0; trial < 25; ++trial) {
                Map<Bone, float[]> rotations = new EnumMap<Bone, float[]>(Bone.class);
                Map<Bone, float[]> translations = new EnumMap<Bone, float[]>(Bone.class);
                for (Bone bone : Bone.values()) {
                    rotations.put(bone, new float[] {
                        random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180});
                    translations.put(bone, new float[] {
                        random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8});
                }
                assertSameMesh(slim, rotations, translations, true);
            }
        }
    }

    /** Aim bones turn the whole arm about the shoulder; the arm's own pose (here a twist) applies inside it. */
    @Test
    void aimedArmMatchesModel() {
        Map<Bone, float[]> rotations = new EnumMap<Bone, float[]>(Bone.class);
        rotations.put(Bone.RIGHT_ARM_AIM, new float[] {35, 0, 0});
        rotations.put(Bone.RIGHT_ARM, new float[] {-90, 0, 120});
        rotations.put(Bone.LEFT_ARM_AIM, new float[] {-50, -30, 0});
        rotations.put(Bone.LEFT_ARM, new float[] {-90, 0, -120});
        rotations.put(Bone.LEFT_FORE_ARM, new float[] {-45, 0, 0});
        for (boolean slim : new boolean[] {false, true}) {
            assertSameMesh(slim, rotations, true);
        }
    }

    /**
     * The item bones are where the game draws held items: Salamander's frame for {@code rightItem} and
     * {@code rightItemFree} must equal RenderBendsPlayer's grip frame with the item bone applied, in any pose.
     */
    @Test
    void itemBonesMatchTheGameGrip() {
        Random random = new Random(99);
        for (boolean slim : new boolean[] {false, true}) {
            for (int trial = 0; trial < 40; ++trial) {
                ModelBendsPlayer model = model(slim);
                BakedGeoModel baked = new GeckoLibGsonLoader()
                    .loadModel(new ResourceLocation("mobends", "geckolib/models/player.geo.json"), export(slim));
                ModelPose pose = ModelPose.create(baked);
                Map<Bone, float[]> rotations = new EnumMap<Bone, float[]>(Bone.class);
                Map<Bone, float[]> translations = new EnumMap<Bone, float[]>(Bone.class);
                Map<Bone, float[]> scales = new EnumMap<Bone, float[]>(Bone.class);
                for (Bone bone : Bone.values()) {
                    float[] r = {random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180};
                    float[] t = {random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8, random.nextFloat() * 16 - 8};
                    rotations.put(bone, r);
                    translations.put(bone, t);
                    pose.get(bone.id).get().setRotation(-r[0] * DEG, -r[1] * DEG, r[2] * DEG).setTranslation(t[0], t[1], t[2]);
                    if (bone.holdingArm() != null) {
                        float[] k = {0.5f + random.nextFloat(), 0.5f + random.nextFloat(), 0.5f + random.nextFloat()};
                        scales.put(bone, k);
                        pose.get(bone.id).get().setScale(k[0], k[1], k[2]);
                    }
                }
                GeoRenderTransforms frames = GeoModelRenderer.captureTransforms(baked, pose);
                String context = (slim ? "slim" : "classic") + " trial " + trial + ": ";
                // root has its own pose here; the game applies root as the whole-body transform before the model,
                // so compare the item frames relative to root.
                Matrix4f fromRoot = frames.getBone(PlayerRig.ROOT).get().modelSpaceMatrix().invert();

                // RenderBendsPlayer: rightArm.postRender (body, arm, forearm, forearm pivot undone), 9 px down.
                Matrix4f hand = partFrame(model, Bone.BODY, rotations, translations);
                hand.mul(partFrame(model, Bone.RIGHT_ARM, rotations, translations));
                hand.mul(partFrame(model, Bone.RIGHT_FORE_ARM, rotations, translations));
                ModelRenderer fore = model.bipedRightForeArm;
                hand.translate(-fore.rotationPointX, -fore.rotationPointY, -fore.rotationPointZ).translate(0, PlayerRig.GRIP_DROP, 0);
                hand.mul(itemFrame(rotations.get(Bone.RIGHT_ITEM), translations.get(Bone.RIGHT_ITEM), scales.get(Bone.RIGHT_ITEM)));
                assertSameFrame(hand, new Matrix4f(fromRoot).mul(frames.getBone("rightItem").get().modelSpaceMatrix()), context + "rightItem");

                // The free item starts at the resting grip in the model's root frame.
                float[] grip = PlayerRig.restPivot(model, Bone.RIGHT_ITEM_FREE);
                Matrix4f free = new Matrix4f().translate(grip[0], grip[1], grip[2]);
                free.mul(itemFrame(rotations.get(Bone.RIGHT_ITEM_FREE), translations.get(Bone.RIGHT_ITEM_FREE), scales.get(Bone.RIGHT_ITEM_FREE)));
                assertSameFrame(free, new Matrix4f(fromRoot).mul(frames.getBone("rightItemFree").get().modelSpaceMatrix()),
                    context + "rightItemFree");
            }
        }
    }

    /** A part's transform relative to its parent part in the game: animated pivot, then rotation (aims folded). */
    private static Matrix4f partFrame(ModelBendsPlayer model, Bone bone, Map<Bone, float[]> rotations,
        Map<Bone, float[]> translations) {
        ModelRenderer part = bone.part(model);
        float[] r = rotations.get(bone);
        float[] t = modelOffset(translations.get(bone));
        if (bone.aim() != null) {
            float[] a = rotations.get(bone.aim());
            double[] aim = EulerZYX.matrix(a[0], a[1], a[2]);
            r = EulerZYX.angles(EulerZYX.multiply(aim, EulerZYX.matrix(r[0], r[1], r[2])), r);
            float[] aimOffset = modelOffset(translations.get(bone.aim()));
            float[] armOffset = t;
            t = new float[3];
            for (int row = 0; row < 3; ++row) {
                t[row] = aimOffset[row]
                    + (float)(aim[row * 3] * armOffset[0] + aim[row * 3 + 1] * armOffset[1] + aim[row * 3 + 2] * armOffset[2]);
            }
        }
        return new Matrix4f().translate(part.rotationPointX + t[0], part.rotationPointY + t[1], part.rotationPointZ + t[2])
            .rotateZ(r[2] * DEG).rotateY(r[1] * DEG).rotateX(r[0] * DEG);
    }

    /** ItemBone.apply: translate, rotate Z, Y, X, scale. */
    private static Matrix4f itemFrame(float[] r, float[] bedrockOffset, float[] k) {
        float[] t = modelOffset(bedrockOffset);
        return new Matrix4f().translate(t[0], t[1], t[2]).rotateZ(r[2] * DEG).rotateY(r[1] * DEG).rotateX(r[0] * DEG)
            .scale(k[0], k[1], k[2]);
    }

    /**
     * Compares a model-space frame (pixels, Y down) with a Salamander frame (blocks, Y up, X mirrored). The frame's
     * position maps like any point; its local axes only change units and direction, since they start at the pivot.
     */
    private static void assertSameFrame(Matrix4f expected, Matrix4f salamander, String context) {
        Matrix4f toModel = new Matrix4f().translate(0, 24, 0).scale(-16, -16, 16);
        Matrix4f localAxes = new Matrix4f().scale(-16, -16, 16);
        Matrix4f actual = new Matrix4f(toModel).mul(salamander).mul(localAxes.invert());
        if (!expected.equals(actual, 0.002f)) throw new AssertionError(context + "\nexpected\n" + expected + "actual\n" + actual);
    }

    /** A detached hand: the forearm moved away from the elbow while the arm is raised. */
    @Test
    void movedForearmMatchesModel() {
        Map<Bone, float[]> rotations = new EnumMap<Bone, float[]>(Bone.class);
        rotations.put(Bone.RIGHT_ARM_AIM, new float[] {20, 0, 0});
        rotations.put(Bone.RIGHT_ARM, new float[] {-90, 0, 120});
        Map<Bone, float[]> translations = new EnumMap<Bone, float[]>(Bone.class);
        translations.put(Bone.RIGHT_FORE_ARM, new float[] {0, 0, 11});
        translations.put(Bone.LEFT_ARM_AIM, new float[] {1, 2, 3});
        translations.put(Bone.LEFT_ARM, new float[] {0, -3, 0});
        for (boolean slim : new boolean[] {false, true}) {
            assertSameMesh(slim, rotations, translations, true);
        }
    }

    /** A single bent elbow and knee, the case the rig exists for. */
    @Test
    void bentForearmMatchesModel() {
        Map<Bone, float[]> rotations = new EnumMap<Bone, float[]>(Bone.class);
        rotations.put(Bone.RIGHT_ARM, new float[] {-90, 0, 20});
        rotations.put(Bone.RIGHT_FORE_ARM, new float[] {-80, 0, 0});
        rotations.put(Bone.LEFT_FORE_LEG, new float[] {45, 0, 0});
        for (boolean slim : new boolean[] {false, true}) {
            assertSameMesh(slim, rotations, true);
        }
    }

    private static JsonObject export(boolean slim) {
        return PlayerRig.exportGeometry(model(slim), slim ? "geometry.mobends.player_slim" : "geometry.mobends.player");
    }

    private static ModelBendsPlayer model(boolean slim) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.ssb$set64x();
        model.ssb$setSlim(slim);
        model.prepareModernOverlays();
        return model;
    }

    private static void assertSameMesh(boolean slim, Map<Bone, float[]> rotations, boolean compareUvs) {
        assertSameMesh(slim, rotations, new EnumMap<Bone, float[]>(Bone.class), compareUvs);
    }

    /** Rotations in degrees and translations in Bedrock pixels, per bone. */
    private static void assertSameMesh(boolean slim, Map<Bone, float[]> rotations, Map<Bone, float[]> translations,
        boolean compareUvs) {
        ModelBendsPlayer model = model(slim);
        BakedGeoModel baked = new GeckoLibGsonLoader()
            .loadModel(new ResourceLocation("mobends", "geckolib/models/player.geo.json"), export(slim));
        ModelPose pose = ModelPose.create(baked);
        for (Map.Entry<Bone, float[]> entry : rotations.entrySet()) {
            float[] r = entry.getValue();
            // Salamander stores Bedrock rotations with X and Y negated, in radians.
            pose.get(entry.getKey().id).get().setRotation(-r[0] * DEG, -r[1] * DEG, r[2] * DEG);
        }
        for (Map.Entry<Bone, float[]> entry : translations.entrySet()) {
            float[] t = entry.getValue();
            pose.get(entry.getKey().id).get().setTranslation(t[0], t[1], t[2]);
        }

        List<float[][]> expected = new ArrayList<float[][]>();
        Map<Bone, Matrix4f> matrices = new EnumMap<Bone, Matrix4f>(Bone.class);
        for (Bone bone : Bone.values()) {
            ModelRenderer part = bone.part(model);
            if (part == null) continue; // Aim bones have no part; they are folded into their arm.
            Bone parent = bone.parentBone();
            if (parent != null && parent.part(model) == null) parent = parent.parentBone();
            Matrix4f matrix = new Matrix4f(parent == null ? new Matrix4f() : matrices.get(parent));
            float[] r = rotations.containsKey(bone) ? rotations.get(bone) : new float[3];
            // Bedrock (x, y, z) pixels is a model-space offset (x, -y, z) of the pivot.
            float[] t = modelOffset(translations.get(bone));
            if (bone.aim() != null) {
                float[] a = rotations.containsKey(bone.aim()) ? rotations.get(bone.aim()) : new float[3];
                double[] aim = EulerZYX.matrix(a[0], a[1], a[2]);
                r = EulerZYX.angles(EulerZYX.multiply(aim, EulerZYX.matrix(r[0], r[1], r[2])), r);
                float[] aimOffset = modelOffset(translations.get(bone.aim()));
                float[] armOffset = t;
                t = new float[3];
                for (int row = 0; row < 3; ++row) {
                    t[row] = aimOffset[row]
                        + (float)(aim[row * 3] * armOffset[0] + aim[row * 3 + 1] * armOffset[1] + aim[row * 3 + 2] * armOffset[2]);
                }
            }
            // ModelRendererBends: translate to the animated pivot, then rotate Z, Y, X.
            matrix.translate(part.rotationPointX + t[0], part.rotationPointY + t[1], part.rotationPointZ + t[2])
                .rotateZ(r[2] * DEG).rotateY(r[1] * DEG).rotateX(r[0] * DEG);
            matrices.put(bone, matrix);
            collectQuads(model, part, matrix, expected);
        }

        final List<float[][]> actual = new ArrayList<float[][]>();
        final List<float[]> quad = new ArrayList<float[]>();
        GeoModelRenderer.render(baked, pose, (x, y, z, u, v, nx, ny, nz, red, green, blue, alpha) -> {
            // Salamander space (blocks, Y up, X mirrored, feet origin) -> model space (pixels).
            quad.add(new float[] {-x * 16, 24 - y * 16, z * 16, u * 64, v * 64});
            if (quad.size() == 4) {
                actual.add(quad.toArray(new float[4][]));
                quad.clear();
            }
        }, 1, 1, 1, 1);

        String context = (slim ? "slim" : "classic") + " mesh, rotations " + describe(rotations);
        assertEquals(expected.size(), actual.size(), context + ": quad count");
        List<float[][]> unmatched = new ArrayList<float[][]>(actual);
        for (float[][] want : expected) {
            float[][] match = null;
            for (float[][] candidate : unmatched) {
                if (sameQuad(want, candidate, compareUvs)) {
                    match = candidate;
                    break;
                }
            }
            if (match == null) throw new AssertionError(context + ": no Salamander quad matches " + describe(want));
            unmatched.remove(match);
        }
    }

    /**
     * Same four vertices in any order, positions within 0.002 px and UVs within 0.002 texels. Folding an aim bone
     * into its arm near gimbal lock costs a little float precision, still far below a visible difference.
     */
    private static boolean sameQuad(float[][] a, float[][] b, boolean uvs) {
        boolean[] used = new boolean[b.length];
        for (float[] va : a) {
            boolean found = false;
            for (int i = 0; i < b.length && !found; ++i) {
                if (used[i]) continue;
                boolean close = true;
                for (int c = 0; c < (uvs ? 5 : 3); ++c) {
                    if (Math.abs(va[c] - b[i][c]) > 0.002f) close = false;
                }
                if (close) {
                    used[i] = true;
                    found = true;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private static String describe(float[][] quad) {
        StringBuilder text = new StringBuilder();
        for (float[] v : quad) {
            text.append(String.format(Locale.ROOT, "(%.3f %.3f %.3f | %.3f %.3f) ", v[0], v[1], v[2], v[3], v[4]));
        }
        return text.toString();
    }

    private static float[] modelOffset(float[] bedrock) {
        return bedrock == null ? new float[3] : new float[] {bedrock[0], -bedrock[1], bedrock[2]};
    }

    private static void collectQuads(ModelBendsPlayer model, ModelRenderer part, Matrix4f matrix, List<float[][]> out) {
        if (!part.showModel || part.isHidden) return;
        for (Object object : part.cubeList) {
            ModelBoxBends box = (ModelBoxBends)object;
            for (int i = 0; i < box.quads.length; ++i) {
                if (!box.isQuadVisible(i)) continue;
                List<float[]> quad = new ArrayList<float[]>();
                for (PositionTextureVertex vertex : box.quads[i].vertexPositions) {
                    Vector4f p = matrix.transform(new Vector4f(
                        (float)vertex.vector3D.xCoord, (float)vertex.vector3D.yCoord, (float)vertex.vector3D.zCoord, 1));
                    quad.add(new float[] {p.x, p.y, p.z,
                        vertex.texturePositionX * part.textureWidth, vertex.texturePositionY * part.textureHeight});
                }
                out.add(quad.toArray(new float[4][]));
            }
        }
        if (part.childModels == null) return;
        for (Object child : part.childModels) {
            ModelRenderer childPart = (ModelRenderer)child;
            if (isAnyBone(model, childPart)) continue;
            Matrix4f childMatrix = new Matrix4f(matrix)
                .translate(childPart.rotationPointX, childPart.rotationPointY, childPart.rotationPointZ);
            collectQuads(model, childPart, childMatrix, out);
        }
    }

    private static boolean isAnyBone(ModelBendsPlayer model, ModelRenderer part) {
        for (Bone bone : Bone.values()) {
            if (bone.part(model) == part) return true;
        }
        return false;
    }

    private static String describe(Map<Bone, float[]> rotations) {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<Bone, float[]> entry : rotations.entrySet()) {
            float[] r = entry.getValue();
            text.append(String.format(Locale.ROOT, "%s=[%.1f %.1f %.1f] ", entry.getKey().id, r[0], r[1], r[2]));
        }
        return text.toString();
    }
}
