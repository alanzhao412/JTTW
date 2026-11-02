package com.jttw.engine.gametest.item;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.BoneServantEntity;
import com.jttw.engine.gametest.character.CharTest;
import com.jttw.engine.item.ArmamentBonuses;
import com.jttw.engine.item.Armaments;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static com.jttw.engine.gametest.GT.*;

/**
 * Laozi's Eight Trigrams Furnace: it refines the Heavenly armaments' tiers V to VIII and forges its
 * own recipes (the Nine-Turn Golden Elixir), each for a smith its gate lets through.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class FurnaceGameTests {

    private static TrigramsFurnaceBlockEntity furnace(GameTestHelper h) {
        BlockPos at = new BlockPos(2, 2, 2);
        h.setBlock(at, ModBlocks.EIGHT_TRIGRAMS_FURNACE.get());
        return (TrigramsFurnaceBlockEntity) h.getBlockEntity(at);
    }

    /** Runs the furnace so many ticks. */
    private static void run(GameTestHelper h, TrigramsFurnaceBlockEntity f, int ticks) {
        for (int i = 0; i < ticks; i++) {
            TrigramsFurnaceBlockEntity.serverTick(h.getLevel(), f.getBlockPos(), h.getLevel().getBlockState(f.getBlockPos()), f);
        }
    }

    private static void put(TrigramsFurnaceBlockEntity f, int slot, Item item, int count) {
        f.setItem(slot, new ItemStack(item, count));
    }

    /** A smith of this character at the furnace: its recipes' gates are held against them. */
    private static ServerPlayer smith(GameTestHelper h, TrigramsFurnaceBlockEntity f, ReincarnationOutcome who) {
        ServerPlayer p = CharTest.as(h, who);
        f.setSmith(p);
        return p;
    }

    /** Nezha's tier-V refinement (data/jttw_engine/recipe/heavenly/nezha_tier_5.json) into the trigrams. */
    private static void nezhaTierFive(TrigramsFurnaceBlockEntity f) {
        put(f, 0, ModItems.HELL_BANK_NOTE.get(), 4);
        put(f, 1, ModItems.SOUL_CHAIN.get(), 2);
        put(f, 2, ModItems.UNDERWORLD_INK.get(), 2);
        put(f, 3, ModItems.CAPTAINS_SIGIL.get(), 1);
    }

    private static ItemStack spearAt(int tier) {
        ItemStack spear = new ItemStack(ModItems.FIRE_TIP_SPEAR.get());
        Armaments.setTier(spear, tier);
        return spear;
    }

    @GameTest(template = PLATFORM)
    public static void theFurnaceRefinesAnArmamentToTierFiveForItsOwner(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        smith(h, f, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, spearAt(4));
        nezhaTierFive(f);
        put(f, 4, ModItems.HELL_BANK_NOTE.get(), 3); // more than it takes: the rest is left
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        check(h, f.refining() != null && f.refining().tier() == 5, "it would refine the spear to tier V");
        run(h, f, TrigramsFurnaceBlockEntity.REFINE_BASE_TICKS + TrigramsFurnaceBlockEntity.REFINE_TICKS_PER_LEVEL * 3);
        eq(h, 5, Armaments.tier(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "tier V");
        for (int i = 0; i < 4; i++) check(h, f.getItem(i).isEmpty(), "its materials used: slot " + i);
        eq(h, 3, f.getItem(4).getCount(), "what it did not take, left");
        check(h, f.refining() == null, "tier VI takes the Flaming Mountains' materials");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFurnaceAnswersOnlyToTheArmamentsOwner(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, spearAt(4));
        nezhaTierFive(f);
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        smith(h, f, ReincarnationOutcome.EYE_OF_HEAVEN);
        check(h, f.refining() == null, "Erlang Shen cannot refine Nezha's spear");
        smith(h, f, ReincarnationOutcome.BULL_DEMON_KING);
        run(h, f, TrigramsFurnaceBlockEntity.REFINE_BASE_TICKS * 3);
        eq(h, 4, Armaments.tier(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "nor a Yaoguai");
        smith(h, f, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        check(h, f.refining() != null, "Nezha can");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nothingRefinesWithoutFuelOrWithTooLittle(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        smith(h, f, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, spearAt(4));
        nezhaTierFive(f);
        run(h, f, TrigramsFurnaceBlockEntity.REFINE_BASE_TICKS * 3);
        eq(h, 4, Armaments.tier(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "no fire, no refining");
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        put(f, 1, ModItems.SOUL_CHAIN.get(), 1);
        check(h, f.refining() == null, "a soul chain short: nothing");
        put(f, 1, ModItems.SOUL_CHAIN.get(), 2);
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, spearAt(3));
        check(h, f.refining() == null, "a tier-III spear is refined at a crafting table, not here");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theMarshalsRakeIsReforgedWithTheRimeShackleAtTierFive(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        smith(h, f, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        ItemStack rake = new ItemStack(ModItems.NINE_TOOTH_RAKE.get());
        Armaments.setTier(rake, 4);
        f.setItem(TrigramsFurnaceBlockEntity.CENTRE, rake);
        put(f, 0, Items.IRON_INGOT, 9);
        put(f, 1, Items.GOLD_INGOT, 3);
        put(f, 3, ModItems.JADE_BLOCK_ITEM.get(), 1);
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        check(h, f.refining() == null, "not without King Chujiang's Rime Shackle");
        put(f, 2, ModItems.RIME_SHACKLE.get(), 1);
        check(h, f.refining() != null, "with it, as Laozi forged it");
        run(h, f, TrigramsFurnaceBlockEntity.REFINE_BASE_TICKS + TrigramsFurnaceBlockEntity.REFINE_TICKS_PER_LEVEL * 3);
        eq(h, 5, Armaments.tier(f.getItem(TrigramsFurnaceBlockEntity.CENTRE)), "the rake at tier V");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void theFurnaceForgesTheGoldenElixirForTheHeavenlyAlone(GameTestHelper h) {
        TrigramsFurnaceBlockEntity f = furnace(h);
        put(f, 0, Items.GOLD_INGOT, 3);
        put(f, 1, ModItems.WRAITH_WISP.get(), 2);
        put(f, 2, ModItems.IMMORTAL_PEACH.get(), 1);
        put(f, 3, ModItems.SERVANT_BONE.get(), 1);
        put(f, TrigramsFurnaceBlockEntity.FUEL, ModItems.TRUE_SAMADHI_FIRE.get(), 8);
        smith(h, f, ReincarnationOutcome.PHOENIX);
        check(h, f.forgingRecipe() == null, "a Divine Beast cannot forge it");
        smith(h, f, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        check(h, f.forgingRecipe() != null, "any Heavenly can");
        run(h, f, TrigramsFurnaceBlockEntity.FORGE_TICKS);
        check(h, f.getItem(TrigramsFurnaceBlockEntity.CENTRE).is(ModItems.NINE_TURN_ELIXIR.get()), "the elixir forged");
        eq(h, 2, f.getItem(TrigramsFurnaceBlockEntity.CENTRE).getCount(), "two pills");
        h.succeed();
    }

    private static double attackBonus(ItemStack stack) {
        return ItemTestSupport.modifiers(stack, EquipmentSlot.MAINHAND, Attributes.ATTACK_DAMAGE).stream()
                .mapToDouble(net.minecraft.world.entity.ai.attributes.AttributeModifier::amount).sum();
    }

    @GameTest(template = PLATFORM)
    public static void aHigherTierGivesMoreOfItsBonus(GameTestHelper h) {
        ItemStack spear = new ItemStack(ModItems.THREE_POINTED_SPEAR.get());
        double base = ArmamentBonuses.THREE_POINTED_SPEAR;
        near(h, base, attackBonus(spear), "its own bonus at tier I");
        for (int t = 2; t <= Armaments.TIERS; t++) {
            Armaments.setTier(spear, t);
            near(h, base * (1 + (t - 1) * 0.1), attackBonus(spear), "x" + (1 + (t - 1) * 0.1) + " at tier " + t);
        }
        ItemStack rake = new ItemStack(ModItems.NINE_TOOTH_RAKE.get());
        check(h, attackBonus(rake) >= ArmamentBonuses.NINE_TOOTH_RAKE, "the rake's own bonus too: " + attackBonus(rake));
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void aCaptainLeavesItsSigil(GameTestHelper h) {
        ServerPlayer p = player(h);
        BoneServantEntity captain = h.spawn(ModEntities.BONE_SERVANT.get(), new BlockPos(3, 2, 3));
        captain.become(27, true);
        captain.hurt(h.getLevel().damageSources().playerAttack(p), Float.MAX_VALUE);
        var sigils = h.getLevel().getEntitiesOfClass(ItemEntity.class, captain.getBoundingBox().inflate(4),
                e -> e.getItem().is(ModItems.CAPTAINS_SIGIL.get()));
        check(h, !sigils.isEmpty(), "a sigil dropped");
        sigils.forEach(ItemEntity::discard);
        h.succeed();
    }
}
