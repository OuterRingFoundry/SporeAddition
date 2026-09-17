package dev.sporebound;

import com.Harbinger.Spore.Sitems.BaseWeapons.SporeBaseArmor;
import com.Harbinger.Spore.Sitems.CustomModelArmorData;
import com.Harbinger.Spore.core.SConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.chat.Component;
import java.util.List;

/** An enhanced native biological exoskeleton; mutations, repair and component upgrades still work. */
public final class HiveboundArmor extends SporeBaseArmor implements CustomModelArmorData {
    public HiveboundArmor(Type type) {
        super(type,boost(new int[]{SConfig.SERVER.boots_durability2.get(),SConfig.SERVER.pants_durability2.get(),
            SConfig.SERVER.chestplate_durability2.get(),SConfig.SERVER.helmet_durability2.get()}),
            boost(new int[]{SConfig.SERVER.boots_protection2.get(),SConfig.SERVER.pants_protection2.get(),
            SConfig.SERVER.chestplate_protection2.get(),SConfig.SERVER.helmet_protection2.get()}),
            SConfig.SERVER.armor_toughness2.get()*1.25f,SConfig.SERVER.knockback_resistance2.get());
    }
    private static int[] boost(int[] values){for(int i=0;i<values.length;i++)values[i]=(int)Math.ceil(values[i]*1.25);return values;}
    @Override public ResourceLocation getTextureLocation(){return ResourceLocation.parse("spore:textures/armor/flesh_armor_set.png");}
    @Override public boolean isFoil(ItemStack stack){return true;}
    @Override public <T extends LivingEntity> int damageItem(ItemStack stack,int amount,T entity,java.util.function.Consumer<Item> broken){
        return Math.min(super.damageItem(stack,amount,entity,broken),Math.max(0,stack.getMaxDamage()-stack.getDamageValue()-10));
    }
    @Override public void appendHoverText(ItemStack stack,Item.TooltipContext context,List<Component> text,TooltipFlag flag){
        text.add(Component.translatable("item.sporebound.hivebound.binding").withStyle(net.minecraft.ChatFormatting.RED));
        text.add(Component.translatable("item.sporebound.hivebound.symbiosis").withStyle(net.minecraft.ChatFormatting.DARK_GREEN));
        text.add(Component.translatable("item.sporebound.hivebound.evolution").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack,context,text,flag);
    }
}
