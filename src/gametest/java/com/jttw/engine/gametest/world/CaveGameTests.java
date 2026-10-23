package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.cave.CaveMonster;
import com.jttw.engine.entity.cave.EarthFlowDecoyEntity;
import com.jttw.engine.entity.cave.FlowerMaidEntity;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.entity.cave.MouseSpiritEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.item.EarthFlowSwordItem;
import com.jttw.engine.item.PairedBlade;
import com.jttw.engine.item.WieldRules;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * The Bottomless Cave's household: Mouse Spirits and Flower Maids, their captains and the XP they
 * pay, Lady Earth Flow (her twin cuts, her household, the embroidered shoe she leaves as she flees
 * once, her death after), her swords, and the karma of it all.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class CaveGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-4 + 1e-3, what + ": expected " + expected + " but was " + actual);
    }

    /**
     * Removes, when the test ends, what this test brought, wherever it went, and whatever of the
     * cave's came of it on its own platform (only there: tests stand ten blocks apart, and a
     * sweep round a mouse at the platform's edge, or round her where she fled to, would take a
     * neighbouring test's household mid-test).
     */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, h.getBounds().inflate(2),
                    x -> x instanceof CaveMonster || x instanceof LadyEarthFlowEntity || x instanceof EarthFlowDecoyEntity
                            || x instanceof ItemEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends CaveMonster> T cave(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    /** An iron golem that stands still and does not fall to a few cuts. */
    private static net.minecraft.world.entity.animal.IronGolem dummy(GameTestHelper h, BlockPos at) {
        var golem = h.spawn(EntityType.IRON_GOLEM, at);
        golem.setNoAi(true);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10_000);
        golem.setHealth(10_000);
        sweepAround(h, golem);
        return golem;
    }

    private static LadyEarthFlowEntity lady(GameTestHelper h, BlockPos at) {
        LadyEarthFlowEntity lady = h.spawn(ModEntities.LADY_EARTH_FLOW.get(), at);
        sweepAround(h, lady);
        return lady;
    }

    // ── Captains ─────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aCaptainIsPurpleNamedLargerAndStronger(GameTestHelper h) {
        MouseSpiritEntity regular = cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 1));
        MouseSpiritEntity captain = cave(h, ModEntities.MOUSE_SPIRIT.get(), 44, true, new BlockPos(4, 2, 4));
        eq(h, ZoneMonster.WHITE, regular.getDisplayName().getStyle().getColor().getValue(), "a regular's name is white");
        eq(h, ZoneMonster.PURPLE, captain.getDisplayName().getStyle().getColor().getValue(), "a captain's purple");
        check(h, captain.getDisplayName().getString().equals("Mouse Spirit Captain · Lv 44"), "named: " + captain.getDisplayName().getString());
        eq(h, 37, regular.regularLevel(), "regulars about 37");
        eq(h, 44, regular.captainLevel(), "captains about 44");
        close(h, MouseSpiritEntity.HITS * MobScaling.typicalHit(37), regular.getMaxHealth(), "two ordinary hits of a level-37 character");
        close(h, MouseSpiritEntity.HITS * MobScaling.typicalHit(44) * ZoneMonster.CAPTAIN_HEALTH, captain.getMaxHealth(), "a captain's health");
        close(h, MouseSpiritEntity.BITE * MobScaling.typicalHealth(44) * ZoneMonster.CAPTAIN_DAMAGE,
                captain.getAttributeValue(Attributes.ATTACK_DAMAGE), "a captain's bite");
        near(h, regular.getBbHeight() * ZoneMonster.CAPTAIN_SCALE, captain.getBbHeight(), "a captain stands taller");
        eq(h, EnemyTier.GENERAL.name(), captain.getPersistentData().getString(ZoneMonster.KEY_TIER), "a captain pays as a General");
        eq(h, Zones.BOTTOMLESS.id(), regular.getPersistentData().getString(ZoneMonster.KEY_ZONE), "the cave's");
        FlowerMaidEntity maid = cave(h, ModEntities.FLOWER_MAID.get(), 37, false, new BlockPos(1, 2, 4));
        close(h, FlowerMaidEntity.HITS * MobScaling.typicalHit(37), maid.getMaxHealth(), "a maid takes five hits");
        close(h, FlowerMaidEntity.STAB * MobScaling.typicalHealth(37), maid.getAttributeValue(Attributes.ATTACK_DAMAGE), "her stab");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aMouseCaptainBringsAPackOfFour(GameTestHelper h) {
        MouseSpiritEntity captain = cave(h, ModEntities.MOUSE_SPIRIT.get(), 44, true, new BlockPos(2, 2, 2));
        captain.bringEscorts(h.getLevel());
        List<MouseSpiritEntity> pack = h.getLevel().getEntitiesOfClass(MouseSpiritEntity.class, captain.getBoundingBox().inflate(6), e -> e != captain);
        eq(h, MouseSpiritEntity.PACK, pack.size(), "its pack");
        for (MouseSpiritEntity m : pack) {
            check(h, !m.isCaptain(), "regulars");
            check(h, Math.abs(m.mobLevel() - 37) <= 1, "about level 37: " + m.mobLevel());
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyTheWorldsOwnSpawnsCanBeCaptains(GameTestHelper h) {
        for (int i = 0; i < 40; i++) {
            MouseSpiritEntity m = ModEntities.MOUSE_SPIRIT.get().create(h.getLevel());
            m.moveTo(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
            m.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(m.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            check(h, !m.isCaptain(), "a summoned one never is");
            check(h, Math.abs(m.mobLevel() - 37) <= 1, "about level 37: " + m.mobLevel());
            m.discard();
        }
        h.succeed();
    }

    // ── Mouse Spirits ───────────────────────────────────────────────────

    private static final String LIVE_MICE = "cave_live_mice", LIVE_LADY = "cave_live_lady", LIVE_ESCAPE = "cave_live_escape";

    private static void clearTheCave(ServerLevel level) {
        level.getEntities((Entity) null, new AABB(level.getSharedSpawnPos()).inflate(30_000_000),
                e -> e instanceof CaveMonster || e instanceof LadyEarthFlowEntity || e instanceof EarthFlowDecoyEntity).forEach(Entity::discard);
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = LIVE_MICE)
    public static void afterTheMice(ServerLevel level) {
        clearTheCave(level);
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = LIVE_LADY)
    public static void afterTheLady(ServerLevel level) {
        clearTheCave(level);
    }

    @net.minecraft.gametest.framework.AfterBatch(batch = LIVE_ESCAPE)
    public static void afterTheEscape(ServerLevel level) {
        clearTheCave(level);
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = LIVE_MICE)
    public static void strikeOneMouseAndThePackTurns(GameTestHelper h) {
        MouseSpiritEntity struck = cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 1));
        MouseSpiritEntity other = cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 4));
        var golem = dummy(h, new BlockPos(4, 2, 2));
        h.runAfterDelay(5, () -> struck.hurt(h.getLevel().damageSources().mobAttack(golem), 1));
        h.succeedWhen(() -> {
            check(h, struck.getTarget() == golem, "the struck one turns on its attacker");
            check(h, other.getTarget() == golem, "and so does the rest of the pack");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 120, batch = LIVE_MICE)
    public static void aMouseBitesThenScurriesBack(GameTestHelper h) {
        openAir(h);
        MouseSpiritEntity mouse = cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 1));
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
        golem.setNoAi(true);
        float full = golem.getHealth();
        mouse.setTarget(golem);
        double[] bitAt = {-1};
        h.succeedWhen(() -> {
            if (bitAt[0] < 0 && golem.getHealth() < full) bitAt[0] = mouse.distanceTo(golem);
            check(h, bitAt[0] >= 0, "it bit");
            check(h, mouse.distanceTo(golem) > bitAt[0] + 1.5, "then scurried back: " + mouse.distanceTo(golem) + " from " + bitAt[0]);
        });
    }

    // ── Flower Maids ────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aMaidTendsTheMostWoundedOfTheHousehold(GameTestHelper h) {
        FlowerMaidEntity maid = cave(h, ModEntities.FLOWER_MAID.get(), 37, false, new BlockPos(1, 2, 1));
        maid.setNoAi(true);
        MouseSpiritEntity scratched = cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(3, 2, 1));
        MouseSpiritEntity mauled = cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 3));
        scratched.setHealth(scratched.getMaxHealth() * 0.8f);
        mauled.setHealth(mauled.getMaxHealth() * 0.3f);
        maid.setHealth(maid.getMaxHealth() * 0.1f);
        check(h, maid.tendHousehold() == mauled, "the most wounded, not herself");
        close(h, mauled.getMaxHealth() * (0.3 + FlowerMaidEntity.MEND), mauled.getHealth(), "a fifth of its health mended");
        LadyEarthFlowEntity mistress = lady(h, new BlockPos(4, 2, 4));
        mistress.setNoAi(true);
        mistress.setHealth(mistress.getMaxHealth() * 0.2f);
        check(h, maid.tendHousehold() == mistress, "her mistress, worse off");
        close(h, mistress.getMaxHealth() * (0.2 + FlowerMaidEntity.MEND_MISTRESS), mistress.getHealth(), "only a little of hers");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aMaidsPollenSlowsAndWeakensThoseNear(GameTestHelper h) {
        FlowerMaidEntity maid = cave(h, ModEntities.FLOWER_MAID.get(), 37, false, new BlockPos(2, 2, 2));
        maid.setNoAi(true);
        ServerPlayer p = player(h);
        p.setGameMode(GameType.SURVIVAL);
        ServerPlayer far = player(h);
        far.setGameMode(GameType.SURVIVAL);
        far.moveTo(far.getX() + 12, far.getY(), far.getZ());
        eq(h, 1, maid.breatheOut(List.of(p, far)), "caught");
        check(h, p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && p.hasEffect(MobEffects.WEAKNESS), "slow and weak");
        check(h, !far.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "beyond its reach, not");
        h.succeed();
    }

    // ── XP and karma ────────────────────────────────────────────────────

    private static void killedBy(ServerPlayer p, LivingEntity victim) {
        LevelManager.onEntityKilled(new LivingDeathEvent(victim, p.level().damageSources().playerAttack(p)));
    }

    @GameTest(template = PLATFORM)
    public static void theCavesHouseholdPaysTheCavesXp(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 38);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.BOTTOMLESS_CAVE_BIOME.location());
        check(h, band != null && band.minLevel() == 37 && band.maxLevel() == 46, "the cave's band is 37 to 46");
        eq(h, 100, band.baseGruntXp(), "its grunt XP is stepped up from the ridge's 80");
        eq(h, 260L, band.xpForTier(EnemyTier.GRUNT), "a grunt pays 260 there");
        killedBy(p, cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 1)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a mouse");
        killedBy(p, cave(h, ModEntities.FLOWER_MAID.get(), 44, true, new BlockPos(4, 2, 4)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.GENERAL), PlayerLevel.getXp(p), "plus a maid captain");
        PlayerLevel.setLevel(p, 52);
        long xp = PlayerLevel.getXp(p);
        killedBy(p, cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 4)));
        eq(h, xp, PlayerLevel.getXp(p), "level 52 is past 46 and its grace");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void karmaForTheCavesKillsButNoneForHerShoe(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, Morality.SLAY_MONSTER, Morality.killDeed(p, cave(h, ModEntities.MOUSE_SPIRIT.get(), 37, false, new BlockPos(1, 2, 1))), "a mouse");
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, cave(h, ModEntities.FLOWER_MAID.get(), 44, true, new BlockPos(4, 2, 1))), "a captain");
        LadyEarthFlowEntity lady = lady(h, new BlockPos(4, 2, 4));
        lady.setNoAi(true);
        eq(h, Morality.SLAY_EARTH_FLOW, Morality.killDeed(p, lady), "Lady Earth Flow");
        EarthFlowDecoyEntity decoy = h.spawn(ModEntities.EARTH_FLOW_DECOY.get(), new BlockPos(1, 2, 4));
        sweepAround(h, decoy);
        eq(h, 0, Morality.killDeed(p, decoy), "her shoe is nothing");
        h.succeed();
    }

    // ── Lady Earth Flow ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void sheIsABossWithTwinSwords(GameTestHelper h) {
        LadyEarthFlowEntity lady = lady(h, new BlockPos(2, 2, 2));
        lady.setNoAi(true);
        eq(h, BossEvent.BossBarColor.PINK, lady.bossBar().getColor(), "a pink bar");
        check(h, lady.getMainHandItem().is(ModItems.EARTH_FLOW_SWORD_MALE.get()) && lady.getOffhandItem().is(ModItems.EARTH_FLOW_SWORD_FEMALE.get()),
                "the male blade in her right hand, the female in her left");
        check(h, PairedBlade.paired(lady), "a pair");
        close(h, LadyEarthFlowEntity.HEALTH, lady.getMaxHealth(), "eighty hits of a level-43 character");
        check(h, lady.getDisplayName().getString().equals("Lady Earth Flow · Lv 43"), "named: " + lady.getDisplayName().getString());
        eq(h, EnemyTier.BOSS.name(), lady.getPersistentData().getString(ZoneMonster.KEY_TIER), "pays as a Boss");
        check(h, !lady.removeWhenFarAway(10_000) && lady.isPersistenceRequired(), "never despawned");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void herStrokeIsTwoCutsAndTheSecondSlows(GameTestHelper h) {
        LadyEarthFlowEntity lady = lady(h, new BlockPos(1, 2, 1));
        lady.setNoAi(true);
        var golem = dummy(h, new BlockPos(2, 2, 2));
        float full = golem.getHealth();
        lady.cut(golem, false);
        float afterFirst = golem.getHealth();
        close(h, LadyEarthFlowEntity.CUT_DAMAGE, full - afterFirst, "the male blade's cut");
        check(h, !golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "does not slow");
        lady.cut(golem, true);
        close(h, LadyEarthFlowEntity.CUT_DAMAGE, afterFirst - golem.getHealth(), "the female blade's cut, straight after");
        check(h, golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "slows");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60, batch = LIVE_LADY)
    public static void herHouseholdComesAtSixtyPercent(GameTestHelper h) {
        LadyEarthFlowEntity lady = lady(h, new BlockPos(2, 2, 2));
        lady.setNoAi(true);
        lady.setHealth(lady.getMaxHealth() * 0.55f);
        h.succeedWhen(() -> {
            eq(h, 1, lady.phase(), "her second phase");
            List<CaveMonster> household = h.getLevel().getEntitiesOfClass(CaveMonster.class, lady.getBoundingBox().inflate(10),
                    m -> m.getPersistentData().getBoolean(LadyEarthFlowEntity.KEY_HOUSEHOLD));
            eq(h, LadyEarthFlowEntity.MICE, (int) household.stream().filter(m -> m instanceof MouseSpiritEntity).count(), "her mice");
            eq(h, LadyEarthFlowEntity.MAIDS, (int) household.stream().filter(m -> m instanceof FlowerMaidEntity).count(), "her maids");
            for (CaveMonster m : household) check(h, !m.isCaptain(), "never captains");
            check(h, !lady.fled(), "she has not fled yet");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = LIVE_ESCAPE)
    public static void atLowHealthSheLeavesHerShoeAndFleesOnceThenDies(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        LadyEarthFlowEntity lady = lady(h, new BlockPos(2, 2, 2));
        lady.setNoAi(true);
        Vec3 stood = lady.position();
        int[] stage = {0};
        h.onEachTick(() -> {
            if (stage[0] == 0 && lady.tickCount > 5) {
                // a blow that would kill her outright: she keeps a breath for her escape
                lady.hurt(h.getLevel().damageSources().playerAttack(p), lady.getMaxHealth() * 10);
                check(h, lady.isAlive() && lady.getHealth() >= 1, "no blow kills her before she flees");
                stage[0] = 1;
            }
        });
        h.succeedWhen(() -> {
            check(h, lady.fled(), "she fled");
            check(h, lady.position().distanceTo(stood) >= LadyEarthFlowEntity.FLEE_AT_LEAST - 0.5, "well away: " + lady.position().distanceTo(stood));
            List<EarthFlowDecoyEntity> decoys = h.getLevel().getEntitiesOfClass(EarthFlowDecoyEntity.class, new AABB(stood, stood).inflate(2));
            eq(h, 1, decoys.size(), "her likeness, where she stood");
            eq(h, 2, lady.phase(), "her last phase");
            // struck to the edge again, she does not flee again: she dies
            lady.hurt(h.getLevel().damageSources().playerAttack(p), lady.getMaxHealth() * 10);
            check(h, lady.isDeadOrDying(), "the second time, she dies");
            eq(h, 1, h.getLevel().getEntitiesOfClass(EarthFlowDecoyEntity.class, new AABB(stood, stood).inflate(3)).size(), "no second shoe");
        });
    }

    @GameTest(template = PLATFORM, batch = LIVE_ESCAPE)
    public static void deathItselfOnlyMakesHerFleeTheFirstTime(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        LadyEarthFlowEntity lady = lady(h, new BlockPos(2, 2, 2));
        lady.setNoAi(true);
        lady.setHealth(0);
        lady.die(h.getLevel().damageSources().playerAttack(p));
        check(h, lady.isAlive() && lady.fled() && lady.getHealth() >= 1, "she slips away instead");
        lady.setHealth(0);
        lady.die(h.getLevel().damageSources().playerAttack(p));
        check(h, lady.isDeadOrDying(), "but only once");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = LIVE_ESCAPE)
    public static void fledAndAloneSheKeepsToHerRefugeAndMendsUpToHalf(GameTestHelper h) {
        openAir(h);
        LadyEarthFlowEntity lady = lady(h, new BlockPos(2, 2, 2));
        lady.escape();
        // back on the platform, where the test's ground is loaded and she ticks
        lady.moveTo(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        lady.setHealth(lady.getMaxHealth() * 0.2f);
        check(h, lady.fled(), "fled");
        check(h, lady.refuge() != null && lady.getRestrictCenter().equals(lady.refuge())
                && lady.getRestrictRadius() == LadyEarthFlowEntity.SHRINE_GROUND, "she keeps to her refuge now");
        float low = lady.getHealth();
        h.succeedWhen(() -> {
            check(h, lady.getHealth() >= low + lady.getMaxHealth() * LadyEarthFlowEntity.MEND_PER_SECOND * 2 - 0.01, "she mends, no one near: " + lady.getHealth() + " from " + low + ", ai " + !lady.isNoAi() + ", ticks " + lady.tickCount + ", near " + lady.level().getNearestPlayer(lady, 16) + ", at " + whereabouts(h, lady));
            check(h, lady.getHealth() <= lady.getMaxHealth() * LadyEarthFlowEntity.MEND_UP_TO + 0.01, "never past half");
        });
    }

    @GameTest(template = PLATFORM, batch = LIVE_ESCAPE)
    public static void sheLeavesHerSwordsAndTheCandleAndHerShoeLeavesItself(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        LadyEarthFlowEntity lady = lady(h, new BlockPos(2, 2, 2));
        lady.setNoAi(true);
        lady.escape();
        lady.moveTo(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        lady.hurt(h.getLevel().damageSources().playerAttack(p), lady.getMaxHealth() * 10);
        check(h, lady.isDeadOrDying(), "dead");
        List<Item> drops = h.getLevel().getEntitiesOfClass(ItemEntity.class, lady.getBoundingBox().inflate(3)).stream()
                .map(i -> i.getItem().getItem()).toList();
        check(h, drops.contains(ModItems.EARTH_FLOW_SWORD_MALE.get()) && drops.contains(ModItems.EARTH_FLOW_SWORD_FEMALE.get()), "both swords: " + drops);
        check(h, drops.contains(ModItems.SPIRIT_MOUNTAIN_CANDLE.get()), "the candle she stole");
        check(h, drops.contains(ModItems.CAPTAINS_SIGIL.get()), "sigils");
        // her likeness, struck down, is a shoe again
        EarthFlowDecoyEntity decoy = h.getLevel().getEntitiesOfClass(EarthFlowDecoyEntity.class, new AABB(h.absolutePos(BlockPos.ZERO)).inflate(40))
                .stream().findFirst().orElse(null);
        check(h, decoy != null, "her likeness was left");
        decoy.hurt(h.getLevel().damageSources().playerAttack(p), decoy.getMaxHealth() * 10);
        check(h, decoy.isDeadOrDying(), "struck down");
        boolean shoe = h.getLevel().getEntitiesOfClass(ItemEntity.class, decoy.getBoundingBox().inflate(3))
                .stream().anyMatch(i -> i.getItem().is(ModItems.EMBROIDERED_SHOE.get()));
        check(h, shoe, "an embroidered shoe");
        h.succeed();
    }

    // ── Her swords ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void herSwordsArePairedAndCutTwice(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(p.getInventory().selected, new ItemStack(ModItems.EARTH_FLOW_SWORD_MALE.get()));
        p.getInventory().setItem(5, new ItemStack(ModItems.EARTH_FLOW_SWORD_FEMALE.get()));
        WieldRules.pairSwords(p);
        check(h, p.getOffhandItem().is(ModItems.EARTH_FLOW_SWORD_FEMALE.get()), "the female blade drawn into the off hand");
        check(h, PairedBlade.paired(p), "paired");
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
        golem.setNoAi(true);
        sweepAround(h, golem);
        float full = golem.getHealth();
        ItemStack male = p.getMainHandItem();
        male.getItem().hurtEnemy(male, golem, p);
        close(h, p.getAttributeValue(Attributes.ATTACK_DAMAGE) * EarthFlowSwordItem.SECOND_CUT_PERCENT / 100.0,
                full - golem.getHealth(), "the off hand's second cut");
        // put away, the drawn blade goes back
        p.getInventory().selected = (p.getInventory().selected + 1) % 9;
        p.getInventory().setItem(p.getInventory().selected, ItemStack.EMPTY);
        WieldRules.pairSwords(p);
        check(h, p.getOffhandItem().isEmpty(), "the female blade goes back");
        check(h, p.getInventory().contains(new ItemStack(ModItems.EARTH_FLOW_SWORD_FEMALE.get())), "into the inventory");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aLoneBladeCutsOnceAndTheYinYangPairStillDraws(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(p.getInventory().selected, new ItemStack(ModItems.EARTH_FLOW_SWORD_FEMALE.get()));
        WieldRules.pairSwords(p);
        check(h, p.getOffhandItem().isEmpty(), "no partner, nothing drawn");
        var golem = h.spawn(EntityType.IRON_GOLEM, new BlockPos(3, 2, 3));
        golem.setNoAi(true);
        sweepAround(h, golem);
        float full = golem.getHealth();
        p.getMainHandItem().getItem().hurtEnemy(p.getMainHandItem(), golem, p);
        eq(h, full, golem.getHealth(), "no second cut alone");
        check(h, golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the female blade slows");
        ServerPlayer nezha = player(h);
        nezha.getInventory().setItem(nezha.getInventory().selected, com.jttw.engine.item.YinYangSwordItem.createYin());
        nezha.getInventory().setItem(4, com.jttw.engine.item.YinYangSwordItem.createYang());
        WieldRules.pairSwords(nezha);
        check(h, com.jttw.engine.item.YinYangSwordItem.isYang(nezha.getOffhandItem()), "Nezha's pair draws as before");
        check(h, !nezha.getItemBySlot(EquipmentSlot.OFFHAND).is(ModItems.EARTH_FLOW_SWORD_MALE.get()), "and never hers");
        h.succeed();
    }
}
