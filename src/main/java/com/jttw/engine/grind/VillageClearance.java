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
     * With {@code stands} (the set, a place: whether a village of it stands there) only a place where
     * one does counts; without, any place the set may put one.
     */
    public static boolean near(RegistryAccess access, ResourceKey<StructureSet> set, long seed, ChunkPos chunk, int range,
                               java.util.function.BiPredicate<StructureSet, ChunkPos> stands) {
        var holder = access.registryOrThrow(Registries.STRUCTURE_SET).getHolder(set).orElse(null);
        if (holder == null || !(holder.value().placement() instanceof RandomSpreadStructurePlacement spread)) return false;
        int step = Math.max(1, spread.spacing() / 2);
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (int dx = -range - spread.spacing(); dx <= range + spread.spacing(); dx += step) {
            for (int dz = -range - spread.spacing(); dz <= range + spread.spacing(); dz += step) {
                ChunkPos p = spread.getPotentialStructureChunk(seed, chunk.x + dx, chunk.z + dz);
                if (!seen.add(p.toLong())) continue;
                if (Math.abs(p.x - chunk.x) > range || Math.abs(p.z - chunk.z) > range) continue;
                if (stands == null || stands.test(holder.value(), p)) return true;
            }
        }
        return false;
    }

    /** Whether a village of {@code set} would stand at {@code at}: by biome alone ({@code biomeAt}), the cheap test. */
    public static boolean byBiome(StructureSet set, ChunkPos at, BiFunction<Integer, Integer, Holder<Biome>> biomeAt) {
        Holder<Biome> b = biomeAt.apply(at.getMiddleBlockX(), at.getMiddleBlockZ());
        for (var entry : set.structures()) if (entry.structure().value().biomes().contains(b)) return true;
        return false;
    }

    /** Whether a village of {@code set} stands at {@code at} in {@code level} as made by {@code gen}: each of its structures' own test, as the land is made. */
    public static boolean stands(net.minecraft.server.level.ServerLevel level, net.minecraft.world.level.chunk.ChunkGenerator gen, StructureSet set, ChunkPos at) {
        // the set's own placement first: its frequency, and its exclusion zone (a raided village never within 8 chunks of a Tang one)
        if (!set.placement().isStructureChunk(level.getChunkSource().getGeneratorState(), at.x, at.z)) return false;
        for (var entry : set.structures()) {
            var s = entry.structure().value();
            var ctx = new net.minecraft.world.level.levelgen.structure.Structure.GenerationContext(level.registryAccess(), gen, gen.getBiomeSource(),
                    level.getChunkSource().randomState(), level.getStructureManager(), level.getSeed(), at, level, s.biomes()::contains);
            if (s.findValidGenerationPoint(ctx).isPresent()) return true;
        }
        return false;
    }
}
