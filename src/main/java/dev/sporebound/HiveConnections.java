package dev.sporebound;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.core.Sblocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.EventHooks;
import java.util.*;

/** Bounded, loaded-chunk tendrils join nearby mature Hive Minds below the surface. */
public final class HiveConnections {
    public static final int RANGE=64, MAX_PATH=192, SEARCH_BUDGET=2048;
    private HiveConnections() {}
    public static int grow(Proto hive,int budget) {
        if(!(hive.level() instanceof ServerLevel level) || !HiveBurrowing.mature(hive)
                || !EventHooks.canEntityGrief(level,hive))return 0;
        var data=HiveBurrowing.data(hive);
        Proto peer=null;
        if(data.hasUUID("Peer") && level.getEntity(data.getUUID("Peer")) instanceof Proto old && eligiblePeer(hive,old))peer=old;
        if(peer==null)peer=level.getEntitiesOfClass(Proto.class,hive.getBoundingBox().inflate(RANGE),
            other->eligiblePeer(hive,other)).stream().min(Comparator.comparingDouble(hive::distanceToSqr)).orElse(null);
        if(peer==null){data.remove("Peer");data.remove("LinkPath");data.remove("Connected");return 0;}
        BlockPos from=hive.blockPosition().below(), to=peer.blockPosition().below();
        boolean same=data.hasUUID("Peer")&&data.getUUID("Peer").equals(peer.getUUID())
            &&data.getLong("LinkFrom")==from.asLong()&&data.getLong("LinkTo")==to.asLong();
        if(!same || data.getLongArray("LinkPath").length==0) {
            var path=plan(level,from,to);
            data.putUUID("Peer",peer.getUUID());data.putLong("LinkFrom",from.asLong());data.putLong("LinkTo",to.asLong());
            data.putLongArray("LinkPath",path.stream().mapToLong(BlockPos::asLong).toArray());
            data.putBoolean("Connected",false);
        }
        long[] path=data.getLongArray("LinkPath");
        if(path.length==0 || path.length>MAX_PATH)return 0;
        int placed=0;
        boolean complete=true;
        for(long node:path) {
            var pos=BlockPos.of(node);
            if(!level.hasChunkAt(pos) || Protection.sterile(level,pos)){complete=false;break;}
            if(level.getBlockState(pos).is(Sblocks.ROOTED_BIOMASS.get()))continue;
            if(placed>=budget || hive.getBiomass()<HiveBurrowing.BIOMASS_THRESHOLD){complete=false;break;}
            if(!passable(level,pos) || !level.setBlockAndUpdate(pos,Sblocks.ROOTED_BIOMASS.get().defaultBlockState())) {
                data.remove("LinkPath");complete=false;break;
            }
            hive.eatBiomass(1);placed++;
        }
        data.putBoolean("Connected",complete);
        return placed;
    }
    private static boolean eligiblePeer(Proto hive,Proto other) {
        return other!=hive && other.level()==hive.level() && HiveBurrowing.mature(other)
            && hive.distanceToSqr(other)<=RANGE*RANGE && Math.abs(hive.getY()-other.getY())<=16;
    }
    static List<BlockPos> plan(ServerLevel level,BlockPos from,BlockPos to) {
        if(from.distSqr(to)>RANGE*RANGE)return List.of();
        int depth=Math.min(from.getY(),to.getY())-4;
        BlockPos a=new BlockPos(from.getX(),depth,from.getZ()), b=new BlockPos(to.getX(),depth,to.getZ());
        var route=new ArrayList<BlockPos>();
        for(var pair:List.of(new BlockPos[]{from,a},new BlockPos[]{a,b},new BlockPos[]{b,to})) {
            var section=search(level,pair[0],pair[1]);
            if(section.isEmpty())return List.of();
            for(var pos:section)if(route.isEmpty()||!route.getLast().equals(pos))route.add(pos);
            if(route.size()>MAX_PATH)return List.of();
        }
        return route;
    }
    private static List<BlockPos> search(ServerLevel level,BlockPos from,BlockPos to) {
        if(!passable(level,from)||!passable(level,to))return List.of();
        var queue=new PriorityQueue<BlockPos>(Comparator.comparingInt(p->p.distManhattan(to)));
        var parents=new HashMap<BlockPos,BlockPos>();
        queue.add(from);parents.put(from,from);
        int inspected=0;
        while(!queue.isEmpty()&&inspected++<SEARCH_BUDGET) {
            BlockPos current=queue.remove();
            if(current.equals(to)) {
                var path=new ArrayList<BlockPos>();
                for(BlockPos pos=to;!pos.equals(from);pos=parents.get(pos))path.add(pos);
                path.add(from);Collections.reverse(path);return path;
            }
            for(Direction direction:Direction.values()) {
                BlockPos next=current.relative(direction);
                if(parents.containsKey(next) || next.getX()<Math.min(from.getX(),to.getX())-4
                    || next.getX()>Math.max(from.getX(),to.getX())+4 || next.getZ()<Math.min(from.getZ(),to.getZ())-4
                    || next.getZ()>Math.max(from.getZ(),to.getZ())+4 || next.getY()<Math.min(from.getY(),to.getY())-4
                    || next.getY()>Math.max(from.getY(),to.getY()) || !passable(level,next))continue;
                parents.put(next,current);queue.add(next);
            }
        }
        return List.of();
    }
    private static boolean passable(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||Protection.sterile(level,pos))return false;
        BlockState state=level.getBlockState(pos);
        return !state.hasBlockEntity() && state.getFluidState().isEmpty()
            && (state.is(Sblocks.ROOTED_BIOMASS.get()) || HiveBurrowing.naturalSubstrate(state)
                || state.is(Sblocks.FUNGAL_ROOTS.get()) || (state.isAir()&&!level.canSeeSky(pos)));
    }
}
