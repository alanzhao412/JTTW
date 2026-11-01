package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
import com.jttw.engine.entity.flowerfruit.SixEaredMacaqueEntity;
import com.jttw.engine.entity.heaven.HeavenlySoldierEntity;
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
 * The pecking order of the mod's creatures, by how hard each hits (a share of a same-level
 * character's health): every regular mob's hardest single hit at most {@link ZoneMonster#MOB_HIT_CAP},
 * every captain's at most that times a captain's measure, every miniboss's between a captain's and a
 * boss's, every boss's blow over them all (and its special hits, {@link ZoneBoss#specialShares}, no harder
 * than its blow may be). And by how much it takes to kill: a boss is a group's
 * fight, at least {@link ZoneBoss#LEAST_HITS} ordinary hits all its lives together, and each of its
 * lives more than any miniboss. Every kind there is is measured, so a new one out of its place fails
 * here. Each is made and measured off the world, never let loose in it.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PeckingOrderGameTests {

    /** The most a captain's hardest hit may be. */
    public static final double CAPTAIN_CAP = 0.145;
    /** A miniboss's blow, at least and at most; and the least any boss's. */
    public static final double MINIBOSS_LEAST = 0.17, MINIBOSS_MOST = 0.20, BOSS_LEAST = ZoneBoss.LEAST_BLOW, BOSS_MOST = ZoneBoss.MOST_BLOW;
    /** The most a lair makes a miniboss's health over a captain's. */
    public static final double MINIBOSS_OVER_CAPTAIN = Math.max(1, Math.max(Math.max(CaveHousehold.GUARDIAN_HEALTH,
            Math.max(CaveGarrison.GUARDIAN_HEALTH, LionCamelCityFeature.WARDEN_HEALTH)), Math.max(com.jttw.engine.entity.spider.SpiderSpiritEntity.BOWER_HEALTH,
            com.jttw.engine.world.flowerfruit.CavePlan.PILGRIM_HEALTH)));
    /** The most ordinary hits a regular mob takes to kill. */
    public static final double MOB_HITS_MOST = 12;
    private static final double EPS = 1e-6;

    /** The kinds a lair makes its floor wardens, gate keepers and guardians of: each must land a miniboss's blow. */
    private static final Set<EntityType<?>> MINIBOSS_KINDS = Set.of(ModEntities.BONE_SERVANT.get(), ModEntities.WRAITH.get(),
            ModEntities.MOUSE_SPIRIT.get(), ModEntities.FLOWER_MAID.get(), ModEntities.FURNACE_BRUTE.get(), ModEntities.STRONG_GENERAL.get(),
            ModEntities.LION_DEMON.get(), ModEntities.ELEPHANT_DEMON.get(), ModEntities.PENG_DEMON.get(),
            com.jttw.engine.registry.BlackWindRegistry.BEAR_SOLDIER.get(), com.jttw.engine.registry.BlackWindRegistry.LINGXUZI.get(),
            com.jttw.engine.registry.BlackWindRegistry.WHITE_ROBED_SCHOLAR.get(),
            com.jttw.engine.registry.FlowerFruitRegistry.FAKE_PILGRIM.get(),
            com.jttw.engine.registry.SpiderCaveRegistry.SPIDER_SPIRIT.get());

    /**
     * The one superboss, the Six-Eared Macaque, an optional fight: level 80 but measured against a
     * level-80 Wukong, not a typical character of 80. Up to this many of that Wukong's hits, all his
     * lives together.
     */
    public static final double SUPERBOSS_HITS_MOST = 460;

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

    /** A boss's health in the hits it is measured by: a same-level character's, or for the superboss, a level-80 Wukong's. */
    private static double bossHits(ZoneBoss b) {
        return superboss(b) ? b.getMaxHealth() / SixEaredMacaqueEntity.WUKONG_HIT : hits(b, b.mobLevel());
    }

    @GameTest(template = PLATFORM)
    public static void aBossTakesAGroupAndEachOfItsLivesOutlastsAnyMiniboss(GameTestHelper h) {
        // the toughest miniboss there is, in hits: any kind a lair could make one of, at the most a lair gives one, and the temple youths
        double miniboss = 0;
        String toughest = "";
        List<ZoneBoss> bosses = new ArrayList<>();
        for (Entity e : every(h)) {
            double m = -1;
            if (e instanceof ZoneMonster z) {
                z.becomeMiniboss(z.captainLevel(), 1);
                m = hits(z, z.captainLevel()) * (z instanceof StrongGeneralEntity ? 1 : MINIBOSS_OVER_CAPTAIN);
            } else if (e instanceof HeavenlySoldierEntity s) {
                s.become(40, true, null);
                m = hits(s, 40);
            } else if (e instanceof TempleYouthEntity y) {
                m = hits(y, y.mobLevel());
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
        check(h, miniboss >= 60, "the minibosses measured: the toughest " + toughest + ", " + miniboss);
        for (ZoneBoss b : bosses) {
            double full = bossHits(b), all = 0, least = Double.MAX_VALUE;
            for (double life : b.lives()) {
                all += full * life;
                least = Math.min(least, full * life);
            }
            check(h, all >= ZoneBoss.LEAST_HITS - EPS, name(b) + " a group's fight: " + all + " hits");
            check(h, all <= (superboss(b) ? SUPERBOSS_HITS_MOST : 360), name(b) + " no endless one: " + all + " hits");
            check(h, least > miniboss, name(b) + "'s every life outlasts any miniboss (" + toughest + ", " + miniboss + "): " + least);
            b.discard();
        }
        check(h, bosses.size() >= 15, "every boss measured: " + bosses.size());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSixEaredMacaqueIsALevel80WukongAsABoss(GameTestHelper h) {
        var macaque = com.jttw.engine.registry.FlowerFruitRegistry.SIX_EARED_MACAQUE.get().create(h.getLevel());
        check(h, macaque != null, "made");
        int lv = macaque.mobLevel();
        eq(h, 80, lv, "level 80");
        check(h, macaque.getDisplayName().getString().contains("Lv 80"), macaque.getDisplayName().getString());
        double his = bossHits(macaque), blow = macaque.getAttributeValue(Attributes.ATTACK_DAMAGE) / SixEaredMacaqueEntity.WUKONG_HEALTH;
        check(h, his >= 400 && his <= SUPERBOSS_HITS_MOST, "some 400-450 hits of a level-80 Wukong: " + his);
        check(h, blow >= BOSS_MOST - 0.005 && blow <= BOSS_MOST + EPS, "his blow the top of a boss's band, of that Wukong's health: " + blow);
        // capped players can beat him: a group's fight, and his blow no one-shot
        check(h, hits(macaque, 100) <= 250 && share(macaque, 100) <= 0.2, "a fight for level-100 players: "
                + hits(macaque, 100) + " hits, a blow of " + share(macaque, 100));
        int others = 0;
        for (Entity e : every(h)) {
            try {
                if (!(e instanceof ZoneBoss b) || superboss(b) || b instanceof TempleYouthEntity || b.mobLevel() > lv) continue;
                others++;
                // over every boss of his level or below: in health and in blow, as the Monkey King's equal is
                check(h, macaque.getMaxHealth() > b.getMaxHealth() * java.util.Arrays.stream(b.lives()).sum(), "more health than " + name(b));
                check(h, macaque.getAttributeValue(Attributes.ATTACK_DAMAGE) > b.getAttributeValue(Attributes.ATTACK_DAMAGE), "a harder blow than " + name(b));
            } finally {
                e.discard();
            }
        }
        check(h, others >= 10, "weighed against every boss up to his level: " + others);
        macaque.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void bossesStandFarAboveTheirZonesInLevel(GameTestHelper h) {
        // the highest level anything of a zone but its bosses comes at: its band's top, its captains and minibosses, the temple youths
        java.util.Map<String, Integer> minions = new java.util.HashMap<>(), head = new java.util.HashMap<>();
        java.util.Map<String, com.jttw.engine.world.Zones.Zone> zones = new java.util.HashMap<>();
        List<ZoneBoss> bosses = new ArrayList<>();
        for (Entity e : every(h)) {
            if (e instanceof TempleYouthEntity y) minions.merge(y.zone().id(), y.mobLevel(), Math::max);
            else if (e instanceof ZoneBoss b) bosses.add(b);
            else if (e instanceof com.jttw.engine.entity.flowerfruit.MacaqueCloneEntity) { /* the superboss's hair: of his level, not the band's */ }
            else if (e instanceof ZoneMonster m) minions.merge(m.zone().id(), Math.max(m.zone().maxLevel(), m.captainLevel()), Math::max);
            if (!(e instanceof ZoneBoss) || e instanceof TempleYouthEntity) e.discard();
        }
        // and the posts of every fortress and garrison, at whatever level each is put there
        String ridge = com.jttw.engine.world.Zones.WHITE_BONE.id(), cave = com.jttw.engine.world.Zones.BOTTOMLESS.id(),
                fire = com.jttw.engine.world.Zones.FIRE_MOUNTAIN.id(), lion = com.jttw.engine.world.Zones.LION_CAMEL.id();
        var bone = com.jttw.engine.world.ridge.WhiteBoneCave.of(h.getLevel().getSeed());
        for (int k = 0; k <= com.jttw.engine.world.ridge.WhiteBoneCave.FLOORS; k++) {
            for (var post : bone.posts(k)) minions.merge(ridge, post.level(), Math::max);
        }
        for (var post : bone.outside()) minions.merge(ridge, post.level(), Math::max);
        int caveTop = com.jttw.engine.world.Zones.BOTTOMLESS.maxLevel();
        for (int k = -1; k < com.jttw.engine.world.feature.Labyrinth.LEVELS; k++) {
            // a household's are put a level either side of their floor's, never past the band (CaveHousehold.put)
            minions.merge(cave, Math.min(caveTop, com.jttw.engine.entity.cave.CaveMonster.captainOn(k) + 1), Math::max);
            minions.merge(cave, Math.min(caveTop, com.jttw.engine.entity.cave.CaveMonster.regularOn(k) + 1), Math::max);
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
        for (var z : List.of(com.jttw.engine.world.Zones.WHITE_BONE, com.jttw.engine.world.Zones.BOTTOMLESS,
                com.jttw.engine.world.Zones.FIRE_MOUNTAIN, com.jttw.engine.world.Zones.LION_CAMEL)) {
            check(h, minions.get(z.id()) <= z.maxLevel(), z.id() + "'s posts within its band: up to " + minions.get(z.id()));
        }
        for (ZoneBoss b : bosses) {
            com.jttw.engine.world.Zones.Zone z = b.zone();
            if (superboss(b)) {
                // the one superboss stands past every band and every head boss (theSixEaredMacaqueIsALevel80WukongAsABoss)
                check(h, b.mobLevel() > ZoneBoss.levelOver(z), name(b) + " far past his zone's head boss's level: " + b.mobLevel());
                b.discard();
                continue;
            }
            zones.put(z.id(), z);
            int top = Math.max(z.maxLevel(), minions.getOrDefault(z.id(), 0));
            check(h, b.mobLevel() > top, name(b) + " (Lv " + b.mobLevel() + ") above everything else of its zone (Lv " + top + ")");
            check(h, b.mobLevel() <= ZoneBoss.levelOver(z), name(b) + " no higher than its zone's head: " + b.mobLevel());
            head.merge(z.id(), b.mobLevel(), Math::max);
            b.discard();
        }
        for (var entry : head.entrySet()) {
            com.jttw.engine.world.Zones.Zone z = zones.get(entry.getKey());
            int top = Math.max(z.maxLevel(), minions.getOrDefault(z.id(), 0));
            eq(h, ZoneBoss.levelOver(z), entry.getValue(), z.id() + "'s head boss: its band's top and " + ZoneBoss.LEVELS_ABOVE_BAND);
            check(h, entry.getValue() >= top + ZoneBoss.LEVELS_ABOVE_BAND,
                    z.id() + "'s head boss (Lv " + entry.getValue() + ") " + ZoneBoss.LEVELS_ABOVE_BAND + " over everything else of it (Lv " + top + ")");
        }
        check(h, head.size() >= 7, "every zone's bosses measured: " + head.keySet());
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
                            || m instanceof com.jttw.engine.entity.spider.SpiderSpiritEntity; // always minibosses
                    if (!general) {
                        // a regular: its blow and its hardest hit (its specials too) within a mob's; its health a mob's
                        int lv = m.regularLevel();
                        m.become(lv, false);
                        check(h, share(m, lv) <= ZoneMonster.MOB_HIT_CAP + EPS, n + "'s blow: " + share(m, lv));
                        check(h, m.hardestShare() <= ZoneMonster.MOB_HIT_CAP + EPS, n + "'s hardest hit: " + m.hardestShare());
                        check(h, m.healthAt(lv, false) / MobScaling.typicalHit(lv) <= MOB_HITS_MOST + EPS, n + "'s hits to kill");
                        // a captain's
                        int cl = m.captainLevel();
                        m.become(cl, true);
                        check(h, share(m, cl) <= CAPTAIN_CAP + EPS, n + " captain's blow: " + share(m, cl));
                        check(h, m.hardestShare() * m.damageScale() <= CAPTAIN_CAP + EPS, n + " captain's hardest hit");
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
                    if (MINIBOSS_KINDS.contains(m.getType())) {
                        double blow = share(m, ml);
                        check(h, blow >= MINIBOSS_LEAST - EPS && blow <= MINIBOSS_MOST + EPS, n + " miniboss's blow: " + blow);
                        check(h, blow > CAPTAIN_CAP, n + " miniboss's blow over any captain's");
                    }
                    if (general) {
                        // whoever brings him, a miniboss
                        m.become(m.captainLevel(), false);
                        check(h, m.isCaptain() && m.isMiniboss() && share(m, m.mobLevel()) > CAPTAIN_CAP, n + " always a miniboss");
                    }
                } else if (e instanceof WildYaoguai y) {
                    wild++;
                    y.become(y.maxLevel());
                    check(h, share(y, y.maxLevel()) <= ZoneMonster.MOB_HIT_CAP + EPS, name(y) + "'s blow");
                    check(h, y.hardestShare() <= ZoneMonster.MOB_HIT_CAP + EPS, name(y) + "'s hardest hit");
                    check(h, y.healthAt(y.maxLevel()) / MobScaling.typicalHit(y.maxLevel()) <= MOB_HITS_MOST + EPS, name(y) + "'s hits to kill");
                } else if (e instanceof HeavenlySoldierEntity s) {
                    s.become(40, false, null);
                    check(h, share(s, 40) <= ZoneMonster.MOB_HIT_CAP + EPS, "a heavenly soldier's blow, a mob's: " + share(s, 40));
                    s.become(40, true, null);
                    double g = share(s, 40);
                    check(h, g >= MINIBOSS_LEAST - EPS && g <= MINIBOSS_MOST + EPS, "a heavenly general's blow, a miniboss's: " + g);
                } else if (e instanceof ZoneBoss b) {
                    // the superboss's blow is of a level-80 Wukong's health, as he is measured
                    double blow = superboss(b) ? b.getAttributeValue(Attributes.ATTACK_DAMAGE) / SixEaredMacaqueEntity.WUKONG_HEALTH : share(b, b.mobLevel());
                    if (b instanceof TempleYouthEntity) {
                        // Clear Wind and Bright Moon are minibosses
                        check(h, blow >= MINIBOSS_LEAST - EPS && blow <= MINIBOSS_MOST + EPS, name(b) + "'s blow, a miniboss's: " + blow);
                        continue;
                    }
                    bosses++;
                    if (b instanceof LadyEarthFlowEntity) blow *= 2; // her stroke is two cuts, the second hard on the first
                    check(h, blow >= BOSS_LEAST - EPS && blow <= BOSS_MOST + EPS, name(b) + "'s blow, a boss's: " + blow);
                    // its special hits (the blows that fall on all near it) no harder than a boss's blow, and as they land
                    for (double special : b.specialShares()) {
                        check(h, special > 0 && special <= BOSS_MOST + EPS, name(b) + "'s special hit: " + special);
                        double lands = b.hitOf(special) / MobScaling.typicalHealth(b.mobLevel());
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
