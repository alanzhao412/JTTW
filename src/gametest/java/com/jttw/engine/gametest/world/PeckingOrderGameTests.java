package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.entity.hell.ImpermanenceEntity;
import com.jttw.engine.entity.wild.WildYaoguai;
import com.jttw.engine.entity.wuzhuang.TempleYouthEntity;
import com.jttw.engine.entity.zone.ZoneBoss;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.world.cave.CaveHousehold;
import com.jttw.engine.world.feature.LionCamelCityFeature;
import com.jttw.engine.world.ridge.CaveGarrison;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.jttw.engine.gametest.GT.*;

/**
 * The pecking order of the mod's creatures, the road's: regulars its wild creatures, captains its
 * trainers, minibosses its elite trainers, a boss its gym's leader. By how hard each hits (a share of
 * a same-level character's health): every regular mob's hardest single hit at most
 * {@link ZoneMonster#MOB_HIT_CAP}, every captain's at most that times a captain's measure, every
 * miniboss's between a captain's and a boss's, every boss's blow over them all (and its special hits,
 * {@link ZoneBoss#specialShares}, no harder than its blow may be). By how much it takes to kill: a
 * regular 2 to 12 ordinary hits, a captain twice its kind's, a miniboss
 * {@link ZoneMonster#MINIBOSS_LEAST_HITS} to {@link ZoneMonster#MINIBOSS_MOST_HITS}, a boss
 * {@link ZoneBoss#LEAST_HITS} to {@link ZoneBoss#MOST_HITS} of the character it is built for (its
 * band's top), all its lives together more than any miniboss. Every kind there is is measured, so a
 * new one out of its place fails here. Each is made and measured off the world, never let loose in it.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PeckingOrderGameTests {

    /** The most a captain's hardest hit may be. */
    public static final double CAPTAIN_CAP = 0.145;
    /** A miniboss's blow, at least and at most (its hardest hit is {@link ZoneMonster#MINIBOSS_HIT}); and the least and most any boss's. */
    public static final double MINIBOSS_LEAST = 0.14, MINIBOSS_MOST = ZoneMonster.MINIBOSS_HIT, BOSS_LEAST = ZoneBoss.LEAST_BLOW, BOSS_MOST = ZoneBoss.MOST_BLOW;
    /** The most a lair makes a miniboss's health over a captain's. */
    public static final double MINIBOSS_OVER_CAPTAIN = Math.max(1, Math.max(Math.max(CaveHousehold.GUARDIAN_HEALTH,
            Math.max(CaveGarrison.GUARDIAN_HEALTH, LionCamelCityFeature.WARDEN_HEALTH)), Math.max(com.jttw.engine.entity.spider.SpiderSpiritEntity.BOWER_HEALTH,
            com.jttw.engine.world.flowerfruit.CavePlan.PILGRIM_HEALTH)));
    /** The least and the most ordinary hits a regular mob takes to kill. */
    public static final double MOB_HITS_LEAST = 2, MOB_HITS_MOST = 12;
    private static final double EPS = 1e-6, HITS_EPS = 1e-3;

    /** The kinds a lair makes its floor wardens, gate keepers and guardians of: each must land a miniboss's blow. */
    private static final Set<EntityType<?>> MINIBOSS_KINDS = Set.of(ModEntities.BONE_SERVANT.get(), ModEntities.WRAITH.get(),
            ModEntities.MOUSE_SPIRIT.get(), ModEntities.FLOWER_MAID.get(), ModEntities.FURNACE_BRUTE.get(), ModEntities.STRONG_GENERAL.get(),
            ModEntities.LION_DEMON.get(), ModEntities.ELEPHANT_DEMON.get(), ModEntities.PENG_DEMON.get(),
            com.jttw.engine.registry.BlackWindRegistry.BEAR_SOLDIER.get(), com.jttw.engine.registry.BlackWindRegistry.LINGXUZI.get(),
            com.jttw.engine.registry.BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(),
            com.jttw.engine.registry.FlowerFruitRegistry.FAKE_PILGRIM.get(),
            com.jttw.engine.registry.SpiderCaveRegistry.SPIDER_SPIRIT.get(),
            com.jttw.engine.registry.HeavenRegistry.STAR_LORD.get(), com.jttw.engine.registry.HeavenRegistry.GREAT_SPIRIT_GIANT.get(),
            com.jttw.engine.registry.HeavenRegistry.THUNDER_LORD.get(), com.jttw.engine.registry.HeavenRegistry.LIGHTNING_MOTHER.get(),
            com.jttw.engine.registry.HeavenRegistry.THOUSAND_MILE_EYE.get(), com.jttw.engine.registry.HeavenRegistry.WIND_FOLLOWING_EAR.get(),
            ModEntities.HEAVENLY_SOLDIER.get());

    /**
     * The one superboss, past every rule a boss keeps but the least: the Six-Eared Macaque, level 120,
     * an optional fight for a group of capped characters, the strongest boss there is (the legendary,
     * not a gym's leader). Up to this many hits (of a character of the level he is measured at, the
     * cap), all his lives together; his blow up to {@link #SUPERBOSS_BLOW_MOST}.
     */
    public static final double SUPERBOSS_HITS_MOST = 460, SUPERBOSS_BLOW_MOST = 0.33;

    private static boolean superboss(Entity e) {
        return e instanceof com.jttw.engine.entity.flowerfruit.SixEaredMacaqueEntity;
    }

    private static List<Entity> every(GameTestHelper h) {
        List<Entity> out = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!JTTWEngine.MOD_ID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace())) continue;
            Entity e = type.create(h.getLevel());
            if (e != null) out.add(e);
        }
        return out;
    }

    /** Its attack now, as a share of a same-level character's health. */
    private static double share(LivingEntity e, int level) {
        return e.getAttributeValue(Attributes.ATTACK_DAMAGE) / MobScaling.typicalHealth(level);
    }

    private static String name(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    /** Its health now, in a same-level character's ordinary hits. */
    private static double hits(LivingEntity e, int level) {
        return e.getMaxHealth() / MobScaling.typicalHit(level);
    }

    /** Its hits to kill, all its lives together, of a character of {@code level}. */
    private static double allHits(ZoneBoss b, int level) {
        double all = 0;
        for (double life : b.lives()) all += hits(b, level) * life;
        return all;
    }

    /** Its main blow as a share of a character of {@code level}'s health (Lady Earth Flow's stroke two cuts, the second hard on the first). */
    private static double blow(ZoneBoss b, int level) {
        return share(b, level) * (b instanceof LadyEarthFlowEntity ? 2 : 1);
    }

    @GameTest(template = PLATFORM)
    public static void aBossIsAGymLeaderAndOutlastsAnyMiniboss(GameTestHelper h) {
        // the toughest miniboss there is, in hits: any kind a lair could make one of, at the most a lair gives one, and the temple youths
        double miniboss = 0;
        String toughest = "";
        List<ZoneBoss> bosses = new ArrayList<>();
        for (Entity e : every(h)) {
            double m = -1;
            if (e instanceof ZoneMonster z) {
                z.becomeMiniboss(z.captainLevel(), MINIBOSS_OVER_CAPTAIN);
                m = hits(z, z.captainLevel());
                check(h, m >= ZoneMonster.MINIBOSS_LEAST_HITS - HITS_EPS && m <= ZoneMonster.MINIBOSS_MOST_HITS + HITS_EPS,
                        name(e) + " a miniboss's hits, an elite trainer's: " + m);
                z.becomeMiniboss(z.captainLevel(), 1);
                double least = hits(z, z.captainLevel());
                check(h, least >= ZoneMonster.MINIBOSS_LEAST_HITS - HITS_EPS, name(e) + " never under a miniboss's least: " + least);
            } else if (e instanceof TempleYouthEntity y) {
                m = hits(y, y.mobLevel());
                check(h, m >= ZoneMonster.MINIBOSS_LEAST_HITS - HITS_EPS && m <= ZoneMonster.MINIBOSS_MOST_HITS + HITS_EPS, name(e) + " a miniboss's hits: " + m);
            } else if (e instanceof ZoneBoss b) {
                bosses.add(b);
                continue;
            }
            if (m > miniboss) {
                miniboss = m;
                toughest = name(e);
            }
            e.discard();
        }
        check(h, miniboss >= ZoneMonster.MINIBOSS_LEAST_HITS && miniboss <= ZoneMonster.MINIBOSS_MOST_HITS + HITS_EPS,
                "the minibosses measured: the toughest " + toughest + ", " + miniboss);
        for (ZoneBoss b : bosses) {
            double all = allHits(b, b.measuredLevel());
            if (superboss(b)) {
                check(h, all >= ZoneBoss.LEAST_HITS - EPS && all <= SUPERBOSS_HITS_MOST, name(b) + " the superboss's hits: " + all);
            } else {
                check(h, all >= ZoneBoss.LEAST_HITS - EPS && all <= ZoneBoss.MOST_HITS + EPS, name(b) + " a gym leader's hits: " + all);
            }
            check(h, all > miniboss, name(b) + ", all its lives, outlasts any miniboss (" + toughest + ", " + miniboss + "): " + all);
            b.discard();
        }
        check(h, bosses.size() >= 15, "every boss measured: " + bosses.size());
        h.succeed();
    }

    /**
     * A boss is a gym's leader: one character of its zone's band's top level beats it alone, in a hard
     * fight. Measured against that character: {@link ZoneBoss#LEAST_HITS} to {@link ZoneBoss#MOST_HITS}
     * of their ordinary hits all its lives together; its blow {@link ZoneBoss#LEAST_BLOW} to
     * {@link ZoneBoss#MOST_BLOW} of their health, so it kills them in five or six; no special harder.
     */
    @GameTest(template = PLATFORM)
    public static void aBossIsSoloableAtItsZonesTopLevel(GameTestHelper h) {
        int bosses = 0;
        for (Entity e : every(h)) {
            try {
                if (!(e instanceof ZoneBoss b) || superboss(b) || b instanceof TempleYouthEntity) continue;
                bosses++;
                int top = Math.min(b.zone().maxLevel(), com.jttw.engine.progression.level.LevelConstants.MAX_LEVEL);
                eq(h, top, ZoneBoss.challengerOf(b.zone()), name(b) + " built for its band's top");
                double all = allHits(b, top), blow = blow(b, top);
                check(h, all >= ZoneBoss.LEAST_HITS - EPS && all <= ZoneBoss.MOST_HITS + EPS,
                        name(b) + " takes 60-80 of a level-" + top + " character's hits: " + all);
                check(h, blow >= BOSS_LEAST - EPS && blow <= BOSS_MOST + EPS, name(b) + "'s blow, of a level-" + top + " character's health: " + blow);
                int kills = (int) Math.ceil(1 / blow - EPS);
                check(h, kills >= 5 && kills <= 6, name(b) + " kills a level-" + top + " character in five or six blows: " + kills);
                for (double special : b.specialShares()) {
                    double lands = b.hitOf(special) / MobScaling.typicalHealth(top);
                    check(h, lands <= BOSS_MOST + EPS, name(b) + "'s special no harder than a boss's blow: " + lands);
                }
            } finally {
                e.discard();
            }
        }
        check(h, bosses >= 15, "every gym leader measured: " + bosses);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSixEaredMacaqueIsTheStrongestBossThereIs(GameTestHelper h) {
        var macaque = com.jttw.engine.registry.FlowerFruitRegistry.SIX_EARED_MACAQUE.get().create(h.getLevel());
        check(h, macaque != null, "made");
        int lv = macaque.mobLevel();
        eq(h, 120, lv, "level 120, past the players' cap");
        check(h, macaque.getDisplayName().getString().contains("Lv 120"), macaque.getDisplayName().getString());
        // measured against a capped character: a group of them beats him, and his blow does not one-shot them
        eq(h, 100, macaque.measuredLevel(), "measured at the cap");
        double his = hits(macaque, 100), blow = share(macaque, 100);
        check(h, his >= 400 && his <= SUPERBOSS_HITS_MOST, "some 400-450 hits of a level-100 character: " + his);
        check(h, blow >= SUPERBOSS_BLOW_MOST - 0.005 && blow <= SUPERBOSS_BLOW_MOST + EPS, "his blow a third of a capped character's health: " + blow);
        int others = 0;
        for (Entity e : every(h)) {
            try {
                if (!(e instanceof ZoneBoss b) || superboss(b) || b instanceof TempleYouthEntity) continue;
                others++;
                double all = allHits(b, b.measuredLevel());
                check(h, his > all, "more hits than " + name(b) + " (" + all + "): " + his);
                check(h, macaque.getAttributeValue(Attributes.ATTACK_DAMAGE) >= b.getAttributeValue(Attributes.ATTACK_DAMAGE) - EPS || b instanceof LadyEarthFlowEntity,
                        "a blow no lighter than " + name(b) + "'s");
                check(h, lv > b.mobLevel(), "higher than " + name(b));
                // and in plain health, far over any: he is the strongest
                check(h, macaque.getMaxHealth() > b.getMaxHealth() * java.util.Arrays.stream(b.lives()).sum(), "more health than " + name(b));
            } finally {
                e.discard();
            }
        }
        check(h, others >= 15, "weighed against every other boss: " + others);
        macaque.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void bossesPastTheCapAreMeasuredAtTheCap(GameTestHelper h) {
        // a creature's own level up to the cap, the cap past it
        for (int lv = 1; lv < 130; lv++) eq(h, Math.min(lv, 100), ZoneBoss.measuredAt(lv), "measured at its own level, up to the cap: " + lv);
        eq(h, 100, ZoneBoss.challengerOf(com.jttw.engine.world.Zones.LION_CAMEL), "Lion-Camel's kings built for a capped character");
        eq(h, 100, ZoneBoss.measuredAt(120), "the Macaque, a capped group's");
        // no boss at all one-shots a capped player: its blow and every special at most a boss's hardest (the superboss's, his)
        int bosses = 0, pastTheCap = 0;
        for (Entity e : every(h)) {
            try {
                if (!(e instanceof ZoneBoss b) || b instanceof TempleYouthEntity) continue;
                bosses++;
                if (b.mobLevel() > 100) {
                    pastTheCap++;
                    eq(h, 100, b.measuredLevel(), name(b) + " (Lv " + b.mobLevel() + ") measured at the cap");
                }
                double most = b.getAttributeValue(Attributes.ATTACK_DAMAGE) * (b instanceof LadyEarthFlowEntity ? 2 : 1);
                for (double special : b.specialShares()) most = Math.max(most, b.hitOf(special));
                double cap = superboss(b) ? SUPERBOSS_BLOW_MOST : BOSS_MOST;
                check(h, most <= cap * MobScaling.typicalHealth(100) + EPS, name(b) + " no one-shot of a capped player: " + most / MobScaling.typicalHealth(100));
            } finally {
                e.discard();
            }
        }
        check(h, bosses >= 15, "every boss weighed: " + bosses);
        check(h, pastTheCap >= 3, "the Lion-Camel kings and the Macaque past the cap: " + pastTheCap);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void bossesStandAboveEverythingElseOfTheirZones(GameTestHelper h) {
        // the highest level anything of a zone but its bosses comes at: its band's top, its captains and minibosses, the temple youths
        java.util.Map<String, Integer> minions = new java.util.HashMap<>(), head = new java.util.HashMap<>();
        java.util.Map<String, com.jttw.engine.world.Zones.Zone> zones = new java.util.HashMap<>();
        List<ZoneBoss> bosses = new ArrayList<>();
        for (Entity e : every(h)) {
            if (e instanceof TempleYouthEntity y) minions.merge(y.zone().id(), y.mobLevel(), Math::max);
            else if (e instanceof ZoneBoss b) bosses.add(b);
            else if (e instanceof com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity) { /* the superboss's hair: of his level, not the band's */ }
            else if (e instanceof ZoneMonster m) {
                minions.merge(m.zone().id(), Math.max(m.zone().maxLevel(), m.captainLevel()), Math::max);
                eq(h, m.zone().maxLevel() + ZoneMonster.CAPTAIN_ABOVE_BAND, m.topLevel(), name(m) + "'s top: its band's and one");
            }
            if (!(e instanceof ZoneBoss) || e instanceof TempleYouthEntity) e.discard();
        }
        // and the posts of every fortress and garrison, at whatever level each is put there
        String ridge = com.jttw.engine.world.Zones.WHITE_BONE.id(), cave = com.jttw.engine.world.Zones.BOTTOMLESS.id(),
                fire = com.jttw.engine.world.Zones.FIRE_MOUNTAIN.id(), lion = com.jttw.engine.world.Zones.LION_CAMEL.id(),
                wind = com.jttw.engine.world.Zones.BLACK_WIND.id(), silk = com.jttw.engine.world.Zones.SPIDER_CAVE.id(),
                island = com.jttw.engine.world.Zones.FLOWER_FRUIT.id();
        var bone = com.jttw.engine.world.ridge.WhiteBoneCave.of(h.getLevel().getSeed());
        for (int k = 0; k <= com.jttw.engine.world.ridge.WhiteBoneCave.FLOORS; k++) {
            for (var post : bone.posts(k)) minions.merge(ridge, post.level(), Math::max);
        }
        for (var post : bone.outside()) minions.merge(ridge, post.level(), Math::max);
        for (int k = -1; k < com.jttw.engine.world.feature.Labyrinth.LEVELS; k++) {
            // a household's are put a level either side of their floor's, never past the band's top and one (CaveHousehold.put)
            minions.merge(cave, Math.min(com.jttw.engine.entity.cave.CaveMonster.TOP, com.jttw.engine.entity.cave.CaveMonster.captainOn(k) + 1), Math::max);
            minions.merge(cave, Math.min(com.jttw.engine.world.Zones.BOTTOMLESS.maxLevel(), com.jttw.engine.entity.cave.CaveMonster.regularOn(k) + 1), Math::max);
            if (k >= 0) minions.merge(cave, com.jttw.engine.entity.cave.CaveMonster.guardianOn(k), Math::max);
        }
        for (int f = 0; f <= com.jttw.engine.world.fire.FortressPlan.FLOORS + 1; f++) {
            minions.merge(fire, com.jttw.engine.world.fire.FortressPlan.regularLevel(f), Math::max);
            minions.merge(fire, com.jttw.engine.world.fire.FortressPlan.captainLevel(f), Math::max);
            minions.merge(fire, com.jttw.engine.world.fire.FortressPlan.guardianLevel(f), Math::max);
        }
        for (int k = 0; k <= com.jttw.engine.world.lion.LionFortress.FLOORS; k++) {
            for (int role : new int[]{com.jttw.engine.world.lion.LionFortress.REGULAR, com.jttw.engine.world.lion.LionFortress.CAPTAIN,
                    com.jttw.engine.world.lion.LionFortress.WARDEN}) {
                minions.merge(lion, com.jttw.engine.world.lion.LionFortress.levelOf(k, role), Math::max);
            }
        }
        for (int floor = 0; floor <= 1; floor++) {
            // a floor's regulars are put a level either side of its own, never past the band (BlackWindCave)
            minions.merge(wind, Math.min(com.jttw.engine.world.Zones.BLACK_WIND.maxLevel(), com.jttw.engine.world.blackwind.BlackWindPlan.regularLevel(floor) + 1), Math::max);
            minions.merge(wind, com.jttw.engine.world.blackwind.BlackWindPlan.captainLevel(floor), Math::max);
            minions.merge(wind, com.jttw.engine.world.blackwind.BlackWindPlan.guardLevel(floor), Math::max);
        }
        for (var post : com.jttw.engine.world.spider.SpiderCavePlan.POSTS) minions.merge(silk, post.level(), Math::max);
        for (var post : com.jttw.engine.world.flowerfruit.CavePlan.POSTS) minions.merge(island, post.level(), Math::max);
        for (var z : List.of(com.jttw.engine.world.Zones.WHITE_BONE, com.jttw.engine.world.Zones.BOTTOMLESS, com.jttw.engine.world.Zones.FIRE_MOUNTAIN,
                com.jttw.engine.world.Zones.LION_CAMEL, com.jttw.engine.world.Zones.BLACK_WIND, com.jttw.engine.world.Zones.SPIDER_CAVE,
                com.jttw.engine.world.Zones.FLOWER_FRUIT)) {
            check(h, minions.get(z.id()) <= z.maxLevel() + ZoneMonster.CAPTAIN_ABOVE_BAND,
                    z.id() + "'s posts within its band (a captain or a miniboss at most one over): up to " + minions.get(z.id()));
        }
        for (ZoneBoss b : bosses) {
            com.jttw.engine.world.Zones.Zone z = b.zone();
            if (superboss(b)) {
                // the one superboss stands past every band and every head boss (theSixEaredMacaqueIsTheStrongestBossThereIs)
                check(h, b.mobLevel() > ZoneBoss.levelOver(z), name(b) + " far past his zone's head boss's level: " + b.mobLevel());
                b.discard();
                continue;
            }
            zones.put(z.id(), z);
            int top = Math.max(z.maxLevel(), minions.getOrDefault(z.id(), 0));
            check(h, b.mobLevel() > top, name(b) + " (Lv " + b.mobLevel() + ") above everything else of its zone (Lv " + top + ")");
            check(h, b.mobLevel() <= ZoneBoss.levelOver(z), name(b) + " no higher than its zone's head: " + b.mobLevel());
            check(h, b.mobLevel() >= ZoneBoss.lesserLevelOver(z), name(b) + " a lesser boss at least its band's top and two: " + b.mobLevel());
            // and its blow, struck at the character it is built for, harder than any miniboss of its zone could land on them
            double minibossHardest = ZoneMonster.MINIBOSS_HIT * MobScaling.typicalHealth(Math.min(100, z.maxLevel() + ZoneMonster.CAPTAIN_ABOVE_BAND));
            check(h, blow(b, b.measuredLevel()) * MobScaling.typicalHealth(b.measuredLevel()) > minibossHardest,
                    name(b) + "'s blow over any miniboss's of its zone");
            head.merge(z.id(), b.mobLevel(), Math::max);
            b.discard();
        }
        for (var entry : head.entrySet()) {
            com.jttw.engine.world.Zones.Zone z = zones.get(entry.getKey());
            eq(h, ZoneBoss.levelOver(z), entry.getValue(), z.id() + "'s head boss: its band's top and " + ZoneBoss.LEVELS_ABOVE_BAND);
            eq(h, z.maxLevel() + 3, entry.getValue(), z.id() + "'s head boss three over its band");
        }
        eq(h, 102, com.jttw.engine.entity.lion.WhiteElephantEntity.LEVEL, "the White Elephant one under the Azure Lion");
        for (int court = 1; court <= 10; court++) {
            eq(h, court == 5 ? 69 : 68, com.jttw.engine.entity.hell.KingOfHellEntity.levelOf(court), "the king of the court " + court);
        }
        check(h, head.size() >= 7, "every zone's bosses measured: " + head.keySet());
        h.succeed();
    }

    /**
     * A regular is a wild creature of its zone's road: its level is fixed where it is put, the band's
     * bottom at the zone's outskirts and higher further in (its region's middle), and the lairs carry the
     * road on, deeper ever higher, to the band's top. Sampled across a region for every kind, off the
     * world (only where it stands and the world's seed weigh: who is near does not).
     */
    @GameTest(template = PLATFORM)
    public static void regularsRollAcrossTheirBand(GameTestHelper h) {
        long seed = h.getLevel().getSeed();
        net.minecraft.util.RandomSource r = net.minecraft.util.RandomSource.create(7);
        int kinds = 0;
        for (Entity e : every(h)) {
            try {
                if (!(e instanceof ZoneMonster m) || m.isMiniboss() || e instanceof com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity) continue;
                com.jttw.engine.world.Zones.Zone z = m.zone();
                if (!z.hasFootprint() || m instanceof com.jttw.engine.entity.blackwind.BlackWindChief) continue;
                int[] c = z.centre(seed, 0, 0);
                // walk out from its region's middle to past its edge, rolling at each step
                int least = Integer.MAX_VALUE, most = Integer.MIN_VALUE, atHeart = 0, atEdge = 0;
                java.util.Set<Integer> seen = new java.util.HashSet<>();
                for (int d = 0; d <= 700; d += 5) {
                    m.setPos(c[0] + d, 300, c[1]); // over any cave's floors: the land's road
                    int lv = m.rollRegularLevel(r);
                    check(h, lv >= z.minLevel() && lv <= z.maxLevel(), name(m) + " within its band: " + lv);
                    seen.add(lv);
                    least = Math.min(least, lv);
                    most = Math.max(most, lv);
                    if (d == 0) atHeart = lv;
                    if (z.depth(seed, c[0] + d, c[1]) <= 0) atEdge = lv;
                }
                check(h, least <= Math.max(z.minLevel(), m.regularLevel()) + 1, name(m) + " at its band's bottom at the outskirts: " + least);
                check(h, atEdge <= Math.max(z.minLevel(), m.regularLevel()) + 1, name(m) + " out past the edge, the outskirts': " + atEdge);
                check(h, atHeart >= m.regularLevel() + 2 || m.regularLevel() >= z.maxLevel() - 3, name(m) + " higher at the heart: " + atHeart);
                check(h, seen.size() >= 4 || m.regularLevel() >= z.maxLevel() - 3, name(m) + " of many levels across its region: " + seen);
                // a captain three to five over the regulars round it, never past its top
                for (int d = 0; d <= 400; d += 40) {
                    m.setPos(c[0] + d, 300, c[1]);
                    int road = m.routeLevel(), cl = m.rollCaptainLevel(r);
                    check(h, cl <= m.topLevel() && (cl >= Math.max(road, m.regularLevel()) + 3 || cl == m.topLevel()),
                            name(m) + " captain three to five over the road's " + road + ": " + cl);
                }
                kinds++;
            } finally {
                e.discard();
            }
        }
        check(h, kinds >= 20, "every kind sampled: " + kinds);
        // Hell's road is its courts, the First to the Tenth
        var yin = ModEntities.YIN_SOLDIER.get().create(h.getLevel());
        var first = com.jttw.engine.world.hell.TenCourts.court(1);
        var tenth = com.jttw.engine.world.hell.TenCourts.court(10);
        yin.setPos(first.x(), 64, first.z());
        eq(h, 57, yin.routeLevel(), "the First Court's");
        yin.setPos(tenth.x(), 64, tenth.z());
        eq(h, 66, yin.routeLevel(), "the Tenth Court's: Hell's band's top");
        yin.discard();
        // and the lairs carry each zone's road on to its band's top, deeper ever higher
        eq(h, 36, com.jttw.engine.world.ridge.WhiteBoneCave.floorLevel(com.jttw.engine.world.ridge.WhiteBoneCave.FLOORS - 1), "the White Bone Cave's last floor");
        eq(h, 36, com.jttw.engine.world.blackwind.BlackWindPlan.regularLevel(1), "the Black Wind Court");
        eq(h, 30, com.jttw.engine.world.flowerfruit.CavePlan.POSTS.stream().filter(p -> !p.captain()).mapToInt(com.jttw.engine.world.flowerfruit.CavePlan.Post::level).max().orElse(0),
                "the Water Curtain Cave's hall");
        eq(h, 46, com.jttw.engine.entity.cave.CaveMonster.regularOn(com.jttw.engine.world.feature.Labyrinth.FLOORS - 1), "the Bottomless Cave's last floor");
        eq(h, 77, com.jttw.engine.world.fire.FortressPlan.regularLevel(8), "the Fire Cloud Cave's eighth floor");
        eq(h, 88, com.jttw.engine.world.spider.SpiderCavePlan.bowerLevel(com.jttw.engine.world.spider.SpiderCavePlan.BOWERS.size() - 1), "the last bower");
        eq(h, 100, com.jttw.engine.world.lion.LionFortress.levelOf(com.jttw.engine.world.lion.LionFortress.FLOORS, com.jttw.engine.world.lion.LionFortress.REGULAR),
                "the Lion-Camel Cave's last floor");
        for (int k = 0; k + 1 < com.jttw.engine.world.ridge.WhiteBoneCave.FLOORS; k++) {
            check(h, com.jttw.engine.world.ridge.WhiteBoneCave.floorLevel(k + 1) >= com.jttw.engine.world.ridge.WhiteBoneCave.floorLevel(k), "deeper, never lower");
            check(h, com.jttw.engine.entity.cave.CaveMonster.regularOn(k + 1) >= com.jttw.engine.entity.cave.CaveMonster.regularOn(k), "deeper, never lower");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void mobsHitFarLessThanTheirBosses(GameTestHelper h) {
        int zone = 0, wild = 0, bosses = 0;
        for (Entity e : every(h)) {
            try {
                if (e instanceof ZoneMonster m) {
                    zone++;
                    String n = name(m);
                    boolean general = m instanceof StrongGeneralEntity || m instanceof com.jttw.engine.entity.blackwind.BlackWindChief
                            || m instanceof com.jttw.engine.entity.spider.SpiderSpiritEntity
                            || m instanceof com.jttw.engine.entity.heaven.HeavenMonster hm && hm.alwaysMiniboss(); // always minibosses
                    if (!general) {
                        // a regular: its blow and its hardest hit (its specials too) within a mob's; its health a mob's
                        int lv = m.regularLevel();
                        m.become(lv, false);
                        check(h, share(m, lv) <= ZoneMonster.MOB_HIT_CAP + EPS, n + "'s blow: " + share(m, lv));
                        check(h, m.hardestShare() <= ZoneMonster.MOB_HIT_CAP + EPS, n + "'s hardest hit: " + m.hardestShare());
                        double regular = m.healthAt(lv, false) / MobScaling.typicalHit(lv);
                        boolean hair = m instanceof com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity; // the superboss's hair, of his level and measure
                        check(h, (hair || regular >= MOB_HITS_LEAST - EPS) && regular <= MOB_HITS_MOST + EPS, n + "'s hits to kill: " + regular);
                        // a captain's: a trainer's, twice its kind's hits, a little harder a blow
                        int cl = m.captainLevel();
                        m.become(cl, true);
                        check(h, share(m, cl) <= CAPTAIN_CAP + EPS, n + " captain's blow: " + share(m, cl));
                        check(h, m.hardestShare() * m.damageScale() <= CAPTAIN_CAP + EPS, n + " captain's hardest hit");
                        double captain = hits(m, cl);
                        check(h, hair || captain > regular && captain < ZoneMonster.MINIBOSS_LEAST_HITS, n + " captain's hits, over a regular's, under any miniboss's: " + captain);
                        if (m instanceof ImpermanenceEntity imp) {
                            imp.enrage();
                            check(h, share(m, cl) <= CAPTAIN_CAP + EPS, n + " captain's blow enraged: " + share(m, cl));
                        }
                    }
                    // a miniboss's: its hardest a miniboss's, and its blow one for the kinds the lairs make minibosses of
                    int ml = m.captainLevel();
                    m.becomeMiniboss(ml, 1);
                    check(h, m.isCaptain() && m.isMiniboss(), n + " a miniboss");
                    check(h, m.hardestShare() * m.damageScale() <= MINIBOSS_MOST + EPS, n + " miniboss's hardest hit");
                    check(h, m.hardestShare() * m.damageScale() > CAPTAIN_CAP, n + " miniboss's hardest hit over any captain's");
                    if (MINIBOSS_KINDS.contains(m.getType())) {
                        double blow = share(m, ml);
                        check(h, blow >= MINIBOSS_LEAST - EPS && blow <= MINIBOSS_MOST + EPS, n + " miniboss's blow: " + blow);
                    }
                    if (general) {
                        // whoever brings him, a miniboss
                        m.become(m.captainLevel(), false);
                        check(h, m.isCaptain() && m.isMiniboss() && share(m, m.mobLevel()) > CAPTAIN_CAP - 0.01, n + " always a miniboss");
                        double hits = hits(m, m.mobLevel());
                        check(h, hits >= ZoneMonster.MINIBOSS_LEAST_HITS - HITS_EPS && hits <= ZoneMonster.MINIBOSS_MOST_HITS + HITS_EPS, n + " a miniboss's hits: " + hits);
                    }
                } else if (e instanceof WildYaoguai y) {
                    wild++;
                    y.become(y.maxLevel());
                    check(h, share(y, y.maxLevel()) <= ZoneMonster.MOB_HIT_CAP + EPS, name(y) + "'s blow");
                    check(h, y.hardestShare() <= ZoneMonster.MOB_HIT_CAP + EPS, name(y) + "'s hardest hit");
                    check(h, y.healthAt(y.maxLevel()) / MobScaling.typicalHit(y.maxLevel()) <= MOB_HITS_MOST + EPS, name(y) + "'s hits to kill");
                } else if (e instanceof ZoneBoss b) {
                    double blow = share(b, b.measuredLevel());
                    if (b instanceof TempleYouthEntity) {
                        // Clear Wind and Bright Moon are minibosses
                        check(h, blow >= MINIBOSS_LEAST - EPS && blow <= MINIBOSS_MOST + EPS, name(b) + "'s blow, a miniboss's: " + blow);
                        continue;
                    }
                    bosses++;
                    if (b instanceof LadyEarthFlowEntity) blow *= 2; // her stroke is two cuts, the second hard on the first
                    double least = superboss(b) ? SUPERBOSS_BLOW_MOST - 0.005 : BOSS_LEAST, most = superboss(b) ? SUPERBOSS_BLOW_MOST : BOSS_MOST;
                    check(h, blow >= least - EPS && blow <= most + EPS, name(b) + "'s blow, a boss's: " + blow);
                    // its special hits (the blows that fall on all near it) no harder than a boss's blow, and as they land
                    for (double special : b.specialShares()) {
                        check(h, special > 0 && special <= most + EPS, name(b) + "'s special hit: " + special);
                        double lands = b.hitOf(special) / MobScaling.typicalHealth(b.measuredLevel());
                        check(h, Math.abs(lands - special) <= EPS, name(b) + "'s special hit lands as declared: " + lands);
                    }
                }
            } finally {
                e.discard();
            }
        }
        check(h, zone >= 20 && wild == 4 && bosses >= 10, "every kind measured: " + zone + " zone, " + wild + " wild, " + bosses + " bosses");
        h.succeed();
    }
}
