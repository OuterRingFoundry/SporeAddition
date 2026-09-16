package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** An inert vanilla-block ritual; never opens an entity portal or changes corruption. */
public final class RiftCairn {
    private RiftCairn() {}
    public static boolean complete(Level level, BlockPos center) {
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            var expected=x==0&&z==0?Blocks.AMETHYST_BLOCK:
                x==0||z==0?Blocks.CRYING_OBSIDIAN:Blocks.POLISHED_DEEPSLATE;
            if(!level.getBlockState(center.offset(x,0,z)).is(expected))return false;
            for(int y=1;y<=3;y++)if(!level.getBlockState(center.offset(x,y,z)).isAir())return false;
        }
        return true;
    }
    public static void build(ServerLevel level, BlockPos center) {
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)
            level.setBlock(center.offset(x,0,z),Blocks.DEEPSLATE_TILES.defaultBlockState(),3);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++) {
            var block=x==0&&z==0?Blocks.AMETHYST_BLOCK:
                x==0||z==0?Blocks.CRYING_OBSIDIAN:Blocks.POLISHED_DEEPSLATE;
            level.setBlock(center.offset(x,0,z),block.defaultBlockState(),3);
        }
    }
    public static boolean enter(ServerPlayer player, BlockPos center) {
        var level=player.serverLevel();
        if(player.getCooldowns().isOnCooldown(Sporebound.TALISMAN.get()))return false;
        if(Protection.mushroom(level,center))return fail(player,"sanctuary");
        if(!complete(level,center))return fail(player,"incomplete");
        int pearl=-1;
        for(int i=0;i<player.getInventory().getContainerSize();i++)
            if(player.getInventory().getItem(i).is(Items.ENDER_PEARL)){pearl=i;break;}
        if(pearl<0&&!player.getAbilities().instabuild)return fail(player,"pearl");
        if(!Travel.enter(player))return false;
        if(!player.getAbilities().instabuild)player.getInventory().removeItem(pearl,1);
        effect(level,center);
        player.getCooldowns().addCooldown(Sporebound.TALISMAN.get(),100);
        player.displayClientMessage(Component.translatable("message.sporebound.arrived"),true);
        return true;
    }
    public static boolean fail(ServerPlayer player,String reason) {
        player.displayClientMessage(Component.translatable("message.sporebound."+reason),true);return false;
    }
    public static void effect(ServerLevel level,BlockPos center) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL,center.getX()+0.5,center.getY()+1.2,center.getZ()+0.5,80,0.7,0.7,0.7,0.03);
        level.playSound(null,center,SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(),SoundSource.BLOCKS,0.7F,0.7F);
    }
    /** Every completed cairn inside the Blighted World doubles as a free emergency exit. */
    public static void interact(PlayerInteractEvent.RightClickBlock event) {
        if(!event.getLevel().dimension().equals(Sporebound.BLIGHT)||event.getHand()!=InteractionHand.MAIN_HAND
            ||!event.getItemStack().isEmpty()||!complete(event.getLevel(),event.getPos()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(event.getEntity() instanceof ServerPlayer player&&!player.getCooldowns().isOnCooldown(Sporebound.TALISMAN.get())) {
            if(Travel.leave(player))player.getCooldowns().addCooldown(Sporebound.TALISMAN.get(),100);
        }
    }
}
