package com.jttw.engine.entity.zone;

import com.jttw.engine.entity.MobScaling;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * A zone's boss: one to each region, put in its lair when the land is made
 * ({@link com.jttw.engine.world.feature.ZoneLairFeature}), and once killed it does not come back:
 * its seat is taken instead, a while after, by a <b>usurper</b>, a boss of another kind, weaker
 * ({@link #becomeUsurper}, {@link com.jttw.engine.world.Usurpers}). So that is never by accident, it
 * is never despawned, not even when the world is set to Peaceful.
 * <ul>
 *   <li>its kill pays as a {@link EnemyTier#BOSS} of its {@link #zone()};</li>
 *   <li>a <b>boss bar</b> shows its health to the players near it ({@link #bossBarRange()}) and to
 *       those who have struck it lately, wherever they are; gone when it dies;</li>
 *   <li>it keeps to its lair: where it was first put, within {@link #lairRadius()} (its walking goals
 *       should include a {@code MoveTowardsRestrictionGoal} to go back);</li>
 *   <li>its fight may come in <b>phases</b>: {@link #phaseThresholds()} and {@link #onPhase(int)};</li>
 *   <li>like the zone's monsters, it spares its zone's allies ({@link Zones.Zone#allies()}), and
 *       leaves them nothing if they strike it down.</li>
 * </ul>
 *
 * <p><b>A boss is its zone's gym leader.</b> It stands {@link #LEVELS_ABOVE_BAND} levels over the top
 * of its zone's band ({@link #levelOver}), the highest thing of its zone, and it is built for one
 * challenger: a character of the band's top level ({@link #challengerOf}), who can beat it alone, in a
 * hard fight. All its lives together it takes {@link #LEAST_HITS} to {@link #MOST_HITS} of that
 * character's ordinary hits to kill, and its main blow takes {@link #LEAST_BLOW} to {@link #MOST_BLOW}
 * of that character's health, so five or six kill; its special hits ({@link #specialShares()}, each
 * struck with {@link #hitOf}) are no harder. All its lives together are more than any miniboss's
 * health (see the README's pecking order). A group finds it easy, and that is as it should be.
 *
 * <p><b>A usurper</b> ({@link #becomeUsurper}) is a boss of one kind sat in another's lair once its
 * master has fallen: of that lair's zone (its kill's XP, its allies), at its band's top
 * ({@link #USURPER_LEVELS_ABOVE_BAND} over it) where a master stands {@link #LEVELS_ABOVE_BAND} over it, {@link #USURPER_HITS} of its kind's
 * hits (never under {@link #USURPER_LEAST_HITS}), and every blow and special of its kit, struck as its
 * kind strikes them, scaled to its new level ({@link #strikes}); half a boss's XP, half its karma, and
 * a lesser loot ({@link #USURPER_LOOT}), none of its kind's own treasures. What its kind is, its level
 * ({@link #nativeLevel()}), its zone ({@link #zone()}) and its hits ({@link #hitOf}), stays its kind's.
 */
public abstract class ZoneBoss extends Monster implements MobLevel {

    /**
     * How far over its zone's level band a boss stands: a zone's head boss is of the band's top level
     * and this many more ({@link #levelOver}), the highest thing of its zone, as a gym's leader stands
     * over its trainers (its captains and minibosses are at most
     * {@link ZoneMonster#CAPTAIN_ABOVE_BAND} over the band; a lesser boss, {@link #LESSER_ABOVE_BAND}).
     * Past the players' cap of 100 at Lion-Camel Ridge.
     */
    public static final int LEVELS_ABOVE_BAND = 3;
    /** How far over its zone's band a lesser boss stands, under its head boss: Hell's Ten Kings under King Yama, the White Elephant under the Azure Lion. */
    public static final int LESSER_ABOVE_BAND = 2;

    /** A boss's level in {@code zone}: the top of its band and {@link #LEVELS_ABOVE_BAND} more. */
    public static int levelOver(Zones.Zone zone) {
        return zone.maxLevel() + LEVELS_ABOVE_BAND;
    }

    /** A lesser boss's level in {@code zone}: the top of its band and {@link #LESSER_ABOVE_BAND} more, one under its head boss. */
    public static int lesserLevelOver(Zones.Zone zone) {
        return zone.maxLevel() + LESSER_ABOVE_BAND;
    }

    /**
     * The character a boss of {@code zone} is built for, its challenger: one of the top of its band (the
     * players' cap, past it). A boss's health is so many of this character's ordinary hits and its blows
     * shares of this character's health, whatever its own level: the gym's leader is a fight for one
     * who has walked the whole road to it. (Measured at its own level, three over, it would be a third
     * again as hard: a level is a tenth more power.)
     */
    public static int challengerOf(Zones.Zone zone) {
        return measuredAt(zone.maxLevel());
    }

    /**
     * The level of character a creature of {@code level} past the bands is measured against: its own up
     * to the players' cap of 100, the cap past it. So no boss past the cap (the kings of Lion-Camel
     * Ridge, at 103; the Six-Eared Macaque, at 120) is out of reach of capped players.
     */
    public static int measuredAt(int level) {
        return Math.min(level, com.jttw.engine.progression.level.LevelConstants.MAX_LEVEL);
    }

    /** The level of character it is measured against now: its kind's ({@link #nativeMeasured()}), or a usurper's, its own new level. */
    public int measuredLevel() {
        return isUsurper() ? measuredAt(mobLevel()) : nativeMeasured();
    }

    /**
     * The level of character its kind is measured against, where the world first puts it: its zone's
     * challenger ({@link #challengerOf}), unless it says otherwise. Its hits ({@link #hitOf}) are of this.
     */
    public int nativeMeasured() {
        return challengerOf(zone());
    }

    /**
     * Its level: its kind's ({@link #nativeLevel()}), or a usurper's, its lair's ({@link #usurperLevel}).
     * The one its name shows, and what weighs it against a player.
     */
    @Override
    public final int mobLevel() {
        int usurping = entityData.get(USURPING);
        return usurping > 0 ? usurping : nativeLevel();
    }

    /** Its kind's level: where the world first puts it, its stats are of. A constant (or its court's, its rank's). */
    public abstract int nativeLevel();

    // ── A usurper ───────────────────────────────────────────────────────

    /** How far over its lair's zone's band a usurper stands: none, at its top (a master stands {@link #LEVELS_ABOVE_BAND} over it). */
    public static final int USURPER_LEVELS_ABOVE_BAND = 0;
    /** A usurper's share of its kind's hits, all its lives together; never fewer than {@link #USURPER_LEAST_HITS}. */
    public static final double USURPER_HITS = 0.6;
    public static final double USURPER_LEAST_HITS = ZoneBoss.LEAST_HITS * USURPER_HITS; // a constant: 36
    /** A usurper's share of a boss's kill XP, and of its kind's karma. */
    public static final double USURPER_XP = 0.5, USURPER_KARMA = 0.5;
    /** What a usurper leaves: some gold, a sigil or two, perhaps a diamond; none of its kind's own treasures. */
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> USURPER_LOOT =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("jttw_engine", "entities/usurper"));
    /** How far past its lair a usurper may be drawn before it is back in it. */
    public static final int USURPER_LEASH = 16;

    /** A usurper's level in a lair of {@code zone}: the top of its band and {@link #USURPER_LEVELS_ABOVE_BAND} more. */
    public static int usurperLevel(Zones.Zone zone) {
        return zone.maxLevel() + USURPER_LEVELS_ABOVE_BAND;
    }

    /** A usurper's level (synced: its name shows it), or 0 for a boss of its own lair. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> USURPING = net.minecraft.network.syncher.SynchedEntityData
            .defineId(ZoneBoss.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    /** Its seat: the lair the world put it in (or, a usurper, the one it took), whose master it is; null for one put anywhere else. */
    @Nullable
    private BlockPos seat;
    /** A usurper's: its lair's zone, how far from its seat it keeps, how much harder (or lighter) than its kind's its blows land. */
    @Nullable
    private Zones.Zone lairZone;
    private int lairReach;
    private double strikeScale = 1;

    /** Whether it is the master of a lair, whose fall a usurper follows: every boss but a lair's lesser immortals (the temple youths). */
    public boolean headsLair() {
        return true;
    }

    /** Whether it is a usurper, sat in a fallen boss's seat. */
    public boolean isUsurper() {
        return entityData.get(USURPING) > 0;
    }

    /** Its seat (see {@link #seat}), or null. */
    @Nullable
    public BlockPos seat() {
        return seat;
    }

    /** Takes the lair at {@code at} for its seat, as the world does putting it there: its fall leaves the seat to a usurper. */
    public void takeSeat(BlockPos at) {
        seat = at.immutable();
    }

    /** The zone of the lair it holds: a usurper's, the lair's; else its own ({@link #zone()}). Its kill pays by it, its allies are its. */
    public Zones.Zone lairZone() {
        return lairZone != null ? lairZone : zone();
    }

    /** How far from its lair it keeps: a usurper, the fallen master's ground; else its own ({@link #lairRadius()}). */
    public int homeReach() {
        return lairZone != null ? lairReach : lairRadius();
    }

    /** Its hits to kill, all its lives together, of a character of the level it is measured at now. */
    public double hitsToKill() {
        return getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).getBaseValue() * livesTogether()
                / MobScaling.typicalHit(measuredLevel());
    }

    private double livesTogether() {
        double all = 0;
        for (double life : lives()) all += life;
        return all;
    }

    /**
     * Makes it a usurper of the lair at {@code seat}, of {@code zone}, keeping within {@code reach} of
     * it: its level the lair's {@link #usurperLevel}, its health {@link #USURPER_HITS} of its kind's hits
     * (never under {@link #USURPER_LEAST_HITS}) of a character of the level it is now measured at, its
     * blows scaled to match ({@link #strikes}), half a boss's XP, its kill paid by the lair's zone.
     */
    public void becomeUsurper(Zones.Zone zone, BlockPos seat, int reach) {
        double kindHits = hitsToKill(); // its kind's, before it is anything else
        entityData.set(USURPING, usurperLevel(zone));
        this.lairZone = zone;
        this.lairReach = reach;
        this.seat = seat.immutable();
        double hits = Math.max(USURPER_HITS * kindHits, USURPER_LEAST_HITS);
        getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                .setBaseValue(hits * MobScaling.typicalHit(measuredLevel()) / livesTogether());
        setHealth(getMaxHealth());
        usurp();
        restrictTo(seat, reach);
    }

    /** What follows from being a usurper, made one or loaded as one: its blows' scale, its XP, its zone's name on it. */
    private void usurp() {
        strikeScale = MobScaling.typicalHealth(measuredLevel()) / MobScaling.typicalHealth(nativeMeasured());
        xpReward = Math.max(1, (int) Math.round(xpReward * USURPER_XP));
        getPersistentData().putString(ZoneMonster.KEY_ZONE, lairZone().id());
        getPersistentData().putDouble(ZoneMonster.KEY_XP_SHARE, USURPER_XP);
    }

    /** How much harder (over 1) or lighter its blows land than its kind's: 1 but for a usurper. */
    public double strikeScale() {
        return isUsurper() ? strikeScale : 1;
    }

    /**
     * A blow of its kit, {@code amount} as its kind strikes it, as it lands from it now: a usurper's
     * scaled to its level ({@link #strikeScale()}) and never past a boss's hardest blow at it
     * ({@link #MOST_BLOW}, so a hit weighed by its target's own health, scaled up, cannot one-shot);
     * else as it is. {@link com.jttw.engine.world.Usurpers} scales every hit a usurper deals by this;
     * a hit that names no striker (frost, scalding oil, its fire round it) is scaled where it is dealt.
     */
    public float strikes(float amount) {
        if (!isUsurper()) return amount;
        return (float) Math.min(amount * strikeScale, MOST_BLOW * MobScaling.typicalHealth(measuredLevel()));
    }

    /** The least and the most ordinary hits of its challenger ({@link #challengerOf}) a boss takes to kill, all its lives together. */
    public static final double LEAST_HITS = 60, MOST_HITS = 80;
    /** A boss's main blow, at least and at most, as a share of its challenger's health: five or six kill. */
    public static final double LEAST_BLOW = 0.18, MOST_BLOW = 0.20;

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

    /** The players it is allied to: its lair's zone's, unless it says otherwise. */
    public Predicate<Player> allies() {
        return lairZone().allies();
    }

    /**
     * Its lives, each as a share of its health it must be brought through: one by default; more for
     * one that rises again (Lady White Bone's four, whole each time; the Wheel's king, born again at half).
     */
    public double[] lives() {
        return new double[]{1};
    }

    /**
     * Its special hits (an area blow, a spin, a shockwave...), each at its hardest as a share of a
     * same-level character's health: none by default. Each no more than {@link #MOST_BLOW} (the
     * pecking order holds it to that).
     */
    public double[] specialShares() {
        return new double[0];
    }

    /**
     * A hit of {@code share} of a same-level character's health, as its kind strikes it, at the level
     * its kind is measured at (a usurper's lands scaled after, as all its blows: {@link #strikes}).
     */
    public float hitOf(double share) {
        return (float) (share * MobScaling.typicalHealth(nativeMeasured()));
    }

    /**
     * Whether it would strike this player with a blow that falls on all near it: by default any it
     * may attack (not its allies); a righteous one only those who have given it cause.
     */
    protected boolean fights(Player p) {
        return true;
    }

    /** Whether {@code p} struck it lately (within {@link #FIGHTER_MEMORY} ticks). */
    public boolean struckLately(Player p) {
        Long at = fighters.get(p.getUUID());
        return at != null && level().getGameTime() - at <= FIGHTER_MEMORY;
    }

    /**
     * Those within {@code r} of it that a blow on all near it falls on ({@link #isFoe}): the players
     * it would fight (not watching or building, not its allies), its own foe, and whatever is fighting it.
     */
    public List<LivingEntity> foesWithin(double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r), e -> e != this && e.isAlive())) {
            if (e.distanceTo(this) <= r && isFoe(e)) out.add(e);
        }
        return out;
    }

    /** Whether a blow on all near it falls on {@code e}: a player it would fight, its own foe, or what is fighting it. */
    public boolean isFoe(LivingEntity e) {
        if (e == this || !e.isAlive()) return false;
        if (e instanceof Player p) return !p.isSpectator() && !p.isCreative() && canAttack(p) && (e == getTarget() || fights(p));
        return e == getTarget() || e instanceof Mob m && m.getTarget() == this;
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
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(USURPING, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (tickCount % BAR_EVERY == 0) updateBossBar();
        else if (!bossBar.getPlayers().isEmpty()) bossBar.setProgress(getHealth() / getMaxHealth());
        checkPhase();
        if (tickCount % 20 == 7) keepToSeat();
    }

    /**
     * A usurper keeps to the lair it took: drawn more than {@value #USURPER_LEASH} blocks past its
     * ground (thrown, carried, chasing), it is back on its seat at once. Returns whether it was.
     * (A lair's own master is kept to it by its lair, as ever.)
     */
    public boolean keepToSeat() {
        if (!isUsurper() || seat == null || isDeadOrDying()) return false;
        double reach = homeReach() + USURPER_LEASH;
        if (distanceToSqr(net.minecraft.world.phys.Vec3.atBottomCenterOf(seat)) <= reach * reach) return false;
        if (isPassenger()) stopRiding();
        getNavigation().stop();
        setTarget(null);
        teleportTo(seat.getX() + 0.5, seat.getY(), seat.getZ() + 0.5);
        setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        fallDistance = 0;
        return true;
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

    /** Dead (truly: not a life lost), it leaves its seat empty, for a usurper to take in time ({@link com.jttw.engine.world.Usurpers}). */
    @Override
    public void die(DamageSource source) {
        boolean was = !dead;
        super.die(source);
        if (dead) bossBar.removeAllPlayers();
        if (was && dead && seat != null && headsLair() && level() instanceof ServerLevel server) {
            com.jttw.engine.world.Usurpers.fallen(server, this);
        }
    }

    /** Struck down by one it is allied to, who never had to fight it, it leaves nothing (as a ZoneMonster). */
    @Override
    protected void dropAllDeathLoot(ServerLevel level, DamageSource source) {
        if (ZoneAllies.allied(allies(), source.getEntity())) return;
        super.dropAllDeathLoot(level, source);
    }

    /** A usurper leaves a usurper's loot ({@link #USURPER_LOOT}), not its kind's treasures. */
    @Override
    protected net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> getDefaultLootTable() {
        return isUsurper() ? USURPER_LOOT : super.getDefaultLootTable();
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

    /** Its lair: where it is first put, and keeps to; put there by the world as the land is made, its seat. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason,
                                        @Nullable SpawnGroupData data) {
        restrictTo(blockPosition(), lairRadius());
        if (reason == MobSpawnType.STRUCTURE && headsLair()) takeSeat(blockPosition());
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
        if (seat != null) tag.put("Seat", NbtUtils.writeBlockPos(seat));
        if (isUsurper() && lairZone != null) {
            CompoundTag u = new CompoundTag();
            u.putString("Zone", lairZone.id());
            u.putInt("Level", entityData.get(USURPING));
            u.putInt("Reach", lairReach);
            tag.put("Usurper", u);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        phase = tag.getInt("BossPhase");
        seat = NbtUtils.readBlockPos(tag, "Seat").orElse(null);
        CompoundTag u = tag.getCompound("Usurper");
        Zones.Zone zone = u.contains("Zone") ? Zones.byId(u.getString("Zone")) : null;
        if (zone != null) {
            lairZone = zone;
            lairReach = u.getInt("Reach");
            entityData.set(USURPING, u.getInt("Level"));
            usurp();
        }
        NbtUtils.readBlockPos(tag, "Lair").ifPresent(lair -> restrictTo(lair, homeReach()));
        if (tag.contains("Lair", Tag.TAG_COMPOUND)) { // as saved before 1.21: {X, Y, Z}
            CompoundTag lair = tag.getCompound("Lair");
            restrictTo(new BlockPos(lair.getInt("X"), lair.getInt("Y"), lair.getInt("Z")), homeReach());
        }
    }
}
