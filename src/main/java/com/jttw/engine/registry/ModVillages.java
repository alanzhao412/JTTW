package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.village.TangFolkEntity;
import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.village.TangVillageStructure;
import com.jttw.engine.village.VillageItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Tang villages' registrations, kept apart from the rest so they merge cleanly: their folk (the
 * mortal villagers, and the merchants of the ghost market and of Heaven), the currency and goods they
 * trade, the talismans and charms, their effects, and the village structure.
 * See {@code com.jttw.engine.village}.
 */
public final class ModVillages {

    private ModVillages() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, JTTWEngine.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, JTTWEngine.MOD_ID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, JTTWEngine.MOD_ID);

    // ── Currency ─────────────────────────────────────────────────────
    /** Copper cash (铜钱), the round coin with a square hole: the unit, one wen. */
    public static final DeferredHolder<Item, Item> COPPER_CASH = ITEMS.register("copper_cash", () -> new VillageItems.Coin(1));
    /** A string of cash (一串钱): copper cash threaded on a cord, eight to the string. */
    public static final DeferredHolder<Item, Item> STRING_OF_CASH = ITEMS.register("string_of_cash", () -> new VillageItems.Coin(8));
    /** A tael of silver (银两), a boat-shaped ingot (元宝): sixty-four wen. */
    public static final DeferredHolder<Item, Item> SILVER_TAEL = ITEMS.register("silver_tael", () -> new VillageItems.Coin(64));

    // ── Food and drink ───────────────────────────────────────────────
    public static final DeferredHolder<Item, Item> RICE = ITEMS.register("rice", () -> new VillageItems.Described(new Item.Properties()));
    public static final DeferredHolder<Item, Item> RICE_BOWL = ITEMS.register("rice_bowl", VillageItems::riceBowl);
    public static final DeferredHolder<Item, Item> STEAMED_BUN = ITEMS.register("steamed_bun", VillageItems::steamedBun);
    public static final DeferredHolder<Item, Item> MANTOU = ITEMS.register("mantou", VillageItems::mantou);
    public static final DeferredHolder<Item, Item> RICE_WINE = ITEMS.register("rice_wine", VillageItems::riceWine);
    public static final DeferredHolder<Item, Item> TEA = ITEMS.register("tea", VillageItems::tea);

    // ── Medicine ─────────────────────────────────────────────────────
    public static final DeferredHolder<Item, Item> HERBAL_DECOCTION = ITEMS.register("herbal_decoction", VillageItems::herbalDecoction);
    public static final DeferredHolder<Item, Item> QI_GATHERING_PILL = ITEMS.register("qi_gathering_pill", () -> new VillageItems.QiTonic(VillageItems.PILL_SHARE, false));
    public static final DeferredHolder<Item, Item> JADE_DEW = ITEMS.register("jade_dew", () -> new VillageItems.QiTonic(VillageItems.DEW_SHARE, true));

    // ── Rumours ──────────────────────────────────────────────────────
    public static final DeferredHolder<Item, Item> RUMOUR_SCROLL = ITEMS.register("rumour_scroll", VillageItems.RumourScroll::new);

    // ── Talismans and charms (sold only in the supernatural world) ───
    public static final DeferredHolder<MobEffect, MobEffect> DEMON_QUELLING = EFFECTS.register("demon_quelling",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD9A23A) {});
    public static final DeferredHolder<MobEffect, MobEffect> GHOST_WARD = EFFECTS.register("ghost_ward",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xC8402E) {});
    public static final DeferredHolder<MobEffect, MobEffect> INCENSE_CALM = EFFECTS.register("incense_calm",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x9C8A6A) {});

    public static final DeferredHolder<Item, Item> DEMON_QUELLING_TALISMAN = ITEMS.register("demon_quelling_talisman",
            () -> new VillageItems.Talisman(DEMON_QUELLING, VillageItems.TALISMAN_TICKS));
    public static final DeferredHolder<Item, Item> GHOST_WARDING_TALISMAN = ITEMS.register("ghost_warding_talisman",
            () -> new VillageItems.Talisman(GHOST_WARD, VillageItems.TALISMAN_TICKS));
    public static final DeferredHolder<Item, Item> HOUSE_WARD_TALISMAN = ITEMS.register("house_ward_talisman", VillageItems.HouseWard::new);
    public static final DeferredHolder<Item, Item> INCENSE = ITEMS.register("incense",
            () -> new VillageItems.Talisman(INCENSE_CALM, VillageItems.INCENSE_TICKS));
    public static final DeferredHolder<Item, Item> PEACE_CHARM = ITEMS.register("peace_charm", VillageItems.PeaceCharm::new);

    // ── The folk ─────────────────────────────────────────────────────
    /** A mortal of a Tang village: a farmer, monk, scholar, merchant, innkeeper, herbalist, fortune teller or smith. */
    public static final DeferredHolder<EntityType<?>, EntityType<TangFolkEntity>> TANG_VILLAGER = ENTITIES.register("tang_villager", () ->
            EntityType.Builder.<TangFolkEntity>of(TangFolkEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(JTTWEngine.id("tang_villager").toString()));
    /** A merchant of the supernatural world: the ghost market of Fengdu, or Heaven. */
    public static final DeferredHolder<EntityType<?>, EntityType<TangFolkEntity>> SPIRIT_MERCHANT = ENTITIES.register("spirit_merchant", () ->
            EntityType.Builder.<TangFolkEntity>of(TangFolkEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(JTTWEngine.id("spirit_merchant").toString()));

    public static final DeferredHolder<Item, Item> TANG_VILLAGER_SPAWN_EGG = ITEMS.register("tang_villager_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(TANG_VILLAGER, 0x3E5A7A, 0xB8402E, new Item.Properties()));
    public static final DeferredHolder<Item, Item> SPIRIT_MERCHANT_SPAWN_EGG = ITEMS.register("spirit_merchant_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(SPIRIT_MERCHANT, 0x9AB8B0, 0x2A2A3A, new Item.Properties()));

    // ── The villages ─────────────────────────────────────────────────
    public static final DeferredHolder<StructureType<?>, StructureType<TangVillageStructure>> TANG_VILLAGE = STRUCTURE_TYPES.register("tang_village",
            () -> () -> TangVillageStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TANG_VILLAGE_PIECE = PIECES.register("tang_village",
            () -> (StructurePieceType.ContextlessType) TangVillagePiece::new);

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        ENTITIES.register(modEventBus);
        EFFECTS.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        PIECES.register(modEventBus);
        modEventBus.addListener(ModVillages::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(TANG_VILLAGER.get(), TangFolkEntity.createAttributes().build());
        event.put(SPIRIT_MERCHANT.get(), TangFolkEntity.createAttributes().build());
    }
}
