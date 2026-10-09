package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.entity.QiWaveEntity;
import com.jttw.engine.item.RuyiJinguBangHandler;
import com.jttw.engine.item.RuyiJinguBangItem;
import com.jttw.engine.item.ThreePointedSpearItem;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.item.ItemTestSupport.*;

/**
 * Ruyi Jingu Bang: who may command it, its three modes, planting it, the 30-ATK wielding rule,
 * the Qi Wave charge, hits in each mode, its attack multiplier and the reach handler.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class RuyiJinguBangGameTests {

    private static final int USE_DURATION = 72000;

    private static ItemStack staff() {
        return new ItemStack(ModItems.RUYI_JINGU_BANG.get());
    }

    private static ItemStack staff(boolean empowered, boolean large) {
        ItemStack s = staff();
        s.getOrCreateTag().putBoolean(RuyiJinguBangItem.TAG_QI_EMPOWERED, empowered);
        s.getOrCreateTag().putBoolean(RuyiJinguBangItem.TAG_LARGE_MODE, large);
        return s;
    }

    private static ServerPlayer wukong(GameTestHelper h) {
        return as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
    }

    // ── Who may command it ────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void onlyWukongCanCommandTheStaff(GameTestHelper h) {
        for (ServerPlayer p : new ServerPlayer[] {
                player(h), as(h, ReincarnationOutcome.EYE_OF_HEAVEN), as(h, ReincarnationOutcome.YUAN_DI)}) {
            ItemStack s = staff();
            eq(h, InteractionResult.FAIL, use(h, p, s), "use result");
            check(h, !RuyiJinguBangItem.isEmpoweredOrLarge(s), "mode unchanged");
            p.setShiftKeyDown(true);
            eq(h, InteractionResult.FAIL, use(h, p, s), "sneak-use result");
            check(h, p.getMainHandItem() == s, "staff not planted");
        }
        h.succeed();
    }

    // ── Modes ─────────────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void rightClickInNormalModeEmpowersTheStaff(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        ItemStack s = staff();
        check(h, !s.isEnchanted() && !s.getItem().isFoil(s), "no glint in normal mode");
        eq(h, UseAnim.NONE, s.getUseAnimation(), "no draw animation in normal mode");
        eq(h, InteractionResult.CONSUME, use(h, p, s), "use result");
        check(h, RuyiJinguBangItem.isQiEmpowered(s), "empowered");
        check(h, !RuyiJinguBangItem.isLargeMode(s), "not large");
        check(h, s.getItem().isFoil(s), "glints while empowered");
        eq(h, UseAnim.BOW, s.getUseAnimation(), "bow-draw animation while empowered");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void rightClickWhileEmpoweredStartsChargingTheWave(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        ItemStack s = staff(true, false);
        eq(h, InteractionResult.CONSUME, use(h, p, s), "use result");
        check(h, p.isUsingItem(), "charging");
        check(h, p.getUseItem() == s, "charging the staff");
        eq(h, USE_DURATION, p.getUseItemRemainingTicks(), "hold up to 72000 ticks");
        check(h, RuyiJinguBangItem.isQiEmpowered(s), "still empowered");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void sneakCyclesEmpoweredToLargeToNormal(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.setShiftKeyDown(true);
        ItemStack s = staff(true, false);
        eq(h, InteractionResult.CONSUME, use(h, p, s), "empowered + sneak");
        check(h, RuyiJinguBangItem.isLargeMode(s) && !RuyiJinguBangItem.isQiEmpowered(s), "now large only");
        check(h, !p.isUsingItem(), "not charging");
        eq(h, InteractionResult.CONSUME, use(h, p, s), "large + sneak");
        check(h, !RuyiJinguBangItem.isEmpoweredOrLarge(s), "back to normal");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void plainRightClickInLargeModeDoesNothing(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        ItemStack s = staff(false, true);
        eq(h, InteractionResult.PASS, use(h, p, s), "use result");
        check(h, RuyiJinguBangItem.isLargeMode(s) && !RuyiJinguBangItem.isQiEmpowered(s), "still large");
        check(h, !p.isUsingItem(), "not charging");
        h.succeed();
    }

    // ── Planting the staff ────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void sneakInNormalModePlantsTheStaffOnTheBlockLookedAt(GameTestHelper h) {
        // Eyes 1.62 above the floor, looking 45 degrees down to the south: the ray meets the
        // floor 1.62 blocks ahead, on floor block (2,1,4); the staff stands on top of it.
        ServerPlayer p = playerAt(h, 2, 2, 0f, 45f);
        setOutcome(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.setShiftKeyDown(true);
        ItemStack s = staff();
        eq(h, InteractionResult.CONSUME, use(h, p, s), "use result");
        check(h, p.getMainHandItem().isEmpty(), "staff leaves the hand");
        List<PlacedStaffEntity> placed = entitiesNear(h, PlacedStaffEntity.class, 2, e -> p.getUUID().equals(e.getOwnerUUID()));
        eq(h, 1, placed.size(), "planted staffs");
        PlacedStaffEntity e = placed.get(0);
        BlockPos expected = h.absolutePos(new BlockPos(2, 2, 4));
        near(h, expected.getX() + 0.5, e.getX(), "x");
        near(h, expected.getY(), e.getY(), "y");
        near(h, expected.getZ() + 0.5, e.getZ(), "z");
        near(h, 1.0, e.getStaffScale(), "starts at 1x scale");
        check(h, e.isOwner(p), "owned by the planter");
        check(h, !e.isFalling(), "standing");
        e.discard();
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void sneakWithNoBlockInReachPlantsTheStaffAtTheFeet(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 2, 2, 0f, -90f); // looking straight up into open air
        setOutcome(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.setShiftKeyDown(true);
        use(h, p, staff());
        List<PlacedStaffEntity> placed = entitiesNear(h, PlacedStaffEntity.class, 2, e -> p.getUUID().equals(e.getOwnerUUID()));
        eq(h, 1, placed.size(), "planted staffs");
        near(h, p.getX(), placed.get(0).getX(), "x");
        near(h, p.getY(), placed.get(0).getY(), "y");
        near(h, p.getZ(), placed.get(0).getZ(), "z");
        placed.get(0).discard();
        h.succeed();
    }

    // ── 30 ATK to wield ───────────────────────────────────────────

    private static List<ItemEntity> droppedStaffs(GameTestHelper h) {
        return entitiesNear(h, ItemEntity.class, 3, e -> e.getItem().getItem() instanceof RuyiJinguBangItem);
    }

    @GameTest(template = PLATFORM)
    public static void tooWeakNonWukongDropsTheStaff(GameTestHelper h) {
        ServerPlayer p = player(h);
        ItemStack s = staff(true, true);
        p.getInventory().setItem(0, s);
        p.getInventory().selected = 0;
        s.inventoryTick(h.getLevel(), p, 0, true);
        check(h, p.getInventory().getItem(0).isEmpty(), "slot emptied");
        List<ItemEntity> dropped = droppedStaffs(h);
        eq(h, 1, dropped.size(), "staffs dropped");
        ItemStack onGround = dropped.get(0).getItem();
        check(h, !RuyiJinguBangItem.isEmpoweredOrLarge(onGround), "dropped staff reset to normal");
        dropped.forEach(ItemEntity::discard);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nonWukongNeedsThirtyBaseAttackToKeepTheStaff(GameTestHelper h) {
        // 30 base ATK, held normally (2x): 60 total, 30 base after the staff's own multiplier.
        ServerPlayer strong = player(h);
        setBaseAtk(strong, 30.0);
        ItemStack kept = staff();
        strong.getInventory().setItem(0, kept);
        strong.getInventory().selected = 0;
        applyEquipmentModifiers(strong, kept, EquipmentSlot.MAINHAND);
        kept.inventoryTick(h.getLevel(), strong, 0, true);
        check(h, strong.getInventory().getItem(0) == kept, "30 ATK keeps the staff");

        ServerPlayer weak = player(h);
        setBaseAtk(weak, 29.0);
        ItemStack lost = staff();
        weak.getInventory().setItem(0, lost);
        weak.getInventory().selected = 0;
        applyEquipmentModifiers(weak, lost, EquipmentSlot.MAINHAND);
        lost.inventoryTick(h.getLevel(), weak, 0, true);
        check(h, weak.getInventory().getItem(0).isEmpty(), "29 ATK drops the staff");
        droppedStaffs(h).forEach(ItemEntity::discard);
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void strongNonWukongCannotKeepEmpoweredOrLargeModes(GameTestHelper h) {
        ServerPlayer p = player(h);
        setBaseAtk(p, 30.0);
        ItemStack s = staff(true, true);
        p.getInventory().setItem(0, s);
        p.getInventory().selected = 0;
        applyEquipmentModifiers(p, s, EquipmentSlot.MAINHAND);
        s.inventoryTick(h.getLevel(), p, 0, true);
        check(h, p.getInventory().getItem(0) == s, "kept");
        check(h, !RuyiJinguBangItem.isEmpoweredOrLarge(s), "modes cleared");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wukongAlwaysKeepsTheStaffEvenWithOneAttack(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        ItemStack s = staff(false, true);
        p.getInventory().setItem(0, s);
        p.getInventory().selected = 0;
        s.inventoryTick(h.getLevel(), p, 0, true);
        check(h, p.getInventory().getItem(0) == s, "kept");
        check(h, RuyiJinguBangItem.isLargeMode(s), "large mode kept");
        eq(h, 0, droppedStaffs(h).size(), "nothing dropped");
        h.succeed();
    }

    /**
     * A too-weak player with the staff in the off hand. Inventory.tick passes the index within
     * the off-hand list (0); dropping the staff must not clear main-inventory slot 0 with it.
     */
    @GameTest(template = PLATFORM)
    public static void droppingAnOffhandStaffLeavesTheHotbarAlone(GameTestHelper h) {
        ServerPlayer p = player(h);
        p.getInventory().setItem(0, new ItemStack(Items.DIAMOND, 5));
        p.setItemInHand(InteractionHand.OFF_HAND, staff());
        p.getInventory().tick();
        List<ItemEntity> dropped = droppedStaffs(h);
        int drops = dropped.size();
        dropped.forEach(ItemEntity::discard);
        eq(h, Items.DIAMOND, p.getInventory().getItem(0).getItem(), "hotbar slot 0 untouched");
        check(h, p.getOffhandItem().isEmpty(), "staff left the off hand");
        eq(h, 1, drops, "staffs dropped");
        h.succeed();
    }

    // ── Qi drain while empowered ──────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void empoweredStaffDrawsQiEachSecondOnlyInTheMainHand(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 10f);
        ItemStack s = staff(true, false);
        p.setItemInHand(InteractionHand.OFF_HAND, s);
        com.jttw.engine.item.WieldRules.drawFormQi(p);
        near(h, 10f, QiData.getCurrent(p), "in the off hand: no draw");
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        com.jttw.engine.item.WieldRules.drawFormQi(p);
        near(h, 10f - com.jttw.engine.item.WieldRules.RUYI_EMPOWERED_QI, QiData.getCurrent(p), "3 Qi a second");
        check(h, RuyiJinguBangItem.isQiEmpowered(s), "still empowered");

        float after = QiData.getCurrent(p);
        ItemStack normal = staff();
        p.setItemInHand(InteractionHand.MAIN_HAND, normal);
        com.jttw.engine.item.WieldRules.drawFormQi(p);
        near(h, after, QiData.getCurrent(p), "normal mode draws nothing");
        ItemStack large = staff(false, true);
        p.setItemInHand(InteractionHand.MAIN_HAND, large);
        com.jttw.engine.item.WieldRules.drawFormQi(p);
        near(h, after - com.jttw.engine.item.WieldRules.RUYI_LARGE_QI, QiData.getCurrent(p), "large mode draws 5 a second");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void empowermentFadesWhenQiRunsOut(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 0.1f);
        ItemStack s = staff(true, false);
        p.setItemInHand(InteractionHand.MAIN_HAND, s);
        com.jttw.engine.item.WieldRules.drawFormQi(p);
        check(h, !RuyiJinguBangItem.isQiEmpowered(s), "empowerment gone");
        near(h, 0.1f, QiData.getCurrent(p), "failed drain costs nothing");
        h.succeed();
    }

    // ── Qi Wave (charge and release) ──────────────────────────────

    private static List<QiWaveEntity> waves(GameTestHelper h) {
        return entitiesNear(h, QiWaveEntity.class, 12, e -> true);
    }

    private static int releaseAfter(GameTestHelper h, ServerPlayer p, ItemStack s, int chargeTicks) {
        s.releaseUsing(h.getLevel(), p, USE_DURATION - chargeTicks);
        List<QiWaveEntity> w = waves(h);
        w.forEach(QiWaveEntity::discard);
        return w.size();
    }

    @GameTest(template = PLATFORM, batch = "ruyi_wave")
    public static void qiWaveCostScalesFromSevenAndAHalfToFifteen(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        ItemStack s = staff(true, false);

        QiData.setCurrent(p, 100f);
        eq(h, 0, releaseAfter(h, p, s, 9), "9 ticks: fizzles");
        near(h, 100f, QiData.getCurrent(p), "fizzle is free");

        eq(h, 1, releaseAfter(h, p, s, 10), "10 ticks: fires");
        near(h, 92.5f, QiData.getCurrent(p), "minimum charge costs 7.5");

        QiData.setCurrent(p, 100f);
        eq(h, 1, releaseAfter(h, p, s, 35), "35 ticks: fires");
        near(h, 88.75f, QiData.getCurrent(p), "half charge costs 11.25");

        QiData.setCurrent(p, 100f);
        eq(h, 1, releaseAfter(h, p, s, 60), "60 ticks: fires");
        near(h, 85f, QiData.getCurrent(p), "full charge costs 15");

        QiData.setCurrent(p, 100f);
        eq(h, 1, releaseAfter(h, p, s, 500), "500 ticks: fires");
        near(h, 85f, QiData.getCurrent(p), "overcharging still costs 15");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "ruyi_wave")
    public static void qiWaveIsRefusedWithoutEnoughQiOrOutsideEmpoweredMode(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        QiData.setMax(p, 100f);
        QiData.setCurrent(p, 7f);
        eq(h, 0, releaseAfter(h, p, staff(true, false), 10), "7 Qi < 7.5");
        near(h, 7f, QiData.getCurrent(p), "refused wave costs nothing");

        QiData.setCurrent(p, 100f);
        eq(h, 0, releaseAfter(h, p, staff(), 60), "normal mode");
        eq(h, 0, releaseAfter(h, p, staff(false, true), 60), "large mode");
        near(h, 100f, QiData.getCurrent(p), "no Qi spent");

        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        QiData.setMax(erlang, 100f);
        QiData.setCurrent(erlang, 100f);
        eq(h, 0, releaseAfter(h, erlang, staff(true, false), 60), "not Wukong");
        near(h, 100f, QiData.getCurrent(erlang), "no Qi spent by non-Wukong");
        h.succeed();
    }

    // ── Hitting mobs ──────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "ruyi_hit_normal")
    public static void normalModeHitHasNoExtraEffect(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 0, 0, 0f, 0f);
        setOutcome(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        setBaseAtk(p, 30.0);
        var golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(4, 2, 2), Blocks.DIRT);
        HurtLog log = HurtLog.on(golem);
        try {
            ItemStack s = staff();
            check(h, s.getItem().hurtEnemy(s, golem, p), "hurtEnemy returns true");
        } finally {
            log.close();
        }
        eq(h, 0, log.hits.size(), "no extra damage");
        h.assertBlockPresent(Blocks.DIRT, new BlockPos(4, 2, 2));
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "ruyi_hit_empowered")
    public static void empoweredHitExplodesWithoutBreakingBlocks(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 0, 0, 0f, 0f);
        setOutcome(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        setBaseAtk(p, 30.0);
        var golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(4, 2, 2), Blocks.DIRT);
        HurtLog log = HurtLog.on(golem);
        try {
            ItemStack s = staff(true, false);
            s.getItem().hurtEnemy(s, golem, p);
        } finally {
            log.close();
        }
        check(h, !log.matching(src -> src.is(DamageTypeTags.IS_EXPLOSION)).isEmpty(), "caught in an explosion");
        check(h, log.matching(src -> src.is(DamageTypes.INDIRECT_MAGIC)).isEmpty(), "no armour-piercing hit");
        h.assertBlockPresent(Blocks.DIRT, new BlockPos(4, 2, 2));
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "ruyi_hit_large")
    public static void largeHitBreaksBlocksAndAddsHalfAttackAsArmourPiercingDamage(GameTestHelper h) {
        ServerPlayer p = playerAt(h, 0, 0, 0f, 0f);
        setOutcome(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
        setBaseAtk(p, 30.0);
        var golem = h.spawnWithNoFreeWill(EntityType.IRON_GOLEM, new BlockPos(2, 2, 2));
        h.setBlock(new BlockPos(4, 2, 2), Blocks.DIRT);
        HurtLog log = HurtLog.on(golem);
        try {
            ItemStack s = staff(false, true);
            s.getItem().hurtEnemy(s, golem, p);
        } finally {
            log.close();
        }
        check(h, !log.matching(src -> src.is(DamageTypeTags.IS_EXPLOSION)).isEmpty(), "caught in an explosion");
        var magic = log.matching(src -> src.is(DamageTypes.INDIRECT_MAGIC));
        eq(h, 1, magic.size(), "one armour-piercing hit");
        near(h, 15.0, magic.get(0).amount(), "half of 30 ATK");
        check(h, magic.get(0).source().is(DamageTypeTags.BYPASSES_ARMOR), "bypasses armour");
        check(h, magic.get(0).source().getEntity() == p, "credited to the wielder");
        // The blast carves over the next ticks, not all on the one it goes off
        h.succeedWhen(() -> h.assertBlockNotPresent(Blocks.DIRT, new BlockPos(4, 2, 2)));
    }

    // ── Attack multiplier ─────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void staffDoublesAttackAndTriplesItWhenEmpoweredOrLarge(GameTestHelper h) {
        double[] expected = {20.0, 30.0, 30.0};
        ItemStack[] stacks = {staff(), staff(true, false), staff(false, true)};
        for (int i = 0; i < stacks.length; i++) {
            ServerPlayer p = player(h);
            setBaseAtk(p, 10.0);
            applyEquipmentModifiers(p, stacks[i], EquipmentSlot.MAINHAND);
            near(h, expected[i], p.getAttributeValue(Attributes.ATTACK_DAMAGE), "ATK with mode " + i);
            removeEquipmentModifiers(p, stacks[i], EquipmentSlot.MAINHAND);
            near(h, 10.0, p.getAttributeValue(Attributes.ATTACK_DAMAGE), "ATK after unequipping " + i);
        }
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.FEET}) {
            check(h, staff(true, true).getAttributeModifiers(slot).isEmpty(), "no modifiers in " + slot);
        }
        h.succeed();
    }

    // ── Reach (RuyiJinguBangHandler) ──────────────────────────────

    private static double reachAfterTick(ServerPlayer p, ItemStack mainHand) {
        p.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
        RuyiJinguBangHandler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, p));
        return p.getAttributeValue(ForgeMod.ENTITY_REACH.get());
    }

    @GameTest(template = PLATFORM)
    public static void wukongReachIsEightWithTheStaffAndThirteenWhenLarge(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        near(h, 3.0, p.getAttributeValue(ForgeMod.ENTITY_REACH.get()), "base reach");
        near(h, 8.0, reachAfterTick(p, staff()), "normal");
        near(h, 8.0, reachAfterTick(p, staff(true, false)), "empowered");
        near(h, 13.0, reachAfterTick(p, staff(false, true)), "large");
        near(h, 8.0, reachAfterTick(p, staff()), "back to normal");
        near(h, 3.0, reachAfterTick(p, ItemStack.EMPTY), "empty hand");
        near(h, 3.0, reachAfterTick(p, new ItemStack(Items.STICK)), "another item");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void martialArtsTwoAddsThreeReachOnTheStaff(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        SkillTreeData.grantNode(p, "wu_martial_1");
        near(h, 8.0, reachAfterTick(p, staff()), "Martial Arts I: no extra reach");
        SkillTreeData.grantNode(p, "wu_martial_2");
        near(h, 11.0, reachAfterTick(p, staff()), "Martial Arts II, normal");
        near(h, 16.0, reachAfterTick(p, staff(false, true)), "Martial Arts II, large");
        near(h, 3.0, reachAfterTick(p, ItemStack.EMPTY), "not on bare hands");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nonWukongGetsNoReachFromTheStaff(GameTestHelper h) {
        for (ServerPlayer p : new ServerPlayer[] {player(h), as(h, ReincarnationOutcome.EYE_OF_HEAVEN)}) {
            SkillTreeData.grantNode(p, "wu_martial_1");
            SkillTreeData.grantNode(p, "wu_martial_2");
            near(h, 3.0, reachAfterTick(p, staff(false, true)), "reach");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangShenGetsFiveReachFromTheThreePointedSpear(GameTestHelper h) {
        ItemStack spear = new ItemStack(ModItems.THREE_POINTED_SPEAR.get());
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, ThreePointedSpearItem.isErlangShen(erlang), "is Erlang Shen");
        near(h, 8.0, reachAfterTick(erlang, spear), "Erlang Shen with the spear");
        near(h, 3.0, reachAfterTick(erlang, ItemStack.EMPTY), "spear put away");
        near(h, 3.0, reachAfterTick(wukong(h), spear), "Wukong with the spear");
        near(h, 3.0, reachAfterTick(player(h), spear), "nobody with the spear");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void reachHandlerOnlyActsAtTheStartOfTheTick(GameTestHelper h) {
        ServerPlayer p = wukong(h);
        p.setItemInHand(InteractionHand.MAIN_HAND, staff());
        RuyiJinguBangHandler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
        near(h, 3.0, p.getAttributeValue(ForgeMod.ENTITY_REACH.get()), "END phase ignored");
        h.succeed();
    }
}
