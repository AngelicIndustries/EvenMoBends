package net.gobbob.mobends.client.render;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.util.ResourceLocation;
import industries.angelic.salamander.cem.client.CemBinding;
import industries.angelic.salamander.cem.client.CemPlayers;
import industries.angelic.salamander.cem.loading.CemLoader;
import industries.angelic.salamander.cem.model.CemBoxMesh;
import industries.angelic.salamander.cem.model.CemModel;
import org.junit.jupiter.api.Test;

/** Opt-in contract check against users' original pack ZIPs; no third-party artwork is redistributed. */
class ExpressionPackTest {
    @Test
    void originalPacksLoadAndSampleTheEditorsSharedSlots() throws Exception {
        String paths = System.getenv("EXPRESSION_PACKS");
        assumeTrue(
            paths != null && !paths.isEmpty(), "Set EXPRESSION_PACKS to original Just Expressions / Fresh Moves ZIPs");
        ModelBiped player = new ModelBiped();
        CemPlayers.register(player);
        Map<String, ModelRenderer> parts = new LinkedHashMap<>();
        parts.put("head", player.bipedHead);
        parts.put("headwear", player.bipedHeadwear);
        parts.put("body", player.bipedBody);
        parts.put("right_arm", player.bipedRightArm);
        parts.put("left_arm", player.bipedLeftArm);
        parts.put("right_leg", player.bipedRightLeg);
        parts.put("left_leg", player.bipedLeftLeg);
        parts.put("ear", player.bipedEars);
        parts.put("cloak", player.bipedCloak);
        CemBinding binding = new CemBinding(player, parts);
        for (String path : paths.split(java.util.regex.Pattern.quote(File.pathSeparator))) {
            try (ZipFile zip = new ZipFile(path)) {
                String folder =
                    zip.getEntry("assets/minecraft/emf/cem/player.jem") != null ? "emf/cem/" : "optifine/cem/";
                for (String file : new String[] {"player.jem", "player_slim.jem"}) {
                    CemModel model = new CemLoader(location -> {
                        ZipEntry entry =
                            zip.getEntry("assets/" + location.getResourceDomain() + "/" + location.getResourcePath());
                        if (entry == null)
                            throw new FileNotFoundException(location.toString());
                        return new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8);
                    }).load(new ResourceLocation("minecraft", folder + file), binding.pivots, binding.parents, 64, 64);
                    assertTrue(model.assignmentCount() > 100, path + ": expressions compile");
                    // Every two-wide eye's lid, white and pupil rectangles in the common format.
                    for (int[] slot : new int[][] {{4, 6, 1}, {4, 4, 1}, {4, 2, 1}, {4, 0, 1}, {24, 5, 2}, {24, 2, 2},
                             {28, 5, 2}, {28, 2, 2}, {32, 4, 3}}) {
                        int u = slot[0], v = slot[1], h = slot[2];
                        for (int side = 0; side < 2; side++) {
                            assertFront(model, u + side * 2, v, 2, 1);
                            assertFront(model, u + side * 2 + (side == 0 ? 0 : 1), v + 1, 1, h);
                            assertFront(model, u + side * 2 + (side == 0 ? 1 : 0), v + 1, 1, Math.min(h, 2));
                        }
                    }
                    // Derp pupils, Enderman eyes, wide eyes/brows, and one-wide blinking eyes.
                    for (int side = 0; side < 2; side++) {
                        for (int row = 1; row < 8; row += 2) assertFront(model, 2 + side, row, 1, 1);
                        for (int row = 0; row < 4; row += 2) {
                            assertFront(model, 32 + side * 5, row, 3, 1);
                            assertFront(model, 32 + side * 5, row + 1, 3, 1);
                            assertFront(model, 35 + side, row + 1, 1, 1);
                        }
                        assertFront(model, 36 + side * 2, 6, 2, 1);
                        assertFront(model, 36 + side * 2 + (side == 0 ? 0 : 1), 7, 1, 1);
                        assertFront(model, 36 + side * 2 + (side == 0 ? 1 : 0), 7, 1, 1);
                        assertFront(model, 36 + side * 2, 4, 2, 1);
                        assertFront(model, 36 + side * 2, 5, 2, 1);
                        for (int row = 0; row < 8; row++) assertFront(model, side, row, 1, 1);
                        for (int row = 4; row < 8; row++) assertFront(model, 56 + side * 2, row, 2, 1);
                        assertFront(model, 56 + side * 2, 0, 2, 1);
                        assertFront(model, side == 0 ? 56 : 59, 1, 1, 2);
                    }
                    assertFront(model, 24, 0, 5, 1);
                    assertFront(model, 24, 1, 5, 1);
                    for (int row = 0; row < 8; row += 2)
                        for (int side = 0; side < 2; side++) {
                            // Mirrored mask and unmirrored brow must sample left-to-right in the same order.
                            assertFront(model, 60 + side * 2, row, 2, 1);
                            assertFront(model, 60 + side * 2, row + 1, 2, 1);
                        }
                }
            }
        }
    }

    private static void assertFront(CemModel model, int u, int v, int width, int height) {
        for (CemModel.Node node : model.nodes)
            for (CemModel.Box box : node.boxes) {
                if (box.faceUvs == null || box.faceUvs[4] == null)
                    continue;
                float[] uv = box.faceUvs[4];
                if (Math.min(uv[0], uv[2]) != u || Math.min(uv[1], uv[3]) != v || Math.abs(uv[2] - uv[0]) != width
                    || Math.abs(uv[3] - uv[1]) != height)
                    continue;
                float[] face = new CemBoxMesh(box).faces[4];
                for (int i = 0; i < 4; i++)
                    for (int j = i + 1; j < 4; j++) {
                        if (width == 1 || face[i * 5] == face[j * 5])
                            continue;
                        assertTrue((face[i * 5] - face[j * 5]) * (face[i * 5 + 3] - face[j * 5 + 3]) > 0,
                            node.id + ": skin columns must keep their left-to-right order");
                    }
                return;
            }
        fail(model.source + ": missing skin slot " + u + "," + v + " " + width + "x" + height);
    }
}
