package dev.sporebound;

import com.Harbinger.Spore.core.Sblocks;
import com.Harbinger.Spore.core.Sentities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import java.util.Arrays;
import java.util.function.BiConsumer;

public final class HiveBurrowingValidation {
    public static void run(ServerLevel level, BiConsumer<Boolean,String> check) {
        BlockPos pos = new BlockPos(200, 240, 200);
        var wilds = level.registryAccess().registryOrThrow(Registries.BIOME)
            .getHolderOrThrow(ResourceKey.create(Registries.BIOME, Sporebound.id("blighted_wilds")));
        level.getChunkAt(pos).fillBiomesFromNoise((x,y,z,sampler) -> wilds, level.getChunkSource().randomState().sampler());
        for (var block : BlockPos.betweenClosed(pos.offset(-6,-24,-6), pos.offset(6,-1,6)))
            level.setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
        var hive = Sentities.PROTO.get().create(level); hive.moveTo(pos.getX(),pos.getY(),pos.getZ());
        hive.getRandom().setSeed(19281); hive.addBiomass(200);
        double index = CorruptionData.get(level).index();
        CorruptionData.get(level).set(7);
        check.accept(!HiveBurrowing.mature(hive) && HiveBurrowing.grow(hive)==0,"young hives do not burrow");
        HiveBurrowing.data(hive).putInt("Age", HiveBurrowing.MATURITY_TICKS);
        CorruptionData.get(level).set(6);
        check.accept(!HiveBurrowing.mature(hive),"underground growth needs local corruption seven");
        CorruptionData.get(level).set(7);
        int food = hive.getBiomass(); hive.eatBiomass(food);
        check.accept(!HiveBurrowing.mature(hive),"underground growth needs stored biomass");
        hive.addBiomass(food);
        boolean grief = level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING);
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(false,level.getServer());
        check.accept(HiveBurrowing.grow(hive)==0,"mobGriefing=false prevents burrowing");
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,level.getServer());
        int total=0;
        for(int i=0;i<8;i++) {
            int count=HiveBurrowing.grow(hive);check.accept(count<=4,"burrowing respects per-step placement budget");total+=count;
        }
        var roots=HiveBurrowing.data(hive).getLongArray("Roots");
        check.accept(total>4 && roots.length==total && hive.getBiomass()==food-total,
            "underground network spreads and charges exactly one biomass per root");
        check.accept(Arrays.stream(roots).mapToObj(BlockPos::of).anyMatch(p -> p.getY()<pos.getY()-2),
            "mature Hive Mind develops below ground");
        check.accept(Arrays.stream(roots).mapToObj(BlockPos::of).allMatch(p -> level.getBlockState(p).is(Sblocks.ROOTED_BIOMASS.get())),
            "root network consists of native Spore biomass blocks");
        CompoundTag save = new CompoundTag();hive.saveWithoutId(save);
        var loaded = Sentities.PROTO.get().create(level);loaded.load(save);
        check.accept(Arrays.equals(HiveBurrowing.data(loaded).getLongArray("Roots"), roots)
            && HiveBurrowing.mature(loaded),"Hive maturity and underground network survive NBT reload");
        CorruptionData.get(level).set(-1);
        check.accept(HiveBurrowing.grow(hive)==0,"dormancy halts underground spread");
        CorruptionData.get(level).set(7);
        hive.moveTo(pos.getX()+4,pos.getY(),pos.getZ()+4);
        level.setBlockAndUpdate(hive.blockPosition().below(),Blocks.BEDROCK.defaultBlockState());
        check.accept(HiveBurrowing.grow(hive)==0,"burrowing cannot replace bedrock after a Hive moves");
        level.setBlockAndUpdate(hive.blockPosition().below(),Blocks.CHEST.defaultBlockState());
        check.accept(HiveBurrowing.grow(hive)==0,"burrowing preserves containers");
        CorruptionData.get(level).set(8);
        var first=Sentities.PROTO.get().create(level);first.moveTo(pos.getX()-4,pos.getY(),pos.getZ()-4);first.setNoAi(true);first.addBiomass(400);
        var second=Sentities.PROTO.get().create(level);second.moveTo(pos.getX()+4,pos.getY(),pos.getZ()-4);second.setNoAi(true);second.addBiomass(400);
        HiveBurrowing.data(first).putInt("Age",HiveBurrowing.MATURITY_TICKS);
        HiveBurrowing.data(second).putInt("Age",HiveBurrowing.MATURITY_TICKS);
        check.accept(level.addFreshEntity(first)&&level.addFreshEntity(second),"connection fixture adds two mature Hive Minds");
        int before=first.getBiomass(),written=0;
        for(int i=0;i<16&&!HiveBurrowing.data(first).getBoolean("Connected");i++)written+=HiveConnections.grow(first,4);
        long[] link=HiveBurrowing.data(first).getLongArray("LinkPath");
        check.accept(HiveBurrowing.data(first).getBoolean("Connected")&&link.length>8
            &&HiveBurrowing.data(first).getUUID("Peer").equals(second.getUUID()),"nearby Hive Minds establish an underground connection");
        check.accept(first.getBiomass()==before-written && Arrays.stream(link).mapToObj(BlockPos::of)
            .allMatch(p->level.getBlockState(p).is(Sblocks.ROOTED_BIOMASS.get())),"Hive link pays for each placed tendril block");
        second.eatBiomass(second.getBiomass()-40);
        int sum=first.getBiomass()+second.getBiomass();
        check.accept(HiveResources.connected(first,second)&&HiveResources.connected(second,first),"completed Hive links carry resources in both directions");
        int sent=HiveResources.share(first);
        check.accept(sent==20&&second.getBiomass()==60&&first.getBiomass()+second.getBiomass()==sum,
            "Hive transfer uses native biomass points and conserves total resources");
        first.setHealth(first.getMaxHealth()/2);
        check.accept(HiveResources.share(first)==0,"injured Hive keeps biomass for its own development");
        first.setHealth(first.getMaxHealth());
        first.setTarget(net.minecraft.world.entity.EntityType.COW.create(level));
        check.accept(HiveResources.share(first)==0,"Hive in combat retains its biomass reserve");first.setTarget(null);
        var broken=BlockPos.of(link[link.length/2]);
        level.setBlockAndUpdate(broken,Blocks.AIR.defaultBlockState());
        check.accept(!HiveResources.connected(first,second)&&HiveResources.share(first)==0,"broken tendrils cannot transport biomass");
        level.setBlockAndUpdate(broken,Sblocks.ROOTED_BIOMASS.get().defaultBlockState());
        second.moveTo(second.getX()+1,second.getY(),second.getZ());
        check.accept(!HiveResources.connected(first,second),"moved Hive cannot use its old connection");
        second.moveTo(second.getX()-1,second.getY(),second.getZ());
        for(int i=0;i<30;i++)HiveResources.share(first);
        check.accept(first.getBiomass()>=HiveResources.RESERVE&&second.getBiomass()<=HiveResources.RESERVE
            &&first.getBiomass()+second.getBiomass()==sum,"repeated sharing retains development reserve without minting points");
        first.eatBiomass(first.getBiomass()-20);
        var host=Sentities.INF_HUMAN.get().create(level);host.moveTo(first.position());host.setNoAi(true);host.setKills(7);
        check.accept(level.addFreshEntity(host),"native biomass collection fixture joins");
        check.accept(HiveResources.gather(first)==7&&first.getBiomass()==27&&host.getKills()==0&&host.isAlive(),
            "developing Hive gathers native kill points exactly once without consuming a productive host");
        host.discard();
        first.addBiomass(300);second.addBiomass(200);
        CompoundTag networkSave=new CompoundTag();first.saveWithoutId(networkSave);
        var restoredNetwork=Sentities.PROTO.get().create(level);restoredNetwork.load(networkSave);
        check.accept(Arrays.equals(HiveBurrowing.data(restoredNetwork).getLongArray("LinkPath"),link)
            &&HiveBurrowing.data(restoredNetwork).getUUID("Peer").equals(second.getUUID()),"Hive connection route and peer survive save/reload");
        second.discard();HiveConnections.grow(first,4);
        check.accept(!HiveBurrowing.data(first).hasUUID("Peer"),"removed Hive peer is released without forced chunk loading");first.discard();
        level.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(grief,level.getServer());
        CorruptionData.get(level).set(index);
    }
}
