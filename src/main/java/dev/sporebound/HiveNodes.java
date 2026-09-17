package dev.sporebound;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.core.Sblocks;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

/** Saved discovery ledger. Block nodes are verified on use; dead hives leave the census. */
public final class HiveNodes extends SavedData {
    public record Node(BlockPos pos, UUID hive) { public boolean mind() { return hive != null; } }
    private final Map<BlockPos,Node> nodes = new LinkedHashMap<>();
    public static HiveNodes get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new Factory<>(()->migrate(level),(tag,lookup)->load(tag)),"sporebound_nodes");
    }
    private static HiveNodes migrate(ServerLevel level) {
        var result=new HiveNodes();
        var folder=net.minecraft.world.level.dimension.DimensionType.getStorageFolder(level.dimension(),
            level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)).resolve("entities");
        // Upgrade existing saves without loading every Hive's chunk. Mirrors the population census.
        if(java.nio.file.Files.isDirectory(folder))try(var paths=java.nio.file.Files.list(folder)){
            for(var path:paths.sorted().toList()){
                var match=java.util.regex.Pattern.compile("r\\.(-?\\d+)\\.(-?\\d+)\\.mca").matcher(path.getFileName().toString());
                if(!match.matches())continue;int rx=Integer.parseInt(match.group(1)),rz=Integer.parseInt(match.group(2));
                try(var region=new net.minecraft.world.level.chunk.storage.RegionFile(
                    new net.minecraft.world.level.chunk.storage.RegionStorageInfo("sporebound-nodes",level.dimension(),"entities"),path,folder,false)){
                    for(int x=0;x<32;x++)for(int z=0;z<32;z++)try(var input=region.getChunkDataInputStream(new net.minecraft.world.level.ChunkPos(rx*32+x,rz*32+z))){
                        if(input!=null)for(var entry:NbtIo.read(input).getList("Entities",Tag.TAG_COMPOUND))result.collect((CompoundTag)entry);
                    }
                }
            }
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot discover stored Hive locations",error);}
        result.setDirty();return result;
    }
    private void collect(CompoundTag tag){
        if(tag.getString("id").equals("spore:proto")&&tag.hasUUID("UUID")){
            var position=tag.getList("Pos",Tag.TAG_DOUBLE);
            if(position.size()==3){var p=BlockPos.containing(position.getDouble(0),position.getDouble(1),position.getDouble(2));
                nodes.put(p,new Node(p,tag.getUUID("UUID")));}
        }
        for(var entry:tag.getList("Passengers",Tag.TAG_COMPOUND))collect((CompoundTag)entry);
    }
    private static HiveNodes load(CompoundTag tag) {
        var result = new HiveNodes();
        for (var entry : tag.getList("Nodes",Tag.TAG_COMPOUND)) {
            var t = (CompoundTag)entry; var p = BlockPos.of(t.getLong("Pos"));
            result.nodes.put(p,new Node(p,t.hasUUID("Hive")?t.getUUID("Hive"):null));
        }
        return result;
    }
    public void rememberHive(Proto hive) {
        var pos = hive.blockPosition();
        boolean changed = nodes.values().removeIf(n -> hive.getUUID().equals(n.hive) && !n.pos.equals(pos));
        Node node = new Node(pos,hive.getUUID());
        if (!node.equals(nodes.put(pos,node)) || changed) setDirty();
    }
    public static boolean blockNode(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return state.is(Sblocks.BIOMASS_LUMP.get()) || state.is(Sblocks.HIVE_SPAWN.get());
    }
    public void discover(ServerLevel level, BlockPos center) {
        // Only a small local cube, staggered by the shared once-per-second network tick.
        for (var p : BlockPos.betweenClosed(center.offset(-6,-3,-6),center.offset(6,3,6)))
            if (blockNode(level,p) && !Protection.sterile(level,p) && !nodes.containsKey(p)) {
                var key=p.immutable();nodes.put(key,new Node(key,null));setDirty();
            }
    }
    public List<Node> available(ServerLevel level) {
        if (nodes.values().removeIf(n -> n.mind() ? !HivePopulation.get(level).contains(n.hive)
            : level.hasChunkAt(n.pos) && !blockNode(level,n.pos))) setDirty();
        return nodes.values().stream().filter(n -> !Protection.sterile(level,n.pos)).limit(128).toList();
    }
    public static boolean travel(ServerPlayer player, int index) {
        ServerLevel level=player.serverLevel();
        if (!Hivebound.member(player) || HiveboundEvolution.stage(player)<2 || !player.isAlive() || player.isSpectator()
            || player.isPassenger() || player.isVehicle() || Protection.sterile(level,player.blockPosition())) return false;
        var ledger=get(level);ledger.discover(level,player.blockPosition());
        var nodes=ledger.available(level);
        if(index<0 || index>=nodes.size() || nodes.stream().noneMatch(n->n.pos.closerToCenterThan(player.position(),12)))return false;
        var data=HiveboundEvolution.data(player);long now=player.server.overworld().getGameTime();
        if(now<data.getLong("NodeTravelReady"))return false;
        var node=nodes.get(index); level.getChunkAt(node.pos);
        BlockPos arrival=node.pos.above();
        if(node.mind()){
            if(!HivePopulation.get(level).contains(node.hive))return false;
            var entity=level.getEntity(node.hive);
            if(entity!=null&&(!(entity instanceof Proto)||!entity.isAlive()))return false;
            // The census is authoritative for unloaded hives; stand beside the saved body while entities load.
            arrival=(entity==null?node.pos:entity.blockPosition()).offset(3,1,0);
        }else if(!blockNode(level,node.pos))return false;
        var landing=Travel.safeNear(player,level,net.minecraft.world.phys.Vec3.atBottomCenterOf(arrival));
        if(landing==null || Protection.sterile(level,BlockPos.containing(landing)))return false;
        player.teleportTo(level,landing.x,landing.y,landing.z,Set.of(),player.getYRot(),player.getXRot());
        player.fallDistance=0;data.putLong("NodeTravelReady",now+600);WorldRules.sync(player);return true;
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup) {
        var list=new ListTag();for(var node:nodes.values()) {
            var t=new CompoundTag();t.putLong("Pos",node.pos.asLong());if(node.mind())t.putUUID("Hive",node.hive);list.add(t);
        }
        tag.put("Nodes",list);return tag;
    }
}
