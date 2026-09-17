package dev.sporebound;

import java.util.LinkedHashSet;
import java.util.UUID;
import java.nio.file.Files;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelResource;
import com.Harbinger.Spore.Sentities.Organoids.Proto;

/** Counts all admitted hives, including unloaded ones. Retired UUIDs never reappear on chunk load. */
public final class HivePopulation extends SavedData {
    private final LinkedHashSet<UUID> active=new LinkedHashSet<>();
    private final LinkedHashSet<UUID> retired=new LinkedHashSet<>();
    public static HivePopulation get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(()->migrate(level),(tag,registries)->load(tag)),"sporebound_hives");
    }
    private static HivePopulation load(CompoundTag tag) {
        if(tag.getInt("schema")!=1||!tag.contains("active",9)||!tag.contains("retired",9))throw new IllegalStateException("Unsupported hive census schema");
        var data=new HivePopulation();
        for(var entry:tag.getList("active",10))data.active.add(((CompoundTag)entry).getUUID("id"));
        for(var entry:tag.getList("retired",10))data.retired.add(((CompoundTag)entry).getUUID("id"));
        return data;
    }
    private static HivePopulation migrate(ServerLevel level) {
        var root=DimensionType.getStorageFolder(level.dimension(),level.getServer().getWorldPath(LevelResource.ROOT));
        if(Files.exists(root.resolve("data/sporebound_hives.dat")))throw new IllegalStateException("Refusing to reset existing Hive Mind census");
        var data=new HivePopulation();var folder=root.resolve("entities");
        // One-time upgrade census reads entity NBT without loading or ticking chunks.
        if(Files.isDirectory(folder))try(var paths=Files.list(folder)) {
            for(var path:paths.sorted().toList()) {
                var match=java.util.regex.Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca").matcher(path.getFileName().toString());
                if(!match.matches())continue;
                int rx=Integer.parseInt(match.group(1)),rz=Integer.parseInt(match.group(2));
                try(var region=new RegionFile(new RegionStorageInfo("sporebound-census",level.dimension(),"entities"),path,folder,false)) {
                    for(int x=0;x<32;x++)for(int z=0;z<32;z++)try(var input=region.getChunkDataInputStream(new ChunkPos(rx*32+x,rz*32+z))) {
                        if(input!=null)for(var entry:NbtIo.read(input).getList("Entities",10))data.collect((CompoundTag)entry);
                    }
                }
            }
        } catch(Exception error){throw new IllegalStateException("Cannot census existing Hive Minds; refusing an incorrect cap",error);}
        data.setDirty();return data;
    }
    private void collect(CompoundTag tag) {
        if(tag.getString("id").equals("spore:proto")&&tag.hasUUID("UUID"))active.add(tag.getUUID("UUID"));
        for(var passenger:tag.getList("Passengers",10))collect((CompoundTag)passenger);
    }
    public void reconcile(ServerLevel level) {
        int limit=CorruptionMath.hiveLimit(CorruptionData.get(level).index());
        while(active.size()>limit){var id=active.getLast();active.remove(id);retired.add(id);setDirty();}
    }
    public boolean contains(UUID id){return active.contains(id)&&!retired.contains(id);}
    public int count(){return active.size();}
    public boolean retired(UUID id){return retired.contains(id);}
    public boolean allows(ServerLevel level,UUID id) {
        reconcile(level);return !retired.contains(id)&&(active.contains(id)||active.size()<CorruptionMath.hiveLimit(CorruptionData.get(level).index()));
    }
    public static boolean admit(Entity entity) {
        return !(entity instanceof Proto)||!(entity.level() instanceof ServerLevel level)||get(level).allows(level,entity.getUUID());
    }
    public static void added(Entity entity) {
        if(entity instanceof Proto&&entity.level() instanceof ServerLevel level) {
            var data=get(level);if(data.active.add(entity.getUUID()))data.setDirty();
            HiveNodes.get(level).rememberHive((Proto)entity);
        }
    }
    public static void removed(Entity entity,Entity.RemovalReason reason) {
        if(entity instanceof Proto&&entity.level() instanceof ServerLevel level
            &&(reason.shouldDestroy()||reason==Entity.RemovalReason.CHANGED_DIMENSION)) {
            var data=get(level);if(data.active.remove(entity.getUUID()))data.setDirty();
        }
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries) {
        tag.putInt("schema",1);tag.put("active",ids(active));tag.put("retired",ids(retired));return tag;
    }
    private static ListTag ids(LinkedHashSet<UUID> ids) {
        var list=new ListTag();for(var id:ids){var entry=new CompoundTag();entry.putUUID("id",id);list.add(entry);}return list;
    }
}
