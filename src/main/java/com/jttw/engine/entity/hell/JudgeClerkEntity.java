package com.jttw.engine.entity.hell;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Judge-Clerk (判官), one of the underworld's scribes, in a black official's cap and a red robe,
 * a brush in one hand and his ledger in the other. He fights with the brush, feebly; what makes
 * him dangerous is his paperwork:
 * <ul>
 *   <li><b>the writ</b>: every few seconds he writes his quarry's name in his ledger. They are
 *       marked (they glow, for all to see), and every one of Hell's creatures near them that has
 *       nothing better to do comes for them;</li>
 *   <li><b>the summons</b>: every second writ also calls a guard, Ox-Head or Horse-Face by turns, to
 *       his side (no more than {@value #MAX_SUMMONED} of his at once).</li>
 * </ul>
 */
public class JudgeClerkEntity extends HellMonster {

    /** Four of a same-level character's ordinary hits to kill; a brush stroke of 9% of their health. */
    public static final double HITS = 4, BLOW = 0.09;
    public static final int LEVEL = 58;
    /** His writ: how often, how far he can see to write it, how long the mark lasts, how far it is heard. */
    public static final int WRIT_EVERY = 100, MARKED = 200;
    public static final double WRIT_RANGE = 16, HEARD = 24;
    /** How many of his summoned guards may stand at once. */
    public static final int MAX_SUMMONED = 2;
    /** Persistent-data key: until when (game time) someone's name is in a clerk's ledger. */
    public static final String KEY_MARKED = "jttw_hell_writ";

    private static final EntityDataAccessor<Integer> WRITING = SynchedEntityData.defineId(JudgeClerkEntity.class, EntityDataSerializers.INT);

    private int writ = WRIT_EVERY / 2, writs;
    private final List<UUID> summoned = new ArrayList<>();

    public JudgeClerkEntity(EntityType<? extends JudgeClerkEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.24)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    @Override
    public int regularLevel() {
        return LEVEL;
    }

    @Override
    protected double hitsToKill() {
        return HITS;
    }

    @Override
    protected double blowShare() {
        return BLOW;
    }

    /** A senior clerk brings one junior. */
    @Override
    public int escorts() {
        return 1;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(WRITING, 0);
    }

    /** Whether he is writing in his ledger (for his pose). */
    public boolean isWriting() {
        return entityData.get(WRITING) > 0;
    }

    /** Raises his brush over the ledger for a moment (a writ does; so does the gallery, to show it). */
    public void showWriting() {
        entityData.set(WRITING, 20);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 0.9, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        int w = entityData.get(WRITING);
        if (w > 0) entityData.set(WRITING, w - 1);
        if (writ > 0) writ--;
        LivingEntity t = getTarget();
        if (writ == 0 && t != null && t.isAlive() && distanceTo(t) <= WRIT_RANGE && hasLineOfSight(t)) {
            write(t);
            writ = WRIT_EVERY;
        }
    }

    /** Whether {@code e}'s name is in a clerk's ledger now. */
    public static boolean isMarked(LivingEntity e) {
        return e.getPersistentData().getLong(KEY_MARKED) > e.level().getGameTime();
    }

    /**
     * Writes {@code target}'s name in his ledger: it is marked, Hell's creatures near it with no
     * quarry turn on it, and every second writ calls a guard. Returns how many creatures were roused.
     */
    public int write(LivingEntity target) {
        entityData.set(WRITING, 20);
        writs++;
        target.getPersistentData().putLong(KEY_MARKED, level().getGameTime() + MARKED);
        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARKED, 0, false, false), this);
        playSound(SoundEvents.BOOK_PAGE_TURN, 1.5f, 0.7f);
        if (target instanceof ServerPlayer p) p.displayClientMessage(Component.translatable("entity.jttw_engine.judge_clerk.writ"), true);
        int roused = 0;
        for (HellMonster m : level().getEntitiesOfClass(HellMonster.class, target.getBoundingBox().inflate(HEARD),
                m -> m != this && m.isAlive() && (m.getTarget() == null || !m.getTarget().isAlive()))) {
            m.setTarget(target);
            if (m.getTarget() == target) roused++;
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(1.1), target.getZ(), 12, 0.4, 0.4, 0.4, 0.02);
            if (writs % 2 == 0) summonGuard(server);
        }
        return roused;
    }

    /** Calls a guard to his side, Ox-Head and Horse-Face by turns, if fewer than {@value #MAX_SUMMONED} of his stand. Returns it, or null. */
    public HellMonster summonGuard(ServerLevel server) {
        summoned.removeIf(id -> !(server.getEntity(id) instanceof LivingEntity e) || !e.isAlive());
        if (summoned.size() >= MAX_SUMMONED) return null;
        EntityType<? extends HellMonster> type = (writs / 2) % 2 == 1 ? ModEntities.OX_HEAD.get() : ModEntities.HORSE_FACE.get();
        double a = random.nextDouble() * Math.PI * 2;
        HellMonster guard = ZoneMonster.summon(server, type, BlockPos.containing(getX() + Math.cos(a) * 2, getY(), getZ() + Math.sin(a) * 2), false);
        if (guard == null) return null;
        summoned.add(guard.getUUID());
        if (getTarget() != null) guard.setTarget(getTarget());
        server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, guard.getX(), guard.getY(0.5), guard.getZ(), 20, 0.3, 0.8, 0.3, 0.02);
        playSound(SoundEvents.EVOKER_PREPARE_SUMMON, 1f, 0.8f);
        return guard;
    }

    /** How many of his summoned guards stand now. */
    public int summonedStanding(ServerLevel server) {
        summoned.removeIf(id -> !(server.getEntity(id) instanceof LivingEntity e) || !e.isAlive());
        return summoned.size();
    }

    @Override
    public float getVoicePitch() {
        return 0.85f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Writ", writ);
        tag.putInt("Writs", writs);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        writ = tag.getInt("Writ");
        writs = tag.getInt("Writs");
    }
}
