package com.jttw.engine.entity.fire;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.fire.SamadhiFire;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * One of Red Boy's Six Strong Generals (六健将, chapters 40–42), who keep the Fire Cloud Cave and
 * whom he sent to fetch his father. Demon officers in lacquered armour, level 74, always of a
 * captain's strength (purple-named, with a captain's sigil to leave), never coming in packs: they
 * stand at the cave's gate and in its hall. All six share one shape; each has his own colours and
 * his own trick, every {@value #TRICK_EVERY} ticks or so while he fights:
 * <ol start="0">
 *   <li><b>Cloud-Soaring Sprite</b> (云里雾): leaps high at his foe and comes down in a burst of
 *       cloud, striking and blinding all round where he lands;</li>
 *   <li><b>Fog-Diving Monster</b> (雾里云): vanishes into fog and comes out behind his foe;</li>
 *   <li><b>Mad-Hurrying Wind</b> (急如火, "swift as fire"): charges headlong, fire springing up in
 *       his track;</li>
 *   <li><b>Swift-Rushing Wind</b> (快如风, "quick as wind"): the fastest of them; a gust from him
 *       hurls all near him away;</li>
 *   <li><b>Flame-Head General</b> (兴烘掀): his head ablaze, he bursts into flame, burning all
 *       round him;</li>
 *   <li><b>Eye-Spark Officer</b> (掀烘兴): sparks fly from his eyes at his foe.</li>
 * </ol>
 */
public class StrongGeneralEntity extends FireMonster {

    /** The six, in the order above. */
    public static final String[] NAMES = {"cloud_soaring_sprite", "fog_diving_monster", "mad_hurrying_wind",
            "swift_rushing_wind", "flame_head_general", "eye_spark_officer"};
    public static final int CLOUD = 0, FOG = 1, MAD_WIND = 2, SWIFT_WIND = 3, FLAME_HEAD = 4, EYE_SPARK = 5;

    /** Ten ordinary hits (twenty-five at a captain's strength); a blow of 24% of health; his trick 60% of a blow. */
    public static final double HITS = 10, BLOW = 0.24, TRICK = 0.6, SPEED = 0.27;
    public static final int TRICK_EVERY = 100;
    /** How far his cloud, his gust and his flame reach. */
    public static final double BURST = 4.0;
    /** How far he keeps from where he was put. */
    public static final int POST = 16;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(StrongGeneralEntity.class, EntityDataSerializers.INT);

    private int trick = TRICK_EVERY / 2;
    /** Whether he is in the air from a leap (the Cloud-Soaring Sprite), and for how much longer his charge burns a track. */
    private boolean leaping;
    private int charging;

    public StrongGeneralEntity(EntityType<? extends StrongGeneralEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(CAPTAIN_LEVEL) * CAPTAIN_HEALTH)
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(CAPTAIN_LEVEL))
                .add(Attributes.MOVEMENT_SPEED, SPEED)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6)
                .add(Attributes.ARMOR, 12)
                .add(Attributes.ATTACK_KNOCKBACK, 0.8)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    /** They come alone. */
    @Override
    public int escorts() {
        return 0;
    }

    @Override
    public float captainChance() {
        return 1f;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
    }

    /** Which of the six he is (0-5). */
    public int variant() {
        return entityData.get(VARIANT);
    }

    public void setVariant(int v) {
        entityData.set(VARIANT, Math.floorMod(v, NAMES.length));
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(variant() == SWIFT_WIND ? 0.36 : variant() == MAD_WIND ? 0.31 : SPEED);
        }
        if (getAttribute(Attributes.ATTACK_KNOCKBACK) != null) {
            getAttribute(Attributes.ATTACK_KNOCKBACK).setBaseValue(variant() == SWIFT_WIND ? 2.5 : 0.8);
        }
    }

    /** Always of a captain's strength, whoever brings him. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (!isCaptain()) become(rollCaptainLevel(random), true);
        if (reason != MobSpawnType.STRUCTURE && reason != MobSpawnType.MOB_SUMMONED) setVariant(random.nextInt(NAMES.length));
        restrictTo(blockPosition(), POST);
        return result;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 14f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** His name, his level: purple, a general's (not "Captain": he is more). */
    @Override
    protected Component getTypeName() {
        return Component.empty().append(Component.translatable("entity.jttw_engine.strong_general." + NAMES[variant()]))
                .append(" · Lv " + mobLevel()).withStyle(s -> s.withColor(TextColor.fromRgb(PURPLE)));
    }

    // ── His trick ───────────────────────────────────────────────────────

    /** His trick's hurt. */
    public float trickDamage() {
        return (float) (blowAt(mobLevel(), true) * TRICK);
    }

    /** Whom a burst round {@code at} may touch: the living near it, but never the mountain's own. */
    private List<LivingEntity> foesWithin(Vec3 at, double range) {
        return level().getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(range),
                e -> e != this && e.isAlive() && !SamadhiFire.spared(e) && !isAlliedTo(e)
                        && e.position().distanceToSqr(at) <= range * range);
    }

    /** His trick, on {@code target}: whichever is his. Returns whether he did it. */
    public boolean trick(LivingEntity target) {
        return switch (variant()) {
            case CLOUD -> leapAt(target);
            case FOG -> diveBehind(target);
            case MAD_WIND -> charge(target);
            case SWIFT_WIND -> gust() > 0;
            case FLAME_HEAD -> {
                flameBurst();
                yield true;
            }
            default -> !sparks(target).isEmpty();
        };
    }

    /** The Cloud-Soaring Sprite's leap: high, at his foe; his cloud bursts where he lands. */
    public boolean leapAt(LivingEntity target) {
        if (!onGround()) return false;
        Vec3 flat = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        double d = flat.length();
        Vec3 lean = d > 1e-3 ? flat.scale(Math.min(1.1, d * 0.1) / d) : Vec3.ZERO;
        setDeltaMovement(lean.x, 0.95, lean.z);
        hurtMarked = true;
        leaping = true;
        playSound(SoundEvents.PHANTOM_FLAP, 1.2f, 0.7f);
        return true;
    }

    /** Where the Cloud-Soaring Sprite lands: a burst of cloud, striking and blinding all round. Returns how many. */
    public int cloudBurst() {
        List<LivingEntity> caught = foesWithin(position(), BURST);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), trickDamage());
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), this);
        }
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.5, getZ(), 80, BURST / 2, 0.4, BURST / 2, 0.08);
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.6f, 1.4f);
        return caught.size();
    }

    /** The Fog-Diving Monster's dive: gone in a puff of fog, out two blocks behind his foe, unseen a moment. */
    public boolean diveBehind(LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 back = new Vec3(-look.x, 0, -look.z);
        back = back.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : back.normalize();
        Vec3 to = target.position().add(back.scale(2.0));
        BlockPos stand = standingSpot((ServerLevel) level(), this, BlockPos.containing(to));
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        teleportTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
        lookAt(target, 360f, 360f);
        addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 30, 0, false, false));
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1, getZ(), 40, 0.4, 0.8, 0.4, 0.02);
        playSound(SoundEvents.ENDERMAN_TELEPORT, 0.8f, 0.6f);
        return true;
    }

    /** The Mad-Hurrying Wind's charge: headlong at his foe, fire springing up in his track a moment. */
    public boolean charge(LivingEntity target) {
        Vec3 flat = new Vec3(target.getX() - getX(), 0, target.getZ() - getZ());
        if (flat.lengthSqr() < 1e-4) return false;
        flat = flat.normalize().scale(1.3);
        setDeltaMovement(flat.x, 0.15, flat.z);
        hurtMarked = true;
        charging = 14;
        playSound(SoundEvents.BLAZE_SHOOT, 1.2f, 0.6f);
        return true;
    }

    /** The Swift-Rushing Wind's gust: all near him hurled away. Returns how many. */
    public int gust() {
        List<LivingEntity> caught = foesWithin(position(), BURST + 1);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), trickDamage() * 0.5f);
            Vec3 away = e.position().subtract(position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : away.normalize();
            e.push(away.x * 2.0, 0.5, away.z * 2.0);
            e.hurtMarked = true;
        }
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.GUST, getX(), getY() + 1, getZ(), 6, 1.5, 0.5, 1.5, 0);
        playSound(SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.2f, 0.8f);
        return caught.size();
    }

    /** The Flame-Head General's burst: he flares up, burning all round him. Returns how many. */
    public int flameBurst() {
        List<LivingEntity> caught = foesWithin(position(), BURST);
        for (LivingEntity e : caught) {
            e.hurt(damageSources().mobAttack(this), trickDamage());
            e.igniteForSeconds(5);
        }
        if (level() instanceof ServerLevel server) server.sendParticles(ParticleTypes.FLAME, getX(), getY() + 1, getZ(), 120, BURST / 2, 0.8, BURST / 2, 0.06);
        playSound(SoundEvents.BLAZE_SHOOT, 1.5f, 0.5f);
        return caught.size();
    }

    /** The Eye-Spark Officer's sparks: two embers from his eyes at his foe. Returns them. */
    public List<EmberBoltEntity> sparks(LivingEntity target) {
        List<EmberBoltEntity> flung = new ArrayList<>();
        Vec3 to = new Vec3(target.getX() - getX(), target.getY(0.5) - getEyeY(), target.getZ() - getZ());
        for (int i = -1; i <= 1; i += 2) {
            EmberBoltEntity bolt = new EmberBoltEntity(level(), this, to.yRot((float) Math.toRadians(5 * i)), trickDamage() * 0.6f);
            level().addFreshEntity(bolt);
            flung.add(bolt);
        }
        playSound(SoundEvents.BLAZE_SHOOT, 1f, 1.2f);
        return flung;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (trick > 0) trick--;
        if (leaping && onGround() && getDeltaMovement().y <= 0) {
            leaping = false;
            cloudBurst();
        }
        if (charging > 0) {
            charging--;
            BlockPos at = blockPosition();
            if (level().isEmptyBlock(at) && net.neoforged.neoforge.event.EventHooks.canEntityGrief(level(), this)
                    && level().getBlockState(at.below()).isFaceSturdy(level(), at.below(), net.minecraft.core.Direction.UP)) {
                level().setBlockAndUpdate(at, BaseFireBlock.getState(level(), at));
            }
        }
        LivingEntity t = getTarget();
        if (trick == 0 && t != null && t.isAlive() && distanceTo(t) < (variant() == EYE_SPARK ? 18 : 10) && hasLineOfSight(t)) {
            boolean close = distanceTo(t) < BURST;
            boolean wants = switch (variant()) {
                case SWIFT_WIND, FLAME_HEAD -> close;
                case MAD_WIND -> distanceTo(t) > 3;
                default -> true;
            };
            if (wants && trick(t)) trick = TRICK_EVERY + random.nextInt(30);
        }
    }

    /** A guard of the cave, put there once: kept, even on Peaceful (where no blow of his lands on a player). */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return variant() != CLOUD && super.causeFallDamage(distance, multiplier, source);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide && variant() == FLAME_HEAD && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.FLAME, getRandomX(0.3), getY() + getBbHeight() + 0.05, getRandomZ(0.3), 0, 0.05, 0);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("General", variant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("General"));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_BRUTE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.8f;
    }
}
