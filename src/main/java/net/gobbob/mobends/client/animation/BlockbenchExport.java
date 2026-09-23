package net.gobbob.mobends.client.animation;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import net.gobbob.mobends.client.animation.PlayerRig.Bone;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.client.render.BlinkingTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.ColorizerGrass;

/**
 * Writes a Blockbench project of the player for animating: the rig with the player's own skin, plus preview items
 * (an iron sword and a grass block) built 1:1 from the game's textures, placed exactly where the game draws held
 * items. The preview items hang under {@code rightItem} and {@code rightItemFree} and are toggled in the outliner;
 * the game only ever reads exported animations.
 *
 * <p>Blockbench's Bedrock format has one texture per model, so everything shares one 64 x 128 atlas: the skin on top,
 * the item textures below.
 */
public final class BlockbenchExport {
    static final int ITEM_ROW = 64;
    static final int SWORD_U = 0, GRASS_TOP_U = 16, GRASS_SIDE_U = 32, DIRT_U = 48;

    /** The textures the preview items are made of, 16 x 16 each; the grass top already tinted. */
    public static final class Textures {
        public BufferedImage skin, sword, grassTop, grassSide, dirt;
    }

    private BlockbenchExport() {}

    /** Exports the local player; {@code slim} null detects the arm width from the skin. */
    public static File export(AbstractClientPlayer player, Boolean slim) throws IOException {
        Textures textures = new Textures();
        textures.skin = BlinkingTextures.copyPlayerSkin(player);
        if (textures.skin == null) throw new IOException("the skin isn't loaded yet");
        textures.sword = gameTexture("textures/items/iron_sword.png");
        int grass = ColorizerGrass.getGrassColor(0.5, 1.0);
        textures.grassTop = tint(gameTexture("textures/blocks/grass_top.png"), grass);
        textures.grassSide = over(gameTexture("textures/blocks/grass_side.png"),
            tint(gameTexture("textures/blocks/grass_side_overlay.png"), grass));
        textures.dirt = gameTexture("textures/blocks/dirt.png");
        boolean slimArms = slim != null ? slim : isSlimSkin(textures.skin);
        String name = slimArms ? "player_slim" : "player";

        ModelBendsPlayer model = new ModelBendsPlayer();
        model.ssb$set64x();
        model.ssb$setSlim(slimArms);
        model.prepareModernOverlays();
        File file = new File(Minecraft.getMinecraft().mcDataDir, "mobends/blockbench/" + name + ".bbmodel");
        file.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(project(model, name, textures), writer);
        }
        return file;
    }

    /** Slim (3 px) arms leave the fourth column of the right arm's front face empty. */
    static boolean isSlimSkin(BufferedImage skin) {
        return skin.getHeight() >= 64 && (skin.getRGB(54, 20) >>> 24) == 0 && (skin.getRGB(54, 31) >>> 24) == 0;
    }

    private static BufferedImage gameTexture(String path) throws IOException {
        try (InputStream in = Minecraft.getMinecraft().getResourceManager()
            .getResource(new ResourceLocation("minecraft", path)).getInputStream()) {
            return sixteen(ImageIO.read(in));
        }
    }

    /** First frame, scaled to 16 x 16 (resource packs may be larger or animated). */
    private static BufferedImage sixteen(BufferedImage image) {
        int size = image.getWidth();
        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) result.setRGB(x, y, image.getRGB(x * size / 16, y * size / 16));
        }
        return result;
    }

    private static BufferedImage tint(BufferedImage image, int rgb) {
        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int c = image.getRGB(x, y);
                int r = ((c >> 16) & 0xFF) * ((rgb >> 16) & 0xFF) / 255;
                int g = ((c >> 8) & 0xFF) * ((rgb >> 8) & 0xFF) / 255;
                int b = (c & 0xFF) * (rgb & 0xFF) / 255;
                result.setRGB(x, y, (c & 0xFF000000) | (r << 16) | (g << 8) | b);
            }
        }
        return result;
    }

    private static BufferedImage over(BufferedImage base, BufferedImage overlay) {
        BufferedImage result = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; ++y) {
            for (int x = 0; x < 16; ++x) {
                int top = overlay.getRGB(x, y);
                result.setRGB(x, y, (top >>> 24) > 0 ? top | 0xFF000000 : base.getRGB(x, y));
            }
        }
        return result;
    }

    // --- The game's held-item placement from the grip (RenderBendsPlayer.renderHeldItem), in model pixels -------

    /** A full-3D item (swords, tools): vanilla's placement, then ItemRenderer.renderItem's 16 px icon. */
    static final Affine SWORD = new Affine()
        .translate(-1, -2, 1)
        .translate(0, 3, 0).scale(0.625, -0.625, 0.625).rotateX(-100).rotateY(45)
        .translate(0, -0.3 * 16, 0).scale(1.5, 1.5, 1.5).rotateY(50).rotateZ(335).translate(-0.9375 * 16, -0.0625 * 16, 0);
    /** A 3D block item, drawn as a 16 px cube centred on this frame. */
    static final Affine BLOCK = new Affine()
        .translate(-1, -2, 1)
        .translate(0, 3, -5).rotateX(20).rotateY(45).scale(-0.375, -0.375, 0.375);

    /**
     * The sword as vanilla draws a held item: its icon extruded one icon pixel deep, one cube per opaque texel, each
     * face coloured by that texel. Icon (x, y, z) in [0,16]x[0,16]x[-1,0] maps through {@link #SWORD}, and texel
     * (u, v) covers icon x in [15 - u, 16 - u], y in [15 - v, 16 - v].
     */
    static List<JsonObject> swordCubes(float[] grip, BufferedImage sword) {
        double scale = Math.cbrt(Math.abs(SWORD.determinant()));
        // SWORD mirrors Y; cubes span icon (x, -y, z) under the remaining proper rotation.
        double[] rotation = SWORD.linear();
        for (int row = 0; row < 3; ++row) {
            rotation[row * 3 + 1] = -rotation[row * 3 + 1];
            for (int col = 0; col < 3; ++col) rotation[row * 3 + col] /= scale;
        }
        double[] pivot = SWORD.transform(0, 0, 0);
        float[] at = {(float)(pivot[0] + grip[0]), (float)(pivot[1] + grip[1]), (float)(pivot[2] + grip[2])};
        List<JsonObject> cubes = new ArrayList<JsonObject>();
        for (int v = 0; v < 16; ++v) {
            for (int u = 0; u < 16; ++u) {
                if ((sword.getRGB(u, v) >>> 24) == 0) continue;
                float x0 = 15 - u, y0 = 15 - v;
                JsonObject uv = new JsonObject();
                for (String face : new String[] {"north", "east", "south", "west", "up", "down"}) {
                    uv.add(face, face(SWORD_U + u, ITEM_ROW + v, 1, 1));
                }
                cubes.add(cube(at, new double[] {x0 * scale, -(y0 + 1) * scale, -scale},
                    new double[] {(x0 + 1) * scale, -y0 * scale, 0}, rotation, uv));
            }
        }
        return cubes;
    }

    /** The grass block: 16 px scaled by 0.375, its top (local +Y, the cube's "down" side in Bedrock) up. */
    static JsonObject grassCube(float[] grip) {
        double scale = Math.cbrt(Math.abs(BLOCK.determinant()));
        double[] rotation = BLOCK.linear();
        for (int i = 0; i < 9; ++i) rotation[i] /= scale;
        double[] pivot = BLOCK.transform(0, 0, 0);
        float[] at = {(float)(pivot[0] + grip[0]), (float)(pivot[1] + grip[1]), (float)(pivot[2] + grip[2])};
        JsonObject uv = new JsonObject();
        uv.add("down", face(GRASS_TOP_U, ITEM_ROW, 16, 16));
        uv.add("up", face(DIRT_U, ITEM_ROW, 16, 16));
        // Sides: grass edge toward the block's top, which is the cube's model +Y side, i.e. the texture flipped.
        for (String side : new String[] {"north", "south", "east", "west"}) uv.add(side, face(GRASS_SIDE_U, ITEM_ROW + 16, 16, -16));
        double half = 8 * scale;
        return cube(at, new double[] {-half, -half, -half}, new double[] {half, half, half}, rotation, uv);
    }

    /** A Bedrock cube spanning pivot + [min, max] in model space before its rotation about the pivot. */
    private static JsonObject cube(float[] pivot, double[] min, double[] max, double[] rotation, JsonObject uv) {
        float[] angles = EulerZYX.angles(rotation, new float[3]);
        JsonObject cube = new JsonObject();
        cube.add("origin", vector(pivot[0] + min[0], 24 - (pivot[1] + max[1]), pivot[2] + min[2]));
        cube.add("size", vector(max[0] - min[0], max[1] - min[1], max[2] - min[2]));
        cube.add("pivot", vector(pivot[0], 24 - pivot[1], pivot[2]));
        cube.add("rotation", vector(angles[0], angles[1], angles[2]));
        cube.add("uv", uv);
        return cube;
    }

    private static JsonObject face(double u, double v, double w, double h) {
        JsonObject face = new JsonObject();
        face.add("uv", array(u, v));
        face.add("uv_size", array(w, h));
        return face;
    }

    // --- The Blockbench project ------------------------------------------------------------------------------------

    static JsonObject project(ModelBendsPlayer model, String name, Textures textures) throws IOException {
        float[] grip = PlayerRig.restPivot(model, Bone.RIGHT_ITEM);
        JsonArray gripPivot = vector(grip[0], 24 - grip[1], grip[2]);
        return project(model, name, atlas(textures), "skin and preview items", PLAYER_PLACEHOLDERS, (groups, elements) -> {
            for (String itemBone : new String[] {"rightItem", "rightItemFree"}) {
                boolean inHand = itemBone.equals("rightItem");
                addPreview(groups.get(itemBone), elements, itemBone + " preview sword", gripPivot, swordCubes(grip, textures.sword), inHand);
                List<JsonObject> grass = new ArrayList<JsonObject>();
                grass.add(grassCube(grip));
                addPreview(groups.get(itemBone), elements, itemBone + " preview grass block", gripPivot, grass, false);
            }
        });
    }

    private static final String PLAYER_PLACEHOLDERS = "q.mobends_head_pitch = 0;\nq.mobends_head_yaw = 0;\nq.mobends_ticks = 0;\n"
        + "q.mobends_ticks_after_punch = 0;\nq.mobends_holding = 1;\nq.mobends_offhand_holding = 0;\nq.mobends_travel_pitch = 0;";
    private static final String MOB_PLACEHOLDERS = "q.mobends_head_pitch = 0;\nq.mobends_head_yaw = 0;\nq.mobends_ticks = 0;";

    /** Exports a mob's rig with its vanilla texture, e.g. {@code zombie} from textures/entity/zombie/zombie.png. */
    public static File exportMob(String name, AnimatedModel model, String texture) throws IOException {
        File file = new File(Minecraft.getMinecraft().mcDataDir, "mobends/blockbench/" + name + ".bbmodel");
        file.getParentFile().mkdirs();
        JsonObject project = project(model, name, gameTexture(texture), name, MOB_PLACEHOLDERS, (groups, elements) -> {});
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting().create().toJson(project, writer);
        }
        return file;
    }

    /** A Bedrock project of a rig with one texture; {@code extras} adds cubes to its groups (bone name to group). */
    static JsonObject project(AnimatedModel model, String name, BufferedImage image, String textureName, String placeholders,
        java.util.function.BiConsumer<Map<String, JsonObject>, JsonArray> extras) throws IOException {
        JsonObject geometry = PlayerRig.exportGeometry(model, "geometry.mobends." + name)
            .getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
        JsonArray elements = new JsonArray();
        Map<String, JsonObject> groups = new LinkedHashMap<String, JsonObject>();
        JsonArray outliner = new JsonArray();
        for (JsonElement boneElement : geometry.getAsJsonArray("bones")) {
            JsonObject bone = boneElement.getAsJsonObject();
            String id = bone.get("name").getAsString();
            JsonArray pivot = bone.getAsJsonArray("pivot");
            JsonObject group = group(id, id, pivot, true);
            groups.put(id, group);
            if (bone.has("parent")) groups.get(bone.get("parent").getAsString()).getAsJsonArray("children").add(group);
            else outliner.add(group);
            if (!bone.has("cubes")) continue;
            int index = 0;
            for (JsonElement cube : bone.getAsJsonArray("cubes")) {
                JsonObject element = element(id, id + " " + index++, cube.getAsJsonObject(), true);
                elements.add(element);
                group.getAsJsonArray("children").add(element.get("uuid"));
            }
        }
        extras.accept(groups, elements);

        JsonObject meta = new JsonObject();
        meta.addProperty("format_version", "4.10");
        meta.addProperty("model_format", "bedrock");
        meta.addProperty("box_uv", false);
        JsonObject resolution = new JsonObject();
        resolution.addProperty("width", image.getWidth());
        resolution.addProperty("height", image.getHeight());
        JsonArray textureList = new JsonArray();
        textureList.add(texture(image, textureName));

        JsonObject project = new JsonObject();
        project.add("meta", meta);
        project.addProperty("name", name);
        project.addProperty("model_identifier", "mobends." + name);
        project.add("visible_box", vector(3, 3, 1.5));
        project.addProperty("animation_variable_placeholders", placeholders);
        project.add("resolution", resolution);
        project.add("elements", elements);
        project.add("outliner", outliner);
        project.add("textures", textureList);
        return project;
    }

    /** The skin (64 x 64, legacy 64 x 32 skins in its top half) above the 16 px item textures. */
    static BufferedImage atlas(Textures textures) {
        BufferedImage atlas = new BufferedImage(64, 128, BufferedImage.TYPE_INT_ARGB);
        BufferedImage skin = textures.skin;
        for (int y = 0; y < Math.min(64, skin.getHeight()); ++y) {
            for (int x = 0; x < Math.min(64, skin.getWidth()); ++x) atlas.setRGB(x, y, skin.getRGB(x, y));
        }
        int[] columns = {SWORD_U, GRASS_TOP_U, GRASS_SIDE_U, DIRT_U};
        BufferedImage[] items = {textures.sword, textures.grassTop, textures.grassSide, textures.dirt};
        for (int i = 0; i < items.length; ++i) {
            for (int y = 0; y < 16; ++y) {
                for (int x = 0; x < 16; ++x) atlas.setRGB(columns[i] + x, ITEM_ROW + y, items[i].getRGB(x, y));
            }
        }
        return atlas;
    }

    private static void addPreview(JsonObject parent, JsonArray elements, String key, JsonArray pivot,
        List<JsonObject> cubes, boolean visible) {
        String name = key.substring(key.indexOf(' ') + 1);
        JsonObject group = group(name, key, pivot, visible);
        int index = 0;
        for (JsonObject cube : cubes) {
            JsonObject element = element(name, key + " " + index++, cube, visible);
            elements.add(element);
            group.getAsJsonArray("children").add(element.get("uuid"));
        }
        parent.getAsJsonArray("children").add(group);
    }

    /** A bone as a Blockbench group; Blockbench stores Bedrock X negated. */
    private static JsonObject group(String name, String key, JsonArray bedrockPivot, boolean visible) {
        JsonObject group = new JsonObject();
        group.addProperty("name", name);
        group.add("origin", vector(-bedrockPivot.get(0).getAsDouble(), bedrockPivot.get(1).getAsDouble(),
            bedrockPivot.get(2).getAsDouble()));
        group.addProperty("color", 0);
        group.addProperty("uuid", uuid(key + " group"));
        group.addProperty("export", true);
        group.addProperty("isOpen", !name.startsWith("preview"));
        group.addProperty("locked", false);
        group.addProperty("visibility", visible);
        group.addProperty("autouv", 0);
        group.add("children", new JsonArray());
        return group;
    }

    /**
     * A Bedrock cube as Blockbench imports one: X negated (from.x = -(origin.x + size.x)), rotation X and Y negated,
     * per-face UVs as [u, v, u + w, v + h] with up and down stored reversed, unlisted faces left out.
     */
    private static JsonObject element(String name, String key, JsonObject cube, boolean visible) {
        double[] origin = doubles(cube.getAsJsonArray("origin"));
        double[] size = doubles(cube.getAsJsonArray("size"));
        JsonObject element = new JsonObject();
        element.addProperty("name", name);
        element.addProperty("box_uv", false);
        element.addProperty("rescale", false);
        element.addProperty("locked", false);
        element.addProperty("visibility", visible);
        element.add("from", vector(-(origin[0] + size[0]), origin[1], origin[2]));
        element.add("to", vector(-origin[0], origin[1] + size[1], origin[2] + size[2]));
        element.addProperty("autouv", 0);
        element.addProperty("color", 0);
        if (cube.has("pivot")) {
            double[] pivot = doubles(cube.getAsJsonArray("pivot"));
            double[] rotation = doubles(cube.getAsJsonArray("rotation"));
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
                double[] at = doubles(uv.getAsJsonObject(face).getAsJsonArray("uv"));
                double[] extent = doubles(uv.getAsJsonObject(face).getAsJsonArray("uv_size"));
                boolean reversed = face.equals("up") || face.equals("down");
                out.add("uv", reversed
                    ? array(at[0] + extent[0], at[1] + extent[1], at[0], at[1])
                    : array(at[0], at[1], at[0] + extent[0], at[1] + extent[1]));
                out.addProperty("texture", 0);
            } else {
                out.add("uv", array(0, 0, 0, 0));
                out.add("texture", JsonNull.INSTANCE);
            }
            faces.add(face, out);
        }
        element.add("faces", faces);
        element.addProperty("type", "cube");
        element.addProperty("uuid", uuid(key));
        return element;
    }

    private static JsonObject texture(BufferedImage image, String name) throws IOException {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);
        JsonObject texture = new JsonObject();
        texture.addProperty("path", "");
        texture.addProperty("name", name);
        texture.addProperty("folder", "");
        texture.addProperty("namespace", "");
        texture.addProperty("id", "0");
        texture.addProperty("width", image.getWidth());
        texture.addProperty("height", image.getHeight());
        texture.addProperty("uv_width", image.getWidth());
        texture.addProperty("uv_height", image.getHeight());
        texture.addProperty("particle", false);
        texture.addProperty("render_mode", "default");
        texture.addProperty("visible", true);
        texture.addProperty("mode", "bitmap");
        texture.addProperty("saved", false);
        texture.addProperty("uuid", uuid("texture"));
        texture.addProperty("source", "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png.toByteArray()));
        return texture;
    }

    private static String uuid(String name) {
        return UUID.nameUUIDFromBytes(("mobends " + name).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static double[] doubles(JsonArray array) {
        double[] values = new double[array.size()];
        for (int i = 0; i < values.length; ++i) values[i] = array.get(i).getAsDouble();
        return values;
    }

    private static JsonArray vector(double x, double y, double z) {
        return array(x, y, z);
    }

    private static JsonArray array(double... values) {
        JsonArray array = new JsonArray();
        for (double value : values) {
            double rounded = Math.round(value * 10000.0) / 10000.0;
            if (rounded == 0) rounded = 0; // no -0
            array.add(rounded == Math.rint(rounded) ? new JsonPrimitive((long)rounded) : new JsonPrimitive(rounded));
        }
        return array;
    }

    /** A minimal 3D affine transform (row-major 3x4) for the held-item chains, applied like GL: post-multiplied. */
    static final class Affine {
        final double[] m = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0};

        Affine translate(double x, double y, double z) {
            for (int row = 0; row < 3; ++row) m[row * 4 + 3] += m[row * 4] * x + m[row * 4 + 1] * y + m[row * 4 + 2] * z;
            return this;
        }

        Affine scale(double x, double y, double z) {
            for (int row = 0; row < 3; ++row) {
                m[row * 4] *= x;
                m[row * 4 + 1] *= y;
                m[row * 4 + 2] *= z;
            }
            return this;
        }

        Affine rotateX(double degrees) {
            return rotate(EulerZYX.matrix(degrees, 0, 0));
        }

        Affine rotateY(double degrees) {
            return rotate(EulerZYX.matrix(0, degrees, 0));
        }

        Affine rotateZ(double degrees) {
            return rotate(EulerZYX.matrix(0, 0, degrees));
        }

        private Affine rotate(double[] r) {
            for (int row = 0; row < 3; ++row) {
                double a = m[row * 4], b = m[row * 4 + 1], c = m[row * 4 + 2];
                for (int col = 0; col < 3; ++col) m[row * 4 + col] = a * r[col] + b * r[3 + col] + c * r[6 + col];
            }
            return this;
        }

        double[] linear() {
            return new double[] {m[0], m[1], m[2], m[4], m[5], m[6], m[8], m[9], m[10]};
        }

        double determinant() {
            double[] l = linear();
            return l[0] * (l[4] * l[8] - l[5] * l[7]) - l[1] * (l[3] * l[8] - l[5] * l[6]) + l[2] * (l[3] * l[7] - l[4] * l[6]);
        }

        double[] transform(double x, double y, double z) {
            return new double[] {
                m[0] * x + m[1] * y + m[2] * z + m[3],
                m[4] * x + m[5] * y + m[6] * z + m[7],
                m[8] * x + m[9] * y + m[10] * z + m[11]};
        }
    }
}
