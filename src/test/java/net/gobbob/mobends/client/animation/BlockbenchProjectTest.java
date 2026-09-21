package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.renderer.base.GeoModelRenderer;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/** The in-game Blockbench export: preview items where the game draws held items, one atlas, the right arms. */
class BlockbenchProjectTest {
    /** The game's held-item chains from the grip (RenderBendsPlayer.renderHeldItem), independently in JOML. */
    private static final Matrix4f SWORD = new Matrix4f()
        .translate(-1, -2, 1)
        .translate(0, 3, 0).scale(0.625f, -0.625f, 0.625f).rotateX(rad(-100)).rotateY(rad(45))
        .translate(0, -0.3f * 16, 0).scale(1.5f).rotateY(rad(50)).rotateZ(rad(335)).translate(-0.9375f * 16, -0.0625f * 16, 0);
    private static final Matrix4f BLOCK = new Matrix4f()
        .translate(-1, -2, 1)
        .translate(0, 3, -5).rotateX(rad(20)).rotateY(rad(45)).scale(-0.375f, -0.375f, 0.375f);

    @Test
    void affineMatchesTheGlChain() {
        Random random = new Random(3);
        for (int i = 0; i < 50; ++i) {
            float x = random.nextFloat() * 32 - 8, y = random.nextFloat() * 32 - 8, z = random.nextFloat() * 32 - 8;
            assertClose(SWORD.transformPosition(new Vector3f(x, y, z)), BlockbenchExport.SWORD.transform(x, y, z));
            assertClose(BLOCK.transformPosition(new Vector3f(x, y, z)), BlockbenchExport.BLOCK.transform(x, y, z));
        }
    }

    /** Baked by Salamander, every sword texel's cube and the grass block sit where the game draws them. */
    @Test
    void previewItemsAreWhereTheGameDrawsThem() {
        BufferedImage sword = testSword();
        for (boolean slim : new boolean[] {false, true}) {
            float[] grip = PlayerRig.restPivot(model(slim), Bone.RIGHT_ITEM);
            List<JsonObject> cubes = new ArrayList<JsonObject>(BlockbenchExport.swordCubes(grip, sword));
            cubes.add(BlockbenchExport.grassCube(grip));
            List<Vector3f> vertices = bake(grip, cubes);
            Matrix4f hand = new Matrix4f().translate(grip[0], grip[1], grip[2]);
            List<Vector3f> expected = new ArrayList<Vector3f>();
            for (int v = 0; v < 16; ++v) {
                for (int u = 0; u < 16; ++u) {
                    if ((sword.getRGB(u, v) >>> 24) == 0) continue;
                    for (int corner = 0; corner < 8; ++corner) {
                        expected.add(new Matrix4f(hand).mul(SWORD).transformPosition(new Vector3f(
                            15 - u + (corner & 1), 15 - v + ((corner >> 1) & 1), -((corner >> 2) & 1))));
                    }
                }
            }
            for (int corner = 0; corner < 8; ++corner) {
                expected.add(new Matrix4f(hand).mul(BLOCK).transformPosition(new Vector3f(
                    (corner & 1) == 0 ? -8 : 8, (corner & 2) == 0 ? -8 : 8, (corner & 4) == 0 ? -8 : 8)));
            }
            for (Vector3f want : expected) {
                boolean found = false;
                for (Vector3f got : vertices) found |= got.distance(want) < 0.002f;
                assertTrue(found, (slim ? "slim: " : "classic: ") + "no preview vertex at " + want);
            }
        }
    }

