package com.jttw.engine.entity.wild;

import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.zone.ZoneBoss;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;

/**
 * A raided village's chief (夜叉王, the Yaksha King): an ogre made a gym's leader, bigger, blue-black
 * where his kind are red, gilded where they are iron. He holds the village his band took; while he
 * lives its raiders come back to their posts, and his fall frees it for good
 * ({@link com.jttw.engine.village.raid.RaidedVillages}).
 *
 * <p>He stands at his village's band's top and {@value ZoneBoss#LEVELS_ABOVE_BAND} more, and is built
 * as a zone's boss is, for one character of the band's top: {@value #HITS} of their ordinary hits to
 * kill and a club whose blow is {@value #BLOW} of their health (six kill). His <b>war-stomp</b> is his
 * blow on all: he heaves the club up over his head while a ring of fire marks the ground round him
 * ({@value #STOMP_WINDUP} ticks), then brings it down: everything within {@value #STOMP_REACH} blocks
 * takes {@value #STOMP_SHARE} of that character's health (seven kill) and is flung away. At half his
 * health he roars, and every raider of his village within {@value #RALLY_REACH} blocks comes for his foe.
 */
public class RaidChiefEntity extends OgreEntity {

    /** His hits to kill, of a character of his band's top; his club's blow, and the stomp's, of their health. */
    public static final double HITS = 68, BLOW = 0.19, STOMP_SHARE = 0.16;
    public static final double STOMP_REACH = 6.0;
    public static final int STOMP_WINDUP = 30, STOMP_EVERY = 180, RALLY_REACH = 48;
    /** How much larger than an ogre he is drawn. */
    public static final float SIZE = 1.2f;
    /** His name's colour: a boss's gold. */
    public static final int GOLD = 0xF2B33D;

    private final ServerBossEvent bossBar = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
    private boolean rallied;

    public RaidChiefEntity(EntityType<? extends RaidChiefEntity> type, Level level) {
        super(type, level);
        xpReward = 80;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return OgreEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, HITS * MobScaling.typicalHit(minLevelOf()))
                .add(Attributes.ATTACK_DAMAGE, BLOW * MobScaling.typicalHealth(minLevelOf()))
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32);
    }

    private static int minLevelOf() {
        return 1 + ZoneBoss.LEVELS_ABOVE_BAND;
    }

    @Override public int minLevel() { return minLevelOf(); }
    @Override public int maxLevel() { return 100 + ZoneBoss.LEVELS_ABOVE_BAND; }
    @Override protected double hitsToKill() { return HITS; }
    @Override protected double blowShare() { return BLOW; }
    @Override public double hardestShare() { return BLOW; }
    @Override public int vanillaXpShare() { return 20; }

    /** The level of character he is built for: his band's top (he stands three over it), the players' cap past it. */
    public int challenger() {
        return ZoneBoss.measuredAt(mobLevel() - ZoneBoss.LEVELS_ABOVE_BAND);
    }

    /** At {@code level} (his band's top and three), measured against his challenger, as a gym's leader. */
    @Override
    public void become(int level, Rank rank) {
        super.become(level, Rank.REGULAR);
        int c = challenger();
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(HITS * MobScaling.typicalHit(c));
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BLOW * MobScaling.typicalHealth(c));
        setHealth(getMaxHealth());
    }

    @Override
    public int rollLevel(RandomSource r) {
        return mobLevel();
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData out = super.finalizeSpawn(level, difficulty, reason, data);
        setPersistenceRequired();
        return out;
    }

    // ── The war-stomp ───────────────────────────────────────────────────

    @Override public double slamReach() { return STOMP_REACH; }
    @Override public int slamWindupTicks() { return STOMP_WINDUP; }
    @Override public int slamEvery() { return STOMP_EVERY; }

    /** The stomp's hurt: {@value #STOMP_SHARE} of his challenger's health. */
    @Override
    public float slamDamage() {
        return (float) (STOMP_SHARE * MobScaling.typicalHealth(challenger()));
    }

    /** The ring of fire marking the ground the stomp will strike, as he heaves his club up. */
    @Override
    protected void slamWindingUp(int left) {
        if (!(level() instanceof ServerLevel server)) return;
        if (left == STOMP_WINDUP - 1) playSound(SoundEvents.RAVAGER_ROAR, 2.0f, 0.5f);
        if (left % 3 != 0) return;
        int n = 36;
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n;
            server.sendParticles(ParticleTypes.FLAME, getX() + STOMP_REACH * Math.cos(a), getY() + 0.15, getZ() + STOMP_REACH * Math.sin(a), 1, 0, 0.02, 0, 0);
        }
        server.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 6, STOMP_REACH * 0.4, 0.05, STOMP_REACH * 0.4, 0.01);
    }

    // ── His band ────────────────────────────────────────────────────────

    /** At half his health he roars: every raider of his village near comes for his foe. Returns how many came. */
    public int rally() {
        rallied = true;
        LivingEntity foe = getTarget();
        playSound(SoundEvents.RAVAGER_ROAR, 3.0f, 0.4f);
        if (foe == null) return 0;
        long village = com.jttw.engine.village.raid.RaidedVillages.villageOf(this);
        int n = 0;
        for (Mob m : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(RALLY_REACH),
                m -> m != this && m.isAlive() && com.jttw.engine.village.raid.RaidedVillages.villageOf(m) == village)) {
            m.setTarget(foe);
            n++;
        }
        return n;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (!rallied && getHealth() <= getMaxHealth() / 2) rally();
        if (tickCount % 10 == 0) updateBossBar();
        else bossBar.setProgress(getHealth() / getMaxHealth());
    }

    /** His bar shows to the players within 32 blocks. */
    public void updateBossBar() {
        if (!(level() instanceof ServerLevel server) || !isAlive()) {
            bossBar.removeAllPlayers();
            return;
        }
        bossBar.setName(getDisplayName());
        bossBar.setProgress(getHealth() / getMaxHealth());
        for (ServerPlayer p : server.players()) {
            if (p.isAlive() && !p.isSpectator() && p.distanceToSqr(this) <= 32 * 32) bossBar.addPlayer(p);
            else bossBar.removePlayer(p);
        }
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        bossBar.removeAllPlayers();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.removeAllPlayers();
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossBar.removePlayer(player);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public float getScale() {
        return 1f;
    }

    /** "Yaksha King · Lv 43", in a boss's gold. */
    @Override
    protected Component getTypeName() {
        return Component.empty().append(getType().getDescription()).append(" · Lv " + mobLevel())
                .withStyle(s -> s.withColor(TextColor.fromRgb(GOLD)));
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Rallied", rallied);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        rallied = tag.getBoolean("Rallied");
        if (hasCustomName()) bossBar.setName(getDisplayName());
    }
}
