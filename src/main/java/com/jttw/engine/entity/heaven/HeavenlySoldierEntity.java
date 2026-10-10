package com.jttw.engine.entity.heaven;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * A soldier of Heaven's host, sent down on a cloud after a player Heaven has outlawed (see
 * {@link Morality}). It flies, as all of Heaven's soldiers ride their clouds; it fights with a
 * halberd; it is levelled to its quarry, so Heaven's wrath never falls behind the one it hunts.
 * Now and then one is a Heavenly General, purple-named, larger and far stronger.
 *
 * <p>It hunts only the wanted: its quarry first, and any other outlaw it finds, and whoever
 * strikes it. With no one left to fight it rides back up to Heaven.
 */
public class HeavenlySoldierEntity extends Monster {

    /** Four of a same-level character's ordinary hits to kill; a blow of 12% of their health, before resistance. */
    public static final double HITS = 4, BLOW = 0.12;
    /** A general's health and blow over a soldier's of its level. */
    public static final double GENERAL_HEALTH = 3, GENERAL_DAMAGE = 1.4;
    public static final float GENERAL_SCALE = 1.2f;
    /** Ticks with no one to fight before it returns to Heaven: 10 s. */
    public static final int IDLE_TICKS = 200;
    public static final int WHITE = 0xFFFFFF, PURPLE = 0xB04DFF;

    private static final EntityDataAccessor<Boolean> GENERAL = SynchedEntityData.defineId(HeavenlySoldierEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> MOB_LEVEL = SynchedEntityData.defineId(HeavenlySoldierEntity.class, EntityDataSerializers.INT);

    @Nullable private UUID quarry;
    private int idle;

    public HeavenlySoldierEntity(EntityType<? extends HeavenlySoldierEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 12;
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.HEAVENLY_HALBERD.get()));
        setDropChance(EquipmentSlot.MAINHAND, 0.04f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                // as a level-20 one; each is set to its own level (become)
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(20))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(20))
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(GENERAL, false);
        entityData.define(MOB_LEVEL, 20);
    }

    public boolean isGeneral() {
        return entityData.get(GENERAL);
    }

    public int mobLevel() {
        return entityData.get(MOB_LEVEL);
    }

    @Nullable
    public UUID quarry() {
        return quarry;
    }

    /** Sets what it is: its level, whether a general, the health and blow that go with them, and whom it hunts. */
    public void become(int level, boolean general, @Nullable UUID quarry) {
        entityData.set(GENERAL, general);
        entityData.set(MOB_LEVEL, level);
        this.quarry = quarry;
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(HITS * MobScaling.typicalHit(level) * (general ? GENERAL_HEALTH : 1));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BLOW * MobScaling.typicalHealth(level) * (general ? GENERAL_DAMAGE : 1));
        setHealth(getMaxHealth());
        getPersistentData().putString(com.jttw.engine.entity.ridge.RidgeMonster.KEY_TIER, (general ? EnemyTier.GENERAL : EnemyTier.GRUNT).name());
        xpReward = general ? 50 : 12;
        refreshDimensions();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new HuntQuarryGoal(this));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                e -> e instanceof Player p && Morality.wanted(p) > 0 && !p.isCreative() && !p.isSpectator()));
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
        if (level().isClientSide) return;
        // with no one to fight (or its quarry pardoned), it rides back up to Heaven
        Player q = quarry == null ? null : level().getPlayerByUUID(quarry);
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

    /** Its name with its level: white, or purple for a general. */
    @Override
    protected Component getTypeName() {
        Component name = isGeneral() ? Component.translatable("entity.jttw_engine.heavenly_general") : super.getTypeName();
        return Component.empty().append(name).append(" · Lv " + mobLevel())
                .withStyle(s -> s.withColor(TextColor.fromRgb(isGeneral() ? PURPLE : WHITE)));
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    @Override
    public float getScale() {
        return isGeneral() ? GENERAL_SCALE : 1f;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GENERAL.equals(key)) refreshDimensions();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos) {}

    /** Heaven's soldiers are not of the world below: they never stay when no one is near. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return true;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ARMOR_EQUIP_GOLD;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("General", isGeneral());
        tag.putInt("MobLevel", mobLevel());
        if (quarry != null) tag.putUUID("Quarry", quarry);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(GENERAL, tag.getBoolean("General"));
        if (tag.contains("MobLevel")) entityData.set(MOB_LEVEL, tag.getInt("MobLevel"));
        quarry = tag.hasUUID("Quarry") ? tag.getUUID("Quarry") : null;
        refreshDimensions();
    }
}
