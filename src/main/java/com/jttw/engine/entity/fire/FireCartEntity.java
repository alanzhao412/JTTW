package com.jttw.engine.entity.fire;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.fire.SamadhiFire;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * One of Red Boy's five little carts (五辆小车, chapter 41), set out round him in the five-element
 * pattern (a pentagon: metal, wood, water, fire, earth) when his health falls past two thirds,
 * and lit with his Samadhi fire. Each stands where it was set and every {@value #PUFF_EVERY}
 * ticks belches the fire round itself; while any of his carts stands, the five-element fire wards
 * him and every blow on him is halved ({@link RedBoyEntity#CART_WARD}). Break them first:
 * {@value #HITS} ordinary hits of a level-77 character each. It is a thing, not a creature: it
 * leaves nothing, pays no XP, and burns out once its master is gone.
 */
public class FireCartEntity extends Mob {

    public static final double HITS = 5;
    /** How often it belches the fire, how far round itself, and for how long it sets one burning. */
    public static final int PUFF_EVERY = 60, PUFF_SECONDS = 3;
    public static final double PUFF_RANGE = 2.5;

    @Nullable
    private UUID master;
    private int puff;

    public FireCartEntity(EntityType<? extends FireCartEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
        puff = PUFF_EVERY / 2 + random.nextInt(PUFF_EVERY);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(RedBoyEntity.LEVEL))
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0);
    }

    /** Whose cart it is. */
    public void setMaster(LivingEntity boy) {
        master = boy.getUUID();
    }

    @Nullable
    public UUID master() {
        return master;
    }

    /** Whether it is {@code boy}'s, and still burning. */
    public boolean wards(LivingEntity boy) {
        return isAlive() && boy.getUUID().equals(master);
    }

    /** Belches the Samadhi fire round itself: all near it (but its master's own) set burning, flames on the ground. Returns who. */
    public List<LivingEntity> puff() {
        List<LivingEntity> caught = level().getEntitiesOfClass(LivingEntity.class, new AABB(blockPosition()).inflate(PUFF_RANGE),
                e -> e != this && e.isAlive() && !SamadhiFire.spared(e) && e.distanceToSqr(this) <= (PUFF_RANGE + 0.5) * (PUFF_RANGE + 0.5));
        for (LivingEntity e : caught) SamadhiFire.ignite(e, PUFF_SECONDS, 0);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 0.8, getZ(), 40, PUFF_RANGE / 2, 0.4, PUFF_RANGE / 2, 0.05);
            for (int i = 0; i < 4; i++) {
                double a = i * Math.PI / 2 + random.nextDouble();
                BlockPos at = BlockPos.containing(getX() + Math.cos(a) * 2, getY() + 0.5, getZ() + Math.sin(a) * 2);
                for (int down = 0; down < 3 && !SamadhiFire.place(level(), at); down++) at = at.below();
            }
        }
        playSound(SoundEvents.BLAZE_SHOOT, 1f, 0.6f);
        return caught;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (random.nextInt(2) == 0) level().addParticle(ParticleTypes.FLAME, getRandomX(0.5), getY() + 0.9 + random.nextDouble() * 0.4, getRandomZ(0.5), 0, 0.03, 0);
            if (random.nextInt(8) == 0) level().addParticle(ParticleTypes.SMOKE, getRandomX(0.4), getY() + 1.4, getRandomZ(0.4), 0, 0.05, 0);
            return;
        }
        if (--puff <= 0) {
            puff = PUFF_EVERY;
            puff();
        }
        // its master gone (slain, or never there), it burns out
        if (tickCount % 20 == 0 && (master == null || !(((ServerLevel) level()).getEntity(master) instanceof RedBoyEntity boy) || !boy.isAlive())) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.6, getZ(), 20, 0.4, 0.4, 0.4, 0.02);
            discard();
        }
    }

    // ── A thing ─────────────────────────────────────────────────────────

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity other) {}

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return other instanceof RedBoyEntity || other instanceof FireMonster || super.isAlliedTo(other);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    /** Only a foe's blows (or arrows, or abilities) break it; fire, its own or the mountain's, is nothing to it. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if ((source.getEntity() instanceof LivingEntity by && !SamadhiFire.spared(by) && !(by instanceof FireCartEntity))
                || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOOD_BREAK;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHIELD_BREAK;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (master != null) tag.putUUID("Master", master);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Master")) master = tag.getUUID("Master");
    }
}
