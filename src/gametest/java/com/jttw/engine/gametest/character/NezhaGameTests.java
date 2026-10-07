package com.jttw.engine.gametest.character;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.nezha.NezhaAbilityHandler;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.item.FireTipSpearItem;
import com.jttw.engine.item.QiankunRingItem;
import com.jttw.engine.item.YinYangSwordItem;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Nezha: weapon cycle, Samadhi Fire, Lotus Detonation, Three Heads Six Arms, Wind Fire Wheels. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class NezhaGameTests {

    private static ServerPlayer nezha(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        qi(p, 100f, 100f);
        return p;
    }

    private static PlayableCharacter character() {
        return Characters.byOutcome(ReincarnationOutcome.THIRD_LOTUS_PRINCE.id);
    }

    private static int count(ServerPlayer p, Class<? extends Item> type) {
        int n = 0;
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (type.isInstance(inv.getItem(i).getItem())) n++;
        }
        return n;
    }

    private static int fireballs(ServerPlayer p) {
        return (int) near(p, SmallFireball.class, 4).stream().filter(f -> f.getOwner() == p).count();
    }

    // ── Weapon cycle (slot 0) ───────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "weaponCycleNeedsArmaments")
    public static void weaponCycleNeedsArmaments(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        character().activate(p, 0, AbilityAction.USE);
        check(h, p.getMainHandItem().isEmpty(), "nothing equipped");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierOneCyclesBetweenRingAndSpear")
    public static void tierOneCyclesBetweenRingAndSpear(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        grant(p, "nz_arm_1");
        character().activate(p, 0, AbilityAction.USE);
        check(h, p.getMainHandItem().getItem() instanceof QiankunRingItem, "first press: Qiankun Ring");
        character().activate(p, 0, AbilityAction.USE);
        check(h, p.getMainHandItem().getItem() instanceof FireTipSpearItem, "second press: Fire-Tip Spear");
        eq(h, 0, count(p, QiankunRingItem.class), "the ring was taken back");
        character().activate(p, 0, AbilityAction.USE);
        check(h, p.getMainHandItem().getItem() instanceof QiankunRingItem, "third press: back to the ring");
        eq(h, 0, count(p, FireTipSpearItem.class), "the spear was taken back");
        eq(h, 0, count(p, YinYangSwordItem.class), "no swords at tier 1");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "tierTwoAddsTheYinYangSwordsInBothHands")
    public static void tierTwoAddsTheYinYangSwordsInBothHands(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        grant(p, "nz_arm_1", "nz_arm_2");
        NezhaAbilityHandler.cycleWeapon(p); // ring
        NezhaAbilityHandler.cycleWeapon(p); // swords
        check(h, p.getMainHandItem().getItem() instanceof YinYangSwordItem, "Yin in the main hand");
        check(h, p.getOffhandItem().getItem() instanceof YinYangSwordItem, "Yang in the off hand");
        eq(h, 0, count(p, QiankunRingItem.class), "ring taken back");
        NezhaAbilityHandler.cycleWeapon(p); // spear
        check(h, p.getMainHandItem().getItem() instanceof FireTipSpearItem, "then the spear");
        eq(h, 0, count(p, YinYangSwordItem.class), "both swords taken back, off hand included");
        h.succeed();
    }

    // ── Samadhi Fire (slot 1) ───────────────────────────────────────

    private static void holdSpear(ServerPlayer p) {
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIRE_TIP_SPEAR.get()));
    }

    @GameTest(template = PLATFORM, batch = "samadhiFireNeedsTheNodeAndTheSpearInHand")
    public static void samadhiFireNeedsTheNodeAndTheSpearInHand(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        holdSpear(p);
        character().activate(p, 1, AbilityAction.USE);
        eq(h, 0, fireballs(p), "no node: no fire");
        grant(p, "nz_fire_1");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.QIANKUN_RING.get()));
        character().activate(p, 1, AbilityAction.USE);
        eq(h, 0, fireballs(p), "ring in hand: no fire");
        near(h, 100f, QiData.getCurrent(p), "refusals cost nothing");
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 100, batch = "samadhiFireCostsEightQiShootsOneFireballAndHasATwoSecondCooldown")
    public static void samadhiFireCostsEightQiShootsOneFireballAndHasATwoSecondCooldown(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        grant(p, "nz_fire_1");
        holdSpear(p);
        character().activate(p, 1, AbilityAction.USE);
        eq(h, 1, fireballs(p), "one fireball");
        near(h, 92f, QiData.getCurrent(p), "Qi");
        NezhaAbilityHandler.castSamadhiFire(p);
        near(h, 92f, QiData.getCurrent(p), "second cast refused during cooldown");
        h.runAfterDelay(38, () -> {
            NezhaAbilityHandler.castSamadhiFire(p);
            near(h, 92f, QiData.getCurrent(p), "still cooling down at 38 ticks");
            h.runAfterDelay(3, () -> {
                NezhaAbilityHandler.castSamadhiFire(p);
                near(h, 84f, QiData.getCurrent(p), "ready again after 40 ticks");
                h.succeed();
            });
        });
    }

    @GameTest(template = PLATFORM, batch = "samadhiFireWithoutEightQiIsRefused")
    public static void samadhiFireWithoutEightQiIsRefused(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        grant(p, "nz_fire_1");
        holdSpear(p);
        QiData.setCurrent(p, 7.9f);
        NezhaAbilityHandler.castSamadhiFire(p);
        eq(h, 0, fireballs(p), "no fireball");
        near(h, 7.9f, QiData.getCurrent(p), "Qi");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "threeHeadsSixArmsShootsSixFireballs")
    public static void threeHeadsSixArmsShootsSixFireballs(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        grant(p, "nz_fire_1", "nz_form_1");
        holdSpear(p);
        NezhaAbilityHandler.toggleThreeHeadsSixArms(p);
        NezhaAbilityHandler.castSamadhiFire(p);
        eq(h, 6, fireballs(p), "fireballs");
        near(h, 92f, QiData.getCurrent(p), "still one 8-Qi cast");
        NezhaAbilityHandler.toggleThreeHeadsSixArms(p);
        h.succeed();
    }

    // ── Three Heads Six Arms (slot 3) ───────────────────────────────

    @GameTest(template = PLATFORM, batch = "threeHeadsSixArmsAddsHalfAgainAttackUntilToggledOff")
    public static void threeHeadsSixArmsAddsHalfAgainAttackUntilToggledOff(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        character().activate(p, 3, AbilityAction.USE);
        eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:three_heads_atk"), "needs nz_form");
        grant(p, "nz_form_1");
        character().activate(p, 3, AbilityAction.USE);
        eq(h, 0.5, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:three_heads_atk"), "+50% attack");
        check(h, p.getPersistentData().getBoolean("jttw_nz_three_heads"), "form flag on");
        character().activate(p, 3, AbilityAction.USE);
        eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:three_heads_atk"), "removed");
        check(h, !p.getPersistentData().getBoolean("jttw_nz_three_heads"), "form flag off");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "threeHeadsSixArmsDrainsQiAndEndsWhenItRunsOut")
    public static void threeHeadsSixArmsDrainsQiAndEndsWhenItRunsOut(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        grant(p, "nz_form_1");
        NezhaAbilityHandler.toggleThreeHeadsSixArms(p);
        tick(p, NezhaAbilityHandler::onPlayerTick, 10);
        near(h, 98.5f, QiData.getCurrent(p), "0.15 Qi per tick for 10 ticks");
        QiData.setCurrent(p, 0.2f);
        tick(p, NezhaAbilityHandler::onPlayerTick, 2);
        eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:three_heads_atk"), "form ended");
        check(h, !p.getPersistentData().getBoolean("jttw_nz_three_heads"), "form flag off");
        h.succeed();
    }

    // ── Lotus Detonation (slot 2, held) ─────────────────────────────

    private static ServerPlayer detonator(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        PlayerLevel.setLevel(p, 50);
        grant(p, "nz_det_1");
        var atk = p.getAttribute(Attributes.ATTACK_DAMAGE);
        atk.removeModifiers();
        atk.setBaseValue(5.0);
        return p;
    }

    @GameTest(template = PLATFORM, batch = "lotusDetonationNeedsLevelFiftyAndTheNode")
    public static void lotusDetonationNeedsLevelFiftyAndTheNode(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        grant(p, "nz_det_1");
        PlayerLevel.setLevel(p, 49);
        character().activate(p, 2, AbilityAction.START);
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "level 49 cannot charge");
        ServerPlayer noNode = nezha(h);
        PlayerLevel.setLevel(noNode, 50);
        character().activate(noNode, 2, AbilityAction.START);
        check(h, !noNode.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "level 50 without the node cannot charge");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "lotusDetonationReleasedEarlyFizzles")
    public static void lotusDetonationReleasedEarlyFizzles(GameTestHelper h) {
        ServerPlayer p = detonator(h);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        List<Hit> hits = watch(golem);
        character().activate(p, 2, AbilityAction.START);
        eq(h, 255, amp(p, MobEffects.MOVEMENT_SLOWDOWN), "rooted while charging");
        tick(p, NezhaAbilityHandler::onPlayerTick, 19);
        character().activate(p, 2, AbilityAction.STOP);
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "root lifted");
        near(h, 100f, QiData.getCurrent(p), "no Qi spent");
        check(h, hits.isEmpty(), "no blast");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "chargedLotusDetonationCostsTwentyQiAndBurnsEverythingNearby")
    public static void chargedLotusDetonationCostsTwentyQiAndBurnsEverythingNearby(GameTestHelper h) {
        ServerPlayer p = detonator(h);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        List<Hit> hits = watch(golem);
        NezhaAbilityHandler.startDetonationCharge(p);
        tick(p, NezhaAbilityHandler::onPlayerTick, 20);
        NezhaAbilityHandler.stopDetonationCharge(p);
        near(h, 80f, QiData.getCurrent(p), "Qi");
        List<Hit> magic = of(hits, DamageTypes.MAGIC);
        eq(h, 1, magic.size(), "one magic hit");
        // attack 5 x 1.2 x max(1, armaments = 0)
        near(h, 6.0, magic.get(0).amount(), "damage");
        eq(h, 200, golem.getRemainingFireTicks(), "set burning for 10 s");
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "root lifted");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "lotusDetonationScalesWithArmamentsAndThreeHeads")
    public static void lotusDetonationScalesWithArmamentsAndThreeHeads(GameTestHelper h) {
        ServerPlayer p = detonator(h);
        p.getInventory().add(new ItemStack(ModItems.FIRE_TIP_SPEAR.get()));
        p.getInventory().add(new ItemStack(ModItems.QIANKUN_RING.get()));
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        List<Hit> hits = watch(golem);
        NezhaAbilityHandler.startDetonationCharge(p);
        tick(p, NezhaAbilityHandler::onPlayerTick, 20);
        NezhaAbilityHandler.stopDetonationCharge(p);
        // attack 5 x 1.2 x 2 armaments
        near(h, 12.0, of(hits, DamageTypes.MAGIC).get(0).amount(), "two armaments");

        ServerPlayer formed = detonator(h);
        grant(formed, "nz_form_1");
        NezhaAbilityHandler.toggleThreeHeadsSixArms(formed);
        // three heads: x2.0 instead of x1.2; its own +50% attack modifier also applies
        double atk = formed.getAttributeValue(Attributes.ATTACK_DAMAGE);
        near(h, 7.5, atk, "attack with Three Heads");
        IronGolem golem2 = mob(h, EntityType.IRON_GOLEM, 4, 4);
        List<Hit> hits2 = watch(golem2);
        NezhaAbilityHandler.startDetonationCharge(formed);
        tick(formed, NezhaAbilityHandler::onPlayerTick, 20);
        NezhaAbilityHandler.stopDetonationCharge(formed);
        near(h, 15.0, of(hits2, DamageTypes.MAGIC).get(0).amount(), "7.5 x 2.0");
        NezhaAbilityHandler.toggleThreeHeadsSixArms(formed);
        h.succeed();
    }

    @GameTest(template = PLATFORM, timeoutTicks = 700, batch = "lotusDetonationHasAThirtySecondCooldown")
    public static void lotusDetonationHasAThirtySecondCooldown(GameTestHelper h) {
        ServerPlayer p = detonator(h);
        NezhaAbilityHandler.startDetonationCharge(p);
        tick(p, NezhaAbilityHandler::onPlayerTick, 20);
        NezhaAbilityHandler.stopDetonationCharge(p);
        NezhaAbilityHandler.startDetonationCharge(p);
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "cannot charge again straight away");
        h.runAfterDelay(601, () -> {
            NezhaAbilityHandler.startDetonationCharge(p);
            check(h, p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "can charge after 600 ticks");
            NezhaAbilityHandler.stopDetonationCharge(p);
            h.succeed();
        });
    }

    @GameTest(template = PLATFORM, batch = "lotusDetonationWithoutTwentyQiDoesNothing")
    public static void lotusDetonationWithoutTwentyQiDoesNothing(GameTestHelper h) {
        ServerPlayer p = detonator(h);
        QiData.setCurrent(p, 19f);
        IronGolem golem = mob(h, EntityType.IRON_GOLEM, 0, 0);
        List<Hit> hits = watch(golem);
        NezhaAbilityHandler.startDetonationCharge(p);
        tick(p, NezhaAbilityHandler::onPlayerTick, 20);
        NezhaAbilityHandler.stopDetonationCharge(p);
        near(h, 19f, QiData.getCurrent(p), "Qi");
        check(h, hits.isEmpty(), "no blast");
        h.succeed();
    }

    // ── Passives ────────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "windFireWheelsGiveFlightWithTheFlightNode")
    public static void windFireWheelsGiveFlightWithTheFlightNode(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.WIND_FIRE_WHEELS.get()));
        tick(p, NezhaAbilityHandler::onPlayerTick, 1);
        check(h, !JTTWFlight.hasUniversalFlight(p), "wheels alone do not fly");
        grant(p, "nz_flight_1");
        tick(p, NezhaAbilityHandler::onPlayerTick, 1);
        check(h, JTTWFlight.hasUniversalFlight(p), "wheels + flight node");
        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        tick(p, NezhaAbilityHandler::onPlayerTick, 1);
        check(h, !JTTWFlight.hasUniversalFlight(p), "flight revoked when the wheels come off");
        h.succeed();
    }

    @GameTest(template = PLATFORM, batch = "huntianClothAppliesArmorToMagicDamage")
    public static void huntianClothAppliesArmorToMagicDamage(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        var armor = p.getAttribute(Attributes.ARMOR);
        armor.removeModifiers();
        armor.setBaseValue(10.0);
        p.getAttribute(Attributes.ARMOR_TOUGHNESS).removeModifiers();
        p.getAttribute(Attributes.ARMOR_TOUGHNESS).setBaseValue(0.0);
        LivingHurtEvent bare = new LivingHurtEvent(p, h.getLevel().damageSources().magic(), 10f);
        NezhaAbilityHandler.onMagicDamage(bare);
        near(h, 10f, bare.getAmount(), "without the cloth");
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HUNTIAN_CLOTH.get()));
        // the fake player never ticks, so the cloth's own armor points are not applied: armor stays 10
        LivingHurtEvent clothed = new LivingHurtEvent(p, h.getLevel().damageSources().magic(), 10f);
        NezhaAbilityHandler.onMagicDamage(clothed);
        // vanilla armor formula: 10 * (1 - max(10/5, 10 - 10/2) / 25) = 8
        near(h, 8f, clothed.getAmount(), "magic damage through the cloth");
        h.succeed();
    }

    // ── Key routing ─────────────────────────────────────────────────

    @GameTest(template = PLATFORM, batch = "nezhaKeysIgnoreTheWrongAction")
    public static void nezhaKeysIgnoreTheWrongAction(GameTestHelper h) {
        ServerPlayer p = detonator(h);
        grant(p, "nz_arm_1", "nz_form_1");
        PlayableCharacter c = character();
        c.activate(p, 0, AbilityAction.START);
        check(h, p.getMainHandItem().isEmpty(), "weapon cycle ignores START");
        c.activate(p, 2, AbilityAction.USE);
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "detonation ignores USE");
        c.activate(p, 3, AbilityAction.START);
        eq(h, null, modifier(p, Attributes.ATTACK_DAMAGE, "jttw:three_heads_atk"), "form ignores START");
        c.activate(p, 2, AbilityAction.START);
        check(h, p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "detonation charges on START");
        c.activate(p, 2, AbilityAction.STOP);
        check(h, !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "and releases on STOP");
        h.succeed();
    }
}
