package com.jttw.engine.entity.wuzhuang;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.world.wuzhuang.Wuzhuang;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * One of the Great Immortal Zhenyuan's disciples (the novel gives him forty-eight), in a blue Daoist
 * robe, his hair in a topknot, a sword at hand. Righteous: he fights no one unprovoked, except the
 * temple's enemies (a thief of its fruit, and, for good, whoever has eaten one:
 * {@link Wuzhuang#isTempleEnemy}); strike one and those near him come to his side. Kept at a
 * distance he throws a <b>binding talisman</b> (定身符): its mark cannot move for a moment, and is
 * weakened. A temple has only so many: they are put there as the land is made, never spawn of
 * themselves, and are kept even on Peaceful ({@link com.jttw.engine.world.wuzhuang.TempleRecord}).
 * Killing one is a bad deed (karma {@link com.jttw.engine.world.morality.Morality#KILL_DISCIPLE}).
 */
public class DaoistDiscipleEntity extends WuzhuangMonster implements TempleFolk {

    /** Six of a same-level character's ordinary hits; a blow of 13% of their health. */
    public static final double HITS = 6, BLOW = 0.13;
    public static final int LEVEL = 78;
    /** The talisman: how far it is thrown, how long its mark is bound, how often. */
    public static final double TALISMAN_FROM = 3, TALISMAN_TO = 10;
    public static final int BOUND = 30, TALISMAN_EVERY = 160;

    private int talisman = TALISMAN_EVERY / 2;

    public DaoistDiscipleEntity(EntityType<? extends DaoistDiscipleEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(LEVEL))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(LEVEL))
                .add(Attributes.MOVEMENT_SPEED, 0.29)
                .add(Attributes.ARMOR, 4)
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

    @Override
    public int escorts() {
        return 2;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p instanceof Player pl && Wuzhuang.isTempleEnemy(pl)));
    }

    /** None comes into the world of itself: a temple's disciples are those put there as it was made. */
    @Override
    public boolean checkSpawnRules(LevelAccessor level, MobSpawnType reason) {
        return reason != MobSpawnType.NATURAL && reason != MobSpawnType.CHUNK_GENERATION && super.checkSpawnRules(level, reason);
    }

    /** Kept, even on Peaceful: one lost is gone for good. */
    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setDropChance(EquipmentSlot.MAINHAND, 0f);
        return result;
    }

    /** Throws a binding talisman at {@code target}: it cannot move for a moment, and is weakened. Returns whether it struck. */
    public boolean bind(LivingEntity target) {
        if (!target.isAlive()) return false;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BOUND, 6), this);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, BOUND * 3, 0), this);
        swing(InteractionHand.OFF_HAND);
        if (level() instanceof ServerLevel server) {
            Vec3 from = getEyePosition(), to = target.position().add(0, target.getBbHeight() / 2, 0);
            for (int i = 0; i <= 12; i++) {
                Vec3 p = from.lerp(to, i / 12.0);
                server.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0);
            }
            server.sendParticles(ParticleTypes.WAX_ON, to.x, to.y, to.z, 12, 0.3, 0.5, 0.3, 0.05);
        }
        playSound(SoundEvents.ILLUSIONER_CAST_SPELL, 1f, 1.3f);
        return true;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (talisman > 0) talisman--;
        LivingEntity t = getTarget();
        if (talisman == 0 && t != null && t.isAlive() && hasLineOfSight(t)) {
            double d = distanceTo(t);
            if (d >= TALISMAN_FROM && d <= TALISMAN_TO) {
                bind(t);
                talisman = TALISMAN_EVERY;
            }
        }
    }

    /** Spoken to, a disciple bows: his master is away. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && getTarget() == null) {
            player.displayClientMessage(Component.translatable("entity.jttw_engine.daoist_disciple.greet"), true);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
