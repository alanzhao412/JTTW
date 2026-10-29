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
 * boss's, every boss's blow over them all. Every kind there is is measured, so a new one that hits
 * out of its place fails here. Each is made and measured off the world, never let loose in it.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PeckingOrderGameTests {

    /** The most a captain's hardest hit may be. */
    public static final double CAPTAIN_CAP = 0.145;
    /** A miniboss's blow, at least and at most; and the least any boss's. */
    public static final double MINIBOSS_LEAST = 0.17, MINIBOSS_MOST = 0.20, BOSS_LEAST = 0.20, BOSS_MOST = 0.33;
    /** The most ordinary hits a regular mob takes to kill. */
    public static final double MOB_HITS_MOST = 12;
    private static final double EPS = 1e-6;

    /** The kinds a lair makes its floor wardens, gate keepers and guardians of: each must land a miniboss's blow. */
    private static final Set<EntityType<?>> MINIBOSS_KINDS = Set.of(ModEntities.BONE_SERVANT.get(), ModEntities.WRAITH.get(),
            ModEntities.MOUSE_SPIRIT.get(), ModEntities.FLOWER_MAID.get(), ModEntities.FURNACE_BRUTE.get(), ModEntities.STRONG_GENERAL.get(),
            ModEntities.LION_DEMON.get(), ModEntities.ELEPHANT_DEMON.get(), ModEntities.PENG_DEMON.get());

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
