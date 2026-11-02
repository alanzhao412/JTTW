package com.jttw.engine.village.raid;

import com.jttw.engine.registry.ModGrinds;
import com.jttw.engine.village.TangVillage;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;

/** A raided village, laid chunk by chunk ({@link RaidedVillage#lay}), the land smoothed toward it as a Tang village's is. */
public class RaidedVillagePiece extends StructurePiece implements PieceBeardifierModifier {

    private final TangVillage.Variant variant;
    private final int cx, floor, cz;
    private final long layout;

    public RaidedVillagePiece(TangVillage.Variant variant, int cx, int floor, int cz, long layout) {
        super(ModGrinds.RAIDED_VILLAGE_PIECE.get(), 0, RaidedVillage.box(new BlockPos(cx, floor, cz)));
        this.variant = variant;
        this.cx = cx;
        this.floor = floor;
        this.cz = cz;
        this.layout = layout;
    }

    public RaidedVillagePiece(CompoundTag tag) {
        super(ModGrinds.RAIDED_VILLAGE_PIECE.get(), tag);
        variant = TangVillage.Variant.valueOf(tag.getString("variant").toUpperCase(java.util.Locale.ROOT));
        cx = tag.getInt("cx");
        floor = tag.getInt("floor");
        cz = tag.getInt("cz");
        layout = tag.getLong("layout");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("variant", variant.getSerializedName());
        tag.putInt("cx", cx);
        tag.putInt("floor", floor);
        tag.putInt("cz", cz);
        tag.putLong("layout", layout);
    }

    public BlockPos centre() {
        return new BlockPos(cx, floor, cz);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox area,
                            ChunkPos chunk, BlockPos pivot) {
        BlockPos c = centre();
        RaidedVillage.lay(level, TangVillage.plan(variant, layout), c, area, layout, RaidedVillage.band(level.getSeed(), c), true);
    }

    @Override
    public BoundingBox getBeardifierBox() {
        return new BoundingBox(cx + TangVillage.WEST, floor + 1, cz + TangVillage.NORTH, cx + TangVillage.EAST, floor + TangVillage.CLEAR,
                cz + TangVillage.SOUTH);
    }

    @Override
    public TerrainAdjustment getTerrainAdjustment() {
        return TerrainAdjustment.BEARD_BOX;
    }

    @Override
    public int getGroundLevelDelta() {
        return 0;
    }
}
