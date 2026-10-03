package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import com.geckolib.cache.model.BakedGeoModel;
import com.geckolib.loading.loader.GeckoLibGsonLoader;
import com.geckolib.renderer.base.GeoModelRenderer;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.*;
import net.minecraft.util.ResourceLocation;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

class BendsGeometryTest {
    private static final float DEG = (float)(Math.PI / 180);
    private static final Path DIR = Paths.get("src/main/resources/assets/mobends/geckolib/models");

    static Map<String, AnimatedModel> models() {
        Map<String, AnimatedModel> models = new LinkedHashMap<>();
        ModelBendsPlayer wide = new ModelBendsPlayer(0);
        wide.initModern(); wide.setSlim(false);
        ModelBendsPlayer slim = new ModelBendsPlayer(0);
        slim.initModern(); slim.setSlim(true);
        models.put("player", wide);
        models.put("player_slim", slim);
        models.put("player_legacy", new ModelBendsPlayer(0));
        models.put("player_armor_inner", new ModelBendsPlayer(.5f));
        models.put("player_armor_outer", new ModelBendsPlayer(1));
        models.put("zombie", new ModelBendsZombie());
        models.put("zombie_villager", new ModelBendsZombieVillager());
        models.put("zombie_villager_modern", new ModelBendsZombieVillagerEtFuturum());
        models.put("zombie_armor_inner", new ModelBendsZombie(.5f, true));
        models.put("zombie_armor_outer", new ModelBendsZombie(1, true));
        models.put("spider", new ModelBendsSpider());
        return models;
    }

    @Test
    void shippedModelsPreserveOriginalGeometryAndUvs() throws Exception {
        for (Map.Entry<String, AnimatedModel> entry : models().entrySet()) {
            String json = new GsonBuilder().setPrettyPrinting().create()
                .toJson(new RenderRig(entry.getValue()).export("geometry.mobends." + entry.getKey())) + "\n";
            Path file = DIR.resolve(entry.getKey() + ".geo.json");
            if ("1".equals(System.getenv("MOBENDS_UPDATE_RIG"))) {
                Files.createDirectories(DIR);
                Files.write(file, json.getBytes(StandardCharsets.UTF_8));
            }
            assertEquals(json, new String(Files.readAllBytes(file), StandardCharsets.UTF_8), entry.getKey());
        }
    }

    static BakedGeoModel load(String name) throws Exception {
        return new GeckoLibGsonLoader().loadModel(new ResourceLocation("mobends", "geckolib/models/" + name + ".geo.json"),
            new JsonParser().parse(new String(Files.readAllBytes(DIR.resolve(name + ".geo.json")), StandardCharsets.UTF_8)).getAsJsonObject());
    }

    @Test
    void smoothedNativePosesAndAttachmentFramesAgree() throws Exception {
        Random random = new Random(659);
        for (Map.Entry<String, AnimatedModel> entry : models().entrySet()) {
            BendsGeometry geometry = new BendsGeometry(entry.getValue(), entry.getKey());
            BakedGeoModel baked = load(entry.getKey());
            RenderRig rig = geometry.rig;
            for (int trial = 0; trial < 12; trial++) {
                for (String name : rig.rigBones()) {
                    ModelRendererBends part = rig.rigPart(name);
                    if (part == null) continue;
                    part.rotation.vSmooth.set(random.nextFloat() * 300 - 150, random.nextFloat() * 300 - 150, random.nextFloat() * 300 - 150);
                    part.aim.vSmooth.set(random.nextFloat() * 80, random.nextFloat() * 80, random.nextFloat() * 80);
                    part.pre_rotation.vSmooth.set(random.nextFloat() * 40, random.nextFloat() * 40, random.nextFloat() * 40);
                    part.position.vSmooth.set(random.nextFloat() * 3, random.nextFloat() * 3, random.nextFloat() * 3);
                    part.scaleX = .8f + random.nextFloat() * .4f;
                    part.scaleY = .8f + random.nextFloat() * .4f;
                    part.scaleZ = .8f + random.nextFloat() * .4f;
                }
                for (String name : rig.rigBones()) if (rig.rigPart(name) != null) {
                    Matrix4f expected = nativeFrame(rig, name);
                    Matrix4f actual = geometry.transform(baked, name, 1f / 16);
                    assertTrue(expected.equals(actual, .0003f), entry.getKey() + "/" + name + "\n" + expected + "\n" + actual);
                }
            }
        }
    }

    private static Matrix4f nativeFrame(RenderRig rig, String name) {
        if (name == null || name.equals("root")) return new Matrix4f();
        Matrix4f frame = nativeFrame(rig, rig.rigParent(name));
        ModelRendererBends part = rig.rigPart(name);
        if (part == null) return frame;
        return frame.translate((part.rotationPointX + part.position.getX()) / 16 + part.offsetX,
                (part.rotationPointY + part.position.getY()) / 16 + part.offsetY,
                (part.rotationPointZ + part.position.getZ()) / 16 + part.offsetZ)
            .rotateZ(part.aim.getZ() * DEG).rotateY(part.aim.getY() * DEG).rotateX(part.aim.getX() * DEG)
            .rotateY(-part.pre_rotation.getY() * DEG).rotateX(part.pre_rotation.getX() * DEG).rotateZ(part.pre_rotation.getZ() * DEG)
            .rotateZ(part.rotation.getZ() * DEG).rotateY(part.rotation.getY() * DEG).rotateX(part.rotation.getX() * DEG)
            .scale(part.scaleX, part.scaleY, part.scaleZ);
    }

    @Test
    void armorVisibilityDoesNotHideHeadWhenBodyIsHidden() throws Exception {
        ModelBendsPlayer player = new ModelBendsPlayer(1);
        player.bipedBody.showModel = false;
        BendsGeometry geometry = player.geometry();
        com.geckolib.animation.state.ModelPose pose = geometry.pose(load(player.geometryName()));
        assertTrue(pose.get("body").get().isHidden());
        assertFalse(pose.get("body").get().areChildrenHidden());
        assertFalse(pose.get("head").get().isHidden());
        player.bipedRightArm.showModel = false;
        pose = geometry.pose(load(player.geometryName()));
        assertTrue(pose.get("rightArm").get().areChildrenHidden());
    }

    @Test
    void slimLayersAndHatCanBeHiddenIndependently() throws Exception {
        ModelBendsPlayer player = (ModelBendsPlayer)models().get("player_slim");
        BendsGeometry geometry = player.geometry();
        BakedGeoModel baked = load(player.geometryName());
        int[] vertices = {0};
        GeoModelRenderer.render(baked, geometry.pose(baked), (x,y,z,u,v,nx,ny,nz,r,g,b,a) -> vertices[0]++, 1,1,1,1);
        int full = vertices[0];
        player.bipedHeadwear.showModel = false;
        vertices[0] = 0;
        GeoModelRenderer.render(baked, geometry.pose(baked), (x,y,z,u,v,nx,ny,nz,r,g,b,a) -> vertices[0]++, 1,1,1,1);
        assertEquals(24, full - vertices[0]);
    }
}
