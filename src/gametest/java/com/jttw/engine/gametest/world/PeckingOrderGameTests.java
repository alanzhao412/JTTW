package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.MobScaling;
import com.jttw.engine.entity.cave.LadyEarthFlowEntity;
import com.jttw.engine.entity.fire.StrongGeneralEntity;
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
 * boss's, every boss's blow over them all. And by how much it takes to kill: a boss is a group's
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
    public static final double MINIBOSS_OVER_CAPTAIN = Math.max(1, Math.max(CaveHousehold.GUARDIAN_HEALTH,
            Math.max(CaveGarrison.GUARDIAN_HEALTH, Math.max(LionCamelCityFeature.WARDEN_HEALTH,
                    com.jttw.engine.world.flowerfruit.CavePlan.PILGRIM_HEALTH))));
    /** The most ordinary hits a regular mob takes to kill. */
    public static final double MOB_HITS_MOST = 12;
    private static final double EPS = 1e-6;

    /** The kinds a lair makes its floor wardens, gate keepers and guardians of: each must land a miniboss's blow. */
    private static final Set<EntityType<?>> MINIBOSS_KINDS = Set.of(ModEntities.BONE_SERVANT.get(), ModEntities.WRAITH.get(),
            ModEntities.MOUSE_SPIRIT.get(), ModEntities.FLOWER_MAID.get(), ModEntities.FURNACE_BRUTE.get(), ModEntities.STRONG_GENERAL.get(),
            ModEntities.LION_DEMON.get(), ModEntities.ELEPHANT_DEMON.get(), ModEntities.PENG_DEMON.get(),
            com.jttw.engine.registry.FlowerFruitRegistry.FAKE_PILGRIM.get());

    /**
     * The one superboss, past every rule a boss keeps but the least: the Six-Eared Macaque, level 120,
     * an optional fight, the strongest boss there is. Up to this many hits, all his lives together.
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
            double full = hits(b, b.mobLevel()), all = 0, least = Double.MAX_VALUE;
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
    public static void theSixEaredMacaqueIsTheStrongestBossThereIs(GameTestHelper h) {
        var macaque = com.jttw.engine.registry.FlowerFruitRegistry.SIX_EARED_MACAQUE.get().create(h.getLevel());
        check(h, macaque != null, "made");
        int lv = macaque.mobLevel();
        eq(h, 120, lv, "level 120, past the players' cap");
        check(h, macaque.getDisplayName().getString().contains("Lv 120"), macaque.getDisplayName().getString());
        double his = hits(macaque, lv), blow = share(macaque, lv);
        check(h, his >= 400 && his <= SUPERBOSS_HITS_MOST, "some 400-450 hits at his level: " + his);
        check(h, blow >= BOSS_MOST - 0.005 && blow <= BOSS_MOST + EPS, "his blow the top of a boss's band: " + blow);
        // a level past the cap is measured against a character grown on past it, gently
        check(h, MobScaling.typicalHealth(120) > MobScaling.typicalHealth(100) * 1.5 && MobScaling.typicalHit(120) > MobScaling.typicalHit(100) * 1.5,
                "his level counts past 100");
        int others = 0;
        for (Entity e : every(h)) {
            try {
                if (!(e instanceof ZoneBoss b) || superboss(b) || b instanceof TempleYouthEntity) continue;
                others++;
                double all = 0;
                for (double life : b.lives()) all += hits(b, b.mobLevel()) * life;
                check(h, his > all, "more hits than " + name(b) + " (" + all + "): " + his);
                check(h, blow >= share(b, b.mobLevel()) - EPS || b instanceof LadyEarthFlowEntity, "a blow no lighter than " + name(b) + "'s");
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
    public static void mobsHitFarLessThanTheirBosses(GameTestHelper h) {
        int zone = 0, wild = 0, bosses = 0;
        for (Entity e : every(h)) {
            try {
                if (e instanceof ZoneMonster m) {
                    zone++;
                    String n = name(m);
                    boolean general = m instanceof StrongGeneralEntity;
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
                        check(h, m.isCaptain() && m.isMiniboss() && share(m, m.mobLevel()) > CAPTAIN_CAP, "a Strong General always a miniboss");
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
                    double blow = share(b, b.mobLevel());
                    if (b instanceof TempleYouthEntity) {
                        // Clear Wind and Bright Moon are minibosses
                        check(h, blow >= MINIBOSS_LEAST - EPS && blow <= MINIBOSS_MOST + EPS, name(b) + "'s blow, a miniboss's: " + blow);
                        continue;
                    }
                    bosses++;
                    if (b instanceof LadyEarthFlowEntity) blow *= 2; // her stroke is two cuts, the second hard on the first
                    check(h, blow >= BOSS_LEAST - EPS && blow <= BOSS_MOST + EPS, name(b) + "'s blow, a boss's: " + blow);
                }
            } finally {
                e.discard();
            }
        }
        check(h, zone >= 20 && wild == 4 && bosses >= 10, "every kind measured: " + zone + " zone, " + wild + " wild, " + bosses + " bosses");
        h.succeed();
    }
}
