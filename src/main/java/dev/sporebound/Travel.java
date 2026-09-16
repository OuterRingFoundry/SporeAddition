package dev.sporebound;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class Travel {
    private Travel() {}
    public static boolean enter(ServerPlayer player) {
        if(player.level().dimension().equals(Sporebound.BLIGHT))return false;
        var level=player.server.getLevel(Sporebound.BLIGHT);
        if(level==null)return RiftCairn.fail(player,"unavailable");
        var center=ArrivalData.get(level).center(level);
        var destination=safeNear(player,level,Vec3.atBottomCenterOf(center.above()));
        if(destination==null)return RiftCairn.fail(player,"obstructed");
        CompoundTag back=new CompoundTag();
        back.putString("dimension",player.level().dimension().location().toString());
        back.putDouble("x",player.getX());back.putDouble("y",player.getY());back.putDouble("z",player.getZ());
        back.putFloat("yaw",player.getYRot());back.putFloat("pitch",player.getXRot());
        player.getPersistentData().put("sporebound_return",back);
        player.teleportTo(level,destination.x,destination.y,destination.z,Set.of(),player.getYRot(),player.getXRot());
        player.fallDistance=0;RiftCairn.effect(level,center);WorldRules.sync(player);return true;
    }
    public static boolean leave(ServerPlayer player) {
        if(!player.level().dimension().equals(Sporebound.BLIGHT))return false;
        var tag=player.getPersistentData().getCompound("sporebound_return");
        var level=player.server.overworld();
        var id=ResourceLocation.tryParse(tag.getString("dimension"));
        boolean savedDimension=false;
        if(id!=null) {
            var saved=player.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
            if(saved!=null&&saved!=player.serverLevel()){level=saved;savedDimension=true;}
        }
        var spawn=level.getSharedSpawnPos();
        var requested=savedDimension&&tag.contains("x")?new Vec3(tag.getDouble("x"),tag.getDouble("y"),tag.getDouble("z")):Vec3.atBottomCenterOf(spawn);
        var destination=safeNear(player,level,requested);
        if(destination==null) {
            // A dismantled or obstructed departure point falls back to the Overworld spawn.
            level=player.server.overworld();spawn=level.getSharedSpawnPos();level.getChunkAt(spawn);
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn.getX(),spawn.getZ());
            destination=safeNear(player,level,new Vec3(spawn.getX()+0.5,y,spawn.getZ()+0.5));
        }
        if(destination==null)return RiftCairn.fail(player,"obstructed");
        RiftCairn.effect(player.serverLevel(),player.blockPosition().below());
        player.teleportTo(level,destination.x,destination.y,destination.z,Set.of(),tag.getFloat("yaw"),tag.getFloat("pitch"));
        player.fallDistance=0;WorldRules.sync(player);return true;
    }
    /** Bounded search, no digging or overwriting player blocks. Prefer the exact saved position. */
    public static Vec3 safeNear(ServerPlayer player,ServerLevel level,Vec3 origin) {
        if(!Double.isFinite(origin.x)||!Double.isFinite(origin.y)||!Double.isFinite(origin.z))return null;
        if(safe(player,level,origin))return origin;
        var base=BlockPos.containing(origin);
        for(int radius=0;radius<=4;radius++)for(int dy=0;dy<=8;dy++)for(int sign:new int[]{1,-1}) {
            if(dy==0&&sign<0)continue;
            for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++) {
                if(Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
                var point=Vec3.atBottomCenterOf(base.offset(x,dy*sign,z));
                if(safe(player,level,point))return point;
            }
        }
        return null;
    }
    private static boolean safe(ServerPlayer player,ServerLevel level,Vec3 point) {
        var feet=BlockPos.containing(point);var below=feet.below();
        if(feet.getY()<=level.getMinBuildHeight()||feet.getY()+2>=level.getMaxBuildHeight()||!level.getWorldBorder().isWithinBounds(feet))return false;
        level.getChunkAt(feet);
        var floor=level.getBlockState(below);
        if(!floor.isFaceSturdy(level,below,Direction.UP)||floor.is(Blocks.MAGMA_BLOCK)||floor.is(Blocks.CAMPFIRE)
            ||floor.is(Blocks.SOUL_CAMPFIRE)||floor.is(Blocks.CACTUS))return false;
        if(!level.getFluidState(feet).isEmpty()||!level.getFluidState(feet.above()).isEmpty())return false;
        if(level.getBlockState(feet).is(Blocks.FIRE)||level.getBlockState(feet).is(Blocks.SOUL_FIRE))return false;
        return level.noCollision(player,player.getBoundingBox().move(point.x-player.getX(),point.y-player.getY(),point.z-player.getZ()));
    }
}
