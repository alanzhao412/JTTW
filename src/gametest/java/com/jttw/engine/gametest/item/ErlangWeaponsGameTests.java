package com.jttw.engine.gametest.item;

import java.util.List;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.level.block.Blocks;
import com.jttw.engine.item.SpearReturn;
import com.jttw.engine.entity.ThrownSpearEntity;
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

    /** Only the enlarged axe charges the ground cleave. */
    private static ItemStack largeAxe() {
        ItemStack a = axe();
        com.jttw.engine.item.ErlangAxeItem.setLarge(a, true);
        return a;
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

    @GameTest(template = PLATFORM, batch = "thrustHitsEverythingInItsArea")
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

    /** The thrust is an area: everything in the cone ahead, as far as the lunge, is hit. */
    @GameTest(template = PLATFORM)
    public static void thrustHitsEverythingInItsArea(GameTestHelper h) {
        Pig near = pig(h, 2, 2);
        Pig far = pig(h, 2, 4);
        HurtLog nearLog = HurtLog.on(near);
        HurtLog farLog = HurtLog.on(far);
        try {
            thrust(h, thruster(h), spear(), 40);
            eq(h, 1, nearLog.playerAttacks().size(), "the near pig");
            eq(h, 1, farLog.playerAttacks().size(), "the pig behind it");
        } finally {
            nearLog.close();
            farLog.close();
        }
        h.succeed();
    }

    /** Spear Mastery II: +25% thrust damage. */
    @GameTest(template = PLATFORM)
    public static void spearTwoAddsAQuarterToTheThrust(GameTestHelper h) {
        Pig target = pig(h, 2, 3);
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        target.setHealth(200f);
        HurtLog log = HurtLog.on(target);
        try {
            ServerPlayer one = thruster(h);
            setOutcome(one, ReincarnationOutcome.EYE_OF_HEAVEN);
            SkillTreeData.grantNode(one, "es_spear_1");
            thrust(h, one, spear(), 5);
            target.invulnerableTime = 0;
            ServerPlayer two = thruster(h);
            setOutcome(two, ReincarnationOutcome.EYE_OF_HEAVEN);
            SkillTreeData.grantNode(two, "es_spear_1");
            SkillTreeData.grantNode(two, "es_spear_2");
            thrust(h, two, spear(), 5);
        } finally {
            log.close();
        }
        var hits = log.playerAttacks();
        eq(h, 2, hits.size(), "one thrust each");
        near(h, hits.get(0).amount() * 1.25, hits.get(1).amount(), 1e-3, "Spear II thrust");
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
        // the thrust's own area reaches it too; make it survive that to take the shockwave
        target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200.0);
        target.setHealth(200f);
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

    // ── Three-Pointed Spear: throw (Spear Mastery I) ──────────────

    /**
     * Erlang Shen with Spear Mastery I at (2,2,0) looking straight south, sneaking. The spear
     * flies at eye height, through blocks at y=3. ATK 2 (the fake player does not tick, so the
     * spear's own bonus is not applied): a budget of 8 hardness and 5 damage.
     */
    private static ServerPlayer thrower(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 0, 0f, 0f);
        setBaseAtk(p, 2.0);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        SkillTreeData.grantNode(p, "es_spear_1");
        p.setShiftKeyDown(true);
        return p;
    }

    /** Spears thrown by these players only: tests in a batch run side by side. */
    private static List<ThrownSpearEntity> thrownSpears(GameTestHelper h, ServerPlayer... by) {
        List<ServerPlayer> owners = List.of(by);
        return entitiesNear(h, ThrownSpearEntity.class, 120, e -> owners.contains(e.getOwner()));
    }

    @GameTest(template = PLATFORM)
    public static void throwNeedsErlangShenTheNodeAndSneaking(GameTestHelper h) {
        ServerPlayer standing = thrower(h);
        standing.setShiftKeyDown(false);
        ItemStack s1 = spear();
        thrust(h, standing, s1, 20);
        eq(h, 0, thrownSpears(h, standing).size(), "not sneaking: a thrust");
        check(h, !standing.getMainHandItem().isEmpty(), "spear still in hand");

        ServerPlayer untrained = playerAt(h, 2, 0, 0f, 0f);
        setOutcome(untrained, ReincarnationOutcome.EYE_OF_HEAVEN);
        untrained.setShiftKeyDown(true);
        thrust(h, untrained, spear(), 20);
        eq(h, 0, thrownSpears(h, untrained).size(), "no Spear Mastery I");

        ServerPlayer other = playerAt(h, 2, 0, 0f, 0f);
        setOutcome(other, ReincarnationOutcome.YUAN_DI);
        SkillTreeData.grantNode(other, "es_spear_1");
        other.setShiftKeyDown(true);
        thrust(h, other, spear(), 20);
        eq(h, 0, thrownSpears(h, other).size(), "not Erlang Shen");

        ServerPlayer quick = thrower(h);
        thrust(h, quick, spear(), 9);
        eq(h, 0, thrownSpears(h, quick).size(), "drawn under 10 ticks");
        h.succeed();
    }

    /** Stone costs its hardness of 1.5 each; bedrock cannot be broken at all. */
    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void throwTearsThroughTerrainAndStopsAtWhatItCannotBreak(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        h.setBlock(new BlockPos(2, 3, 3), Blocks.STONE);
        h.setBlock(new BlockPos(2, 3, 4), Blocks.BEDROCK);
        ServerPlayer p = thrower(h);
        thrust(h, p, spear(), 20);
        check(h, p.getMainHandItem().isEmpty(), "the spear left the hand");
        check(h, SpearReturn.isOut(p), "the spear is out");
        eq(h, 1, thrownSpears(h, p).size(), "one spear in flight");
        h.runAfterDelay(5, () -> {
            h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 3, 2));
            h.assertBlockPresent(Blocks.AIR, new BlockPos(2, 3, 3));
            h.assertBlockPresent(Blocks.BEDROCK, new BlockPos(2, 3, 4));
            ThrownSpearEntity thrown = thrownSpears(h, p).get(0);
            check(h, thrown.isStuck(), "stopped at the bedrock");
            near(h, 8.0 - 3.0, thrown.budgetLeft(), "two stone blocks spent 3 of the 8");
            check(h, thrown.travelled() < 4.0, "did not pass the bedrock: " + thrown.travelled());
            h.succeed();
        });
    }

    /** A weak throw (ATK 0.3: 1.2 hardness) cannot get through even one stone block. */
    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void throwStopsWhenTheThrowersStrengthRunsOut(GameTestHelper h) {
        h.setBlock(new BlockPos(2, 3, 2), Blocks.STONE);
        ServerPlayer p = thrower(h);
        setBaseAtk(p, 0.3);
        thrust(h, p, spear(), 20);
        h.runAfterDelay(5, () -> {
            h.assertBlockPresent(Blocks.STONE, new BlockPos(2, 3, 2));
            check(h, thrownSpears(h, p).get(0).isStuck(), "stopped at the stone");
            h.succeed();
        });
    }

    /** Everything on the line is hit once, for 250% ATK. */
    @GameTest(template = PLATFORM, timeoutTicks = 40)
    public static void throwHitsEverythingOnItsLine(GameTestHelper h) {
        IronGolem first = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 2));
        IronGolem second = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 4));
        HurtLog firstLog = HurtLog.on(first);
        HurtLog secondLog = HurtLog.on(second);
        ServerPlayer p = thrower(h);
        thrust(h, p, spear(), 20);
        h.runAfterDelay(5, () -> {
            try {
                var a = firstLog.matching(src -> src.is(DamageTypes.TRIDENT));
                var b = secondLog.matching(src -> src.is(DamageTypes.TRIDENT));
                eq(h, 1, a.size(), "first golem hit once");
                eq(h, 1, b.size(), "second golem hit once");
                near(h, 5.0, a.get(0).amount(), 1e-3, "2 ATK x 2.5");
            } finally {
                firstLog.close();
                secondLog.close();
            }
            h.succeed();
        });
    }

    /** No wall: the spear stops after 100 blocks. */
    @GameTest(template = PLATFORM, timeoutTicks = 80)
    public static void throwFliesAtMostAHundredBlocks(GameTestHelper h) {
        // straight up from y 200, in open sky (test platforms are buried in terrain),
        // staying in the same loaded chunk column
        ServerPlayer p = playerAt(h, 2, 0, 0f, -90f);
        p.moveTo(p.getX(), 200, p.getZ(), 0f, -90f);
        setBaseAtk(p, 2.0);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        SkillTreeData.grantNode(p, "es_spear_1");
        p.setShiftKeyDown(true);
        thrust(h, p, spear(), 20);
        ThrownSpearEntity thrown = h.getLevel().getEntitiesOfClass(ThrownSpearEntity.class,
                p.getBoundingBox().inflate(4), e -> e.getOwner() == p).get(0);
        h.runAfterDelay(40, () -> {
            check(h, thrown.isStuck(), "stopped, travelled " + thrown.travelled());
            near(h, ThrownSpearEntity.MAX_DISTANCE, thrown.travelled(), "100 blocks");
            thrown.discard();
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM)
    public static void thrownSpearReturnsToTheInventoryAfterTwentySeconds(GameTestHelper h) {
        ServerPlayer p = thrower(h);
        ItemStack s = spear();
        s.setHoverName(net.minecraft.network.chat.Component.literal("Named"));
        thrust(h, p, s, 20);
        long now = h.getLevel().getGameTime();
        eq(h, now + 400, SpearReturn.returnAt(p), "returns 400 ticks after the throw");
        SpearReturn.returnIfDue(p);
        check(h, SpearReturn.isOut(p), "not yet");

        // a second throw while one is out is refused
        ItemStack other = spear();
        thrust(h, p, other, 20);
        eq(h, 1, thrownSpears(h, p).size(), "only one spear out at a time");

        p.getPersistentData().getCompound(SpearReturn.KEY).putLong("return_at", now);
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        SpearReturn.returnIfDue(p);
        check(h, !SpearReturn.isOut(p), "returned");
        eq(h, 0, thrownSpears(h, p).size(), "the flying spear is gone");
        boolean found = false;
        for (ItemStack inv : p.getInventory().items) {
            if (inv.getItem() instanceof ThreePointedSpearItem && inv.getHoverName().getString().equals("Named")) found = true;
        }
        check(h, found, "the same spear, name and all, is back in the inventory");
        h.succeed();
    }

    /** A spear still out when its thrower dies comes back to the respawned player. */
    @GameTest(template = PLATFORM)
    public static void thrownSpearSurvivesTheThrowersDeath(GameTestHelper h) {
        ServerPlayer before = thrower(h);
        thrust(h, before, spear(), 20);
        ServerPlayer after = player(h);
        SpearReturn.onPlayerClone(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(after, before, true));
        check(h, SpearReturn.isOut(after), "the note moved to the respawned player");
        eq(h, SpearReturn.returnAt(before), SpearReturn.returnAt(after), "same return time");
        thrownSpears(h, before).forEach(e -> e.discard());
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
        com.jttw.engine.reincarnation.AlignmentData.markReincarnated(p, com.jttw.engine.reincarnation.ReincarnationOutcome.EYE_OF_HEAVEN); // the cleave is Erlang's alone
        setBaseAtk(p, 4.0);
        ItemStack a = largeAxe();
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
        com.jttw.engine.reincarnation.AlignmentData.markReincarnated(p, com.jttw.engine.reincarnation.ReincarnationOutcome.EYE_OF_HEAVEN); // the cleave is Erlang's alone
        setBaseAtk(p, 4.0);
        ItemStack a = largeAxe();
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
        // 100% of 4 ATK, and Erlang's x1.667 with his own axe (it is his cleave now)
        near(h, 4.0 * 1.667, cleave.get(0).amount(), "minimum charge: 100% of 4 ATK, x1.667 in Erlang's hand");
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
        com.jttw.engine.reincarnation.AlignmentData.markReincarnated(p, com.jttw.engine.reincarnation.ReincarnationOutcome.EYE_OF_HEAVEN); // the cleave is Erlang's alone
        setBaseAtk(p, 4.0);
        ItemStack a = largeAxe();
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
        near(h, 10.0 * 1.667, cleave.get(0).amount(), "full charge: 250% of 4 ATK, x1.667 in Erlang's hand");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "axe_cleave_es")
    public static void erlangShenGroundCleaveGetsTheAxeBonus(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 1, 0f, 0f);
        setOutcome(p, ReincarnationOutcome.EYE_OF_HEAVEN);
        setBaseAtk(p, 4.0);
        ItemStack a = largeAxe();
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
