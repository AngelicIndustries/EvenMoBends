package net.gobbob.mobends.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/**
 * Lanterns worn in a Baubles Expanded belt slot hang by a chain from the front of the belt's right side and swing with
 * the thigh. A worn lantern is an {@link ItemBlock} whose block draws as a hanging lantern for some metadata, with its
 * own block renderer; register that block's class here, from init. Even Mo' Bends registers Et Futurum Requiem's
 * lantern and soul lantern. This only decides how a worn one looks: the item must be a belt bauble itself (Baubles
 * Expanded's {@code IBaubleExpanded} with the "belt" type). Safe to call on either side; only the client draws.
 */
public final class WornLanterns {

    private static final List<Entry> LANTERNS = new CopyOnWriteArrayList<>();

    private WornLanterns() {}

    /**
     * Every block of this class (or a subclass) worn as its item hangs drawn with this metadata, which its renderer
     * reads as the hanging state.
     */
    public static void registerHangingBlock(Class<? extends Block> blockClass, int hangingMeta) {
        LANTERNS.add(new Entry(blockClass, hangingMeta));
    }

    /** The metadata a worn stack's block hangs with, or -1 when it isn't a registered lantern. */
    public static int hangingMeta(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemBlock)) return -1;
        Block block = ((ItemBlock)stack.getItem()).field_150939_a;
        for (Entry entry : LANTERNS) {
            if (entry.blockClass.isInstance(block)) return entry.meta;
        }
        return -1;
    }

    private static final class Entry {
        final Class<? extends Block> blockClass;
        final int meta;

        Entry(Class<? extends Block> blockClass, int meta) {
            this.blockClass = blockClass;
            this.meta = meta;
        }
    }
}
