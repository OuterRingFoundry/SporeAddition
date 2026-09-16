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
    public static final int EVOLUTION_MASS = 8, ABSORB_TICKS = 40;
    private static final EntityDataAccessor<Integer> MASS = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RECIPIENT = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PROGRESS = SynchedEntityData.defineId(InfectedBiomass.class, EntityDataSerializers.INT);
    private String origin = "";
    private int evolveCooldown;

    public InfectedBiomass(EntityType<? extends InfectedBiomass> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 12).add(Attributes.MOVEMENT_SPEED, 0.18)
            .add(Attributes.FOLLOW_RANGE, 16).add(Attributes.ATTACK_DAMAGE, 0);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder); builder.define(MASS, 1); builder.define(RECIPIENT, -1); builder.define(PROGRESS, 0);
    }
    public int mass() { return entityData.get(MASS); }
    public void setMass(int value) { entityData.set(MASS, Math.clamp(value, 1, EVOLUTION_MASS)); }
    public int absorptionTicks() { return entityData.get(PROGRESS); }
    public boolean absorbing() { return entityData.get(RECIPIENT) >= 0; }
    public void setOrigin(String value) { origin = value; }
    public String origin() { return origin; }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new CoalesceGoal());
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new RandomLookAroundGoal(this));
    }
    @Override protected boolean shouldDespawnInPeaceful() { return true; }
    @Override public boolean canBreatheUnderwater() { return true; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }
    @Override public boolean isAlliedTo(Entity entity) { return Protection.spore(entity) || super.isAlliedTo(entity); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SLIME_HURT_SMALL; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.SLIME_DEATH_SMALL; }

    public boolean beginAbsorption(Mob recipient) {
        if (level().isClientSide || absorbing() || !validRecipient(recipient) || receiving()) return false;
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
            return !biomass.absorbing() && biomass.mass() + mass() <= EVOLUTION_MASS;
        return target instanceof Infected infected && infected.isStarving();
    }
    @Override public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server) || !isAlive()) return;
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
                if (recipient instanceof InfectedBiomass biomass) { biomass.setMass(biomass.mass() + mass()); biomass.heal(mass() * 2); }
                else if (recipient instanceof Infected infected) {
                    infected.setHunger(0); infected.removeEffect(Seffects.STARVATION);
                    infected.setEvoPoints(infected.getEvoPoints() + mass()); infected.heal(mass() * 2);
                }
                recipient.playSound(SoundEvents.SLIME_SQUISH, 0.7F, 0.7F);
                discard();
            }
        } else if (mass() >= EVOLUTION_MASS && --evolveCooldown <= 0) {
            evolveCooldown = 100;
            evolve(server);
        }
    }
    private void cancelAbsorption() { entityData.set(RECIPIENT, -1); entityData.set(PROGRESS, 0); }
    public boolean evolve(ServerLevel server) {
        if (mass() < EVOLUTION_MASS || absorbing() || !isAlive() || Protection.sterile(server, blockPosition())) return false;
        EntityType<?> type = random.nextBoolean() ? com.Harbinger.Spore.core.Sentities.SLASHER.get() : com.Harbinger.Spore.core.Sentities.BRUTE.get();
        Entity result = type.create(server);
        if (!(result instanceof Infected infected)) return false;
        infected.moveTo(getX(), getY(), getZ(), getYRot(), 0);
        if (!server.noCollision(infected)) return false;
        infected.finalizeSpawn(server, server.getCurrentDifficultyAt(blockPosition()), MobSpawnType.CONVERSION, null);
        infected.setCustomName(getCustomName()); infected.setPersistenceRequired(); infected.setHunger(0);
        if (!server.addFreshEntity(infected)) return false;
        playSound(SoundEvents.SLIME_SQUISH, 1, 0.4F); discard(); return true;
    }
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag); tag.putInt("BiomassMass", mass()); tag.putString("BiomassOrigin", origin);
        // An interrupted animation restarts after reload; it never persists half a transfer.
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag); setMass(tag.getInt("BiomassMass")); origin = tag.getString("BiomassOrigin"); cancelAbsorption();
    }
    private final class CoalesceGoal extends Goal {
        private InfectedBiomass other;
        CoalesceGoal() { setFlags(EnumSet.of(Flag.MOVE)); }
        @Override public boolean canUse() {
            if (absorbing() || tickCount % 20 != 0 || mass() >= EVOLUTION_MASS) return false;
            other = level().getEntitiesOfClass(InfectedBiomass.class, getBoundingBox().inflate(12),
                b -> b != InfectedBiomass.this && b.isAlive() && !b.absorbing() && b.getId() < getId()
                    && b.mass() + mass() <= EVOLUTION_MASS).stream()
                .min(Comparator.comparingDouble(InfectedBiomass.this::distanceToSqr)).orElse(null);
            return other != null;
        }
        @Override public boolean canContinueToUse() { return other != null && other.isAlive() && !other.absorbing() && !absorbing() && other.mass() + mass() <= EVOLUTION_MASS; }
        @Override public void tick() {
            if (distanceToSqr(other) <= 2.25) beginAbsorption(other);
            else if (tickCount % 10 == 0) getNavigation().moveTo(other, 1);
        }
        @Override public void stop() { other = null; getNavigation().stop(); }
    }
}
