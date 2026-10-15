package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.wukong.WukongAbilityHandler;
import com.jttw.engine.character.wukong.WukongCheckpoints;
import com.jttw.engine.character.wukong.WukongCheckpoints.Checkpoint;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.item.ImmortalPeachItem;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.item.ItemTestSupport.*;

/**
 * Immortal Pill, Immortal Peach and the True Samadhi Fire trial: who gets what, the flags each
 * writes, and what full immortality unlocks.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ImmortalItemsGameTests {

    private static ServerPlayer wukong(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        PlayerLevel.setProgressionClass(p, ProgressionClass.PRIMORDIAL);
        return p;
    }

    private static void eat(GameTestHelper h, ServerPlayer p, ItemStack food) {
        food.getItem().finishUsingItem(food, h.getLevel(), p);
    }

    private static void eatPill(GameTestHelper h, ServerPlayer p) {
        eat(h, p, new ItemStack(ModItems.IMMORTAL_PILL.get()));
    }

    private static void eatPeach(GameTestHelper h, ServerPlayer p) {
        eat(h, p, new ItemStack(ModItems.IMMORTAL_PEACH.get()));
    }

    private static ServerPlayer fullyImmortalWukong(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        eatPill(h, p);
        eatPeach(h, p);
        return p;
    }

    // ── Immortal Pill ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void pillGivesWukongThePillFlagAndCompletesItsCheckpoint(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.getFoodData().setFoodLevel(0);
        eatPill(h, p);
        check(h, p.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PILLS), "pill flag set");
        check(h, WukongCheckpoints.isComplete(p, Checkpoint.IMMORTALITY_PILLS), "pill checkpoint complete");
        check(h, !p.hasEffect(MobEffects.CONFUSION), "Wukong is not nauseous");
        eq(h, 2, p.getFoodData().getFoodLevel(), "pill restores 2 hunger");
        check(h, !ImmortalItems.hasFullImmortality(p), "pill alone is not full immortality");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void pillGivesEveryoneElseQiButNotImmortality(GameTestHelper h) {
        for (ServerPlayer p : new ServerPlayer[] {
                player(h), as(h, ReincarnationOutcome.EYE_OF_HEAVEN), as(h, ReincarnationOutcome.YUAN_DI)}) {
            eatPill(h, p);
            check(h, !p.hasEffect(MobEffects.CONFUSION), "no nausea");
            check(h, !p.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PILLS), "no pill flag");
            check(h, !WukongCheckpoints.isComplete(p, Checkpoint.IMMORTALITY_PILLS), "no checkpoint");
        }
        h.succeed();
    }

    // ── Immortal Peach ────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void peachRefillsQiAndHungerForAnyone(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        PlayerLevel.setProgressionClass(p, ProgressionClass.HEAVENLY);
        QiData.setMax(p, 5000f);
        QiData.setCurrent(p, 100f);
        p.getFoodData().setFoodLevel(0);
        eatPeach(h, p);
        near(h, 4800f, QiData.getCurrent(p), "100 + 4700 Qi refill");
        near(h, 5000f, QiData.getMax(p), "max Qi unchanged");
        eq(h, 6, p.getFoodData().getFoodLevel(), "3 hunger bars");
        eq(h, null, com.jttw.engine.gametest.character.CharTest.modifier(p, com.jttw.engine.gametest.character.CharTest.PHYSICAL_RESISTANCE(), "JTTW Peach Physical Resistance"), "no resistance bonus");
        check(h, !p.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES), "no peach flag");
        check(h, !p.getPersistentData().getBoolean("jttw_projectile_immune"), "no projectile immunity");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void peachRefillStopsAtMaxQi(GameTestHelper h) {
        ServerPlayer p = player(h);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 10f);
        eatPeach(h, p);
        near(h, 100f, QiData.getCurrent(p), "refill capped at max");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void peachGivesWukongPermanentBonusesOnFirstEat(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 0f);
        eatPeach(h, p);
        near(h, 4800f, QiData.getMax(p), "+4700 max Qi");
        near(h, 4800f, QiData.getCurrent(p), "topped up to the new max");
        eq(h, 48.0, com.jttw.engine.gametest.character.CharTest.modifier(p, com.jttw.engine.gametest.character.CharTest.PHYSICAL_RESISTANCE(), "JTTW Peach Physical Resistance"), "+48 physical resistance");
        eq(h, 40.0, com.jttw.engine.gametest.character.CharTest.modifier(p, com.jttw.engine.gametest.character.CharTest.MAGIC_RESISTANCE(), "JTTW Peach Magic Resistance"), "+40 magic resistance");
        near(h, 0.0, p.getAttributeValue(Attributes.ARMOR), "no vanilla armor");
        check(h, p.getPersistentData().getBoolean("jttw_projectile_immune"), "projectile immunity flag");
        check(h, p.getPersistentData().getBoolean("jttw_peach_consumed"), "first-eat flag");
        check(h, p.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES), "peach progression flag");
        check(h, WukongCheckpoints.isComplete(p, Checkpoint.IMMORTAL_PEACHES), "peach checkpoint complete");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void secondPeachDoesNotStackWukongsBonuses(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        eatPeach(h, p);
        QiData.setCurrent(p, 0f);
        eatPeach(h, p);
        near(h, 4800f, QiData.getMax(p), "max Qi raised only once");
        near(h, 4700f, QiData.getCurrent(p), "second peach is just the 4700 refill");
        eq(h, 48.0, com.jttw.engine.gametest.character.CharTest.modifier(p, com.jttw.engine.gametest.character.CharTest.PHYSICAL_RESISTANCE(), "JTTW Peach Physical Resistance"), "physical bonus not stacked");
        eq(h, 40.0, com.jttw.engine.gametest.character.CharTest.modifier(p, com.jttw.engine.gametest.character.CharTest.MAGIC_RESISTANCE(), "JTTW Peach Magic Resistance"), "magic bonus not stacked");
        h.succeed();
    }

    /** The permanent bonus is Wukong's alone, not every Primordial's. */
    @GameTest(template = PLATFORM)
    public static void peachBonusIsOnlyForWukongNotOtherPrimordials(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.YUAN_DI);
        PlayerLevel.setProgressionClass(p, ProgressionClass.PRIMORDIAL);
        QiData.setMax(p, 100f);
        eatPeach(h, p);
        near(h, 100f, QiData.getMax(p), "Yuan Di max Qi");
        eq(h, null, com.jttw.engine.gametest.character.CharTest.modifier(p, com.jttw.engine.gametest.character.CharTest.PHYSICAL_RESISTANCE(), "JTTW Peach Physical Resistance"), "Yuan Di resistance");
        check(h, !p.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES), "Yuan Di peach flag");
        check(h, !WukongCheckpoints.isComplete(p, Checkpoint.IMMORTAL_PEACHES), "Wukong checkpoint on Yuan Di");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void peachResistanceIsReappliedToTheRespawnedPlayerOnce(GameTestHelper h) {
        ServerPlayer before = wukong(h);
        QiData.setMax(before, 100f);
        eatPeach(h, before);
        ServerPlayer after = player(h);
        WukongCheckpoints.copyTo(before, after);
        ImmortalPeachItem.reapplyPeachModifiers(after);
        ImmortalPeachItem.reapplyPeachModifiers(after);
        eq(h, 48.0, com.jttw.engine.gametest.character.CharTest.modifier(after, com.jttw.engine.gametest.character.CharTest.PHYSICAL_RESISTANCE(), "JTTW Peach Physical Resistance"), "physical resistance after respawn");
        eq(h, 40.0, com.jttw.engine.gametest.character.CharTest.modifier(after, com.jttw.engine.gametest.character.CharTest.MAGIC_RESISTANCE(), "JTTW Peach Magic Resistance"), "magic resistance after respawn");
        check(h, after.getPersistentData().getBoolean("jttw_projectile_immune"), "projectile immunity kept");

        ServerPlayer neverAte = player(h);
        ImmortalPeachItem.reapplyPeachModifiers(neverAte);
        eq(h, null, com.jttw.engine.gametest.character.CharTest.modifier(neverAte, com.jttw.engine.gametest.character.CharTest.PHYSICAL_RESISTANCE(), "JTTW Peach Physical Resistance"), "nothing without the peach");
        h.succeed();
    }

    // ── Full immortality and what it unlocks ─────────────────────

    @GameTest(template = PLATFORM)
    public static void pillPlusPeachIsFullImmortalityInEitherOrder(GameTestHelper h) {
        check(h, ImmortalItems.hasFullImmortality(fullyImmortalWukong(h)), "pill then peach");
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        eatPeach(h, p);
        check(h, !ImmortalItems.hasFullImmortality(p), "peach alone");
        eatPill(h, p);
        check(h, ImmortalItems.hasFullImmortality(p), "peach then pill");
        h.succeed();
    }

    private static boolean cancels(ServerPlayer p, DamageSource source) {
        LivingIncomingDamageEvent event = new LivingIncomingDamageEvent(p, source, 5f);
        WukongAbilityHandler.onImmortalityDamage(event);
        return event.isCanceled();
    }

    @GameTest(template = PLATFORM)
    public static void fullImmortalityBlocksFireExplosionLightningAndProjectiles(GameTestHelper h) {
        ServerPlayer p = fullyImmortalWukong(h);
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new net.minecraft.core.BlockPos(0, 2, 0));
        Snowball snowball = new Snowball(EntityType.SNOWBALL, h.getLevel());
        var src = p.damageSources();
        check(h, cancels(p, src.onFire()), "fire");
        check(h, cancels(p, src.inFire()), "standing in fire");
        check(h, cancels(p, src.explosion(null, null)), "explosion");
        check(h, cancels(p, src.lightningBolt()), "lightning");
        check(h, cancels(p, src.thrown(snowball, zombie)), "projectile");
        check(h, !cancels(p, src.mobAttack(zombie)), "melee still hurts");
        check(h, !cancels(p, src.magic()), "magic still hurts");

        LivingKnockBackEvent knock = new LivingKnockBackEvent(p, 0.4f, 1.0, 0.0);
        WukongAbilityHandler.onKnockback(knock);
        check(h, knock.isCanceled(), "knockback cancelled");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void halfImmortalOrNonWukongGetsNoImmunities(GameTestHelper h) {
        ServerPlayer pillOnly = wukong(h);
        eatPill(h, pillOnly);
        check(h, !cancels(pillOnly, pillOnly.damageSources().onFire()), "pill only: fire hurts");

        ServerPlayer impostor = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        impostor.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PILLS, true);
        impostor.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES, true);
        check(h, !cancels(impostor, impostor.damageSources().onFire()), "not Wukong: fire hurts");
        LivingKnockBackEvent knock = new LivingKnockBackEvent(impostor, 0.4f, 1.0, 0.0);
        WukongAbilityHandler.onKnockback(knock);
        check(h, !knock.isCanceled(), "not Wukong: knocked back");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void peachProjectileImmunityCancelsIndirectHitsOnly(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        eatPeach(h, p);
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new net.minecraft.core.BlockPos(0, 2, 0));
        Snowball snowball = new Snowball(EntityType.SNOWBALL, h.getLevel());
        LivingIncomingDamageEvent thrown = new LivingIncomingDamageEvent(p, p.damageSources().thrown(snowball, zombie), 4f);
        WukongCheckpoints.onHurt(thrown);
        check(h, thrown.isCanceled(), "projectile cancelled");
        LivingIncomingDamageEvent melee = new LivingIncomingDamageEvent(p, p.damageSources().mobAttack(zombie), 4f);
        WukongCheckpoints.onHurt(melee);
        check(h, !melee.isCanceled(), "melee not cancelled");
        h.succeed();
    }

    // ── True Samadhi Fire ─────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void samadhiFireStartsTheTrialOnlyForAFullyImmortalWukong(GameTestHelper h) {
        ServerPlayer notWukong = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        notWukong.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PILLS, true);
        notWukong.getPersistentData().putBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES, true);
        ImmortalItems.onSamadhiFireHit(notWukong);
        check(h, !notWukong.getPersistentData().contains(ImmortalItems.KEY_SAMADHI_FIRE_TICK), "not Wukong");

        ServerPlayer mortal = wukong(h);
        eatPill(h, mortal);
        ImmortalItems.onSamadhiFireHit(mortal);
        check(h, !mortal.getPersistentData().contains(ImmortalItems.KEY_SAMADHI_FIRE_TICK), "not fully immortal");
        check(h, mortal.getRemainingFireTicks() <= 0, "mortal not set alight");

        ServerPlayer p = fullyImmortalWukong(h);
        long now = h.getLevel().getGameTime();
        ImmortalItems.onSamadhiFireHit(p);
        eq(h, now, p.getPersistentData().getLong(ImmortalItems.KEY_SAMADHI_FIRE_TICK), "trial start tick");
        eq(h, 100, p.getRemainingFireTicks(), "burns for 5 seconds");
        check(h, WukongCheckpoints.isComplete(p, Checkpoint.TRUE_SAMADHI_FIRE), "fire checkpoint complete");
        check(h, !p.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES), "no eyes yet");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 200)
    public static void samadhiFireGrantsFieryGoldenEyesAfterOneHundredTicks(GameTestHelper h) {
        ServerPlayer p = fullyImmortalWukong(h);
        ImmortalItems.onSamadhiFireHit(p);
        ImmortalItems.tickSamadhiFire(p);
        check(h, !p.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES), "not yet at 0 ticks");
        h.runAfterDelay(90, () -> {
            ImmortalItems.tickSamadhiFire(p);
            check(h, !p.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES), "not yet at 90 ticks");
        });
        h.runAfterDelay(101, () -> {
            ImmortalItems.tickSamadhiFire(p);
            check(h, p.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES), "eyes granted");
            check(h, !p.getPersistentData().contains(ImmortalItems.KEY_SAMADHI_FIRE_TICK), "trial cleared");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void samadhiFireIsIgnoredOnceTheEyesAreEarned(GameTestHelper h) {
        ServerPlayer p = fullyImmortalWukong(h);
        p.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        ImmortalItems.onSamadhiFireHit(p);
        check(h, !p.getPersistentData().contains(ImmortalItems.KEY_SAMADHI_FIRE_TICK), "no new trial");
        check(h, p.getRemainingFireTicks() <= 0, "not set alight");

        // A trial left over from before the eyes were earned is cleaned up on the next tick.
        p.getPersistentData().putLong(ImmortalItems.KEY_SAMADHI_FIRE_TICK, h.getLevel().getGameTime());
        ImmortalItems.tickSamadhiFire(p);
        check(h, !p.getPersistentData().contains(ImmortalItems.KEY_SAMADHI_FIRE_TICK), "stale trial removed");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void immortalityFlagsSurviveRespawn(GameTestHelper h) {
        ServerPlayer before = fullyImmortalWukong(h);
        before.getPersistentData().putBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES, true);
        ServerPlayer after = player(h);
        ImmortalItems.copyTo(before, after);
        check(h, after.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PILLS), "pills");
        check(h, after.getPersistentData().getBoolean(ImmortalItems.KEY_IMMORTAL_PEACHES), "peaches");
        check(h, after.getPersistentData().getBoolean(ImmortalItems.KEY_FIERY_GOLDEN_EYES), "eyes");

        ServerPlayer blank = player(h);
        ServerPlayer copy = player(h);
        ImmortalItems.copyTo(blank, copy);
        check(h, !copy.getPersistentData().contains(ImmortalItems.KEY_IMMORTAL_PILLS), "nothing invented");
        h.succeed();
    }
}
