package com.jttw.engine.entity;

import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.s2c.SyncXtqStatsPacket;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.character.erlang.XiaotianQuanManager;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.joml.Vector3f;

import javax.annotation.Nullable;

/**
 * Erlang Shen's celestial hound companion — Xiaotian Quan.
 *
 * <p>Extends PathfinderMob (NOT Monster), so iron golems won't target it.
 * Uses wolf sounds. Goals are registered by {@link XiaotianQuanManager}.
 * Stats are overridden at summon time based on the player's Hound Growth tier.
 *
 * <p>XTQ has its own Qi and Will pools stored in persistent data.
 * Qi is consumed by the Howl ability and regenerates over time.
 *
 * <p>While its owner is in Wrath at Zhàn Shén II it is berserk ({@link #isBerserk}, set by the
 * manager and synced, so its markings burn crimson and it sheds red embers).
 */
public class XiaotianQuanEntity extends PathfinderMob {

    public static final String KEY_QI      = "jttw_xtq_qi";
    public static final String KEY_MAX_QI  = "jttw_xtq_max_qi";
    public static final String KEY_WILL    = "jttw_xtq_will";
    public static final String KEY_HOWL_CD = "jttw_xtq_howl_cd";

    private static final float DEFAULT_QI     = 50f;
    private static final float DEFAULT_WILL   = 10f;
    private static final float QI_REGEN_TICK  = 0.05f; // per tick
    private static final float HOWL_QI_COST   = 15f;
    public  static final int   HOWL_COOLDOWN  = 100; // 5 seconds
    /** The hidden name hounds were once given; dropped on sight. */
    public  static final String LEGACY_NAME_PREFIX = "jttw_xtq:";

    private static final EntityDataAccessor<Boolean> BERSERK =
            SynchedEntityData.defineId(XiaotianQuanEntity.class, EntityDataSerializers.BOOLEAN);

    public final AnimationState walkAnimState = new AnimationState();

    public XiaotianQuanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(BERSERK, false);
    }

    /** Berserk: its owner is in Wrath at Zhàn Shén II or more. */
    public boolean isBerserk() {
        return this.entityData.get(BERSERK);
    }

    public void setBerserk(boolean berserk) {
        this.entityData.set(BERSERK, berserk);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
                walkAnimState.startIfStopped(this.tickCount);
            } else {
                walkAnimState.stop();
            }
            // Black smoke trail
            this.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    this.getRandomX(0.5), this.getY() + 0.2, this.getRandomZ(0.5),
                    0.0, 0.02, 0.0);
            if (isBerserk()) {
                // berserk: red embers off its back and a flash of rage at its brow
                this.level().addParticle(new DustParticleOptions(new Vector3f(0.9f, 0.1f, 0.05f), 1.0f),
                        this.getRandomX(0.6), this.getY() + this.getBbHeight() * (0.4 + 0.6 * this.random.nextDouble()),
                        this.getRandomZ(0.6), 0.0, 0.0, 0.0);
                if (this.tickCount % 20 == 0) {
                    this.level().addParticle(ParticleTypes.ANGRY_VILLAGER,
                            this.getX(), this.getEyeY() + 0.4, this.getZ(), 0.0, 0.0, 0.0);
                }
            }
            return;
        }

        // Hounds once carried their owner in a hidden name ("jttw_xtq:<uuid>"), which death
        // messages showed; the owner is in the persistent data, so drop any such name
        if (this.getCustomName() != null && this.getCustomName().getString().startsWith(LEGACY_NAME_PREFIX)) {
            this.setCustomName(null);
        }

        // Server-side: regen Qi, tick howl cooldown
        CompoundTag pd = this.getPersistentData();
        float qi = pd.getFloat(KEY_QI);
        float maxQi = pd.getFloat(KEY_MAX_QI);
        if (maxQi <= 0) maxQi = DEFAULT_QI;
        if (qi < maxQi) {
            qi = Math.min(maxQi, qi + QI_REGEN_TICK);
            pd.putFloat(KEY_QI, qi);
        }

        int cd = pd.getInt(KEY_HOWL_CD);
        if (cd > 0) pd.putInt(KEY_HOWL_CD, cd - 1);
    }

    @Override
    protected void registerGoals() {
        XiaotianQuanManager.registerCompanionGoals(this);
    }

    // ── Right-click: open stat sheet for the owner ────────────────────
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        // Only the owner can view stats
        CompoundTag pd = this.getPersistentData();
        if (!pd.hasUUID(XiaotianQuanManager.XTQ_OWNER_TAG)) return InteractionResult.PASS;
        if (!pd.getUUID(XiaotianQuanManager.XTQ_OWNER_TAG).equals(sp.getUUID())) return InteractionResult.PASS;

        int growthTier = SkillTreeData.getXtqGrowthTier(sp);
        float qi = pd.getFloat(KEY_QI);
        float maxQi = pd.getFloat(KEY_MAX_QI);
        if (maxQi <= 0) maxQi = DEFAULT_QI;
        float will = pd.getFloat(KEY_WILL);
        if (will <= 0) will = DEFAULT_WILL;

        var atk = this.getAttribute(Attributes.ATTACK_DAMAGE);
        float str = atk != null ? (float) atk.getValue() : 4.0f;

        NetworkHandler.sendToPlayer(sp, new SyncXtqStatsPacket(
                this.getHealth(), this.getMaxHealth(), str,
                qi, maxQi, will,
                growthTier,
                XiaotianQuanManager.biteSlowness(sp) != null,
                true,                // the hound howls from the first (only Xiaotian Quan III hastens it)
                XiaotianQuanManager.trackCount(SkillTreeData.getXtqTier(sp), growthTier) > 0
        ));
        return InteractionResult.SUCCESS;
    }

    // ── Qi helpers (used by howl AI goal) ──────────────────────────────
    public boolean canHowl() {
        CompoundTag pd = this.getPersistentData();
        return pd.getInt(KEY_HOWL_CD) <= 0 && pd.getFloat(KEY_QI) >= HOWL_QI_COST;
    }

    /** Spends a howl's Qi; it can howl again in {@code recharge} ticks. */
    public void spendHowlQi(int recharge) {
        CompoundTag pd = this.getPersistentData();
        float qi = pd.getFloat(KEY_QI);
        pd.putFloat(KEY_QI, Math.max(0, qi - HOWL_QI_COST));
        pd.putInt(KEY_HOWL_CD, recharge);
    }

    // ── Init stats (called by manager at summon time) ─────────────────
    public void initXtqStats(int growthTier) {
        CompoundTag pd = this.getPersistentData();
        float maxQi = DEFAULT_QI + growthTier * 15f;
        pd.putFloat(KEY_MAX_QI, maxQi);
        pd.putFloat(KEY_QI, maxQi);
        pd.putFloat(KEY_WILL, DEFAULT_WILL + growthTier * 5f);
        pd.putInt(KEY_HOWL_CD, 0);
    }

    /**
     * Base attributes (Tier 0 defaults, at level 1). The manager overrides these at summon
     * time based on the owner's Hound Growth skill tree tier, with health and attack
     * scaled by the owner's level power.
     */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.MOVEMENT_SPEED, 0.35)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // Wolf sounds for the celestial hound
    @Nullable @Override protected SoundEvent getAmbientSound() { return SoundEvents.WOLF_AMBIENT; }
    @Nullable @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.WOLF_HURT; }
    @Nullable @Override protected SoundEvent getDeathSound() { return SoundEvents.WOLF_DEATH; }

    @Override protected boolean shouldDropLoot() { return false; }
    @Override public boolean removeWhenFarAway(double dist) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
}
