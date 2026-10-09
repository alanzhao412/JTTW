package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.WieldRules;
import com.jttw.engine.item.YinYangSwordItem;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.stats.ProgressionClass;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.character.CharTest.*;

/** Who may wield the artifacts, and what they cost in the wrong hands. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class WieldRulesGameTests {

    private static boolean swingRefused(GameTestHelper h, ServerPlayer p, ItemStack held) {
        p.setItemInHand(InteractionHand.MAIN_HAND, held);
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 3);
        AttackEntityEvent swing = new AttackEntityEvent(p, z);
        WieldRules.onAttack(swing);
        z.discard();
        return swing.isCanceled();
    }

    private static void tickRules(ServerPlayer p, int ticks) {
        tick(p, WieldRules::onPlayerTick, ticks);
    }

    @GameTest(template = PLATFORM)
    public static void theRuyiNeedsThirtyStrExceptForWukong(GameTestHelper h) {
        ServerPlayer weak = as(h, ReincarnationOutcome.EYE_OF_HEAVEN); // level 1
        check(h, WieldRules.strength(weak) < 30, "a new Heavenly is weak: " + WieldRules.strength(weak));
        check(h, swingRefused(h, weak, new ItemStack(ModItems.RUYI_JINGU_BANG.get())), "too heavy for them");

        ServerPlayer strong = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        int lv = WieldRules.levelFor(ProgressionClass.HEAVENLY, 30);
        check(h, lv > 0, "a Heavenly reaches 30 STR at some level");
        PlayerLevel.setLevel(strong, lv);
        check(h, !swingRefused(h, strong, new ItemStack(ModItems.RUYI_JINGU_BANG.get())), "at 30 STR it is theirs to swing");

        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE); // level 1, no need
        check(h, !swingRefused(h, wukong, new ItemStack(ModItems.RUYI_JINGU_BANG.get())), "Wukong is never held back");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangsAxeNeedsFortyStrAndOnlyErlangCleaves(GameTestHelper h) {
        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        check(h, swingRefused(h, other, new ItemStack(ModItems.ERLANG_AXE.get())), "needs 40 STR");
        ItemStack axe = new ItemStack(ModItems.ERLANG_AXE.get());
        other.setItemInHand(InteractionHand.MAIN_HAND, axe);
        axe.use(other.level(), other, InteractionHand.MAIN_HAND);
        check(h, !other.isUsingItem(), "no one else charges the cleave");

        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, !swingRefused(h, erlang, new ItemStack(ModItems.ERLANG_AXE.get())), "Erlang swings it at any strength");
        ItemStack his = new ItemStack(ModItems.ERLANG_AXE.get());
        com.jttw.engine.item.ErlangAxeItem.setLarge(his, true); // the cleave is the large axe's
        erlang.setItemInHand(InteractionHand.MAIN_HAND, his);
        his.use(erlang.level(), erlang, InteractionHand.MAIN_HAND);
        check(h, erlang.isUsingItem(), "and charges its cleave");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nezhasSpearAndRingBurnOtherHands(GameTestHelper h) {
        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIRE_TIP_SPEAR.get()));
        tickRules(other, 20);
        check(h, other.isOnFire(), "the spear burns");
        other.clearFire();
        other.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        other.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(ModItems.QIANKUN_RING.get()));
        tickRules(other, 20);
        check(h, other.isOnFire(), "the ring burns, even in the off hand");

        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        nezha.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FIRE_TIP_SPEAR.get()));
        tickRules(nezha, 20);
        check(h, !nezha.isOnFire(), "not Nezha");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void onlyNezhaThrowsOrWieldsTheRing(GameTestHelper h) {
        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        check(h, swingRefused(h, other, new ItemStack(ModItems.QIANKUN_RING.get())), "cannot fight with it");
        ItemStack ring = new ItemStack(ModItems.QIANKUN_RING.get());
        other.setItemInHand(InteractionHand.MAIN_HAND, ring);
        eq(h, InteractionResult.FAIL, ring.use(other.level(), other, InteractionHand.MAIN_HAND).getResult(), "cannot throw it");

        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        check(h, !swingRefused(h, nezha, new ItemStack(ModItems.QIANKUN_RING.get())), "Nezha fights with it");
        ItemStack his = new ItemStack(ModItems.QIANKUN_RING.get());
        nezha.setItemInHand(InteractionHand.MAIN_HAND, his);
        eq(h, InteractionResult.CONSUME, his.use(nezha.level(), nezha, InteractionHand.MAIN_HAND).getResult(), "and throws it");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSwordsFillBothHandsAndGoBackTogether(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        p.getInventory().selected = 0;
        p.getInventory().items.set(0, YinYangSwordItem.createYin());
        p.getInventory().items.set(5, YinYangSwordItem.createYang());
        ItemStack shield = new ItemStack(net.minecraft.world.item.Items.SHIELD);
        p.setItemSlot(EquipmentSlot.OFFHAND, shield);
        WieldRules.pairSwords(p);
        check(h, YinYangSwordItem.isYang(p.getOffhandItem()), "the yang sword drawn into the off hand");
        check(h, p.getInventory().items.get(5).is(net.minecraft.world.item.Items.SHIELD), "the shield takes its place");

        p.getInventory().selected = 1; // put the pair away
        WieldRules.pairSwords(p);
        check(h, p.getOffhandItem().isEmpty(), "the drawn sword goes back");
        check(h, p.getInventory().items.stream().anyMatch(YinYangSwordItem::isYang), "into the inventory");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void inOtherHandsTheSwordsFreezeAndBurn(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.setItemInHand(InteractionHand.MAIN_HAND, YinYangSwordItem.createYin());
        tickRules(p, 1);
        check(h, p.isFullyFrozen(), "frostbitten");
        check(h, p.isOnFire(), "and on fire at once");

        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        nezha.setItemInHand(InteractionHand.MAIN_HAND, YinYangSwordItem.createYin());
        tickRules(nezha, 1);
        check(h, !nezha.isFullyFrozen() && !nezha.isOnFire(), "not Nezha");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theClothBindsAndWoundsAnyWearerButNezha(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HUNTIAN_CLOTH.get()));
        tickRules(p, 20);
        // Bound; the wound it deals cannot be seen here, as a test's fake player takes no damage
        check(h, p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN) && p.hasEffect(MobEffects.WEAKNESS), "bound");

        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        nezha.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.HUNTIAN_CLOTH.get()));
        tickRules(nezha, 20);
        check(h, !nezha.hasEffect(MobEffects.MOVEMENT_SLOWDOWN), "not Nezha");
        h.succeed();
    }

    // ── Enhanced forms ─────────────────────────────────────────────

    private static ItemStack sneakUse(ServerPlayer p, ItemStack stack) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        p.setShiftKeyDown(true);
        stack.use(p.level(), p, InteractionHand.MAIN_HAND);
        p.setShiftKeyDown(false);
        return stack;
    }

    @GameTest(template = PLATFORM)
    public static void erlangGrowsHisAxeAndOnlyTheLargeAxeCleaves(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        qi(erlang, 100f, 100f);
        ItemStack axe = new ItemStack(ModItems.ERLANG_AXE.get());
        erlang.setItemInHand(InteractionHand.MAIN_HAND, axe);
        axe.use(erlang.level(), erlang, InteractionHand.MAIN_HAND);
        check(h, !erlang.isUsingItem(), "regular: no cleave");

        sneakUse(erlang, axe);
        check(h, com.jttw.engine.item.ErlangAxeItem.isLarge(axe), "sneak + right-click: twice the size");
        double large = modOf(axe, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double slow = modOf(axe, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        check(h, large > 0.5 && slow < -0.2, "harder and slower: " + large + " " + slow);
        axe.use(erlang.level(), erlang, InteractionHand.MAIN_HAND);
        check(h, erlang.isUsingItem(), "large: charges the cleave");
        erlang.stopUsingItem();

        sneakUse(erlang, axe);
        check(h, !com.jttw.engine.item.ErlangAxeItem.isLarge(axe), "and back");

        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        qi(other, 100f, 100f);
        ItemStack theirs = sneakUse(other, new ItemStack(ModItems.ERLANG_AXE.get()));
        check(h, !com.jttw.engine.item.ErlangAxeItem.isLarge(theirs), "only Erlang grows it");
        h.succeed();
    }

    private static double modOf(ItemStack stack, net.minecraft.world.entity.ai.attributes.Attribute attr) {
        return stack.getAttributeModifiers(EquipmentSlot.MAINHAND).get(attr).stream()
                .mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::getAmount).sum();
    }

    @GameTest(template = PLATFORM)
    public static void nezhaLightsTheSpearAndOnlyALitSpearIgnites(GameTestHelper h) {
        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        qi(nezha, 100f, 100f);
        ItemStack spear = new ItemStack(ModItems.FIRE_TIP_SPEAR.get());
        Zombie z = mob(h, EntityType.ZOMBIE, 2, 3);
        spear.getItem().hurtEnemy(spear, z, nezha);
        check(h, !z.isOnFire(), "unlit: no fire");
        double plain = modOf(spear, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        sneakUse(nezha, spear);
        check(h, com.jttw.engine.item.FireTipSpearItem.isAflame(spear), "sneak + right-click: lit");
        check(h, modOf(spear, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > plain, "hits harder");
        spear.getItem().hurtEnemy(spear, z, nezha);
        check(h, z.isOnFire(), "and ignites");
        z.discard();

        ServerPlayer other = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        qi(other, 100f, 100f);
        check(h, !com.jttw.engine.item.FireTipSpearItem.isAflame(sneakUse(other, new ItemStack(ModItems.FIRE_TIP_SPEAR.get()))),
                "only Nezha lights it");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theSpearsFireballIsAPlainRightClick(GameTestHelper h) {
        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        ItemStack spear = new ItemStack(ModItems.FIRE_TIP_SPEAR.get());
        nezha.setItemInHand(InteractionHand.MAIN_HAND, spear);
        spear.use(nezha.level(), nezha, InteractionHand.MAIN_HAND);
        check(h, !near(nezha, net.minecraft.world.entity.projectile.SmallFireball.class, 4).isEmpty(), "a fireball");
        near(nezha, net.minecraft.world.entity.projectile.SmallFireball.class, 4).forEach(f -> f.discard());
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void everyEnhancedFormDrawsQiAndFallsBackWhenItRunsOut(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        ItemStack axe = new ItemStack(ModItems.ERLANG_AXE.get());
        com.jttw.engine.item.ErlangAxeItem.setLarge(axe, true);
        erlang.setItemInHand(InteractionHand.MAIN_HAND, axe);
        qi(erlang, 10f, 100f);
        WieldRules.drawFormQi(erlang);
        near(h, 10f - com.jttw.engine.item.ErlangAxeItem.LARGE_QI_PER_SECOND, QiData.getCurrent(erlang), "a second's Qi");
        qi(erlang, 1f, 100f);
        WieldRules.drawFormQi(erlang);
        check(h, !com.jttw.engine.item.ErlangAxeItem.isLarge(axe), "out of Qi: back to its size");

        ServerPlayer wukong = as(h, ReincarnationOutcome.CELESTIAL_PRIMATE);
        ItemStack staff = new ItemStack(ModItems.RUYI_JINGU_BANG.get());
        staff.getOrCreateTag().putBoolean(com.jttw.engine.item.RuyiJinguBangItem.TAG_LARGE_MODE, true);
        wukong.setItemInHand(InteractionHand.MAIN_HAND, staff);
        qi(wukong, 20f, 100f);
        WieldRules.drawFormQi(wukong);
        near(h, 20f - WieldRules.RUYI_LARGE_QI, QiData.getCurrent(wukong), "the Ruyi's large form draws Qi too");

        ServerPlayer nezha = as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        ItemStack spear = new ItemStack(ModItems.FIRE_TIP_SPEAR.get());
        com.jttw.engine.item.FireTipSpearItem.setAflame(spear, true);
        nezha.setItemInHand(InteractionHand.MAIN_HAND, spear);
        qi(nezha, 2f, 100f);
        WieldRules.drawFormQi(nezha);
        check(h, !com.jttw.engine.item.FireTipSpearItem.isAflame(spear), "the spear's fire goes out without Qi");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void thePillGivesEveryoneQi(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        qi(p, 0f, 10000f);
        ItemStack pill = new ItemStack(ModItems.IMMORTAL_PILL.get());
        pill.finishUsingItem(p.level(), p);
        near(h, 4700f, QiData.getCurrent(p), "as much Qi as the peach");
        check(h, !p.hasEffect(MobEffects.CONFUSION), "and no nausea");
        h.succeed();
    }
}
