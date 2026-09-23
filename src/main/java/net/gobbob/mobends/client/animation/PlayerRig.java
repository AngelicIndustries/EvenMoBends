package net.gobbob.mobends.client.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.gobbob.mobends.client.model.ModelBoxBends;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.PositionTextureVertex;
import net.minecraft.client.model.TexturedQuad;

/**
 * The Blockbench/GeckoLib rig of the player: one bone per animated {@link ModelRendererBends} part.
 *
 * <p>Coordinates: a Mo' Bends model-space point {@code (x, y, z)} (Y down, origin at the neck) is the Bedrock
 * point {@code (x, 24 - y, z)} (Y up, origin at the feet). Bedrock bone rotations equal
 * {@link ModelRendererBends#rotation} in degrees, applied in the same Z, Y, X order.
 *
 * <p>Aim bones ({@code rightArmAim}, {@code leftArmAim}, {@code neck}) have no geometry and no part of their own.
 * They sit at the pivot between the body and the arm or head, so an animation can turn the whole limb (follow the
 * look, swim sideways, tilt the head back) outside its own pose. They drive the part's
 * {@link ModelRendererBends#aim} rotation.
 *
 * <p>Item bones have no geometry either. {@code rightItem}/{@code leftItem} hang from the forearm at the grip, where
 * the held item is drawn; {@code rightItemFree}/{@code leftItemFree} hang from {@code root} at the same point at
 * rest, for items that leave the hand (thrown, caught). The item is drawn at each of its bones whose scale isn't 0.
 */
public final class PlayerRig {
    public static final String ROOT = "root";
    /** The grip (where held items are drawn) is this far down the arm from the shoulder pivot, in pixels. */
    public static final float GRIP_DROP = 9.0f;

    /** Bones in parent-before-child order. */
    public enum Bone {
        BODY("body", ROOT),
        NECK("neck", "body"),
        HEAD("head", "neck"),
        RIGHT_ARM_AIM("rightArmAim", "body"),
        RIGHT_ARM("rightArm", "rightArmAim"),
        RIGHT_FORE_ARM("rightForeArm", "rightArm"),
        LEFT_ARM_AIM("leftArmAim", "body"),
        LEFT_ARM("leftArm", "leftArmAim"),
        LEFT_FORE_ARM("leftForeArm", "leftArm"),
        RIGHT_ITEM("rightItem", "rightForeArm"),
        LEFT_ITEM("leftItem", "leftForeArm"),
        RIGHT_ITEM_FREE("rightItemFree", ROOT),
        LEFT_ITEM_FREE("leftItemFree", ROOT),
        RIGHT_LEG("rightLeg", ROOT),
        RIGHT_FORE_LEG("rightForeLeg", "rightLeg"),
        LEFT_LEG("leftLeg", ROOT),
        LEFT_FORE_LEG("leftForeLeg", "leftLeg");

        public final String id;
        public final String parent;

        Bone(String id, String parent) {
            this.id = id;
            this.parent = parent;
        }

        /** The model part this bone drives, or null for an aim or item bone. */
        public ModelRendererBends part(BipedRigModel rig) {
            ModelBiped model = rig.biped();
            switch (this) {
                case NECK:
                case RIGHT_ARM_AIM:
                case LEFT_ARM_AIM:
                case RIGHT_ITEM:
                case LEFT_ITEM:
                case RIGHT_ITEM_FREE:
                case LEFT_ITEM_FREE: return null;
                case BODY: return (ModelRendererBends)model.bipedBody;
                case HEAD: return (ModelRendererBends)model.bipedHead;
                case RIGHT_ARM: return (ModelRendererBends)model.bipedRightArm;
                case RIGHT_FORE_ARM: return (ModelRendererBends)rig.foreArm(true);
                case LEFT_ARM: return (ModelRendererBends)model.bipedLeftArm;
                case LEFT_FORE_ARM: return (ModelRendererBends)rig.foreArm(false);
                case RIGHT_LEG: return (ModelRendererBends)model.bipedRightLeg;
                case RIGHT_FORE_LEG: return (ModelRendererBends)rig.foreLeg(true);
                case LEFT_LEG: return (ModelRendererBends)model.bipedLeftLeg;
                case LEFT_FORE_LEG: return (ModelRendererBends)rig.foreLeg(false);
                default: throw new AssertionError(this);
            }
        }

