package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.geckolib.animation.state.ModelPose;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.renderer.base.GeoModelRenderer;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

/**
 * Builds the Blockbench projects {@code blockbench/player.bbmodel} and {@code player_slim.bbmodel}: the rig plus
 * preview items at their true in-game size and place, to toggle while animating. Regenerate with
 * {@code MOBENDS_UPDATE_RIG=1 ./gradlew test}.
 */
class BlockbenchProjectTest {
    private static final Path DIR = Paths.get("blockbench");
    private static final int SKIN = 0, ITEMS = 1;

    @Test
    void committedProjectsAreUpToDate() throws IOException {
        boolean update = "1".equals(System.getenv("MOBENDS_UPDATE_RIG"));
        for (boolean slim : new boolean[] {false, true}) {
            String name = slim ? "player_slim.bbmodel" : "player.bbmodel";
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(project(slim)) + "\n";
            Path file = DIR.resolve(name);
            if (update) Files.write(file, json.getBytes(StandardCharsets.UTF_8));
            String committed = Files.exists(file) ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
            assertEquals(committed, json, name + " is stale; regenerate with MOBENDS_UPDATE_RIG=1 ./gradlew test");
        }
    }

    /** The preview items, as Bedrock cubes baked by Salamander, sit exactly where the game draws held items. */
    @Test
    void previewItemsAreWhereTheGameDrawsThem() {
        for (boolean slim : new boolean[] {false, true}) {
            ModelBendsPlayer model = model(slim);
            float[] grip = PlayerRig.restPivot(model, Bone.RIGHT_ITEM);
            JsonObject geo = new JsonObject();
            JsonObject description = new JsonObject();
            description.addProperty("identifier", "geometry.preview");
            description.addProperty("texture_width", 64);
            description.addProperty("texture_height", 64);
            JsonObject bone = new JsonObject();
            bone.addProperty("name", "rightItem");
            bone.add("pivot", vector(grip[0], 24 - grip[1], grip[2]));
            JsonArray cubes = new JsonArray();
            cubes.add(swordCube(grip));
            cubes.add(grassCube(grip));
            bone.add("cubes", cubes);
            JsonArray bones = new JsonArray();
            bones.add(bone);
            JsonObject geometry = new JsonObject();
            geometry.add("description", description);
            geometry.add("bones", bones);
            JsonArray geometries = new JsonArray();
            geometries.add(geometry);
            geo.addProperty("format_version", "1.12.0");
            geo.add("minecraft:geometry", geometries);
            BakedGeoModel baked = new GeckoLibGsonLoader().loadModel(new ResourceLocation("mobends", "geckolib/models/p.geo.json"), geo);

            List<Vector3f> vertices = new ArrayList<Vector3f>();
            GeoModelRenderer.render(baked, ModelPose.create(baked), (x, y, z, u, v, nx, ny, nz, r, g, b, a) ->
                vertices.add(new Vector3f(-x * 16, 24 - y * 16, z * 16)), 1, 1, 1, 1);

            // The game's own chain from the grip: the sword's icon corners and the block's corners.
            Matrix4f hand = new Matrix4f().translate(grip[0], grip[1], grip[2]);
            List<Vector3f> expected = new ArrayList<Vector3f>();
            for (float[] corner : new float[][] {{0, 0, 0}, {16, 0, 0}, {16, 16, 0}, {0, 16, 0}, {0, 0, -1}, {16, 16, -1}}) {
                expected.add(new Matrix4f(hand).mul(SWORD).transformPosition(new Vector3f(corner[0], corner[1], corner[2])));
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

    // --- The game's held-item placement, from the grip (RenderBendsPlayer.renderHeldItem), in model pixels -------

    /** A full-3D item (swords, tools): vanilla's placement, then ItemRenderer.renderItem's 2D icon, 16 px square. */
    static final Matrix4f SWORD = new Matrix4f()
        .translate(-1, -2, 1)
        .translate(0, 3, 0).scale(0.625f, -0.625f, 0.625f).rotateX(rad(-100)).rotateY(rad(45))
        .translate(0, -0.3f * 16, 0).scale(1.5f).rotateY(rad(50)).rotateZ(rad(335)).translate(-0.9375f * 16, -0.0625f * 16, 0);
    /** A 3D block item, drawn as a 16 px cube centred on this frame. */
    static final Matrix4f BLOCK = new Matrix4f()
        .translate(-1, -2, 1)
        .translate(0, 3, -5).rotateX(rad(20)).rotateY(rad(45)).scale(-0.375f, -0.375f, 0.375f);

    private static float rad(float degrees) {
        return (float)Math.toRadians(degrees);
    }

    /**
     * The sword icon as a thin Bedrock cube: its corners are icon (x, y, z) in [0,16]x[0,16]x[-1,0] under
     * {@link #SWORD}. That matrix mirrors Y, so the cube spans (x, -y, z) under a proper rotation, and the texture
     * is laid on accordingly (vanilla draws icon x as texture u = 16 - x, y as v = 16 - y).
     */
    private static JsonObject swordCube(float[] grip) {
        Matrix3f linear = SWORD.get3x3(new Matrix3f());
        float scale = (float)Math.cbrt(Math.abs(linear.determinant()));
        Matrix3f rotation = new Matrix3f(linear).scale(1, -1, 1).scale(1 / scale);
        Vector3f pivot = SWORD.transformPosition(new Vector3f()).add(grip[0], grip[1], grip[2]);
        JsonObject uv = new JsonObject();
        uv.add("south", face(0, 0, 16, 16));
        uv.add("north", face(16, 0, -16, 16));
        return cube(pivot, new float[] {0, -16 * scale, -scale}, new float[] {16 * scale, 0, 0}, rotation, uv);
    }

    /** The grass block: a 6 px cube (16 px scaled by 0.375), top up, sides with their grass edge at the top. */
    private static JsonObject grassCube(float[] grip) {
        Matrix3f linear = BLOCK.get3x3(new Matrix3f());
        float scale = (float)Math.cbrt(Math.abs(linear.determinant()));
        Matrix3f rotation = new Matrix3f(linear).scale(1 / scale);
        Vector3f pivot = BLOCK.transformPosition(new Vector3f()).add(grip[0], grip[1], grip[2]);
        JsonObject uv = new JsonObject();
        // The block's top (its local +Y) is the cube's model +Y side, which Bedrock calls "down".
        uv.add("down", face(16, 0, 16, 16));
        uv.add("up", face(48, 0, 16, 16));
        for (String side : new String[] {"north", "south", "east", "west"}) uv.add(side, face(32, 16, 16, -16));
        float half = 8 * scale;
        return cube(pivot, new float[] {-half, -half, -half}, new float[] {half, half, half}, rotation, uv);
    }

    /** A Bedrock cube spanning pivot + [min, max] in model space before its rotation about the pivot. */
    private static JsonObject cube(Vector3f pivot, float[] min, float[] max, Matrix3f rotation, JsonObject uv) {
        double[] m = {rotation.m00(), rotation.m10(), rotation.m20(), rotation.m01(), rotation.m11(), rotation.m21(),
            rotation.m02(), rotation.m12(), rotation.m22()};
        float[] angles = EulerZYX.angles(m, new float[3]);
        JsonObject cube = new JsonObject();
        cube.add("origin", vector(pivot.x + min[0], 24 - (pivot.y + max[1]), pivot.z + min[2]));
        cube.add("size", vector(max[0] - min[0], max[1] - min[1], max[2] - min[2]));
        cube.add("pivot", vector(pivot.x, 24 - pivot.y, pivot.z));
        cube.add("rotation", vector(angles[0], angles[1], angles[2]));
        cube.add("uv", uv);
        return cube;
    }

    private static JsonObject face(float u, float v, float w, float h) {
        JsonObject face = new JsonObject();
        face.add("uv", pair(u, v));
        face.add("uv_size", pair(w, h));
        return face;
    }

    // --- The Blockbench project ------------------------------------------------------------------------------------

    private static JsonObject project(boolean slim) throws IOException {
        ModelBendsPlayer model = model(slim);
        String name = slim ? "player_slim" : "player";
        JsonObject geometry = PlayerRig.exportGeometry(model, "geometry.mobends." + name)
            .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();

        JsonArray elements = new JsonArray();
        Map<String, JsonObject> groups = new LinkedHashMap<String, JsonObject>();
        List<JsonObject> topLevel = new ArrayList<JsonObject>();
        for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
            JsonObject bone = boneElement.getAsJsonObject();
            String id = bone.get("name").getAsString();
            JsonObject group = group(id, bone.getAsJsonArray("pivot"), true);
            groups.put(id, group);
            if (bone.has("parent")) groups.get(bone.get("parent").getAsString()).getAsJsonArray("children").add(group);
            else topLevel.add(group);
            if (!bone.has("cubes")) continue;
            int index = 0;
            for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                JsonObject element = element(id, id + " " + index++, cube.getAsJsonObject(), SKIN);
                elements.add(element);
                group.getAsJsonArray("children").add(element.get("uuid"));
            }
        }
        float[] grip = PlayerRig.restPivot(model, Bone.RIGHT_ITEM);
        for (String itemBone : new String[] {"rightItem", "rightItemFree"}) {
            boolean inHand = itemBone.equals("rightItem");
            addPreview(groups.get(itemBone), elements, itemBone + " preview sword", swordCube(grip), inHand);
            addPreview(groups.get(itemBone), elements, itemBone + " preview grass block", grassCube(grip), false);
        }

        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", "4.10");
        meta.addProperty("model_format", "bedrock");
        meta.addProperty("box_uv", false);
        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", 64);
        resolution.addProperty("height", 64);
        JsonArray outliner = new JsonArray();
        for (JsonObject group : topLevel) outliner.add(group);
        JsonArray textures = new JsonArray();
        textures.add(texture("skin (replace with yours)", SKIN, placeholderSkin()));
        textures.add(texture("preview items", ITEMS, previewItems()));

        JsonObject project = new JsonObject();
        project.add("meta", meta);
        project.addProperty("name", name);
        project.addProperty("model_identifier", "mobends." + name);
        project.add("visible_box", vector(3, 3, 1.5f));
        project.addProperty("animation_variable_placeholders",
            "q.mobends_head_pitch = 0;\nq.mobends_head_yaw = 0;\nq.mobends_ticks = 0;\nq.mobends_ticks_after_punch = 0;");
        project.add("resolution", resolution);
        project.add("elements", elements);
        project.add("outliner", outliner);
        project.add("textures", textures);
        return project;
    }

    private static void addPreview(JsonObject parent, JsonArray elements, String name, JsonObject cube, boolean visible) {
        // Same pivot as the item bone; group() takes Bedrock coordinates, the parent holds Blockbench ones (X negated).
        float[] origin = floats(parent.getAsJsonArray("origin"));
        JsonObject group = group(name.substring(name.indexOf(' ') + 1), vector(-origin[0], origin[1], origin[2]), visible);
        group.addProperty("uuid", uuid(name + " group"));
        JsonObject element = element(name.substring(name.indexOf(' ') + 1), name, cube, ITEMS);
        element.addProperty("visibility", visible);
        elements.add(element);
        group.getAsJsonArray("children").add(element.get("uuid"));
        parent.getAsJsonArray("children").add(group);
    }

    /** A bone as a Blockbench group; Blockbench stores Bedrock X negated. */
    private static JsonObject group(String name, JsonArray bedrockPivot, boolean visible) {
        JsonObject group = new JsonObject();
        group.addProperty("name", name);
        group.add("origin", vector(-bedrockPivot.get(0).getAsFloat(), bedrockPivot.get(1).getAsFloat(), bedrockPivot.get(2).getAsFloat()));
        group.addProperty("color", 0);
        group.addProperty("uuid", uuid(name));
        group.addProperty("export", true);
        group.addProperty("isOpen", true);
        group.addProperty("locked", false);
        group.addProperty("visibility", visible);
        group.addProperty("autouv", 0);
        group.add("children", new JsonArray());
        return group;
    }

    /**
     * A Bedrock cube as a Blockbench element, the way Blockbench imports one: X negated (so from.x = -(origin.x +
     * size.x)), rotation X and Y negated, per-face UVs as [u, v, u + w, v + h] with up and down stored reversed, and
     * faces the cube doesn't list left untextured.
     */
    private static JsonObject element(String name, String key, JsonObject cube, int texture) {
        float[] origin = floats(cube.getAsJsonArray("origin"));
        float[] size = floats(cube.getAsJsonArray("size"));
        JsonObject element = new JsonObject();
        element.addProperty("name", name);
        element.addProperty("box_uv", false);
        element.addProperty("rescale", false);
        element.addProperty("locked", false);
        element.add("from", vector(-(origin[0] + size[0]), origin[1], origin[2]));
        element.add("to", vector(-origin[0], origin[1] + size[1], origin[2] + size[2]));
        element.addProperty("autouv", 0);
        element.addProperty("color", texture == ITEMS ? 2 : 0);
        if (cube.has("pivot")) {
            float[] pivot = floats(cube.getAsJsonArray("pivot"));
            float[] rotation = floats(cube.getAsJsonArray("rotation"));
            element.add("origin", vector(-pivot[0], pivot[1], pivot[2]));
            element.add("rotation", vector(-rotation[0], -rotation[1], rotation[2]));
        } else {
            element.add("origin", vector(0, 0, 0));
        }
        JsonObject uv = cube.getAsJsonObject("uv");
        JsonObject faces = new JsonObject();
        for (String face : new String[] {"north", "east", "south", "west", "up", "down"}) {
            JsonObject out = new JsonObject();
            if (uv.has(face)) {
                float[] at = floats(uv.getAsJsonObject(face).getAsJsonArray("uv"));
                float[] extent = floats(uv.getAsJsonObject(face).getAsJsonArray("uv_size"));
                boolean reversed = face.equals("up") || face.equals("down");
                out.add("uv", reversed
                    ? quad(at[0] + extent[0], at[1] + extent[1], at[0], at[1])
                    : quad(at[0], at[1], at[0] + extent[0], at[1] + extent[1]));
                out.addProperty("texture", texture);
            } else {
                out.add("uv", quad(0, 0, 0, 0));
                out.add("texture", JsonNull.INSTANCE);
            }
            faces.add(face, out);
        }
        element.add("faces", faces);
        element.addProperty("type", "cube");
        element.addProperty("uuid", uuid(key));
        return element;
    }

    private static JsonObject texture(String name, int id, BufferedImage image) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        JsonObject texture = new JsonObject();
        texture.addProperty("path", "");
        texture.addProperty("name", name);
        texture.addProperty("folder", "");
        texture.addProperty("namespace", "");
        texture.addProperty("id", Integer.toString(id));
        texture.addProperty("width", 64);
        texture.addProperty("height", 64);
        texture.addProperty("uv_width", 64);
        texture.addProperty("uv_height", 64);
        texture.addProperty("particle", false);
        texture.addProperty("render_mode", "default");
        texture.addProperty("visible", true);
        texture.addProperty("mode", "bitmap");
        texture.addProperty("saved", false);
        texture.addProperty("uuid", uuid("texture " + id));
        texture.addProperty("source", "data:image/png;base64," + Base64.getEncoder().encodeToString(png.toByteArray()));
        return texture;
    }

