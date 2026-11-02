package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.heaven.AreaBlow;
import com.jttw.engine.entity.heaven.HeavenBoss;
import com.jttw.engine.entity.heaven.HeavenChampion;
import com.jttw.engine.entity.heaven.HeavenFolk;
import com.jttw.engine.entity.heaven.HeavenMonster;
import com.jttw.engine.entity.heaven.HeavenlyKingEntity;
import com.jttw.engine.entity.heaven.LunarMansionEntity;
import com.jttw.engine.entity.heaven.StarLordEntity;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.BiomeLevelBand;
import com.jttw.engine.progression.level.EnemyTier;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.HeavenRegistry;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.heaven.HeavenCourt;
import com.jttw.engine.world.heaven.HeavenGate;
import com.jttw.engine.world.heaven.HeavenHostility;
import com.jttw.engine.world.heaven.HeavenRegions;
import com.jttw.engine.world.heaven.HeavenSpawner;
import com.jttw.engine.world.morality.Morality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.as;

/**
 * Heaven's host: Heaven as wild grass of every level (its regions, the road's levels, the XP), whom its
 * host fights (by class and karma), who struck first (karma), its ranks and kits (each kind's hits, blow and
 * telegraphed area blow), its court's posts, the pass up, and the champions who will not fight themselves.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class HeavenHostGameTests {

    private static final double EPS = 1e-6;

    private static List<Entity> heavenKinds(GameTestHelper h) {
        List<Entity> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!JTTWEngine.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) continue;
            Entity e = type.create(h.getLevel());
            if (e instanceof HeavenFolk) out.add(e);
            else if (e != null) e.discard();
        }
        return out;
    }

    private static String name(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    // ── Wild grass ──────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void heavensRegionLevelsSpanOneToAHundred(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        BlockPos palace = HeavenRegions.palace(seed);
        eq(h, 100, HeavenRegions.roadLevel(seed, palace.getX(), palace.getZ()), "the Jade Palace is the road's top");
        eq(h, 1, HeavenRegions.roadLevel(seed, palace.getX() + 5000, palace.getZ()), "the outermost clouds its bottom");
        Set<Integer> seen = new HashSet<>();
        int last = 101;
        for (int d = 0; d <= 4000; d += 9) {
            int lv = HeavenRegions.levelAtDistance(d);
            check(h, lv <= last, "never higher further out: " + d);
            last = lv;
            seen.add(lv);
        }
        eq(h, 100, seen.size(), "every level from 1 to 100 somewhere on the road");
        eq(h, 5, Zones.HEAVEN_REGIONS.size(), "five regions");
        int expectMin = 1;
        for (Zones.Zone z : Zones.HEAVEN_REGIONS) {
            eq(h, expectMin, z.minLevel(), z.id() + " starts where the last ended");
            eq(h, expectMin + (expectMin == 1 ? 19 : 19), z.maxLevel(), z.id() + " twenty levels");
            eq(h, z, HeavenRegions.regionOf(z.minLevel()), "its bottom is its");
            eq(h, z, HeavenRegions.regionOf(z.maxLevel()), "its top is its");
            expectMin = z.maxLevel() + 1;
        }
        eq(h, 101, expectMin, "the regions end at 100");
        for (int lv = 1; lv <= 100; lv++) {
            eq(h, lv, HeavenRegions.levelAtDistance(HeavenRegions.distanceFor(lv)), "the road at its own level's ring: " + lv);
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heavensRegularsCaptainsAndMinibossesRollByTheRoad(GameTestHelper h) {
        RandomSource r = RandomSource.create(5);
        for (int road = 1; road <= 100; road++) {
            Zones.Zone z = HeavenRegions.regionOf(road);
            for (int i = 0; i < 6; i++) {
                int reg = HeavenRegions.regularLevel(road, r);
                check(h, Math.abs(reg - road) <= 1 && reg >= z.minLevel() && reg <= z.maxLevel(), "a regular one either way of the road, in its band: " + road + " " + reg);
                int cap = HeavenRegions.captainLevel(road, r);
                check(h, cap <= z.maxLevel() + 1 && (cap >= road + 3 || cap == z.maxLevel() + 1), "a captain three to five over: " + road + " " + cap);
            }
            int mb = HeavenRegions.minibossLevel(road);
            check(h, mb <= z.maxLevel() + 1 && (mb == road + 5 || mb == z.maxLevel() + 1 || road < z.minLevel()), "a miniboss five over: " + road + " " + mb);
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heavensKillsPayAsAZonesOfTheirLevel(GameTestHelper h) {
        for (int lv : new int[]{20, 30, 45, 50, 60, 72, 85, 95, 100}) {
            BiomeLevelBand band = LevelManager.bandHolding(lv);
            check(h, band != null && lv >= band.minLevel() && lv <= band.maxLevel(), "a band holds level " + lv);
            for (EnemyTier tier : EnemyTier.values()) {
                eq(h, band.xpForTier(tier), LevelManager.heavenKillXp(lv, tier, lv, 2), "a level-" + lv + " " + tier + " pays its zone's");
            }
            eq(h, band.xpForTier(EnemyTier.GRUNT), LevelManager.heavenKillXp(lv, EnemyTier.GRUNT, lv + LevelManager.HEAVEN_OVER, 2), "five levels over it still paid");
            eq(h, 0L, LevelManager.heavenKillXp(lv, EnemyTier.GRUNT, lv + LevelManager.HEAVEN_OVER + 1, 2), "six over it, nothing");
        }
        // a level-50 grunt pays the Mystical Waters' (47-56) grunt XP
        eq(h, Zones.MYSTICAL_WATERS.levelBand().xpForTier(EnemyTier.GRUNT), LevelManager.heavenKillXp(50, EnemyTier.GRUNT, 50, 2), "the Waters' pay at 50");
        // a level-103 great name pays a boss of the last band's
        eq(h, LevelManager.bandHolding(100).xpForTier(EnemyTier.BOSS), LevelManager.heavenKillXp(103, EnemyTier.BOSS, 100, 20), "Li Jing pays Lion-Camel's boss XP");
        // below 20, as any monster's, weighed by rank
        eq(h, 12L, LevelManager.heavenKillXp(8, EnemyTier.GRUNT, 10, 2), "a regular, twice a zombie's at 10");
        eq(h, 36L, LevelManager.heavenKillXp(8, EnemyTier.GENERAL, 10, 6), "a captain six times");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aHeavenKillIsPaidWhenItDies(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setLevel(p, 50);
        long before = PlayerLevel.getXp(p);
        HeavenMonster s = h.spawn(ModEntities.HEAVENLY_SOLDIER.get(), new BlockPos(2, 2, 3));
        s.become(50, false);
        s.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        eq(h, LevelManager.heavenKillXp(50, EnemyTier.GRUNT, 50, 2), PlayerLevel.getXp(p) - before, "a level-50 soldier's XP");
        h.succeed();
    }

    // ── Whom the host fights ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void heavenAttacksDemonsAndTheWickedOnSight(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN), bull = as(h, ReincarnationOutcome.BULL_DEMON_KING),
                dragon = as(h, ReincarnationOutcome.DRAGON);
        eq(h, HeavenHostility.Why.DEMON, HeavenHostility.why(bull), "a Yaoguai on sight");
        eq(h, HeavenHostility.Why.NOT, HeavenHostility.why(erlang), "a Heavenly left alone");
        eq(h, HeavenHostility.Why.NOT, HeavenHostility.why(dragon), "a Divine Beast left alone");
        Morality.set(dragon, HeavenHostility.ON_SIGHT + 1);
        check(h, !HeavenHostility.attacksOnSight(dragon), "karma just over a suspect's: still left alone");
        Morality.set(dragon, HeavenHostility.ON_SIGHT);
        eq(h, HeavenHostility.Why.BAD_KARMA, HeavenHostility.why(dragon), "a suspect on sight");
        eq(h, "Suspect", Morality.standing(HeavenHostility.ON_SIGHT), "the threshold is Heaven's Suspect");
        Morality.set(bull, 1000);
        check(h, HeavenHostility.attacksOnSight(bull), "a demon however good");
        bull.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        check(h, !HeavenHostility.attacksOnSight(bull), "never one in creative");
        // and the host's own targeting goal asks it
        HeavenMonster s = h.spawn(ModEntities.HEAVENLY_SOLDIER.get(), new BlockPos(2, 2, 3));
        check(h, s.targetSelector.getAvailableGoals().stream().anyMatch(g -> g.getGoal() instanceof net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<?>),
                "it looks for those it attacks on sight");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aNeutralPlayerIsLeftAloneUntilTheyStrike(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        HeavenMonster struck = h.spawn(ModEntities.HEAVENLY_SOLDIER.get(), new BlockPos(1, 2, 3));
        HeavenMonster near = h.spawn(HeavenRegistry.HEAVENLY_ARCHER.get(), new BlockPos(3, 2, 3));
        HeavenMonster far = h.spawn(HeavenRegistry.THUNDER_DRUMMER.get(), new BlockPos(2, 2, 3));
        far.teleportTo(far.getX() + HeavenHostility.RALLY + 6, far.getY(), far.getZ());
        for (HeavenMonster m : List.of(struck, near, far)) m.become(20, false);
        check(h, struck.getTarget() == null && near.getTarget() == null, "no one comes at a neutral player");
        struck.hurt(h.getLevel().damageSources().playerAttack(p), 1f);
        struck.setTarget(p); // as its HurtByTargetGoal does
        check(h, near.getTarget() == p, "its ally near rallies to it");
        check(h, far.getTarget() == null, "one past the rally's reach does not");
        check(h, struck.grudge().struckFirst(p) && !struck.grudge().selfDefence(p), "the player struck it first");
        check(h, near.grudge().selfDefence(p), "the one that rallied came at them");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heavenJudgesWhoStruckFirst(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        Morality.set(p, 200);
        HeavenMonster first = h.spawn(ModEntities.HEAVENLY_SOLDIER.get(), new BlockPos(1, 2, 3));
        first.become(20, false);
        first.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        eq(h, 200 + HeavenHostility.SOLDIER, Morality.karma(p), "striking one down unprovoked is a crime");
        HeavenMonster came = h.spawn(HeavenRegistry.LUNAR_MANSION.get(), new BlockPos(3, 2, 3));
        came.become(25, true);
        came.setTarget(p);
        came.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        eq(h, 200 + HeavenHostility.SOLDIER, Morality.karma(p), "one that came at you, nothing");
        HeavenMonster captain = h.spawn(HeavenRegistry.LUNAR_MANSION.get(), new BlockPos(3, 2, 2));
        captain.become(25, true);
        eq(h, HeavenHostility.STAR_GENERAL, HeavenHostility.killDeed(p, captain), "a star general struck first");
        HeavenMonster lord = h.spawn(HeavenRegistry.STAR_LORD.get(), new BlockPos(2, 2, 1));
        eq(h, HeavenHostility.GENERAL, HeavenHostility.killDeed(p, lord), "a star lord struck first");
        eq(h, HeavenHostility.HEAVENLY_KING, HeavenRegistry.KING_CHIGUO.get().create(h.getLevel()).crime(), "a Heavenly King");
        eq(h, HeavenHostility.LI_JING, HeavenRegistry.LI_JING.get().create(h.getLevel()).crime(), "Li Jing");
        eq(h, HeavenHostility.CHAMPION, HeavenRegistry.ERLANG_SHEN.get().create(h.getLevel()).crime(), "Erlang Shen");
        // a good player who starts every fight with the whole court is not yet an outlaw; a grinder of the host never is one by it
        int court = 4 * HeavenHostility.HEAVENLY_KING + HeavenHostility.LI_JING + 2 * HeavenHostility.CHAMPION;
        check(h, Morality.wanted(100 + 4 * HeavenHostility.HEAVENLY_KING) == 0, "all four Kings: not an outlaw");
        check(h, Morality.wanted(court) >= 1, "every great name, struck first: an outlaw, as the Dragon Kings make one");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "heavenly_host")
    public static void heavenSendsItsHostDownRoundAnOutlaw(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.DRAGON);
        Morality.set(p, Morality.OUTLAW);
        check(h, HeavenHostility.attacksOnSight(p), "an outlaw is attacked on sight");
        List<HeavenMonster> sent = new ArrayList<>(com.jttw.engine.world.morality.HeavenlyHost.wave(h.getLevel(), p, Morality.wanted(p)));
        onEnd(h, () -> sent.forEach(Entity::discard));
        eq(h, com.jttw.engine.world.morality.HeavenlyHost.WAVE[1], sent.size(), "a wave of the host round them");
        check(h, sent.stream().allMatch(m -> m.getTarget() == p && m.grudge().selfDefence(p)), "every one comes at them: self-defence to kill");
        check(h, com.jttw.engine.world.morality.HeavenlyHost.INTERVAL[3] < com.jttw.engine.world.morality.HeavenlyHost.INTERVAL[1], "oftener the more wanted");
        h.succeed();
    }

    // ── Ranks and kits ──────────────────────────────────────────────────

    /** A wind-up long enough to step out of: at least this many ticks for a miniboss's or a boss's. */
    public static final int LEAST_WIND_UP = 20;

    @GameTest(template = PLATFORM)
    public static void everyKindOfHeavensHostKeepsItsRank(GameTestHelper h) {
        int regulars = 0, minibosses = 0, bosses = 0;
        for (Entity e : heavenKinds(h)) {
            try {
                if (e instanceof HeavenMonster m && !m.alwaysMiniboss()) {
                    regulars++;
                    m.become(40, false);
                    double hits = m.getMaxHealth() / MobScaling.typicalHit(40), blow = m.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(40);
                    check(h, hits >= 2 - EPS && hits <= 12 + EPS, name(m) + " a regular's 2-12 hits: " + hits);
                    check(h, blow <= 0.12 + EPS && m.hardestShare() <= 0.12 + EPS, name(m) + " a regular's blow: " + blow);
                    m.become(44, true);
                    double ch = m.getMaxHealth() / MobScaling.typicalHit(44), cb = m.hardestShare() * m.damageScale();
                    check(h, ch >= 4 - EPS && ch <= 24 + EPS, name(m) + " a captain's 4-24 hits: " + ch);
                    check(h, cb <= 0.144 + EPS, name(m) + " a captain's hardest at most 14.4%: " + cb);
                } else if (e instanceof HeavenMonster m) {
                    minibosses++;
                    m.become(45, false);
                    check(h, m.isMiniboss(), name(m) + " always a miniboss");
                    double hits = m.getMaxHealth() / MobScaling.typicalHit(45), blow = m.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(45);
                    check(h, hits >= 25 - EPS && hits <= 40 + EPS, name(m) + " a miniboss's 25-40 hits: " + hits);
                    check(h, blow >= 0.14 - EPS && blow <= 0.16 + EPS, name(m) + " a miniboss's blow 14-16%: " + blow);
                    eq(h, 7, (int) Math.ceil(1 / (m.hardestShare() * m.damageScale()) - EPS), name(m) + " its hardest kills in seven");
                    AreaBlow b = m.blow();
                    check(h, b != null && b.spec().windUp() >= LEAST_WIND_UP, name(m) + " a telegraphed area blow");
                    check(h, Math.abs(b.spec().share() * m.damageScale() - ZoneMonster.MINIBOSS_HIT) < EPS, name(m) + " its area blow its hardest, 16%");
                } else if (e instanceof HeavenBoss b) {
                    bosses++;
                    int top = b.zone().maxLevel();
                    eq(h, top + (b.lesser() ? ZoneBoss.LESSER_ABOVE_BAND : ZoneBoss.LEVELS_ABOVE_BAND), b.mobLevel(), name(b) + " its region's top and three (a lesser two)");
                    double hits = b.hitsToKill(), blow = b.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(Math.min(100, top));
                    check(h, hits >= 60 - EPS && hits <= 80 + EPS, name(b) + " 60-80 hits of its region's top: " + hits);
                    check(h, blow >= 0.18 - EPS && blow <= 0.20 + EPS, name(b) + " a boss's blow: " + blow);
                    int kills = (int) Math.ceil(1 / blow - EPS);
                    check(h, kills >= 5 && kills <= 6, name(b) + " kills in five or six: " + kills);
                    check(h, b.blow().spec().windUp() >= LEAST_WIND_UP, name(b) + " its area blow telegraphed");
                    check(h, b.blow().spec().share() <= 0.20 + EPS, name(b) + " its area blow no harder than a boss's blow");
                    check(h, !b.headsLair(), name(b) + " no usurper takes Heaven's posts");
                }
            } finally {
                e.discard();
            }
        }
        eq(h, 5, regulars, "the halberdier, archer, cavalry, drummer and the Lunar Mansions");
        eq(h, 6, minibosses, "the star lords, the Giant, the Thunder Lord, the Lightning Mother, the Eye and the Ear");
        eq(h, 7, bosses, "the Four Kings, Li Jing, Nezha and Erlang Shen");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heavensGreatNamesStandAtTheirRegionsTops(GameTestHelper h) {
        eq(h, 23, HeavenRegistry.KING_CHIGUO.get().create(h.getLevel()).mobLevel(), "King Chiguo of the East, the Outer Clouds' head");
        eq(h, 43, HeavenRegistry.KING_ZENGZHANG.get().create(h.getLevel()).mobLevel(), "King Zengzhang of the South, the Cloud Road's");
        eq(h, 63, HeavenRegistry.KING_GUANGMU.get().create(h.getLevel()).mobLevel(), "King Guangmu of the West, the Star Palaces'");
        eq(h, 83, HeavenRegistry.KING_DUOWEN.get().create(h.getLevel()).mobLevel(), "King Duowen of the North, the Thunder Court's");
        eq(h, 103, HeavenRegistry.LI_JING.get().create(h.getLevel()).mobLevel(), "Li Jing, the Jade Palace's");
        eq(h, 102, HeavenRegistry.NEZHA.get().create(h.getLevel()).mobLevel(), "Nezha a level under him");
        eq(h, 102, HeavenRegistry.ERLANG_SHEN.get().create(h.getLevel()).mobLevel(), "Erlang Shen too");
        for (int q = 0; q < 4; q++) eq(h, q, ((HeavenlyKingEntity) HeavenRegistry.kings().get(q).create(h.getLevel())).quarter(), "the kings by quarter");
        Set<String> names = new HashSet<>();
        LunarMansionEntity m = HeavenRegistry.LUNAR_MANSION.get().create(h.getLevel());
        for (int v = 0; v < 28; v++) {
            m.setVariant(v);
            names.add(m.getDisplayName().getString());
        }
        eq(h, 28, names.size(), "twenty-eight Lunar Mansions, each its own");
        StarLordEntity s = HeavenRegistry.STAR_LORD.get().create(h.getLevel());
        names.clear();
        for (int v = 0; v < 9; v++) {
            s.setVariant(v);
            names.add(s.getDisplayName().getString());
        }
        eq(h, 9, names.size(), "nine luminaries");
        h.succeed();
    }

    /** Each area blow falls on a foe in its marked area, and not on one who stepped out of it. */
    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void everyAreaBlowCanBeSteppedOutOf(GameTestHelper h) {
        int tried = 0;
        for (Entity e : heavenKinds(h)) {
            AreaBlow b = e instanceof HeavenMonster m ? m.blow() : e instanceof HeavenBoss bb ? bb.blow() : null;
            if (b == null || !(e instanceof Mob owner)) {
                e.discard();
                continue;
            }
            tried++;
            Vec3 at = Vec3.atBottomCenterOf(h.absolutePos(new BlockPos(2, 2, 0)));
            owner.moveTo(at.x, at.y, at.z, 0f, 0f); // facing south (+z)
            owner.setNoAi(true);
            h.getLevel().addFreshEntity(owner);
            if (owner instanceof HeavenMonster hm) hm.become(30, hm.alwaysMiniboss());
            Zombie in = foe(h, owner, new BlockPos(2, 2, 3)), out = foe(h, owner, new BlockPos(2, 2, 3));
            check(h, b.begin(in), name(owner) + " begins its blow");
            check(h, b.winding(), name(owner) + " winds up first");
            // one steps out of it: far off to the side and behind
            out.teleportTo(at.x + 14, at.y, at.z - 14);
            List<?> caught = b.strike();
            check(h, caught.contains(in), name(owner) + " strikes the one who stayed in it");
            check(h, !caught.contains(out), name(owner) + " spares the one who stepped out");
            in.discard();
            out.discard();
            owner.discard();
        }
        eq(h, 15, tried, "every kind with an area blow (the cavalry's charge, the drummer's roll, six minibosses', seven bosses')");
        h.succeed();
    }

    private static Zombie foe(GameTestHelper h, Mob of, BlockPos at) {
        Zombie z = h.spawn(EntityType.ZOMBIE, at);
        z.setNoAi(true);
        z.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1e6);
        z.setHealth(1e6f);
        z.setTarget(of);
        return z;
    }

    // ── Heaven's champions and the players who are them ─────────────────

    @GameTest(template = PLATFORM)
    public static void heavensChampionsWillNotFightThemselves(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN), nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE),
                bull = as(h, ReincarnationOutcome.BULL_DEMON_KING);
        HeavenChampion npcErlang = h.spawn(HeavenRegistry.ERLANG_SHEN.get(), new BlockPos(1, 2, 3));
        HeavenChampion npcNezha = h.spawn(HeavenRegistry.NEZHA.get(), new BlockPos(3, 2, 3));
        check(h, npcErlang.isHimself(erlang) && !npcErlang.isHimself(nezha), "Erlang knows himself");
        check(h, npcNezha.isHimself(nezha) && !npcNezha.isHimself(erlang), "Nezha knows himself");
        check(h, !npcErlang.canAttack(erlang) && npcErlang.canAttack(bull), "Erlang never takes himself for a foe");
        npcErlang.setTarget(erlang);
        check(h, npcErlang.getTarget() == null, "not even when told to");
        float before = npcErlang.getHealth();
        check(h, !npcErlang.hurt(h.getLevel().damageSources().playerAttack(erlang), 50f), "his own self's blow passes through him");
        eq(h, before, npcErlang.getHealth(), "unhurt");
        check(h, npcNezha.hurt(h.getLevel().damageSources().playerAttack(erlang), 1f), "but Nezha is no Erlang: he takes the blow");
        Morality.set(erlang, Morality.OUTLAW);
        check(h, !npcErlang.isFoe(erlang), "an outlawed Erlang still: his blows pass over his own self");
        check(h, npcNezha.isFoe(erlang), "Nezha strikes the outlaw");
        h.succeed();
    }

    // ── The court, the spawner, the pass ────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void heavensCourtKeepsAPostForEachInItsRegion(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        var places = HeavenCourt.places(seed);
        eq(h, HeavenCourt.SEATS.size(), places.size(), "a place for every post");
        Set<String> spots = new HashSet<>();
        for (HeavenCourt.Seat s : HeavenCourt.SEATS) {
            HeavenCourt.Place p = places.get(s.id());
            check(h, spots.add(p.x() + "," + p.z()), s.id() + " a place of its own");
            Zones.Zone region = Zones.HEAVEN_REGIONS.get(s.region());
            int road = HeavenRegions.roadLevel(seed, p.x(), p.z());
            check(h, road >= region.minLevel() && road <= region.maxLevel(), s.id() + " in its region " + region.id() + ": " + road);
            if (HeavenRegistry.STAR_LORD.get().equals(s.kind().get()) || !(s.kind().get().create(h.getLevel()) instanceof HeavenBoss)) {
                int lv = HeavenRegions.minibossLevel(road);
                check(h, lv <= region.maxLevel() + 1, s.id() + " a miniboss at most one over its band: " + lv);
            }
        }
        BlockPos palace = HeavenRegions.palace(seed);
        eq(h, palace.getX(), places.get("li_jing").x(), "Li Jing before the palace's gate");
        eq(h, 4L, HeavenCourt.SEATS.stream().filter(s -> s.id().startsWith("king_")).count(), "four Kings at four gates");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 400)
    public static void heavensCourtSeatsItsHoldersInHeaven(GameTestHelper h) {
        var heaven = h.getLevel().getServer().getLevel(com.jttw.engine.dimension.JTTWDimensions.HEAVEN);
        check(h, heaven != null, "Heaven is loaded");
        for (String id : List.of("rahu", "king_chiguo", "li_jing")) {
            HeavenCourt.Seat seat = HeavenCourt.seat(id);
            HeavenCourt.Place place = HeavenCourt.place(heaven.getSeed(), id);
            heaven.getChunk(place.x() >> 4, place.z() >> 4);
            Mob m = HeavenCourt.put(heaven, seat);
            try {
                check(h, m != null && m.isPersistenceRequired(), id + " put, and kept");
                check(h, id.equals(m.getPersistentData().getString(HeavenCourt.KEY)), id + " knows his post");
                check(h, heaven.getBlockState(m.blockPosition().below()).isSolid(), id + " stands on ground: " + m.blockPosition());
                Zones.Zone region = Zones.HEAVEN_REGIONS.get(seat.region());
                if (m instanceof HeavenMonster hm) {
                    check(h, hm.isMiniboss() && hm.mobLevel() <= region.maxLevel() + 1 && hm.mobLevel() >= region.minLevel(), id + " a miniboss of his region: " + hm.mobLevel());
                    eq(h, seat.variant(), hm.variant(), id + " his own star");
                } else check(h, m instanceof HeavenBoss b && b.zone() == region, id + " a great name of his region");
            } finally {
                if (m != null) m.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void heavensWildHostComesDownInBandsOfTheRoadsLevel(GameTestHelper h) {
        RandomSource r = RandomSource.create(11);
        BlockPos at = h.absolutePos(new BlockPos(2, 2, 2));
        int road = HeavenRegions.roadLevel(h.getLevel().getSeed(), at.getX(), at.getZ());
        Zones.Zone region = HeavenRegions.regionOf(road);
        List<HeavenMonster> all = new ArrayList<>();
        onEnd(h, () -> all.forEach(Entity::discard));
        int led = 0;
        for (int i = 0; i < 40; i++) {
            List<HeavenMonster> band = HeavenSpawner.spawnBand(h.getLevel(), at, r);
            all.addAll(band);
            check(h, band.size() >= HeavenSpawner.LEAST && band.size() <= HeavenSpawner.MOST + 1, "a band of two to five: " + band.size());
            for (HeavenMonster m : band) {
                check(h, !m.isPersistenceRequired() && m.hasRestriction(), "wild, kept to where it came down");
                if (m.isCaptain()) {
                    led++;
                    check(h, m.mobLevel() <= region.maxLevel() + 1 && m.mobLevel() >= Math.min(region.maxLevel() + 1, road + 3), "a captain over the road: " + m.mobLevel());
                } else check(h, Math.abs(m.mobLevel() - road) <= 1 || m.mobLevel() == region.minLevel() || m.mobLevel() == region.maxLevel(), "a regular of the road's level: " + m.mobLevel());
            }
            all.forEach(Entity::discard);
        }
        check(h, led > 0 && led < 40, "some bands led by a captain, most not: " + led);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thePassTakesEachToTheCloudsOfTheirLevel(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        for (int lv : new int[]{1, 5, 20, 35, 50, 65, 80, 95, 100}) {
            var island = HeavenGate.islandFor(seed, lv, RandomSource.create(lv));
            if (island == null) continue; // none near enough: a cloud is laid on the ring of their level
            int road = HeavenRegions.roadLevel(seed, island.x(), island.z());
            check(h, Math.abs(road - lv) <= HeavenGate.NEAR_ENOUGH, "its road of their level: " + lv + " " + road);
            if (lv <= 80) check(h, Math.abs(road - lv) <= HeavenGate.WITHIN, "out from the palace, where islands are many, within two: " + lv + " " + road);
        }
        check(h, HeavenRegistry.HEAVEN_PASS.get() instanceof com.jttw.engine.item.HeavenPassItem, "the pass");
        h.succeed();
    }
}
