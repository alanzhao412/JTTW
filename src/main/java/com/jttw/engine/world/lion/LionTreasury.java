package com.jttw.engine.world.lion;

import com.jttw.engine.JTTWEngine;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * One Yin-Yang Vase a world. A world whose Lion-Camel City was made before the Lion-Camel Cave
 * was keeps the old treasury chest on the city hall's dais (the vase in it, or taken from it
 * already); the throne room's treasury, made later, would hold a second. So the vase's entry in
 * {@code chests/lion_camel_treasury} is under {@link #ONE_VASE}: it holds unless the chest being
 * unpacked is in the cave and the city's old chest is there ({@link #oldCityChest}).
 */
public final class LionTreasury {

    private LionTreasury() {}

    /** Where the old city treasury chest stood: local x and z of the city, and the heights above its floor to look at. */
    public static final int OLD_X = LionCamelCity.THRONES[2] + 3, OLD_Z = LionCamelCity.THRONE_Z, OLD_FROM = 0, OLD_TO = 8;

    public static final DeferredRegister<LootItemConditionType> CONDITIONS =
            DeferredRegister.create(Registries.LOOT_CONDITION_TYPE, JTTWEngine.MOD_ID);

    /** The condition: {@code {"condition": "jttw_engine:one_vase"}}. */
    public static final DeferredHolder<LootItemConditionType, LootItemConditionType> ONE_VASE =
            CONDITIONS.register("one_vase", () -> new LootItemConditionType(OneVase.CODEC));

    public static void register(IEventBus modEventBus) {
        CONDITIONS.register(modEventBus);
    }

    /** The old city treasury chest of the region round (x, z), if a world made before left one there; else null. */
    public static BlockPos oldCityChest(ServerLevel level, int x, int z) {
        if (!level.dimension().equals(Level.OVERWORLD)) return null;
        LionGates.Fortress f = LionGates.of(level, x, z);
        if (f == null) return null;
        return oldCityChest(level, f);
    }

    /** The old city treasury chest of fortress {@code f}'s city, if a world made before left one there; else null. */
    public static BlockPos oldCityChest(ServerLevel level, LionGates.Fortress f) {
        for (int y = OLD_FROM; y <= OLD_TO; y++) {
            BlockPos p = f.world(OLD_X, y, OLD_Z);
            if (level.getBlockState(p).is(Blocks.CHEST)) return p;
        }
        return null;
    }

    /** Holds unless a second vase would be made: the chest unpacked is not the old city one, and that one is there. */
    public enum OneVase implements LootItemCondition {
        INSTANCE;

        public static final MapCodec<OneVase> CODEC = MapCodec.unit(INSTANCE);

        @Override
        public LootItemConditionType getType() {
            return ONE_VASE.get();
        }

        @Override
        public boolean test(LootContext context) {
            Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
            ServerLevel level = context.getLevel();
            if (origin == null) return true;
            BlockPos at = BlockPos.containing(origin);
            // only a chest in the cave is a second: anything else is as it was (and costs no look at the city)
            if (!level.dimension().equals(Level.OVERWORLD)) return true;
            int[] c = com.jttw.engine.world.Zones.LION_CAMEL.centre(level.getSeed(), at.getX(), at.getZ());
            if (c == null || !LionFortress.covers(at.getX() - c[0], at.getZ() - c[1])) return true;
            BlockPos old = oldCityChest(level, at.getX(), at.getZ());
            return old == null || old.equals(at);
        }
    }
}
