package com.jttw.engine.entity.wild;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;
import java.util.List;

/**
 * A wolfman (狼妖), a wolf yaoguai soldier of the kind that serves the demon kings: tall and lean,
 * a full wolf's head with a long muzzle, a ruff and a grey pelt darker down its back, in a cuirass
 * of black lamellae laced in red over leather, a red sash and a beast-faced bronze buckle, its legs
 * a wolf's, a bushy tail, and a long glaive (偃月刀) in its hands. Packs of two to four hunt the
 * forests and the taiga by night.
 *
 * <p>When it first sights a foe it throws back its head and <b>howls</b> ({@value #HOWL_TICKS}
 * ticks, standing): every wolfman within {@value #HOWL_RANGE} blocks that has no foe of its own
 * takes up the hunt. It howls again only after {@value #HOWL_EVERY} ticks. Alone at night it howls
 * now and then, at nothing.
 */
public class WolfmanEntity extends WildYaoguai {

    /** Eight ordinary hits; a glaive's cut of 20% of a same-level character's health. Quick. */
    public static final double HITS = 8, BLOW = 0.20, SPEED = 0.32;
    public static final int MIN_LEVEL = 10, MAX_LEVEL = 17;
    public static final int HOWL_TICKS = 30, HOWL_EVERY = 400;
    public static final double HOWL_RANGE = 32;

    private static final EntityDataAccessor<Integer> HOWLING = SynchedEntityData.defineId(WolfmanEntity.class, EntityDataSerializers.INT);

    private int howlCooldown;
    private LivingEntity howledFor;

    public WolfmanEntity(EntityType<? extends WolfmanEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(MIN_LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(MIN_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.ARMOR, 6)
                .add(Attributes.FOLLOW_RANGE, 36)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override public int minLevel() { return MIN_LEVEL; }
    @Override public int maxLevel() { return MAX_LEVEL; }
    @Override protected double hitsToKill() { return HITS; }
    @Override protected double blowShare() { return BLOW; }
    @Override public int vanillaXpShare() { return 2; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HOWLING, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new HowlGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.75));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** How far into a howl it is (0 when not howling): the model throws its head back. */
    public int howling() {
        return entityData.get(HOWLING);
    }

    /** For the gallery: shown mid-howl. */
    public void showHowling() {
        entityData.set(HOWLING, HOWL_TICKS / 2);
    }

    /**
     * Howls: throws back its head, and every wolfman within {@value #HOWL_RANGE} blocks with no foe of
     * its own takes up {@code foe} (none, if null: a howl at the moon). Returns those it called.
     */
    public List<WolfmanEntity> howl(LivingEntity foe) {
        entityData.set(HOWLING, HOWL_TICKS);
        howlCooldown = HOWL_EVERY;
        playSound(SoundEvents.WOLF_HOWL, 2.5f, 0.7f + random.nextFloat() * 0.1f);
        if (foe == null) return List.of();
        List<WolfmanEntity> pack = level().getEntitiesOfClass(WolfmanEntity.class, new AABB(blockPosition()).inflate(HOWL_RANGE),
                w -> w != this && w.isAlive() && w.getTarget() == null && !w.isAlliedTo(foe));
        pack.forEach(w -> w.setTarget(foe));
        return pack;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (howlCooldown > 0) howlCooldown--;
        int h = howling();
        if (h > 0) entityData.set(HOWLING, h - 1);
        LivingEntity t = getTarget();
        if (t != null && t.isAlive() && t != howledFor && howlCooldown == 0) {
            howledFor = t;
            howl(t);
        } else if (t == null && howlCooldown == 0 && !level().isDay() && random.nextInt(900) == 0) {
            howl(null);
        }
    }

    /** While it howls it stands: nothing else moves it. */
    static final class HowlGoal extends Goal {
        private final WolfmanEntity wolf;

        HowlGoal(WolfmanEntity wolf) {
            this.wolf = wolf;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return wolf.howling() > 0;
        }

        @Override
        public void start() {
            wolf.getNavigation().stop();
        }

        @Override
        public void tick() {
            wolf.getNavigation().stop();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("HowlCooldown", howlCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        howlCooldown = tag.getInt("HowlCooldown");
    }

    @Override
    public float getVoicePitch() {
        return 0.75f + (random.nextFloat() - 0.5f) * 0.1f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? SoundEvents.WOLF_GROWL : SoundEvents.WOLF_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }
}
