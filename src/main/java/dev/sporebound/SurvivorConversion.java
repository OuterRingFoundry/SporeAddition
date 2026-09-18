package dev.sporebound;

import com.Harbinger.Spore.core.Sentities;
import com.Harbinger.Spore.core.Seffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/** Uses native Infected Adventurers, preserving real equipment without copying death drops. */
public final class SurvivorConversion {
    public static boolean convert(LivingDeathEvent event){
        if(event.isCanceled()||!(event.getEntity() instanceof Survivor survivor)||survivor.isRemoved()
            ||!(survivor.level() instanceof ServerLevel level)||Protection.sterile(level,survivor.blockPosition())
            ||!survivor.hasEffect(Seffects.MYCELIUM))return false;
        var infected=Sentities.INF_PLAYER.get().create(level);if(infected==null)return false;
        infected.moveTo(survivor.getX(),survivor.getY(),survivor.getZ(),survivor.getYRot(),0);
        // Native infected skin enum orders the same nine characters differently.
        infected.setVariant(new int[]{0,1,6,2,7,3,8,4,5}[survivor.skin()]);infected.setCustomName(survivor.getCustomName());infected.setPersistenceRequired();
        infected.setOrigin("sporebound:survivor");
        for(var slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.HUMANOID_ARMOR||slot.getType()==EquipmentSlot.Type.HAND){
            infected.setItemSlot(slot,survivor.getItemBySlot(slot).copy());infected.setDropChance(slot,1);
        }
        if(!level.addFreshEntity(infected))return false;
        for(var slot:EquipmentSlot.values())if(slot.getType()==EquipmentSlot.Type.HUMANOID_ARMOR||slot.getType()==EquipmentSlot.Type.HAND)
            survivor.setItemSlot(slot,ItemStack.EMPTY);
        net.minecraft.world.Containers.dropContents(level,survivor.blockPosition(),survivor.supplies());survivor.supplies().clearContent();
        if(!survivor.miningTool().isEmpty())survivor.spawnAtLocation(survivor.miningTool());survivor.setMiningTool(ItemStack.EMPTY);
        if(event.getSource().getEntity() instanceof net.minecraft.world.entity.LivingEntity killer)
            killer.awardKillScore(survivor,1,event.getSource());
        survivor.discard();return true;
    }
}
