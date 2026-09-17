package dev.sporebound;

import com.Harbinger.Spore.core.Seffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import java.util.List;
import java.util.function.Supplier;

public final class Hivebound {
    public static final Supplier<Item> HELMET=Sporebound.ITEMS.register("hivebound_helmet",()->new HiveboundArmor(ArmorItem.Type.HELMET));
    public static final Supplier<Item> CHEST=Sporebound.ITEMS.register("hivebound_chestplate",()->new HiveboundArmor(ArmorItem.Type.CHESTPLATE));
    public static final Supplier<Item> LEGS=Sporebound.ITEMS.register("hivebound_leggings",()->new HiveboundArmor(ArmorItem.Type.LEGGINGS));
    public static final Supplier<Item> BOOTS=Sporebound.ITEMS.register("hivebound_boots",()->new HiveboundArmor(ArmorItem.Type.BOOTS));
    public static final List<EquipmentSlot> SLOTS=List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET);
    private static final String SAVED="sporebound:bound_armor";
    private static final ResourceLocation HEALTH=Sporebound.id("symbiotic_health"),DAMAGE=Sporebound.id("symbiotic_damage"),SPEED=Sporebound.id("symbiotic_speed");
    public static void register(net.neoforged.bus.api.IEventBus bus){
        bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event)->{
            if(event.getTabKey().equals(CreativeModeTabs.COMBAT))for(var item:List.of(HELMET,CHEST,LEGS,BOOTS))event.accept(item.get());
        });
    }
    public static boolean member(Entity entity){
        if(!(entity instanceof Player player))return false;
        for(var slot:SLOTS)if(!(player.getItemBySlot(slot).getItem() instanceof HiveboundArmor))return false;
        return true;
    }
    public static boolean locked(Player player,ItemStack stack){
        if(!(stack.getItem() instanceof HiveboundArmor))return false;
        double index;
        if(player.level() instanceof ServerLevel level)index=CorruptionData.get(level).index();
        else {
            var state=CorruptionPayload.ClientState.current;
            if(state==null||!state.dimension().equals(player.level().dimension().location()))return true;
            index=state.index();
        }
        return index!=-1&&index!=-2;
    }
    /** Health and damage scale with local pressure; clean dimensions weaken the full symbiosis. */
    public static void update(Player player){
        if(!(player.level() instanceof ServerLevel level))return;
        boolean member=member(player);
        double index=Protection.sterile(level,player.blockPosition())?0:RegionalCorruption.at(level,player.blockPosition());
        double evolution=EvolutionMath.bonus(HiveboundEvolution.points(player));
        double health=member?(index>0?index*0.10+evolution*0.05:-0.20):0;
        double damage=member?(index>0?index*0.075+evolution*0.025:-0.25):0;
        double speed=member?(index>0?index*0.015+HiveboundEvolution.stage(player)*0.025:-0.15):0;
        float oldMaximum=player.getMaxHealth(),fraction=player.getHealth()/oldMaximum;
        String owned="sporebound:armor_symbiosis";
        if(!member||index<=0){
            if(player.getPersistentData().getBoolean(owned))player.removeEffect(Seffects.SYMBIOSIS);
            player.getPersistentData().remove(owned);
        }else {
            for(var effect:List.copyOf(player.getActiveEffects()))
                if(BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()).getNamespace().equals("spore")
                    &&!effect.getEffect().value().isBeneficial())player.removeEffect(effect.getEffect());
            var symbiosis=player.getEffect(Seffects.SYMBIOSIS);
            if(symbiosis==null||(player.getPersistentData().getBoolean(owned)&&symbiosis.getDuration()<20)){
                player.addEffect(new MobEffectInstance(Seffects.SYMBIOSIS,40,0,false,false));
                player.getPersistentData().putBoolean(owned,true);
            }
        }
        modifier(player,Attributes.MAX_HEALTH,HEALTH,health);
        modifier(player,Attributes.ATTACK_DAMAGE,DAMAGE,damage);modifier(player,Attributes.MOVEMENT_SPEED,SPEED,speed);
        if(player.getMaxHealth()!=oldMaximum)player.setHealth(Math.min(player.getMaxHealth(),player.getMaxHealth()*fraction));
    }
    private static boolean modifier(Player player,Holder<Attribute> attribute,ResourceLocation id,double value){
        var instance=player.getAttribute(attribute);if(instance==null)return false;
        var old=instance.getModifier(id);if(old!=null&&old.amount()==value)return false;
        instance.removeModifier(id);
        if(value!=0)instance.addPermanentModifier(new AttributeModifier(id,value,AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        return old!=null||value!=0;
    }
    @SubscribeEvent public void tick(EntityTickEvent.Pre event){
        if(event.getEntity().level().isClientSide)return;
        if(event.getEntity() instanceof Player player){
            update(player);
            if(player.tickCount%20==0){HiveboundEvolution.hungerSecond(player);HiveboundEvolution.feedStructure(player);}
        }
        if(event.getEntity() instanceof Mob mob&&Protection.spore(mob)&&member(mob.getTarget()))mob.setTarget(null);
    }
    @SubscribeEvent public void damage(LivingIncomingDamageEvent event){
        var source=event.getSource();
        if(member(event.getEntity())&&((source.getEntity()!=null&&Protection.spore(source.getEntity()))
            ||(source.getDirectEntity()!=null&&Protection.spore(source.getDirectEntity()))))event.setCanceled(true);
    }
    @SubscribeEvent public void effect(MobEffectEvent.Applicable event){
        if(member(event.getEntity())&&!event.getEffectInstance().getEffect().value().isBeneficial()
            &&BuiltInRegistries.MOB_EFFECT.getKey(event.getEffectInstance().getEffect().value()).getNamespace().equals("spore"))
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }
    /** Called just before vanilla drops the inventory. Saved stacks never also become item entities. */
    public static void retainOnDeath(Player player){
        if(player.level().isClientSide)return;
        ListTag armor=new ListTag();
        for(var slot:SLOTS){var stack=player.getItemBySlot(slot);if(!locked(player,stack))continue;
            CompoundTag entry=new CompoundTag();entry.putString("Slot",slot.getName());entry.put("Item",stack.save(player.registryAccess()));
            armor.add(entry);player.setItemSlot(slot,ItemStack.EMPTY);
        }
        if(!armor.isEmpty())player.getPersistentData().put(SAVED,armor);
    }
    public static void restore(Player player,CompoundTag data){
        if(!data.contains(SAVED))return;
        var armor=data.getList(SAVED,Tag.TAG_COMPOUND);
        for(int i=0;i<armor.size();i++){
            var entry=armor.getCompound(i);var slot=EquipmentSlot.byName(entry.getString("Slot"));
            var stack=ItemStack.parseOptional(player.registryAccess(),entry.getCompound("Item"));
            if(stack.isEmpty())continue;
            var existing=player.getItemBySlot(slot);
            if(!existing.isEmpty()&&!player.getInventory().add(existing))player.drop(existing,false);
            player.setItemSlot(slot,stack);
        }
        data.remove(SAVED);
    }
    @SubscribeEvent public void clone(PlayerEvent.Clone event){if(event.isWasDeath())restore(event.getEntity(),event.getOriginal().getPersistentData());}
    @SubscribeEvent public void login(PlayerEvent.PlayerLoggedInEvent event){restore(event.getEntity(),event.getEntity().getPersistentData());}
}
