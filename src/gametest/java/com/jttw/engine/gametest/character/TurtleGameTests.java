package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.turtle.TurtleAbilityHandler;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The World Turtle: Shell Guard, Unstoppable Force, Tectonic Slam, Gravity Well and passives. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TurtleGameTests {

    private static ServerPlayer turtle(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.TURTLE);
        qi(p, 100f, 100f);
        return p;
    }

    /** Physical resistance fixed at 25, so the resistance-scaled abilities have known damage. */
    private static void presTwentyFive(ServerPlayer p) {
        setResistances(p, 25.0, 0.0);
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.TURTLE.id);
    }

    private static void ticks(ServerPlayer p, int n) {
        tick(p, TurtleAbilityHandler::onPlayerTick, n);
    }

    private static float hurt(ServerPlayer p, DamageSource src, float amount) {
        LivingHurtEvent ev = new LivingHurtEvent(p, src, amount);
        TurtleAbilityHandler.onLivingHurt(ev);
        return ev.isCanceled() ? 0f : ev.getAmount();
    }

    // ── Innate passive ──────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "turtleGainsOnePhysicalResistancePerLevel")
    public static void turtleGainsOnePhysicalResistancePerLevel(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        PlayerLevel.setLevel(p, 20);
        LevelManager.reapplyStats(p);
        eq(h, 20.0, modifier(p, PHYSICAL_RESISTANCE(), "jttw:turtle_pres"), "resistance bonus at 20");
        eq(h, null, modifier(p, Attributes.ARMOR, "jttw:turtle_def"), "no vanilla armor bonus");
        eq(h, null, modifier(p, Attributes.ARMOR_TOUGHNESS, "jttw:turtle_toughness"), "no toughness bonus");

        ServerPlayer tiger = as(h, ReincarnationOutcome.WHITE_TIGER);
        PlayerLevel.setLevel(tiger, 20);
        LevelManager.reapplyStats(tiger);
        eq(h, null, modifier(tiger, PHYSICAL_RESISTANCE(), "jttw:turtle_pres"), "only the Earth line");
        h.succeed();
    }

    /** The passive's full +100 lands at level 100: resistance has no cap, unlike vanilla armor's 30. */
    @GameTest(template = PLATFORM, batch = "turtlesPassiveIsNotCappedAtLevelHundred")
    public static void turtlesPassiveIsNotCappedAtLevelHundred(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        PlayerLevel.setLevel(p, 100);
        LevelManager.reapplyStats(p);
        double fromLevels = com.jttw.engine.progression.stats.StatBlock
                .compute(com.jttw.engine.progression.stats.ProgressionClass.DIVINE_BEAST, 100).physicalResistance();
        near(h, fromLevels + 100.0, pres(p), "level stats + passive");
        h.succeed();
    }

    /** Players who levelled before resistances existed lose the old armor/toughness version. */
    @GameTest(template = PLATFORM, batch = "turtlesOldArmorPassiveIsRemoved")
    public static void turtlesOldArmorPassiveIsRemoved(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        java.util.UUID id = java.util.UUID.fromString("c3d4e5f6-a7b8-9012-cdef-234567890100");
        p.getAttribute(Attributes.ARMOR).addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                id, "jttw:turtle_def", 20.0, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
        LevelManager.reapplyStats(p);
        eq(h, null, modifier(p, Attributes.ARMOR, "jttw:turtle_def"), "old armor bonus");
        h.succeed();
    }

    // ── Shell Guard (slot 0, held) ──────────────────────────────────

    @GameTest(template = PLATFORM, batch = "shellGuardNeedsTheNode")
    public static void shellGuardNeedsTheNode(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        character().activate(p, 0, AbilityAction.START);
        check(h, !TurtleAbilityHandler.isGuarding(p.getUUID()), "not guarding");
        near(h, 8f, hurt(p, h.getLevel().damageSources().generic(), 8f), "full damage");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "shellGuardCutsDamageToAQuarterSlowsAndBlocksAttacks")
    public static void shellGuardCutsDamageToAQuarterSlowsAndBlocksAttacks(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_shell_1");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        var ds = h.getLevel().damageSources();
        character().activate(p, 0, AbilityAction.START);
        check(h, TurtleAbilityHandler.isGuarding(p.getUUID()), "guarding");
        eq(h, -0.70, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:shell_guard_slow"), "70% slower");
        near(h, 2f, hurt(p, ds.mobAttack(husk), 8f), "a quarter of the damage");
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        near(h, 2f, hurt(p, ds.arrow(arrow, husk), 8f), "tier 1 only reduces arrows");
        AttackEntityEvent swing = new AttackEntityEvent(p, husk);
        TurtleAbilityHandler.onAttackEntity(swing);
        check(h, swing.isCanceled(), "cannot attack from the shell");
        ticks(p, 10);
        near(h, 98.5f, QiData.getCurrent(p), "0.15 Qi per tick");
        character().activate(p, 0, AbilityAction.STOP);
        check(h, !TurtleAbilityHandler.isGuarding(p.getUUID()), "out of the shell");
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:shell_guard_slow"), "speed restored");
        near(h, 8f, hurt(p, ds.mobAttack(husk), 8f), "full damage again");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "shellGuardTierTwoStopsArrowsAndTierThreeExplosions")
    public static void shellGuardTierTwoStopsArrowsAndTierThreeExplosions(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_shell_1", "tu_shell_2");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        var ds = h.getLevel().damageSources();
        TurtleAbilityHandler.startGuard(p);
        near(h, 0f, hurt(p, ds.arrow(arrow, husk), 8f), "tier 2 negates arrows");
        near(h, 2f, hurt(p, ds.explosion(null, null), 8f), "tier 2 only reduces explosions");
        grant(p, "tu_shell_3");
        near(h, 0f, hurt(p, ds.explosion(null, null), 8f), "tier 3 negates explosions");
        TurtleAbilityHandler.stopGuard(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "shellGuardDropsWhenQiRunsOut")
    public static void shellGuardDropsWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_shell_1");
        QiData.setCurrent(p, 0.2f);
        TurtleAbilityHandler.startGuard(p);
        ticks(p, 2);
        check(h, !TurtleAbilityHandler.isGuarding(p.getUUID()), "shell dropped");
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:shell_guard_slow"), "speed restored");
        h.succeed();
    }

    // ── Unstoppable Force (slot 1) ──────────────────────────────────

    @GameTest(template = PLATFORM, batch = "unstoppableForceNeedsTheNodeAndFifteenQi")
    public static void unstoppableForceNeedsTheNodeAndFifteenQi(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        character().activate(p, 1, AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "no node");
        grant(p, "tu_force_1");
        QiData.setCurrent(p, 14f);
        character().activate(p, 1, AbilityAction.USE);
        near(h, 14f, QiData.getCurrent(p), "14 Qi");
        near(h, 0.0, p.getDeltaMovement().length(), "no launch");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "unstoppableForceLaunchesThenASecondPressSlamsDown")
    public static void unstoppableForceLaunchesThenASecondPressSlamsDown(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_force_1");
        presTwentyFive(p);
        p.setYRot(0f);
        p.setXRot(0f);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4); // 2 blocks ahead
        List<Hit> hits = watch(golem);
        character().activate(p, 1, AbilityAction.USE);
        near(h, 85f, QiData.getCurrent(p), "15 Qi");
        near(h, 3.5, p.getDeltaMovement().z, "launched forward at 3.5");
        character().activate(p, 1, AbilityAction.USE);
        near(h, 0.0, p.getDeltaMovement().length(), "stopped on impact");
        List<Hit> blows = of(hits, DamageTypes.PLAYER_ATTACK);
        eq(h, 1, blows.size(), "impact hits the golem");
        // tier 1: 6 + resistance 25 x 0.04 = 7, falloff 1 - 2/8
        near(h, 5.25, blows.get(0).amount(), "impact damage");
        check(h, golem.getDeltaMovement().z > 0, "knocked away");
        eq(h, -1, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "no stun below tier 3");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "unstoppableForceLandsByItself")
    public static void unstoppableForceLandsByItself(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_force_1", "tu_force_2", "tu_force_3");
        presTwentyFive(p);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        TurtleAbilityHandler.castUnstoppableForce(p);
        p.setOnGround(false);
        ticks(p, 1);
        check(h, hits.isEmpty(), "still in the air");
        p.setOnGround(true);
        ticks(p, 1);
        // tier 3: 15 + resistance 25 x 0.08 = 17, falloff 1 - 2/8
        near(h, 12.75, of(hits, DamageTypes.PLAYER_ATTACK).get(0).amount(), "landing impact");
        eq(h, 4, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "tier 3 stun: Slowness V");
        check(h, golem.hasEffect(MobEffects.BLINDNESS), "tier 3 stun: blind");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200, batch = "unstoppableForceHasAFiveSecondCooldown")
    public static void unstoppableForceHasAFiveSecondCooldown(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_force_1");
        TurtleAbilityHandler.castUnstoppableForce(p);
        TurtleAbilityHandler.castUnstoppableForce(p); // impact
        TurtleAbilityHandler.castUnstoppableForce(p);
        near(h, 85f, QiData.getCurrent(p), "cooling down");
        h.runAfterDelay(95, () -> {
            TurtleAbilityHandler.castUnstoppableForce(p);
            near(h, 85f, QiData.getCurrent(p), "still cooling down before 100 ticks");
            h.runAfterDelay(6, () -> {
                TurtleAbilityHandler.castUnstoppableForce(p);
                near(h, 70f, QiData.getCurrent(p), "ready after 100 ticks");
                TurtleAbilityHandler.castUnstoppableForce(p);
                h.succeed();
            });
        });
    }

    // ── Tectonic Slam (slot 2) ──────────────────────────────────────

    @GameTest(template = PLATFORM, timeoutTicks = 300, batch = "tectonicSlamCostsTenQiHitsAroundAndHasATenSecondCooldown")
    public static void tectonicSlamCostsTenQiHitsAroundAndHasATenSecondCooldown(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        character().activate(p, 2, AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "needs the node");
        grant(p, "tu_tectonic_1");
        presTwentyFive(p);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        character().activate(p, 2, AbilityAction.USE);
        near(h, 90f, QiData.getCurrent(p), "10 Qi");
        // 4 + resistance 25 x 0.04, no falloff
        near(h, 5.0, of(hits, DamageTypes.PLAYER_ATTACK).get(0).amount(), "slam damage");
        TurtleAbilityHandler.castTectonicSlam(p);
        near(h, 90f, QiData.getCurrent(p), "on cooldown");
        h.runAfterDelay(201, () -> {
            TurtleAbilityHandler.castTectonicSlam(p);
            near(h, 80f, QiData.getCurrent(p), "ready after 200 ticks");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "tectonicSlamTierTwoLeavesADamagingZoneAndTierThreeLaunches")
    public static void tectonicSlamTierTwoLeavesADamagingZoneAndTierThreeLaunches(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        online(h, p);
        grant(p, "tu_tectonic_1", "tu_tectonic_2", "tu_tectonic_3");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        TurtleAbilityHandler.castTectonicSlam(p);
        near(h, 1.5, golem.getDeltaMovement().y, "tier 3 launches upward");
        int slamHits = of(hits, DamageTypes.PLAYER_ATTACK).size();
        h.onEachTick(() -> ticks(p, 1)); // zones are processed once per game tick
        h.runAfterDelay(45, () -> {
            List<Hit> zone = of(hits, DamageTypes.PLAYER_ATTACK).subList(slamHits, of(hits, DamageTypes.PLAYER_ATTACK).size());
            check(h, zone.size() >= 2, "zone pulses once a second, " + zone.size() + " pulses");
            near(h, 1.0, zone.get(0).amount(), "1 damage per pulse");
            h.succeed();
        });
    }

    // ── Gravity Well (slot 3, toggle) ───────────────────────────────

    @GameTest(template = PLATFORM, batch = "gravityWellPullsMobsInAndDrainsQiUntilToggledOff")
    public static void gravityWellPullsMobsInAndDrainsQiUntilToggledOff(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        character().activate(p, 3, AbilityAction.USE);
        check(h, !TurtleAbilityHandler.isGravityWellActive(p.getUUID()), "needs the node");
        grant(p, "tu_gravity_1");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4); // 2 blocks south
        character().activate(p, 3, AbilityAction.USE);
        check(h, TurtleAbilityHandler.isGravityWellActive(p.getUUID()), "on");
        ticks(p, 10);
        near(h, 99f, QiData.getCurrent(p), "0.1 Qi per tick");
        near(h, -0.75, golem.getDeltaMovement().z, "pulled north at 1.5 blocks/s for 10 ticks");
        character().activate(p, 3, AbilityAction.USE);
        check(h, !TurtleAbilityHandler.isGravityWellActive(p.getUUID()), "off");
        ticks(p, 5);
        near(h, 99f, QiData.getCurrent(p), "no drain when off");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "gravityWellNeedsQiToStartAndCollapsesWhenItRunsOut")
    public static void gravityWellNeedsQiToStartAndCollapsesWhenItRunsOut(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_gravity_1");
        QiData.setCurrent(p, 0.05f);
        TurtleAbilityHandler.toggleGravityWell(p);
        check(h, !TurtleAbilityHandler.isGravityWellActive(p.getUUID()), "refused below one tick of Qi");
        QiData.setCurrent(p, 0.25f);
        TurtleAbilityHandler.toggleGravityWell(p);
        ticks(p, 3);
        check(h, !TurtleAbilityHandler.isGravityWellActive(p.getUUID()), "collapsed when Qi ran out");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "gravityWellTierTwoMakesNearbyMobsTakeFifteenPercentMore")
    public static void gravityWellTierTwoMakesNearbyMobsTakeFifteenPercentMore(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        online(h, p);
        grant(p, "tu_gravity_1", "tu_gravity_2");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        var ds = h.getLevel().damageSources();
        LivingHurtEvent before = new LivingHurtEvent(golem, ds.generic(), 10f);
        TurtleAbilityHandler.onLivingHurt(before);
        near(h, 10f, before.getAmount(), "no well");
        TurtleAbilityHandler.toggleGravityWell(p);
        LivingHurtEvent during = new LivingHurtEvent(golem, ds.generic(), 10f);
        TurtleAbilityHandler.onLivingHurt(during);
        near(h, 11.5f, during.getAmount(), "+15% inside the well");
        TurtleAbilityHandler.toggleGravityWell(p);
        h.succeed();
    }

    /** The Gravity Well's +15% damage taken is for enemies, not the Turtle or allies. */
    @GameTest(template = PLATFORM, batch = "gravityWellDoesNotMakeTheTurtleItselfVulnerable")
    public static void gravityWellDoesNotMakeTheTurtleItselfVulnerable(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        online(h, p);
        grant(p, "tu_gravity_1", "tu_gravity_2");
        TurtleAbilityHandler.toggleGravityWell(p);
        float taken = hurt(p, h.getLevel().damageSources().generic(), 10f);
        TurtleAbilityHandler.toggleGravityWell(p);
        near(h, 10f, taken, "damage the Turtle takes with its own well on");
        h.succeed();
    }

    // ── Mountain's Patience (passive) ───────────────────────────────

    @GameTest(template = PLATFORM, batch = "standingStillBuildsUpToTwentyFivePercentForTheNextHit")
    public static void standingStillBuildsUpToTwentyFivePercentForTheNextHit(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_patience_1");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        ticks(p, 21);
        near(h, 0.05f, TurtleAbilityHandler.getPatienceBonus(p.getUUID()), "+5% after a second");
        ticks(p, 200);
        near(h, 0.25f, TurtleAbilityHandler.getPatienceBonus(p.getUUID()), "capped at +25%");
        LivingHurtEvent strike = new LivingHurtEvent(golem, h.getLevel().damageSources().playerAttack(p), 10f);
        TurtleAbilityHandler.onLivingHurt(strike);
        near(h, 12.5f, strike.getAmount(), "+25% on the next hit");
        near(h, 0f, TurtleAbilityHandler.getPatienceBonus(p.getUUID()), "spent");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "movingResetsPatience")
    public static void movingResetsPatience(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_patience_1");
        ticks(p, 61);
        near(h, 0.15f, TurtleAbilityHandler.getPatienceBonus(p.getUUID()), "+15% after 3 s");
        p.setPos(p.getX() + 2, p.getY(), p.getZ());
        ticks(p, 1);
        near(h, 0f, TurtleAbilityHandler.getPatienceBonus(p.getUUID()), "reset by moving");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "patienceTierTwoFullStacksCritAndStun")
    public static void patienceTierTwoFullStacksCritAndStun(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_patience_1", "tu_patience_2");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        ticks(p, 101);
        check(h, TurtleAbilityHandler.hasMaxPatienceStacks(p.getUUID()), "full stacks after 5 s");
        LivingHurtEvent strike = new LivingHurtEvent(golem, h.getLevel().damageSources().playerAttack(p), 10f);
        TurtleAbilityHandler.onLivingHurt(strike);
        near(h, 18.75f, strike.getAmount(), "x1.25 x1.5");
        eq(h, 4, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "stunned: Slowness V");
        eq(h, 40, duration(golem, MobEffects.MOVEMENT_SLOWDOWN), "for 2 s");
        check(h, !TurtleAbilityHandler.hasMaxPatienceStacks(p.getUUID()), "stacks spent");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "turtleKeysIgnoreTheWrongAction")
    public static void turtleKeysIgnoreTheWrongAction(GameTestHelper h) {
        ServerPlayer p = turtle(h);
        grant(p, "tu_shell_1", "tu_force_1", "tu_tectonic_1", "tu_gravity_1");
        PlayableCharacter c = character();
        c.activate(p, 0, AbilityAction.USE);
        check(h, !TurtleAbilityHandler.isGuarding(p.getUUID()), "shell ignores USE");
        c.activate(p, 1, AbilityAction.START);
        c.activate(p, 2, AbilityAction.START);
        near(h, 100f, QiData.getCurrent(p), "force and slam ignore START");
        c.activate(p, 3, AbilityAction.STOP);
        check(h, !TurtleAbilityHandler.isGravityWellActive(p.getUUID()), "well ignores STOP");
        c.activate(p, 0, AbilityAction.START);
        check(h, TurtleAbilityHandler.isGuarding(p.getUUID()), "shell on START");
        c.activate(p, 0, AbilityAction.STOP);
        check(h, !TurtleAbilityHandler.isGuarding(p.getUUID()), "shell off on STOP");
        h.succeed();
    }
}
