package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.wild.RaidChiefEntity;
import com.jttw.engine.grind.InfestationMarks;
import com.jttw.engine.village.raid.RaidedVillagePiece;
import com.jttw.engine.village.raid.RaidedVillageStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The open world's grinding grounds' registrations, kept apart so they merge cleanly: the raided
 * villages (their structure and piece, their chief) and the infested wilds' marks. See
 * {@code com.jttw.engine.village.raid} and {@code com.jttw.engine.grind}.
 */
public final class ModGrinds {

    private ModGrinds() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, JTTWEngine.MOD_ID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, JTTWEngine.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, JTTWEngine.MOD_ID);

    /** A raided village's chief, the Yaksha King: an ogre made a gym's leader. */
    public static final DeferredHolder<EntityType<?>, EntityType<RaidChiefEntity>> RAID_CHIEF = ENTITIES.register("raid_chief", () ->
            EntityType.Builder.<RaidChiefEntity>of(RaidChiefEntity::new, MobCategory.MONSTER)
                    .sized(1.6F * RaidChiefEntity.SIZE, 3.3F * RaidChiefEntity.SIZE)
                    .eyeHeight(2.9F * RaidChiefEntity.SIZE)
                    .clientTrackingRange(10)
                    .build(JTTWEngine.id("raid_chief").toString()));

    public static final DeferredHolder<Item, Item> RAID_CHIEF_SPAWN_EGG = ITEMS.register("raid_chief_spawn_egg",
            () -> new net.neoforged.neoforge.common.DeferredSpawnEggItem(RAID_CHIEF, 0x1E2440, 0xE2AE3E, new Item.Properties()));

    public static final DeferredHolder<StructureType<?>, StructureType<RaidedVillageStructure>> RAIDED_VILLAGE = STRUCTURE_TYPES.register("raided_village",
            () -> () -> RaidedVillageStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> RAIDED_VILLAGE_PIECE = PIECES.register("raided_village",
            () -> (StructurePieceType.ContextlessType) RaidedVillagePiece::new);

    public static final DeferredHolder<Feature<?>, InfestationMarks> INFESTATION_MARKS = FEATURES.register("infestation_marks",
            () -> new InfestationMarks(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
        ITEMS.register(modEventBus);
        STRUCTURE_TYPES.register(modEventBus);
        PIECES.register(modEventBus);
        FEATURES.register(modEventBus);
        modEventBus.addListener(ModGrinds::onAttributeCreate);
    }

    private static void onAttributeCreate(EntityAttributeCreationEvent event) {
        event.put(RAID_CHIEF.get(), RaidChiefEntity.createAttributes().build());
    }
}
