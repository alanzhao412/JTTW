package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.heaven.CloudCavalryEntity;
import com.jttw.engine.entity.heaven.ErlangShenEntity;
import com.jttw.engine.entity.heaven.GreatSpiritGiantEntity;
import com.jttw.engine.entity.heaven.HeavenBoss;
import com.jttw.engine.entity.heaven.HeavenlyArcherEntity;
import com.jttw.engine.entity.heaven.HeavenlyKingEntity;
import com.jttw.engine.entity.heaven.LiJingEntity;
import com.jttw.engine.entity.heaven.LightningMotherEntity;
import com.jttw.engine.entity.heaven.LunarMansionEntity;
import com.jttw.engine.entity.heaven.NezhaEntity;
import com.jttw.engine.entity.heaven.StarLordEntity;
import com.jttw.engine.entity.heaven.ThousandMileEyeEntity;
import com.jttw.engine.entity.heaven.ThunderDrummerEntity;
import com.jttw.engine.entity.heaven.ThunderLordEntity;
import com.jttw.engine.entity.heaven.WindFollowingEarEntity;
import com.jttw.engine.world.Zones;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

/**
 * What Heaven's host registers, kept to itself (one line in {@code JTTWEngine} wires it): its creatures
 * (the soldiers' kinds, the Twenty-Eight Lunar Mansions, the Nine Luminaries, the gods of the Thunder
 * Court, the Great Spirit Giant, the watchers of the Southern Gate, the Four Heavenly Kings, Li Jing, Nezha
 * and Erlang Shen; the halberdier, {@code heavenly_soldier}, is ModEntities' as it always was), their
 * attributes, and its items (the pass up to Heaven, the star shards, spawn eggs).
 */
public final class HeavenRegistry {

    private HeavenRegistry() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(JTTWEngine.MOD_ID);

