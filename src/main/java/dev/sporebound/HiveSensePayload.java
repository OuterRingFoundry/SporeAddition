package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.*;

/** Private vision, never the shared glowing flag that would reveal enemies to outsiders. */
public record HiveSensePayload(ResourceLocation dimension,List<Site> sites,List<Target> targets,UUID assignment) implements CustomPacketPayload {
    public record Site(BlockPos pos,boolean mind) {}
    public record Target(UUID id,BlockPos pos) {}
    public static final Type<HiveSensePayload> TYPE=new Type<>(Sporebound.id("hive_sense"));
    public static final StreamCodec<RegistryFriendlyByteBuf,HiveSensePayload> CODEC=new StreamCodec<>() {
        public HiveSensePayload decode(RegistryFriendlyByteBuf b) {
            var dimension=b.readResourceLocation();var sites=new ArrayList<Site>();var targets=new ArrayList<Target>();
            int count=b.readVarInt();if(count<0||count>128)throw new IllegalArgumentException("Too many hive sites");
            for(int i=0;i<count;i++)sites.add(new Site(b.readBlockPos(),b.readBoolean()));
            count=b.readVarInt();if(count<0||count>128)throw new IllegalArgumentException("Too many hive targets");
            for(int i=0;i<count;i++)targets.add(new Target(b.readUUID(),b.readBlockPos()));
            return new HiveSensePayload(dimension,List.copyOf(sites),List.copyOf(targets),b.readBoolean()?b.readUUID():null);
        }
        public void encode(RegistryFriendlyByteBuf b,HiveSensePayload p) {
            b.writeResourceLocation(p.dimension);b.writeVarInt(p.sites.size());
            for(var s:p.sites){b.writeBlockPos(s.pos);b.writeBoolean(s.mind);}
            b.writeVarInt(p.targets.size());for(var t:p.targets){b.writeUUID(t.id);b.writeBlockPos(t.pos);}
            b.writeBoolean(p.assignment!=null);if(p.assignment!=null)b.writeUUID(p.assignment);
        }
    };
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void register(RegisterPayloadHandlersEvent e){e.registrar("1").playToClient(TYPE,CODEC,
        (p,c)->c.enqueueWork(()->ClientState.current=p));}
    public static void sync(ServerPlayer player) {
        var level=player.serverLevel();boolean bound=Hivebound.member(player)&&!Protection.sterile(level,player.blockPosition());
        var sites=bound?HiveNodes.get(level).available(level).stream().map(n->new Site(n.pos(),n.mind())).toList():List.<Site>of();
        var visible=new LinkedHashMap<UUID,Target>();
        if(bound){
            for(var mark:HiveNetwork.marks(level))visible.put(mark.id(),new Target(mark.id(),mark.pos()));
            // Vanilla does not synchronize arbitrary mobs' effect lists to observers.
            // Send native Marker targets privately, including when no Hive Mind is active.
            for(var entity:level.getAllEntities())if(entity instanceof net.minecraft.world.entity.LivingEntity living
                &&living.hasEffect(com.Harbinger.Spore.core.Seffects.MARKER)&&FungalEcology.prey(living))
                visible.put(living.getUUID(),new Target(living.getUUID(),living.blockPosition()));
        }
        var targets=visible.values().stream().sorted(Comparator.comparingDouble(t->t.pos().distToCenterSqr(player.position()))).limit(128).toList();
        var order=bound&&HiveboundEvolution.stage(player)>=2?HiveNetwork.assignment(player):null;
        PacketDistributor.sendToPlayer(player,new HiveSensePayload(level.dimension().location(),sites,targets,order==null?null:order.id()));
    }
    public static final class ClientState { public static HiveSensePayload current; }
}
