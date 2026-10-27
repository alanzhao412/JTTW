package com.jttw.engine.village;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModVillages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.world.PieceBeardifierModifier;

/**
 * A Tang village, laid chunk by chunk from its blueprint ({@link TangVillage}): in each chunk it
 * spans, its share of the ground brought level (footings built down to firm ground, everything
 * above cleared), the buildings, fields and streets, the signs, the chests, and its folk, each put
 * in the chunk they stand in, settled with their calling and their bed. The land round it is
 * smoothed toward it as it is made (its beard).
 */
public class TangVillagePiece extends StructurePiece implements PieceBeardifierModifier {

    public static final ResourceKey<LootTable> HOUSE_CHEST = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/tang_village_house"));
    public static final ResourceKey<LootTable> SMITHY_CHEST = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/tang_village_smithy"));
    /** How far down footings are built to reach firm ground. */
    private static final int FOOTINGS = 12;

    private final TangVillage.Variant variant;
    private final int cx, floor, cz;
    private final long layout;

    public TangVillagePiece(TangVillage.Variant variant, int cx, int floor, int cz, long layout) {
        super(ModVillages.TANG_VILLAGE_PIECE.get(), 0, box(cx, floor, cz));
        this.variant = variant;
        this.cx = cx;
        this.floor = floor;
        this.cz = cz;
        this.layout = layout;
    }

    public TangVillagePiece(CompoundTag tag) {
        super(ModVillages.TANG_VILLAGE_PIECE.get(), tag);
        variant = TangVillage.Variant.valueOf(tag.getString("variant").toUpperCase(java.util.Locale.ROOT));
        cx = tag.getInt("cx");
        floor = tag.getInt("floor");
        cz = tag.getInt("cz");
        layout = tag.getLong("layout");
    }

    static BoundingBox box(int cx, int floor, int cz) {
        return new BoundingBox(cx + TangVillage.WEST, floor + TangVillage.BOTTOM, cz + TangVillage.NORTH,
                cx + TangVillage.EAST, floor + TangVillage.CLEAR, cz + TangVillage.SOUTH);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("variant", variant.getSerializedName());
        tag.putInt("cx", cx);
        tag.putInt("floor", floor);
        tag.putInt("cz", cz);
        tag.putLong("layout", layout);
    }

    public TangVillage.Plan plan() {
        return TangVillage.plan(variant, layout);
    }

    /** Where its centre (the crossroads) is, on its ground. */
    public BlockPos centre() {
        return new BlockPos(cx, floor, cz);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox area,
                            ChunkPos chunk, BlockPos pivot) {
        lay(level, plan(), new BlockPos(cx, floor, cz), area, true);
    }

