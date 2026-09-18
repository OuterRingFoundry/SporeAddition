package dev.sporebound;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import java.util.*;

/** Settlements persist independently of residents. Work uses loaded terrain and finite supplies. */
public final class SurvivorColonies extends SavedData {
    private final List<BlockPos> homes=new ArrayList<>();
    private final Set<BlockPos> complete=new HashSet<>();
    private long nextSpawn;
    public static SurvivorColonies get(ServerLevel level){return level.getDataStorage().computeIfAbsent(
        new Factory<>(SurvivorColonies::new,(tag,lookup)->load(tag)),"sporebound_survivors");}
    private static SurvivorColonies load(CompoundTag tag){var data=new SurvivorColonies();
        for(long pos:tag.getLongArray("Homes"))data.homes.add(BlockPos.of(pos));
        for(long pos:tag.getLongArray("Complete"))data.complete.add(BlockPos.of(pos));data.nextSpawn=tag.getLong("NextSpawn");return data;}
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){tag.putLongArray("Homes",homes.stream().mapToLong(BlockPos::asLong).toArray());
        tag.putLongArray("Complete",complete.stream().mapToLong(BlockPos::asLong).toArray());tag.putLong("NextSpawn",nextSpawn);return tag;}
    public List<BlockPos> homes(){return List.copyOf(homes);}
    public boolean protectedSite(BlockPos pos){return homes.stream().anyMatch(h->Math.abs(h.getX()-pos.getX())<=4&&Math.abs(h.getZ()-pos.getZ())<=4&&Math.abs(h.getY()-pos.getY())<=5);}
    public BlockPos patrol(BlockPos home,boolean peer){return peer?homes.stream().filter(h->!h.equals(home)&&h.distSqr(home)<=192*192)
        .min(Comparator.comparingDouble(h->h.distSqr(home))).orElse(home):home;}
    public static boolean allowed(ServerLevel level){return level.dimension().equals(Level.OVERWORLD)||level.dimension().equals(Level.NETHER)||level.dimension().equals(Sporebound.BLIGHT);}
    private static boolean natural(net.minecraft.world.level.block.state.BlockState state){return state.is(net.minecraft.tags.BlockTags.DIRT)
        ||state.is(Blocks.STONE)||state.is(Blocks.GRAVEL)||state.is(Blocks.SAND)||state.is(Blocks.NETHERRACK)
        ||state.is(Blocks.BLACKSTONE)||state.is(Blocks.BASALT)||(state.is(FungalContent.CRUST.get()) || state.is(FungalContent.PALE.get()))
        ||state.is(com.Harbinger.Spore.core.Sblocks.INFESTED_DIRT.get());}
    public static boolean site(ServerLevel level,BlockPos base){
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){
            var p=base.offset(x,0,z);if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!natural(level.getBlockState(p.below()))
                ||!level.getBlockState(p.below()).isFaceSturdy(level,p.below(),Direction.UP))return false;
            for(int y=0;y<=4;y++)if(!level.getBlockState(p.above(y)).canBeReplaced()||!level.getFluidState(p.above(y)).isEmpty())return false;
        }return true;
    }
    public boolean found(ServerLevel level,BlockPos base){
        if(!allowed(level)||homes.size()>=32||homes.stream().anyMatch(h->h.distSqr(base)<72*72)||!site(level,base))return false;
        var residents=new ArrayList<Survivor>();
        for(int i=0;i<3;i++){
            var survivor=FungalContent.SURVIVOR.get().create(level);if(survivor==null){residents.forEach(net.minecraft.world.entity.Entity::discard);return false;}
            survivor.moveTo(base.getX()+i-0.5,base.getY(),base.getZ()+0.5,0,0);survivor.settle(base);
            survivor.finalizeSpawn(level,level.getCurrentDifficultyAt(base),MobSpawnType.EVENT,null);
            // Every new three-person colony has an archer and two sword/shield defenders.
            if(i==0)survivor.equipStarterCombat(true);
            survivor.supplies().addItem(new ItemStack(Items.COBBLESTONE,32));survivor.supplies().addItem(new ItemStack(Items.BREAD,8));
            survivor.supplies().addItem(new ItemStack(Items.TORCH,4));
            if(!level.addFreshEntity(survivor)){residents.forEach(net.minecraft.world.entity.Entity::discard);return false;}residents.add(survivor);
        }
        homes.add(base.immutable());setDirty();return true;
    }
    private void spawn(ServerLevel level){
        if(!allowed(level)||!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)||level.getGameTime()<nextSpawn)return;
        nextSpawn=level.getGameTime()+2400;setDirty();
        for(var player:level.players()){
            if(player.isSpectator())continue;
            for(int attempt=0;attempt<12;attempt++){
                int x=player.blockPosition().getX()+level.random.nextInt(145)-72,z=player.blockPosition().getZ()+level.random.nextInt(145)-72;
                var column=new BlockPos(x,player.blockPosition().getY(),z);if(!level.hasChunkAt(column)||column.distSqr(player.blockPosition())<32*32)continue;
                if(level.dimension().equals(Level.NETHER)){
                    for(int y=Math.min(115,player.blockPosition().getY()+16);y>=Math.max(16,player.blockPosition().getY()-24);y--)
                        if(found(level,new BlockPos(x,y,z)))return;
                }else{
                    int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                    if(found(level,new BlockPos(x,y,z)))return;
                }
            }
        }
    }
    public int build(ServerLevel level,BlockPos home,Survivor worker,int budget){
        if(!homes.contains(home)||!worker.isAlive()||worker.getTarget()!=null||worker.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(home))>144
            ||!net.neoforged.neoforge.event.EventHooks.canEntityGrief(level,worker))return 0;
        int placed=0;
        for(int y=0;y<=3;y++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){
            if(y<3&&Math.abs(x)!=2&&Math.abs(z)!=2)continue;
            if(z==-2&&x==0&&y<2)continue;
            if(y==1&&((x==0&&z==2)||(z==0&&Math.abs(x)==2)))continue;
            var pos=home.offset(x,y,z);if(!level.hasChunkAt(pos))return placed;
            var state=level.getBlockState(pos);if(state.is(Blocks.COBBLESTONE)||state.is(Blocks.COBBLED_DEEPSLATE)||state.is(Blocks.BLACKSTONE)||state.is(net.minecraft.tags.BlockTags.PLANKS))continue;
            if(!state.canBeReplaced()||!state.getFluidState().isEmpty())return placed;
            if(!level.getEntities(null,new net.minecraft.world.phys.AABB(pos)).isEmpty())return placed;
            if(placed>=budget||!worker.material())return placed;
            if(level.setBlockAndUpdate(pos,Block.byItem(worker.buildingMaterial()).defaultBlockState())){worker.payMaterial();placed++;}
        }
        if(complete.add(home))setDirty();return placed;
    }
    /** Lay a navigable, lit trail along a real ground path. Obstacles are left in place. */
    public int road(ServerLevel level,Survivor worker){
        var home=worker.home();if(home==null||!complete.contains(home)||!worker.material()
            ||!net.neoforged.neoforge.event.EventHooks.canEntityGrief(level,worker))return 0;
        var peer=patrol(home,true);if(peer.equals(home))return 0;
        var path=worker.getNavigation().createPath(peer,2,192);if(path==null||!path.canReach())return 0;
        int placed=0;
        for(int i=0;i<path.getNodeCount()&&placed<4;i++){
            var node=path.getNode(i);var feet=new BlockPos(node.x,node.y,node.z);var floor=feet.below();
            if(!level.hasChunkAt(floor)||protectedSite(floor)||!natural(level.getBlockState(floor))||!level.getFluidState(feet).isEmpty()
                ||!level.getBlockState(floor).isFaceSturdy(level,floor,Direction.UP))continue;
            if(!worker.material())break;
            if(level.setBlockAndUpdate(floor,Block.byItem(worker.buildingMaterial()).defaultBlockState())){worker.payMaterial();placed++;}
            if(i%8==0){var lamp=feet.east();if(level.hasChunkAt(lamp)&&level.getBlockState(lamp).isAir()
                &&Blocks.TORCH.defaultBlockState().canSurvive(level,lamp)&&worker.supplies().countItem(Items.TORCH)>0){
                if(level.setBlockAndUpdate(lamp,Blocks.TORCH.defaultBlockState()))worker.pay(Items.TORCH);}}
        }return placed;
    }
    @SubscribeEvent public void monster(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event){
        if(event.getLevel() instanceof ServerLevel&&event.getEntity() instanceof net.minecraft.world.entity.Mob mob
            &&mob instanceof net.minecraft.world.entity.monster.Enemy&&!Protection.spore(mob))
            mob.targetSelector.addGoal(2,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(mob,Survivor.class,true));
    }
    @SubscribeEvent public void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level)||!allowed(level)||level.getGameTime()%100!=0)return;
        var data=get(level);data.spawn(level);
        var residents=new ArrayList<Survivor>();for(var e:level.getAllEntities())if(e instanceof Survivor survivor&&survivor.isAlive())residents.add(survivor);
        for(var resident:residents){
            if(resident.getTarget()!=null||resident.isNoAi())continue;
            if(resident.home()!=null&&residents.stream().filter(r->resident.home().equals(r.home())).count()>=2){
                data.build(level,resident.home(),resident,3);data.road(level,resident);
            }
        }
        if(level.getGameTime()%1200==0&&data.homes.size()<32)for(var resident:residents){
            if(data.homes.stream().anyMatch(h->h.distSqr(resident.blockPosition())<72*72)||!resident.material())continue;
            var group=residents.stream().filter(r->r.distanceToSqr(resident)<144&&r.getTarget()==null).toList();
            if(group.size()>=2&&site(level,resident.blockPosition())){
                var home=resident.blockPosition();data.homes.add(home);for(var r:group)r.settle(home);data.setDirty();break;
            }
        }
    }
}
