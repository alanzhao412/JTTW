package com.jttw.engine.village;

import com.jttw.engine.registry.ModVillages;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * A Tang village (data: {@code worldgen/structure/tang_village_<variant>.json}, placed by
 * {@code worldgen/structure_set/tang_villages.json} alongside vanilla's villages, never within a few
 * chunks of one). It stands where its land is fit: in its variant's biomes, out of every zone's region
 * and a margin round it ({@link VillageSites}), on ground near enough level (its footprint's corners
 * and middle within {@value #STEEPEST} blocks of each other) and dry. Its ground is the middle height
 * of those places; the piece brings the rest to it.
 */
public class TangVillageStructure extends Structure {

    public static final MapCodec<TangVillageStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            TangVillage.Variant.CODEC.fieldOf("variant").forGetter(s -> s.variant)
    ).apply(i, TangVillageStructure::new));

    /** The most the ground may rise and fall across a village's footprint. */
    public static final int STEEPEST = 16;
    /** How many of the places looked at may be under water. */
    public static final int WET_PLACES = 1;

    private final TangVillage.Variant variant;

    public TangVillageStructure(StructureSettings settings, TangVillage.Variant variant) {
        super(settings);
        this.variant = variant;
    }

    public TangVillage.Variant variant() {
        return variant;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        Optional<BlockPos> site = site(ctx);
        if (site.isEmpty()) return Optional.empty();
        BlockPos centre = site.get();
        long layout = ctx.random().nextLong();
        return Optional.of(new GenerationStub(centre, b -> b.addPiece(new TangVillagePiece(variant, centre.getX(), centre.getY(), centre.getZ(), layout))));
    }

    /**
     * Where a village of the chunk being looked at would stand (its centre, on its ground), or none:
     * out of every zone and its margin, on ground near enough level and dry, over the sea. The raided
     * villages stand by the same rules.
     */
    public static Optional<BlockPos> site(GenerationContext ctx) {
        ChunkPos chunk = ctx.chunkPos();
        int cx = chunk.getMiddleBlockX(), cz = chunk.getMiddleBlockZ();
        if (!VillageSites.clearOfZones(ctx.seed(), cx, cz)) return Optional.empty();
        int[][] at = {{0, 0}, {TangVillage.WEST, TangVillage.NORTH}, {TangVillage.EAST, TangVillage.NORTH}, {TangVillage.WEST, TangVillage.SOUTH},
                {TangVillage.EAST, TangVillage.SOUTH}, {0, TangVillage.NORTH}, {0, TangVillage.SOUTH}, {TangVillage.WEST, 0}, {TangVillage.EAST, 0}};
        int[] heights = new int[at.length];
        int wet = 0;
        for (int i = 0; i < at.length; i++) {
            int x = cx + at[i][0], z = cz + at[i][1];
            int surface = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            int ground = ctx.chunkGenerator().getFirstFreeHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, ctx.heightAccessor(), ctx.randomState());
            if (surface > ground) wet++;
            heights[i] = ground - 1;
        }
        if (wet > WET_PLACES) return Optional.empty();
        int[] sorted = heights.clone();
        Arrays.sort(sorted);
        if (sorted[sorted.length - 1] - sorted[0] > STEEPEST) return Optional.empty();
        int floor = Math.max(sorted[sorted.length / 2], heights[0] - 2);
        if (floor < ctx.chunkGenerator().getSeaLevel()) return Optional.empty();
        return Optional.of(new BlockPos(cx, floor, cz));
    }

    @Override
    public StructureType<?> type() {
        return ModVillages.TANG_VILLAGE.get();
    }
}
