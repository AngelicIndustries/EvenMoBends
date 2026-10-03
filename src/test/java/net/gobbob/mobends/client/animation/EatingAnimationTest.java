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
            com.geckolib.animation.state.BoneSnapshot end=AnimationProcessor.evaluate(animation,.3,ignored->0).get(name);
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

    @Test
    void migratedWristReachesTheMouthForBothSkinWidths() throws Exception {
        for (String name:new String[]{"player","player_slim"}) for (double time:new double[]{0,.075,.15,.225}) {
            ModelBendsPlayer model=(ModelBendsPlayer)BendsGeometryTest.models().get(name);
            Animation animation=eating();
            AnimationBaking.frame(model,m->BendsAnimator.apply(m,animation,
                AnimationProcessor.evaluate(animation,time,BendsAnimator.context(m,null)),1000,1),1000);
            Vector3f hand=model.geometry().transform(BendsGeometryTest.load(name),"rightItem",1f/16).getTranslation(new Vector3f()).mul(16);
            assertTrue(hand.y<1 && hand.y> -5,"hand height "+hand);
            assertTrue(hand.z< -3.5 && hand.z> -7,"hand in front of face "+hand);
            assertTrue(hand.x> -5 && hand.x<0,"hand beside mouth "+hand);
        }
    }
}
