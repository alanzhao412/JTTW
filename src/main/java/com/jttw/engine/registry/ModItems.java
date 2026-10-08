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

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
