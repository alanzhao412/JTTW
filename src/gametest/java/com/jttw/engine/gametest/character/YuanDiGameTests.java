package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.yuandi.YuanDiAbilityHandler;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.LevelPower;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/**
 * Yuan Di: Primordial Transformation, Sovereign's Command, Demon's Breath, Primordial Smite,
 * Abyssal Gravity, Primordial Step, Night Power and Ancient Dominion.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class YuanDiGameTests {

    private static final String DOMINATED_BY = "jttw_dominated_by";

    private static ServerPlayer yuanDi(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.YUAN_DI);
        qi(p, 100f, 100f);
        return p;
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.YUAN_DI.id);
    }

    private static void ticks(ServerPlayer p, int n) {
        tick(p, YuanDiAbilityHandler::onPlayerTick, n);
    }

    private static void endIfTransformed(ServerPlayer p) {
        if (YuanDiAbilityHandler.isTransformed(p)) YuanDiAbilityHandler.toggleTransform(p);
    }

    // ── Primordial Transformation (slot 0) ──────────────────────────

    @GameTest(template = PLATFORM, batch = "transformKeyNeedsTierTwo")
    public static void transformKeyNeedsTierTwo(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1");
        character().use(p, "yd_transform", AbilityAction.USE);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "tier 1 is automatic only");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierTwoTransformGivesItsBonusesAndRechargesFor90sAfterEnding")
    public static void tierTwoTransformGivesItsBonusesAndRechargesFor90sAfterEnding(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2");
        character().use(p, "yd_transform", AbilityAction.USE);
        check(h, YuanDiAbilityHandler.isTransformed(p), "transformed");
        eq(h, 2, YuanDiAbilityHandler.getTransformTier(p), "tier");
        eq(h, 0.18, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:yd_transform_dmg"), "+18% damage");
        eq(h, 0.20, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_transform_speed"), "+20% speed");
        eq(h, 24.0, modifier(p, PHYSICAL_RESISTANCE(), "jttw:yd_transform_pres"), "+24 physical resistance");
        eq(h, 0.40, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "jttw:yd_transform_kb"), "+40% KB resist");

        character().use(p, "yd_transform", AbilityAction.USE);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "second press ends it");
        eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:yd_transform_dmg"), "bonuses removed");

        character().use(p, "yd_transform", AbilityAction.USE);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "recharging");
        ticks(p, 1799);
        YuanDiAbilityHandler.toggleTransform(p);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "still recharging one tick early");
        ticks(p, 1);
        YuanDiAbilityHandler.toggleTransform(p);
        check(h, YuanDiAbilityHandler.isTransformed(p), "recharged after 1800 ticks");
        endIfTransformed(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierTwoTransformLastsSixtySeconds")
    public static void tierTwoTransformLastsSixtySeconds(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2");
        YuanDiAbilityHandler.toggleTransform(p);
        ticks(p, 1199);
        check(h, YuanDiAbilityHandler.isTransformed(p), "still transformed at 1199 ticks");
        ticks(p, 1);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "faded at 1200 ticks");
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_transform_speed"), "bonuses removed");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierThreeTransformIsUnlimitedButCostsTwoQiPerSecond")
    public static void tierThreeTransformIsUnlimitedButCostsTwoQiPerSecond(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_transform_3");
        qi(p, 1000f, 1000f);
        YuanDiAbilityHandler.toggleTransform(p);
        eq(h, 0.30, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:yd_transform_dmg"), "+30% damage");
        eq(h, 0.25, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_transform_speed"), "+25% speed");
        eq(h, 32.0, modifier(p, PHYSICAL_RESISTANCE(), "jttw:yd_transform_pres"), "+32 physical resistance");
        eq(h, 0.50, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "jttw:yd_transform_kb"), "+50% KB resist");
        ticks(p, 1400);
        check(h, YuanDiAbilityHandler.isTransformed(p), "past tier 2's 60 s limit");
        near(h, 1000f - 2f * 70, QiData.getCurrent(p), "2 Qi per 20 ticks");

        QiData.setCurrent(p, 1.5f);
        ticks(p, 20);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "ends when Qi cannot pay");
        YuanDiAbilityHandler.toggleTransform(p);
        check(h, YuanDiAbilityHandler.isTransformed(p), "no recharge after the mastered form");
        endIfTransformed(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierOneAwakensBelowThirtyPercentHealthForTenSeconds")
    public static void tierOneAwakensBelowThirtyPercentHealthForTenSeconds(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1");
        p.setHealth(p.getMaxHealth() * 0.31f);
        ticks(p, 1);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "31% health: no awakening");
        p.setHealth(p.getMaxHealth() * 0.29f);
        ticks(p, 1);
        check(h, YuanDiAbilityHandler.isTransformed(p), "29% health: awakened");
        eq(h, 1, YuanDiAbilityHandler.getTransformTier(p), "tier");
        eq(h, 0.10, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:yd_transform_dmg"), "+10% damage");
        eq(h, 0.15, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_transform_speed"), "+15% speed");
        eq(h, 16.0, modifier(p, PHYSICAL_RESISTANCE(), "jttw:yd_transform_pres"), "+16 physical resistance");
        eq(h, 0.30, modifier(p, Attributes.KNOCKBACK_RESISTANCE, "jttw:yd_transform_kb"), "+30% KB resist");
        ticks(p, 200);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "faded after 200 ticks");
        ticks(p, 599);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "30 s cooldown: not yet");
        ticks(p, 1);
        check(h, YuanDiAbilityHandler.isTransformed(p), "awakens again once the cooldown is over");
        endIfTransformed(p);
        h.succeed();
    }

    // ── Sovereign's Command (fires on transform) ────────────────────

    @GameTest(template = PLATFORM, batch = "transformWithoutCommandNodeDoesNotSlowAnything")
    public static void transformWithoutCommandNodeDoesNotSlowAnything(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        YuanDiAbilityHandler.toggleTransform(p);
        check(h, !husk.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "no pulse");
        endIfTransformed(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "commandTierOneSlowsEverythingNearbyForThreeSeconds")
    public static void commandTierOneSlowsEverythingNearbyForThreeSeconds(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_command_1");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        Pig pig = mob(h, EntityType.PIG, 4, 4);
        YuanDiAbilityHandler.toggleTransform(p);
        eq(h, 2, amp(husk, MobEffects.MOVEMENT_SLOWDOWN), "husk Slowness III");
        eq(h, 60, duration(husk, MobEffects.MOVEMENT_SLOWDOWN), "for 60 ticks");
        eq(h, 2, amp(pig, MobEffects.MOVEMENT_SLOWDOWN), "pig Slowness III");
        eq(h, -1, amp(husk, MobEffects.WEAKNESS), "no Weakness at tier 1");
        eq(h, "", husk.getPersistentData().getString(DOMINATED_BY), "no domination at tier 1");
        endIfTransformed(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "commandTierTwoWeakensAndDominatesOnlyMonsters")
    public static void commandTierTwoWeakensAndDominatesOnlyMonsters(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_command_1", "yd_command_2");
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        Pig pig = mob(h, EntityType.PIG, 4, 4);
        YuanDiAbilityHandler.toggleTransform(p);
        eq(h, 1, amp(husk, MobEffects.WEAKNESS), "Weakness II");
        eq(h, 200, duration(husk, MobEffects.WEAKNESS), "for 10 s");
        eq(h, p.getStringUUID(), husk.getPersistentData().getString(DOMINATED_BY), "husk dominated");
        check(h, com.jttw.engine.world.PrivateGlow.glowsFor(p, husk), "dominated mobs glow, for him");
        eq(h, "", pig.getPersistentData().getString(DOMINATED_BY), "a pig is not a monster");
        endIfTransformed(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "commandTierThreeDominatesAnyMob")
    public static void commandTierThreeDominatesAnyMob(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_command_1", "yd_command_2", "yd_command_3");
        Pig pig = mob(h, EntityType.PIG, 4, 4);
        YuanDiAbilityHandler.toggleTransform(p);
        eq(h, p.getStringUUID(), pig.getPersistentData().getString(DOMINATED_BY), "pig dominated");
        endIfTransformed(p);
        h.succeed();
    }

    /** Like a tamed wolf: a dominated mob attacks what Yuan Di attacks. */
    @GameTest(template = PLATFORM, batch = "dominatedMobsAttackWhatYuanDiAttacksThenWearOff")
    public static void dominatedMobsAttackWhatYuanDiAttacksThenWearOff(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_command_1", "yd_command_2");
        Husk dominated = mob(h, EntityType.HUSK, 0, 0);
        dominated.setTarget(p);
        YuanDiAbilityHandler.toggleTransform(p);
        // arrives after the pulse, so it is not dominated
        Husk rival = mob(h, EntityType.HUSK, 4, 0);
        ticks(p, 10);
        eq(h, null, dominated.getTarget(), "stops hunting Yuan Di, and has no quarrel of its own");
        p.setLastHurtMob(rival);
        ticks(p, 10);
        check(h, dominated.getTarget() == rival, "attacks the husk Yuan Di hit, target " + dominated.getTarget());
        ticks(p, 180);
        eq(h, "", dominated.getPersistentData().getString(DOMINATED_BY), "domination expired after 200 ticks");
        endIfTransformed(p);
        h.succeed();
    }

    /** ...and what attacks Yuan Di. */
    @GameTest(template = PLATFORM, batch = "dominatedMobsDefendYuanDi")
    public static void dominatedMobsDefendYuanDi(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_command_1", "yd_command_2");
        Husk dominated = mob(h, EntityType.HUSK, 0, 0);
        YuanDiAbilityHandler.toggleTransform(p);
        Husk attacker = mob(h, EntityType.HUSK, 4, 0);
        p.setLastHurtByMob(attacker);
        ticks(p, 10);
        check(h, dominated.getTarget() == attacker, "turns on Yuan Di's attacker, target " + dominated.getTarget());
        endIfTransformed(p);
        h.succeed();
    }

    /** Bystanders, Yuan Di's other dominated mobs, and far-off foes are left alone. */
    @GameTest(template = PLATFORM, batch = "dominatedMobsLeaveBystandersAndEachOtherAlone")
    public static void dominatedMobsLeaveBystandersAndEachOtherAlone(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_command_1", "yd_command_2");
        Husk dominated = mob(h, EntityType.HUSK, 0, 0);
        Husk fellow = mob(h, EntityType.HUSK, 1, 0);
        YuanDiAbilityHandler.toggleTransform(p);
        eq(h, p.getStringUUID(), fellow.getPersistentData().getString(DOMINATED_BY), "fellow dominated too");
        Pig pig = mob(h, EntityType.PIG, 4, 0);
        ticks(p, 10);
        check(h, dominated.getTarget() != pig, "no quarrel with the pig");
        p.setLastHurtMob(fellow);
        ticks(p, 10);
        check(h, dominated.getTarget() != fellow, "never turns on another of Yuan Di's dominated mobs");
        endIfTransformed(p);
        h.succeed();
    }

    // ── Demon's Breath (slot 1, held) ───────────────────────────────

    /** A pig two blocks ahead, with the player aiming at its middle. */
    private static Pig pigInBreath(GameTestHelper h, ServerPlayer p) {
        Pig pig = mob(h, EntityType.PIG, 2, 4);
        p.setYRot(0f);
        p.setXRot(30f);
        return pig;
    }

    @GameTest(template = PLATFORM, batch = "demonsBreathNeedsTheNode")
    public static void demonsBreathNeedsTheNode(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        Pig pig = pigInBreath(h, p);
        List<Hit> hits = watch(pig);
        character().use(p, "yd_breath", AbilityAction.START);
        ticks(p, 5);
        check(h, hits.isEmpty(), "no breath");
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_breath_slow"), "not rooted");
        near(h, 100f, QiData.getCurrent(p), "Qi");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "demonsBreathRootsDrainsAndWithersWhatItTouches")
    public static void demonsBreathRootsDrainsAndWithersWhatItTouches(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_breath_1");
        Pig pig = pigInBreath(h, p);
        List<Hit> hits = watch(pig);
        character().use(p, "yd_breath", AbilityAction.START);
        eq(h, -1.0, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_breath_slow"), "cannot move");
        ticks(p, 10);
        near(h, 98f, QiData.getCurrent(p), "0.2 Qi per tick");
        List<Hit> magic = of(hits, DamageTypes.MAGIC);
        eq(h, 10, magic.size(), "one breath hit per tick");
        near(h, 0.4, magic.get(0).amount(), "damage per tick");
        eq(h, 0, amp(pig, MobEffects.WITHER), "Wither I");
        character().use(p, "yd_breath", AbilityAction.STOP);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_breath_slow"), "free to move");
        ticks(p, 5);
        eq(h, 10, of(hits, DamageTypes.MAGIC).size(), "no more hits after release");
        near(h, 98f, QiData.getCurrent(p), "no more drain after release");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "demonsBreathTierThreeHitsHarderCostsLessAndWithersMore")
    public static void demonsBreathTierThreeHitsHarderCostsLessAndWithersMore(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_breath_1", "yd_breath_2", "yd_breath_3");
        Pig pig = pigInBreath(h, p);
        List<Hit> hits = watch(pig);
        YuanDiAbilityHandler.startBreath(p);
        ticks(p, 10);
        near(h, 98.6f, QiData.getCurrent(p), "0.14 Qi per tick from tier 2");
        // 0.4 x 1.3 x 1.3
        near(h, 0.676, of(hits, DamageTypes.MAGIC).get(0).amount(), "damage per tick");
        eq(h, 2, amp(pig, MobEffects.WITHER), "Wither III");
        YuanDiAbilityHandler.stopBreath(p);
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "demonsBreathStopsWhenQiRunsOut")
    public static void demonsBreathStopsWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_breath_1");
        QiData.setCurrent(p, 0.5f);
        YuanDiAbilityHandler.startBreath(p);
        ticks(p, 3);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_breath_slow"), "breath ended, root lifted");
        QiData.setCurrent(p, 50f);
        ticks(p, 3);
        near(h, 50f, QiData.getCurrent(p), "no drain once ended");
        h.succeed();
    }

    // ── Primordial Smite (slot 2) ───────────────────────────────────

    @GameTest(template = PLATFORM, batch = "smiteNeedsTheNodeAndTwentyQi")
    public static void smiteNeedsTheNodeAndTwentyQi(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        character().use(p, "yd_smite", AbilityAction.USE);
        check(h, hits.isEmpty(), "no node");
        grant(p, "yd_smite_1");
        QiData.setCurrent(p, 19f);
        character().use(p, "yd_smite", AbilityAction.USE);
        check(h, hits.isEmpty(), "19 Qi");
        near(h, 19f, QiData.getCurrent(p), "nothing spent");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "smiteTierOneHitsWithinFiveBlocksAndHasA20SecondCooldown")
    public static void smiteTierOneHitsWithinFiveBlocksAndHasA20SecondCooldown(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_smite_1");
        PlayerLevel.setLevel(p, 20); // the shockwave's damage is set at level 1, scaled by his power
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4); // exactly 2 blocks away
        List<Hit> hits = watch(golem);
        YuanDiAbilityHandler.usePrimordialSmite(p);
        near(h, 80f, QiData.getCurrent(p), "20 Qi");
        List<Hit> blows = of(hits, DamageTypes.MOB_ATTACK);
        eq(h, 1, blows.size(), "one blow");
        // 8 damage at level 1, scaled to level 20, falloff 1 - 2/5
        near(h, LevelPower.scaled(p, 8.0) * 0.6f, blows.get(0).amount(), "damage at 2 blocks, level 20");
        check(h, golem.getDeltaMovement().z > 0 && golem.getDeltaMovement().y > 0, "launched away");
        eq(h, -1, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "no slow field at tier 1");

        YuanDiAbilityHandler.usePrimordialSmite(p);
        near(h, 80f, QiData.getCurrent(p), "on cooldown");
        ticks(p, 399);
        YuanDiAbilityHandler.usePrimordialSmite(p);
        near(h, 80f, QiData.getCurrent(p), "still on cooldown at 399 ticks");
        ticks(p, 1);
        YuanDiAbilityHandler.usePrimordialSmite(p);
        near(h, 60f, QiData.getCurrent(p), "ready after 400 ticks");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "smiteTierTwoAndThreeHitHarderAndSlowOrStun")
    public static void smiteTierTwoAndThreeHitHarderAndSlowOrStun(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_smite_1", "yd_smite_2");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        YuanDiAbilityHandler.usePrimordialSmite(p);
        // 12 damage, falloff 1 - 2/8
        near(h, 9.0, of(hits, DamageTypes.MOB_ATTACK).get(0).amount(), "tier 2 damage at 2 blocks");
        eq(h, 1, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "Slowness II field");
        eq(h, 100, duration(golem, MobEffects.MOVEMENT_SLOWDOWN), "for 5 s");

        ServerPlayer p3 = yuanDi(h);
        grant(p3, "yd_smite_1", "yd_smite_2", "yd_smite_3");
        IronGolem golem3 = mob(h, EntityType.IRON_GOLEM, 4, 2);
        List<Hit> hits3 = watch(golem3);
        YuanDiAbilityHandler.usePrimordialSmite(p3);
        // 16 damage, falloff 1 - 2/12
        near(h, 16.0 * (1 - 2.0 / 12.0), of(hits3, DamageTypes.MOB_ATTACK).get(0).amount(), "tier 3 damage at 2 blocks");
        eq(h, 4, amp(golem3, MobEffects.MOVEMENT_SLOWDOWN), "stunned: Slowness V");
        check(h, golem3.hasEffect(MobEffects.BLINDNESS), "stunned: blind");
        eq(h, 3, amp(golem3, MobEffects.DIG_SLOWDOWN), "stunned: Mining Fatigue IV");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "smiteInMasteredFormHitsForTwentyFourWithHalfCooldown")
    public static void smiteInMasteredFormHitsForTwentyFourWithHalfCooldown(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_transform_3",
                "yd_smite_1", "yd_smite_2", "yd_smite_3");
        YuanDiAbilityHandler.toggleTransform(p);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        YuanDiAbilityHandler.usePrimordialSmite(p);
        // 24 damage, falloff 1 - 2/18
        near(h, 24.0 * (1 - 2.0 / 18.0), of(hits, DamageTypes.MOB_ATTACK).get(0).amount(), "damage in form");
        QiData.setCurrent(p, 100f);
        ticks(p, 119);
        float waiting = QiData.getCurrent(p);
        YuanDiAbilityHandler.usePrimordialSmite(p);
        near(h, waiting, QiData.getCurrent(p), "on cooldown at 119 ticks");
        ticks(p, 1);
        float before = QiData.getCurrent(p);
        YuanDiAbilityHandler.usePrimordialSmite(p);
        near(h, before - 20f, QiData.getCurrent(p), "120-tick cooldown in form");
        endIfTransformed(p);
        h.succeed();
    }

    // ── Abyssal Gravity (slot 3) ────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "abyssalGravityCosts15QiPullsMobsAndHasA20SecondCooldown")
    public static void abyssalGravityCosts15QiPullsMobsAndHasA20SecondCooldown(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        character().use(p, "yd_gravity", AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "needs the node");
        grant(p, "yd_gravity_1");
        p.setXRot(90f); // the well opens on the floor at the player's feet
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        character().use(p, "yd_gravity", AbilityAction.USE);
        near(h, 85f, QiData.getCurrent(p), "15 Qi");
        ticks(p, 1);
        Vec3 pull = golem.getDeltaMovement();
        check(h, pull.z < 0, "golem pulled toward the well (north), motion " + pull);
        YuanDiAbilityHandler.useAbyssalGravity(p);
        near(h, 85f, QiData.getCurrent(p), "on cooldown");
        ticks(p, 398); // 399 ticks since the cast
        YuanDiAbilityHandler.useAbyssalGravity(p);
        near(h, 85f, QiData.getCurrent(p), "still on cooldown at 400 - 1");
        ticks(p, 1);
        YuanDiAbilityHandler.useAbyssalGravity(p);
        near(h, 70f, QiData.getCurrent(p), "ready after 400 ticks");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "abyssalGravityTierThreeCollapsesForThirtyPercentMaxHealth")
    public static void abyssalGravityTierThreeCollapsesForThirtyPercentMaxHealth(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_gravity_1", "yd_gravity_2", "yd_gravity_3");
        PlayerLevel.setLevel(p, 20); // the pulses are set at level 1, scaled by his power; the collapse is not
        p.setXRot(90f);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        YuanDiAbilityHandler.useAbyssalGravity(p);
        ticks(p, 59);
        List<Hit> pulses = of(hits, DamageTypes.MAGIC);
        check(h, !pulses.isEmpty(), "tier 2+ well deals damage while pulling");
        near(h, 2.0 * LevelPower.of(p), pulses.get(0).amount(), "2 magic per second at level 1, scaled by his power at 20");
        int before = pulses.size();
        ticks(p, 1);
        List<Hit> after = of(hits, DamageTypes.MAGIC);
        eq(h, before + 1, after.size(), "collapse hit when the well expires at 60 ticks");
        near(h, 30.0, after.get(after.size() - 1).amount(), "30% of the golem's 100 max health");
        h.succeed();
    }

    // ── Primordial Step (passive: sprint + sneak) ───────────────────

    @GameTest(template = PLATFORM, batch = "primordialStepCostsFiveQiAndTierTwoFrightensTheOrigin")
    public static void primordialStepCostsFiveQiAndTierTwoFrightensTheOrigin(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_step_1", "yd_step_2");
        Husk husk = mob(h, EntityType.HUSK, 2, 4);
        p.setXRot(-90f);
        p.setSprinting(true);
        ticks(p, 1);
        near(h, 100f, QiData.getCurrent(p), "sprinting alone does not step");
        p.setShiftKeyDown(true);
        ticks(p, 1);
        near(h, 95f, QiData.getCurrent(p), "sprint + sneak steps for 5 Qi");
        eq(h, 2, amp(husk, MobEffects.MOVEMENT_SLOWDOWN), "Slowness III at the origin");
        ticks(p, 119);
        near(h, 95f, QiData.getCurrent(p), "tier 2 cooldown is 120 ticks");
        ticks(p, 1);
        near(h, 90f, QiData.getCurrent(p), "steps again at 120 ticks");
        h.succeed();
    }

    // ── Night Power (passive) ───────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "nightPowerOnlyAppliesAtNight")
    public static void nightPowerOnlyAppliesAtNight(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_night_1", "yd_night_2", "yd_night_3");
        ServerLevel level = h.getLevel();
        long time = level.getDayTime();
        try {
            level.setDayTime(18000); // midnight
            level.updateSkyBrightness();
            p.tickCount = 39;
            ticks(p, 1);
            eq(h, 0.30, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:yd_night_dmg"), "tier 3 night damage");
            eq(h, 0.20, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_night_speed"), "tier 3 night speed");
            eq(h, 24.0, modifier(p, PHYSICAL_RESISTANCE(), "jttw:yd_night_pres"), "tier 3 night physical resistance");
            check(h, p.hasEffect(MobEffects.NIGHT_VISION), "night vision");

            level.setDayTime(6000); // noon
            level.updateSkyBrightness();
            p.tickCount = 79;
            ticks(p, 1);
            eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:yd_night_dmg"), "gone by day");
        } finally {
            level.setDayTime(time);
            level.updateSkyBrightness();
        }
        h.succeed();
    }

    // ── Ancient Dominion (passive) ──────────────────────────────────

    @GameTest(template = PLATFORM, batch = "ancientDominionMarksStruckMobsAndSoftensTheirHits")
    public static void ancientDominionMarksStruckMobsAndSoftensTheirHits(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        online(h, p);
        Husk husk = mob(h, EntityType.HUSK, 0, 0);
        var ds = h.getLevel().damageSources();

        YuanDiAbilityHandler.onYuanDiDealtDamage(new LivingHurtEvent(husk, ds.playerAttack(p), 4f));
        check(h, !husk.getPersistentData().contains("jttw_dominion_by"), "no mark without the node");

        grant(p, "yd_dominion_1");
        YuanDiAbilityHandler.onYuanDiDealtDamage(new LivingHurtEvent(husk, ds.playerAttack(p), 4f));
        eq(h, p.getStringUUID(), husk.getPersistentData().getString("jttw_dominion_by"), "marked");
        eq(h, 200, husk.getPersistentData().getInt("jttw_dominion_ticks"), "for 10 s");

        LivingHurtEvent back = new LivingHurtEvent(p, ds.mobAttack(husk), 10f);
        YuanDiAbilityHandler.onDominionDamageReduction(back);
        near(h, 8.5f, back.getAmount(), "15% less damage to Yuan Di");

        ServerPlayer ally = yuanDi(h);
        LivingHurtEvent allyHit = new LivingHurtEvent(ally, ds.mobAttack(husk), 10f);
        YuanDiAbilityHandler.onDominionDamageReduction(allyHit);
        near(h, 10f, allyHit.getAmount(), "tier 1 protects only Yuan Di");

        grant(p, "yd_dominion_2");
        LivingHurtEvent allyHit2 = new LivingHurtEvent(ally, ds.mobAttack(husk), 10f);
        YuanDiAbilityHandler.onDominionDamageReduction(allyHit2);
        near(h, 8.5f, allyHit2.getAmount(), "tier 2 protects players within 10 blocks");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "yuanDiKeysIgnoreTheWrongAction")
    public static void yuanDiKeysIgnoreTheWrongAction(GameTestHelper h) {
        ServerPlayer p = yuanDi(h);
        grant(p, "yd_transform_1", "yd_transform_2", "yd_breath_1", "yd_smite_1", "yd_gravity_1");
        PlayableCharacter c = character();
        c.use(p, "yd_transform", AbilityAction.START);
        check(h, !YuanDiAbilityHandler.isTransformed(p), "transform ignores START");
        c.use(p, "yd_breath", AbilityAction.USE);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_breath_slow"), "breath ignores USE");
        c.use(p, "yd_smite", AbilityAction.START);
        c.use(p, "yd_gravity", AbilityAction.START);
        near(h, 100f, QiData.getCurrent(p), "smite and gravity ignore START");
        c.use(p, "yd_breath", AbilityAction.START);
        eq(h, -1.0, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_breath_slow"), "breath starts on START");
        c.use(p, "yd_breath", AbilityAction.STOP);
        eq(h, null, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:yd_breath_slow"), "and stops on STOP");
        h.succeed();
    }
}