    /** A plain grey skin so the rig is visible until you drop your own onto it. */
    private static BufferedImage placeholderSkin() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; ++y) {
            for (int x = 0; x < 64; ++x) {
                boolean overlay = (y < 16 && x >= 32) || (y >= 32 && y < 48 && x < 56) || (y >= 48 && (x < 16 || x >= 48));
                if (!overlay) image.setRGB(x, y, ((x + y) & 1) == 0 ? 0xFFB0B0B0 : 0xFFA4A4A4);
            }
        }
        return image;
    }

    /** Simple stand-in art: a sword icon, grass top, grass side and dirt, each 16 px, along the top row. */
    private static BufferedImage previewItems() {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        // Sword, drawn like an item icon: handle at the bottom left, blade to the top right.
        for (int i = 0; i < 16; ++i) {
            int x = i, y = 15 - i;
            if (i < 3) paint(image, x, y, 0xFF6B4A2A);            // handle
            else if (i == 3) paint(image, x, y, 0xFF3A3A3A);      // pommel of the guard
            else {
                paint(image, x, y, 0xFFE6E6F0);                    // blade
                if (x + 1 < 16) paint(image, x + 1, y, 0xFFB8B8C8);
            }
        }
        for (int[] guard : new int[][] {{2, 11}, {4, 13}, {1, 10}, {5, 14}}) paint(image, guard[0], guard[1], 0xFF3A3A3A);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                boolean speck = ((x * 7 + y * 13) % 5) == 0;
                paint(image, 16 + x, y, speck ? 0xFF4F9A2E : 0xFF5DB036);                       // grass top
                paint(image, 32 + x, y, y < 3 + (x % 3 == 0 ? 1 : 0) ? 0xFF5DB036 : (speck ? 0xFF6E4B2E : 0xFF7A5635)); // side
                paint(image, 48 + x, y, speck ? 0xFF6E4B2E : 0xFF7A5635);                        // dirt
            }
        }
        return image;
    }

    private static void paint(BufferedImage image, int x, int y, int argb) {
        if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) image.setRGB(x, y, argb);
    }

    private static ModelBendsPlayer model(boolean slim) {
        ModelBendsPlayer model = new ModelBendsPlayer();
        model.ssb$set64x();
        model.ssb$setSlim(slim);
        model.prepareModernOverlays();
        return model;
    }

    private static String uuid(String name) {
        return UUID.nameUUIDFromBytes(("mobends " + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static float[] floats(JsonArray array) {
        float[] values = new float[array.size()];
        for (int i = 0; i < values.length; ++i) values[i] = array.get(i).getAsFloat();
        return values;
    }

    private static JsonArray vector(float x, float y, float z) {
        JsonArray array = new JsonArray();
        array.add(number(x));
        array.add(number(y));
        array.add(number(z));
        return array;
    }

    private static JsonArray pair(float a, float b) {
        JsonArray array = new JsonArray();
        array.add(number(a));
        array.add(number(b));
        return array;
    }

    private static JsonArray quad(float a, float b, float c, float d) {
        JsonArray array = new JsonArray();
        for (float value : new float[] {a, b, c, d}) array.add(number(value));
        return array;
    }

    private static JsonPrimitive number(float value) {
        double rounded = Math.round(value * 10000.0) / 10000.0;
        if (rounded == 0) rounded = 0; // no -0
        if (rounded == Math.rint(rounded)) return new JsonPrimitive((long)rounded);
        return new JsonPrimitive(rounded);
    }
}
