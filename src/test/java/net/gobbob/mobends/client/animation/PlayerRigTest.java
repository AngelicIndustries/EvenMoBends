package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.renderer.base.GeoModelRenderer;
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
                for (Bone bone : Bone.values()) {
                    rotations.put(bone, new float[] {
                        random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180, random.nextFloat() * 360 - 180});
                }
                assertSameMesh(slim, rotations, true);
            }
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
        ModelBendsPlayer model = model(slim);
        BakedGeoModel baked = new GeckoLibGsonLoader()
            .loadModel(new ResourceLocation("mobends", "geckolib/models/player.geo.json"), export(slim));
        ModelPose pose = ModelPose.create(baked);
        for (Map.Entry<Bone, float[]> entry : rotations.entrySet()) {
            float[] r = entry.getValue();
            // Salamander stores Bedrock rotations with X and Y negated, in radians.
            pose.get(entry.getKey().id).get().setRotation(-r[0] * DEG, -r[1] * DEG, r[2] * DEG);
        }

        List<float[][]> expected = new ArrayList<float[][]>();
        Map<Bone, Matrix4f> matrices = new EnumMap<Bone, Matrix4f>(Bone.class);
        for (Bone bone : Bone.values()) {
            Bone parent = bone.parentBone();
            Matrix4f matrix = new Matrix4f(parent == null ? new Matrix4f() : matrices.get(parent));
            ModelRenderer part = bone.part(model);
            float[] r = rotations.containsKey(bone) ? rotations.get(bone) : new float[3];
            // ModelRendererBends: translate to the rotation point, then rotate Z, Y, X.
            matrix.translate(part.rotationPointX, part.rotationPointY, part.rotationPointZ)
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

    /** Same four vertices in any order, positions within 0.001 px and UVs within 0.001 texels. */
    private static boolean sameQuad(float[][] a, float[][] b, boolean uvs) {
        boolean[] used = new boolean[b.length];
        for (float[] va : a) {
            boolean found = false;
            for (int i = 0; i < b.length && !found; ++i) {
                if (used[i]) continue;
                boolean close = true;
                for (int c = 0; c < (uvs ? 5 : 3); ++c) {
                    if (Math.abs(va[c] - b[i][c]) > 0.001f) close = false;
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
