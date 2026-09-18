package dev.sporebound;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Consumed only by a server-confirmed dormant-world awakening. */
public final class SporeCatalyst extends Item {
    public SporeCatalyst() { super(new Properties()); }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.sporebound.spore_catalyst").withStyle(ChatFormatting.GRAY));
    }
}
