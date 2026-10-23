package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * The Black and White Impermanence (黑白无常), the pair who come for the souls of the dying, in
 * tall hats with their mottoes on them. They go together: one met in the world comes with the
 * other. Each is the other's strength:
 * <ul>
 *   <li><b>the Black</b> (黑无常, Fan Wujiu, short and stern, "天下太平") shackles what he strikes:
 *       slowed hard for a moment;</li>
 *   <li><b>the White</b> (白无常, Xie Bi'an, tall and pale, his long red tongue out, "一见生财")
 *       lashes from further off, and what his tongue takes, he drinks: half of it mends him;</li>
 *   <li><b>together</b>, near each other, each takes a quarter less from every blow; when one
 *       falls, the other is <b>enraged</b> (stronger and quicker for good).</li>
 * </ul>
 * One class for both; which it is follows from its type.
 */
public class ImpermanenceEntity extends HellMonster {

    public static final int LEVEL = 59;
    /** The Black: hits to kill, blow; the White's. */
    public static final double BLACK_HITS = 6, BLACK_BLOW = 0.15, WHITE_HITS = 4.5, WHITE_BLOW = 0.12;
    /** The Black's shackles: how long, how hard; the White's draught: the share of his blow he drinks. */
    public static final int SHACKLED = 40, SHACKLE_LEVEL = 2;
    public static final double DRINK = 0.5;
    /** Near each other (within this), each takes this share of a blow. */
    public static final double TOGETHER = 16, GUARDED = 0.75;
    public static final String KEY_PAIR = "jttw_impermanence_pair";

    private static final EntityDataAccessor<Boolean> ENRAGED = SynchedEntityData.defineId(ImpermanenceEntity.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID partner;

    public ImpermanenceEntity(EntityType<? extends ImpermanenceEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes(boolean white) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, (white ? WHITE_HITS : BLACK_HITS) * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, (white ? WHITE_BLOW : BLACK_BLOW) * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, white ? 0.29 : 0.27)
                .add(Attributes.FOLLOW_RANGE, 32)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    /** Whether this is the White (else the Black). */
    public boolean white() {
        return getType() == ModEntities.WHITE_IMPERMANENCE.get();
    }

    @Override
    public int regularLevel() {
        return LEVEL;
    }

    @Override
    protected double hitsToKill() {
        return white() ? WHITE_HITS : BLACK_HITS;
    }

    @Override
    protected double blowShare() {
        return white() ? WHITE_BLOW : BLACK_BLOW;
    }

    /** A captain brings none of its own kind: only its partner. */
    @Override
    public int escorts() {
        return 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ENRAGED, false);
    }

    public boolean enraged() {
        return entityData.get(ENRAGED);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** The White's tongue reaches further. */
    @Override
    protected net.minecraft.world.phys.AABB getAttackBoundingBox() {
        return white() ? super.getAttackBoundingBox().inflate(1.2, 0, 1.2) : super.getAttackBoundingBox();
    }

    // ── The pair ────────────────────────────────────────────────────────

    /** Its partner, if it lives and is in this world. */
    @Nullable
    public ImpermanenceEntity partner() {
        if (partner == null || !(level() instanceof ServerLevel server)) return null;
        return server.getEntity(partner) instanceof ImpermanenceEntity p && p.isAlive() ? p : null;
    }

    /** Makes these two a pair. */
    public static void pair(ImpermanenceEntity a, ImpermanenceEntity b) {
        a.partner = b.getUUID();
        b.partner = a.getUUID();
    }

    /** Whether its partner is alive and near it. */
    public boolean together() {
        ImpermanenceEntity p = partner();
        return p != null && p.distanceTo(this) <= TOGETHER;
    }

    /** Met in the world, it comes with its partner (one of the other colour, beside it). */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        boolean bring = reason == MobSpawnType.NATURAL || reason == MobSpawnType.CHUNK_GENERATION || reason == MobSpawnType.EVENT
                || reason == MobSpawnType.SPAWN_EGG || reason == MobSpawnType.COMMAND;
        if (bring && level instanceof ServerLevel server) bringPartner(server);
        return result;
    }

    /** Brings its partner to its side. Returns it, or null. */
    @Nullable
    public ImpermanenceEntity bringPartner(ServerLevel server) {
        EntityType<ImpermanenceEntity> other = white() ? ModEntities.BLACK_IMPERMANENCE.get() : ModEntities.WHITE_IMPERMANENCE.get();
        ImpermanenceEntity p = ZoneMonster.summon(server, other, blockPosition().offset(1, 0, 1), false);
        if (p == null) return null;
        pair(this, p);
        if (getTarget() != null) p.setTarget(getTarget());
        return p;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 20 != 0) return;
        if (partner == null) {
            // find a partner of the other colour near, unpaired (as the fortress places them)
            for (ImpermanenceEntity o : level().getEntitiesOfClass(ImpermanenceEntity.class, getBoundingBox().inflate(TOGETHER),
                    o -> o != this && o.white() != white() && o.partner == null && o.isAlive())) {
                pair(this, o);
                break;
            }
        }
    }

    /** Its partner fallen, it is enraged: stronger and quicker, for good. */
    public void enrage() {
        if (enraged()) return;
        entityData.set(ENRAGED, true);
        addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, -1, 1, false, true));
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, -1, 1, false, true));
        playSound(SoundEvents.VEX_CHARGE, 1.5f, white() ? 1.2f : 0.7f);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SOUL, getX(), getY(1), getZ(), 25, 0.4, 0.6, 0.4, 0.05);
        }
    }

    /** Its partner dead beside it: enraged at once. */
    public void partnerDied() {
        enrage();
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!level().isClientSide && dead) {
            ImpermanenceEntity p = partner();
            if (p != null) p.partnerDied();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && together() && !source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            amount *= (float) GUARDED;
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        float before = target instanceof LivingEntity l ? l.getHealth() : 0;
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity l) strike(l, before - l.getHealth());
        return hit;
    }

    /** What its blow does besides the blow: the Black shackles; the White drinks half of what he took. */
    public void strike(LivingEntity target, float taken) {
        if (white()) {
            if (taken > 0) heal((float) (taken * DRINK));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0), this);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHACKLED, SHACKLE_LEVEL), this);
            playSound(SoundEvents.CHAIN_HIT, 1.2f, 0.8f);
        }
    }

    @Override
    public float getVoicePitch() {
        return white() ? 1.2f : 0.7f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VEX_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VEX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VEX_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (partner != null) tag.putUUID("Partner", partner);
        tag.putBoolean("Enraged", enraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Partner")) partner = tag.getUUID("Partner");
        entityData.set(ENRAGED, tag.getBoolean("Enraged"));
    }
}
