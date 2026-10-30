package com.jttw.engine.entity.waters;

import com.jttw.engine.entity.MobScaling;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * A Yaksha (夜叉) of the sea patrol, the Dragon Kings' watchmen, who go the rounds of the sea in
 * bands: a blue-skinned demon with a shock of red hair, horns and tusks, armed with a trident. It
 * swims fast (only the Fish Soldiers are quicker in the water); at a distance it hurls bolts of the tide that throw what
 * they strike back, close in it thrusts with its trident; and when it finds a foe it raises the
 * alarm, and every creature of the sea near by turns on that foe too.
 */
public class YakshaEntity extends SeaMonster {

    /** Five of a same-level character's ordinary hits to kill; a thrust of 10% of their health, a bolt of 8%. */
    public static final double HITS = 5, THRUST = 0.10, BOLT = 0.08;
    /** Within this it thrusts; beyond, up to its reach, it hurls bolts, one each so many ticks. */
    public static final double CLOSE = 4, BOLT_REACH = 18;
    public static final int BOLT_EVERY = 50;
    /** How far its alarm carries. */
    public static final double ALARM = 20;
    /**
     * Its thrust, a swing of so many ticks: drawn back and cocked, then driven home (the blow lands
     * {@value #STAB} ticks in, as the trident drives out), held, and drawn back.
     */
    public static final int THRUST_TICKS = 20, STAB = 8;

    private int bolt = BOLT_EVERY / 2;
    /** The foe its thrust is driving at, and the ticks until it lands (0: none under way). */
    @Nullable
    private LivingEntity stabbing;
    private int stab;

    public YakshaEntity(EntityType<? extends YakshaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, THRUST * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ARMOR, 2)
                .add(Attributes.FOLLOW_RANGE, 40);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return THRUST;
    }

    @Override
    protected double[] specialShares() {
        return new double[]{BOLT};
    }

    /** Its bolt: a share of a same-level character's health, as its blow is. */
    public float boltDamage() {
        return hitOf(BOLT);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.TRIDENT));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
    }

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level, DifficultyInstance difficulty,
            net.minecraft.world.entity.MobSpawnType reason, @Nullable net.minecraft.world.entity.SpawnGroupData data) {
        populateDefaultEquipmentSlots(random, difficulty);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true) {
            @Override
            protected void checkAndPerformAttack(LivingEntity target) {
                if (canPerformAttack(target)) {
                    resetAttackCooldown();
                    thrust(target);
                }
            }
        });
        goalSelector.addGoal(5, new RandomSwimmingGoal(this, 0.9, 20));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public int getCurrentSwingDuration() {
        return THRUST_TICKS;
    }

    /** Begins a thrust at {@code target}: the wind-up shows, and the blow lands {@value #STAB} ticks on if it is still in reach. */
    public void thrust(LivingEntity target) {
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        stabbing = target;
        stab = STAB;
    }

    /** Hurls a bolt of the tide at {@code target}. */
    public SeaBoltEntity hurl(LivingEntity target) {
        Vec3 way = new Vec3(target.getX() - getX(), target.getY(0.5) - getEyeY(), target.getZ() - getZ());
        SeaBoltEntity b = new SeaBoltEntity(level(), this, way, boltDamage(), SeaBoltEntity.TIDE);
        level().addFreshEntity(b);
        swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        playSound(SoundEvents.TRIDENT_THROW.value(), 1f, 0.8f);
        return b;
    }

    /**
     * The alarm: every creature of the sea within {@value #ALARM} blocks with no foe of its own
     * turns on {@code foe}. Returns how many answered.
     */
    public int raiseAlarm(LivingEntity foe) {
        int answered = 0;
        for (SeaMonster m : level().getEntitiesOfClass(SeaMonster.class, getBoundingBox().inflate(ALARM), m -> m != this && m.getTarget() == null)) {
            m.setTarget(foe);
            answered++;
        }
        if (answered > 0) playSound(SoundEvents.DROWNED_AMBIENT_WATER, 2f, 0.5f);
        return answered;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        LivingEntity before = getTarget();
        super.setTarget(target);
        if (!level().isClientSide && target instanceof Player && before != target && getTarget() == target) raiseAlarm(target);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (stab > 0 && --stab == 0) {
            LivingEntity foe = stabbing;
            stabbing = null;
            if (foe != null && foe.isAlive() && isWithinMeleeAttackRange(foe)) doHurtTarget(foe);
        }
        LivingEntity target = getTarget();
        if (bolt > 0) bolt--;
        if (target == null || !target.isAlive() || bolt > 0) return;
        double d = distanceTo(target);
        if (d > CLOSE && d < BOLT_REACH && hasLineOfSight(target)) {
            hurl(target);
            bolt = BOLT_EVERY;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DROWNED_AMBIENT_WATER;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.DROWNED_HURT_WATER;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DROWNED_DEATH_WATER;
    }
}
