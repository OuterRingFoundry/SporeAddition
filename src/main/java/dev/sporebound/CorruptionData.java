package dev.sporebound;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class CorruptionData extends SavedData {
    private static final java.util.concurrent.ConcurrentHashMap<ServerLevel, CorruptionData> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile double index;
    private int seeded;
    public CorruptionData(double initial) { index = initial; }
    public static CorruptionData get(ServerLevel level) {
        double initial = level.dimension().equals(Sporebound.BLIGHT) ? 6 : -1;
        return CACHE.computeIfAbsent(level, key -> key.getDataStorage().computeIfAbsent(new Factory<>(() -> freshOrRefuse(key, initial),
            (tag, registries) -> load(tag)), "sporebound_corruption"));
    }
    private static CorruptionData freshOrRefuse(ServerLevel level, double initial) {
        var root=level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        var file=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),root)
            .resolve("data").resolve("sporebound_corruption.dat");
        // Vanilla catches deserialization failures. Its fallback must not silently unlock quarantine.
        if(java.nio.file.Files.exists(file))throw new IllegalStateException("Refusing to reset existing Sporebound corruption save: "+file);
        return new CorruptionData(initial);
    }
    /** Safe worldgen read: no disk access, locks or chunk loads on generation workers. */
    public static double peek(ServerLevel level) {
        var data=CACHE.get(level);
        return data==null?(level.dimension().equals(Sporebound.BLIGHT)?6:-1):data.index;
    }
    public static void release(ServerLevel level) { CACHE.remove(level); }
    private static CorruptionData load(CompoundTag tag) {
        if (tag.getInt("schema") != 1 || !tag.contains("index", 6))
            throw new IllegalStateException("Invalid or unsupported Sporebound save; refusing to reset corruption");
        var data = new CorruptionData(CorruptionMath.validate(tag.getDouble("index")));
        data.seeded = tag.getInt("seeded");
        if (data.seeded < 0 || data.seeded > 7) throw new IllegalStateException("Invalid founding hive mask");
        return data;
    }
    public double index() { return index; }
    public void set(double value) { index = CorruptionMath.validate(value); setDirty(); }
    public boolean seeded(int slot) { return (seeded & (1 << slot)) != 0; }
    public void markSeeded(int slot) { seeded |= 1 << slot; setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("schema", 1); tag.putDouble("index", index); tag.putInt("seeded", seeded); return tag;
    }
}
