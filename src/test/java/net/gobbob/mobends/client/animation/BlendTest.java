package net.gobbob.mobends.client.animation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import net.gobbob.mobends.data.Data_Player;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

class BlendTest {
    @Test
    void blendEasesInThenFollows() {
        assertEquals(BendsAnimator.EASE_RATE, BendsAnimator.blendRate(0), 1e-6f);
        assertEquals(BendsAnimator.BLEND_END_RATE, BendsAnimator.blendRate(0.9999f), 1e-2f);
        assertEquals(BendsAnimator.FOLLOW_RATE, BendsAnimator.blendRate(1), 1e-6f);
    }

    @Test
    void slotTimingRestartsOnSwitchOrPause() throws Exception {
        Data_Player data = data();
        data.ticks = 100;
        assertEquals(0, data.ticksPlaying("swim_upper", "moving"), 1e-6f);
        // Played every frame, as the game does.
        while (data.ticks < 110) {
            data.ticks += 0.5f;
            data.ticksPlaying("swim_upper", "moving");
        }
        assertEquals(10, data.ticksPlaying("swim_upper", "moving"), 1e-6f);
        assertEquals(0, data.ticksPlaying("swim_legs", "moving"), 1e-6f, "slots are independent");
        data.ticks = 111;
        assertEquals(0, data.ticksPlaying("swim_upper", "idle"), 1e-6f, "switching animation restarts");
        data.ticks = 120;
        assertEquals(0, data.ticksPlaying("swim_upper", "idle"), 1e-6f, "not played for a moment restarts");
    }

    @Test
    void holdingVariantIsUsedOnlyWhenPresent() {
        String json = "{\"format_version\": \"1.8.0\", \"animations\": {"
            + "\"animation.a.swim\": {\"bones\": {}}, \"animation.a.swim_holding\": {\"bones\": {}},"
            + " \"animation.a.swim_legs\": {\"bones\": {}}}}";
        com.geckolib.cache.animation.BakedAnimations animations =
            new com.geckolib.loading.loader.GeckoLibGsonLoader().loadAnimations(new java.io.StringReader(json));
        assertEquals("animation.a.swim_holding", BendsAnimator.variant(animations, "animation.a.swim", true));
        assertEquals("animation.a.swim", BendsAnimator.variant(animations, "animation.a.swim", false));
        assertEquals("animation.a.swim_legs", BendsAnimator.variant(animations, "animation.a.swim_legs", true),
            "no holding version: the normal one");
    }

    @Test
    void holdingIsFalseOutsideAWorld() {
        assertEquals(false, BendsAnimator.isHolding(null));
    }

    private static Data_Player data() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Data_Player)((Unsafe)field.get(null)).allocateInstance(Data_Player.class);
    }
}
