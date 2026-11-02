package com.jttw.engine.grind;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Where the villages may be, read from their structure sets' placement alone (no ground made): every
 * set's one potential place in each of its cells ({@link RandomSpreadStructurePlacement#getPotentialStructureChunk}),
 * as a structure set's {@code exclusion_zone} reads another's. Optionally narrowed by biome: a place
 * whose biome none of its set's structures stands in will never have one.
 */
public final class VillageClearance {

    private VillageClearance() {}

    public static final ResourceKey<StructureSet> VANILLA_VILLAGES = ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.withDefaultNamespace("villages"));
    public static final ResourceKey<StructureSet> TANG_VILLAGES = ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath("jttw_engine", "tang_villages"));
    public static final ResourceKey<StructureSet> RAIDED_VILLAGES = ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath("jttw_engine", "raided_villages"));
    /** Every kind of village. */
    public static final List<ResourceKey<StructureSet>> ALL = List.of(VANILLA_VILLAGES, TANG_VILLAGES, RAIDED_VILLAGES);

    /**
     * Whether a village of {@code set} may lie within {@code range} chunks (either way) of {@code chunk}.
     * With {@code biomeAt} (block x, z → biome) a place whose biome takes none of its structures is passed over.
     */
    public static boolean near(RegistryAccess access, ResourceKey<StructureSet> set, long seed, ChunkPos chunk, int range,
                               BiFunction<Integer, Integer, Holder<Biome>> biomeAt) {
        var holder = access.registryOrThrow(Registries.STRUCTURE_SET).getHolder(set).orElse(null);
        if (holder == null || !(holder.value().placement() instanceof RandomSpreadStructurePlacement spread)) return false;
        int step = Math.max(1, spread.spacing() / 2);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int dx = -range - spread.spacing(); dx <= range + spread.spacing(); dx += step) {
            for (int dz = -range - spread.spacing(); dz <= range + spread.spacing(); dz += step) {
                ChunkPos p = spread.getPotentialStructureChunk(seed, chunk.x + dx, chunk.z + dz);
                if (!seen.add(p.toLong())) continue;
                if (Math.abs(p.x - chunk.x) > range || Math.abs(p.z - chunk.z) > range) continue;
                if (biomeAt == null) return true;
                Holder<Biome> b = biomeAt.apply(p.getMiddleBlockX(), p.getMiddleBlockZ());
                for (var entry : holder.value().structures()) if (entry.structure().value().biomes().contains(b)) return true;
            }
        }
        return false;
    }
}
