package dev.sporebound;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public final class RiftTalisman extends Item {
    public RiftTalisman() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if(context.getPlayer() instanceof ServerPlayer player) {
            if(player.level().dimension().equals(Sporebound.BLIGHT))recall(player);
            else RiftCairn.enter(player,context.getClickedPos());
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if(player instanceof ServerPlayer serverPlayer) {
            if(level.dimension().equals(Sporebound.BLIGHT))recall(serverPlayer);
            else RiftCairn.fail(serverPlayer,"instructions");
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand),level.isClientSide);
    }
    private void recall(ServerPlayer player) {
        if(!player.getCooldowns().isOnCooldown(this)&&Travel.leave(player))player.getCooldowns().addCooldown(this,100);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.sporebound.rift_talisman.enter").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.sporebound.rift_talisman.return").withStyle(ChatFormatting.DARK_AQUA));
    }
}
