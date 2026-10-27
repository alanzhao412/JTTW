package com.jttw.engine.village;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModVillages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

import java.util.Map;

/**
 * Coin in the world's chests, modestly: copper cash in vanilla's village chests and dungeons, a
 * tael or two of silver in shipwrecks' treasure, and silver in the treasuries of the mod's zones
 * (the Dragon Kings', Lion-Camel City's, Heaven's pavilions and palace). The Tang villages' own
 * chests have tables of their own ({@code chests/tang_village_house}, {@code _smithy}).
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class VillageLoot {

    private VillageLoot() {}

    /** A pool: with this chance, so many coins of one kind. */
    record Coins(boolean silver, int min, int max, float chance) {}

    /** What each table gains. */
    static final Map<String, Coins> ADDED = Map.ofEntries(
            Map.entry("minecraft:chests/village/village_plains_house", new Coins(false, 3, 12, 0.6f)),
            Map.entry("minecraft:chests/village/village_taiga_house", new Coins(false, 3, 12, 0.6f)),
            Map.entry("minecraft:chests/village/village_savanna_house", new Coins(false, 3, 12, 0.6f)),
            Map.entry("minecraft:chests/village/village_snowy_house", new Coins(false, 3, 12, 0.6f)),
            Map.entry("minecraft:chests/village/village_desert_house", new Coins(false, 3, 12, 0.6f)),
            Map.entry("minecraft:chests/village/village_weaponsmith", new Coins(false, 4, 16, 0.5f)),
            Map.entry("minecraft:chests/village/village_toolsmith", new Coins(false, 4, 16, 0.5f)),
            Map.entry("minecraft:chests/simple_dungeon", new Coins(false, 6, 20, 0.5f)),
            Map.entry("minecraft:chests/desert_pyramid", new Coins(false, 8, 24, 0.5f)),
            Map.entry("minecraft:chests/shipwreck_treasure", new Coins(true, 1, 2, 0.5f)),
            Map.entry("jttw_engine:chests/heaven_pavilion", new Coins(true, 1, 3, 0.5f)),
            Map.entry("jttw_engine:chests/jade_palace", new Coins(true, 2, 5, 0.7f)),
            Map.entry("jttw_engine:chests/dragon_treasury", new Coins(true, 2, 6, 0.7f)),
            Map.entry("jttw_engine:chests/lion_camel_treasury", new Coins(true, 2, 6, 0.7f)));

    @SubscribeEvent
    public static void onLoad(LootTableLoadEvent event) {
        ResourceLocation name = event.getName();
        Coins c = name == null ? null : ADDED.get(name.toString());
        if (c == null) return;
        Item coin = c.silver() ? ModVillages.SILVER_TAEL.get() : ModVillages.COPPER_CASH.get();
        event.getTable().addPool(LootPool.lootPool()
                .add(LootItem.lootTableItem(coin).apply(SetItemCountFunction.setCount(UniformGenerator.between(c.min(), c.max()))))
                .when(LootItemRandomChanceCondition.randomChance(c.chance()))
                .name(JTTWEngine.MOD_ID + ":coins")
                .build());
    }
}