    private static String key(String name) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, name).toString();
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> creature(String name, EntityType.EntityFactory<T> make,
                                                                                         float w, float h, int range) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(make, MobCategory.MONSTER).sized(w, h).clientTrackingRange(range).build(key(name)));
    }

    // ── The host ────────────────────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<HeavenlyArcherEntity>> HEAVENLY_ARCHER =
            creature("heavenly_archer", HeavenlyArcherEntity::new, 0.6f, 1.95f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<CloudCavalryEntity>> CLOUD_CAVALRY =
            creature("cloud_cavalry", CloudCavalryEntity::new, 1.3f, 2.6f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<ThunderDrummerEntity>> THUNDER_DRUMMER =
            creature("thunder_drummer", ThunderDrummerEntity::new, 0.7f, 2.0f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<LunarMansionEntity>> LUNAR_MANSION =
            creature("lunar_mansion", LunarMansionEntity::new, 0.7f, 2.1f, 10);

    // ── Its gods and star lords (minibosses) ────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<StarLordEntity>> STAR_LORD =
            creature("star_lord", StarLordEntity::new, 0.7f, 2.2f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<GreatSpiritGiantEntity>> GREAT_SPIRIT_GIANT =
            creature("great_spirit_giant", GreatSpiritGiantEntity::new, 1.6f, 4.2f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<ThunderLordEntity>> THUNDER_LORD =
            creature("thunder_lord", ThunderLordEntity::new, 0.9f, 2.3f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<LightningMotherEntity>> LIGHTNING_MOTHER =
            creature("lightning_mother", LightningMotherEntity::new, 0.7f, 2.1f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<ThousandMileEyeEntity>> THOUSAND_MILE_EYE =
            creature("thousand_mile_eye", ThousandMileEyeEntity::new, 0.7f, 2.1f, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<WindFollowingEarEntity>> WIND_FOLLOWING_EAR =
            creature("wind_following_ear", WindFollowingEarEntity::new, 0.7f, 2.1f, 10);

    // ── Its great names (bosses) ────────────────────────────────────────

    public static final DeferredHolder<EntityType<?>, EntityType<HeavenlyKingEntity.Chiguo>> KING_CHIGUO =
            creature("king_chiguo", HeavenlyKingEntity.Chiguo::new, 1.0f, 3.0f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<HeavenlyKingEntity.Zengzhang>> KING_ZENGZHANG =
            creature("king_zengzhang", HeavenlyKingEntity.Zengzhang::new, 1.0f, 3.0f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<HeavenlyKingEntity.Guangmu>> KING_GUANGMU =
            creature("king_guangmu", HeavenlyKingEntity.Guangmu::new, 1.0f, 3.0f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<HeavenlyKingEntity.Duowen>> KING_DUOWEN =
            creature("king_duowen", HeavenlyKingEntity.Duowen::new, 1.0f, 3.0f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<LiJingEntity>> LI_JING =
            creature("li_jing", LiJingEntity::new, 0.9f, 2.6f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<NezhaEntity>> NEZHA =
            creature("nezha", NezhaEntity::new, 0.6f, 1.8f, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<ErlangShenEntity>> ERLANG_SHEN =
            creature("erlang_shen", ErlangShenEntity::new, 0.7f, 2.2f, 12);

    /** The Four Heavenly Kings, East, South, West, North. */
    public static List<EntityType<? extends HeavenlyKingEntity>> kings() {
        return List.of(KING_CHIGUO.get(), KING_ZENGZHANG.get(), KING_GUANGMU.get(), KING_DUOWEN.get());
    }

    // ── Items ───────────────────────────────────────────────────────────

    /** A pass through Heaven's gates: up to the clouds of one's own level, and home again. */
    public static final DeferredHolder<Item, Item> HEAVEN_PASS = ITEMS.register("heaven_pass", com.jttw.engine.item.HeavenPassItem::new);
    /**
     * A shard of starlight, from Heaven's star generals and star lords: a forging material for any
     * class, which the Eight Trigrams Furnace makes into Starfall Talismans and Star-Essence Elixirs.
     */
    public static final DeferredHolder<Item, Item> STAR_SHARD = ITEMS.register("star_shard", () -> new Item(new Item.Properties()) {
        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context,
                                    List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
            tooltip.add(net.minecraft.network.chat.Component.translatable("item.jttw_engine.star_shard.desc")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    });

    private static DeferredHolder<Item, Item> egg(String name, Supplier<? extends EntityType<? extends net.minecraft.world.entity.Mob>> type, int a, int b) {
        return ITEMS.register(name + "_spawn_egg", () -> new DeferredSpawnEggItem(type, a, b, new Item.Properties()));
    }

    public static final DeferredHolder<Item, Item> HEAVENLY_ARCHER_EGG = egg("heavenly_archer", HEAVENLY_ARCHER, 0xE8C860, 0x3A6AB0);
    public static final DeferredHolder<Item, Item> CLOUD_CAVALRY_EGG = egg("cloud_cavalry", CLOUD_CAVALRY, 0xF0F0F8, 0xD8A838);
    public static final DeferredHolder<Item, Item> THUNDER_DRUMMER_EGG = egg("thunder_drummer", THUNDER_DRUMMER, 0xB03028, 0xE8C860);
    public static final DeferredHolder<Item, Item> LUNAR_MANSION_EGG = egg("lunar_mansion", LUNAR_MANSION, 0x2E6A4A, 0xE8D890);
    public static final DeferredHolder<Item, Item> STAR_LORD_EGG = egg("star_lord", STAR_LORD, 0x24305A, 0xF0E070);
    public static final DeferredHolder<Item, Item> GREAT_SPIRIT_GIANT_EGG = egg("great_spirit_giant", GREAT_SPIRIT_GIANT, 0x8A5A30, 0xD8B040);
    public static final DeferredHolder<Item, Item> THUNDER_LORD_EGG = egg("thunder_lord", THUNDER_LORD, 0x3A5AA0, 0xE8E060);
    public static final DeferredHolder<Item, Item> LIGHTNING_MOTHER_EGG = egg("lightning_mother", LIGHTNING_MOTHER, 0xE890B0, 0xD8E8FF);
    public static final DeferredHolder<Item, Item> THOUSAND_MILE_EYE_EGG = egg("thousand_mile_eye", THOUSAND_MILE_EYE, 0x5A3A8A, 0xF0C040);
    public static final DeferredHolder<Item, Item> WIND_FOLLOWING_EAR_EGG = egg("wind_following_ear", WIND_FOLLOWING_EAR, 0x2A7A5A, 0xF0C040);
    public static final DeferredHolder<Item, Item> KING_CHIGUO_EGG = egg("king_chiguo", KING_CHIGUO, 0xE8E8F0, 0x8A6A30);
    public static final DeferredHolder<Item, Item> KING_ZENGZHANG_EGG = egg("king_zengzhang", KING_ZENGZHANG, 0x3A5AB0, 0xC8C8D8);
    public static final DeferredHolder<Item, Item> KING_GUANGMU_EGG = egg("king_guangmu", KING_GUANGMU, 0xB03028, 0x3A9A4A);
    public static final DeferredHolder<Item, Item> KING_DUOWEN_EGG = egg("king_duowen", KING_DUOWEN, 0x2A6A4A, 0xE8C040);
    public static final DeferredHolder<Item, Item> LI_JING_EGG = egg("li_jing", LI_JING, 0xD8A830, 0x8A2A20);
    public static final DeferredHolder<Item, Item> NEZHA_EGG = egg("nezha", NEZHA, 0xD02818, 0xF0D060);
    public static final DeferredHolder<Item, Item> ERLANG_SHEN_EGG = egg("erlang_shen", ERLANG_SHEN, 0xC8CCD8, 0x2A2A3A);

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(HeavenRegistry::onAttributes);
    }

    private static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(HEAVENLY_ARCHER.get(), HeavenlyArcherEntity.createAttributes().build());
        event.put(CLOUD_CAVALRY.get(), CloudCavalryEntity.createAttributes().build());
        event.put(THUNDER_DRUMMER.get(), ThunderDrummerEntity.createAttributes().build());
        event.put(LUNAR_MANSION.get(), LunarMansionEntity.createAttributes().build());
        event.put(STAR_LORD.get(), StarLordEntity.createAttributes().build());
        event.put(GREAT_SPIRIT_GIANT.get(), GreatSpiritGiantEntity.createAttributes().build());
        event.put(THUNDER_LORD.get(), ThunderLordEntity.createAttributes().build());
        event.put(LIGHTNING_MOTHER.get(), LightningMotherEntity.createAttributes().build());
        event.put(THOUSAND_MILE_EYE.get(), ThousandMileEyeEntity.createAttributes().build());
        event.put(WIND_FOLLOWING_EAR.get(), WindFollowingEarEntity.createAttributes().build());
        event.put(KING_CHIGUO.get(), HeavenBoss.attributesFor(Zones.HEAVEN_OUTER_CLOUDS.maxLevel(), 0.26).build());
        event.put(KING_ZENGZHANG.get(), HeavenBoss.attributesFor(Zones.HEAVEN_CLOUD_ROAD.maxLevel(), 0.28).build());
        event.put(KING_GUANGMU.get(), HeavenBoss.attributesFor(Zones.HEAVEN_STAR_PALACES.maxLevel(), 0.26).build());
        event.put(KING_DUOWEN.get(), HeavenBoss.attributesFor(Zones.HEAVEN_THUNDER_COURT.maxLevel(), 0.26).build());
        event.put(LI_JING.get(), LiJingEntity.createAttributes().build());
        event.put(NEZHA.get(), NezhaEntity.createAttributes().build());
        event.put(ERLANG_SHEN.get(), ErlangShenEntity.createAttributes().build());
    }
}
