package dev.sporebound;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import com.Harbinger.Spore.core.Sblocks;
import java.util.Comparator;

/** Native Spore points only: harvest with Proto's own accounting, then share conserved surplus. */
public final class HiveResources {
    public static final int RESERVE=200, TRANSFER_LIMIT=20;
    private HiveResources() {}
    public static boolean connected(Proto a,Proto b) {
        return intact(a,b)||intact(b,a);
    }
    private static boolean intact(Proto owner,Proto peer) {
        if(!(owner.level() instanceof ServerLevel level)||peer.level()!=level||!owner.isAlive()||!peer.isAlive()
            ||owner.distanceToSqr(peer)>HiveConnections.RANGE*HiveConnections.RANGE)return false;
        var data=HiveBurrowing.data(owner);
        long[] path=data.getLongArray("LinkPath");
        if(!data.getBoolean("Connected")||!data.hasUUID("Peer")||!data.getUUID("Peer").equals(peer.getUUID())
            ||data.getLong("LinkFrom")!=owner.blockPosition().below().asLong()
            ||data.getLong("LinkTo")!=peer.blockPosition().below().asLong()
            ||path.length==0||path.length>HiveConnections.MAX_PATH)return false;
        for(long node:path){var pos=BlockPos.of(node);
            if(!level.hasChunkAt(pos)||Protection.sterile(level,pos)
                ||!level.getBlockState(pos).is(Sblocks.ROOTED_BIOMASS.get()))return false;
        }
        return true;
    }
    public static int share(Proto donor) {
        if(!(donor.level() instanceof ServerLevel level)||!donor.isAlive()
            ||Protection.sterile(level,donor.blockPosition())||donor.getTarget()!=null
            ||donor.getHealth()<donor.getMaxHealth()*0.75f||donor.getBiomass()<=RESERVE)return 0;
        var peer=level.getEntitiesOfClass(Proto.class,donor.getBoundingBox().inflate(HiveConnections.RANGE),
            p->p!=donor&&p.getBiomass()<RESERVE&&connected(donor,p)).stream()
            .min(Comparator.comparingInt(Proto::getBiomass)).orElse(null);
        if(peer==null)return 0;
        int amount=Math.min(TRANSFER_LIMIT,Math.min(donor.getBiomass()-RESERVE,RESERVE-peer.getBiomass()));
        if(amount<=0)return 0;
        donor.eatBiomass(amount);peer.addBiomass(amount);
        return amount;
    }
    /** One local collection attempt per ten seconds, preserving Spore's kill and evolution values. */
    public static int gather(Proto hive) {
        if(!(hive.level() instanceof ServerLevel level)||!hive.isAlive()||hive.getTarget()!=null
            ||Protection.sterile(level,hive.blockPosition())||hive.getBiomass()>=RESERVE)return 0;
        var hosts=level.getEntitiesOfClass(Infected.class,hive.getBoundingBox().inflate(32),
            host->host.isAlive()&&host.getTarget()==null&&!Protection.sterile(level,host.blockPosition()));
        var candidate=hosts.stream().filter(host->host.getKills()>0||hosts.size()>=6)
            .max(Comparator.comparingInt(Infected::getKills)).orElse(null);
        int before=hive.getBiomass();
        if(candidate!=null)hive.harvestBiomassByDespawning(candidate);
        return hive.getBiomass()-before;
    }
    public static void develop(Proto hive) {
        gather(hive);
        int sent=share(hive);
        HiveBurrowing.data(hive).putString("ResourceMode",sent>0?"SUPPORTING":"DEVELOPING");
    }
}
