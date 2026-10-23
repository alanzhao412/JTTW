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
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The mod's items: consumables, block items and the characters' artifacts. */
public final class ModItems {

    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, JTTWEngine.MOD_ID);

    // ── Consumables and blocks ────────────────────────────────────
    public static final DeferredHolder<Item, Item> IMMORTAL_PEACH =
            ITEMS.register("immortal_peach", ImmortalPeachItem::new);

    public static final DeferredHolder<Item, Item> CELESTIAL_CLOUD_ITEM =
            ITEMS.register("celestial_cloud", () -> new BlockItem(
                    ModBlocks.CELESTIAL_CLOUD.get(), new Item.Properties()));

    // ── Forging: the furnace, and the zones' materials ─────────────
    public static final DeferredHolder<Item, Item> EIGHT_TRIGRAMS_FURNACE_ITEM =
            ITEMS.register("eight_trigrams_furnace", () -> new com.jttw.engine.world.furnace.TrigramsFurnaceItem(ModBlocks.EIGHT_TRIGRAMS_FURNACE.get(), new Item.Properties()));
    /** The furnace's only fuel: forged from the zones' materials (Nezha and the Phoenix need none: their fire lights it). */
    public static final DeferredHolder<Item, Item> TRUE_SAMADHI_FIRE = ITEMS.register("true_samadhi_fire",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** Erlang Shen's Golden Bow (see GoldenBowItem). */
    public static final DeferredHolder<Item, Item> GOLDEN_BOW = ITEMS.register("golden_bow", com.jttw.engine.item.GoldenBowItem::new);
    /** The ji of Heaven's soldiers; now and then one falls from a soldier struck down. */
    public static final DeferredHolder<Item, Item> HEAVENLY_HALBERD = ITEMS.register("heavenly_halberd",
            () -> new net.minecraft.world.item.SwordItem(net.minecraft.world.item.Tiers.GOLD,
                    new Item.Properties().durability(600).rarity(net.minecraft.world.item.Rarity.UNCOMMON)
                            .attributes(net.minecraft.world.item.SwordItem.createAttributes(net.minecraft.world.item.Tiers.GOLD, 4, -2.8f))));
    /** From Bone Servants. */
    public static final DeferredHolder<Item, Item> SERVANT_BONE = ITEMS.register("servant_bone", () -> new Item(new Item.Properties()));
    /** From Wraiths. */
    public static final DeferredHolder<Item, Item> WRAITH_WISP = ITEMS.register("wraith_wisp", () -> new Item(new Item.Properties()));
    /** From any captain. */
    public static final DeferredHolder<Item, Item> CAPTAINS_SIGIL = ITEMS.register("captains_sigil",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    /** From Lady White Bone. */
    public static final DeferredHolder<Item, Item> HEART_BONE = ITEMS.register("heart_bone",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));

    // ── The Bottomless Cave ───────────────────────────────────────
    /** From Mouse Spirits. */
    public static final DeferredHolder<Item, Item> SPIRIT_WHISKER = ITEMS.register("spirit_whisker", () -> new Item(new Item.Properties()));
    /** From Flower Maids. */
    public static final DeferredHolder<Item, Item> NIGHT_PETAL = ITEMS.register("night_petal", () -> new Item(new Item.Properties()));
    /** From Lady Earth Flow: one of the candles she stole from the Buddha's altar on Spirit Mountain. */
    public static final DeferredHolder<Item, Item> SPIRIT_MOUNTAIN_CANDLE = ITEMS.register("spirit_mountain_candle",
            () -> new Item(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    /** What her likeness falls back into: her embroidered shoe. */
    public static final DeferredHolder<Item, Item> EMBROIDERED_SHOE = ITEMS.register("embroidered_shoe",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    /** Her twin swords, the male blade and the female (a pair: see EarthFlowSwordItem). */
    public static final DeferredHolder<Item, Item> EARTH_FLOW_SWORD_MALE = ITEMS.register("earth_flow_sword_male",
            () -> new com.jttw.engine.item.EarthFlowSwordItem(true));
    public static final DeferredHolder<Item, Item> EARTH_FLOW_SWORD_FEMALE = ITEMS.register("earth_flow_sword_female",
            () -> new com.jttw.engine.item.EarthFlowSwordItem(false));
    public static final DeferredHolder<Item, Item> MOUSE_SPIRIT_SPAWN_EGG = ITEMS.register("mouse_spirit_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.MOUSE_SPIRIT, 0xB8B0A4, 0xE8A0A8, new Item.Properties()));
    public static final DeferredHolder<Item, Item> FLOWER_MAID_SPAWN_EGG = ITEMS.register("flower_maid_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.FLOWER_MAID, 0xF2B8CC, 0x6FA05A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> LADY_EARTH_FLOW_SPAWN_EGG = ITEMS.register("lady_earth_flow_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.LADY_EARTH_FLOW, 0xF4F1EC, 0xD9A93A, new Item.Properties()));

    // ── Hell: the Ten Courts ──────────────────────────────────────
    /** From wandering ghosts: the paper money burned for the dead. */
    public static final DeferredHolder<Item, Item> HELL_BANK_NOTE = ITEMS.register("hell_bank_note", () -> new Item(new Item.Properties()));
    /** From Ox-Head. */
    public static final DeferredHolder<Item, Item> OX_HORN = ITEMS.register("ox_horn", () -> new Item(new Item.Properties()));
    /** From Horse-Face. */
    public static final DeferredHolder<Item, Item> HORSE_MANE = ITEMS.register("horse_mane", () -> new Item(new Item.Properties()));
    /** From the Judge-Clerks. */
    public static final DeferredHolder<Item, Item> UNDERWORLD_INK = ITEMS.register("underworld_ink", () -> new Item(new Item.Properties()));
    /** From the Black and White Impermanence. */
    public static final DeferredHolder<Item, Item> SOUL_CHAIN = ITEMS.register("soul_chain", () -> new Item(new Item.Properties()));
    /** From King Qinguang (First Court). */
    public static final DeferredHolder<Item, Item> KARMA_MIRROR_SHARD = ITEMS.register("karma_mirror_shard",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Chujiang (Second Court). */
    public static final DeferredHolder<Item, Item> RIME_SHACKLE = ITEMS.register("rime_shackle",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Songdi (Third Court). */
    public static final DeferredHolder<Item, Item> IRON_SOUL_HOOK = ITEMS.register("iron_soul_hook",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Wuguan (Fourth Court). */
    public static final DeferredHolder<Item, Item> SCALES_WEIGHT = ITEMS.register("scales_weight",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Yama (Fifth Court): his seal. */
    public static final DeferredHolder<Item, Item> YAMA_SEAL = ITEMS.register("yama_seal",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    /** From King Yama, and now and then another king: a page torn from the Book of Life and Death. */
    public static final DeferredHolder<Item, Item> BOOK_PAGE = ITEMS.register("book_page",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant()));
    /** From King Biancheng (Sixth Court). */
    public static final DeferredHolder<Item, Item> WAILING_BELL = ITEMS.register("wailing_bell",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Taishan (Seventh Court). */
    public static final DeferredHolder<Item, Item> OIL_CAULDRON_EMBER = ITEMS.register("oil_cauldron_ember",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Dushi (Eighth Court). */
    public static final DeferredHolder<Item, Item> CART_WHEEL_SPOKE = ITEMS.register("cart_wheel_spoke",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Pingdeng (Ninth Court). */
    public static final DeferredHolder<Item, Item> EQUAL_MEASURE_ROD = ITEMS.register("equal_measure_rod",
            () -> new com.jttw.engine.item.HellRelicItem(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE).fireResistant()));
    /** From King Yama: his Vermilion Brush of Judgement (see JudgeBrushItem). */
    public static final DeferredHolder<Item, Item> YAMA_BRUSH = ITEMS.register("yama_brush", com.jttw.engine.item.JudgeBrushItem::new);
    /** From King Zhuanlun (Tenth Court): Meng Po's Soup of Forgetting (see MengPoSoupItem). */
    public static final DeferredHolder<Item, Item> MENG_PO_SOUP = ITEMS.register("meng_po_soup",
            () -> new com.jttw.engine.item.MengPoSoupItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredHolder<Item, Item> OX_HEAD_SPAWN_EGG = ITEMS.register("ox_head_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.OX_HEAD, 0x2A2A30, 0x8B5A2B, new Item.Properties()));
    public static final DeferredHolder<Item, Item> HORSE_FACE_SPAWN_EGG = ITEMS.register("horse_face_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.HORSE_FACE, 0x2A2A30, 0x6B4A2A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> JUDGE_CLERK_SPAWN_EGG = ITEMS.register("judge_clerk_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.JUDGE_CLERK, 0x8E1B1B, 0x111111, new Item.Properties()));
    public static final DeferredHolder<Item, Item> BLACK_IMPERMANENCE_SPAWN_EGG = ITEMS.register("black_impermanence_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.BLACK_IMPERMANENCE, 0x111114, 0xEDEDED, new Item.Properties()));
    public static final DeferredHolder<Item, Item> WHITE_IMPERMANENCE_SPAWN_EGG = ITEMS.register("white_impermanence_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.WHITE_IMPERMANENCE, 0xEDEDED, 0xB01818, new Item.Properties()));
    public static final DeferredHolder<Item, Item> WANDERING_GHOST_SPAWN_EGG = ITEMS.register("wandering_ghost_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.WANDERING_GHOST, 0xC8D4D8, 0x6F8A94, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_QINGUANG_SPAWN_EGG = ITEMS.register("king_qinguang_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_QINGUANG, 0x1E3A6E, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_CHUJIANG_SPAWN_EGG = ITEMS.register("king_chujiang_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_CHUJIANG, 0x9CC8E8, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_SONGDI_SPAWN_EGG = ITEMS.register("king_songdi_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_SONGDI, 0x4A4A52, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_WUGUAN_SPAWN_EGG = ITEMS.register("king_wuguan_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_WUGUAN, 0x2E6B3A, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_YAMA_SPAWN_EGG = ITEMS.register("king_yama_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_YAMA, 0x6E1010, 0xF0C040, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_BIANCHENG_SPAWN_EGG = ITEMS.register("king_biancheng_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_BIANCHENG, 0x5A2A6E, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_TAISHAN_SPAWN_EGG = ITEMS.register("king_taishan_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_TAISHAN, 0xB8501A, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_DUSHI_SPAWN_EGG = ITEMS.register("king_dushi_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_DUSHI, 0x6B5A3A, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_PINGDENG_SPAWN_EGG = ITEMS.register("king_pingdeng_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_PINGDENG, 0xE0E0D0, 0xD4AF37, new Item.Properties()));
    public static final DeferredHolder<Item, Item> KING_ZHUANLUN_SPAWN_EGG = ITEMS.register("king_zhuanlun_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.KING_ZHUANLUN, 0x1A1A1A, 0xD4AF37, new Item.Properties()));

    // ── Heaven and Hell ───────────────────────────────────────────
    public static final DeferredHolder<Item, Item> JADE_BLOCK_ITEM =
            ITEMS.register("jade_block", () -> new BlockItem(ModBlocks.JADE_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> HEAVENLY_WINE = ITEMS.register("heavenly_wine",
            () -> new com.jttw.engine.world.heaven.HeavenlyWineItem(new Item.Properties().stacksTo(16).rarity(net.minecraft.world.item.Rarity.RARE)));

    // ── White Bone Ridge spawn eggs ───────────────────────────────
    public static final DeferredHolder<Item, Item> BONE_SERVANT_SPAWN_EGG = ITEMS.register("bone_servant_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.BONE_SERVANT,0xE8E4D8, 0x6B6458, new Item.Properties()));
    public static final DeferredHolder<Item, Item> WRAITH_SPAWN_EGG = ITEMS.register("wraith_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.WRAITH,0xF4F6FA, 0x9AA3B5, new Item.Properties()));
    public static final DeferredHolder<Item, Item> LADY_WHITE_BONE_SPAWN_EGG = ITEMS.register("lady_white_bone_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(com.jttw.engine.registry.ModEntities.LADY_WHITE_BONE,0xF2EFE6, 0x5A2D82, new Item.Properties()));

    // ── The Mystical Waters ───────────────────────────────────────
    /** The Water-Repelling Pearl: breath under the sea. */
    public static final DeferredHolder<Item, Item> WATER_REPELLING_PEARL = ITEMS.register("water_repelling_pearl",
            com.jttw.engine.item.WaterRepellingPearlItem::new);
    /** Ao Run's (West): the Phoenix-Plume Purple-Gold Cap. */
    public static final DeferredHolder<Item, Item> PHOENIX_PLUME_CAP = ITEMS.register("phoenix_plume_cap",
            () -> new com.jttw.engine.item.DragonRegaliaItem(net.minecraft.world.item.ArmorItem.Type.HELMET));
    /** Ao Qin's (South): the Golden Chain Mail. */
    public static final DeferredHolder<Item, Item> GOLDEN_CHAIN_MAIL = ITEMS.register("golden_chain_mail",
            () -> new com.jttw.engine.item.DragonRegaliaItem(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
    /** Ao Shun's (North): the Lotus-Fibre Cloud-Stepping Boots. */
    public static final DeferredHolder<Item, Item> CLOUD_STEPPING_BOOTS = ITEMS.register("cloud_stepping_boots",
            () -> new com.jttw.engine.item.DragonRegaliaItem(net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final DeferredHolder<Item, Item> SHRIMP_SOLDIER_SPAWN_EGG = ITEMS.register("shrimp_soldier_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.SHRIMP_SOLDIER, 0xE0603A, 0xF2C8A0, new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRAB_GENERAL_SPAWN_EGG = ITEMS.register("crab_general_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.CRAB_GENERAL, 0xA8322A, 0xD9A441, new Item.Properties()));
    public static final DeferredHolder<Item, Item> YAKSHA_SPAWN_EGG = ITEMS.register("yaksha_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.YAKSHA, 0x3A6FB0, 0xC8302A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> AO_GUANG_SPAWN_EGG = ITEMS.register("ao_guang_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.AO_GUANG, 0x2E9C8C, 0xE0B040, new Item.Properties()));
    public static final DeferredHolder<Item, Item> AO_QIN_SPAWN_EGG = ITEMS.register("ao_qin_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.AO_QIN, 0xB3262A, 0xE0B040, new Item.Properties()));
    public static final DeferredHolder<Item, Item> AO_RUN_SPAWN_EGG = ITEMS.register("ao_run_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.AO_RUN, 0xE8E6DE, 0xE0B040, new Item.Properties()));
    public static final DeferredHolder<Item, Item> AO_SHUN_SPAWN_EGG = ITEMS.register("ao_shun_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(ModEntities.AO_SHUN, 0x1C1C24, 0xE0B040, new Item.Properties()));

    public static final DeferredHolder<Item, Item> IMMORTAL_PILL =
            ITEMS.register("jttw_immortal_pill", ImmortalItems.ImmortalPillItem::new);

    // True Samadhi Fire is an entity, not an item.
    // Call ImmortalItems.onSamadhiFireHit(player) when the fire entity hits a Wukong player.

    // ── Wukong ────────────────────────────────────────────────────
    public static final DeferredHolder<Item, Item> RUYI_JINGU_BANG =
            ITEMS.register("ruyi_jingu_bang", RuyiJinguBangItem::new);

    // ── Erlang Shen ───────────────────────────────────────────────
    public static final DeferredHolder<Item, Item> THREE_POINTED_SPEAR =
            ITEMS.register("three_pointed_spear", ThreePointedSpearItem::new);

    public static final DeferredHolder<Item, Item> ERLANG_AXE =
            ITEMS.register("erlang_axe", ErlangAxeItem::new);

    // ── Nezha ─────────────────────────────────────────────────────
    public static final DeferredHolder<Item, Item> FIRE_TIP_SPEAR =
            ITEMS.register("fire_tip_spear", FireTipSpearItem::new);
    public static final DeferredHolder<Item, Item> QIANKUN_RING =
            ITEMS.register("qiankun_ring", QiankunRingItem::new);
    public static final DeferredHolder<Item, Item> YIN_YANG_SWORD =
            ITEMS.register("yin_yang_sword", YinYangSwordItem::new);
    public static final DeferredHolder<Item, Item> WIND_FIRE_WHEELS =
            ITEMS.register("wind_fire_wheels", WindFireWheelsItem::new);
    public static final DeferredHolder<Item, Item> HUNTIAN_CLOTH =
            ITEMS.register("huntian_cloth", HuntianClothItem::new);

    // ── Grand Saint of Nine Spirits ───────────────────────────────
    public static final DeferredHolder<Item, Item> BELL_OF_TAIYI =
            ITEMS.register("bell_of_taiyi", BellOfTaiyiItem::new);

    public static final DeferredHolder<Item, Item> NINE_TOOTH_RAKE =
            ITEMS.register("nine_tooth_rake", com.jttw.engine.item.NineToothRakeItem::new);

    public static final DeferredHolder<Item, Item> YIN_YANG_VASE =
            ITEMS.register("yin_yang_vase", com.jttw.engine.item.YinYangVaseItem::new);

    public static final DeferredHolder<Item, Item> MIXED_IRON_CUDGEL =
            ITEMS.register("mixed_iron_cudgel", com.jttw.engine.item.MixedIronCudgelItem::new);

    public static final DeferredHolder<Item, Item> BANANA_LEAF_FAN =
            ITEMS.register("banana_leaf_fan", com.jttw.engine.item.BananaLeafFanItem::new);


    // ── Tough stone ──
    public static final DeferredHolder<Item, Item> BLEACHED_STONE_ITEM =
            ITEMS.register("bleached_stone", () -> new BlockItem(ModBlocks.BLEACHED_STONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> GRAVE_TUFF_ITEM =
            ITEMS.register("grave_tuff", () -> new BlockItem(ModBlocks.GRAVE_TUFF.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> BONE_ANDESITE_ITEM =
            ITEMS.register("bone_andesite", () -> new BlockItem(ModBlocks.BONE_ANDESITE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> OSSIFIED_BONE_ITEM =
            ITEMS.register("ossified_bone", () -> new BlockItem(ModBlocks.OSSIFIED_BONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> BONE_GRAVEL_ITEM =
            ITEMS.register("bone_gravel", () -> new BlockItem(ModBlocks.BONE_GRAVEL.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CELESTIAL_MARBLE_ITEM =
            ITEMS.register("celestial_marble", () -> new BlockItem(ModBlocks.CELESTIAL_MARBLE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> UNDERWORLD_STONE_ITEM =
            ITEMS.register("underworld_stone", () -> new BlockItem(ModBlocks.UNDERWORLD_STONE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> SOUL_ASH_ITEM =
            ITEMS.register("soul_ash", () -> new BlockItem(ModBlocks.SOUL_ASH.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
