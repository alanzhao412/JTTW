package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.hell.HellKings;
import com.jttw.engine.entity.hell.HellMonster;
import com.jttw.engine.entity.hell.HorseFaceEntity;
import com.jttw.engine.entity.hell.ImpermanenceEntity;
import com.jttw.engine.entity.hell.JudgeClerkEntity;
import com.jttw.engine.entity.hell.KingOfHellEntity;
import com.jttw.engine.entity.hell.KingYamaEntity;
import com.jttw.engine.entity.hell.OxHeadEntity;
import com.jttw.engine.entity.hell.WanderingGhostEntity;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.item.MengPoSoupItem;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.hell.HellRules;
import com.jttw.engine.world.hell.HellTerrain;
import com.jttw.engine.world.hell.HellWorks;
import com.jttw.engine.world.hell.TenCourts;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * Hell, the Ten Courts: its creatures (Ox-Head's charge, Horse-Face's hook, the clerks' writs and
 * summons, the Impermanence pair, the ghosts' chill), captains and the XP they pay, each of the Ten
 * Kings' judgements and relics, King Yama's three phases, the karma of it all, and the arrival's
 * safety in the real Hell: the Book's plaza level and empty, the courts far off, nothing spawning
 * near it, and no creature of Hell attacking anyone in its sanctuary.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class HellGameTests {

    private static void close(GameTestHelper h, double expected, double actual, String what) {
        check(h, Math.abs(expected - actual) <= Math.abs(expected) * 1e-3 + 1e-2, what + ": expected " + expected + " but was " + actual);
    }

    /**
     * Removes, when the test ends, what this test brought, wherever it went, and whatever of
     * Hell's came of it on its own platform (only there: tests stand ten blocks apart, and a sweep
     * round a king at the platform's edge would take a neighbouring test's court mid-test).
     */
    private static void sweepAround(GameTestHelper h, Entity... held) {
        onEnd(h, () -> {
            h.getLevel().getEntities((Entity) null, h.getBounds().inflate(2),
                    x -> x instanceof HellMonster || x instanceof KingOfHellEntity || x instanceof ItemEntity).forEach(Entity::discard);
            for (Entity e : held) e.discard();
        });
    }

    private static <T extends HellMonster> T hell(GameTestHelper h, EntityType<T> type, int level, boolean captain, BlockPos at) {
        T m = h.spawn(type, at);
        m.become(level, captain);
        sweepAround(h, m);
        return m;
    }

    private static <T extends KingOfHellEntity> T king(GameTestHelper h, EntityType<T> type, BlockPos at) {
        T k = h.spawn(type, at);
        k.setNoAi(true);
        sweepAround(h, k);
        return k;
    }

    /** An iron golem that stands still and does not fall to a few blows. */
    private static IronGolem dummy(GameTestHelper h, BlockPos at) {
        IronGolem golem = h.spawn(EntityType.IRON_GOLEM, at);
        golem.setNoAi(true);
        golem.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10_000);
        golem.setHealth(10_000);
        sweepAround(h, golem);
        return golem;
    }

    private static List<Item> dropsNear(GameTestHelper h, Entity e) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, e.getBoundingBox().inflate(4)).stream().map(i -> i.getItem().getItem()).toList();
    }

    // ── Captains, levels, XP ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void hellsCreaturesAreOfItsBandAndItsCaptainsNeverPastIt(GameTestHelper h) {
        OxHeadEntity ox = hell(h, ModEntities.OX_HEAD.get(), 58, false, new BlockPos(1, 2, 1));
        eq(h, Zones.HELL.id(), ox.getPersistentData().getString(ZoneMonster.KEY_ZONE), "Hell's");
        close(h, OxHeadEntity.HITS * MobScaling.typicalHit(58), ox.getMaxHealth(), "seven hits of a level-58 character");
        check(h, ox.captainLevel() <= 66, "a captain is never past 66: " + ox.captainLevel());
        for (int i = 0; i < 30; i++) check(h, ox.rollCaptainLevel(h.getLevel().random) <= 66, "a captain's roll is never past 66");
        ImpermanenceEntity white = hell(h, ModEntities.WHITE_IMPERMANENCE.get(), 66, true, new BlockPos(4, 2, 4));
        check(h, white.getDisplayName().getString().equals("White Impermanence Captain · Lv 66"), "named: " + white.getDisplayName().getString());
        eq(h, ZoneMonster.PURPLE, white.getDisplayName().getStyle().getColor().getValue(), "purple");
        eq(h, EnemyTier.GENERAL.name(), white.getPersistentData().getString(ZoneMonster.KEY_TIER), "a General");
        WanderingGhostEntity ghost = hell(h, ModEntities.WANDERING_GHOST.get(), 57, false, new BlockPos(1, 2, 4));
        eq(h, 57, ghost.regularLevel(), "ghosts about 57");
        check(h, !ghost.isOfficial() && ox.isOfficial() && white.isOfficial(), "ghosts are no officials; the rest are");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void hellsCreaturesPayHellsXp(GameTestHelper h) {
        ServerPlayer p = player(h);
        LevelManager.initClass(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 58);
        PlayerLevel.setXp(p, 0);
        BiomeLevelBand band = BiomeLevelBand.forBiome(Zones.HELL_BIOME.location());
        check(h, band != null && band.minLevel() == 57 && band.maxLevel() == 66, "Hell's band is 57 to 66");
        eq(h, 125, band.baseGruntXp(), "its grunt XP, stepped up from the Mystical Waters' 110");
        eq(h, 575L, band.xpForTier(EnemyTier.GRUNT), "a grunt pays 575 there");
        LevelManager.onEntityKilled(new LivingDeathEvent(hell(h, ModEntities.WANDERING_GHOST.get(), 57, false, new BlockPos(1, 2, 1)),
                p.level().damageSources().playerAttack(p)));
        eq(h, band.xpForTier(EnemyTier.GRUNT), PlayerLevel.getXp(p), "a ghost");
        KingOfHellEntity k = king(h, ModEntities.KING_QINGUANG.get(), new BlockPos(4, 2, 4));
        LevelManager.onEntityKilled(new LivingDeathEvent(k, p.level().damageSources().playerAttack(p)));
        eq(h, band.xpForTier(EnemyTier.GRUNT) + band.xpForTier(EnemyTier.BOSS), PlayerLevel.getXp(p), "plus a king, as a boss");
        h.succeed();
    }

    // ── Ox-Head and Horse-Face ──────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void oxHeadGoresAndThrows(GameTestHelper h) {
        OxHeadEntity ox = hell(h, ModEntities.OX_HEAD.get(), 58, false, new BlockPos(1, 2, 1));
        ox.setNoAi(true);
        IronGolem golem = dummy(h, new BlockPos(3, 2, 3));
        float full = golem.getHealth();
        check(h, ox.gore(golem), "gored");
        close(h, ox.getAttributeValue(Attributes.ATTACK_DAMAGE) * OxHeadEntity.GORE, full - golem.getHealth(), "his blow and a half");
        Vec3 away = golem.getDeltaMovement();
        check(h, away.y > 0.2 && Math.hypot(away.x, away.z) > 0.3, "thrown: " + away);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 140)
    public static void oxHeadChargesWhatKeepsAway(GameTestHelper h) {
        openAir(h);
        OxHeadEntity ox = hell(h, ModEntities.OX_HEAD.get(), 58, false, new BlockPos(0, 2, 0));
        IronGolem golem = dummy(h, new BlockPos(5, 2, 5));
        float full = golem.getHealth();
        boolean[] lowered = {false};
        h.runAfterDelay(3, () -> ox.setTarget(golem));
        h.onEachTick(() -> lowered[0] |= ox.isCharging());
        h.succeedWhen(() -> {
            check(h, lowered[0], "he lowered his horns");
            check(h, golem.getHealth() < full, "and ran it down");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void horseFaceHooksAndHaulsIn(GameTestHelper h) {
        openAir(h);
        HorseFaceEntity horse = hell(h, ModEntities.HORSE_FACE.get(), 58, false, new BlockPos(0, 2, 0));
        horse.setNoAi(true);
        // an armour stand: no mind of its own, but it moves as it is hauled
        var zombie = h.spawn(EntityType.ARMOR_STAND, new BlockPos(5, 2, 5));
        sweepAround(h, zombie);
        double before = zombie.distanceTo(horse);
        check(h, horse.hookIn(zombie), "within his hook's reach");
        IronGolem golem = dummy(h, new BlockPos(1, 2, 4));
        horse.hookIn(golem);
        check(h, golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "what he hooks is slowed");
        h.succeedWhen(() -> check(h, zombie.distanceTo(horse) < before - 1.5, "hauled in: " + zombie.distanceTo(horse) + " from " + before));
    }

    // ── The Judge-Clerk ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aClerksWritMarksAndRousesAndEverySecondSummonsAGuard(GameTestHelper h) {
        JudgeClerkEntity clerk = hell(h, ModEntities.JUDGE_CLERK.get(), 58, false, new BlockPos(1, 2, 1));
        clerk.setNoAi(true);
        WanderingGhostEntity idle = hell(h, ModEntities.WANDERING_GHOST.get(), 57, false, new BlockPos(4, 2, 1));
        idle.setNoAi(true);
        IronGolem golem = dummy(h, new BlockPos(3, 2, 4));
        check(h, clerk.write(golem) >= 1, "it rouses those near");
        check(h, JudgeClerkEntity.isMarked(golem) && golem.hasEffect(MobEffects.GLOWING), "marked: its name in his ledger");
        eq(h, golem, idle.getTarget(), "the ghost turns on it");
        eq(h, 0, clerk.summonedStanding(h.getLevel()), "the first writ calls no one");
        clerk.write(golem);
        eq(h, 1, clerk.summonedStanding(h.getLevel()), "the second calls a guard");
        check(h, !h.getLevel().getEntitiesOfClass(OxHeadEntity.class, clerk.getBoundingBox().inflate(5)).isEmpty(), "Ox-Head first");
        for (int i = 0; i < 6; i++) clerk.write(golem);
        eq(h, JudgeClerkEntity.MAX_SUMMONED, clerk.summonedStanding(h.getLevel()), "never more than two of his at once");
        h.succeed();
    }

    // ── The Black and White Impermanence ───────────────────────────────

    @GameTest(template = PLATFORM)
    public static void theImpermanenceComeTogetherAndGuardEachOther(GameTestHelper h) {
        ImpermanenceEntity black = ModEntities.BLACK_IMPERMANENCE.get().create(h.getLevel());
        black.moveTo(h.absoluteVec(new Vec3(1.5, 2, 1.5)));
        black.finalizeSpawn(h.getLevel(), h.getLevel().getCurrentDifficultyAt(black.blockPosition()), MobSpawnType.SPAWN_EGG, null);
        h.getLevel().addFreshEntity(black);
        sweepAround(h, black);
        ImpermanenceEntity white = black.partner();
        check(h, white != null && white.white() && !black.white(), "the Black came with the White");
        check(h, white.partner() == black, "a pair");
        black.setNoAi(true);
        white.setNoAi(true);
        check(h, black.together(), "together");
        IronGolem golem = dummy(h, new BlockPos(4, 2, 4));
        float full = black.getHealth();
        black.invulnerableTime = 0;
        black.hurt(h.getLevel().damageSources().mobAttack(golem), 10);
        close(h, 10 * ImpermanenceEntity.GUARDED, full - black.getHealth(), "a quarter less, beside his partner");
        // the Black shackles; the White drinks
        black.strike(golem, 5);
        check(h, golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && golem.getEffect(MobEffects.MOVEMENT_SLOWDOWN).getAmplifier() == ImpermanenceEntity.SHACKLE_LEVEL, "shackled");
        white.setHealth(white.getMaxHealth() / 2);
        float half = white.getHealth();
        white.strike(golem, 8);
        close(h, half + 8 * ImpermanenceEntity.DRINK, white.getHealth(), "the White drinks half of what he took");
        // one falls; the other rages
        white.hurt(h.getLevel().damageSources().mobAttack(golem), white.getMaxHealth() * 10);
        check(h, white.isDeadOrDying(), "the White falls");
        check(h, black.enraged() && black.hasEffect(MobEffects.DAMAGE_BOOST), "the Black is enraged");
        h.succeed();
    }

    // ── The wandering ghost ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void aGhostsTouchChills(GameTestHelper h) {
        WanderingGhostEntity ghost = hell(h, ModEntities.WANDERING_GHOST.get(), 57, false, new BlockPos(1, 2, 1));
        IronGolem golem = dummy(h, new BlockPos(2, 2, 2));
        ghost.chill(golem);
        check(h, golem.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && golem.hasEffect(MobEffects.HUNGER), "slowed and hungry");
        check(h, !ghost.causeFallDamage(20, 1, h.getLevel().damageSources().fall()), "it drifts, it does not fall");
        h.succeed();
    }

    // ── The Ten Kings ───────────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void everyKingIsABossOfHisCourtAndLeavesHisRelic(GameTestHelper h) {
        openAir(h);
        ServerPlayer p = player(h);
        Item[] relics = {ModItems.KARMA_MIRROR_SHARD.get(), ModItems.RIME_SHACKLE.get(), ModItems.IRON_SOUL_HOOK.get(), ModItems.SCALES_WEIGHT.get(),
                ModItems.YAMA_BRUSH.get(), ModItems.WAILING_BELL.get(), ModItems.OIL_CAULDRON_EMBER.get(), ModItems.CART_WHEEL_SPOKE.get(),
                ModItems.EQUAL_MEASURE_ROD.get(), ModItems.MENG_PO_SOUP.get()};
        for (int n = 1; n <= 10; n++) {
            KingOfHellEntity k = king(h, HellKings.type(n), new BlockPos(2, 2, 2));
            eq(h, n, k.court(), "court");
            int level = KingOfHellEntity.levelOf(n);
            eq(h, n == 5 ? 66 : n < 5 ? 56 + n : 55 + n, level, "his level: 57 to 65, Yama 66");
            check(h, k.getDisplayName().getString().endsWith(" · Lv " + level), "named: " + k.getDisplayName().getString());
            eq(h, EnemyTier.BOSS.name(), k.getPersistentData().getString(ZoneMonster.KEY_TIER), "pays as a Boss");
            eq(h, Zones.HELL.id(), k.getPersistentData().getString(ZoneMonster.KEY_ZONE), "of Hell");
            double hits = n == 5 ? KingYamaEntity.YAMA_HITS : KingOfHellEntity.HITS;
            close(h, hits * MobScaling.typicalHit(level), k.getMaxHealth(), "his health");
            eq(h, n == 5 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.RED, k.bossBar().getColor(), "his bar");
            // struck down (the Tenth twice: the Wheel turns once)
            for (int blow = 0; blow < 2 && !k.isDeadOrDying(); blow++) {
                k.invulnerableTime = 0;
                k.hurt(h.getLevel().damageSources().playerAttack(p), k.getMaxHealth() * 10);
            }
            check(h, k.isDeadOrDying(), "king " + n + " struck down");
            List<Item> drops = dropsNear(h, k);
            check(h, drops.contains(relics[n - 1]), "king " + n + " leaves his relic: " + drops);
            check(h, drops.contains(ModItems.CAPTAINS_SIGIL.get()) && drops.contains(ModItems.HELL_BANK_NOTE.get()), "sigils and bank notes");
            if (n == 5) check(h, drops.contains(ModItems.YAMA_SEAL.get()) && drops.contains(ModItems.BOOK_PAGE.get()), "Yama: his seal and a page of the Book");
            h.getLevel().getEntitiesOfClass(ItemEntity.class, k.getBoundingBox().inflate(6)).forEach(Entity::discard);
            k.discard();
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void qinguangsMirrorStrikesTheWickedHarder(GameTestHelper h) {
        eq(h, HellKings.Qinguang.FOR_THE_GOOD, HellKings.Qinguang.mirrorFactor(300), "lightly the good");
        eq(h, 1.0, HellKings.Qinguang.mirrorFactor(0), "the plain");
        eq(h, 2.0, HellKings.Qinguang.mirrorFactor(-500), "the wicked");
        eq(h, HellKings.Qinguang.AT_WORST, HellKings.Qinguang.mirrorFactor(-9000), "at worst three times");
        var k = king(h, ModEntities.KING_QINGUANG.get(), new BlockPos(1, 2, 1));
        ServerPlayer good = player(h), wicked = player(h);
        Morality.set(good, 400);
        Morality.set(wicked, -1000);
        close(h, k.blow() * HellKings.Qinguang.FOR_THE_GOOD, k.mirrorBlow(good), "the good's");
        close(h, k.blow() * 3, k.mirrorBlow(wicked), "the wicked's");
        IronGolem golem = dummy(h, new BlockPos(3, 2, 3));
        float full = golem.getHealth();
        check(h, k.judge(golem), "the mirror judges");
        close(h, k.blow(), full - golem.getHealth(), "a golem has no karma: his blow");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void chujiangFreezesAndBianchengsBellBringsToTheKnees(GameTestHelper h) {
        var ice = king(h, ModEntities.KING_CHUJIANG.get(), new BlockPos(1, 2, 1));
        IronGolem a = dummy(h, new BlockPos(3, 2, 3));
        eq(h, 1, ice.freeze(List.of(a)), "frozen");
        check(h, a.getTicksFrozen() > a.getTicksRequiredToFreeze() && a.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "frost over it, slowed hard");
        ice.discard();
        var bell = king(h, ModEntities.KING_BIANCHENG.get(), new BlockPos(1, 2, 1));
        IronGolem b = dummy(h, new BlockPos(4, 2, 1));
        eq(h, 1, bell.toll(List.of(b)), "it kneels");
        MobEffectInstance slow = b.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        check(h, slow != null && slow.getAmplifier() >= 6, "cannot move");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void songdiHooksAndHaulsToHisFeet(GameTestHelper h) {
        openAir(h);
        var k = king(h, ModEntities.KING_SONGDI.get(), new BlockPos(0, 2, 0));
        IronGolem golem = dummy(h, new BlockPos(5, 2, 5));
        float full = golem.getHealth();
        check(h, k.hookAndHaul(golem), "hooked");
        check(h, golem.distanceTo(k) < 3, "at his feet: " + golem.distanceTo(k));
        check(h, golem.getHealth() < full, "and struck");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wuguansScalesLevyTheHealthier(GameTestHelper h) {
        var k = king(h, ModEntities.KING_WUGUAN.get(), new BlockPos(1, 2, 1));
        IronGolem golem = dummy(h, new BlockPos(3, 2, 3));
        k.setHealth(k.getMaxHealth() * 0.4f);
        float full = golem.getHealth();
        close(h, 0.6 * HellKings.Wuguan.LEVY * golem.getMaxHealth(), k.levy(golem), "half the difference");
        k.judge(golem);
        close(h, 0.6 * HellKings.Wuguan.LEVY * golem.getMaxHealth(), full - golem.getHealth(), "taken");
        golem.setHealth(golem.getMaxHealth() * 0.2f);
        eq(h, 0f, k.levy(golem), "nothing from the worse off");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void taishanScaldsAndPingdengStrikesBackInEqualMeasure(GameTestHelper h) {
        var oil = king(h, ModEntities.KING_TAISHAN.get(), new BlockPos(1, 2, 1));
        IronGolem golem = dummy(h, new BlockPos(3, 2, 3));
        eq(h, 1, oil.scald(List.of(golem)), "scalded");
        check(h, golem.isOnFire(), "it burns");
        check(h, oil.fireImmune(), "he does not");
        oil.discard();
        var equal = king(h, ModEntities.KING_PINGDENG.get(), new BlockPos(1, 2, 1));
        IronGolem zombie = dummy(h, new BlockPos(3, 2, 1));
        float zFull = zombie.getHealth(), kFull = equal.getHealth();
        equal.hurt(h.getLevel().damageSources().mobAttack(zombie), 12);
        float taken = kFull - equal.getHealth();
        check(h, taken > 0, "he was struck");
        close(h, taken * HellKings.Pingdeng.EQUAL, zFull - zombie.getHealth(), "a third of it struck back");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void dushiChargesAndCrushes(GameTestHelper h) {
        openAir(h);
        var k = h.spawn(ModEntities.KING_DUSHI.get(), new BlockPos(0, 2, 0));
        sweepAround(h, k);
        IronGolem golem = dummy(h, new BlockPos(5, 2, 4));
        float full = golem.getHealth();
        h.runAfterDelay(3, () -> k.charge(golem));
        h.succeedWhen(() -> check(h, golem.getHealth() <= full - k.blow() * HellKings.Dushi.CRUSH * 0.99, "run down and crushed: " + (full - golem.getHealth())));
    }

    @GameTest(template = PLATFORM)
    public static void zhuanlunIsBornAgainOnceAndServesMengPosSoup(GameTestHelper h) {
        ServerPlayer p = player(h);
        var k = king(h, ModEntities.KING_ZHUANLUN.get(), new BlockPos(1, 2, 1));
        k.hurt(h.getLevel().damageSources().playerAttack(p), k.getMaxHealth() * 10);
        check(h, k.isAlive() && k.reborn(), "the Wheel turns: born again");
        close(h, k.getMaxHealth() * HellKings.Zhuanlun.REBORN_AT, k.getHealth(), "at half his health");
        k.invulnerableTime = 0;
        k.hurt(h.getLevel().damageSources().playerAttack(p), k.getMaxHealth() * 10);
        check(h, k.isDeadOrDying(), "but once only");
        IronGolem golem = dummy(h, new BlockPos(3, 2, 3));
        golem.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
        golem.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        var k2 = king(h, ModEntities.KING_ZHUANLUN.get(), new BlockPos(1, 2, 4));
        k2.soup(golem);
        check(h, !golem.hasEffect(MobEffects.REGENERATION), "what strengthened it, forgotten");
        check(h, golem.hasEffect(MobEffects.POISON) && golem.hasEffect(MobEffects.CONFUSION), "the rest stays; it reels");
        // the soup he leaves, drunk: everything forgotten
        MengPoSoupItem.drink(golem);
        check(h, !golem.hasEffect(MobEffects.POISON), "drunk: every effect forgotten");
        check(h, new ItemStack(ModItems.MENG_PO_SOUP.get()).getUseAnimation() == net.minecraft.world.item.UseAnim.DRINK, "a drink");
        h.succeed();
    }

    // ── King Yama ───────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 60)
    public static void yamaCallsHisCourtThenComesHisWrath(GameTestHelper h) {
        openAir(h);
        KingYamaEntity yama = king(h, ModEntities.KING_YAMA.get(), new BlockPos(2, 2, 2));
        float tall = yama.getBbHeight();
        double power = yama.getAttributeValue(Attributes.ATTACK_DAMAGE);
        int[] stage = {0};
        h.onEachTick(() -> {
            if (stage[0] == 0 && yama.tickCount > 3) {
                yama.setHealth(yama.getMaxHealth() * 0.6f);
                stage[0] = 1;
            } else if (stage[0] == 1 && yama.phase() == 1) {
                yama.setHealth(yama.getMaxHealth() * 0.3f);
                stage[0] = 2;
            }
        });
        h.succeedWhen(() -> {
            eq(h, 2, yama.phase(), "his last phase");
            List<HellMonster> court = h.getLevel().getEntitiesOfClass(HellMonster.class, yama.getBoundingBox().inflate(8),
                    m -> m.getPersistentData().getBoolean(KingYamaEntity.KEY_COURT));
            eq(h, 3, court.size(), "his court rose at 66%");
            check(h, court.stream().anyMatch(m -> m instanceof OxHeadEntity) && court.stream().anyMatch(m -> m instanceof HorseFaceEntity)
                    && court.stream().anyMatch(m -> m instanceof JudgeClerkEntity), "Ox-Head, Horse-Face and a clerk");
            check(h, yama.inWrath(), "his wrath at 33%");
            close(h, tall * KingYamaEntity.WRATH_SCALE, yama.getBbHeight(), "he grows");
            close(h, power * (1 + KingYamaEntity.WRATH_POWER), yama.getAttributeValue(Attributes.ATTACK_DAMAGE), "he strikes harder");
        });
    }

    @GameTest(template = PLATFORM, timeoutTicks = 80)
    public static void yamasSentenceFallsWhereItWasPassed(GameTestHelper h) {
        openAir(h);
        // with his wits about him (a sentence falls in his own time)
        KingYamaEntity yama = h.spawn(ModEntities.KING_YAMA.get(), new BlockPos(0, 2, 0));
        sweepAround(h, yama);
        IronGolem stays = dummy(h, new BlockPos(4, 2, 4)), steps = dummy(h, new BlockPos(4, 2, 1));
        float full = stays.getHealth();
        long[] passedAt = {-1};
        h.runAfterDelay(3, () -> {
            yama.sentence(stays.position());
            yama.sentence(steps.position());
            passedAt[0] = h.getLevel().getGameTime();
            // one steps out of its ring at once
            steps.teleportTo(steps.getX() - 4, steps.getY(), steps.getZ() + 0);
        });
        h.succeedWhen(() -> {
            check(h, passedAt[0] > 0 && yama.pending().isEmpty(), "the sentences have fallen");
            check(h, h.getLevel().getGameTime() - passedAt[0] >= KingYamaEntity.SENTENCE_DELAY, "after their delay");
            close(h, full * KingYamaEntity.SENTENCE_SHARE, full - stays.getHealth(), "a third of the health of what stayed");
            eq(h, full, steps.getHealth(), "nothing of what stepped out");
        });
    }

    @GameTest(template = PLATFORM)
    public static void yamasWritMarksAndRouses(GameTestHelper h) {
        KingYamaEntity yama = king(h, ModEntities.KING_YAMA.get(), new BlockPos(1, 2, 1));
        WanderingGhostEntity ghost = hell(h, ModEntities.WANDERING_GHOST.get(), 57, false, new BlockPos(4, 2, 1));
        ghost.setNoAi(true);
        IronGolem golem = dummy(h, new BlockPos(3, 2, 4));
        check(h, yama.writ(golem) >= 1, "rouses");
        check(h, JudgeClerkEntity.isMarked(golem) && ghost.getTarget() == golem, "marked; the ghost comes for it");
        h.succeed();
    }

    // ── Karma ───────────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void karmaForHell(GameTestHelper h) {
        ServerPlayer p = player(h);
        eq(h, Morality.SLAY_MONSTER, Morality.killDeed(p, hell(h, ModEntities.WANDERING_GHOST.get(), 57, false, new BlockPos(1, 2, 1))), "a ghost laid to rest");
        eq(h, 0, Morality.killDeed(p, hell(h, ModEntities.OX_HEAD.get(), 58, false, new BlockPos(4, 2, 1))), "an official doing his work: nothing");
        eq(h, Morality.SLAY_CAPTAIN, Morality.killDeed(p, hell(h, ModEntities.JUDGE_CLERK.get(), 65, true, new BlockPos(1, 2, 4))), "a captain, as any");
        eq(h, Morality.SLAY_KING_OF_HELL, Morality.killDeed(p, king(h, ModEntities.KING_DUSHI.get(), new BlockPos(4, 2, 4))), "a King of Hell");
        eq(h, Morality.SLAY_KING_YAMA, Morality.killDeed(p, king(h, ModEntities.KING_YAMA.get(), new BlockPos(2, 2, 2))), "King Yama");
        check(h, 9 * Morality.SLAY_KING_OF_HELL + Morality.SLAY_KING_YAMA <= Morality.OUTLAW, "all ten make an outlaw of Heaven");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theKingsAreBossesOfTheReincarnationAndGhostsTheRestlessDead(GameTestHelper h) {
        ServerPlayer p = player(h);
        var source = h.getLevel().damageSources().playerAttack(p);
        int primordial = com.jttw.engine.reincarnation.AlignmentData.getPrimordial(p);
        com.jttw.engine.reincarnation.AlignmentEarningHandler.onEntityKill(new LivingDeathEvent(king(h, ModEntities.KING_SONGDI.get(), new BlockPos(1, 2, 1)), source));
        eq(h, primordial + 20, com.jttw.engine.reincarnation.AlignmentData.getPrimordial(p), "a King of Hell: +20 Primordial");
        com.jttw.engine.reincarnation.AlignmentEarningHandler.onEntityKill(new LivingDeathEvent(king(h, ModEntities.KING_YAMA.get(), new BlockPos(4, 2, 4)), source));
        eq(h, primordial + 70, com.jttw.engine.reincarnation.AlignmentData.getPrimordial(p), "King Yama: +50");
        int heavenly = com.jttw.engine.reincarnation.AlignmentData.getHeavenly(p);
        WanderingGhostEntity ghost = hell(h, ModEntities.WANDERING_GHOST.get(), 57, false, new BlockPos(1, 2, 4));
        check(h, ghost.isInvertedHealAndHarm(), "healing harms a ghost");
        com.jttw.engine.reincarnation.AlignmentEarningHandler.onEntityKill(new LivingDeathEvent(ghost, source));
        eq(h, heavenly + 2, com.jttw.engine.reincarnation.AlignmentData.getHeavenly(p), "laying the restless dead to rest is Heavenly");
        h.succeed();
    }

    // ── The arrival, in the real Hell ───────────────────────────────────

    /** As QiAndFlightGameTests: a fake player taken to another world must leave its player list. */
    private static void leaveWorld(ServerPlayer p) {
        p.serverLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void theArrivalIsLevelEmptyAndKeepsTheBook(GameTestHelper h) {
        ServerLevel hell = h.getLevel().getServer().getLevel(JTTWDimensions.HELL);
        check(h, hell != null, "Hell is loaded");
        ServerPlayer p = player(h);
        JTTWDimensions.teleportTo(p, JTTWDimensions.HELL);
        eq(h, JTTWDimensions.HELL, p.level().dimension(), "in Hell");
        // where travellers arrive (JTTWDimensions.findSafeSpawn: the first ground out from 0, 0)
        BlockPos arrival = JTTWDimensions.findSafeSpawn(hell);
        eq(h, HellTerrain.GROUND + 1, arrival.getY(), "they stand on the plaza");
        p.moveTo(arrival.getX() + 0.5, arrival.getY(), arrival.getZ() + 0.5);
        BlockPos book = arrival.offset(0, 0, 4);
        check(h, hell.getBlockState(book).is(ModBlocks.BOOK_OF_LIFE_AND_DEATH.get()), "the Book on its dais");
        check(h, hell.getBiome(book).is(Zones.HELL_BIOME), "in Hell's biome");
        check(h, HellRules.inSanctuary(p), "in the sanctuary");
        // a second traveller finds the same Book, not a second one
        ServerPlayer q = player(h);
        JTTWDimensions.teleportTo(q, JTTWDimensions.HELL);
        int books = 0;
        for (BlockPos at : BlockPos.betweenClosed(book.offset(-24, -4, -24), book.offset(24, 4, 24))) {
            if (hell.getBlockState(at).is(ModBlocks.BOOK_OF_LIFE_AND_DEATH.get())) books++;
        }
        eq(h, 1, books, "one Book");
        // round the arrival: the plaza's paving, nothing of Hell's near
        check(h, hell.getHeight(Heightmap.Types.MOTION_BLOCKING, 10, -10) == HellTerrain.GROUND + 1, "the plaza is level");
        List<LivingEntity> near = hell.getEntitiesOfClass(LivingEntity.class, new AABB(p.blockPosition()).inflate(96),
                e -> e instanceof HellMonster || e instanceof KingOfHellEntity);
        check(h, near.isEmpty(), "no creature of Hell near the arrival: " + near);
        // the Ox-Head who comes anyway will not touch anyone in the sanctuary
        OxHeadEntity ox = ModEntities.OX_HEAD.get().create(hell);
        ox.moveTo(p.getX() + 5, p.getY(), p.getZ() + 5);
        ox.setTarget(p);
        check(h, ox.getTarget() == null && !ox.canAttack(p), "Hell's creatures spare those in the sanctuary");
        ox.discard();
        var yama = ModEntities.KING_YAMA.get().create(hell);
        yama.moveTo(p.getX() + 5, p.getY(), p.getZ() + 5);
        yama.setTarget(p);
        check(h, yama.getTarget() == null && !yama.canAttack(p), "even King Yama");
        yama.discard();
        leaveWorld(p);
        leaveWorld(q);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 600)
    public static void theFortressStandsFarNorthWithItsKingsOnTheirThrones(GameTestHelper h) {
        ServerLevel hell = h.getLevel().getServer().getLevel(JTTWDimensions.HELL);
        TenCourts.Court first = TenCourts.court(1), yama = TenCourts.court(5);
        List<BlockPos> spots = List.of(HellWorks.kingSpot(first), HellWorks.kingSpot(yama));
        for (BlockPos s : spots) hell.setChunkForced(s.getX() >> 4, s.getZ() >> 4, true);
        onEnd(h, () -> {
            for (BlockPos s : spots) hell.setChunkForced(s.getX() >> 4, s.getZ() >> 4, false);
        });
        h.succeedWhen(() -> {
            for (BlockPos s : spots) {
                check(h, HellRules.fromArrival(s.getX(), s.getZ()) >= HellRules.FORTRESS_AT_LEAST, "far from the arrival");
                List<KingOfHellEntity> kings = hell.getEntitiesOfClass(KingOfHellEntity.class, new AABB(s).inflate(6));
                check(h, !kings.isEmpty(), "a king on the throne at " + s);
            }
            check(h, hell.getEntitiesOfClass(KingYamaEntity.class, new AABB(spots.get(1)).inflate(6)).size() == 1, "King Yama on his");
            check(h, hell.getEntitiesOfClass(HellKings.Qinguang.class, new AABB(spots.get(0)).inflate(6)).size() == 1, "King Qinguang on his");
            check(h, !hell.getBlockState(spots.get(1).below()).isAir(), "standing on his dais");
        });
    }
}
