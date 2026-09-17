package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.Sentities.BaseEntities.UtilityEntity;
import com.Harbinger.Spore.Sentities.TrueCalamity;
import com.Harbinger.Spore.ExtremelySusThings.CustomJsonReader.SporeMobConversionData;
import com.Harbinger.Spore.core.SConfig;
import com.Harbinger.Spore.core.Seffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Adds prey selection without replacing Spore's movement, combat or evolution AI. */
public final class FungalEcology {
    public static void installTargetPolicy() {
        com.Harbinger.Spore.ExtremelySusThings.Utilities.TARGET_SELECTOR =
            new com.Harbinger.Spore.ExtremelySusThings.BooleanCache<LivingEntity>(8, FungalEcology::prey) {
                // Eligibility changes when a creature enters sanctuary or a player changes mode.
                @Override public boolean Test(LivingEntity entity) { return entity != null && prey(entity); }
            };
    }
    public static void prepareConversion(LivingDeathEvent event) {
        var victim=event.getEntity();var attacker=event.getSource().getEntity();
        if (!event.isCanceled() && victim.level() instanceof ServerLevel level && attacker != null
                && Protection.spore(attacker) && !Protection.spore(victim)
                && !Protection.sterile(level,victim.blockPosition()) && victim instanceof Mob)
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(Seffects.MYCELIUM, 600));
    }
    public static boolean prey(LivingEntity entity) {
        return entity.isAlive() && !entity.isInvulnerable() && !entity.isSpectator()
            && !(entity instanceof ArmorStand) && !Protection.spore(entity) && !Hivebound.member(entity)
            && !(entity instanceof UtilityEntity) && !(entity instanceof TrueCalamity)
            && !(entity instanceof Player player && player.isCreative())
            && (!(entity.level() instanceof ServerLevel level) || !Protection.sterile(level, entity.blockPosition()));
    }
    @SubscribeEvent public void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel) || event.isCanceled()
                || !(event.getEntity() instanceof Mob mob) || !Protection.spore(mob)
                || mob instanceof InfectedBiomass) return;
        if(mob.targetSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof HiveOrderGoal))
            mob.targetSelector.addGoal(-1,new HiveOrderGoal(mob));
        if(mob instanceof Infected infected&&mob.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof SporeConsolidationGoal))
            mob.goalSelector.addGoal(2,new SporeConsolidationGoal(infected));
        // Runtime marker, not persistent NBT: goals must be reinstalled after a save reload.
        if (mob.targetSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof AllCreatureTarget))
            mob.targetSelector.addGoal(0, new AllCreatureTarget(mob));
        if (mob instanceof Infected infected
                && mob.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof BiomassFeedingGoal))
            mob.goalSelector.addGoal(2, new BiomassFeedingGoal(infected));
        if(mob instanceof Infected infected && mob.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof HiveboundFollowGoal))
            mob.goalSelector.addGoal(4,new HiveboundFollowGoal(infected));
        if(mob instanceof Infected infected && BiomassAssimilationGoal.basic(infected)
                && mob.goalSelector.getAvailableGoals().stream().noneMatch(g->g.getGoal() instanceof BiomassAssimilationGoal))
            mob.goalSelector.addGoal(3,new BiomassAssimilationGoal(infected));
    }
    @SubscribeEvent public void idle(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if(event.getEntity() instanceof Infected infected && infected.level() instanceof ServerLevel level
                && !infected.isNoAi() && infected.isAlive() && infected.tickCount%20==0
                && !Protection.sterile(level,infected.blockPosition())) {BiomassAssimilationGoal.age(infected);SporeConsolidationGoal.age(infected);}
    }
    private static final class AllCreatureTarget extends NearestAttackableTargetGoal<LivingEntity> {
        AllCreatureTarget(Mob mob) { super(mob, LivingEntity.class, 10, true, false, FungalEcology::prey); }
    }
    @SubscribeEvent public void target(LivingChangeTargetEvent event) {
        if (Protection.spore(event.getEntity()) && event.getNewAboutToBeSetTarget() != null
                && !prey(event.getNewAboutToBeSetTarget())) event.setCanceled(true);
    }
    public static boolean hasConversion(LivingEntity victim) {
        if (victim instanceof Player || victim instanceof net.minecraft.world.entity.animal.IronGolem) return true;
        if (SporeMobConversionData.getResult(victim.getType()) != null) return true;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        for (String entry : SConfig.SERVER.inf_human_conv.get()) {
            String[] pair = entry.split("\\|", 2);
            if (pair.length == 2 && pair[0].equals(id)) {
                ResourceLocation result = ResourceLocation.tryParse(pair[1]);
                if (result != null && BuiltInRegistries.ENTITY_TYPE.containsKey(result)) return true;
            }
        }
        return false;
    }
    public static boolean convertUnmatched(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || victim.isRemoved() || !(victim.level() instanceof ServerLevel level)
                || !(victim instanceof Mob) || Protection.spore(victim)
                || Protection.sterile(level, victim.blockPosition()) || hasConversion(victim)
                || (!victim.hasEffect(Seffects.MYCELIUM)
                    && (event.getSource().getEntity() == null || !Protection.spore(event.getSource().getEntity())))) return false;
        int remaining = BiomassMath.fromHealth(victim.getMaxHealth());
        var created = new java.util.ArrayList<InfectedBiomass>();
        // Stage every lump before removing the victim. A rejected spawn rolls back the conversion.
        while (remaining > 0) {
            InfectedBiomass biomass = FungalContent.BIOMASS.get().create(level);
            if (biomass == null) { created.forEach(Entity::discard); return false; }
            biomass.setMass(Math.min(remaining, BiomassMath.MAX_SIZE_MASS));
            biomass.moveTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0);
            biomass.setCustomName(victim.getCustomName());
            biomass.setPersistenceRequired();
            biomass.setOrigin(BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString());
            if (!level.addFreshEntity(biomass)) { created.forEach(Entity::discard); return false; }
            created.add(biomass);
            remaining -= biomass.mass();
        }
        victim.discard();
        return true;
    }
}
