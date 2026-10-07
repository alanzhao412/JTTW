package com.jttw.engine.gametest.transformation;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.c2s.TransmuteItemPacket;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import com.jttw.engine.transformation.transmute.TransmutationCategory;
import com.jttw.engine.transformation.transmute.TransmutationHelper;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * Transmutation: the option tables (tools within a tier, crops, raw and cooked foods) and the
 * server's handling of a transmute request — who may transmute and what the held stack becomes.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class TransmutationGameTests {

    // ── option tables ─────────────────────────────────────────────

    @GameTest(template = PLATFORM)
    public static void toolOptionsAreTheOtherToolsOfTheSameTier(GameTestHelper h) {
        eq(h, List.of(Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SHOVEL),
                TransmutationHelper.getToolOptions(new ItemStack(Items.IRON_SWORD)), "iron sword");
        eq(h, List.of(Items.NETHERITE_SWORD, Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_HOE),
                TransmutationHelper.getToolOptions(new ItemStack(Items.NETHERITE_SHOVEL)), "netherite shovel");
        eq(h, List.of(Items.WOODEN_SWORD, Items.WOODEN_PICKAXE, Items.WOODEN_HOE, Items.WOODEN_SHOVEL),
                TransmutationHelper.getToolOptions(new ItemStack(Items.WOODEN_AXE)), "wooden axe");
        eq(h, List.of(Items.STONE_SWORD, Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL),
                TransmutationHelper.getToolOptions(new ItemStack(Items.STONE_HOE)), "stone hoe");
        eq(h, List.of(Items.GOLDEN_SWORD, Items.GOLDEN_AXE, Items.GOLDEN_HOE, Items.GOLDEN_SHOVEL),
                TransmutationHelper.getToolOptions(new ItemStack(Items.GOLDEN_PICKAXE)), "golden pickaxe");
        eq(h, List.of(Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.DIAMOND_SHOVEL),
                TransmutationHelper.getToolOptions(new ItemStack(Items.DIAMOND_SWORD)), "diamond sword");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void nonToolsHaveNoToolOptions(GameTestHelper h) {
        for (Item item : new Item[]{Items.STICK, Items.BOW, Items.SHEARS, Items.TRIDENT, Items.IRON_INGOT}) {
            check(h, TransmutationHelper.getToolOptions(new ItemStack(item)).isEmpty(), item + " has no tool options");
            check(h, !TransmutationHelper.isTransmutableTool(new ItemStack(item)), item + " is not a transmutable tool");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void cropAndFoodOptionsExcludeTheHeldItem(GameTestHelper h) {
        eq(h, List.of(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.POTATO, Items.MELON_SEEDS, Items.PUMPKIN_SEEDS),
                TransmutationHelper.getCropOptions(new ItemStack(Items.CARROT)), "carrot");
        eq(h, List.of(Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT, Items.COD, Items.SALMON),
                TransmutationHelper.getRawFoodOptions(new ItemStack(Items.BEEF)), "raw beef");
        eq(h, List.of(Items.COOKED_BEEF, Items.COOKED_PORKCHOP, Items.COOKED_CHICKEN, Items.COOKED_MUTTON,
                        Items.COOKED_RABBIT, Items.COOKED_COD),
                TransmutationHelper.getCookedFoodOptions(new ItemStack(Items.COOKED_SALMON)), "cooked salmon");
        check(h, TransmutationHelper.getCropOptions(new ItemStack(Items.BEEF)).isEmpty(), "beef is not a crop");
        check(h, TransmutationHelper.getRawFoodOptions(new ItemStack(Items.COOKED_BEEF)).isEmpty(), "cooked is not raw");
        check(h, TransmutationHelper.getCookedFoodOptions(new ItemStack(Items.BEEF)).isEmpty(), "raw is not cooked");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void detectCategorySortsHeldItems(GameTestHelper h) {
        eq(h, TransmutationCategory.TOOL, TransmutationHelper.detectCategory(new ItemStack(Items.GOLDEN_HOE)), "golden hoe");
        eq(h, TransmutationCategory.CROP, TransmutationHelper.detectCategory(new ItemStack(Items.PUMPKIN_SEEDS)), "pumpkin seeds");
        eq(h, TransmutationCategory.RAW_FOOD, TransmutationHelper.detectCategory(new ItemStack(Items.MUTTON)), "mutton");
        eq(h, TransmutationCategory.COOKED_FOOD, TransmutationHelper.detectCategory(new ItemStack(Items.COOKED_COD)), "cooked cod");
        check(h, TransmutationHelper.detectCategory(ItemStack.EMPTY) == null, "empty hand");
        for (Item item : new Item[]{Items.WHEAT, Items.BREAD, Items.STICK, Items.DIRT, Items.BOW}) {
            check(h, TransmutationHelper.detectCategory(new ItemStack(item)) == null, item + " is not transmutable");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void getTargetPicksByIndexAndRefusesOutOfRange(GameTestHelper h) {
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        eq(h, Items.IRON_PICKAXE, TransmutationHelper.getTarget(TransmutationCategory.TOOL, sword, 0), "first");
        eq(h, Items.IRON_SHOVEL, TransmutationHelper.getTarget(TransmutationCategory.TOOL, sword, 3), "last");
        check(h, TransmutationHelper.getTarget(TransmutationCategory.TOOL, sword, 4) == null, "past the end");
        check(h, TransmutationHelper.getTarget(TransmutationCategory.TOOL, sword, -1) == null, "negative");
        check(h, TransmutationHelper.getTarget(TransmutationCategory.CROP, sword, 0) == null, "sword is not a crop");
        check(h, TransmutationHelper.getOptions(TransmutationCategory.RAW_FOOD, sword).isEmpty(), "no raw-food options for a sword");

        ItemStack potato = new ItemStack(Items.POTATO);
        eq(h, Items.WHEAT_SEEDS, TransmutationHelper.getTarget(TransmutationCategory.CROP, potato, 0), "potato -> wheat seeds");
        check(h, TransmutationHelper.getTarget(TransmutationCategory.CROP, potato, 5) == null, "only five crop options");
        eq(h, Items.SALMON, TransmutationHelper.getTarget(TransmutationCategory.RAW_FOOD, new ItemStack(Items.COD), 5), "cod -> salmon");
        eq(h, Items.COOKED_PORKCHOP, TransmutationHelper.getTarget(TransmutationCategory.COOKED_FOOD,
                new ItemStack(Items.COOKED_BEEF), 0), "cooked beef -> cooked porkchop");
        check(h, TransmutationHelper.getTarget(TransmutationCategory.COOKED_FOOD,
                new ItemStack(Items.COOKED_BEEF), 6) == null, "only six cooked options");
        h.succeed();
    }

    // ── TransmuteItemPacket (server side) ─────────────────────────

    @GameTest(template = PLATFORM)
    public static void wukongTierThreeTransmutesAToolKeepingItsEnchantsWearAndName(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(Enchantments.UNBREAKING, 2);
        sword.setDamageValue(100);
        sword.setHoverName(Component.literal("Ruyi"));
        sword.setRepairCost(7);
        p.setItemInHand(InteractionHand.MAIN_HAND, sword);
        Tf.send(p, new TransmuteItemPacket(TransmutationCategory.TOOL, 1));
        ItemStack out = p.getMainHandItem();
        eq(h, Items.IRON_AXE, out.getItem(), "iron sword -> option 1, the iron axe");
        eq(h, 1, out.getCount(), "one tool");
        eq(h, 2, EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, out), "unbreaking kept");
        // same tier, same max durability (250): wear carries over unchanged
        eq(h, 100, out.getDamageValue(), "damage kept");
        eq(h, "Ruyi", out.getHoverName().getString(), "name kept");
        eq(h, 7, out.getBaseRepairCost(), "repair cost kept");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void erlangTierThreeAndTheMarshalMayTransmute(GameTestHelper h) {
        ServerPlayer erlang = Tf.player(h);
        Tf.grantErlangTier(erlang, 3);
        erlang.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BEEF, 5));
        Tf.send(erlang, new TransmuteItemPacket(TransmutationCategory.RAW_FOOD, 0));
        eq(h, Items.PORKCHOP, erlang.getMainHandItem().getItem(), "Erlang tier 3");

        ServerPlayer marshal = Tf.player(h);
        AlignmentData.markReincarnated(marshal, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        marshal.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKED_BEEF, 5));
        Tf.send(marshal, new TransmuteItemPacket(TransmutationCategory.COOKED_FOOD, 1));
        eq(h, Items.COOKED_CHICKEN, marshal.getMainHandItem().getItem(), "Marshal, no nodes");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void cropsAndFoodKeepTheStackSizeAndName(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        ItemStack carrots = new ItemStack(Items.CARROT, 17);
        carrots.setHoverName(Component.literal("Peach seeds"));
        p.setItemInHand(InteractionHand.MAIN_HAND, carrots);
        Tf.send(p, new TransmuteItemPacket(TransmutationCategory.CROP, 2));
        ItemStack out = p.getMainHandItem();
        eq(h, Items.POTATO, out.getItem(), "carrot -> option 2, the potato");
        eq(h, 17, out.getCount(), "count kept");
        eq(h, "Peach seeds", out.getHoverName().getString(), "name kept");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void transmuteChangesOnlyTheSelectedHotbarSlot(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        p.getInventory().selected = 4;
        p.getInventory().setItem(4, new ItemStack(Items.MUTTON, 3));
        p.getInventory().setItem(0, new ItemStack(Items.BEEF, 3));
        p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.CHICKEN, 3));
        Tf.send(p, new TransmuteItemPacket(TransmutationCategory.RAW_FOOD, 0));
        eq(h, Items.BEEF, p.getInventory().getItem(4).getItem(), "selected slot: mutton -> beef");
        eq(h, Items.BEEF, p.getInventory().getItem(0).getItem(), "slot 0 untouched");
        eq(h, Items.CHICKEN, p.getOffhandItem().getItem(), "offhand untouched");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void transmuteIsRefusedBelowTierThree(GameTestHelper h) {
        ServerPlayer none = Tf.player(h);
        holdAndSend(none, new ItemStack(Items.IRON_SWORD), TransmutationCategory.TOOL, 0);
        eq(h, Items.IRON_SWORD, none.getMainHandItem().getItem(), "no nodes");

        ServerPlayer wukong1 = Tf.player(h);
        Tf.grantWukongTier(wukong1, 1);
        holdAndSend(wukong1, new ItemStack(Items.IRON_SWORD), TransmutationCategory.TOOL, 0);
        eq(h, Items.IRON_SWORD, wukong1.getMainHandItem().getItem(), "Wukong tier 1");

        ServerPlayer other = Tf.player(h);
        AlignmentData.markReincarnated(other, ReincarnationOutcome.PHOENIX);
        holdAndSend(other, new ItemStack(Items.IRON_SWORD), TransmutationCategory.TOOL, 0);
        eq(h, Items.IRON_SWORD, other.getMainHandItem().getItem(), "another character, no nodes");
        h.succeed();
    }

    /** "72 Transformations III" is the node that says "Transmute equipped items". */
    @GameTest(template = PLATFORM)
    public static void transmuteNeedsTierThreeAsTheNodeSays(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 2);
        holdAndSend(p, new ItemStack(Items.IRON_SWORD), TransmutationCategory.TOOL, 0);
        eq(h, Items.IRON_SWORD, p.getMainHandItem().getItem(), "tier 2 cannot transmute");
        Tf.grantWukongTier(p, 3);
        holdAndSend(p, new ItemStack(Items.IRON_SWORD), TransmutationCategory.TOOL, 0);
        eq(h, Items.IRON_PICKAXE, p.getMainHandItem().getItem(), "tier 3 transmutes");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void badRequestsLeaveTheHeldItemAlone(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        holdAndSend(p, new ItemStack(Items.IRON_SWORD), TransmutationCategory.TOOL, 4);
        eq(h, Items.IRON_SWORD, p.getMainHandItem().getItem(), "index past the end");
        holdAndSend(p, new ItemStack(Items.IRON_SWORD), TransmutationCategory.TOOL, -1);
        eq(h, Items.IRON_SWORD, p.getMainHandItem().getItem(), "negative index");
        holdAndSend(p, new ItemStack(Items.IRON_SWORD), TransmutationCategory.CROP, 0);
        eq(h, Items.IRON_SWORD, p.getMainHandItem().getItem(), "category does not match the item");
        holdAndSend(p, new ItemStack(Items.STICK, 9), TransmutationCategory.TOOL, 0);
        eq(h, Items.STICK, p.getMainHandItem().getItem(), "untransmutable item");
        eq(h, 9, p.getMainHandItem().getCount(), "count untouched");

        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Tf.send(p, new TransmuteItemPacket(TransmutationCategory.CROP, 0));
        check(h, p.getMainHandItem().isEmpty(), "empty hand stays empty");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void unknownCategoryFromTheWireIsIgnored(GameTestHelper h) {
        ServerPlayer p = Tf.player(h);
        Tf.grantWukongTier(p, 3);
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeVarInt(99); // no such category
        buf.writeVarInt(0);
        Tf.send(p, TransmuteItemPacket.decode(buf));
        eq(h, Items.IRON_SWORD, p.getMainHandItem().getItem(), "unchanged");
        h.succeed();
    }

    private static void holdAndSend(ServerPlayer p, ItemStack stack, TransmutationCategory cat, int index) {
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        Tf.send(p, new TransmuteItemPacket(cat, index));
    }
}
