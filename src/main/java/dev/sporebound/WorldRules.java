package dev.sporebound;

import java.util.ArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WorldRules {
    private static final ResourceLocation HEALTH = Sporebound.id("corruption_health");
    private static final ResourceLocation DAMAGE = Sporebound.id("corruption_damage");
    @SubscribeEvent public void load(net.neoforged.neoforge.event.level.LevelEvent.Load event) {
        if(event.getLevel() instanceof ServerLevel level){CorruptionData.get(level);HivePopulation.get(level).reconcile(level);}
    }
    @SubscribeEvent public void unload(net.neoforged.neoforge.event.level.LevelEvent.Unload event) {
        if(event.getLevel() instanceof ServerLevel level)CorruptionData.release(level);
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void spawnPlacement(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck event) {
        if(!BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntityType()).getNamespace().equals("spore"))return;
        var access=event.getLevel(); var level=access.getLevel();
        boolean mushroom=level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)
            && (access.getBiome(event.getPos()).is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS)
            || access.getBiome(new net.minecraft.core.BlockPos(event.getPos().getX(),64,event.getPos().getZ())).is(net.minecraft.world.level.biome.Biomes.MUSHROOM_FIELDS));
        double index=CorruptionData.peek(level);
        boolean quietGrove=level.dimension().equals(Sporebound.BLIGHT)
            &&access.getBiome(event.getPos()).is(Sporebound.id("remnant_grove"))
            &&CorruptionMath.regional(index,-4)<5;
        if(index<0 || mushroom || quietGrove)
            event.setResult(net.neoforged.neoforge.event.entity.living.MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void join(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (Protection.denyEntity(level, event.getEntity(), !event.loadedFromDisk())) {
            event.setCanceled(true); return;
        }
        if (event.getEntity() instanceof LivingEntity living && Protection.spore(living)) scale(living, RegionalCorruption.at(level,living.blockPosition()));
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void tick(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level)) return;
        if (Protection.spore(entity)) {
            if (Protection.denyEntity(level, entity, false)) {
                entity.discard(); event.setCanceled(true); return;
            }
            if (CorruptionData.get(level).index() < 0) { event.setCanceled(true); return; }
        }
        if (entity instanceof LivingEntity living && Protection.sterile(level, entity.blockPosition())) {
            // Remove carried infection before its next effect tick, including portal arrivals.
            for (var effect : new ArrayList<>(living.getActiveEffects()))
                if (BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).getNamespace().equals("spore"))
                    living.removeEffect(effect.getEffect());
        }
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void effect(MobEffectEvent.Applicable event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && BuiltInRegistries.MOB_EFFECT.getKey(event.getEffectInstance().getEffect().value()).getNamespace().equals("spore")
                && Protection.sterile(level, event.getEntity().blockPosition()))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    @SubscribeEvent public void started(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) CorruptionData.get(level);
        FoundingHives.seed(event.getServer().getLevel(Sporebound.BLIGHT));
    }
    @SubscribeEvent public void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long time = level.getGameTime();
        if (time % 1200 == 0) {
            var data = CorruptionData.get(level);
            if (data.index() > 0 && data.index() < 10) {
                double population = 0;
                for (Entity entity : level.getAllEntities()) if (entity.isAlive() && !Protection.mushroom(level, entity.blockPosition())) population += Protection.weight(entity);
                double next = CorruptionMath.advance(data.index(), population, 1200);
                if (next != data.index()) data.set(next);
            }
            FoundingHives.seed(level);
        }
        if (time % 20 == 0) {
            double index = CorruptionData.get(level).index();
            for (Entity entity : level.getAllEntities()) if (entity instanceof LivingEntity living && Protection.spore(entity)) scale(living, RegionalCorruption.at(level,living.blockPosition()));
            for (ServerPlayer player : level.players()) sync(player);
        }
    }
    @SubscribeEvent public void explosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Entity source = event.getExplosion().getDirectSourceEntity();
        if (source == null || !Protection.spore(source)) return;
        event.getAffectedBlocks().removeIf(pos -> Protection.sterile(level, pos));
        event.getAffectedEntities().removeIf(entity -> Protection.sterile(level, entity.blockPosition()));
    }
    public static void sync(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CorruptionPayload(player.level().dimension().location(),
            CorruptionData.get(player.serverLevel()).index(), Protection.mushroom(player.level(), player.blockPosition()),
            RegionalCorruption.at(player.serverLevel(),player.blockPosition()),RegionalCorruption.name(player.serverLevel(),player.blockPosition())));
    }
    public static void enforce(ServerLevel level) {
        HivePopulation.get(level).reconcile(level);
        var snapshot = new ArrayList<Entity>(); level.getAllEntities().forEach(snapshot::add);
        for (Entity entity : snapshot) {
            if (Protection.denyEntity(level, entity, false)) entity.discard();
            else if (entity instanceof LivingEntity living && Protection.spore(entity)) scale(living, RegionalCorruption.at(level,living.blockPosition()));
        }
        level.players().forEach(WorldRules::sync);
    }
    public static void scale(LivingEntity entity, double index) {
        float fraction = entity.getMaxHealth() > 0 ? entity.getHealth() / entity.getMaxHealth() : 1;
        boolean changed = modifier(entity, Attributes.MAX_HEALTH, HEALTH, CorruptionMath.healthBonus(index));
        modifier(entity, Attributes.ATTACK_DAMAGE, DAMAGE, CorruptionMath.damageBonus(index));
        if (changed) entity.setHealth(Math.min(entity.getMaxHealth(), entity.getMaxHealth() * fraction));
    }
    private static boolean modifier(LivingEntity entity, Holder<Attribute> attribute, ResourceLocation id, double value) {
        var instance = entity.getAttribute(attribute);
        if (instance == null) return false;
        var old = instance.getModifier(id);
        if (old != null && old.amount() == value) return false;
        instance.removeModifier(id);
        if (value != 0) instance.addPermanentModifier(new AttributeModifier(id, value, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        return old != null || value != 0;
    }
}
