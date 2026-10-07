package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.nezha.NezhaAbilityHandler;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;
import static com.jttw.engine.gametest.item.ItemTestSupport.*;

/** Nezha's Wind Fire Wheels (boots, flight) and Huntian Cloth (chestplate, magic defence). */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class NezhaArmorGameTests {

    private static ItemStack wheels() {
        return new ItemStack(ModItems.WIND_FIRE_WHEELS.get());
    }

    private static ItemStack cloth() {
        return new ItemStack(ModItems.HUNTIAN_CLOTH.get());
    }

    private static ServerPlayer nezha(GameTestHelper h) {
        return as(h, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
    }

    private static void wear(ServerPlayer p, ItemStack stack, EquipmentSlot slot) {
        p.setItemSlot(slot, stack);
        applyEquipmentModifiers(p, stack, slot);
    }

    // ── Wind Fire Wheels ──────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void wheelsAreNetheriteGradeBoots(GameTestHelper h) {
        ItemStack w = wheels();
        check(h, w.getItem() instanceof ArmorItem, "armor item");
        eq(h, EquipmentSlot.FEET, ((ArmorItem) w.getItem()).getEquipmentSlot(), "worn on the feet");
        ServerPlayer p = player(h);
        wear(p, w, EquipmentSlot.FEET);
        near(h, 3.0, p.getAttributeValue(Attributes.ARMOR), "armor");
        near(h, 3.0, p.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness");
        near(h, 0.1, p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "knockback resistance");
        check(h, w.getAttributeModifiers(EquipmentSlot.MAINHAND).isEmpty(), "nothing when held");
        h.succeed();
    }

    private static void endTick(ServerPlayer p) {
        NezhaAbilityHandler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, p));
    }

    @GameTest(template = PLATFORM)
    public static void nezhaFliesWithWheelsAndTheFlightNode(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        SkillTreeData.grantNode(p, "nz_flight_1");
        p.setItemSlot(EquipmentSlot.FEET, wheels());
        endTick(p);
        check(h, JTTWFlight.hasUniversalFlight(p), "flight granted");

        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        endTick(p);
        check(h, !JTTWFlight.hasUniversalFlight(p), "flight revoked when the wheels come off");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wheelsAloneOrTheNodeAloneGiveNoFlight(GameTestHelper h) {
        ServerPlayer noNode = nezha(h);
        noNode.setItemSlot(EquipmentSlot.FEET, wheels());
        endTick(noNode);
        check(h, !JTTWFlight.hasUniversalFlight(noNode), "wheels without the flight node");

        ServerPlayer noWheels = nezha(h);
        SkillTreeData.grantNode(noWheels, "nz_flight_1");
        endTick(noWheels);
        check(h, !JTTWFlight.hasUniversalFlight(noWheels), "node without the wheels");

        ServerPlayer handHeld = nezha(h);
        SkillTreeData.grantNode(handHeld, "nz_flight_1");
        handHeld.setItemSlot(EquipmentSlot.MAINHAND, wheels());
        endTick(handHeld);
        check(h, !JTTWFlight.hasUniversalFlight(handHeld), "wheels held, not worn");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wheelsGiveOtherCharactersNoFlight(GameTestHelper h) {
        ServerPlayer p = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        SkillTreeData.grantNode(p, "nz_flight_1");
        p.setItemSlot(EquipmentSlot.FEET, wheels());
        endTick(p);
        check(h, !JTTWFlight.hasUniversalFlight(p), "Erlang Shen in wheels");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void wheelHandlerOnlyActsAtTheEndOfTheTick(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        SkillTreeData.grantNode(p, "nz_flight_1");
        p.setItemSlot(EquipmentSlot.FEET, wheels());
        NezhaAbilityHandler.onPlayerTick(new TickEvent.PlayerTickEvent(TickEvent.Phase.START, p));
        check(h, !JTTWFlight.hasUniversalFlight(p), "START phase ignored");
        h.succeed();
    }

    // ── Huntian Cloth ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void clothIsALeatherGradeChestplate(GameTestHelper h) {
        ItemStack c = cloth();
        eq(h, EquipmentSlot.CHEST, ((ArmorItem) c.getItem()).getEquipmentSlot(), "worn on the chest");
        ServerPlayer p = player(h);
        wear(p, c, EquipmentSlot.CHEST);
        near(h, 3.0, p.getAttributeValue(Attributes.ARMOR), "armor");
        near(h, 0.0, p.getAttributeValue(Attributes.ARMOR_TOUGHNESS), "toughness");
        h.succeed();
    }

    private static float afterClothHandler(ServerPlayer p, DamageSource source, float amount) {
        LivingHurtEvent event = new LivingHurtEvent(p, source, amount);
        NezhaAbilityHandler.onMagicDamage(event);
        return event.getAmount();
    }

    @GameTest(template = PLATFORM)
    public static void clothLetsArmorReduceMagicDamageForNezha(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        wear(p, cloth(), EquipmentSlot.CHEST);
        var src = p.damageSources();
        // 3 armor, 0 toughness, 10 damage: vanilla absorption keeps 10 x (1 - 0.6 / 25) = 9.76.
        near(h, 9.76, afterClothHandler(p, src.magic(), 10f), "magic");
        near(h, 9.76, afterClothHandler(p, src.indirectMagic(p, null), 10f), "indirect magic");
        near(h, 9.76, afterClothHandler(p, src.wither(), 10f), "wither");
        near(h, 10.0, afterClothHandler(p, src.onFire(), 10f), "fire is not magic");
        near(h, 10.0, afterClothHandler(p, src.generic(), 10f), "generic is not magic");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void clothScalesWithTheRestOfNezhasArmor(GameTestHelper h) {
        ServerPlayer p = nezha(h);
        wear(p, cloth(), EquipmentSlot.CHEST);
        wear(p, wheels(), EquipmentSlot.FEET);
        // 6 armor, 3 toughness, 10 damage: 10 / (2 + 3/4) = 3.636; 6 - 3.636 = 2.364 armor counts, keeping 9.0545.
        near(h, 9.0545, afterClothHandler(p, p.damageSources().magic(), 10f), 1e-3, "magic");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void clothDoesNothingForOthersOrWhenNotWorn(GameTestHelper h) {
        ServerPlayer erlang = as(h, ReincarnationOutcome.EYE_OF_HEAVEN);
        wear(erlang, cloth(), EquipmentSlot.CHEST);
        near(h, 10.0, afterClothHandler(erlang, erlang.damageSources().magic(), 10f), "Erlang Shen in the cloth");

        ServerPlayer bare = nezha(h);
        wear(bare, wheels(), EquipmentSlot.FEET);
        near(h, 10.0, afterClothHandler(bare, bare.damageSources().magic(), 10f), "Nezha without the cloth");

        ServerPlayer unarmoured = nezha(h);
        unarmoured.setItemSlot(EquipmentSlot.CHEST, cloth()); // worn but its armour not yet applied
        near(h, 10.0, afterClothHandler(unarmoured, unarmoured.damageSources().magic(), 10f), "no armour to use");
        h.succeed();
    }
}
