package com.jttw.engine.gametest.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.ErlangAxeItem;
import com.jttw.engine.item.FireTipSpearItem;
import com.jttw.engine.item.HuntianClothItem;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.item.ImmortalPeachItem;
import com.jttw.engine.item.QiankunRingItem;
import com.jttw.engine.item.RuyiJinguBangItem;
import com.jttw.engine.item.ThreePointedSpearItem;
import com.jttw.engine.item.WindFireWheelsItem;
import com.jttw.engine.item.YinYangSwordItem;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.registry.ModItems;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jttw.engine.gametest.GT.*;

/** Every item in ModItems is registered under its id, as the right class, with its stack rules. */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ModItemsGameTests {

    private static final Map<String, Class<? extends Item>> EXPECTED = Map.ofEntries(
            Map.entry("ginseng_fruit", com.jttw.engine.item.GinsengFruitItem.class),
            Map.entry("golden_striker", com.jttw.engine.item.GoldenStrikerItem.class),
            Map.entry("silk_lined_tray", com.jttw.engine.item.SilkLinedTrayItem.class),
            Map.entry("zhenyuan_flywhisk", com.jttw.engine.item.FlywhiskItem.class),
            Map.entry("crane_plume", Item.class),
            Map.entry("daoist_talisman", Item.class),
            Map.entry("daoist_disciple_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("cloud_crane_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("clear_wind_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("bright_moon_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("zhenyuan_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("immortal_peach", ImmortalPeachItem.class),
            Map.entry("celestial_cloud", BlockItem.class),
            Map.entry("jttw_immortal_pill", ImmortalItems.ImmortalPillItem.class),
            Map.entry("ruyi_jingu_bang", RuyiJinguBangItem.class),
            Map.entry("three_pointed_spear", ThreePointedSpearItem.class),
            Map.entry("erlang_axe", ErlangAxeItem.class),
            Map.entry("fire_tip_spear", FireTipSpearItem.class),
            Map.entry("qiankun_ring", QiankunRingItem.class),
            Map.entry("yin_yang_sword", YinYangSwordItem.class),
            Map.entry("wind_fire_wheels", WindFireWheelsItem.class),
            Map.entry("huntian_cloth", HuntianClothItem.class),
            Map.entry("bell_of_taiyi", com.jttw.engine.item.BellOfTaiyiItem.class),
            Map.entry("nine_tooth_rake", com.jttw.engine.item.NineToothRakeItem.class),
            Map.entry("yin_yang_vase", com.jttw.engine.item.YinYangVaseItem.class),
            Map.entry("mixed_iron_cudgel", com.jttw.engine.item.MixedIronCudgelItem.class),
            Map.entry("banana_leaf_fan", com.jttw.engine.item.BananaLeafFanItem.class),
            Map.entry("red_boy_spear", com.jttw.engine.item.RedBoySpearItem.class),
            Map.entry("bone_servant_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("wraith_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("lady_white_bone_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("eight_trigrams_furnace", com.jttw.engine.world.furnace.TrigramsFurnaceItem.class),
            Map.entry("true_samadhi_fire", Item.class),
            Map.entry("jade_block", BlockItem.class),
            Map.entry("heavenly_wine", com.jttw.engine.world.heaven.HeavenlyWineItem.class),
            Map.entry("servant_bone", Item.class),
            Map.entry("wraith_wisp", Item.class),
            Map.entry("captains_sigil", Item.class),
            Map.entry("heart_bone", Item.class),
            Map.entry("heavenly_halberd", net.minecraft.world.item.SwordItem.class),
            Map.entry("golden_bow", com.jttw.engine.item.GoldenBowItem.class),
            Map.entry("bleached_stone", BlockItem.class),
            Map.entry("grave_tuff", BlockItem.class),
            Map.entry("bone_andesite", BlockItem.class),
            Map.entry("ossified_bone", BlockItem.class),
            Map.entry("bone_gravel", BlockItem.class),
            Map.entry("celestial_marble", BlockItem.class),
            Map.entry("underworld_stone", BlockItem.class),
            Map.entry("soul_ash", BlockItem.class),
            Map.entry("spirit_whisker", Item.class),
            Map.entry("night_petal", Item.class),
            Map.entry("spirit_mountain_candle", Item.class),
            Map.entry("embroidered_shoe", Item.class),
            Map.entry("earth_flow_sword_male", com.jttw.engine.item.EarthFlowSwordItem.class),
            Map.entry("earth_flow_sword_female", com.jttw.engine.item.EarthFlowSwordItem.class),
            Map.entry("mouse_spirit_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("flower_maid_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("lady_earth_flow_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("water_repelling_pearl", com.jttw.engine.item.WaterRepellingPearlItem.class),
            Map.entry("phoenix_plume_cap", com.jttw.engine.item.DragonRegaliaItem.class),
            Map.entry("golden_chain_mail", com.jttw.engine.item.DragonRegaliaItem.class),
            Map.entry("cloud_stepping_boots", com.jttw.engine.item.DragonRegaliaItem.class),
            Map.entry("shrimp_soldier_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("crab_general_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("yaksha_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("ao_guang_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("ao_qin_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("ao_run_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("ao_shun_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("hell_bank_note", Item.class),
            Map.entry("ox_horn", Item.class),
            Map.entry("horse_mane", Item.class),
            Map.entry("underworld_ink", Item.class),
            Map.entry("soul_chain", Item.class),
            Map.entry("karma_mirror_shard", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("rime_shackle", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("iron_soul_hook", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("scales_weight", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("yama_seal", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("book_page", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("wailing_bell", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("oil_cauldron_ember", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("cart_wheel_spoke", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("equal_measure_rod", com.jttw.engine.item.HellRelicItem.class),
            Map.entry("yama_brush", com.jttw.engine.item.JudgeBrushItem.class),
            Map.entry("meng_po_soup", com.jttw.engine.item.MengPoSoupItem.class),
            Map.entry("ox_head_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("horse_face_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("judge_clerk_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("black_impermanence_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("white_impermanence_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("wandering_ghost_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_qinguang_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_chujiang_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_songdi_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_wuguan_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_yama_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_biancheng_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_taishan_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_dushi_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_pingdeng_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("king_zhuanlun_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("ember_core", Item.class),
            Map.entry("furnace_brick", Item.class),
            Map.entry("flame_rock", BlockItem.class),
            Map.entry("cinder_ash", BlockItem.class),
            Map.entry("burning_earth", BlockItem.class),
            Map.entry("ember_imp_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("furnace_brute_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("strong_general_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("red_boy_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("mane_tuft", Item.class),
            Map.entry("ivory_shard", Item.class),
            Map.entry("storm_feather", Item.class),
            Map.entry("patrol_token", Item.class),
            Map.entry("azure_lion_mane", Item.class),
            Map.entry("white_tusk", Item.class),
            Map.entry("azure_lion_sabre", com.jttw.engine.item.AzureLionSabreItem.class),
            Map.entry("white_elephant_lance", com.jttw.engine.item.WhiteElephantLanceItem.class),
            Map.entry("lion_demon_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("elephant_demon_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("peng_demon_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("little_wind_driller_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("azure_lion_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class),
            Map.entry("white_elephant_spawn_egg", net.neoforged.neoforge.common.DeferredSpawnEggItem.class));

    @GameTest(template = PLATFORM)
    public static void everyModItemIsRegisteredUnderItsIdAsTheRightClass(GameTestHelper h) {
        eq(h, EXPECTED.size(), ModItems.ITEMS.getEntries().size(), "number of registered items");
        for (DeferredHolder<Item, ? extends Item> entry : ModItems.ITEMS.getEntries()) {
            String id = entry.getId().getPath();
            eq(h, JTTWEngine.MOD_ID, entry.getId().getNamespace(), id + " namespace");
            check(h, entry.isBound(), id + " is present");
            Item item = entry.get();
            check(h, item != null, id + " is non-null");
            Class<? extends Item> expected = EXPECTED.get(id);
            check(h, expected != null, "unexpected item id " + id);
            eq(h, expected, item.getClass(), id + " class");
            eq(h, item, BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, id)),
                    id + " in the item registry");
        }
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void registryObjectsPointAtTheirOwnIds(GameTestHelper h) {
        eq(h, "immortal_peach", ModItems.IMMORTAL_PEACH.getId().getPath(), "IMMORTAL_PEACH");
        eq(h, "celestial_cloud", ModItems.CELESTIAL_CLOUD_ITEM.getId().getPath(), "CELESTIAL_CLOUD_ITEM");
        eq(h, "jttw_immortal_pill", ModItems.IMMORTAL_PILL.getId().getPath(), "IMMORTAL_PILL");
        eq(h, "ruyi_jingu_bang", ModItems.RUYI_JINGU_BANG.getId().getPath(), "RUYI_JINGU_BANG");
        eq(h, "three_pointed_spear", ModItems.THREE_POINTED_SPEAR.getId().getPath(), "THREE_POINTED_SPEAR");
        eq(h, "erlang_axe", ModItems.ERLANG_AXE.getId().getPath(), "ERLANG_AXE");
        eq(h, "fire_tip_spear", ModItems.FIRE_TIP_SPEAR.getId().getPath(), "FIRE_TIP_SPEAR");
        eq(h, "qiankun_ring", ModItems.QIANKUN_RING.getId().getPath(), "QIANKUN_RING");
        eq(h, "yin_yang_sword", ModItems.YIN_YANG_SWORD.getId().getPath(), "YIN_YANG_SWORD");
        eq(h, "wind_fire_wheels", ModItems.WIND_FIRE_WHEELS.getId().getPath(), "WIND_FIRE_WHEELS");
        eq(h, "huntian_cloth", ModItems.HUNTIAN_CLOTH.getId().getPath(), "HUNTIAN_CLOTH");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void celestialCloudItemPlacesTheCelestialCloudBlock(GameTestHelper h) {
        Item item = ModItems.CELESTIAL_CLOUD_ITEM.get();
        check(h, item instanceof BlockItem, "is a block item");
        eq(h, ModBlocks.CELESTIAL_CLOUD.get(), ((BlockItem) item).getBlock(), "block");
        h.succeed();
    }

    @GameTest(template = PLATFORM)
    public static void stackSizesRarityAndFireResistance(GameTestHelper h) {
        eq(h, 16, ModItems.IMMORTAL_PEACH.get().getDefaultMaxStackSize(), "peach stack size");
        eq(h, 1, ModItems.IMMORTAL_PILL.get().getDefaultMaxStackSize(), "pill stack size");
        for (DeferredHolder<Item, ? extends Item> artifact : List.of(ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR,
                ModItems.ERLANG_AXE, ModItems.FIRE_TIP_SPEAR, ModItems.QIANKUN_RING, ModItems.YIN_YANG_SWORD,
                ModItems.WIND_FIRE_WHEELS, ModItems.HUNTIAN_CLOTH, ModItems.BELL_OF_TAIYI,
                ModItems.NINE_TOOTH_RAKE, ModItems.YIN_YANG_VASE, ModItems.MIXED_IRON_CUDGEL, ModItems.BANANA_LEAF_FAN)) {
            eq(h, 1, artifact.get().getDefaultMaxStackSize(), artifact.getId().getPath() + " stack size");
        }
        for (DeferredHolder<Item, ? extends Item> epic : List.of(ModItems.IMMORTAL_PEACH, ModItems.IMMORTAL_PILL,
                ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR, ModItems.ERLANG_AXE,
                ModItems.FIRE_TIP_SPEAR, ModItems.QIANKUN_RING, ModItems.YIN_YANG_SWORD,
                ModItems.WIND_FIRE_WHEELS, ModItems.HUNTIAN_CLOTH, ModItems.BELL_OF_TAIYI,
                ModItems.NINE_TOOTH_RAKE, ModItems.YIN_YANG_VASE, ModItems.MIXED_IRON_CUDGEL, ModItems.BANANA_LEAF_FAN)) {
            eq(h, Rarity.EPIC, new ItemStack(epic.get()).getRarity(), epic.getId().getPath() + " rarity");
        }
        for (DeferredHolder<Item, ? extends Item> fireproof : List.of(ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR,
                ModItems.ERLANG_AXE, ModItems.FIRE_TIP_SPEAR, ModItems.WIND_FIRE_WHEELS,
                ModItems.NINE_TOOTH_RAKE, ModItems.MIXED_IRON_CUDGEL)) {
            check(h, new ItemStack(fireproof.get()).has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT), fireproof.getId().getPath() + " survives lava");
        }
        for (DeferredHolder<Item, ? extends Item> burnable : List.of(ModItems.QIANKUN_RING, ModItems.YIN_YANG_SWORD,
                ModItems.HUNTIAN_CLOTH, ModItems.IMMORTAL_PEACH)) {
            check(h, !new ItemStack(burnable.get()).has(net.minecraft.core.component.DataComponents.FIRE_RESISTANT), burnable.getId().getPath() + " is not fire resistant");
        }
        check(h, new ItemStack(ModItems.IMMORTAL_PEACH.get()).has(net.minecraft.core.component.DataComponents.FOOD), "peach is edible");
        check(h, new ItemStack(ModItems.IMMORTAL_PILL.get()).has(net.minecraft.core.component.DataComponents.FOOD), "pill is edible");
        h.succeed();
    }

    /**
     * Every registered item should ship a display name and an item model; without them the
     * client shows the raw translation key and the purple-black missing texture.
     */
    @GameTest(template = PLATFORM)
    public static void everyItemHasALangEntryAndAnItemModel(GameTestHelper h) {
        var modFile = ModList.get().getModFileById(JTTWEngine.MOD_ID).getFile();
        JsonObject lang;
        try (Reader r = Files.newBufferedReader(modFile.findResource("assets", JTTWEngine.MOD_ID, "lang", "en_us.json"))) {
            lang = JsonParser.parseReader(r).getAsJsonObject();
        } catch (Exception e) {
            throw new IllegalStateException("cannot read en_us.json", e);
        }
        List<String> missing = new ArrayList<>();
        for (DeferredHolder<Item, ? extends Item> entry : ModItems.ITEMS.getEntries()) {
            String id = entry.getId().getPath();
            String key = entry.get().getDescriptionId();
            if (!lang.has(key)) missing.add("lang " + key);
            Path model = modFile.findResource("assets", JTTWEngine.MOD_ID, "models", "item", id + ".json");
            if (!Files.exists(model)) missing.add("model item/" + id + ".json");
        }
        check(h, missing.isEmpty(), "missing client resources: " + missing);
        h.succeed();
    }
}
