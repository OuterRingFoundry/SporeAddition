package dev.sporebound;

import com.Harbinger.Spore.Sentities.Organoids.Proto;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/** One shared intelligence board per dimension; never loads chunks to find targets. */
public final class HiveNetwork {
    public record Mark(UUID id, BlockPos pos, double threat, long expires) {}
    private static final Map<ServerLevel, LinkedHashMap<UUID, Mark>> BOARDS = new WeakHashMap<>();
    public static List<Mark> marks(ServerLevel level) {
        return List.copyOf(BOARDS.getOrDefault(level, new LinkedHashMap<>()).values());
    }
    public static boolean active(ServerLevel level) {
        return CorruptionData.get(level).index() >= 5 && HivePopulation.get(level).count() > 0;
    }
    public static void report(ServerLevel level, LivingEntity target) {
        if (!active(level) || !FungalEcology.prey(target)) return;
        var board = BOARDS.computeIfAbsent(level, k -> new LinkedHashMap<>());
        double damage = target.getAttribute(Attributes.ATTACK_DAMAGE)==null?0:target.getAttributeValue(Attributes.ATTACK_DAMAGE);
        board.put(target.getUUID(), new Mark(target.getUUID(), target.blockPosition(),
            HiveTactics.threat(target.getMaxHealth(), damage), level.getGameTime() + HiveTactics.MARK_SECONDS * 20));
        while (board.size() > 128) board.remove(board.keySet().iterator().next());
    }
    public static void update(ServerLevel level) {
        var board = BOARDS.computeIfAbsent(level, k -> new LinkedHashMap<>());
        if (!active(level)) {
            board.clear();
            for(var e:level.getAllEntities())if(e instanceof Mob mob)mob.getPersistentData().remove("sporebound:order");
            return;
        }
        var troops = new ArrayList<Mob>();
        for (var entity : level.getAllEntities()) {
            if (entity instanceof Proto hive && hive.isAlive()) HiveNodes.get(level).rememberHive(hive);
            if (!(entity instanceof Mob mob) || !Protection.spore(mob) || mob instanceof Proto
                || !mob.isAlive() || mob.isNoAi() || Protection.sterile(level,mob.blockPosition())) continue;
            troops.add(mob);
            var target = mob.getTarget();
            if (target != null && mob.hasLineOfSight(target)) report(level,target);
        }
        board.values().removeIf(mark -> mark.expires <= level.getGameTime()
            || !(level.getEntity(mark.id) instanceof LivingEntity target) || !FungalEcology.prey(target));
        var assigned = new HashMap<UUID,Integer>();
        // Strong units receive orders first; threat, distance and current staffing distribute the rest.
        troops.sort(Comparator.comparingDouble((Mob mob) -> mob.getMaxHealth()).reversed());
        for (var mob : troops) {
            Mark best = null; double score = -1;
            for (var mark : board.values()) {
                double value = HiveTactics.assignment(mark.threat, mark.pos.distToCenterSqr(mob.position()), assigned.getOrDefault(mark.id,0));
                if (value > score) { best = mark; score = value; }
            }
            if (best == null) { mob.getPersistentData().remove("sporebound:order"); continue; }
            if (level.getEntity(best.id) instanceof LivingEntity target) {
                mob.getPersistentData().putUUID("sporebound:order", best.id);
                mob.setTarget(target);
                if (mob.distanceToSqr(target) > 16) mob.getNavigation().moveTo(target,1.1);
                assigned.merge(best.id,1,Integer::sum);
            }
        }
    }
    public static Mark assignment(ServerPlayer player) {
        return marks(player.serverLevel()).stream().max(Comparator.comparingDouble(mark ->
            HiveTactics.assignment(mark.threat, mark.pos.distToCenterSqr(player.position()),0))).orElse(null);
    }
    @SubscribeEvent public void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime()%20 != 0) return;
        update(level);
        for (var player : level.players()) {
            if (Hivebound.member(player)) HiveNodes.get(level).discover(level,player.blockPosition());
            HiveSensePayload.sync(player);
            InfusionPayload.sync(player);
        }
    }
}
