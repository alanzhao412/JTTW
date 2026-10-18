package com.jttw.engine.entity.zone;

import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.world.ZoneAllies;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A zone's boss: one to each region, put in its lair when the land is made
 * ({@link com.jttw.engine.world.feature.ZoneLairFeature}), and once killed it does not come back.
 * So that is never by accident, it is never despawned, not even when the world is set to Peaceful.
 * <ul>
 *   <li>its kill pays as a {@link EnemyTier#BOSS} of its {@link #zone()};</li>
 *   <li>a <b>boss bar</b> shows its health to the players near it ({@link #bossBarRange()}) and to
 *       those who have struck it lately, wherever they are; gone when it dies;</li>
 *   <li>it keeps to its lair: where it was first put, within {@link #lairRadius()} (its walking goals
 *       should include a {@code MoveTowardsRestrictionGoal} to go back);</li>
 *   <li>its fight may come in <b>phases</b>: {@link #phaseThresholds()} and {@link #onPhase(int)};</li>
 *   <li>like the zone's monsters, it spares its zone's allies ({@link Zones.Zone#allies()}).</li>
 * </ul>
 */
public abstract class ZoneBoss extends Monster {

    /** How long a player who struck it keeps seeing its bar, wherever they are, in ticks. */
    public static final int FIGHTER_MEMORY = 600;
    /** How often the bar's viewers are checked, in ticks. */
    public static final int BAR_EVERY = 10;

    private final ServerBossEvent bossBar;
    /** The players who struck it, and when. */
    private final Map<UUID, Long> fighters = new HashMap<>();
    /** The phase it is in: 0 at full health, then one more for each threshold its health falls past. */
    private int phase;

    protected ZoneBoss(EntityType<? extends ZoneBoss> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        getPersistentData().putString(ZoneMonster.KEY_TIER, EnemyTier.BOSS.name());
        getPersistentData().putString(ZoneMonster.KEY_ZONE, zone().id());
        bossBar = new ServerBossEvent(getDisplayName(), bossBarColor(), BossEvent.BossBarOverlay.PROGRESS);
    }

    // ── What a subclass says ────────────────────────────────────────────

    /** Its zone: whose levels its kill counts toward. A constant. */
    public abstract Zones.Zone zone();

    /** How far from where it was first put it keeps. */
    public abstract int lairRadius();

    /** Its bar's colour. */
    protected BossEvent.BossBarColor bossBarColor() {
        return BossEvent.BossBarColor.RED;
    }

    /** How near a player must be to see its bar. */
    public double bossBarRange() {
        return 32;
    }

    /** Whether its bar shows now (not, say, while it passes for something else). */
    public boolean showsBossBar() {
        return true;
    }

    /** Health shares at which a new phase begins, highest first (e.g. 0.66, 0.33); none by default. */
    protected float[] phaseThresholds() {
        return new float[0];
    }

    /** Called once as each phase begins: 1 when its health first falls past the first threshold, and on. */
    protected void onPhase(int phase) {}

    /** The players it is allied to: its zone's, unless it says otherwise. */
    public Predicate<Player> allies() {
        return zone().allies();
    }

    // ── The boss bar ────────────────────────────────────────────────────

    public ServerBossEvent bossBar() {
        return bossBar;
    }

    /** The players its bar is shown to, as of the last check. */
    public Collection<ServerPlayer> bossBarViewers() {
        return bossBar.getPlayers();
    }

    /** Checks who sees its bar among the world's players. */
    public void updateBossBar() {
        if (level() instanceof ServerLevel server) updateBossBar(server.players());
    }

    /** Checks who sees its bar among these players: near it, or who struck it lately; none once it is dead. */
    public void updateBossBar(Collection<? extends ServerPlayer> players) {
        if (dead || !isAlive() || !showsBossBar()) {
            bossBar.removeAllPlayers();
            return;
        }
        bossBar.setName(getDisplayName());
        bossBar.setProgress(getHealth() / getMaxHealth());
        long now = level().getGameTime();
        fighters.values().removeIf(at -> now - at > FIGHTER_MEMORY);
        for (ServerPlayer p : List.copyOf(bossBar.getPlayers())) {
            if (!players.contains(p) && !fighters.containsKey(p.getUUID())) bossBar.removePlayer(p);
        }
        double range = bossBarRange();
        for (ServerPlayer p : players) {
            boolean sees = p.isAlive() && !p.isSpectator() && p.level() == level()
                    && (p.distanceToSqr(this) <= range * range || fighters.containsKey(p.getUUID()));
            if (sees) bossBar.addPlayer(p);
            else bossBar.removePlayer(p);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount % BAR_EVERY == 0) updateBossBar();
        else if (!bossBar.getPlayers().isEmpty()) bossBar.setProgress(getHealth() / getMaxHealth());
        checkPhase();
    }

    /** Moves it on to the phase its health has fallen to, calling {@link #onPhase} for each one begun. */
    protected void checkPhase() {
        float[] at = phaseThresholds();
        float share = getHealth() / getMaxHealth();
        while (phase < at.length && share <= at[phase]) onPhase(++phase);
    }

    /** The phase it is in: 0 at the start. */
    public int phase() {
        return phase;
    }

    /** Back to the first phase (as when it rises whole again). */
    protected void resetPhase() {
        phase = 0;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide && source.getEntity() instanceof ServerPlayer p) {
            fighters.put(p.getUUID(), level().getGameTime());
            if (showsBossBar() && isAlive()) bossBar.addPlayer(p);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (dead) bossBar.removeAllPlayers();
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

    // ── A boss ──────────────────────────────────────────────────────────

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /** Its lair: where it is first put, and keeps to. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        restrictTo(blockPosition(), lairRadius());
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    @Override
    public boolean isAlliedTo(Entity other) {
        return super.isAlliedTo(other) || ZoneAllies.allied(allies(), other);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !ZoneAllies.allied(allies(), target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && ZoneAllies.allied(allies(), target)) return;
        super.setTarget(target);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("BossPhase", phase);
        if (hasRestriction()) tag.put("Lair", NbtUtils.writeBlockPos(getRestrictCenter()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = tag.getInt("BossPhase");
        NbtUtils.readBlockPos(tag, "Lair").ifPresent(lair -> restrictTo(lair, lairRadius()));
        if (tag.contains("Lair", Tag.TAG_COMPOUND)) { // as saved before 1.21: {X, Y, Z}
            CompoundTag lair = tag.getCompound("Lair");
            restrictTo(new BlockPos(lair.getInt("X"), lair.getInt("Y"), lair.getInt("Z")), lairRadius());
        }
    }
}
