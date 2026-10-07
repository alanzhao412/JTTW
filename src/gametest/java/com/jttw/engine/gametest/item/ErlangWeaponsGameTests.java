package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.erlang.ErlangShenAbilityHandler;
import com.jttw.engine.item.ErlangAxeItem;
import com.jttw.engine.item.ThreePointedSpearItem;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.item.ItemTestSupport.*;

/**
 * Erlang Shen's Three-Pointed Spear (triple strike, charged thrust) and Divine Axe (cleave on
 * hit, ground cleave), including Erlang Shen's damage bonus with each.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
// Line- and area-targeted attacks run in their own batch: in the shared batch a
// neighbouring test's mob can be first in the line.
public class ErlangWeaponsGameTests {

    private static final int USE_DURATION = 72000;
    /** ErlangShenAbilityHandler.onWeaponDamageBonus multiplies Erlang Shen's hits by this. */
    private static final double ES_BONUS = 1.667;

    private static ItemStack spear() {
        return new ItemStack(ModItems.THREE_POINTED_SPEAR.get());
    }

    private static ItemStack axe() {
        return new ItemStack(ModItems.ERLANG_AXE.get());
    }

    private static Pig pig(GameTestHelper h, int x, int z) {
        return h.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(x, 2, z));
    }

    // ── Three-Pointed Spear: attributes and triple strike ─────────

    @GameTest(template = PLATFORM, batch = "anyoneCanStartAThrust")
    public static void spearAddsTwentyPercentAttackInTheMainHandOnly(GameTestHelper h) {
        ServerPlayer p = player(h);
        setBaseAtk(p, 10.0);
        ItemStack s = spear();
        applyEquipmentModifiers(p, s, EquipmentSlot.MAINHAND);
        near(h, 12.0, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "ATK");
        check(h, s.getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty(), "nothing off-hand");
        eq(h, UseAnim.SPEAR, s.getUseAnimation(), "spear animation");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "minimumChargeThrustDealsOneAndAHalfAttackAndStartsCooldown")
    public static void everyThirdSpearHitStrikesTwiceMoreForHalfAttack(GameTestHelper h) {
        ServerPlayer p = player(h);
        setBaseAtk(p, 4.0);
        ItemStack s = spear();
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        Pig target = pig(h, 2, 3);
        HurtLog log = HurtLog.on(target);
        try {
            s.getItem().hurtEnemy(s, target, p);
            s.getItem().hurtEnemy(s, target, p);
            eq(h, 0, log.playerAttacks().size(), "first two hits add nothing");
            s.getItem().hurtEnemy(s, target, p);
            eq(h, 2, log.playerAttacks().size(), "third hit strikes twice more");
            for (var hit : log.playerAttacks()) {
                near(h, 2.0, hit.amount(), "half of 4 ATK");
                check(h, hit.source().is(DamageTypes.PLAYER_ATTACK), "player attack");
            }
            near(h, 6.0, target.getHealth(), "pig health after the triple strike");
            s.getItem().hurtEnemy(s, target, p);
            s.getItem().hurtEnemy(s, target, p);
            eq(h, 2, log.playerAttacks().size(), "combo restarted after the strike");
            s.getItem().hurtEnemy(s, target, p);
            eq(h, 4, log.playerAttacks().size(), "sixth hit strikes again");
        } finally {
            log.close();
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "fullChargeThrustDealsTwoAndAHalfAttack")
    public static void erlangShenTripleStrikeGetsTheSpearBonus(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        setBaseAtk(p, 4.0);
        ItemStack s = spear();
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        Pig target = pig(h, 2, 3);
        HurtLog log = HurtLog.on(target);
        try {
            for (int i = 0; i < 3; i++) s.getItem().hurtEnemy(s, target, p);
        } finally {
            log.close();
        }
        eq(h, 2, log.playerAttacks().size(), "two bonus strikes");
        for (var hit : log.playerAttacks()) near(h, 2.0 * ES_BONUS, hit.amount(), 0.01, "2 x 1.667");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "thrustOnCooldownIsIgnoredForThirtyTicks", timeoutTicks = 150)
    public static void spearComboResetsAfterThreeSecondsIdle(GameTestHelper h) {
        ServerPlayer p = player(h);
        setBaseAtk(p, 4.0);
        ItemStack s = spear();
        Pig target = pig(h, 2, 3);
        HurtLog log = HurtLog.on(target);
        s.getItem().hurtEnemy(s, target, p);
        s.getItem().hurtEnemy(s, target, p);
        h.runAfterDelay(61, () -> {
            try {
                s.getItem().hurtEnemy(s, target, p);
                eq(h, 0, log.playerAttacks().size(), "third hit after 61 idle ticks counts as the first " + log.describe());
                s.getItem().hurtEnemy(s, target, p);
                s.getItem().hurtEnemy(s, target, p);
                eq(h, 2, log.playerAttacks().size(), "strike after three fresh hits " + log.describe());
            } finally {
                log.close();
            }
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "thrustMissesTargetsOffTheLine")
    public static void spearHitByANonPlayerDoesNothing(GameTestHelper h) {
        var zombie = h.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(0, 2, 0));
        Pig target = pig(h, 2, 3);
        ItemStack s = spear();
        HurtLog log = HurtLog.on(target);
        try {
            for (int i = 0; i < 3; i++) check(h, s.getItem().hurtEnemy(s, target, zombie), "returns true");
        } finally {
            log.close();
        }
        eq(h, 0, log.playerAttacks().size(), "no strikes");
        h.succeed();
    }

    // ── Three-Pointed Spear: charged thrust ───────────────────────

    /** Player at (2,1,0) looking south and 20 degrees down, at a pig two blocks ahead. */
    private static ServerPlayer thruster(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 0, 0f, 20f);
        setBaseAtk(p, 2.0);
        return p;
    }

    private static void thrust(GameTestHelper h, ServerPlayer p, ItemStack s, int chargeTicks) {
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        s.releaseUsing(h.getLevel(), p, USE_DURATION - chargeTicks);
    }

    @GameTest(template = PLATFORM, batch = "wrathShortensThrustCooldownAndLengthensTheLunge")
    public static void anyoneCanStartAThrust(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack s = spear();
        eq(h, InteractionResult.CONSUME, use(h, p, s), "use result");
        check(h, p.isUsingItem() && p.getUseItem() == s, "charging");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "thrustHitsOneTargetUnlessSpearTwoPierces")
    public static void minimumChargeThrustDealsOneAndAHalfAttackAndStartsCooldown(GameTestHelper h) {
        ServerPlayer p = thruster(h);
        Pig target = pig(h, 2, 3);
        ItemStack s = spear();
        HurtLog log = HurtLog.on(target);
        try {
            thrust(h, p, s, 4);
            eq(h, 0, log.playerAttacks().size(), "4 ticks: too short");
            check(h, !p.getCooldowns().isOnCooldown(s.getItem()), "no cooldown for a fizzle");
            thrust(h, p, s, 5);
        } finally {
            log.close();
        }
        eq(h, 1, log.playerAttacks().size(), "5 ticks: one hit");
        near(h, 3.0, log.playerAttacks().get(0).amount(), "150% of 2 ATK");
        near(h, 2.0, p.getDeltaMovement().length(), 1e-3, "lunge speed 8 x 0.25");
        check(h, p.getDeltaMovement().z > 0, "lunges forward");
        check(h, p.getCooldowns().isOnCooldown(s.getItem()), "on cooldown");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "eyeOfHeavenAddsThirtyPercentMagicToTheThrust")
    public static void fullChargeThrustDealsTwoAndAHalfAttack(GameTestHelper h) {
        ServerPlayer p = thruster(h);
        Pig target = pig(h, 2, 3);
        HurtLog log = HurtLog.on(target);
        try {
            thrust(h, p, spear(), 40);
        } finally {
            log.close();
        }
        eq(h, 1, log.playerAttacks().size(), "one hit");
        near(h, 5.0, log.playerAttacks().get(0).amount(), "250% of 2 ATK");
        near(h, 5.0, p.getDeltaMovement().length(), 1e-3, "lunge speed 20 x 0.25");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "erlangShenAxeHitCleavesEnemiesWithinTwoBlocks")
    public static void thrustOnCooldownIsIgnoredForThirtyTicks(GameTestHelper h) {
        ServerPlayer p = thruster(h);
        Pig target = pig(h, 2, 3);
        ItemStack s = spear();
        HurtLog log = HurtLog.on(target);
        try {
            thrust(h, p, s, 5);
            thrust(h, p, s, 5);
            eq(h, 1, log.playerAttacks().size(), "second thrust refused");
            for (int i = 0; i < 29; i++) p.getCooldowns().tick();
            check(h, p.getCooldowns().isOnCooldown(s.getItem()), "still cooling after 29 ticks");
            p.getCooldowns().tick();
            check(h, !p.getCooldowns().isOnCooldown(s.getItem()), "ready after 30 ticks");
            target.invulnerableTime = 0;
            thrust(h, p, s, 5);
            eq(h, 2, log.playerAttacks().size(), "thrust allowed again");
        } finally {
            log.close();
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "axeHitDoesNotCleaveForOthers")
    public static void thrustMissesTargetsOffTheLine(GameTestHelper h) {
        ServerPlayer p = thruster(h);
        Pig side = pig(h, 0, 2);
        Pig behind = pig(h, 2, 0);
        HurtLog sideLog = HurtLog.on(side);
        HurtLog behindLog = HurtLog.on(behind);
        try {
            p.moveTo(p.getX(), p.getY(), p.getZ() + 1, 0f, 20f); // pig "behind" is now under/behind the player
            thrust(h, p, spear(), 40);
        } finally {
            sideLog.close();
            behindLog.close();
        }
        eq(h, 0, sideLog.playerAttacks().size(), "pig to the side");
        eq(h, 0, behindLog.playerAttacks().size(), "pig behind");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wrathShortensThrustCooldownAndLengthensTheLunge(GameTestHelper h) {
        ServerPlayer p = thruster(h);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        SkillTreeData.grantNode(p, "es_wrath_1");
        p.setHealth(p.getMaxHealth() * 0.25f);
        ItemStack s = spear();
        thrust(h, p, s, 5);
        near(h, 3.0, p.getDeltaMovement().length(), 1e-3, "lunge 8 x 1.5 x 0.25");
        for (int i = 0; i < 14; i++) p.getCooldowns().tick();
        check(h, p.getCooldowns().isOnCooldown(s.getItem()), "cooling after 14 ticks");
        p.getCooldowns().tick();
        check(h, !p.getCooldowns().isOnCooldown(s.getItem()), "ready after 15 ticks");

        ServerPlayer healthy = thruster(h);
        setOutcome(healthy, ReincarnationOutcome.EYE_OF_HEAVEN);
        SkillTreeData.grantNode(healthy, "es_wrath_1");
        thrust(h, healthy, spear(), 5);
        near(h, 2.0, healthy.getDeltaMovement().length(), 1e-3, "no wrath above 40% health");

        ServerPlayer untrained = thruster(h);
        setOutcome(untrained, ReincarnationOutcome.EYE_OF_HEAVEN);
        untrained.setHealth(untrained.getMaxHealth() * 0.25f);
        thrust(h, untrained, spear(), 5);
        near(h, 2.0, untrained.getDeltaMovement().length(), 1e-3, "no wrath without the node");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thrustHitsOneTargetUnlessSpearTwoPierces(GameTestHelper h) {
        Pig near = pig(h, 2, 2);
        Pig far = pig(h, 2, 4);
        HurtLog nearLog = HurtLog.on(near);
        HurtLog farLog = HurtLog.on(far);
        try {
            thrust(h, thruster(h), spear(), 40);
            eq(h, 1, nearLog.playerAttacks().size() + farLog.playerAttacks().size(), "without piercing: one target");

            ServerPlayer piercer = thruster(h);
            setOutcome(piercer, ReincarnationOutcome.EYE_OF_HEAVEN);
            SkillTreeData.grantNode(piercer, "es_spear_1");
            SkillTreeData.grantNode(piercer, "es_spear_2");
            near.invulnerableTime = 0;
            far.invulnerableTime = 0;
            int before = nearLog.playerAttacks().size() + farLog.playerAttacks().size();
            thrust(h, piercer, spear(), 5);
            eq(h, before + 2, nearLog.playerAttacks().size() + farLog.playerAttacks().size(), "piercing: both targets");
        } finally {
            nearLog.close();
            farLog.close();
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void eyeOfHeavenAddsThirtyPercentMagicToTheThrust(GameTestHelper h) {
        ServerPlayer p = thruster(h);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        SkillTreeData.grantNode(p, "es_eye_1");
        ErlangShenAbilityHandler.toggleEye(p);
        check(h, ErlangShenAbilityHandler.isEyeActive(p.getUUID()), "eye open");
        Pig target = pig(h, 2, 3);
        HurtLog log = HurtLog.on(target);
        try {
            thrust(h, p, spear(), 5);
        } finally {
            log.close();
            ErlangShenAbilityHandler.toggleEye(p);
        }
        var melee = log.matching(src -> src.is(DamageTypes.PLAYER_ATTACK));
        var magic = log.matching(src -> src.is(DamageTypes.MAGIC));
        eq(h, 1, melee.size(), "thrust hit");
        near(h, 3.0 * ES_BONUS, melee.get(0).amount(), 0.01, "3 x Erlang Shen's spear bonus");
        eq(h, 1, magic.size(), "eye hit");
        near(h, 0.9, magic.get(0).amount(), 1e-3, "30% of the 3-damage thrust");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "spear_shockwave")
    public static void spearThreeFullChargeThrustEndsInAShockwave(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 0, 0f, 0f);
        setBaseAtk(p, 2.0);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        for (int t = 1; t <= 3; t++) SkillTreeData.grantNode(p, "es_spear_" + t);
        // Full charge: lunge 20, so the shockwave centres 20 blocks ahead of the feet.
        Pig target = pig(h, 2, 20);
        target.setNoGravity(true);
        HurtLog log = HurtLog.on(target);
        try {
            thrust(h, p, spear(), 40);
        } finally {
            log.close();
        }
        var magic = log.matching(src -> src.is(DamageTypes.MAGIC));
        eq(h, 1, magic.size(), "shockwave hit");
        near(h, 1.0, magic.get(0).amount(), 1e-3, "half of 2 ATK");
        target.discard();
        h.succeed();
    }

    // ── Divine Axe ────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void axeAddsFiftyPercentAttackAndSlowsSwingsByTwentyPercent(GameTestHelper h) {
        ServerPlayer p = player(h);
        setBaseAtk(p, 10.0);
        near(h, 4.0, p.getAttributeValue(Attributes.ATTACK_SPEED), "player base attack speed");
        ItemStack a = axe();
        applyEquipmentModifiers(p, a, EquipmentSlot.MAINHAND);
        near(h, 15.0, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "ATK");
        near(h, 3.2, p.getAttributeValue(Attributes.ATTACK_SPEED), "attack speed");
        check(h, a.getAttributeModifiers(EquipmentSlot.OFFHAND).isEmpty(), "nothing off-hand");
        eq(h, UseAnim.BOW, a.getUseAnimation(), "draw animation");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangShenAxeHitCleavesEnemiesWithinTwoBlocks(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 4, 0, 0f, 0f);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        setBaseAtk(p, 2.0);
        ItemStack a = axe();
        p.setItemInHand(InteractionHand.MAIN_HAND, a);
        Pig target = pig(h, 1, 1);
        Pig beside = pig(h, 1, 3);
        Pig far = pig(h, 4, 4);
        HurtLog targetLog = HurtLog.on(target);
        HurtLog besideLog = HurtLog.on(beside);
        HurtLog farLog = HurtLog.on(far);
        try {
            check(h, a.getItem().hurtEnemy(a, target, p), "returns true");
        } finally {
            targetLog.close();
            besideLog.close();
            farLog.close();
        }
        eq(h, 0, targetLog.playerAttacks().size(), "the struck target takes no splash");
        eq(h, 1, besideLog.playerAttacks().size(), "neighbour cleaved");
        near(h, 1.0 * ES_BONUS, besideLog.playerAttacks().get(0).amount(), 0.01, "50% of 2 ATK x Erlang Shen's axe bonus");
        eq(h, 0, farLog.playerAttacks().size(), "pig 3 blocks away untouched");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void axeHitDoesNotCleaveForOthers(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 4, 0, 0f, 0f);
        setBaseAtk(p, 2.0);
        ItemStack a = axe();
        p.setItemInHand(InteractionHand.MAIN_HAND, a);
        Pig target = pig(h, 1, 1);
        Pig beside = pig(h, 1, 3);
        HurtLog besideLog = HurtLog.on(beside);
        try {
            a.getItem().hurtEnemy(a, target, p);
        } finally {
            besideLog.close();
        }
        eq(h, 0, besideLog.playerAttacks().size(), "no cleave");
        h.succeed();
    }

    private static Pig cleaveTarget(GameTestHelper h) {
        return pig(h, 2, 3);
    }

    @GameTest(template = PLATFORM, batch = "axe_cleave_short")
    public static void groundCleaveNeedsTenTicksOfCharge(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 1, 0f, 0f);
        setBaseAtk(p, 4.0);
        ItemStack a = axe();
        Pig target = cleaveTarget(h);
        HurtLog log = HurtLog.on(target);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, a);
            a.releaseUsing(h.getLevel(), p, USE_DURATION - 9);
        } finally {
            log.close();
        }
        eq(h, 0, log.playerAttacks().size(), "nothing happens");
        check(h, !p.getCooldowns().isOnCooldown(a.getItem()), "no cooldown");
        eq(h, InteractionResult.CONSUME, use(h, p, a), "can start charging again");
        check(h, p.isUsingItem(), "charging");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "axe_cleave_min", timeoutTicks = 150)
    public static void groundCleaveHitsForOneAttackAndLocksTheAxeForThreeSeconds(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 1, 0f, 0f);
        setBaseAtk(p, 4.0);
        ItemStack a = axe();
        Pig target = cleaveTarget(h);
        HurtLog log = HurtLog.on(target);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, a);
            a.releaseUsing(h.getLevel(), p, USE_DURATION - 10);
        } finally {
            log.close();
        }
        var cleave = log.matching(src -> src.is(DamageTypes.PLAYER_ATTACK));
        eq(h, 1, cleave.size(), "cleave hit");
        near(h, 4.0, cleave.get(0).amount(), "minimum charge: 100% of 4 ATK");
        check(h, p.getCooldowns().isOnCooldown(a.getItem()), "item cooldown");
        p.stopUsingItem();
        eq(h, InteractionResult.FAIL, use(h, p, a), "refused right after");
        h.runAfterDelay(55, () -> eq(h, InteractionResult.FAIL, use(h, p, a), "refused at 55 ticks"));
        h.runAfterDelay(61, () -> {
            eq(h, InteractionResult.CONSUME, use(h, p, a), "allowed after 60 ticks");
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "axe_cleave_full")
    public static void fullyChargedGroundCleaveHitsForTwoAndAHalfAttack(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 1, 0f, 0f);
        setBaseAtk(p, 4.0);
        ItemStack a = axe();
        Pig target = cleaveTarget(h);
        HurtLog log = HurtLog.on(target);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, a);
            a.releaseUsing(h.getLevel(), p, USE_DURATION - 40);
        } finally {
            log.close();
        }
        var cleave = log.matching(src -> src.is(DamageTypes.PLAYER_ATTACK));
        eq(h, 1, cleave.size(), "cleave hit");
        near(h, 10.0, cleave.get(0).amount(), "full charge: 250% of 4 ATK");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "axe_cleave_es")
    public static void erlangShenGroundCleaveGetsTheAxeBonus(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 1, 0f, 0f);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        setBaseAtk(p, 4.0);
        ItemStack a = axe();
        Pig target = cleaveTarget(h);
        HurtLog log = HurtLog.on(target);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, a);
            a.releaseUsing(h.getLevel(), p, USE_DURATION - 10);
        } finally {
            log.close();
        }
        var cleave = log.matching(src -> src.is(DamageTypes.PLAYER_ATTACK));
        eq(h, 1, cleave.size(), "cleave hit");
        near(h, 4.0 * ES_BONUS, cleave.get(0).amount(), 0.01, "4 ATK x 1.667");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangHelpersRecogniseOnlyEyeOfHeaven(GameTestHelper h) {
        check(h, ErlangAxeItem.isErlangShen(as(h, ReincarnationOutcome.EYE_OF_HEAVEN)), "axe: Erlang Shen");
        check(h, !ErlangAxeItem.isErlangShen(as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE)), "axe: Nezha");
        check(h, !ErlangAxeItem.isErlangShen(player(h)), "axe: nobody");
        check(h, ThreePointedSpearItem.isErlangShen(as(h, ReincarnationOutcome.EYE_OF_HEAVEN)), "spear: Erlang Shen");
        check(h, !ThreePointedSpearItem.isErlangShen(as(h, ReincarnationOutcome.CELESTIAL_PRIMATE)), "spear: Wukong");
        h.succeed();
    }
}