    /**
     * Lays the village's share within {@code area} round {@code centre} (its crossroads, on its
     * ground), its folk too if {@code folk}. The gallery's and tests' way to lay one whole as well.
     */
    public static void lay(WorldGenLevel level, TangVillage.Plan plan, BlockPos centre, BoundingBox area, boolean folk) {
        int cx = centre.getX(), floor = centre.getY(), cz = centre.getZ();
        int minY = level.getMinBuildHeight() + 1, maxY = level.getMaxBuildHeight() - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int x0 = Math.max(area.minX(), cx + TangVillage.WEST), x1 = Math.min(area.maxX(), cx + TangVillage.EAST);
        int z0 = Math.max(area.minZ(), cz + TangVillage.NORTH), z1 = Math.min(area.maxZ(), cz + TangVillage.SOUTH);
        BlockState footing = plan.variant == TangVillage.Variant.OASIS ? Blocks.SANDSTONE.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int lx = x - cx, lz = z - cz;
                // footings, down to firm ground
                for (int y = floor + TangVillage.BOTTOM - 1; y >= Math.max(minY, floor - FOOTINGS); y--) {
                    BlockState under = level.getBlockState(p.set(x, y, z));
                    if (under.blocksMotion() && under.getFluidState().isEmpty()) break;
                    level.setBlock(p, y < floor - 6 ? Blocks.STONE.defaultBlockState() : footing, Block.UPDATE_CLIENTS);
                }
                for (int ly = TangVillage.BOTTOM; ly <= TangVillage.CLEAR; ly++) {
                    int y = floor + ly;
                    if (y < minY || y > maxY) continue;
                    BlockState st = plan.at(lx, ly, lz);
                    if (st == null) {
                        // below the ground, keep what is solid; above it, leave nothing standing
                        if (ly <= 0 && !level.getBlockState(p.set(x, y, z)).blocksMotion()) level.setBlock(p, footing, Block.UPDATE_CLIENTS);
                        continue;
                    }
                    if (st.isAir() && level.getBlockState(p.set(x, y, z)).isAir()) continue;
                    level.setBlock(p.set(x, y, z), st, Block.UPDATE_CLIENTS);
                }
                if (plan.riceAt(lx, lz)) level.setBlock(p.set(x, floor, z), Blocks.SEAGRASS.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (TangVillage.Sign sign : plan.signs) {
            BlockPos at = new BlockPos(cx + plan.planX(sign.x()), floor + sign.y(), cz + sign.z());
            if (area.isInside(at)) sign(level, at, plan.facing(sign.facing()), sign.lines());
        }
        for (TangVillage.Chest chest : plan.chests) {
            BlockPos at = new BlockPos(cx + plan.planX(chest.x()), floor + chest.y(), cz + chest.z());
            if (!area.isInside(at)) continue;
            level.setBlock(at, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, plan.facing(Direction.SOUTH)), Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(at) instanceof RandomizableContainerBlockEntity c) {
                c.setLootTable(chest.smithy() ? SMITHY_CHEST : HOUSE_CHEST, level.getSeed() ^ at.asLong());
            }
        }
        if (!folk) return;
        for (TangVillage.Resident r : plan.residents) {
            BlockPos at = new BlockPos(cx + plan.planX(r.x()), floor + 1, cz + r.z());
            if (!area.isInside(at)) continue;
            TangFolkEntity f = ModVillages.TANG_VILLAGER.get().create(level.getLevel());
            if (f == null) continue;
            f.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            f.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            BlockPos bed = r.bed() == null ? null : new BlockPos(cx + plan.planX(r.bed()[0]), floor + r.bed()[1], cz + r.bed()[2]);
            f.settle(r.calling(), r.woman(), bed, centre.above());
            level.addFreshEntityWithPassengers(f);
        }
        for (int[] c : plan.camels) {
            BlockPos at = new BlockPos(cx + plan.planX(c[0]), floor + 1, cz + c[1]);
            if (!area.isInside(at)) continue;
            var camel = EntityType.CAMEL.create(level.getLevel());
            if (camel == null) continue;
            camel.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360f, 0f);
            camel.setPersistenceRequired();
            level.addFreshEntity(camel);
        }
    }

    /** Puts up a sign at {@code pos}, its text glowing (written as saved data: while the land is made a sign has no level to tell). */
    static void sign(WorldGenLevel level, BlockPos pos, Direction facing, String... lines) {
        level.setBlock(pos, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, facing), Block.UPDATE_CLIENTS);
        if (!(level.getBlockEntity(pos) instanceof SignBlockEntity be)) return;
        SignText text = new SignText();
        for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
        text = text.setColor(net.minecraft.world.item.DyeColor.YELLOW).setHasGlowingText(true);
        var registries = level.registryAccess();
        var tag = be.saveWithoutMetadata(registries);
        SignText.DIRECT_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), text).result().ifPresent(saved -> {
            tag.put("front_text", saved);
            tag.put("back_text", saved.copy());
        });
        tag.putBoolean("is_waxed", true);
        be.loadWithComponents(tag, registries);
    }

    // ── The land smoothed toward it ──────────────────────────────────

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
