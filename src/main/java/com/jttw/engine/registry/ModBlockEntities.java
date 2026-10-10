package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** The mod's block entities. */
public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, JTTWEngine.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<TrigramsFurnaceBlockEntity>> TRIGRAMS_FURNACE =
            BLOCK_ENTITIES.register("eight_trigrams_furnace", () -> BlockEntityType.Builder
                    .of(TrigramsFurnaceBlockEntity::new, ModBlocks.EIGHT_TRIGRAMS_FURNACE.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
