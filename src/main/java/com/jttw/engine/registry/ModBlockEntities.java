package com.jttw.engine.registry;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.furnace.TrigramsFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/** The mod's block entities. */
public final class ModBlockEntities {

    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, JTTWEngine.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TrigramsFurnaceBlockEntity>> TRIGRAMS_FURNACE =
            BLOCK_ENTITIES.register("eight_trigrams_furnace", () -> BlockEntityType.Builder
                    .of(TrigramsFurnaceBlockEntity::new, ModBlocks.EIGHT_TRIGRAMS_FURNACE.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
