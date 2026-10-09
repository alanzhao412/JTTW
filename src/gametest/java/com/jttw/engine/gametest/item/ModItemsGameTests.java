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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

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
            Map.entry("bone_servant_spawn_egg", net.minecraftforge.common.ForgeSpawnEggItem.class),
            Map.entry("wraith_spawn_egg", net.minecraftforge.common.ForgeSpawnEggItem.class),
            Map.entry("lady_white_bone_spawn_egg", net.minecraftforge.common.ForgeSpawnEggItem.class));

    @GameTest(template = PLATFORM)
    public static void everyModItemIsRegisteredUnderItsIdAsTheRightClass(GameTestHelper h) {
        eq(h, EXPECTED.size(), ModItems.ITEMS.getEntries().size(), "number of registered items");
        for (RegistryObject<Item> entry : ModItems.ITEMS.getEntries()) {
            String id = entry.getId().getPath();
            eq(h, JTTWEngine.MOD_ID, entry.getId().getNamespace(), id + " namespace");
            check(h, entry.isPresent(), id + " is present");
            Item item = entry.get();
            check(h, item != null, id + " is non-null");
            Class<? extends Item> expected = EXPECTED.get(id);
            check(h, expected != null, "unexpected item id " + id);
            eq(h, expected, item.getClass(), id + " class");
            eq(h, item, ForgeRegistries.ITEMS.getValue(new ResourceLocation(JTTWEngine.MOD_ID, id)),
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
        eq(h, 16, ModItems.IMMORTAL_PEACH.get().getMaxStackSize(), "peach stack size");
        eq(h, 1, ModItems.IMMORTAL_PILL.get().getMaxStackSize(), "pill stack size");
        for (RegistryObject<Item> artifact : List.of(ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR,
                ModItems.ERLANG_AXE, ModItems.FIRE_TIP_SPEAR, ModItems.QIANKUN_RING, ModItems.YIN_YANG_SWORD,
                ModItems.WIND_FIRE_WHEELS, ModItems.HUNTIAN_CLOTH, ModItems.BELL_OF_TAIYI,
                ModItems.NINE_TOOTH_RAKE, ModItems.YIN_YANG_VASE, ModItems.MIXED_IRON_CUDGEL, ModItems.BANANA_LEAF_FAN)) {
            eq(h, 1, artifact.get().getMaxStackSize(), artifact.getId().getPath() + " stack size");
        }
        for (RegistryObject<Item> epic : List.of(ModItems.IMMORTAL_PEACH, ModItems.IMMORTAL_PILL,
                ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR, ModItems.ERLANG_AXE,
                ModItems.FIRE_TIP_SPEAR, ModItems.QIANKUN_RING, ModItems.YIN_YANG_SWORD,
                ModItems.WIND_FIRE_WHEELS, ModItems.HUNTIAN_CLOTH, ModItems.BELL_OF_TAIYI,
                ModItems.NINE_TOOTH_RAKE, ModItems.YIN_YANG_VASE, ModItems.MIXED_IRON_CUDGEL, ModItems.BANANA_LEAF_FAN)) {
            eq(h, Rarity.EPIC, new ItemStack(epic.get()).getRarity(), epic.getId().getPath() + " rarity");
        }
        for (RegistryObject<Item> fireproof : List.of(ModItems.RUYI_JINGU_BANG, ModItems.THREE_POINTED_SPEAR,
                ModItems.ERLANG_AXE, ModItems.FIRE_TIP_SPEAR, ModItems.WIND_FIRE_WHEELS,
                ModItems.NINE_TOOTH_RAKE, ModItems.MIXED_IRON_CUDGEL)) {
            check(h, fireproof.get().isFireResistant(), fireproof.getId().getPath() + " survives lava");
        }
        for (RegistryObject<Item> burnable : List.of(ModItems.QIANKUN_RING, ModItems.YIN_YANG_SWORD,
                ModItems.HUNTIAN_CLOTH, ModItems.IMMORTAL_PEACH)) {
            check(h, !burnable.get().isFireResistant(), burnable.getId().getPath() + " is not fire resistant");
        }
        check(h, ModItems.IMMORTAL_PEACH.get().isEdible(), "peach is edible");
        check(h, ModItems.IMMORTAL_PILL.get().isEdible(), "pill is edible");
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
        for (RegistryObject<Item> entry : ModItems.ITEMS.getEntries()) {
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