        /** The part an aim bone turns, or null for other bones. */
        public Bone aimed() {
            switch (this) {
                case NECK: return HEAD;
                case RIGHT_ARM_AIM: return RIGHT_ARM;
                case LEFT_ARM_AIM: return LEFT_ARM;
                default: return null;
            }
        }

        /** The arm whose hand an item bone belongs to, or null for other bones. */
        public Bone holdingArm() {
            switch (this) {
                case RIGHT_ITEM:
                case RIGHT_ITEM_FREE: return RIGHT_ARM;
                case LEFT_ITEM:
                case LEFT_ITEM_FREE: return LEFT_ARM;
                default: return null;
            }
        }

        /** The aim bone above this part, or null for parts without one. */
        public Bone aim() {
            switch (this) {
                case HEAD: return NECK;
                case RIGHT_ARM: return RIGHT_ARM_AIM;
                case LEFT_ARM: return LEFT_ARM_AIM;
                default: return null;
            }
        }

        public static Bone byId(String id) {
            for (Bone bone : values()) {
                if (bone.id.equals(id)) return bone;
            }
            return null;
        }

        public Bone parentBone() {
            for (Bone bone : values()) {
                if (bone.id.equals(this.parent)) return bone;
            }
            return null;
        }
    }

    // ModelBoxBends quad index -> Bedrock face. Bedrock X is mirrored relative to model space.
    private static final String[] FACES = {"west", "east", "up", "down", "north", "south"};

    private PlayerRig() {}

    /** The pivot of a bone in model space at rest (Y down, pixels). */
    public static float[] restPivot(ModelBendsPlayer model, Bone bone) {
        return restPivot((AnimatedModel)model, bone.id);
    }

    /**
     * The pivot of any rig's bone in model space at rest (Y down, pixels): an aim bone at its part's pivot, an item
     * bone at its hand's grip, other bones where their parts' pivots add up.
     */
    public static float[] restPivot(AnimatedModel model, String bone) {
        if (model.rigAimed(bone) != null) return restPivot(model, model.rigAimed(bone));
        if (model.rigHoldingArm(bone) != null) {
            float[] grip = restPivot(model, model.rigHoldingArm(bone));
            grip[1] += GRIP_DROP;
            return grip;
        }
        float[] pivot = new float[3];
        for (String b = bone; b != null && !ROOT.equals(b); b = model.rigParent(b)) {
            ModelRenderer part = model.rigPart(b);
            if (part == null) continue;
            pivot[0] += part.rotationPointX;
            pivot[1] += part.rotationPointY;
            pivot[2] += part.rotationPointZ;
        }
        return pivot;
    }

    /** The player's geometry file (Bedrock 1.12), as Blockbench and GeckoLib read it. */
    public static JsonObject exportGeometry(ModelBendsPlayer model, String identifier) {
        return exportGeometry((AnimatedModel)model, identifier);
    }

    /** Any rig's geometry file (Bedrock 1.12): its bones with their pivots and each part's cubes. */
    public static JsonObject exportGeometry(AnimatedModel model, String identifier) {
        ModelRenderer anyPart = null;
        for (String bone : model.rigBones()) if (anyPart == null) anyPart = model.rigPart(bone);
        JsonObject description = new JsonObject();
        description.addProperty("identifier", identifier);
        description.addProperty("texture_width", (int)anyPart.textureWidth);
        description.addProperty("texture_height", (int)anyPart.textureHeight);
        description.addProperty("visible_bounds_width", 3);
        description.addProperty("visible_bounds_height", 3);
        description.add("visible_bounds_offset", vector(0, 1.5f, 0));

        JsonArray bones = new JsonArray();
        JsonObject root = new JsonObject();
        root.addProperty("name", ROOT);
        root.add("pivot", vector(0, 0, 0));
        bones.add(root);
        for (String bone : model.rigBones()) {
            float[] pivot = restPivot(model, bone);
            JsonObject json = new JsonObject();
            json.addProperty("name", bone);
            json.addProperty("parent", model.rigParent(bone));
            json.add("pivot", vector(pivot[0], 24 - pivot[1], pivot[2]));
            JsonArray cubes = new JsonArray();
            ModelRendererBends part = model.rigPart(bone);
            if (part != null) addCubes(cubes, part, pivot, model);
            json.add("cubes", cubes);
            bones.add(json);
        }

        JsonObject geometry = new JsonObject();
        geometry.add("description", description);
        geometry.add("bones", bones);
        JsonArray geometries = new JsonArray();
        geometries.add(geometry);
        JsonObject file = new JsonObject();
        file.addProperty("format_version", "1.12.0");
        file.add("minecraft:geometry", geometries);
        return file;
    }

