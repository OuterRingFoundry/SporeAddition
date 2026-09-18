package dev.sporebound;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.event.EventHooks;
import java.util.*;

/** Persistent player-like survivor: supplies, harvesting, combat, healing and patrols. */
public final class Survivor extends PathfinderMob implements net.minecraft.world.entity.monster.RangedAttackMob {
    private static final EntityDataAccessor<Integer> SKIN=SynchedEntityData.defineId(Survivor.class,EntityDataSerializers.INT);
    private final SimpleContainer supplies=new SimpleContainer(18);
    private BlockPos home;
    private int experience,fuel,shieldCooldown;
    private boolean archer;
    public boolean archer(){return archer;}
    public boolean usingBow(){return getMainHandItem().is(Items.BOW)&&supplies.countItem(Items.ARROW)>0;}
    public int shieldCooldown(){return shieldCooldown;}
    /** Starter equipment is assigned only when a new resident is spawned. */
    public void equipStarterCombat(boolean ranged){
        archer=ranged;
        setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ranged?Items.BOW:Items.STONE_SWORD));
        setItemSlot(EquipmentSlot.OFFHAND,ranged?ItemStack.EMPTY:new ItemStack(Items.SHIELD));
        setDropChance(EquipmentSlot.MAINHAND,0);setDropChance(EquipmentSlot.OFFHAND,0);
        if(ranged){supplies.addItem(new ItemStack(Items.ARROW,16));supplies.addItem(new ItemStack(Items.STONE_SWORD));}
    }
    @Override public void performRangedAttack(LivingEntity target,float power){SurvivorCombat.shoot(this,target,power);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.AbstractArrow
            &&source.getEntity() instanceof Survivor)return false;
        return super.hurt(source,amount);
    }
    @Override protected void hurtCurrentlyUsedShield(float amount){
        if(getUseItem().is(Items.SHIELD)&&amount>=3){
            getUseItem().hurtAndBreak(1+(int)amount,this,EquipmentSlot.OFFHAND);
            if(getOffhandItem().isEmpty())stopUsingItem();
        }
    }
    @Override protected void blockUsingShield(LivingEntity attacker){
        super.blockUsingShield(attacker);
        if(attacker.getMainHandItem().getItem() instanceof AxeItem){shieldCooldown=100;stopUsingItem();}
    }
    private boolean furnace;
    private ItemStack miningTool=new ItemStack(Items.STONE_PICKAXE);
    public int experience(){return experience;}
    public void learn(int amount){experience=(int)Math.clamp((long)experience+Math.max(0,amount),0,10000);SurvivorProgression.refresh(this);}
    public boolean hasFurnace(){return furnace;}
    public void makeFurnace(){furnace=true;}
    public int fuel(){return fuel;}
    public void addFuel(int amount){fuel=Math.clamp(fuel+amount,0,8);}
    public ItemStack miningTool(){return miningTool;}
    public void setMiningTool(ItemStack tool){miningTool=tool;}
    @Override public void awardKillScore(Entity victim,int score,net.minecraft.world.damagesource.DamageSource source){
        super.awardKillScore(victim,score,source);if(victim instanceof LivingEntity&&(Protection.spore(victim)||victim instanceof Enemy))learn(6);
    }
    public Survivor(EntityType<? extends Survivor> type,Level level){super(type,level);setPersistenceRequired();}
    public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,24)
        .add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,0.29).add(Attributes.FOLLOW_RANGE,96);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(SKIN,0);}
    public int skin(){return entityData.get(SKIN);}
    public BlockPos home(){return home;}
    public void settle(BlockPos pos){home=pos.immutable();restrictTo(home,192);}
    public SimpleContainer supplies(){return supplies;}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(1,new RangedBowAttackGoal<Survivor>(this,1.0,35,16){
            @Override public boolean canUse(){return usingBow()&&super.canUse();}
            @Override public boolean canContinueToUse(){return usingBow()&&super.canContinueToUse();}
        });
        goalSelector.addGoal(2,new SurvivorCombat.Melee(this));
        goalSelector.addGoal(3,new WorkGoal());goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,0.7));
        goalSelector.addGoal(7,new LookAtPlayerGoal(this,net.minecraft.world.entity.player.Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this,Survivor.class).setAlertOthers());
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
            e->!(e instanceof Survivor)&&e.isAlive()&&(Protection.spore(e)||e instanceof Enemy)));
    }
    @Override public SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){
        var result=super.finalizeSpawn(level,difficulty,reason,data);entityData.set(SKIN,random.nextInt(9));
        equipStarterCombat((reason==MobSpawnType.SPAWN_EGG||reason==MobSpawnType.NATURAL)&&random.nextInt(3)==0);
        setCustomName(net.minecraft.network.chat.Component.literal(new String[]{"Ash","Rowan","Mira","Flint","Ember","Reed","Fern","Slate","Wren"}[skin()]));
        if(home==null)settle(blockPosition());return result;
    }
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean isAlliedTo(Entity e){return e instanceof Survivor||super.isAlliedTo(e);}
    public boolean collect(ItemEntity item){
        if(!(level() instanceof ServerLevel server)||!EventHooks.canEntityGrief(server,this)||!item.isAlive()||item.hasPickUpDelay()
            ||distanceToSqr(item)>4||!hasLineOfSight(item))return false;
        int before=item.getItem().getCount();var remaining=supplies.addItem(item.getItem().copy());
        if(remaining.getCount()==before)return false;
        if(remaining.isEmpty())item.discard();else item.setItem(remaining);learn(Math.min(4,before-remaining.getCount()));return true;
    }
    public boolean pay(Item item){for(int i=0;i<supplies.getContainerSize();i++)if(supplies.getItem(i).is(item)){supplies.removeItem(i,1);return true;}return false;}
    public Item buildingMaterial(){
        for(int i=0;i<supplies.getContainerSize();i++){var stack=supplies.getItem(i);
            if(stack.is(Items.COBBLESTONE)||stack.is(Items.COBBLED_DEEPSLATE)||stack.is(Items.BLACKSTONE)
                ||stack.is(net.minecraft.tags.ItemTags.PLANKS))return stack.getItem();}
        return Items.AIR;
    }
    public boolean material(){return buildingMaterial()!=Items.AIR;}
    public boolean payMaterial(){return pay(buildingMaterial());}
    private void craftPlanks(ServerLevel level){
        for(int i=0;i<supplies.getContainerSize();i++)if(supplies.getItem(i).is(net.minecraft.tags.ItemTags.LOGS)){
            var input=net.minecraft.world.item.crafting.CraftingInput.of(1,1,List.of(supplies.getItem(i).copyWithCount(1)));
            var recipe=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,input,level);
            if(recipe.isEmpty())continue;var result=recipe.get().value().assemble(input,registryAccess());
            if(!result.is(net.minecraft.tags.ItemTags.PLANKS)||!supplies.canAddItem(result))continue;
            supplies.removeItem(i,1);var left=supplies.addItem(result);if(!left.isEmpty())spawnAtLocation(left);return;
        }
    }
    @Override public void tick(){super.tick();if(!(level() instanceof ServerLevel server)||!isAlive()||isNoAi())return;
        if(shieldCooldown>0)shieldCooldown--;
        if(tickCount%20==0){
            SurvivorCombat.equipWeapon(this);SurvivorCombat.cooperate(this);
            for(var item:server.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(1.5)))collect(item);
            if(home==null)settle(blockPosition());craftPlanks(server);
            if(tickCount%100==0)SurvivorProgression.improve(this);
            if(getHealth()<getMaxHealth()&&tickCount%100==0)for(int i=0;i<supplies.getContainerSize();i++){
                var stack=supplies.getItem(i);var food=stack.get(net.minecraft.core.component.DataComponents.FOOD);
                if(food!=null){stack.shrink(1);heal(Math.max(1,food.nutrition()));break;}
            }
        }
    }
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean recentlyHit){
        super.dropCustomDeathLoot(level,source,recentlyHit);Containers.dropContents(level,blockPosition(),supplies);supplies.clearContent();
        if(!miningTool.isEmpty())spawnAtLocation(miningTool);miningTool=ItemStack.EMPTY;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("SurvivorSkin",skin());
        tag.putBoolean("Archer",archer);tag.putInt("ShieldCooldown",shieldCooldown);tag.putInt("Experience",experience);tag.putInt("SmeltingFuel",fuel);tag.putBoolean("Furnace",furnace);tag.put("MiningTool",miningTool.save(registryAccess()));
        if(home!=null)tag.putLong("ColonyHome",home.asLong());var list=new ListTag();
        for(int i=0;i<supplies.getContainerSize();i++)if(!supplies.getItem(i).isEmpty()){
            var t=new CompoundTag();t.putInt("Slot",i);t.put("Item",supplies.getItem(i).save(registryAccess()));list.add(t);}
        tag.put("Supplies",list);
    }
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(SKIN,Math.clamp(tag.getInt("SurvivorSkin"),0,8));
        archer=tag.getBoolean("Archer");shieldCooldown=Math.clamp(tag.getInt("ShieldCooldown"),0,100);
        experience=Math.clamp(tag.getInt("Experience"),0,10000);fuel=Math.clamp(tag.getInt("SmeltingFuel"),0,8);furnace=tag.getBoolean("Furnace");
        if(tag.contains("MiningTool"))miningTool=ItemStack.parseOptional(registryAccess(),tag.getCompound("MiningTool"));
        SurvivorProgression.refresh(this);
        if(tag.contains("ColonyHome"))settle(BlockPos.of(tag.getLong("ColonyHome")));supplies.clearContent();
        for(var entry:tag.getList("Supplies",Tag.TAG_COMPOUND)){var t=(CompoundTag)entry;int slot=t.getInt("Slot");
            if(slot>=0&&slot<supplies.getContainerSize())supplies.setItem(slot,ItemStack.parseOptional(registryAccess(),t.getCompound("Item")));}
    }
    static boolean resource(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);if(state.hasBlockEntity()||state.getDestroySpeed(level,pos)<0)return false;
        return state.is(BlockTags.COAL_ORES)||state.is(BlockTags.IRON_ORES)||state.is(BlockTags.COPPER_ORES)||state.is(BlockTags.DIAMOND_ORES)
            ||state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)||state.is(Blocks.BLACKSTONE)||state.is(BlockTags.LOGS)
            ||state.getBlock() instanceof CropBlock crop&&crop.isMaxAge(state);
    }
    boolean canMine(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);return !state.requiresCorrectToolForDrops()||miningTool.isCorrectToolForDrops(state);
    }
    private final class WorkGoal extends Goal {
        private BlockPos work;private ItemEntity drop;private int clock,progress;
        WorkGoal(){setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
        @Override public boolean canUse(){
            if(getTarget()!=null||tickCount%40!=0||!(level() instanceof ServerLevel server)||home==null)return false;
            drop=server.getEntitiesOfClass(ItemEntity.class,getBoundingBox().inflate(16),i->!i.hasPickUpDelay()&&supplies.canAddItem(i.getItem()))
                .stream().min(Comparator.comparingDouble(Survivor.this::distanceToSqr)).orElse(null);
            work=null;if(drop!=null)return true;
            if(EventHooks.canEntityGrief(server,Survivor.this))for(var pos:BlockPos.betweenClosed(blockPosition().offset(-6,0,-6),blockPosition().offset(6,2,6))){
                if(!server.hasChunkAt(pos)||!resource(server,pos)||!canMine(server,pos)||SurvivorColonies.get(server).protectedSite(pos)||java.util.Arrays.stream(net.minecraft.core.Direction.values()).noneMatch(d->server.getBlockState(pos.relative(d)).isAir()))continue;
                var path=getNavigation().createPath(pos,1);if(path!=null&&path.canReach()){work=pos.immutable();return true;}
            }
            work=SurvivorColonies.get(server).patrol(home,random.nextBoolean());return work!=null;
        }
        @Override public void start(){clock=0;progress=0;}
        @Override public boolean canContinueToUse(){return getTarget()==null&&clock<400&&(drop!=null?drop.isAlive():work!=null);}
        @Override public void tick(){clock++;
            if(drop!=null){if(distanceToSqr(drop)<4){collect(drop);clock=400;}else if(clock%10==0)getNavigation().moveTo(drop,1);return;}
            if(work.closerToCenterThan(position(),3)){
                getNavigation().stop();if(!(level() instanceof ServerLevel server))return;
                if(resource(server,work)&&canMine(server,work)&&!SurvivorColonies.get(server).protectedSite(work)&&EventHooks.canEntityGrief(server,Survivor.this)){
                    getLookControl().setLookAt(work.getX()+0.5,work.getY()+0.5,work.getZ()+0.5);swing(InteractionHand.MAIN_HAND);
                    if(++progress>=40){
                        var hit=server.clip(new net.minecraft.world.level.ClipContext(getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(work),
                            net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,Survivor.this));
                        if(hit.getBlockPos().equals(work)) {
                            var loot=Block.getDrops(server.getBlockState(work),server,work,null,Survivor.this,miningTool);
                            if(server.destroyBlock(work,false,Survivor.this)){
                                for(var stack:loot){var left=supplies.addItem(stack);if(!left.isEmpty())spawnAtLocation(left);}learn(2);
                                if(miningTool.isDamageableItem()){miningTool.setDamageValue(miningTool.getDamageValue()+1);
                                    if(miningTool.getDamageValue()>=miningTool.getMaxDamage())miningTool=ItemStack.EMPTY;}
                            }
                        }clock=400;
                    }
                }else clock=400;
            }else if(clock%10==0)getNavigation().moveTo(getNavigation().createPath(work,1,192),0.9);
        }
        @Override public void stop(){drop=null;work=null;getNavigation().stop();}
    }
}
