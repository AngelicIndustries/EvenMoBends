package net.gobbob.mobends.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.entity.Entity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Mo' Bends asks Salamander, by name, whether a resource pack's CEM model takes an entity over. */
class CemCompatTest {
    /** Stands in for Salamander's {@code SalamanderCem}: takes over whatever {@link #claims} says. */
    public static final class FakeCem {
        static boolean claims;
        static boolean broken;

        public static boolean hasModel(Entity entity) {
            if (broken) throw new IllegalStateException("broken");
            return claims;
        }
    }

    @AfterEach
    void restore() {
        FakeCem.claims = false;
        FakeCem.broken = false;
        CemCompat.useApi(CemCompat.API);
    }

    private static Entity entity() throws Exception {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Entity)((sun.misc.Unsafe)field.get(null)).allocateInstance(net.minecraft.entity.monster.EntitySpider.class);
    }

    @Test
    void aSalamanderWithoutCemNeverTakesOver() throws Exception {
        CemCompat.useApi("org.example.NoSuchApi");
        assertFalse(CemCompat.hasModel(entity()));
    }

    @Test
    void followsSalamandersAnswer() throws Exception {
        CemCompat.useApi(FakeCem.class.getName());
        Entity spider = entity();
        assertFalse(CemCompat.hasModel(spider));
        FakeCem.claims = true;
        assertTrue(CemCompat.hasModel(spider), "a pack model takes the spider over");
        FakeCem.claims = false;
        assertFalse(CemCompat.hasModel(spider), "and gives it back when the pack goes");
    }

    @Test
    void aFailingCheckLeavesMoBendsRendering() throws Exception {
        CemCompat.useApi(FakeCem.class.getName());
        FakeCem.claims = true;
        FakeCem.broken = true;
        assertFalse(CemCompat.hasModel(entity()));
    }
}
