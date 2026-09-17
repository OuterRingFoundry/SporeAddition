package dev.sporebound.client;
import dev.sporebound.*;
import com.Harbinger.Spore.Client.ArmorModelList;
import com.Harbinger.Spore.Client.ArmorParts.*;
import com.Harbinger.Spore.core.Sitems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import java.util.List;
@EventBusSubscriber(modid=Sporebound.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class HiveboundRendering {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->{
        for(var part:List.copyOf(ArmorModelList.ARMOR_RENDERING_BITS)){
            Item item=part.item==Sitems.LIVING_HELMET.get()?Hivebound.HELMET.get():part.item==Sitems.LIVING_CHEST.get()?Hivebound.CHEST.get():
                part.item==Sitems.LIVING_PANTS.get()?Hivebound.LEGS.get():part.item==Sitems.LIVING_BOOTS.get()?Hivebound.BOOTS.get():null;
            if(item!=null)ArmorModelList.ARMOR_RENDERING_BITS.add(new BoundPart(part,item));
        }
    });}
    public static final class BoundPart extends BaseArmorRenderingBit {
        private final BaseArmorRenderingBit original;
        BoundPart(BaseArmorRenderingBit p,Item item){
            // Native pelvis uses a chest attachment, but the equipped item belongs to LEGS.
            super(item==Hivebound.LEGS.get()?EquipmentSlot.LEGS:p.slot,item,p.model,p.part,p.x,p.y,p.z,p.expand*1.025f,p.Xspin,p.Yspin,p.Zspin);original=p;
        }
        @Override protected ModelPart getPiece(HumanoidModel<LivingEntity> model){
            if(original instanceof HelmetArmorPart)return model.head;
            if(original instanceof LeftArmArmorPart)return model.leftArm;
            if(original instanceof RightArmArmorPart)return model.rightArm;
            if(original instanceof LeftLegArmorPart||original instanceof LeftBootArmorPart)return model.leftLeg;
            if(original instanceof RightLegArmorPart||original instanceof RightBootArmorPart)return model.rightLeg;
            return model.body;
        }
    }
}
