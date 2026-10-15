package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import com.geckolib.animation.AnimationProcessor;
import com.geckolib.cache.animation.Animation;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Arrays;
import net.gobbob.mobends.animation.player.Animation_Eating;
import net.gobbob.mobends.client.model.ModelRendererBends;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.minecraft.item.EnumAction;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

class EatingAnimationTest {
    private Animation eating() throws Exception {
        return AnimationBaking.load(Paths.get("src/main/resources/assets/mobends/geckolib/animations/player/eating.animation.json"))
            .getAnimation(Animation_Eating.NAME);
    }

    @Test
    void requiresActiveConsumptionAndSelectsTheActualHand() {
        assertEquals(0,Animation_Eating.hand(EnumAction.eat,0,false));
        assertEquals(0,Animation_Eating.hand(null,20,false));
        assertEquals(0,Animation_Eating.hand(EnumAction.bow,20,false));
        assertEquals(0,Animation_Eating.hand(EnumAction.block,20,true));
        assertEquals(1,Animation_Eating.hand(EnumAction.eat,20,false));
        assertEquals(2,Animation_Eating.hand(EnumAction.eat,20,true));
        assertEquals(1,Animation_Eating.hand(EnumAction.drink,20,false));
    }

    @Test
    void onlyTheConsumingArmIsReplacedAndTheLoopCloses() throws Exception {
        Animation animation=eating();
        java.util.Map<String,com.geckolib.animation.state.BoneSnapshot> start=AnimationProcessor.evaluate(animation,0,ignored->0);
        assertEquals(new HashSet<>(Arrays.asList("rightArmAim","rightArm","rightForeArm","rightItem")),start.keySet());
        for (String name:start.keySet()) {
            com.geckolib.animation.state.BoneSnapshot end=AnimationProcessor.evaluate(animation,animation.length(),ignored->0).get(name);
            assertEquals(start.get(name).getRotX(),end.getRotX(),.0001);
        }
        for (boolean offhand:new boolean[]{false,true}) {
            ModelBendsPlayer model=AnimationBaking.model(0,0);
            ModelRendererBends leg=(ModelRendererBends)model.bipedRightLeg;
            leg.rotation.setSmoothX(27);
            BendsAnimator.apply(model,animation,AnimationProcessor.evaluate(animation,.1,BendsAnimator.context(model,null)),1000,1,null,null,offhand);
            assertEquals(27,leg.rotation.vFinal.x);
            assertEquals(0,((ModelRendererBends)(offhand?model.bipedRightArm:model.bipedLeftArm)).rotation.vFinal.x);
        }
    }

    /** The held food is centered in front of the mouth, outside the head, and each bite lifts it. */
    @Test
    void heldFoodIsAtTheMouthAndEachBiteMoves() throws Exception {
        for (String name:new String[]{"player","player_slim"}) {
            Animation animation=eating();
            float rest=0, bite=0;
            for (double time:new double[]{0,.05,.1,.15,.2}) {
                Vector3f[] food=food(name,animation,time,0,0);
                assertTrue(Math.abs(food[0].x)<1,"food centered on the mouth "+food[0]);
                assertTrue(food[0].y> -3 && food[0].y<.5,"food at mouth height "+food[0]);
                assertTrue(food[0].z> -9 && food[0].z< -5,"food in front of the face "+food[0]);
                assertEquals(0,insideHead(food),"food clips into the head at "+time);
                if (time==0) rest=food[0].y;
                if (time==.1) bite=food[0].y;
            }
            assertTrue(rest-bite>1,"bite lifts the food "+rest+" -> "+bite);
        }
    }

    /** The arm follows the look, so the food stays in front of the face as the head turns. */
    @Test
    void heldFoodFollowsTheLook() throws Exception {
        Animation animation=eating();
        for (float pitch:new float[]{-50,-25,0,25,50}) {
            for (float yaw:new float[]{-30,0,30}) {
                for (double time:new double[]{0,.1}) {
                    Vector3f[] food=food("player",animation,time,pitch,yaw);
                    assertTrue(food[0].z< -4.6f,"food in front of the face at "+pitch+"/"+yaw+": "+food[0]);
                    assertTrue(insideHead(food)<1,"food clips into the head at "+pitch+"/"+yaw);
                }
            }
        }
    }

    private static final float DEG=(float)(Math.PI/180);

    /**
     * The held food's center, then its corners, in pixels in the head's frame. The item draw is RenderBendsPlayer's
     * placement for a plain item, then ItemRenderer's for an equipped 2D icon (a unit square, 1/16 thick).
     */
    private static Vector3f[] food(String name,Animation animation,double time,float pitch,float yaw) throws Exception {
        ModelBendsPlayer model=(ModelBendsPlayer)BendsGeometryTest.models().get(name);
        model.headRotationX=pitch;
        model.headRotationY=yaw;
        AnimationBaking.frame(model,m->BendsAnimator.apply(m,animation,
            AnimationProcessor.evaluate(animation,time,BendsAnimator.context(m,null)),1000,1),1000);
        Matrix4f item=new Matrix4f().rotateX(-pitch*DEG).rotateY(-yaw*DEG).scale(16)
            .mul(model.geometry().transform(BendsGeometryTest.load(name),"rightItem",1f/16))
            .translate(-.0625f,-.125f,.0625f).translate(.25f,.1875f,-.1875f).scale(.375f)
            .rotateZ(60*DEG).rotateX(-90*DEG).rotateZ(20*DEG).translate(0,-.3f,0).scale(1.5f)
            .rotateY(50*DEG).rotateZ(335*DEG).translate(-.9375f,-.0625f,0);
        float[][] points={{.5f,.5f,-.03125f},{0,0,0},{1,0,0},{0,1,0},{1,1,0},{0,0,-.0625f},{1,0,-.0625f},{0,1,-.0625f},{1,1,-.0625f}};
        Vector3f[] out=new Vector3f[points.length];
        for (int i=0;i<points.length;i++) out[i]=item.transformPosition(new Vector3f(points[i][0],points[i][1],points[i][2]));
        return out;
    }

    /** How far the points sit behind the front of the head with its hat layer, summed. */
    private static float insideHead(Vector3f[] points) {
        float depth=0;
        for (Vector3f p:points) if (Math.abs(p.x)<4.6f && p.y> -8.6f && p.y<.6f && p.z> -4.6f) depth+=p.z+4.6f;
        return depth;
    }
}
