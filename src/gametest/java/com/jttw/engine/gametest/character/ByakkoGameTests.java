package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.byakko.ByakkoAbilityHandler;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Byakko, the White Tiger: Predator's Leap, Territorial Roar, bleed, velocity, instinct, passives. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ByakkoGameTests {

    private static ServerPlayer byakko(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.WHITE_TIGER);
        qi(p, 100f, 100f);
        return p;
    }

    /** Attack fixed at 4 so STR-scaled damage is predictable. */
    private static void attackFour(ServerPlayer p) {
        var atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
        atk.removeModifiers();
        atk.setBaseValue(4.0);
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.WHITE_TIGER.id);
    }

    private static void ticks(ServerPlayer p, int n) {
        tick(p, ByakkoAbilityHandler::onPlayerTick, n);
    }

    private static void land(ServerPlayer p) {
        p.setOnGround(false);
        ticks(p, 1);
        p.setOnGround(true);
        ticks(p, 1);
    }

    // ── Innate passive ──────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "byakkoGetsFasterAndStepsHigherWithLevel")
    public static void byakkoGetsFasterAndStepsHigherWithLevel(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        PlayerLevel.setLevel(p, 100);
        LevelManager.reapplyStats(p);
        // constants: +0.008 speed per level (80% at 100), step 1.0 + 0.01 per level (2.0 at 100)
        near(h, 0.8, modifier(p, Attributes.MOVEMENT_SPEED, "jttw:byakko_speed"), "speed bonus at 100");
        near(h, 2.0, modifier(p, ForgeMod.STEP_HEIGHT_ADDITION.get(), "jttw:byakko_step"), "step bonus at 100");
        h.succeed();
    }

    // ── Predator's Leap (slot 0) ────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "leapNeedsTheNodeAndTenQi")
    public static void leapNeedsTheNodeAndTenQi(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        character().activate(p, 0, AbilityAction.USE);
        near(h, 0.0, p.getDeltaMovement().length(), "no node: no leap");
        grant(p, "bk_leap_1");
        QiData.setCurrent(p, 9f);
        character().activate(p, 0, AbilityAction.USE);
        near(h, 9f, QiData.getCurrent(p), "9 Qi: refused");
        near(h, 0.0, p.getDeltaMovement().length(), "no launch");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "leapLaunchesForTenQiAndCrashesDownOnLanding")
    public static void leapLaunchesForTenQiAndCrashesDownOnLanding(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_leap_1");
        attackFour(p);
        p.setYRot(0f);
        p.setXRot(0f);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 3); // 1 block away
        List<Hit> hits = watch(golem);
        character().activate(p, 0, AbilityAction.USE);
        near(h, 90f, QiData.getCurrent(p), "10 Qi");
        near(h, 2.0, p.getDeltaMovement().z, "launched forward at 2");
        check(h, hits.isEmpty(), "no impact before landing");
        land(p);
        List<Hit> blows = of(hits, DamageTypes.PLAYER_ATTACK);
        eq(h, 1, blows.size(), "landing impact");
        // tier 1: 5 + attack 4 x 0.5 = 7, falloff 1 - 1/3
        near(h, 7.0 * (2.0 / 3.0), blows.get(0).amount(), "impact damage");
        land(p);
        eq(h, 1, of(hits, DamageTypes.PLAYER_ATTACK).size(), "only one impact per leap");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "leapTierThreeHitsWiderAndStuns")
    public static void leapTierThreeHitsWiderAndStuns(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_leap_1", "bk_leap_2", "bk_leap_3");
        attackFour(p);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4); // 2 blocks away
        List<Hit> hits = watch(golem);
        ByakkoAbilityHandler.castLeap(p);
        land(p);
        // tier 3: 12 + attack 4 x 1.0 = 16, falloff 1 - 2/7
        near(h, 16.0 * (5.0 / 7.0), of(hits, DamageTypes.PLAYER_ATTACK).get(0).amount(), "impact damage");
        eq(h, 3, amp(golem, MobEffects.MOVEMENT_SLOWDOWN), "stun: Slowness IV");
        check(h, golem.hasEffect(MobEffects.BLINDNESS), "stun: blind");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 150, batch = "leapHasAThreeSecondCooldown")
    public static void leapHasAThreeSecondCooldown(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_leap_1");
        ByakkoAbilityHandler.castLeap(p);
        ByakkoAbilityHandler.castLeap(p);
        near(h, 90f, QiData.getCurrent(p), "second leap refused");
        h.runAfterDelay(55, () -> {
            ByakkoAbilityHandler.castLeap(p);
            near(h, 90f, QiData.getCurrent(p), "still cooling down before 60 ticks");
            h.runAfterDelay(6, () -> {
                ByakkoAbilityHandler.castLeap(p);
                near(h, 80f, QiData.getCurrent(p), "ready after 60 ticks");
                h.succeed();
            });
        });
    }

    // ── Territorial Roar (slot 1) ───────────────────────────────────

    @GameTest(template = PLATFORM, batch = "roarCostsEightQiAndPinsDownEverythingWithinEightBlocks")
    public static void roarCostsEightQiAndPinsDownEverythingWithinEightBlocks(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        character().activate(p, 1, AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "needs the node");
        grant(p, "bk_roar_1");
        Husk husk = mob(h, EntityType.HUSK, 2, 4);
        husk.setTarget(p);
        Pig hound = mob(h, EntityType.PIG, 0, 0);
        hound.setCustomName(Component.literal("jttw_xtq:" + p.getUUID()));
        character().activate(p, 1, AbilityAction.USE);
        near(h, 92f, QiData.getCurrent(p), "8 Qi");
        eq(h, 5, amp(husk, MobEffects.MOVEMENT_SLOWDOWN), "Slowness VI");
        eq(h, 60, duration(husk, MobEffects.MOVEMENT_SLOWDOWN), "for 3 s");
        check(h, husk.getTarget() == null, "forgets its target");
        near(h, 1.5, husk.getDeltaMovement().z, "thrown back");
        check(h, !hound.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "the player's own companion is spared");
        eq(h, -1, amp(p, MobEffects.MOVEMENT_SPEED), "no Speed below tier 2");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "roarTierTwoStripsBuffsAndSpeedsTheTiger")
    public static void roarTierTwoStripsBuffsAndSpeedsTheTiger(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_roar_1", "bk_roar_2");
        Husk husk = mob(h, EntityType.HUSK, 2, 4);
        husk.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200));
        husk.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200));
        ByakkoAbilityHandler.castRoar(p);
        check(h, !husk.hasEffect(MobEffects.REGENERATION), "buff stripped");
        check(h, husk.hasEffect(MobEffects.WEAKNESS), "debuff kept");
        eq(h, 1, amp(p, MobEffects.MOVEMENT_SPEED), "Speed II");
        eq(h, 100, duration(p, MobEffects.MOVEMENT_SPEED), "for 5 s");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 700, batch = "roarHasAThirtySecondCooldown")
    public static void roarHasAThirtySecondCooldown(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_roar_1");
        ByakkoAbilityHandler.castRoar(p);
        ByakkoAbilityHandler.castRoar(p);
        near(h, 92f, QiData.getCurrent(p), "second roar refused");
        h.runAfterDelay(601, () -> {
            ByakkoAbilityHandler.castRoar(p);
            near(h, 84f, QiData.getCurrent(p), "ready after 600 ticks");
            h.succeed();
        });
    }

    // ── Savage Bleed (passive) ──────────────────────────────────────

    private static void strike(GameTestHelper h, ServerPlayer p, IronGolem target, float amount) {
        ByakkoAbilityHandler.onLivingHurt(new LivingHurtEvent(target, h.getLevel().damageSources().playerAttack(p), amount));
    }

    @GameTest(template = PLATFORM, batch = "bleedDealsDamageEverySecondScaledByAttack")
    public static void bleedDealsDamageEverySecondScaledByAttack(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        attackFour(p);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        strike(h, p, golem, 2f);
        ticks(p, 40);
        check(h, hits.isEmpty(), "no bleed without the node");

        grant(p, "bk_bleed_1");
        strike(h, p, golem, 2f);
        ticks(p, 20);
        eq(h, 1, hits.size(), "first bleed tick after a second");
        // tier 1: 1 + attack 4 x 0.10
        near(h, 1.4, hits.get(0).amount(), "bleed damage");
        ticks(p, 100);
        int total = hits.size();
        ticks(p, 40);
        eq(h, total, hits.size(), "bleed has run out");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "bleedTierThreeSpreadsAtHalfStrength")
    public static void bleedTierThreeSpreadsAtHalfStrength(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        attackFour(p);
        grant(p, "bk_bleed_1", "bk_bleed_2", "bk_bleed_3");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        IronGolem neighbour = mob(h, EntityType.IRON_GOLEM, 0, 2);
        List<Hit> hits = watch(golem);
        List<Hit> spread = watch(neighbour);
        strike(h, p, golem, 2f);
        ticks(p, 20);
        // tier 3: 3 + attack 4 x 0.30
        near(h, 4.2, hits.get(0).amount(), "bleed on the struck golem");
        near(h, 2.1, spread.get(0).amount(), "half strength on its neighbour");
        h.succeed();
    }

    /**
     * Suspected bug: a tier-1 bleed is documented as lasting 3 seconds (60 ticks, 1 hit per
     * second), but the countdown is decremented before the hit, so only two hits land.
     */
    @GameTest(template = PLATFORM, required = false, batch = "bleedTierOneHitsThreeTimes")
    public static void bleedTierOneHitsThreeTimes(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_bleed_1");
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        List<Hit> hits = watch(golem);
        strike(h, p, golem, 2f);
        ticks(p, 100);
        eq(h, 3, hits.size(), "bleed hits over 3 s");
        h.succeed();
    }

    /**
     * Suspected bug: each bleed tick is dealt as a fresh attack by the Byakko, which re-runs the
     * tier-3 spread; two neighbours can keep re-infecting each other long after the 5-second
     * bleed should have ended. Runs on real ticks so i-frames and hurt events behave as in play.
     */
    @GameTest(template = PLATFORM, required = false, timeoutTicks = 400, batch = "bleedSpreadDiesOutAfterFiveSeconds")
    public static void bleedSpreadDiesOutAfterFiveSeconds(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        online(h, p);
        grant(p, "bk_bleed_1", "bk_bleed_2", "bk_bleed_3");
        IronGolem a = mob(h, EntityType.IRON_GOLEM, 0, 0);
        IronGolem b = mob(h, EntityType.IRON_GOLEM, 0, 2);
        List<Hit> hitsA = watch(a);
        List<Hit> hitsB = watch(b);
        strike(h, p, a, 2f);
        h.onEachTick(() -> ticks(p, 1));
        h.runAfterDelay(160, () -> {
            // bleed is dealt as the (online) Byakko's player attack; ignore any other damage
            int before = of(hitsA, DamageTypes.PLAYER_ATTACK).size() + of(hitsB, DamageTypes.PLAYER_ATTACK).size();
            h.runAfterDelay(160, () -> {
                int after = of(hitsA, DamageTypes.PLAYER_ATTACK).size() + of(hitsB, DamageTypes.PLAYER_ATTACK).size();
                eq(h, before, after, "bleed hits between 8 s and 16 s after the strike");
                h.succeed();
            });
        });
    }

    // ── Velocity Strike and Predator's Instinct (passives) ──────────

    @GameTest(template = PLATFORM, batch = "velocityStrikeAddsDamageForSpeed")
    public static void velocityStrikeAddsDamageForSpeed(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 2, 4);
        p.setDeltaMovement(new Vec3(0.5, 0, 0)); // 10 blocks per second
        LivingHurtEvent none = new LivingHurtEvent(golem, h.getLevel().damageSources().playerAttack(p), 10f);
        ByakkoAbilityHandler.onLivingHurt(none);
        near(h, 10f, none.getAmount(), "no node");
        grant(p, "bk_velocity_1");
        LivingHurtEvent t1 = new LivingHurtEvent(golem, h.getLevel().damageSources().playerAttack(p), 10f);
        ByakkoAbilityHandler.onLivingHurt(t1);
        near(h, 15f, t1.getAmount(), "tier 1: 5% per block/s");
        grant(p, "bk_velocity_2");
        LivingHurtEvent t2 = new LivingHurtEvent(golem, h.getLevel().damageSources().playerAttack(p), 10f);
        ByakkoAbilityHandler.onLivingHurt(t2);
        near(h, 20f, t2.getAmount(), "tier 2: 10% per block/s");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "instinctMarksWoundedPreyAndTierTwoDoublesTheFirstHit")
    public static void instinctMarksWoundedPreyAndTierTwoDoublesTheFirstHit(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_instinct_1");
        IronGolem wounded = mob(h, EntityType.IRON_GOLEM, 2, 4);
        IronGolem healthy = mob(h, EntityType.IRON_GOLEM, 0, 0);
        wounded.setHealth(wounded.getMaxHealth() * 0.4f);
        ticks(p, 20);
        check(h, wounded.hasEffect(MobEffects.GLOWING), "below half health: glowing");
        check(h, !healthy.hasEffect(MobEffects.GLOWING), "healthy prey is not marked");

        LivingHurtEvent t1 = new LivingHurtEvent(wounded, h.getLevel().damageSources().playerAttack(p), 10f);
        ByakkoAbilityHandler.onLivingHurt(t1);
        near(h, 10f, t1.getAmount(), "tier 1 only marks");

        grant(p, "bk_instinct_2");
        LivingHurtEvent first = new LivingHurtEvent(wounded, h.getLevel().damageSources().playerAttack(p), 10f);
        ByakkoAbilityHandler.onLivingHurt(first);
        near(h, 20f, first.getAmount(), "tier 2: first hit doubled");
        check(h, !wounded.hasEffect(MobEffects.GLOWING), "mark consumed");
        ticks(p, 20);
        check(h, !wounded.hasEffect(MobEffects.GLOWING), "not re-marked during the 5 s cooldown");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "eatingClearsHungerPoisonAndNausea")
    public static void eatingClearsHungerPoisonAndNausea(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        p.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600));
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 600));
        p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600));
        ItemStack flesh = new ItemStack(Items.ROTTEN_FLESH);
        ByakkoAbilityHandler.onItemUseFinish(new LivingEntityUseItemEvent.Finish(p, flesh, 0, flesh));
        check(h, !p.hasEffect(MobEffects.HUNGER), "hunger cleared");
        check(h, !p.hasEffect(MobEffects.POISON), "poison cleared");
        check(h, !p.hasEffect(MobEffects.CONFUSION), "nausea cleared");
        check(h, p.hasEffect(MobEffects.WEAKNESS), "other effects untouched");

        ServerPlayer other = as(h, ReincarnationOutcome.DRAGON);
        other.addEffect(new MobEffectInstance(MobEffects.HUNGER, 600));
        ByakkoAbilityHandler.onItemUseFinish(new LivingEntityUseItemEvent.Finish(other, flesh, 0, flesh));
        check(h, other.hasEffect(MobEffects.HUNGER), "only Byakko");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "byakkoKeysOnlyReactToUseOnTheirSlots")
    public static void byakkoKeysOnlyReactToUseOnTheirSlots(GameTestHelper h) {
        ServerPlayer p = byakko(h);
        grant(p, "bk_leap_1", "bk_roar_1");
        PlayableCharacter c = character();
        for (int slot = 0; slot < 4; slot++) {
            c.activate(p, slot, AbilityAction.START);
            c.activate(p, slot, AbilityAction.STOP);
        }
        c.activate(p, 2, AbilityAction.USE);
        c.activate(p, 3, AbilityAction.USE);
        near(h, 100f, QiData.getCurrent(p), "nothing fired");
        c.activate(p, 0, AbilityAction.USE);
        near(h, 90f, QiData.getCurrent(p), "slot 0: leap");
        c.activate(p, 1, AbilityAction.USE);
        near(h, 82f, QiData.getCurrent(p), "slot 1: roar");
        h.succeed();
    }
}
