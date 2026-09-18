package dev.sporebound;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.item.*;
import net.minecraft.tags.ItemTags;

/** Bounded progression and finite recipes: every upgrade consumes carried supplies. */
public final class SurvivorProgression {
    private static final net.minecraft.resources.ResourceLocation HEALTH=Sporebound.id("survivor_health"),DAMAGE=Sporebound.id("survivor_damage");
    public static int level(int experience){return experience>=96?3:experience>=48?2:experience>=16?1:0;}
    public static void refresh(Survivor s){
        int tier=level(s.experience());float old=s.getMaxHealth();
        modifier(s,Attributes.MAX_HEALTH,HEALTH,tier*4);modifier(s,Attributes.ATTACK_DAMAGE,DAMAGE,tier);
        if(s.getMaxHealth()>old)s.heal(s.getMaxHealth()-old);
    }
    private static void modifier(Survivor s,net.minecraft.core.Holder<Attribute> type,net.minecraft.resources.ResourceLocation id,double amount){
        var a=s.getAttribute(type);var old=a.getModifier(id);if(old!=null&&old.amount()==amount)return;
        a.removeModifier(id);if(amount>0)a.addPermanentModifier(new AttributeModifier(id,amount,AttributeModifier.Operation.ADD_VALUE));
    }
    private static boolean spend(SimpleContainer inventory,Item item,int count){
        if(inventory.countItem(item)<count)return false;
        for(int i=0;i<inventory.getContainerSize()&&count>0;i++){var stack=inventory.getItem(i);if(stack.is(item)){
            int used=Math.min(count,stack.getCount());stack.shrink(used);count-=used;}}
        return true;
    }
    private static int sword(ItemStack s){
        if(s.is(Items.NETHERITE_SWORD))return 5;if(s.is(Items.DIAMOND_SWORD))return 4;
        if(s.is(Items.IRON_SWORD))return 3;if(s.is(Items.STONE_SWORD))return 2;
        return s.getItem() instanceof SwordItem?1:0;
    }
    public static void improve(Survivor s){
        var inv=s.supplies();
        // Equip real recovered gear before spending materials. Replaced gear goes back into supplies.
        for(int i=0;i<inv.getContainerSize();i++){
            var stack=inv.getItem(i);EquipmentSlot slot=null;
            if(stack.getItem() instanceof ArmorItem armor&&armor.getDefense()>
                    (s.getItemBySlot(armor.getEquipmentSlot()).getItem() instanceof ArmorItem old?old.getDefense():0))slot=armor.getEquipmentSlot();
            else if(!s.usingBow()&&sword(stack)>sword(s.getMainHandItem()))slot=EquipmentSlot.MAINHAND;
            else if(stack.is(Items.SHIELD)&&s.getOffhandItem().isEmpty())slot=EquipmentSlot.OFFHAND;
            if(slot!=null){var old=s.getItemBySlot(slot);s.setItemSlot(slot,inv.removeItem(i,1));s.setDropChance(slot,1);
                if(!old.isEmpty()){var left=inv.addItem(old);if(!left.isEmpty())s.spawnAtLocation(left);}}
        }
        int remainingPlanks=0;for(int i=0;i<inv.getContainerSize();i++)if(inv.getItem(i).is(ItemTags.PLANKS))remainingPlanks+=inv.getItem(i).getCount();
        if(s.getOffhandItem().isEmpty()&&remainingPlanks>=6&&inv.countItem(Items.IRON_INGOT)>0){
            spend(inv,Items.IRON_INGOT,1);int needed=6;
            for(int i=0;i<inv.getContainerSize()&&needed>0;i++)if(inv.getItem(i).is(ItemTags.PLANKS)){
                int n=Math.min(needed,inv.getItem(i).getCount());inv.removeItem(i,n);needed-=n;}
            s.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));s.setDropChance(EquipmentSlot.OFFHAND,1);
        }
        // Two planks make four sticks, matching the vanilla material cost.
        int planks=0;for(int i=0;i<inv.getContainerSize();i++)if(inv.getItem(i).is(ItemTags.PLANKS))planks+=inv.getItem(i).getCount();
        if(inv.countItem(Items.STICK)<(s.archer()?3:2)&&planks>=2&&inv.canAddItem(new ItemStack(Items.STICK,4))){
            int remaining=2;for(int i=0;i<inv.getContainerSize()&&remaining>0;i++)if(inv.getItem(i).is(ItemTags.PLANKS)){
                int n=Math.min(remaining,inv.getItem(i).getCount());inv.removeItem(i,n);remaining-=n;}
            inv.addItem(new ItemStack(Items.STICK,4));
        }
        // A carried furnace is made once; saved fuel smelts eight ore per coal, one ore every five seconds.
        if(!s.hasFurnace()&&inv.countItem(Items.RAW_IRON)>0&&spend(inv,Items.COBBLESTONE,8))s.makeFurnace();
        if(s.hasFurnace()&&inv.countItem(Items.RAW_IRON)>0&&inv.canAddItem(new ItemStack(Items.IRON_INGOT))){
            if(s.fuel()==0&&(spend(inv,Items.COAL,1)||spend(inv,Items.CHARCOAL,1)))s.addFuel(8);
            if(s.fuel()>0&&spend(inv,Items.RAW_IRON,1)){s.addFuel(-1);inv.addItem(new ItemStack(Items.IRON_INGOT));s.learn(1);}
        }
        if(s.miningTool().isEmpty()&&inv.countItem(Items.COBBLESTONE)>=3&&inv.countItem(Items.STICK)>=2){
            spend(inv,Items.COBBLESTONE,3);spend(inv,Items.STICK,2);s.setMiningTool(new ItemStack(Items.STONE_PICKAXE));
        }
        if(!s.miningTool().is(Items.IRON_PICKAXE)&&!s.miningTool().is(Items.DIAMOND_PICKAXE)
                &&inv.countItem(Items.IRON_INGOT)>=3&&inv.countItem(Items.STICK)>=2){
            spend(inv,Items.IRON_INGOT,3);spend(inv,Items.STICK,2);s.setMiningTool(new ItemStack(Items.IRON_PICKAXE));
        }
        if(s.archer()&&!s.getMainHandItem().is(Items.BOW)&&inv.countItem(Items.BOW)==0
            &&inv.countItem(Items.STRING)>=3&&inv.countItem(Items.STICK)>=3&&inv.canAddItem(new ItemStack(Items.BOW))){
            spend(inv,Items.STRING,3);spend(inv,Items.STICK,3);inv.addItem(new ItemStack(Items.BOW));
        }
        if(s.archer()&&inv.countItem(Items.ARROW)<16&&inv.countItem(Items.FLINT)>0&&inv.countItem(Items.FEATHER)>0
            &&inv.countItem(Items.STICK)>0&&inv.canAddItem(new ItemStack(Items.ARROW,4))){
            spend(inv,Items.FLINT,1);spend(inv,Items.FEATHER,1);spend(inv,Items.STICK,1);inv.addItem(new ItemStack(Items.ARROW,4));
        }
        SurvivorCombat.equipWeapon(s);
        Item material=inv.countItem(Items.DIAMOND)>=2?Items.DIAMOND:Items.IRON_INGOT;
        Item weapon=material==Items.DIAMOND?Items.DIAMOND_SWORD:Items.IRON_SWORD;
        if(!s.usingBow()&&sword(s.getMainHandItem())<sword(new ItemStack(weapon))&&inv.countItem(material)>=2&&inv.countItem(Items.STICK)>=1){
            spend(inv,material,2);spend(inv,Items.STICK,1);var old=s.getMainHandItem();s.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(weapon));
            s.setDropChance(EquipmentSlot.MAINHAND,1);var left=inv.addItem(old);if(!left.isEmpty())s.spawnAtLocation(left);
        }
        var slots=new EquipmentSlot[]{EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.HEAD,EquipmentSlot.FEET};
        var items=new Item[]{Items.IRON_CHESTPLATE,Items.IRON_LEGGINGS,Items.IRON_HELMET,Items.IRON_BOOTS};
        int[] costs={8,7,5,4};
        for(int i=0;i<slots.length;i++)if(!(s.getItemBySlot(slots[i]).getItem() instanceof ArmorItem armor)
                ||armor.getDefense()<((ArmorItem)items[i]).getDefense()){
            if(spend(inv,Items.IRON_INGOT,costs[i])){var old=s.getItemBySlot(slots[i]);s.setItemSlot(slots[i],new ItemStack(items[i]));
                s.setDropChance(slots[i],1);var left=inv.addItem(old);if(!left.isEmpty())s.spawnAtLocation(left);}break;
        }
        refresh(s);
    }
}
