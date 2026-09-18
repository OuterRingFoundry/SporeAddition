package dev.sporebound;

import com.Harbinger.Spore.core.Seffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

public final class HiveProgression {
    /** A deliberate two-step ritual: arm at a cairn, then explicitly confirm with the awakening key. */
    public static boolean awaken(ServerPlayer player, BlockPos center) {
        var level=player.serverLevel();var state=HiveboundEvolution.data(player);long now=level.getGameTime();
        if (!player.isShiftKeyDown() || !player.getMainHandItem().is(Sporebound.TALISMAN.get())
            || !player.getOffhandItem().is(Items.AMETHYST_SHARD) || !RiftCairn.complete(level,center)
            || CorruptionData.get(level).index()!=-1 || Protection.mushroom(level,center)
            || player.isSpectator() || !player.isAlive() || !center.closerToCenterThan(player.position(),6))return false;
        if(!state.getString("AwakeningDimension").equals(level.dimension().location().toString())
            ||state.getLong("AwakeningSite")!=center.asLong()||now>state.getLong("AwakeningUntil")
            ||!state.contains("AwakeningUntil")) {
            state.putString("AwakeningDimension",level.dimension().location().toString());state.putLong("AwakeningSite",center.asLong());
            state.putLong("AwakeningUntil",now+200);state.putLong("AwakeningStart",now);
            player.displayClientMessage(Component.literal("Awaken this dimension? Press your Confirm Awakening key (default K) within 10 seconds while staying here. This consumes one amethyst shard and permits Spore infection."),false);
            return true;
        }
        return true;
    }
    public static boolean confirmAwakening(ServerPlayer player) {
        var level=player.serverLevel();var state=HiveboundEvolution.data(player);
        if(!state.contains("AwakeningUntil")||level.getGameTime()>state.getLong("AwakeningUntil")
            ||!state.getString("AwakeningDimension").equals(level.dimension().location().toString()))return false;
        var center=BlockPos.of(state.getLong("AwakeningSite"));
        if(!player.getMainHandItem().is(Sporebound.TALISMAN.get())
            ||!player.getOffhandItem().is(Items.AMETHYST_SHARD)||!RiftCairn.complete(level,center)
            ||CorruptionData.get(level).index()!=-1||Protection.mushroom(level,center)
            ||player.isSpectator()||!player.isAlive()||!center.closerToCenterThan(player.position(),6))return false;
        CorruptionData.get(level).set(0);player.getOffhandItem().shrink(1);state.remove("AwakeningUntil");
        WorldRules.enforce(level);player.displayClientMessage(Component.literal("The dormant Spore awakens. Dimension Index: 0."),false);return true;
    }
    @SubscribeEvent public void ritual(PlayerInteractEvent.RightClickBlock event) {
        if(event.getHand()==InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player
            &&awaken(player,event.getPos())){event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);}
    }
    public static boolean consume(Player player,InfectedBiomass biomass) {
        if(!(player.level() instanceof ServerLevel level)||!Hivebound.member(player)||!player.isAlive()||player.isSpectator()
            ||!player.isShiftKeyDown()||!player.getMainHandItem().isEmpty()||biomass.level()!=level||!biomass.isAlive()
            ||biomass.busy()||biomass.isPassenger()||biomass.isVehicle()||player.distanceToSqr(biomass)>9
            ||!player.hasLineOfSight(biomass)||Protection.sterile(level,player.blockPosition())
            ||Protection.sterile(level,biomass.blockPosition()))return false;
        int mass=biomass.mass();biomass.discard();HiveboundEvolution.add(player,mass,mass);
        player.getFoodData().eat(Math.min(10,mass*2),0.5f);player.heal(mass);Hivebound.update(player);return true;
    }
    @SubscribeEvent public void eat(PlayerInteractEvent.EntityInteract event) {
        if(event.getHand()==InteractionHand.MAIN_HAND&&event.getTarget() instanceof InfectedBiomass biomass
            &&consume(event.getEntity(),biomass)){event.setCancellationResult(InteractionResult.SUCCESS);event.setCanceled(true);}
    }
    @SubscribeEvent public void melee(LivingDamageEvent.Post event) {
        var source=event.getSource();var victim=event.getEntity();
        if(event.getNewDamage()<=0||!(source.getEntity() instanceof ServerPlayer player)||source.getDirectEntity()!=player
            ||!source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||!Hivebound.member(player)
            ||HiveboundEvolution.stage(player)<1||!FungalEcology.prey(victim)
            ||!(victim.level() instanceof ServerLevel level)||Protection.sterile(level,victim.blockPosition())
            ||Protection.sterile(level,player.blockPosition()))return;
        boolean fresh=!victim.hasEffect(Seffects.MYCELIUM);
        if(victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(Seffects.MYCELIUM,600),player)&&fresh) {
            var data=CorruptionData.get(level);data.set(Math.min(10,data.index()+0.01));
            HiveNetwork.report(level,victim);
        }
    }
}
