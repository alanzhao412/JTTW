package com.jttw.engine.entity.heaven;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A soldier of Heaven's host (天兵), a halberdier in gilt armour on a small white cloud: Heaven's
 * commonest regular, met everywhere in it, of the road's level where it stands. Its captain is
 * purple-named; now and then one is a Heavenly General (天将), a miniboss.
 *
 * <p>Sent after a player Heaven has outlawed ({@link com.jttw.engine.world.morality.HeavenlyHost}), it
 * carries its quarry ({@link #become(int, boolean, UUID)}): it hunts them first, and any other outlaw it
 * finds, and with no one left to fight rides back up into the clouds.
 */
public class HeavenlySoldierEntity extends HeavenMonster {

    /** Four of a same-level character's ordinary hits to kill; a blow of 9% of their health, before resistance. */
    public static final double HITS = 4, BLOW = 0.09;
    /** Ticks with no one to fight before one sent after an outlaw returns to the clouds: 10 s. */
    public static final int IDLE_TICKS = 200;

    @Nullable
    private UUID quarry;
    private int idle;

    public HeavenlySoldierEntity(EntityType<? extends HeavenlySoldierEntity> type, Level level) {
        super(type, level);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HEAVENLY_HALBERD.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.04f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(20))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(20))
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    /** Whether it is a Heavenly General (a miniboss) or a captain: purple, in crimson. */
    public boolean isGeneral() {
        return isCaptain();
    }

    @Nullable
    public UUID quarry() {
        return quarry;
    }

    /**
     * Sets what it is, sent after {@code quarry} (or null): its level, and whether a Heavenly General
     * (a miniboss) at the host's head.
     */
    public void become(int level, boolean general, @Nullable UUID quarry) {
        if (general) becomeMiniboss(level, 1);
        else become(level, false);
        this.quarry = quarry;
    }

    @Override
    protected Component kindName() {
        return isMiniboss() ? Component.translatable("entity.jttw_engine.heavenly_general") : super.kindName();
    }

    @Override
    protected boolean namedRank() {
        return isMiniboss();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        targetSelector.addGoal(2, new HuntQuarryGoal(this));
    }

    /** Its quarry, wherever it is in this world: it was sent for them. */
    private static final class HuntQuarryGoal extends TargetGoal {
        private final HeavenlySoldierEntity soldier;

        HuntQuarryGoal(HeavenlySoldierEntity soldier) {
            super(soldier, false);
            this.soldier = soldier;
        }

        @Nullable
        private Player quarry() {
            if (soldier.quarry == null) return null;
            Player p = soldier.level().getPlayerByUUID(soldier.quarry);
            return p != null && p.isAlive() && !p.isCreative() && !p.isSpectator() ? p : null;
        }

        @Override
        public boolean canUse() {
            Player p = quarry();
            if (p == null || p == soldier.getTarget()) return false;
            targetMob = p;
            return true;
        }

        @Override
        public void start() {
            soldier.setTarget(targetMob);
            super.start();
        }

        @Override
        protected double getFollowDistance() {
            return 128;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || quarry == null) return;
        // with no one to fight (or its quarry pardoned), one sent after an outlaw rides back up into the clouds
        Player q = level().getPlayerByUUID(quarry);
        boolean pardoned = q != null && Morality.wanted(q) == 0 && getTarget() == q;
        if (pardoned) setTarget(null);
        idle = getTarget() == null ? idle + 1 : 0;
        if (idle > IDLE_TICKS) returnToHeaven();
    }

    /** Goes back up in a puff of cloud. */
    public void returnToHeaven() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 20, 0.5, 0.4, 0.5, 0.02);
        }
        discard();
    }

    /** One sent after an outlaw never stays when no one is near. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return quarry != null || super.removeWhenFarAway(distance);
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ARMOR_EQUIP_GOLD.value();
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (quarry != null) tag.putUUID("Quarry", quarry);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
        // as saved before Heaven's host had ranks: a general was a flag of its own
        if (tag.getBoolean("General") && !isCaptain()) becomeMiniboss(mobLevel(), 1);
    }
}