    @Test
    void projectUsesOneAtlasWithTheSkinOnTop() throws Exception {
        BlockbenchExport.Textures textures = new BlockbenchExport.Textures();
        textures.skin = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        textures.skin.setRGB(9, 9, 0xFF123456);
        textures.sword = testSword();
        textures.grassTop = solid(0xFF00FF00);
        textures.grassSide = solid(0xFF00AA00);
        textures.dirt = solid(0xFF884400);
        JsonObject project = BlockbenchExport.project(model(false), "player", textures);
        assertEquals(1, project.getAsJsonArray("textures").size(), "Bedrock projects have one texture");
        assertEquals(128, project.getAsJsonObject("resolution").get("height").getAsInt());
        BufferedImage atlas = BlockbenchExport.atlas(textures);
        assertEquals(0xFF123456, atlas.getRGB(9, 9), "the skin, unchanged");
        assertEquals(textures.sword.getRGB(10, 5), atlas.getRGB(BlockbenchExport.SWORD_U + 10, BlockbenchExport.ITEM_ROW + 5));
        assertEquals(0xFF884400, atlas.getRGB(BlockbenchExport.DIRT_U + 3, BlockbenchExport.ITEM_ROW + 3));

        int texels = 0;
        for (int v = 0; v < 16; ++v) for (int u = 0; u < 16; ++u) if ((textures.sword.getRGB(u, v) >>> 24) != 0) ++texels;
        int rigCubes = 0;
        JsonArray bones = PlayerRig.exportGeometry(model(false), "g").getAsJsonArray("minecraft:geometry").get(0)
            .getAsJsonObject().getAsJsonArray("bones");
        for (int i = 0; i < bones.size(); ++i) {
            if (bones.get(i).getAsJsonObject().has("cubes")) rigCubes += bones.get(i).getAsJsonObject().getAsJsonArray("cubes").size();
        }
        // A sword and a grass block under each of rightItem and rightItemFree.
        assertEquals(rigCubes + 2 * (texels + 1), project.getAsJsonArray("elements").size());
    }

    @Test
    void detectsSlimSkins() {
        BufferedImage classic = solid64(0xFF808080);
        BufferedImage slim = solid64(0xFF808080);
        for (int y = 20; y < 32; ++y) {
            slim.setRGB(54, y, 0);
            slim.setRGB(55, y, 0);
        }
        assertFalse(BlockbenchExport.isSlimSkin(classic));
        assertTrue(BlockbenchExport.isSlimSkin(slim));
    }

    private static List<Vector3f> bake(float[] grip, List<JsonObject> cubes) {
        JsonObject description = new JsonObject();
        description.addProperty("identifier", "geometry.preview");
        description.addProperty("texture_width", 64);
        description.addProperty("texture_height", 128);
        JsonObject bone = new JsonObject();
        bone.addProperty("name", "rightItem");
        JsonArray pivot = new JsonArray();
        pivot.add(new com.google.gson.JsonPrimitive(grip[0]));
        pivot.add(new com.google.gson.JsonPrimitive(24 - grip[1]));
        pivot.add(new com.google.gson.JsonPrimitive(grip[2]));
        bone.add("pivot", pivot);
        JsonArray cubeArray = new JsonArray();
        for (JsonObject cube : cubes) cubeArray.add(cube);
        bone.add("cubes", cubeArray);
        JsonArray bones = new JsonArray();
        bones.add(bone);
        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);
        JsonObject geo = new JsonObject();
        geo.addProperty("format_version", "1.12.0");
        geo.add("minecraft:geometry", geometries);
        BakedGeoModel baked = new GeckoLibGsonLoader().loadModel(new ResourceLocation("mobends", "geckolib/models/p.geo.json"), geo);
        List<Vector3f> vertices = new ArrayList<Vector3f>();
        GeoModelRenderer.render(baked, ModelPose.create(baked), (x, y, z, u, v, nx, ny, nz, r, g, b, a) ->
            vertices.add(new Vector3f(-x * 16, 24 - y * 16, z * 16)), 1, 1, 1, 1);
        return vertices;
    }

    /** A sword-like icon: a diagonal from the bottom left to the top right with a guard. */
    private static BufferedImage testSword() {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 16; ++i) image.setRGB(i, 15 - i, 0xFFC0C0C0);
        image.setRGB(2, 11, 0xFF303030);
        image.setRGB(4, 13, 0xFF303030);
        return image;
    }

    private static BufferedImage solid(int argb) {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) for (int x = 0; x < 16; ++x) image.setRGB(x, y, argb);
        return image;
    }

    private static BufferedImage solid64(int argb) {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; ++y) for (int x = 0; x < 64; ++x) image.setRGB(x, y, argb);
        return image;
    }

    private static ModelBendsPlayer model(boolean slim) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.ssb$set64x();
        model.ssb$setSlim(slim);
        model.prepareModernOverlays();
        return model;
    }

    private static void assertClose(Vector3f expected, double[] actual) {
        assertEquals(expected.x, actual[0], 1e-3);
        assertEquals(expected.y, actual[1], 1e-3);
        assertEquals(expected.z, actual[2], 1e-3);
    }

    private static float rad(float degrees) {
        return (float)Math.toRadians(degrees);
    }
}
