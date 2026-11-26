package net.gobbob.mobends.client.gui;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

/**
 * An empty, fully lit world for the menu's previews when no world is open, as from the main menu: mobs and the player
 * need one to be drawn. It holds no entities and saves nothing.
 */
public final class PreviewWorld extends World {

    public static final PreviewWorld INSTANCE = new PreviewWorld();

    private PreviewWorld() {
        super(new SaveHandler(), "MoBendsPreview", new WorldProviderSurface(),
            new WorldSettings(1L, WorldSettings.GameType.SURVIVAL, true, false, WorldType.DEFAULT), new Profiler());
        // Register the provider with this world without claiming a real dimension's ID.
        this.provider.setDimension(Integer.MAX_VALUE);
        int dimension = this.provider.dimensionId;
        this.provider.registerWorld(this);
        this.provider.setDimension(dimension);
        this.chunkProvider = this.createChunkProvider();
        this.calculateInitialSkylight();
        if (this.difficultySetting == null) this.difficultySetting = EnumDifficulty.NORMAL;
    }

    @Override
    protected IChunkProvider createChunkProvider() {
        return new Chunks(this);
    }

    @Override
    protected int func_152379_p() {
        return -1;
    }

    @Override
    public Entity getEntityByID(int id) {
        return null;
    }

    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block) {}

    @Override
    public void notifyBlocksOfNeighborChange(int x, int y, int z, Block block, int side) {}

    @Override
    public void markAndNotifyBlock(int x, int y, int z, Chunk chunk, Block oldBlock, Block newBlock, int flag) {}

    @Override
    public void markBlockForUpdate(int x, int y, int z) {}

    @Override
    public void markBlockRangeForRenderUpdate(int x1, int y1, int z1, int x2, int y2, int z2) {}

    @Override
    public boolean updateLightByType(EnumSkyBlock type, int x, int y, int z) {
        return true;
    }

    @Override
    public float getLightBrightness(int x, int y, int z) {
        return 1.0f;
    }

    @Override
    public int getLightBrightnessForSkyBlocks(int x, int y, int z, int lightValue) {
        return 15728880;
    }

    @Override
    public int getBlockLightValue(int x, int y, int z) {
        return 15;
    }

    @Override
    public int getBlockLightValue_do(int x, int y, int z, boolean useNeighborBrightness) {
        return 15;
    }

    @Override
    public int getSkyBlockTypeBrightness(EnumSkyBlock type, int x, int y, int z) {
        return 15;
    }

    @Override
    public int getSavedLightValue(EnumSkyBlock type, int x, int y, int z) {
        return 15;
    }

    /** Empty chunks, made when first asked for. */
    private static final class Chunks implements IChunkProvider {

        private final World world;
        private final Map<Long, Chunk> chunks = new HashMap<Long, Chunk>();

        Chunks(World world) {
            this.world = world;
        }

        private static Long key(int x, int z) {
            return ((long)x & 0xffffffffL) | ((long)z & 0xffffffffL) << 32;
        }

        @Override
        public boolean chunkExists(int x, int z) {
            return true;
        }

        @Override
        public Chunk provideChunk(int x, int z) {
            Chunk chunk = this.chunks.get(key(x, z));
            if (chunk == null) this.chunks.put(key(x, z), chunk = new Chunk(this.world, x, z));
            return chunk;
        }

        @Override
        public Chunk loadChunk(int x, int z) {
            return this.provideChunk(x, z);
        }

        @Override
        public void populate(IChunkProvider provider, int x, int z) {}

        @Override
        public boolean saveChunks(boolean all, IProgressUpdate progress) {
            return false;
        }

        @Override
        public boolean unloadQueuedChunks() {
            return false;
        }

        @Override
        public boolean canSave() {
            return false;
        }

        @Override
        public String makeString() {
            return "MoBendsPreview";
        }

        @SuppressWarnings("rawtypes")
        @Override
        public List getPossibleCreatures(EnumCreatureType type, int x, int y, int z) {
            return Collections.emptyList();
        }

        @Override
        public ChunkPosition func_147416_a(World world, String structure, int x, int y, int z) {
            return null;
        }

        @Override
        public int getLoadedChunkCount() {
            return this.chunks.size();
        }

        @Override
        public void recreateStructures(int x, int z) {}

        @Override
        public void saveExtraData() {}
    }

    /** Saves nothing and loads nothing. */
    private static final class SaveHandler implements ISaveHandler, IPlayerFileData, IChunkLoader {

        @Override
        public WorldInfo loadWorldInfo() {
            return null;
        }

        @Override
        public void checkSessionLock() {}

        @Override
        public IChunkLoader getChunkLoader(WorldProvider provider) {
            return this;
        }

        @Override
        public void saveWorldInfoWithPlayer(WorldInfo info, NBTTagCompound tag) {}

        @Override
        public void saveWorldInfo(WorldInfo info) {}

        @Override
        public IPlayerFileData getSaveHandler() {
            return this;
        }

        @Override
        public void flush() {}

        @Override
        public File getWorldDirectory() {
            return null;
        }

        @Override
        public File getMapFileFromName(String name) {
            return null;
        }

        @Override
        public String getWorldDirectoryName() {
            return "";
        }

        @Override
        public void writePlayerData(EntityPlayer player) {}

        @Override
        public NBTTagCompound readPlayerData(EntityPlayer player) {
            return null;
        }

        @Override
        public String[] getAvailablePlayerDat() {
            return new String[0];
        }

        @Override
        public Chunk loadChunk(World world, int x, int z) {
            return null;
        }

        @Override
        public void saveChunk(World world, Chunk chunk) {}

        @Override
        public void saveExtraChunkData(World world, Chunk chunk) {}

        @Override
        public void chunkTick() {}

        @Override
        public void saveExtraData() {}
    }
}
