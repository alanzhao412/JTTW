package com.jttw.engine.world.hell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * A window on the world to build Hell's works through: a box of columns (a chunk, when the land is
 * made) outside which nothing is touched, so that each chunk lays its own part of a work worked
 * out whole, and a chunk made later never undoes another's. Its box is in the plan's coordinates;
 * what is laid may be shifted from them ({@code ox, dy, oz}: the gallery lays pieces of the works
 * on its flat world, near its stage).
 */
public final class Site {

    public final WorldGenLevel level;
    public final int minX, minZ, maxX, maxZ, ox, dy, oz;
    private final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

    public Site(WorldGenLevel level, int minX, int minZ, int maxX, int maxZ, int ox, int dy, int oz) {
        this.level = level;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.ox = ox;
        this.dy = dy;
        this.oz = oz;
    }

    /** The chunk starting at (x0, z0), as the land is made. */
    public static Site chunk(WorldGenLevel level, int x0, int z0) {
        return new Site(level, x0, z0, x0 + 15, z0 + 15, 0, 0, 0);
    }

    /** Whether the box (ax, az)..(bx, bz) (either way round) reaches into this site. */
    public boolean touches(int ax, int az, int bx, int bz) {
        return Math.max(ax, bx) >= minX && Math.min(ax, bx) <= maxX && Math.max(az, bz) >= minZ && Math.min(az, bz) <= maxZ;
    }

    /** Whether the column (x, z) is within it. */
    public boolean has(int x, int z) {
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public void set(int x, int y, int z, BlockState s) {
        if (!has(x, z)) return;
        level.setBlock(p.set(x + ox, y + dy, z + oz), s, Block.UPDATE_CLIENTS);
    }

    public BlockState get(int x, int y, int z) {
        return level.getBlockState(p.set(x + ox, y + dy, z + oz));
    }

    /** Fills a box (corners either way round), what of it is within. */
    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        int ax = Math.max(Math.min(x0, x1), minX), bx = Math.min(Math.max(x0, x1), maxX);
        int az = Math.max(Math.min(z0, z1), minZ), bz = Math.min(Math.max(z0, z1), maxZ);
        for (int x = ax; x <= bx; x++) for (int z = az; z <= bz; z++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) level.setBlock(p.set(x + ox, y + dy, z + oz), s, Block.UPDATE_CLIENTS);
    }

    /** The world position of (x, y, z), heights shifted. */
    public BlockPos pos(int x, int y, int z) {
        return new BlockPos(x + ox, y + dy, z + oz);
    }

    /** A sign (standing or on a wall) with these lines on its front, in a dark ink that glows. */
    public void sign(int x, int y, int z, BlockState sign, String... lines) {
        if (!has(x, z)) return;
        set(x, y, z, sign);
        if (level.getBlockEntity(pos(x, y, z)) instanceof SignBlockEntity be) {
            SignText text = new SignText();
            for (int i = 0; i < Math.min(4, lines.length); i++) text = text.setMessage(i, Component.literal(lines[i]));
            text = text.setColor(net.minecraft.world.item.DyeColor.ORANGE).setHasGlowingText(true);
            // as saved data: while the land is made the sign has no level to tell of a change
            var registries = level.registryAccess();
            var tag = be.saveWithoutMetadata(registries);
            SignText.DIRECT_CODEC.encodeStart(registries.createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), text)
                    .result().ifPresent(saved -> {
                        tag.put("front_text", saved);
                        tag.put("back_text", saved.copy());
                    });
            tag.putBoolean("is_waxed", true);
            be.loadWithComponents(tag, registries);
        }
    }

    /** Puts a creature at (x, y, z) facing {@code yaw}, if the column is within, as a structure's (it keeps there). */
    @Nullable
    public <T extends Mob> T mob(EntityType<T> type, double x, int y, double z, float yaw, @Nullable Consumer<T> then) {
        if (!has((int) Math.floor(x), (int) Math.floor(z))) return null;
        T mob = type.create(level.getLevel());
        if (mob == null) return null;
        mob.moveTo(x + ox, y + dy, z + oz, yaw, 0f);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        if (then != null) then.accept(mob);
        level.addFreshEntityWithPassengers(mob);
        return mob;
    }

    /** The horizontal direction of a unit step (dx, dz). */
    public static Direction dir(int dx, int dz) {
        return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }
}
