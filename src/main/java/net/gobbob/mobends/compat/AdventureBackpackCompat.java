package net.gobbob.mobends.compat;

import cpw.mods.fml.common.Loader;
import net.gobbob.mobends.client.model.entity.ModelBendsPlayer;
import net.gobbob.mobends.compat.adventurebackpack.BackWearableRenderer;
import net.gobbob.mobends.util.BendsLogger;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Adventure Backpack draws what a player wears on the back, a backpack, the copter pack or the coal jetpack, while
 * the player's specials are drawn, from a model of its own posed like vanilla's body: on a bending player it would
 * stay put while the body bends. Its renderer is swapped for one that draws it on the body bone instead.
 */
public final class AdventureBackpackCompat {
    private static final boolean LOADED = Loader.isModLoaded("adventurebackpack");
    private static boolean installed;
    private static Entity drawing;
    private static ModelBendsPlayer pose;

    private AdventureBackpackCompat() {}

    public static void install() {
        if (!LOADED) return;
        try {
            BackWearableRenderer.install();
        } catch (LinkageError | RuntimeException e) {
            BendsLogger.log("Adventure Backpack's wearables stay where it puts them: " + e, BendsLogger.ERROR);
            return;
        }
        installed = true;
        BendsLogger.log("Adventure Backpack detected: what players wear on their backs follows the body.", BendsLogger.INFO);
    }

    /** {@code entity}'s specials are being drawn now, on {@code pose}; both null once they are done. */
    public static void drawingSpecials(Entity entity, ModelBendsPlayer pose) {
        AdventureBackpackCompat.drawing = entity;
        AdventureBackpackCompat.pose = pose;
    }

    /**
     * Whether {@code player} flies on what it wears on its back, a running copter pack or a coal jetpack giving thrust:
     * it flies as in creative flight rather than jumping and falling.
     */
    public static boolean flies(EntityPlayer player) {
        if (!installed) return false;
        try {
            return BackWearableRenderer.flies(player);
        } catch (LinkageError | RuntimeException e) {
            installed = false;
            BendsLogger.log("Adventure Backpack's flight could not be read: " + e, BendsLogger.ERROR);
            return false;
        }
    }

    /** The bones {@code entity}'s specials are being drawn on, or null when this mod isn't drawing them. */
    public static ModelBendsPlayer pose(Entity entity) {
        return entity != null && entity == drawing ? pose : null;
    }
}
