package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.*;
import com.Harbinger.Spore.Sentities.Organoids.Proto;
import com.Harbinger.Spore.SBlockEntities.LivingStructureBlocks;
import com.Harbinger.Spore.core.SConfig;
import com.Harbinger.Spore.core.Sblocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Mirrors Spore's separate evolution and spendable kill counters, stored with the player. */
public final class HiveboundEvolution {
    private static final String DATA="sporebound:evolution",CREDITED="sporebound:evolution_credited";
    public static CompoundTag data(Player player){
        var data=player.getPersistentData();
        if(!data.contains(Player.PERSISTED_NBT_TAG))data.put(Player.PERSISTED_NBT_TAG,new CompoundTag());
        var persisted=data.getCompound(Player.PERSISTED_NBT_TAG);
        if(!persisted.contains(DATA))persisted.put(DATA,new CompoundTag());
        return persisted.getCompound(DATA);
    }
    public static int points(Player player){return Math.clamp(data(player).getInt("EvolutionPoints"),0,EvolutionMath.MAX_POINTS);}
    public static int kills(Player player){return Math.clamp(data(player).getInt("Kills"),0,EvolutionMath.MAX_POINTS);}
    public static int first(){return EvolutionMath.first(SConfig.SERVER.min_kills.get());}
    public static int hyper(){return EvolutionMath.hyper(first(),SConfig.SERVER.min_kills_hyper.get());}
    public static int stage(Player player){return EvolutionMath.stage(points(player),first(),hyper());}
    public static void add(Player player,int points,int kills){
        var data=data(player);data.putInt("EvolutionPoints",EvolutionMath.add(points(player),points));
        data.putInt("Kills",EvolutionMath.add(kills(player),kills));
    }
    /** ServerPlayer's real kill-score callback; conversion callbacks for the same victim are deduplicated. */
    public static boolean award(Player player,Entity victim){
        if(!(player.level() instanceof ServerLevel)||!Hivebound.member(player)||player.isSpectator()
            ||!(victim instanceof LivingEntity)||victim instanceof net.minecraft.world.entity.decoration.ArmorStand
            ||victim==player||victim.getPersistentData().getBoolean(CREDITED))return false;
        victim.getPersistentData().putBoolean(CREDITED,true);add(player,1,1);
        data(player).putInt("TotalKills",EvolutionMath.add(data(player).getInt("TotalKills"),1));
        Hivebound.update(player);return true;
    }
    public static int power(Infected mob){
        return Math.max(Math.max(0,mob.getEvoPoints()),mob instanceof Hyper?hyper():mob instanceof EvolvedInfected?first():0);
    }
    public static boolean dominates(Player player,Infected mob){
        return Hivebound.member(player)&&player.isAlive()&&!player.isSpectator()&&mob.isAlive()
            &&player.level()==mob.level()&&points(player)>power(mob)
            &&player.level() instanceof ServerLevel level&&!Protection.sterile(level,player.blockPosition())
            &&!Protection.sterile(level,mob.blockPosition());
    }
    public static boolean consume(Player player,Infected mob){
        if(!dominates(player,mob)||!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty()
            ||player.distanceToSqr(mob)>9||!player.hasLineOfSight(mob)||mob.isPassenger()||mob.isVehicle())return false;
        int gained=Math.max(1,power(mob)),stored=Math.max(0,mob.getKills());
        int nutrition=mob instanceof Hyper?10:mob instanceof EvolvedInfected?6:3;
        // Discard instead of killing: no death loot, duplicate kill award or fungal conversion.
        mob.setKills(0);mob.setEvoPoints(0);mob.discard();
        add(player,gained,stored);player.getFoodData().eat(nutrition,0.5f);
        player.level().playSound(null,player.blockPosition(),net.minecraft.sounds.SoundEvents.GENERIC_EAT,
            net.minecraft.sounds.SoundSource.PLAYERS,1,0.75f);
        Hivebound.update(player);return true;
    }
    @SubscribeEvent public void interact(PlayerInteractEvent.EntityInteract event){
        if(event.getHand()==InteractionHand.MAIN_HAND&&event.getTarget() instanceof Infected mob
            &&event.getEntity().level() instanceof ServerLevel&&consume(event.getEntity(),mob)){
            event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);
        }
    }
    public static void hungerSecond(Player player){
        if(player.isCreative()||player.isSpectator())return;
        int pieces=0;for(var slot:Hivebound.SLOTS)if(player.getItemBySlot(slot).getItem() instanceof HiveboundArmor)pieces++;
        if(pieces>0)player.causeFoodExhaustion(EvolutionMath.exhaustion(pieces,stage(player)));
    }
    /** Transfer unspent player kill points into the native Hive counter, preserving evolution progress. */
    public static int feedHive(Player player,Proto hive,int maximum){
        if(!Hivebound.member(player)||!player.isAlive()||player.isSpectator()||!hive.isAlive()
            ||player.level()!=hive.level()||player.distanceToSqr(hive)>32*32
            ||!(player.level() instanceof ServerLevel level)||Protection.sterile(level,player.blockPosition())
            ||Protection.sterile(level,hive.blockPosition()))return 0;
        int amount=Math.min(Math.max(0,maximum),kills(player));
        if(amount==0)return 0;
        data(player).putInt("Kills",kills(player)-amount);hive.addBiomass(amount);return amount;
    }
    /** Standing on a native biomass lump or reconstructed mind contributes one saved kill per second. */
    public static boolean feedStructure(Player player){
        if(!Hivebound.member(player)||kills(player)<=1||!(player.level() instanceof ServerLevel level)
            ||Protection.sterile(level,player.blockPosition()))return false;
        for(var pos:java.util.List.of(player.blockPosition(),player.blockPosition().below())){
            var state=level.getBlockState(pos);
            int cap=state.is(Sblocks.BIOMASS_LUMP.get())?SConfig.DATAGEN.biomass_lump_kills.get():
                state.is(Sblocks.HIVE_SPAWN.get())?SConfig.DATAGEN.hive_spawn_kills.get():-1;
            if(cap<0||!(level.getBlockEntity(pos) instanceof LivingStructureBlocks structure)||structure.getKills()>=cap)continue;
            data(player).putInt("Kills",kills(player)-1);structure.addKills();structure.setChanged();return true;
        }
        return false;
    }
}
