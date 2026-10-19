package net.gobbob.mobends.compat.wawelauth;

import net.gobbob.mobends.client.animation.BendsGeometry;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.MinecraftForgeClient;

import industries.angelic.wawelauth.api.skin.DrawKind;
import industries.angelic.wawelauth.api.skin.LayerDraw;
import industries.angelic.wawelauth.api.skin.LayerSegment;
import industries.angelic.wawelauth.api.skin.SkinLayers;
import industries.angelic.wawelauth.api.skin.SkinPart;
import org.lwjgl.opengl.GL11;

/**
 * Draws a Mo' Bends player's overlay layers through Wawel Auth, which decides flat or voxel, visibility, armor and
 * distance. Head and body get whole boxes; each jointed limb gets its upper half on the limb bone and its lower half
 * on the fore bone, whose origin is the joint. The boxes match ModelBendsPlayer's own wear boxes. Loaded only when
 * Wawel Auth is present.
 */
public final class WawelAuthLayers {

    static final LayerSegment HAT = LayerSegment.box(SkinPart.HAT, -4, -8, -4, 8, 8, 8, 32, 0);
    static final LayerSegment JACKET = LayerSegment.box(SkinPart.JACKET, -4, -12, -2, 8, 12, 4, 16, 32);

    static final LayerSegment RIGHT_SLEEVE_UPPER = upperArm(SkinPart.RIGHT_SLEEVE, -3, 4, 40, 32);
    static final LayerSegment RIGHT_SLEEVE_LOWER = lowerArm(SkinPart.RIGHT_SLEEVE, 4, 40, 32);
    static final LayerSegment RIGHT_SLEEVE_UPPER_SLIM = upperArm(SkinPart.RIGHT_SLEEVE, -2, 3, 40, 32);
    static final LayerSegment RIGHT_SLEEVE_LOWER_SLIM = lowerArm(SkinPart.RIGHT_SLEEVE, 3, 40, 32);
    static final LayerSegment LEFT_SLEEVE_UPPER = upperArm(SkinPart.LEFT_SLEEVE, -1, 4, 48, 48);
    static final LayerSegment LEFT_SLEEVE_LOWER = lowerArm(SkinPart.LEFT_SLEEVE, 4, 48, 48);
    static final LayerSegment LEFT_SLEEVE_UPPER_SLIM = upperArm(SkinPart.LEFT_SLEEVE, -1, 3, 48, 48);
    static final LayerSegment LEFT_SLEEVE_LOWER_SLIM = lowerArm(SkinPart.LEFT_SLEEVE, 3, 48, 48);

    static final LayerSegment RIGHT_PANTS_UPPER = upperLeg(SkinPart.RIGHT_PANTS, 0, 32);
    static final LayerSegment RIGHT_PANTS_LOWER = lowerLeg(SkinPart.RIGHT_PANTS, 0, 32);
    static final LayerSegment LEFT_PANTS_UPPER = upperLeg(SkinPart.LEFT_PANTS, 0, 48);
    static final LayerSegment LEFT_PANTS_LOWER = lowerLeg(SkinPart.LEFT_PANTS, 0, 48);

    private static final int LIMB = 12;
    private static final int HALF = 6;

    private WawelAuthLayers() {}

    private static LayerSegment upperArm(SkinPart part, float x, int width, int u, int v) {
        return LayerSegment.box(part, x, -2, -2, width, LIMB, 4, u, v).rows(0, HALF);
    }

    private static LayerSegment lowerArm(SkinPart part, int width, int u, int v) {
        return LayerSegment.box(part, 0, -HALF, -4, width, LIMB, 4, u, v).rows(HALF, LIMB);
    }

    private static LayerSegment upperLeg(SkinPart part, int u, int v) {
        return LayerSegment.box(part, -2, 0, -2, 4, LIMB, 4, u, v).rows(0, HALF);
    }

    private static LayerSegment lowerLeg(SkinPart part, int u, int v) {
        return LayerSegment.box(part, -2, -HALF, 0, 4, LIMB, 4, u, v).rows(HALF, LIMB);
    }

    /**
     * Draws the rigged model without its own overlay boxes, then the layers. The draw is open around the model too,
     * so code that replaces it can suppress these layers or claim some of their parts.
     */
    public static void render(ModelBendsPlayer model, Entity entity, float scale) {
        try (LayerDraw draw = SkinLayers.begin(entity, drawKind(), scale)) {
            ModelBendsPlayer.WearState shown = model.hideWear();
            try {
                model.renderGeometry(scale);
            } finally {
                model.restoreWear(shown);
            }
            if (draw.suppressed()) return;

            if (model.isChild) {
                // The same groups as ModelBendsPlayer.renderGeometry.
                GL11.glPushMatrix();
                GL11.glScalef(0.75f, 0.75f, 0.75f);
                GL11.glTranslatef(0.0f, 16.0f * scale, 0.0f);
                if (shown.hat) submit(draw, model, model.bipedHead, HAT, scale);
                GL11.glPopMatrix();
                GL11.glPushMatrix();
                GL11.glScalef(0.5f, 0.5f, 0.5f);
                GL11.glTranslatef(0.0f, 24.0f * scale, 0.0f);
                submitBody(draw, model, shown, scale);
                GL11.glPopMatrix();
            } else {
                if (shown.hat) submit(draw, model, model.bipedHead, HAT, scale);
                submitBody(draw, model, shown, scale);
            }
        }
    }

    private static void submitBody(LayerDraw draw, ModelBendsPlayer model, ModelBendsPlayer.WearState shown,
        float scale) {
        boolean slim = model.isSlim();
        if (shown.jacket) submit(draw, model, model.bipedBody, JACKET, scale);
        if (shown.rightSleeve) {
            submit(draw, model, model.bipedRightArm, slim ? RIGHT_SLEEVE_UPPER_SLIM : RIGHT_SLEEVE_UPPER, scale);
            submit(draw, model, model.foreArm(true), slim ? RIGHT_SLEEVE_LOWER_SLIM : RIGHT_SLEEVE_LOWER, scale);
        }
        if (shown.leftSleeve) {
            submit(draw, model, model.bipedLeftArm, slim ? LEFT_SLEEVE_UPPER_SLIM : LEFT_SLEEVE_UPPER, scale);
            submit(draw, model, model.foreArm(false), slim ? LEFT_SLEEVE_LOWER_SLIM : LEFT_SLEEVE_LOWER, scale);
        }
        if (shown.rightPants) {
            submit(draw, model, model.bipedRightLeg, RIGHT_PANTS_UPPER, scale);
            submit(draw, model, model.foreLeg(true), RIGHT_PANTS_LOWER, scale);
        }
        if (shown.leftPants) {
            submit(draw, model, model.bipedLeftLeg, LEFT_PANTS_UPPER, scale);
            submit(draw, model, model.foreLeg(false), LEFT_PANTS_LOWER, scale);
        }
    }

    private static void submit(LayerDraw draw, ModelBendsPlayer model, ModelRenderer part, LayerSegment segment,
        float scale) {
        if (part.isHidden || !part.showModel || draw.claimed(segment.part())) return;
        BendsGeometry geometry = model.geometry();
        String bone = geometry.rig.name(part);
        if (bone == null) return;
        GL11.glPushMatrix();
        geometry.attach(bone, scale);
        draw.submit(segment);
        GL11.glPopMatrix();
    }

    /** World entity passes set a render pass; GUI drawing (inventory, previews) happens outside them. */
    private static DrawKind drawKind() {
        return MinecraftForgeClient.getRenderPass() == -1 ? DrawKind.GUI : DrawKind.WORLD;
    }
}
