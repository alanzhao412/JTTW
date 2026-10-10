package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.item.ErlangAxeItem;
import com.jttw.engine.item.FireTipSpearItem;
import com.jttw.engine.item.BellOfTaiyiItem;
import com.jttw.engine.item.HuntianClothItem;
import com.jttw.engine.item.ImmortalItems;
import com.jttw.engine.item.ImmortalPeachItem;
import com.jttw.engine.item.QiankunRingItem;
import com.jttw.engine.item.RuyiJinguBangItem;
import com.jttw.engine.item.ThreePointedSpearItem;
import com.jttw.engine.item.WindFireWheelsItem;
import com.jttw.engine.item.YinYangSwordItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's items: consumables, block items and the characters' artifacts. */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, JTTWEngine.MOD_ID);

    // ── Consumables and blocks ────────────────────────────────────
    public static final RegistryObject<Item> IMMORTAL_PEACH =
            ITEMS.register("immortal_peach", ImmortalPeachItem::new);

    public static final RegistryObject<Item> CELESTIAL_CLOUD_ITEM =
            ITEMS.register("celestial_cloud", () -> new BlockItem(
                    ModBlocks.CELESTIAL_CLOUD.get(), new Item.Properties()));

    // ── Forging: the furnace, and the zones' materials ─────────────
    public static final RegistryObject<Item> EIGHT_TRIGRAMS_FURNACE_ITEM =
            ITEMS.register("eight_trigrams_furnace", () -> new com.jttw.engine.world.furnace.TrigramsFurnaceItem(ModBlocks.EIGHT_TRIGRAMS_FURNACE.get(), new Item.Properties()));
    /** The furnace's only fuel: forged from the zones' materials (Nezha and the Phoenix need none: their fire lights it). */
    public static final RegistryObject<Item> TRUE_SAMADHI_FIRE = ITEMS.register("true_samadhi_fire",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From Bone Servants. */
    public static final RegistryObject<Item> SERVANT_BONE = ITEMS.register("servant_bone", () -> new Item(new Item.Properties()));
    /** From Wraiths. */
    public static final RegistryObject<Item> WRAITH_WISP = ITEMS.register("wraith_wisp", () -> new Item(new Item.Properties()));
    /** From any captain. */
    public static final RegistryObject<Item> CAPTAINS_SIGIL = ITEMS.register("captains_sigil",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    /** From Lady White Bone. */
    public static final RegistryObject<Item> HEART_BONE = ITEMS.register("heart_bone",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // ── Heaven and Hell ───────────────────────────────────────────
    public static final RegistryObject<Item> JADE_BLOCK_ITEM =
            ITEMS.register("jade_block", () -> new BlockItem(ModBlocks.JADE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> HEAVENLY_WINE = ITEMS.register("heavenly_wine",
            () -> new com.jttw.engine.world.heaven.HeavenlyWineItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE)));

    // ── White Bone Ridge spawn eggs ───────────────────────────────
    public static final RegistryObject<Item> BONE_SERVANT_SPAWN_EGG = ITEMS.register("bone_servant_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(com.jttw.engine.registry.ModEntities.BONE_SERVANT, 0xE8E4D8, 0x6B6458, new Item.Properties()));
    public static final RegistryObject<Item> WRAITH_SPAWN_EGG = ITEMS.register("wraith_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(com.jttw.engine.registry.ModEntities.WRAITH, 0xF4F6FA, 0x9AA3B5, new Item.Properties()));
    public static final RegistryObject<Item> LADY_WHITE_BONE_SPAWN_EGG = ITEMS.register("lady_white_bone_spawn_egg",
            () -> new net.minecraftforge.common.ForgeSpawnEggItem(com.jttw.engine.registry.ModEntities.LADY_WHITE_BONE, 0xF2EFE6, 0x5A2D82, new Item.Properties()));

    public static final RegistryObject<Item> IMMORTAL_PILL =
            ITEMS.register("jttw_immortal_pill", ImmortalItems.ImmortalPillItem::new);

    // True Samadhi Fire is an entity, not an item.
    // Call ImmortalItems.onSamadhiFireHit(player) when the fire entity hits a Wukong player.

    // ── Wukong ────────────────────────────────────────────────────
    public static final RegistryObject<Item> RUYI_JINGU_BANG =
            ITEMS.register("ruyi_jingu_bang", RuyiJinguBangItem::new);

    // ── Erlang Shen ───────────────────────────────────────────────
    public static final RegistryObject<Item> THREE_POINTED_SPEAR =
            ITEMS.register("three_pointed_spear", ThreePointedSpearItem::new);

    public static final RegistryObject<Item> ERLANG_AXE =
            ITEMS.register("erlang_axe", ErlangAxeItem::new);

    // ── Nezha ─────────────────────────────────────────────────────
    public static final RegistryObject<Item> FIRE_TIP_SPEAR =
            ITEMS.register("fire_tip_spear", FireTipSpearItem::new);
    public static final RegistryObject<Item> QIANKUN_RING =
            ITEMS.register("qiankun_ring", QiankunRingItem::new);
    public static final RegistryObject<Item> YIN_YANG_SWORD =
            ITEMS.register("yin_yang_sword", YinYangSwordItem::new);
    public static final RegistryObject<Item> WIND_FIRE_WHEELS =
            ITEMS.register("wind_fire_wheels", WindFireWheelsItem::new);
    public static final RegistryObject<Item> HUNTIAN_CLOTH =
            ITEMS.register("huntian_cloth", HuntianClothItem::new);

    // ── Grand Saint of Nine Spirits ───────────────────────────────
    public static final RegistryObject<Item> BELL_OF_TAIYI =
            ITEMS.register("bell_of_taiyi", BellOfTaiyiItem::new);

    public static final RegistryObject<Item> NINE_TOOTH_RAKE =
            ITEMS.register("nine_tooth_rake", com.jttw.engine.item.NineToothRakeItem::new);

    public static final RegistryObject<Item> YIN_YANG_VASE =
            ITEMS.register("yin_yang_vase", com.jttw.engine.item.YinYangVaseItem::new);

    public static final RegistryObject<Item> MIXED_IRON_CUDGEL =
            ITEMS.register("mixed_iron_cudgel", com.jttw.engine.item.MixedIronCudgelItem::new);

    public static final RegistryObject<Item> BANANA_LEAF_FAN =
            ITEMS.register("banana_leaf_fan", com.jttw.engine.item.BananaLeafFanItem::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
