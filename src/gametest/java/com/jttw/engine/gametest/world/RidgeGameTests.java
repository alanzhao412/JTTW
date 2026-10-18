package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.ridge.BoneServantEntity;
import com.jttw.engine.entity.ridge.BoneShardEntity;
import com.jttw.engine.entity.ridge.LadyWhiteBoneEntity;
import com.jttw.engine.entity.ridge.RidgeMonster;
import com.jttw.engine.entity.ridge.WraithEntity;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.progression.stats.ResistanceHandler;
import com.jttw.engine.progression.stats.StatBlock;
import net.minecraft.world.damagesource.DamageSource;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.CurseOfTheDead;
import com.jttw.engine.world.Zones;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/** White Bone Ridge's dead: the Bone Servant, the Wraith, captains, Lady White Bone, the curse, and the XP they pay. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class RidgeGameTests {

    /** Equal to within a part in ten thousand: these run into the hundreds, stored as floats. */
    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-4 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    /**
     * Removes, when the test ends, the ridge's dead this test brought: those it holds, and any
     * within a few blocks of them (escorts, summons). Never a sweep of the whole area: tests run
     * side by side, and one ending would take a neighbour's mobs mid-fight.
     */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        onEnd(h, () -> {
            for (Entity e : held) {
                h.getLevel().getEntities((Entity) null, e.getBoundingBox().inflate(6),
                        x -> x instanceof RidgeMonster || x instanceof LadyWhiteBoneEntity || x instanceof BoneShardEntity
                                || x instanceof com.jttw.engine.entity.ridge.LadyHuskEntity).forEach(Entity::discard);
                e.discard();
            }
        });
    }

    private static <T extends RidgeMonster> T ridge(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    private static LadyWhiteBoneEntity lady(GameTestHelper h, BlockPos at) {
        LadyWhiteBoneEntity lady = h.spawn(ModEntities.LADY_WHITE_BONE.get(), at);
        sweepAround(h, lady);
        return lady;
    }

    // ── Captains ─────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aCaptainIsPurpleNamedLargerAndStronger(GameTestHelper h) {
        BoneServantEntity regular = ridge(h, ModEntities.BONE_SERVANT.get(), 20, false, new BlockPos(1, 2, 1));
        BoneServantEntity captain = ridge(h, ModEntities.BONE_SERVANT.get(), 27, true, new BlockPos(4, 2, 4));
        eq(h, RidgeMonster.WHITE, regular.getDisplayName().getStyle().getColor().getValue(), "a regular's name is white");
        eq(h, RidgeMonster.PURPLE, captain.getDisplayName().getStyle().getColor().getValue(), "a captain's name is purple");
        check(h, captain.getDisplayName().getString().contains("Captain") && captain.getDisplayName().getString().contains("Lv 27"),
                "a captain's name says so, and its level: " + captain.getDisplayName().getString());
        check(h, regular.getDisplayName().getString().contains("Lv 20"), "a regular's level: " + regular.getDisplayName().getString());
        // measured against a typical character of its level
        close(h, BoneServantEntity.HITS * MobScaling.typicalHit(20), regular.getMaxHealth(), "a level-20 servant takes eight ordinary hits");
        close(h, BoneServantEntity.HITS * MobScaling.typicalHit(27) * RidgeMonster.CAPTAIN_HEALTH, captain.getMaxHealth(), "a level-27 captain's health");
        close(h, BoneServantEntity.BLOW * MobScaling.typicalHealth(27) * RidgeMonster.CAPTAIN_DAMAGE,
                captain.getAttributeValue(Attributes.ATTACK_DAMAGE), "its blow");
        near(h, regular.getBbHeight() * RidgeMonster.CAPTAIN_SCALE, captain.getBbHeight(), "it stands taller");
        eq(h, EnemyTier.GENERAL.name(), captain.getPersistentData().getString(RidgeMonster.KEY_TIER), "a captain pays as a General");
        eq(h, EnemyTier.GRUNT.name(), regular.getPersistentData().getString(RidgeMonster.KEY_TIER), "a regular as a Grunt");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aCaptainBringsThreeRegularsOfItsKind(GameTestHelper h) {
        WraithEntity captain = ridge(h, ModEntities.WRAITH.get(), 27, true, new BlockPos(2, 3, 2));
        captain.bringEscorts(h.getLevel());
        List<WraithEntity> escorts = h.getLevel().getEntitiesOfClass(WraithEntity.class, captain.getBoundingBox().inflate(6), e -> e != captain);
        eq(h, RidgeMonster.ESCORTS, escorts.size(), "escorts");
        for (WraithEntity e : escorts) {
            check(h, !e.isCaptain(), "escorts are regulars");
            check(h, Math.abs(e.mobLevel() - RidgeMonster.LEVEL) <= 1, "about level 20: " + e.mobLevel());
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyTheWorldsOwnSpawnsCanBeCaptains(GameTestHelper h) {
        for (int i = 0; i < 60; i++) {
            BoneServantEntity m = ModEntities.BONE_SERVANT.get().create(h.getLevel());
            m.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 2, 2.5)));
            m.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            check(h, !m.isCaptain(), "a summoned servant is never a captain");
            check(h, Math.abs(m.mobLevel() - RidgeMonster.LEVEL) <= 1, "level about 20: " + m.mobLevel());
            check(h, m.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND).is(net.minecraft.world.item.Items.BONE), "it swings a bone");
            m.discard(); // never added to the world
        }
        h.succeed();
    }

    // ── The Bone Servant ────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aServantsBlowHitsLikeATruck(GameTestHelper h) {
        BoneServantEntity servant = ridge(h, ModEntities.BONE_SERVANT.get(), 20, false, new BlockPos(1, 2, 1));
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
        float before = golem.getHealth();
        servant.doHurtTarget(golem);
        double blow = servant.blowAt(20, false);
        close(h, blow, before - golem.getHealth(), "one blow, in full on a mob without resistance");
        // what that blow does to a level-20 character of each class, after their physical resistance
        for (ProgressionClass cls : ProgressionClass.values()) {
            StatBlock s = StatBlock.compute(cls, 20);
            double taken = ResistanceHandler.damageAfter(blow, s.physicalResistance());
            check(h, taken >= s.maxHealth() / 5 && taken < s.maxHealth() * 0.6,
                    cls + " at 20 loses a fifth to (for the frailest) over half its health to one blow: " + taken + " of " + s.maxHealth());
        }
        h.succeed();
    }

    // ── Live fights: each runs alone in its own batch and, after it, every one of the ridge's dead is cleared
    // away: what these summon wanders (a Wraith flies off) further than any tight sweep reaches, and would
    // otherwise drift into the tests after them.

    private static final String LIVE_SERVANT = "ridge_live_servant", LIVE_LADY = "ridge_live_lady";

    private static void clearTheDead(net.minecraft.server.level.ServerLevel level) {
        level.getEntities((Entity) null, new AABB(level.getSharedSpawnPos()).inflate(30_000_000),
                e -> e instanceof RidgeMonster || e instanceof LadyWhiteBoneEntity || e instanceof BoneShardEntity).forEach(Entity::discard);
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = LIVE_SERVANT)
    public static void afterTheServant(net.minecraft.server.level.ServerLevel level) {
        clearTheDead(level);
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = LIVE_LADY)
    public static void afterTheLady(net.minecraft.server.level.ServerLevel level) {
        clearTheDead(level);
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = LIVE_SERVANT)
    public static void aServantsBlowLandsOnlyAfterItsClubIsRaised(GameTestHelper h) {
        BoneServantEntity servant = ridge(h, ModEntities.BONE_SERVANT.get(), 20, false, new BlockPos(1, 2, 1));
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
        golem.setNoAi(true);
        float full = golem.getHealth();
        servant.setTarget(golem);
        int[] raisedAt = {-1};
        h.succeedWhen(() -> {
            if (raisedAt[0] < 0 && servant.isRaising()) raisedAt[0] = (int) h.getTick();
            if (raisedAt[0] < 0) check(h, golem.getHealth() == full, "no blow before the club is up");
            check(h, raisedAt[0] >= 0, "it raised its club (alive " + servant.isAlive() + ", target " + servant.getTarget()
                    + ", in reach " + servant.inReach(golem) + ", at " + whereabouts(h, servant) + ")");
            check(h, golem.getHealth() < full, "the blow landed (alive " + servant.isAlive() + ", raising " + servant.isRaising()
                    + ", in reach " + servant.inReach(golem) + ", golem " + whereabouts(h, golem) + ", servant " + whereabouts(h, servant) + ")");
            check(h, h.getTick() - raisedAt[0] >= BoneServantEntity.WINDUP - 1, "and only after the wind-up");
        });
    }

    // ── The Wraith ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aWraithsTouchIsMagicAndChills(GameTestHelper h) {
        WraithEntity wraith = ridge(h, ModEntities.WRAITH.get(), 20, false, new BlockPos(1, 3, 1));
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
        float before = golem.getHealth();
        wraith.doHurtTarget(golem);
        DamageSource took = golem.getLastDamageSource();
        close(h, wraith.blowAt(20, false), before - golem.getHealth(), "its touch");
        check(h, took != null && ResistanceHandler.isMagic(took), "is magic, for magic resistance to take");
        check(h, golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and chilled");
        check(h, wraith.isNoGravity(), "it floats");
        h.succeed();
    }

    // ── XP ──────────────────────────────────────────────────────────────

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(template = PLATFORM)
    public static void theRidgesDeadPayTheRidgesXpWhereverTheyDie(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 20);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.WHITE_BONE_RIDGE.location());
        check(h, band != null && band.minLevel() == 20, "the ridge's band starts at 20");
        // the test platform is not in the ridge: the kill pays by the mob's own zone
        killedBy(p, ridge(h, ModEntities.BONE_SERVANT.get(), 20, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a regular");
        killedBy(p, ridge(h, ModEntities.WRAITH.get(), 27, true, new BlockPos(4, 3, 4)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a captain");
        long xp = PlayerLevel.getXp(p);
        killedBy(p, h.spawn(EntityType.ZOMBIE, new BlockPos(2, 2, 4)));
        eq(h, xp, PlayerLevel.getXp(p), "a vanilla mob pays nothing past 20");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theRidgeStopsPayingWellPastItsBand(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 42);
        long xp = PlayerLevel.getXp(p);
        killedBy(p, ridge(h, ModEntities.BONE_SERVANT.get(), 20, false, new BlockPos(1, 2, 1)));
        eq(h, xp, PlayerLevel.getXp(p), "level 42 is past 36 and its five levels' grace");
        h.succeed();
    }

    // ── Lady White Bone ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void herVeilBlindsThoseNear(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); // the veil passes over creative players
        LadyWhiteBoneEntity lady = lady(h, new BlockPos(4, 2, 4));
        lady.setNoAi(true);
        // a test's player is not in the world's player list, so it is handed to the veil
        ServerPlayer far = player(h);
        far.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        far.moveTo(far.getX() + LadyWhiteBoneEntity.VEIL_RANGE + 6, far.getY(), far.getZ());
        ServerPlayer building = player(h);
        building.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        eq(h, 1, lady.dropVeil(List.of(p, far, building)), "caught");
        check(h, p.hasEffect(MobEffects.BLINDNESS), "blind");
        check(h, !far.hasEffect(MobEffects.BLINDNESS), "beyond its reach, not");
        check(h, !building.hasEffect(MobEffects.BLINDNESS), "a creative player, not");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void sheVanishesToSomewhereRoundHerTarget(GameTestHelper h) {
        LadyWhiteBoneEntity lady = lady(h, new BlockPos(4, 2, 4));
        lady.setNoAi(true);
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(2, 2, 2));
        golem.setNoAi(true);
        check(h, lady.vanish(golem), "she vanished");
        double d = Math.sqrt(lady.distanceToSqr(golem.getX(), lady.getY(), golem.getZ()));
        check(h, d >= 7 && d <= 15, "8 to 14 blocks from her target: " + d);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60, batch = LIVE_LADY)
    public static void herServantsRiseAtAQuarterOfHerHealth(GameTestHelper h) {
        LadyWhiteBoneEntity lady = lady(h, new BlockPos(2, 2, 2));
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(4, 2, 4));
        golem.setNoAi(true);
        golem.setInvulnerable(true);
        lady.setTarget(golem);
        lady.setHealth(lady.getMaxHealth() * 0.7f);
        h.succeedWhen(() -> {
            int retinue = lady.retinue();
            check(h, retinue == 4, "two servants and two wraiths at three quarters: " + retinue);
            for (RidgeMonster m : h.getLevel().getEntitiesOfClass(RidgeMonster.class, lady.getBoundingBox().inflate(48))) {
                check(h, !m.isCaptain(), "her dead are never captains");
            }
        });
    }

    @GameTest(template = PLATFORM)
    public static void herKillerCarriesTheCurse(GameTestHelper h) {
        ServerPlayer p = player(h);
        LadyWhiteBoneEntity lady = lady(h, new BlockPos(4, 2, 4));
        lady.setNoAi(true);
        // she falls three times and rises; the fourth is her death
        for (int i = 0; i <= LadyWhiteBoneEntity.FALSE_DEATHS; i++) lady.die(h.getLevel().damageSources().playerAttack(p));
        h.getLevel().getEntitiesOfClass(com.jttw.engine.entity.ridge.LadyHuskEntity.class, lady.getBoundingBox().inflate(40)).forEach(Entity::discard);
        check(h, CurseOfTheDead.isCursed(p), "cursed");
        near(h, CurseOfTheDead.LASTS, CurseOfTheDead.remaining(p), "for ten days");
        check(h, p.hasEffect(ModEffects.CURSE_OF_THE_DEAD), "and it shows");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void disguisedSheIsOneOfHerDeadUntilAPlayerStrikesHer(GameTestHelper h) {
        ServerPlayer p = player(h);
        LadyWhiteBoneEntity lady = lady(h, new BlockPos(4, 2, 4));
        lady.setNoAi(true);
        lady.setDisguise(LadyWhiteBoneEntity.SERVANT_CAPTAIN);
        check(h, lady.getDisplayName().getString().startsWith("Bone Servant Captain · Lv " + RidgeMonster.CAPTAIN_LEVEL), "named as a servant captain: " + lady.getDisplayName().getString());
        close(h, ModEntities.BONE_SERVANT.get().getDimensions().height() * RidgeMonster.CAPTAIN_SCALE, lady.getBbHeight(), "a captain's height");
        lady.setDisguise(LadyWhiteBoneEntity.WRAITH);
        check(h, lady.isNoGravity() && lady.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation, "as a Wraith she drifts");
        check(h, lady.getDisplayName().getString().startsWith("Wraith · Lv " + RidgeMonster.LEVEL), "and is named one");
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(1, 2, 1));
        golem.setNoAi(true);
        lady.hurt(h.getLevel().damageSources().mobAttack(golem), 1);
        check(h, lady.disguised(), "a beast's blow does not find her out");
        lady.hurt(h.getLevel().damageSources().playerAttack(p), 1);
        check(h, !lady.disguised(), "a player's does");
        check(h, !lady.isNoGravity(), "and she stands on the ground as herself");
        check(h, lady.getDisplayName().getString().startsWith("Lady White Bone"), "named as herself");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void sheFallsThriceLeavingMortalsBodiesThenDies(GameTestHelper h) {
        ServerPlayer p = player(h);
        LadyWhiteBoneEntity lady = lady(h, new BlockPos(4, 2, 4));
        lady.setNoAi(true);
        var husks = new java.util.ArrayList<com.jttw.engine.entity.ridge.LadyHuskEntity>();
        // each body is left where she falls, and she falls back on the platform: her vanishing takes
        // her up to 14 blocks off it, where a body could land in a chunk whose entities the count
        // below cannot see yet, or among a neighbouring test's (which then counted 2, now and then)
        AABB platform = new AABB(h.absolutePos(BlockPos.ZERO)).inflate(8);
        onEnd(h, () -> h.getLevel().getEntitiesOfClass(com.jttw.engine.entity.ridge.LadyHuskEntity.class, platform).forEach(Entity::discard));
        for (int i = 0; i < LadyWhiteBoneEntity.FALSE_DEATHS; i++) {
            lady.moveTo(h.absoluteVec(new net.minecraft.world.phys.Vec3(4.5, 2, 4.5)));
            lady.setHealth(1);
            lady.die(h.getLevel().damageSources().playerAttack(p));
            check(h, lady.isAlive() && lady.getHealth() == lady.getMaxHealth(), "she rises again, whole (" + (i + 1) + ")");
        }
        husks.addAll(h.getLevel().getEntitiesOfClass(com.jttw.engine.entity.ridge.LadyHuskEntity.class, platform));
        eq(h, LadyWhiteBoneEntity.FALSE_DEATHS, husks.size(), "a body left each time");
        check(h, husks.stream().map(com.jttw.engine.entity.ridge.LadyHuskEntity::mortal).collect(java.util.stream.Collectors.toSet())
                .equals(java.util.Set.of(0, 1, 2)), "the girl, the old woman, the old man");
        check(h, !CurseOfTheDead.isCursed(p), "no curse yet");
        lady.die(h.getLevel().damageSources().playerAttack(p));
        check(h, CurseOfTheDead.isCursed(p), "the fourth time she is gone, and the curse falls");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void sheIsNeverDespawned(GameTestHelper h) {
        LadyWhiteBoneEntity lady = lady(h, new BlockPos(4, 2, 4));
        check(h, !lady.removeWhenFarAway(100000), "not when far from everyone");
        check(h, lady.isPersistenceRequired(), "she persists");
        h.succeed();
    }

    // ── The Curse of the Dead ───────────────────────────────────────────

    private static final long NOON = 6000, MIDNIGHT = 18000, DAY = 24000;

    @GameTest(template = PLATFORM)
    public static void theDeadRiseOnceEachNightOfTheCurse(GameTestHelper h) {
        ServerPlayer p = player(h);
        long day = 100 * DAY; // any day
        CurseOfTheDead.lay(p);
        p.getPersistentData().putLong(CurseOfTheDead.KEY_LAST_NIGHT, CurseOfTheDead.night(day + NOON));
        eq(h, 0, CurseOfTheDead.check(p, day + NOON).size(), "nothing by day");
        List<RidgeMonster> first = CurseOfTheDead.check(p, day + MIDNIGHT);
        sweepAround(h, first.toArray(Entity[]::new));
        check(h, first.size() == CurseOfTheDead.SERVANTS + CurseOfTheDead.WRAITHS, "the dead rise at night: " + first.size());
        eq(h, CurseOfTheDead.SERVANTS, (int) first.stream().filter(m -> m instanceof BoneServantEntity).count(), "servants");
        eq(h, CurseOfTheDead.WRAITHS, (int) first.stream().filter(m -> m instanceof WraithEntity).count(), "wraiths");
        for (RidgeMonster m : first) check(h, m.getTarget() == p, "and come for the cursed");
        eq(h, 0, CurseOfTheDead.check(p, day + MIDNIGHT + 2000).size(), "once a night");
        eq(h, 0, CurseOfTheDead.check(p, day + DAY + 1000).size(), "still that night, just before dawn");
        List<RidgeMonster> second = CurseOfTheDead.check(p, day + DAY + MIDNIGHT);
        sweepAround(h, second.toArray(Entity[]::new));
        check(h, !second.isEmpty(), "and again the next night");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theCurseEndsAfterTenDays(GameTestHelper h) {
        ServerPlayer p = player(h);
        CurseOfTheDead.lay(p);
        p.getPersistentData().putLong(CurseOfTheDead.KEY_UNTIL, h.getLevel().getGameTime() - 1);
        eq(h, 0, CurseOfTheDead.check(p, MIDNIGHT).size(), "nothing rises once it is over");
        check(h, !p.getPersistentData().contains(CurseOfTheDead.KEY_UNTIL), "the curse is gone");
        check(h, !p.hasEffect(ModEffects.CURSE_OF_THE_DEAD), "and its icon");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theCurseOutlastsDeathAndMilk(GameTestHelper h) {
        ServerPlayer p = player(h);
        CurseOfTheDead.lay(p);
        check(h, !p.removeEffectsCuredBy(net.neoforged.neoforge.common.EffectCures.MILK)
                || p.hasEffect(ModEffects.CURSE_OF_THE_DEAD), "milk does not take it");
        check(h, p.hasEffect(ModEffects.CURSE_OF_THE_DEAD), "still showing after milk");
        ServerPlayer reborn = player(h);
        CurseOfTheDead.onClone(new PlayerEvent.Clone(reborn, p, true));
        check(h, CurseOfTheDead.isCursed(reborn), "still cursed after dying");
        eq(h, p.getPersistentData().getLong(CurseOfTheDead.KEY_UNTIL), reborn.getPersistentData().getLong(CurseOfTheDead.KEY_UNTIL), "the same ten days");
        h.succeed();
    }
}
