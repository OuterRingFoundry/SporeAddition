package dev.sporebound;

import com.Harbinger.Spore.Sentities.BaseEntities.Infected;
import com.Harbinger.Spore.core.Seffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import java.util.Comparator;
import java.util.EnumSet;

/** A mobile fungal remnant. Assimilation uses no damage, death event or loot. */
public final class InfectedBiomass extends PathfinderMob {
    public static final int EVOLUTION_MASS = BiomassMath.MAX_SIZE_MASS + 1, ABSORB_TICKS = 40;
    private static final EntityDataAccessor<Integer> MASS = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RECIPIENT = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROGRESS = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MEAL = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MEAL_PROGRESS = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HUNGER = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    public static final int SHRINK_SECONDS = 60;
    private int hungerClock, starvationSeconds;
    private String origin = "";
    private int evolveCooldown;
    private int idleTicks;

    public InfectedBiomass(EntityType<? extends InfectedBiomass> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12).add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.FOLLOW_RANGE, 6).add(Attributes.ATTACK_DAMAGE, 1);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(MASS, 1); builder.define(RECIPIENT, -1); builder.define(PROGRESS, 0);
        builder.define(MEAL, -1); builder.define(MEAL_PROGRESS, 0); builder.define(HUNGER, 0);
    }
    public int mass() { return entityData.get(MASS); }
    public void setMass(int value) {
        float fraction = getMaxHealth() > 0 ? getHealth() / getMaxHealth() : 1;
        entityData.set(MASS, Math.clamp(value, 1, BiomassMath.MAX_SIZE_MASS * 2));
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(12 * mass());
        setHealth(getMaxHealth() * fraction);
    }
    public float massScale() { return BiomassMath.scale(mass()); }
    public int idleTicks() { return idleTicks; }
    @Override public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(massScale());
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (MASS.equals(key)) refreshDimensions();
    }
    public int absorptionTicks() { return entityData.get(PROGRESS); }
    public boolean absorbing() { return entityData.get(RECIPIENT) >= 0; }
    public void setOrigin(String value) { origin = value; }
    public String origin() { return origin; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new BiomassScavengingGoal(this));
        goalSelector.addGoal(2, new ItemForagingGoal());
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 0.9, false));
        goalSelector.addGoal(4, new CoalesceGoal());
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
            this, LivingEntity.class, 40, true, false, target -> !busy() && FungalEcology.prey(target)));
    }
    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean isAlliedTo(Entity entity) { return Protection.spore(entity) || super.isAlliedTo(entity); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SLIME_HURT_SMALL; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SLIME_DEATH_SMALL; }

    public boolean beginAbsorption(Mob recipient) {
        if (level().isClientSide || busy() || !validRecipient(recipient)) return false;
        entityData.set(RECIPIENT, recipient.getId()); entityData.set(PROGRESS, 0);
        getNavigation().stop();
        playSound(SoundEvents.SLIME_SQUISH_SMALL, 0.6F, 0.6F);
        return true;
    }
    private boolean receiving() {
        return !level().getEntitiesOfClass(InfectedBiomass.class, getBoundingBox().inflate(3),
            b -> b.isAlive() && b.entityData.get(RECIPIENT) == getId()).isEmpty();
    }
    private boolean validRecipient(Mob target) {
        if (!isAlive() || target == this || !target.isAlive() || target.level() != level()
                || distanceToSqr(target) > 6.25 || !hasLineOfSight(target)
                || !(level() instanceof ServerLevel server) || Protection.sterile(server, blockPosition())
                || Protection.sterile(server, target.blockPosition())) return false;
        if (target instanceof InfectedBiomass biomass)
            return !biomass.absorbing() && !biomass.feeding() && biomass.mass() + mass() <= BiomassMath.MAX_SIZE_MASS * 2;
        return target instanceof Infected infected && infected.isStarving();
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive() || Protection.sterile(server, blockPosition())) return;
        if (!isNoAi() && ++hungerClock >= 20) { hungerClock = 0; hungerSecond(); }
        if (!isAlive()) return;
        if (feeding()) { tickAssimilation(server); return; }
        if (getTarget() != null) idleTicks = 0;
        boolean beingAbsorbedInto = receiving();
        if (beingAbsorbedInto) {
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.15, 1, 0.15));
        } else if (!absorbing() && getTarget() == null) idleTicks = Math.min(BiomassMath.IDLE_TICKS, idleTicks + 1);
        if (absorbing()) {
            Entity entity = server.getEntity(entityData.get(RECIPIENT));
            if (!(entity instanceof Mob recipient) || !validRecipient(recipient)) { cancelAbsorption(); return; }
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0.15, 1, 0.15));
            int step = absorptionTicks() + 1; entityData.set(PROGRESS, step);
            // A contracting helix travels from donor to recipient; sent to all nearby clients.
            if (step % 2 == 0) {
                var from = position().add(0, 0.3, 0); var to = recipient.position().add(0, recipient.getBbHeight() * 0.4, 0);
                for (int i = 0; i < 6; i++) {
                    double t = (i + (step % 10) / 10.0) / 6;
                    var p = from.lerp(to, t).add(Math.sin(step * 0.4 + i) * 0.10, Math.cos(step * 0.4 + i) * 0.1, 0);
                    server.sendParticles(new DustParticleOptions(new Vector3f(0.73F, 0.55F, 0.58F), 0.65F), p.x, p.y, p.z, 1, 0, 0, 0, 0);
                }
            }
            if (step >= ABSORB_TICKS) {
                if (recipient instanceof InfectedBiomass biomass) { biomass.nourish(mass()); }
                else if (recipient instanceof Infected infected) {
                    infected.setHunger(0); infected.removeEffect(Seffects.STARVATION);
                    infected.setEvoPoints(infected.getEvoPoints() + mass()); infected.heal(mass() * 2);
                }
                recipient.playSound(SoundEvents.SLIME_SQUISH, 0.7F, 0.7F);
                discard();
            }
        } else if (!isNoAi() && !beingAbsorbedInto && mass() >= EVOLUTION_MASS && --evolveCooldown <= 0) {
            evolveCooldown = 100;
            evolve(server);
        }
    }
    private void cancelAbsorption() { entityData.set(RECIPIENT, -1); entityData.set(PROGRESS, 0); }
    public boolean evolve(ServerLevel server) {
        if (mass() < EVOLUTION_MASS || busy() || isHungry() || !isAlive() || Protection.sterile(server, blockPosition())) return false;
        EntityType<?> type = random.nextBoolean() ? com.Harbinger.Spore.core.Sentities.SLASHER.get() : com.Harbinger.Spore.core.Sentities.BRUTE.get();
        Entity result = type.create(server);
        if (!(result instanceof Infected infected)) return false;
        infected.moveTo(getX(), getY(), getZ(), getYRot(), 0);
        if (!server.noCollision(infected)) return false;
        infected.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null);
        infected.setCustomName(getCustomName()); infected.setPersistenceRequired(); infected.setHunger(0);
        infected.setEvoPoints(infected.getEvoPoints() + mass());
        if (!server.addFreshEntity(infected)) return false;
        playSound(SoundEvents.SLIME_SQUISH, 1, 0.4F); discard(); return true;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("BiomassMass", mass()); tag.putString("BiomassOrigin", origin); tag.putInt("BiomassIdle", idleTicks);
        tag.putInt("BiomassHunger", hunger()); tag.putInt("BiomassStarvation", starvationSeconds);
        // An interrupted animation restarts after reload; it never persists half a transfer.
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setMass(tag.getInt("BiomassMass")); origin = tag.getString("BiomassOrigin");
        idleTicks = Math.clamp(tag.getInt("BiomassIdle"), 0, BiomassMath.IDLE_TICKS);
        setHunger(tag.getInt("BiomassHunger"));
        starvationSeconds = Math.clamp(tag.getInt("BiomassStarvation"), 0, SHRINK_SECONDS - 1);
        cancelAbsorption(); cancelAssimilation();
    }
    public int hunger() { return entityData.get(HUNGER); }
    public void setHunger(int seconds) { entityData.set(HUNGER, Math.clamp(seconds, 0, hungerLimit())); }
    private int hungerLimit() { return Math.max(1, com.Harbinger.Spore.core.SConfig.SERVER.hunger.get()); }
    public boolean isHungry() { return com.Harbinger.Spore.core.SConfig.SERVER.should_starve.get() && hunger() >= Math.max(1, hungerLimit() / 2); }
    public boolean feeding() { return entityData.get(MEAL) >= 0; }
    public int mealTicks() { return entityData.get(MEAL_PROGRESS); }
    public boolean busy() { return absorbing() || feeding() || receiving(); }
    public void nourish(int units) {
        setMass(mass() + units); heal(Math.max(4, units * 2)); idleTicks = 0;
        setHunger(0); starvationSeconds = 0; hungerClock = 0;
    }
    /** Called once per loaded second; hunger never damages a larger lump directly. */
    public void hungerSecond() {
        if (!(level() instanceof ServerLevel server) || !isAlive() || Protection.sterile(server,blockPosition())
                || !com.Harbinger.Spore.core.SConfig.SERVER.should_starve.get()) return;
        setHunger(hunger() + (hunger() < hungerLimit() ? 1 : 0));
        if (hunger() < hungerLimit() || ++starvationSeconds < SHRINK_SECONDS) return;
        starvationSeconds = 0;
        if (mass() > 1) { setMass(mass() - 1); return; }
        var mushroom = com.Harbinger.Spore.core.Sblocks.FUNGAL_STEM_SAPLING.get().defaultBlockState();
        boolean planted = false;
        if (net.neoforged.neoforge.event.EventHooks.canEntityGrief(server,this)) {
            for (var pos : net.minecraft.core.BlockPos.betweenClosed(blockPosition().offset(-1,0,-1),blockPosition().offset(1,1,1))) {
                if (!Protection.sterile(server,pos) && server.getBlockState(pos).isAir() && mushroom.canSurvive(server,pos)
                        && server.setBlockAndUpdate(pos,mushroom)) { planted = true; break; }
            }
        }
        if (!planted) spawnAtLocation(com.Harbinger.Spore.core.Sblocks.FUNGAL_STEM_SAPLING.get());
        playSound(SoundEvents.SLIME_DEATH_SMALL,0.7F,0.5F); discard();
    }
    @Override public boolean doHurtTarget(Entity target) {
        return !busy() && target instanceof LivingEntity living && FungalEcology.prey(living) && super.doHurtTarget(target);
    }
    public boolean canAssimilate(Infected donor) {
        return isAlive() && donor.level() == level() && !absorbing() && !receiving()
            && (!feeding() || entityData.get(MEAL) == donor.getId()) && mass() < EVOLUTION_MASS
            && getTarget() == null && BiomassAssimilationGoal.eligible(donor)
            && level() instanceof ServerLevel server && !Protection.sterile(server,blockPosition());
    }
    public boolean beginAssimilation(Infected donor) {
        if (!(level() instanceof ServerLevel) || !canAssimilate(donor) || !BiomassAssimilationGoal.crowded(donor)
                || distanceToSqr(donor)>6.25 || !hasLineOfSight(donor)) return false;
        if (!feeding()) { entityData.set(MEAL,donor.getId()); entityData.set(MEAL_PROGRESS,0); }
        getNavigation().stop(); donor.getNavigation().stop(); return true;
    }
    private void cancelAssimilation() { entityData.set(MEAL,-1); entityData.set(MEAL_PROGRESS,0); }
    private void tickAssimilation(ServerLevel server) {
        var entity=server.getEntity(entityData.get(MEAL));
        if (!(entity instanceof Infected donor) || !canAssimilate(donor) || hurtTime>0
                || !BiomassAssimilationGoal.crowded(donor) || distanceToSqr(donor)>6.25 || !hasLineOfSight(donor)) {
            cancelAssimilation(); return;
        }
        getNavigation().stop(); donor.getNavigation().stop();
        setDeltaMovement(getDeltaMovement().multiply(0.15,1,0.15));
        int progress=mealTicks()+1; entityData.set(MEAL_PROGRESS,progress);
        var point=donor.position().add(0,0.5,0).lerp(position().add(0,0.3,0),(double)progress/ABSORB_TICKS);
        server.sendParticles(new DustParticleOptions(new Vector3f(0.73F,0.55F,0.58F),0.8F),
            point.x,point.y,point.z,5,0.12,0.12,0.12,0.02);
        if(progress>=ABSORB_TICKS) {
            nourish(Math.min(BiomassMath.MAX_SIZE_MASS,BiomassMath.fromHealth(donor.getMaxHealth())));
            donor.discard();cancelAssimilation();playSound(SoundEvents.SLIME_SQUISH,0.8F,0.5F);
        }
    }
    /** Every dropped item is food, including non-food and Spore items. */
    public boolean digest(net.minecraft.world.entity.item.ItemEntity item) {
        if (!(level() instanceof ServerLevel server) || !isAlive() || busy()
                || (mass() >= EVOLUTION_MASS && !isHungry()) || !edible(item) || distanceToSqr(item) > 2.25
                || !hasLineOfSight(item) || Protection.sterile(server, blockPosition())
                || Protection.sterile(server, item.blockPosition())
                || !net.neoforged.neoforge.event.EventHooks.canEntityGrief(server, this)) return false;
        var bite = item.getItem().copyWithCount(1);
        var remainder = item.getItem().copy(); remainder.shrink(1);
        if (remainder.isEmpty()) item.discard(); else item.setItem(remainder);
        nourish(mass() < EVOLUTION_MASS ? 1 : 0);
        server.sendParticles(new net.minecraft.core.particles.ItemParticleOption(
            net.minecraft.core.particles.ParticleTypes.ITEM, bite), getX(), getEyeY(), getZ(), 8, 0.2, 0.1, 0.2, 0.03);
        playSound(SoundEvents.GENERIC_EAT, 0.6F, 0.7F);
        return true;
    }
    private boolean edible(net.minecraft.world.entity.item.ItemEntity item) {
        return item.level() == level() && item.isAlive() && !item.hasPickUpDelay() && !item.getItem().isEmpty();
    }
    private final class ItemForagingGoal extends Goal {
        private net.minecraft.world.entity.item.ItemEntity food;
        private int pursuing;
        ItemForagingGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
        @Override public boolean canUse() {
            if (busy() || (mass() >= EVOLUTION_MASS && !isHungry()) || tickCount % 20 != 0
                    || !(level() instanceof ServerLevel server) || Protection.sterile(server, blockPosition())
                    || !net.neoforged.neoforge.event.EventHooks.canEntityGrief(server, InfectedBiomass.this)) return false;
            food = level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, getBoundingBox().inflate(12),
                i -> edible(i) && !Protection.sterile(server, i.blockPosition()) && hasLineOfSight(i)).stream()
                .sorted(Comparator.comparingDouble(InfectedBiomass.this::distanceToSqr))
                .filter(i -> { var path = getNavigation().createPath(i, 0); return path != null && path.canReach(); })
                .findFirst().orElse(null);
            return food != null;
        }
        @Override public void start() { pursuing = 0; }
        @Override public boolean canContinueToUse() {
            return food != null && edible(food) && !busy() && (mass() < EVOLUTION_MASS || isHungry())
                && distanceToSqr(food) < 256 && pursuing < 200;
        }
        @Override public void tick() {
            pursuing++;
            getLookControl().setLookAt(food, 30, 30);
            if (distanceToSqr(food) <= 2.25) {
                getNavigation().stop();
                if (tickCount % 20 == 0) digest(food);
            } else if (tickCount % 10 == 0) getNavigation().moveTo(food, 1);
        }
        @Override public void stop() { food = null; getNavigation().stop(); }
    }
    private final class CoalesceGoal extends Goal {
        private InfectedBiomass other;
        CoalesceGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (busy() || getTarget()!=null || idleTicks < BiomassMath.IDLE_TICKS || tickCount % 20 != 0 || mass() >= EVOLUTION_MASS) return false;
            other = level().getEntitiesOfClass(InfectedBiomass.class, getBoundingBox().inflate(12),
                b -> b != InfectedBiomass.this && b.isAlive() && !b.busy() && b.getTarget()==null && b.getId() < getId()
                    && b.idleTicks >= BiomassMath.IDLE_TICKS && !b.receiving() && b.mass() < EVOLUTION_MASS
                    && b.mass() + mass() <= BiomassMath.MAX_SIZE_MASS * 2).stream()
                .min(Comparator.comparingDouble(InfectedBiomass.this::distanceToSqr)).orElse(null);
            return other != null;
        }
        @Override public boolean canContinueToUse() { return other != null && other.isAlive() && !other.absorbing() && !other.feeding() && !busy() && getTarget()==null
            && idleTicks >= BiomassMath.IDLE_TICKS && other.idleTicks >= BiomassMath.IDLE_TICKS
            && other.mass() + mass() <= BiomassMath.MAX_SIZE_MASS * 2 && distanceToSqr(other) < 256; }
        @Override public void tick() {
            if (distanceToSqr(other) <= 2.25) beginAbsorption(other);
            else if (tickCount % 10 == 0) getNavigation().moveTo(other, 1);
        }
        @Override public void stop() { other = null; getNavigation().stop(); }
    }
}
