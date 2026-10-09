package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.phoenix.PhoenixAbilityHandler;
import com.jttw.engine.character.phoenix.PhoenixReviveData;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** The Phoenix (Solar Sovereign): Flamethrower, Solar Radiance, Rebirth and the fire passives. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class PhoenixGameTests {

    private static ServerPlayer phoenix(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.PHOENIX);
        qi(p, 100f, 100f);
        return p;
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.PHOENIX.id);
    }

    private static void ticks(ServerPlayer p, int n) {
        tick(p, PhoenixAbilityHandler::onPlayerTick, n);
    }

    /** An iron golem two blocks south, with the player aiming at its middle. */
    private static IronGolem golemInFlame(GameTestHelper h, ServerPlayer p) {
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        p.setYRot(0f);
        p.setXRot(8f);
        return golem;
    }

    // ── Flamethrower (slot 0, held) ─────────────────────────────────

    @GameTest(template = PLATFORM, batch = "flamethrowerNeedsTheNode")
    public static void flamethrowerNeedsTheNode(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        IronGolem golem = golemInFlame(h, p);
        List<Hit> hits = watch(golem);
        character().activate(p, 0, AbilityAction.START);
        ticks(p, 5);
        check(h, hits.isEmpty(), "no flame");
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:flamethrower_slow"), "not rooted");
        near(h, 100f, QiData.getCurrent(p), "Qi");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "flamethrowerRootsDrainsFiveQiPerSecondAndBurns")
    public static void flamethrowerRootsDrainsFiveQiPerSecondAndBurns(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_flame_1");
        IronGolem golem = golemInFlame(h, p);
        List<Hit> hits = watch(golem);
        character().activate(p, 0, AbilityAction.START);
        eq(h, -1.0, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:flamethrower_slow"), "cannot move");
        ticks(p, 10);
        near(h, 97.5f, QiData.getCurrent(p), "0.25 Qi per tick");
        List<Hit> physical = of(hits, DamageTypes.MOB_ATTACK);
        List<Hit> magic = of(hits, DamageTypes.MAGIC);
        eq(h, 10, physical.size(), "one physical half per tick");
        eq(h, 10, magic.size(), "one magic half per tick");
        near(h, 0.25, physical.get(0).amount(), "half of 0.5");
        near(h, 0.25, magic.get(0).amount(), "half of 0.5");
        eq(h, 40, golem.getRemainingFireTicks(), "burning for 2 s");
        character().activate(p, 0, AbilityAction.STOP);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:flamethrower_slow"), "free to move");
        ticks(p, 5);
        near(h, 97.5f, QiData.getCurrent(p), "no drain after release");
        eq(h, 10, of(hits, DamageTypes.MAGIC).size(), "no more hits after release");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "flamethrowerTierThreeBurnsHotterLongerAndCheaper")
    public static void flamethrowerTierThreeBurnsHotterLongerAndCheaper(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_flame_1", "ph_flame_2", "ph_flame_3");
        PlayerLevel.setLevel(p, 20); // the flame's damage is set at level 1, scaled by the player's power
        IronGolem golem = golemInFlame(h, p);
        List<Hit> hits = watch(golem);
        PhoenixAbilityHandler.startFlame(p);
        ticks(p, 10);
        near(h, 98.25f, QiData.getCurrent(p), "0.175 Qi per tick from tier 2");
        // 0.5 x 1.5 x 1.5 = 1.125 at level 1, half physical and half magic, scaled to level 20
        near(h, 0.5625 * LevelPower.of(20), firstHit(h, hits, DamageTypes.MAGIC, golem).amount(), "magic half at level 20");
        eq(h, 60, golem.getRemainingFireTicks(), "burning for 3 s from tier 2");
        PhoenixAbilityHandler.stopFlame(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "flamethrowerGoesOutWhenQiRunsOut")
    public static void flamethrowerGoesOutWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_flame_1");
        QiData.setCurrent(p, 0.6f);
        PhoenixAbilityHandler.startFlame(p);
        ticks(p, 3);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:flamethrower_slow"), "flame out, root lifted");
        QiData.setCurrent(p, 50f);
        ticks(p, 3);
        near(h, 50f, QiData.getCurrent(p), "no drain once out");
        h.succeed();
    }

    // ── Solar Radiance (slot 1, toggle) ─────────────────────────────

    private static void hitBy(GameTestHelper h, ServerPlayer p, net.minecraft.world.damagesource.DamageSource src) {
        PhoenixAbilityHandler.onTakeDamageRadiance(new LivingHurtEvent(p, src, 4f));
    }

    @GameTest(template = PLATFORM, batch = "solarRadianceTogglesWithTheNode")
    public static void solarRadianceTogglesWithTheNode(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        character().activate(p, 1, AbilityAction.USE);
        check(h, !PhoenixAbilityHandler.isRadianceActive(p), "needs ph_radiance");
        grant(p, "ph_radiance_1");
        character().activate(p, 1, AbilityAction.USE);
        check(h, PhoenixAbilityHandler.isRadianceActive(p), "on");
        near(h, 100f, QiData.getCurrent(p), "the aura costs no Qi to raise");
        character().activate(p, 1, AbilityAction.USE);
        check(h, !PhoenixAbilityHandler.isRadianceActive(p), "off");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "solarRadianceSetsMeleeAttackersAlight")
    public static void solarRadianceSetsMeleeAttackersAlight(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_radiance_1");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        var ds = h.getLevel().damageSources();
        hitBy(h, p, ds.mobAttack(husk));
        check(h, husk.getRemainingFireTicks() <= 0, "aura off: no thorns");
        PhoenixAbilityHandler.toggleRadiance(p);
        hitBy(h, p, ds.mobAttack(husk));
        eq(h, 60, husk.getRemainingFireTicks(), "tier 1: 3 s of fire");
        Husk archer = mob(h, EntityType.HUSK, 4, 0);
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        hitBy(h, p, ds.arrow(arrow, archer));
        check(h, archer.getRemainingFireTicks() <= 0, "projectiles do not trigger thorns");
        PhoenixAbilityHandler.toggleRadiance(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "solarRadianceTierThreeBurnsLongerHitsBackAndWeakens")
    public static void solarRadianceTierThreeBurnsLongerHitsBackAndWeakens(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_radiance_1", "ph_radiance_2", "ph_radiance_3");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        List<Hit> hits = watch(golem);
        PhoenixAbilityHandler.toggleRadiance(p);
        hitBy(h, p, h.getLevel().damageSources().mobAttack(golem));
        eq(h, 100, golem.getRemainingFireTicks(), "5 s of fire from tier 2");
        List<Hit> thorns = of(hits, DamageTypes.ON_FIRE);
        eq(h, 1, thorns.size(), "thorns damage");
        near(h, 8.0, thorns.get(0).amount(), "twice the 4 damage taken");
        eq(h, 0, amp(golem, MobEffects.WEAKNESS), "Weakness I");
        eq(h, 200, duration(golem, MobEffects.WEAKNESS), "for 10 s");
        PhoenixAbilityHandler.toggleRadiance(p);
        h.succeed();
    }

    // ── Rebirth (innate) ────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "newPhoenixStartsWithFiveRevives")
    public static void newPhoenixStartsWithFiveRevives(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        eq(h, 5, PhoenixReviveData.getRevives(p), "revives");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "lethalHitAtLowHealthIsCancelledByARevive")
    public static void lethalHitAtLowHealthIsCancelledByARevive(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4); // 2 blocks away
        List<Hit> hits = watch(golem);
        float max = p.getMaxHealth();
        p.setHealth(max * 0.5f);
        LivingHurtEvent hit = new LivingHurtEvent(p, h.getLevel().damageSources().generic(), max * 0.45f);
        PhoenixAbilityHandler.onTakeDamage(hit);
        check(h, hit.isCanceled(), "hit dropping HP to 5% is cancelled");
        near(h, max, p.getHealth(), "fully healed");
        eq(h, 4, PhoenixReviveData.getRevives(p), "one revive spent");
        List<Hit> blast = of(hits, DamageTypes.ON_FIRE);
        eq(h, 1, blast.size(), "rebirth blast hits the golem");
        // 25% of max HP, falloff 1 - 2/5 at the base 5-block radius
        near(h, max * 0.25 * 0.6, blast.get(0).amount(), "blast damage");
        eq(h, 100, golem.getRemainingFireTicks(), "set burning for 5 s");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "rebirthOnlyTriggersAtTenPercentAndNeedsACharge")
    public static void rebirthOnlyTriggersAtTenPercentAndNeedsACharge(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        float max = p.getMaxHealth();
        p.setHealth(max);
        LivingHurtEvent glancing = new LivingHurtEvent(p, h.getLevel().damageSources().generic(), max * 0.5f);
        PhoenixAbilityHandler.onTakeDamage(glancing);
        check(h, !glancing.isCanceled(), "leaves 50%: no rebirth");
        eq(h, 5, PhoenixReviveData.getRevives(p), "no revive spent");

        PhoenixReviveData.setRevives(p, 0);
        p.setHealth(max * 0.5f);
        LivingHurtEvent lethal = new LivingHurtEvent(p, h.getLevel().damageSources().generic(), max);
        PhoenixAbilityHandler.onTakeDamage(lethal);
        check(h, !lethal.isCanceled(), "no charges left: the hit lands");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "rebirthMasteryWidensTheBlast")
    public static void rebirthMasteryWidensTheBlast(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_rebirth_1");
        IronGolem far = mob(h, EntityType.IRON_GOLEM, 4, 4); // ~2.8 blocks away
        List<Hit> hits = watch(far);
        float max = p.getMaxHealth();
        p.setHealth(max * 0.05f);
        PhoenixAbilityHandler.onTakeDamage(new LivingHurtEvent(p, h.getLevel().damageSources().generic(), 1f));
        double dist = p.distanceTo(far);
        // tier 1: 8-block radius
        near(h, max * 0.25 * (1 - dist / 8.0), of(hits, DamageTypes.ON_FIRE).get(0).amount(), "blast damage at 8-block radius");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "aReviveChargeReturnsEverySevenDays")
    public static void aReviveChargeReturnsEverySevenDays(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        long today = p.level().getDayTime() / 24000L;
        PhoenixReviveData.setRevives(p, 2);
        PhoenixReviveData.setLastGainDay(p, today - 6);
        p.tickCount = 99;
        ticks(p, 1);
        eq(h, 2, PhoenixReviveData.getRevives(p), "six days: nothing yet");
        PhoenixReviveData.setLastGainDay(p, today - 7);
        p.tickCount = 199;
        ticks(p, 1);
        eq(h, 3, PhoenixReviveData.getRevives(p), "seventh day: +1");
        eq(h, today, PhoenixReviveData.getLastGainDay(p), "clock restarted");

        grant(p, "ph_rebirth_1", "ph_rebirth_2");
        PhoenixReviveData.setLastGainDay(p, today - 7);
        p.tickCount = 299;
        ticks(p, 1);
        eq(h, 5, PhoenixReviveData.getRevives(p), "Rebirth Mastery II: +2 a week");
        h.succeed();
    }

    // ── Passives ────────────────────────────────────────────────────

    /** Regen is 0.5 + 0.015 a level per second, times the player's power. */
    @GameTest(template = PLATFORM, batch = "phoenixRegenGrowsWithLevelAndPower")
    public static void phoenixRegenGrowsWithLevelAndPower(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        PlayerLevel.setLevel(p, 20);
        LevelManager.reapplyStats(p); // room above 10 health for the heal to show
        p.setHealth(10f);
        p.tickCount = 19;
        ticks(p, 1);
        near(h, 10f + LevelPower.scaled(p, 0.5), p.getHealth(), "0.5 x power at level 20");
        PlayerLevel.setLevel(p, 1);
        LevelManager.reapplyStats(p);
        p.setHealth(10f);
        p.tickCount = 39;
        ticks(p, 1);
        close(h, 10.5, p.getHealth(), 0.02, "+0.5 at level 1");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "lifeDrainHealsOnEveryHit")
    public static void lifeDrainHealsOnEveryHit(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        PlayerLevel.setLevel(p, 20); // the heals are set at level 1, scaled by the player's power
        LevelManager.reapplyStats(p);
        float power = (float) LevelPower.of(20);
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();
        p.setHealth(10f);
        PhoenixAbilityHandler.onDealDamage(new LivingHurtEvent(pig, ds.playerAttack(p), 3f));
        near(h, 10f, p.getHealth(), "no node, no heal");
        grant(p, "ph_drain_1");
        PhoenixAbilityHandler.onDealDamage(new LivingHurtEvent(pig, ds.playerAttack(p), 3f));
        near(h, 10f + power, p.getHealth(), "tier 1: half a heart at level 1, scaled to 20");
        grant(p, "ph_drain_2");
        PhoenixAbilityHandler.onDealDamage(new LivingHurtEvent(pig, ds.playerAttack(p), 3f));
        near(h, 10f + 3 * power, p.getHealth(), "tier 2: a heart at level 1, scaled to 20");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "moltenTalonsIgniteOnMeleeAndFireDamageHeals")
    public static void moltenTalonsIgniteOnMeleeAndFireDamageHeals(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_talons_1");
        Pig pig = mob(h, EntityType.PIG, 0, 0);
        var ds = h.getLevel().damageSources();
        PhoenixAbilityHandler.onDealDamage(new LivingHurtEvent(pig, ds.playerAttack(p), 3f));
        eq(h, 60, pig.getRemainingFireTicks(), "melee sets fire for 3 s");

        Pig shot = mob(h, EntityType.PIG, 4, 0);
        Arrow arrow = EntityType.ARROW.create(h.getLevel());
        PhoenixAbilityHandler.onDealDamage(new LivingHurtEvent(shot, ds.arrow(arrow, p), 3f));
        check(h, shot.getRemainingFireTicks() <= 0, "arrows do not ignite");

        grant(p, "ph_talons_2");
        p.setHealth(10f);
        SmallFireball ball = new SmallFireball(h.getLevel(), p, 0, 0, 0);
        PhoenixAbilityHandler.onDealDamage(new LivingHurtEvent(shot, ds.fireball(ball, p), 8f));
        near(h, 12f, p.getHealth(), "fire damage heals 25% of 8");
        ball.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "moltenTalonsTierThreeBurningKillsSpreadFire")
    public static void moltenTalonsTierThreeBurningKillsSpreadFire(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_talons_1", "ph_talons_2", "ph_talons_3");
        Pig victim = mob(h, EntityType.PIG, 0, 0);
        // golems, so the blast itself does not kill them before the fire spreads
        IronGolem neighbour = mob(h, EntityType.IRON_GOLEM, 0, 2);
        IronGolem distant = mob(h, EntityType.IRON_GOLEM, 4, 4); // ~5.7 blocks from the victim
        victim.setRemainingFireTicks(40);
        PhoenixAbilityHandler.onEntityDeath(new LivingDeathEvent(victim, h.getLevel().damageSources().playerAttack(p)));
        eq(h, 100, neighbour.getRemainingFireTicks(), "fire spreads 5 s within 4 blocks");
        check(h, distant.getRemainingFireTicks() <= 0, "not beyond 4 blocks");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "risingEmberTierTwoMakesThePhoenixFireproof")
    public static void risingEmberTierTwoMakesThePhoenixFireproof(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        var ds = h.getLevel().damageSources();
        LivingHurtEvent before = new LivingHurtEvent(p, ds.inFire(), 2f);
        PhoenixAbilityHandler.onTakeDamageRadiance(before);
        check(h, !before.isCanceled(), "fire hurts without Rising Ember II");
        grant(p, "ph_ember_1", "ph_ember_2");
        LivingHurtEvent fire = new LivingHurtEvent(p, ds.inFire(), 2f);
        PhoenixAbilityHandler.onTakeDamageRadiance(fire);
        check(h, fire.isCanceled(), "fire cancelled");
        LivingHurtEvent lava = new LivingHurtEvent(p, ds.lava(), 4f);
        PhoenixAbilityHandler.onTakeDamageRadiance(lava);
        check(h, lava.isCanceled(), "lava cancelled");
        p.setRemainingFireTicks(100);
        ticks(p, 1);
        check(h, !p.isOnFire(), "flames put out on tick");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "phoenixKeysIgnoreTheWrongAction")
    public static void phoenixKeysIgnoreTheWrongAction(GameTestHelper h) {
        ServerPlayer p = phoenix(h);
        grant(p, "ph_flame_1", "ph_radiance_1");
        PlayableCharacter c = character();
        c.activate(p, 0, AbilityAction.USE);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:flamethrower_slow"), "flame ignores USE");
        c.activate(p, 1, AbilityAction.START);
        check(h, !PhoenixAbilityHandler.isRadianceActive(p), "radiance ignores START");
        c.activate(p, 0, AbilityAction.START);
        eq(h, -1.0, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:flamethrower_slow"), "flame starts on START");
        c.activate(p, 0, AbilityAction.STOP);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:flamethrower_slow"), "and stops on STOP");
        h.succeed();
    }
}
