package net.gobbob.mobends.client.animation;

import net.gobbob.mobends.client.model.ItemBone;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Puts the left hand on the handle of the axe in the right hand, a hand's width below it: a two-bone solve of the
 * left arm (its aim bone points it, the elbow bends to reach) from where the right hand and its axe are drawn.
 * Everything is in the body's frame, in GL units (a sixteenth of a unit is a model pixel).
 */
final class AxeGrip {
    private static final float PX = 1 / 16f;
    /** The handle's centre line on the vanilla axe icon, end to head, in ItemRenderer's 2D item frame. */
    static final Vector3f HANDLE_END = new Vector3f(13 / 16f, 2 / 16f, -1 / 32f), HANDLE_TOP = new Vector3f(7.5f / 16, 7.5f / 16, -1 / 32f);
    /** Between the hands' centres along the handle. */
    static final float HAND_GAP = 4 * PX;
    /** The left arm keeps its Java twist, a quarter turn about its length. */
    static final float[] LEFT_ARM_ROTATION = {0, 90, 0};
    /** Where the elbow prefers to point: down, out to the left and a little back. */
    private static final Vector3f ELBOW = new Vector3f(0.6f, 1, 0.4f).normalize();

    private float[] lastAim = new float[3];

    /** The left arm's aim bone angles, and its forearm's bend, holding the handle. */
    static final class Solution {
        float[] aim;
        float bend;
        /** How far the shoulder rolls toward the handle when the arm can't reach it, in the rig's (Bedrock) pixels. */
        float[] shoulder;
    }

    Solution solve(ModelBendsPlayer model) {
        Vector3f target = leftTarget(model);
        ModelRendererBends arm = (ModelRendererBends)model.bipedLeftArm, fore = (ModelRendererBends)model.bipedLeftForeArm;
        Vector3f shoulder = new Vector3f(arm.rotationPointX, arm.rotationPointY, arm.rotationPointZ).mul(PX);
        Vector3f reach = new Vector3f(target).sub(shoulder);
        float longest = handLocal(model, 0).length();
        Vector3f roll = new Vector3f();
        if (reach.length() > longest) {
            roll.set(reach).normalize().mul(reach.length() - longest + 0.01f * PX);
            reach.sub(roll);
        }
        // The elbow bend that makes the hand as far from the shoulder as the target.
        float lo = -160, hi = 0;
        for (int i = 0; i < 40; ++i) {
            float mid = (lo + hi) / 2;
            if (handLocal(model, mid).length() > reach.length()) hi = mid;
            else lo = mid;
        }
        Solution solution = new Solution();
        solution.bend = (lo + hi) / 2;
        // Model pixels (x, y, z) are Bedrock (x, -y, z).
        solution.shoulder = new float[] {roll.x / PX, -roll.y / PX, roll.z / PX};
        Vector3f hand = handLocal(model, solution.bend);
        Vector3f elbow = new Matrix3f().rotateZYX(0, LEFT_ARM_ROTATION[1] * DEG, 0)
            .transform(new Vector3f(fore.rotationPointX, fore.rotationPointY, fore.rotationPointZ).mul(PX));
        Quaternionf align = new Quaternionf().rotationTo(hand, reach);
        // Turning about the reach keeps the hand on target; take the turn that points the elbow best.
        Quaternionf best = null;
        float bestScore = -Float.MAX_VALUE;
        Vector3f axis = new Vector3f(reach).normalize();
        for (int step = 0; step < 720; ++step) {
            Quaternionf turn = new Quaternionf().rotationAxis((float)(step * Math.PI / 360), axis).mul(align);
            Vector3f e = turn.transform(new Vector3f(elbow));
            float score = e.sub(new Vector3f(axis).mul(e.dot(axis))).normalize().dot(ELBOW);
            if (score > bestScore) {
                bestScore = score;
                best = turn;
            }
        }
        Matrix3f m = new Matrix3f().rotation(best);
        double[] rowMajor = new double[9];
        for (int row = 0; row < 3; ++row) for (int col = 0; col < 3; ++col) rowMajor[row * 3 + col] = m.get(col, row);
        solution.aim = EulerZYX.angles(rowMajor, this.lastAim);
        this.lastAim = solution.aim;
        return solution;
    }

    private static final float DEG = (float)(Math.PI / 180);

    /** The left hand's centre, from the shoulder, before the aim: the arm's own turn, the elbow bent by {@code bend}. */
    private static Vector3f handLocal(ModelBendsPlayer model, float bend) {
        ModelRendererBends fore = (ModelRendererBends)model.bipedLeftForeArm;
        Vector3f elbow = new Vector3f(fore.rotationPointX, fore.rotationPointY, fore.rotationPointZ).mul(PX);
        // The hand's centre is the arm's box centre at the grip, from the elbow.
        Vector3f hand = new Vector3f(1, PlayerRig.GRIP_DROP, 0).mul(PX).sub(elbow);
        new Matrix3f().rotationX(bend * DEG).transform(hand);
        return new Matrix3f().rotateZYX(LEFT_ARM_ROTATION[2] * DEG, LEFT_ARM_ROTATION[1] * DEG, LEFT_ARM_ROTATION[0] * DEG)
            .transform(hand.add(elbow));
    }

