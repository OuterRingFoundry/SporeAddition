package dev.sporebound;

import com.Harbinger.Spore.core.Sentities;
import com.Harbinger.Spore.core.Sitems;
import com.Harbinger.Spore.core.Seffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.GameType;
import java.util.UUID;
import java.util.function.BiConsumer;

public final class HiveboundValidation {
    public static void run(ServerLevel level,BiConsumer<Boolean,String> check){
        double before=CorruptionData.get(level).index();
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
            new com.mojang.authlib.GameProfile(UUID.fromString("30000000-0000-0000-0000-000000000006"),"spore-armor-test"));
        var pos=new BlockPos(232,240,232);var wilds=level.registryAccess().registryOrThrow(Registries.BIOME)
            .getHolderOrThrow(ResourceKey.create(Registries.BIOME,Sporebound.id("blighted_wilds")));
        level.getChunkAt(pos).fillBiomesFromNoise((x,y,z,sampler)->wilds,level.getChunkSource().randomState().sampler());
        player.moveTo(pos.getX(),pos.getY(),pos.getZ());player.setGameMode(GameType.SURVIVAL);
        player.getInventory().clearContent();CorruptionData.get(level).set(6);
        Item[] originals={Sitems.LIVING_HELMET.get(),Sitems.LIVING_CHEST.get(),Sitems.LIVING_PANTS.get(),Sitems.LIVING_BOOTS.get()};
        Item[] upgrades={Hivebound.HELMET.get(),Hivebound.CHEST.get(),Hivebound.LEGS.get(),Hivebound.BOOTS.get()};
        for(int i=0;i<4;i++){
            var base=new ItemStack(originals[i]);base.set(DataComponents.CUSTOM_NAME,Component.literal("Preserved symbiont"));
            base.setDamageValue(12);
            base.enchant(level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.VANISHING_CURSE),1);
            var input=new SmithingRecipeInput(new ItemStack(Sitems.LIVING_CORE.get()),base,new ItemStack(Sitems.REFORGED_BIOMASS_A.get()));
            var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMITHING,input,level);
            check.accept(recipe.isPresent(),"Hivebound smithing recipe loads for "+Hivebound.SLOTS.get(i));
            var result=recipe.orElseThrow().value().assemble(input,level.registryAccess());
            check.accept(result.is(upgrades[i])&&result.getDamageValue()==12&&result.getHoverName().getString().equals("Preserved symbiont"),
                "smithing preserves existing armor components for "+Hivebound.SLOTS.get(i));
            check.accept(result.getMaxDamage()>base.getMaxDamage(),"Hivebound durability improves on living armor for "+Hivebound.SLOTS.get(i));
            player.setItemSlot(Hivebound.SLOTS.get(i),result);
        }
        check.accept(Hivebound.member(player)&&!FungalEcology.prey(player)&&!SporeExposure.hazardous(player),"full Hivebound set grants fungal membership and fog protection");
        var mob=Sentities.INF_HUMAN.get().create(level);mob.moveTo(player.position());
        check.accept(mob.isAlliedTo(player)&&player.isAlliedTo(mob),"Spore and Hivebound wearer recognize each other as allies");
        mob.setTarget(player);check.accept(mob.getTarget()==null,"Spore targeting rejects Hivebound members");
        check.accept(!player.addEffect(new net.minecraft.world.effect.MobEffectInstance(Seffects.MYCELIUM,200)),"fungal infection cannot poison a Hivebound member");
        check.accept(!player.hurt(level.damageSources().mobAttack(mob),4),"Spore friendly fire cannot hurt its Hivebound member");
        Hivebound.update(player);double health=player.getMaxHealth(),damage=player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for(int i=0;i<10;i++)Hivebound.update(player);
        check.accept(player.getMaxHealth()==health&&player.getAttributeValue(Attributes.ATTACK_DAMAGE)==damage,"symbiotic bonuses do not stack each tick");
        CorruptionData.get(level).set(10);Hivebound.update(player);
        check.accept(player.getMaxHealth()>health&&player.getAttributeValue(Attributes.ATTACK_DAMAGE)>damage,"higher Index strengthens the symbiotic player");
        var helmet=player.getItemBySlot(EquipmentSlot.HEAD);var menu=player.inventoryMenu;
        for(var type:new ClickType[]{ClickType.PICKUP,ClickType.QUICK_MOVE,ClickType.SWAP,ClickType.THROW}){
            menu.clicked(5,0,type,player);
            check.accept(player.getItemBySlot(EquipmentSlot.HEAD)==helmet&&menu.getCarried().isEmpty(),"bound armor rejects inventory action "+type);
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_HELMET));
        ((Equipable)Items.DIAMOND_HELMET).swapWithEquipmentSlot(Items.DIAMOND_HELMET,level,player,InteractionHand.MAIN_HAND);
        check.accept(player.getItemBySlot(EquipmentSlot.HEAD)==helmet&&player.getMainHandItem().is(Items.DIAMOND_HELMET),"right-click replacement cannot remove bound armor");
        player.setGameMode(GameType.CREATIVE);
        check.accept(!menu.getSlot(5).mayPickup(player),"creative inventory still respects armor binding");
        player.connection.handleSetCreativeModeSlot(new net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket(5,ItemStack.EMPTY));
        check.accept(player.getItemBySlot(EquipmentSlot.HEAD)==helmet,"creative slot packet cannot delete bound armor");
        player.setGameMode(GameType.SURVIVAL);
        CorruptionData.get(level).set(0);Hivebound.update(player);
        check.accept(Hivebound.locked(player,helmet)&&player.getMaxHealth()<20&&player.getAttributeValue(Attributes.MOVEMENT_SPEED)<0.1,
            "Normal Index zero weakens symbiosis but does not unlock it: health="+player.getMaxHealth()+", speed="+player.getAttributeValue(Attributes.MOVEMENT_SPEED)+", locked="+Hivebound.locked(player,helmet));
        for(int index:new int[]{-1,-2}){
            CorruptionData.get(level).set(index);
            check.accept(!Hivebound.locked(player,helmet)&&menu.getSlot(5).mayPickup(player),"negative dimension Index unlocks armor: "+index);
        }
        menu.clicked(5,0,ClickType.PICKUP,player);
        check.accept(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&menu.getCarried().is(Hivebound.HELMET.get())&&menu.getCarried().getCount()==1,"unlocked armor can actually be removed");
        menu.clicked(5,0,ClickType.PICKUP,player);
        CorruptionData.get(level).set(6);
        try {
            var vanishing=net.minecraft.world.entity.player.Player.class.getDeclaredMethod("destroyVanishingCursedItems");
            vanishing.setAccessible(true);vanishing.invoke(player);
        }catch(ReflectiveOperationException error){throw new RuntimeException(error);}
        player.getInventory().dropAll();
        check.accept(player.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),"death moves bound armor into recovery storage before inventory drops");
        var saved=player.getPersistentData().copy();Hivebound.restore(player,saved);
        check.accept(Hivebound.member(player)&&player.getItemBySlot(EquipmentSlot.HEAD).getHoverName().getString().equals("Preserved symbiont"),
            "death recovery preserves cursed bound armor and its components before vanishing");
        var recovered=player.getItemBySlot(EquipmentSlot.HEAD);Hivebound.restore(player,saved);
        check.accept(recovered==player.getItemBySlot(EquipmentSlot.HEAD),"death recovery consumes its saved payload exactly once");
        player.getPersistentData().remove("sporebound:bound_armor");
        player.getInventory().clearContent();Hivebound.update(player);
        check.accept(!Hivebound.member(player)&&player.getMaxHealth()==20,"removing the set clears membership and scaling");
        CorruptionData.get(level).set(before);
    }
}