    private static void addCubes(JsonArray cubes, ModelRenderer part, float[] origin, AnimatedModel model) {
        if (!part.showModel || part.isHidden) return;
        for (Object box : part.cubeList) {
            cubes.add(cube((ModelBoxBends)box, origin, part.textureWidth, part.textureHeight));
        }
        if (part.childModels == null) return;
        for (Object child : part.childModels) {
            ModelRenderer childPart = (ModelRenderer)child;
            if (isBone(model, childPart)) continue;
            float[] childOrigin = {
                origin[0] + childPart.rotationPointX,
                origin[1] + childPart.rotationPointY,
                origin[2] + childPart.rotationPointZ};
            addCubes(cubes, childPart, childOrigin, model);
        }
    }

    private static boolean isBone(AnimatedModel model, ModelRenderer part) {
        for (String bone : model.rigBones()) {
            if (model.rigPart(bone) == part) return true;
        }
        return false;
    }

    private static JsonObject cube(ModelBoxBends box, float[] origin, float textureWidth, float textureHeight) {
        float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        for (PositionTextureVertex vertex : box.vertices) {
            float[] p = {(float)vertex.vector3D.xCoord, (float)vertex.vector3D.yCoord, (float)vertex.vector3D.zCoord};
            for (int axis = 0; axis < 3; ++axis) {
                min[axis] = Math.min(min[axis], p[axis]);
                max[axis] = Math.max(max[axis], p[axis]);
            }
        }
        JsonObject cube = new JsonObject();
        cube.add("origin", vector(origin[0] + min[0], 24 - (origin[1] + max[1]), origin[2] + min[2]));
        cube.add("size", vector(max[0] - min[0], max[1] - min[1], max[2] - min[2]));

        JsonObject uv = new JsonObject();
        for (int quad = 0; quad < FACES.length; ++quad) {
            if (!box.isQuadVisible(quad)) continue;
            uv.add(FACES[quad], faceUv(box.quads[quad], FACES[quad], textureWidth, textureHeight));
        }
        cube.add("uv", uv);
        return cube;
    }

    private static JsonObject faceUv(TexturedQuad quad, String face, float textureWidth, float textureHeight) {
        float minU = Float.MAX_VALUE, minV = Float.MAX_VALUE, maxU = -Float.MAX_VALUE, maxV = -Float.MAX_VALUE;
        for (PositionTextureVertex vertex : quad.vertexPositions) {
            minU = Math.min(minU, vertex.texturePositionX * textureWidth);
            maxU = Math.max(maxU, vertex.texturePositionX * textureWidth);
            minV = Math.min(minV, vertex.texturePositionY * textureHeight);
            maxV = Math.max(maxV, vertex.texturePositionY * textureHeight);
        }
        JsonObject json = new JsonObject();
        // Bedrock's bottom face runs its V axis the other way, as in box UV (size.y = -depth).
        boolean down = face.equals("down");
        json.add("uv", pair(minU, down ? maxV : minV));
        json.add("uv_size", pair(maxU - minU, down ? minV - maxV : maxV - minV));
        return json;
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

    /** Rounds float noise (e.g. 4.0200005) so the files stay readable and diff-stable. */
    private static JsonPrimitive number(float value) {
        double rounded = Math.round(value * 10000.0) / 10000.0;
        if (rounded == Math.rint(rounded)) return new JsonPrimitive((long)rounded);
        return new JsonPrimitive(rounded);
    }
}