    /** Where the left hand's centre goes: on the handle, a hand's width from the right hand toward the handle's end. */
    static Vector3f leftTarget(ModelBendsPlayer model) {
        Matrix4f grip = rightGrip(model);
        Matrix4f item = itemPlacement(model, grip);
        Vector3f end = item.transformPosition(new Vector3f(HANDLE_END)), top = item.transformPosition(new Vector3f(HANDLE_TOP));
        Vector3f right = grip.transformPosition(new Vector3f(-PX, 0, 0));
        Vector3f along = new Vector3f(end).sub(top).normalize();
        Vector3f onHandle = new Vector3f(top).add(new Vector3f(along).mul(new Vector3f(right).sub(top).dot(along)));
        return onHandle.add(along.mul(HAND_GAP));
    }

    /** The left hand's centre as the model's left arm has it now. */
    static Vector3f leftHand(ModelBendsPlayer model) {
        ModelRendererBends arm = (ModelRendererBends)model.bipedLeftArm, fore = (ModelRendererBends)model.bipedLeftForeArm;
        return limb(arm, fore).transformPosition(new Vector3f(1, PlayerRig.GRIP_DROP, 0).mul(PX));
    }

    /** The handle's line (a point on it and its direction) as the right hand draws the axe now. */
    static Vector3f[] handle(ModelBendsPlayer model) {
        Matrix4f item = itemPlacement(model, rightGrip(model));
        Vector3f end = item.transformPosition(new Vector3f(HANDLE_END)), top = item.transformPosition(new Vector3f(HANDLE_TOP));
        return new Vector3f[] {top, end.sub(top).normalize()};
    }

    /** The right hand's grip frame, as RenderBendsPlayer draws the held item from (before the item bone). */
    private static Matrix4f rightGrip(ModelBendsPlayer model) {
        return limb((ModelRendererBends)model.bipedRightArm, (ModelRendererBends)model.bipedRightForeArm)
            .translate(0, PlayerRig.GRIP_DROP * PX, 0);
    }

    /** A limb's frame with its forearm, as ModelRendererBends_SeperatedChild.postRender leaves it. */
    private static Matrix4f limb(ModelRendererBends arm, ModelRendererBends fore) {
        return part(new Matrix4f(), arm).mul(part(new Matrix4f(), fore))
            .translate(-fore.rotationPointX * PX, -fore.rotationPointY * PX, -fore.rotationPointZ * PX);
    }

    /** A part's transform: its animated pivot, aim (Z, Y, X), pre-rotation (-Y, X, Z) and rotation (Z, Y, X). */
    private static Matrix4f part(Matrix4f m, ModelRendererBends part) {
        org.lwjgl.util.vector.Vector3f aim = part.aim.vSmooth, pre = part.pre_rotation.vSmooth, rot = part.rotation.vSmooth;
        return m.translate((part.rotationPointX + part.position.vSmooth.x) * PX, (part.rotationPointY + part.position.vSmooth.y) * PX,
                (part.rotationPointZ + part.position.vSmooth.z) * PX)
            .rotateZ(aim.z * DEG).rotateY(aim.y * DEG).rotateX(aim.x * DEG)
            .rotateY(-pre.y * DEG).rotateX(pre.x * DEG).rotateZ(pre.z * DEG)
            .rotateZ(rot.z * DEG).rotateY(rot.y * DEG).rotateX(rot.x * DEG);
    }

    /**
     * From the grip to ItemRenderer's 2D item frame for a full-3D tool: the item bone, the Java item turn, then
     * RenderBendsPlayer.renderHeldItem's and ItemRenderer.renderItem's placements.
     */
    private static Matrix4f itemPlacement(ModelBendsPlayer model, Matrix4f grip) {
        ItemBone bone = model.rightItem;
        org.lwjgl.util.vector.Vector3f turn = model.renderItemRotation.vSmooth;
        return new Matrix4f(grip)
            .translate(bone.position.vSmooth.x * PX, bone.position.vSmooth.y * PX, bone.position.vSmooth.z * PX)
            .rotateZ(bone.rotation.vSmooth.z * DEG).rotateY(bone.rotation.vSmooth.y * DEG).rotateX(bone.rotation.vSmooth.x * DEG)
            .scale(bone.scaleX, bone.scaleY, bone.scaleZ)
            .rotateX(turn.x * DEG).rotateY(-turn.y * DEG).rotateZ(turn.z * DEG)
            .translate(-PX, -2 * PX, PX)
            .translate(0, 0.1875f, 0).scale(0.625f, -0.625f, 0.625f).rotateX(-100 * DEG).rotateY(45 * DEG)
            .translate(0, -0.3f, 0).scale(1.5f).rotateY(50 * DEG).rotateZ(335 * DEG).translate(-0.9375f, -0.0625f, 0);
    }
}
