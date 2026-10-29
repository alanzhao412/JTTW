package com.jttw.engine.world.cave;

import com.jttw.engine.entity.cave.CaveMonster;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.feature.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lady Earth Flow's household put in its places: a group of mice or maids (or both) at a post, a
 * floor's guardian, the gate's guard; as the land is made (kept, never despawned) or as the cave
 * breeds more near someone walking it (not kept). Each is of its floor's strength
 * ({@link CaveMonster#regularOn}) and keeps to its floor.
 */
public final class CaveHousehold {

    private CaveHousehold() {}

    /** A guardian's health over a captain's (it is a miniboss: see ZoneMonster#becomeMiniboss). */
    public static final double GUARDIAN_HEALTH = 2.0;
    /** How far from its post a kept group strays, a guardian, the gate's guard. */
    public static final int POST_RANGE = 10, GUARDIAN_RANGE = 4, GATE_RANGE = 10;

    /**
     * Puts a group at {@code at}: {@code count} of the kind ({@link Labyrinth#MICE}, {@code MAIDS},
     * {@code MIXED}), the first a captain if {@code captain}; of floor k's strength (-1 the mountain);
     * kept at the post if {@code kept}. Returns them.
     */
    public static List<CaveMonster> group(ServerLevelAccessor level, BlockPos at, int kind, int count, boolean captain, int k, boolean kept,
                                          RandomSource random) {
        List<CaveMonster> out = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            boolean maid = kind == Labyrinth.MAIDS || (kind == Labyrinth.MIXED && n % 2 == 1);
            EntityType<? extends CaveMonster> type = maid ? ModEntities.FLOWER_MAID.get() : ModEntities.MOUSE_SPIRIT.get();
            double a = n * Math.PI * 2 / Math.max(1, count) + random.nextDouble() * 0.5;
            BlockPos spot = n == 0 ? at : BlockPos.containing(at.getX() + 0.5 + Math.cos(a) * 1.2, at.getY(), at.getZ() + 0.5 + Math.sin(a) * 1.2);
            CaveMonster m = put(level, type, spot, at, n == 0 && captain, k, kept ? POST_RANGE : -1, random);
            if (m != null) out.add(m);
        }
        for (CaveMonster m : out) level.addFreshEntityWithPassengers(m);
        return out;
    }

    /** Floor k's guardian, holding the cell before its way down: a miniboss, of more than a captain's health, named for the floor. */
    @Nullable
    public static CaveMonster guardian(ServerLevelAccessor level, BlockPos at, int k, RandomSource random) {
        EntityType<? extends CaveMonster> type = k % 2 == 0 ? ModEntities.MOUSE_SPIRIT.get() : ModEntities.FLOWER_MAID.get();
        CaveMonster m = put(level, type, at, at, true, k, GUARDIAN_RANGE, random);
        if (m == null) return null;
        int lv = CaveMonster.guardianOn(k);
        m.becomeMiniboss(lv, GUARDIAN_HEALTH);
        m.getPersistentData().putBoolean(BottomlessCave.KEY_GUARDIAN, true);
        m.setCustomName(Component.literal("Warden of " + Labyrinth.FLOOR_NAMES[k] + " · Lv " + lv)
                .withStyle(s -> s.withColor(TextColor.fromRgb(ZoneMonster.PURPLE))));
        m.setCustomNameVisible(true);
        level.addFreshEntityWithPassengers(m);
        return m;
    }

    /** The gate's guard before the great door: a mouse captain, three of his pack, two maids. */
    public static List<CaveMonster> gateGuard(ServerLevelAccessor level, BlockPos at, RandomSource random) {
        List<CaveMonster> out = new ArrayList<>();
        for (int n = 0; n < 6; n++) {
            boolean maid = n >= 4;
            double a = n * Math.PI / 3;
            BlockPos spot = BlockPos.containing(at.getX() + 0.5 + Math.cos(a) * 2, at.getY(), at.getZ() + 0.5 + Math.sin(a) * 2);
            CaveMonster m = put(level, maid ? ModEntities.FLOWER_MAID.get() : ModEntities.MOUSE_SPIRIT.get(), spot, at, n == 0, -1, GATE_RANGE, random);
            if (m == null) continue;
            m.getPersistentData().putBoolean(BottomlessCave.KEY_GATE, true);
            out.add(m);
        }
        for (CaveMonster m : out) level.addFreshEntityWithPassengers(m);
        return out;
    }

    /**
     * One creature of the household at {@code spot}, standing on the ground nearest its height: of
     * floor k's strength, a captain or not; kept within {@code range} of {@code home} (-1: not kept, it
     * may be gone once no one is near, as the world's own are). Not yet in the world: the caller adds it, all set.
     */
    @Nullable
    static CaveMonster put(ServerLevelAccessor level, EntityType<? extends CaveMonster> type, BlockPos spot, BlockPos home, boolean captain, int k,
                           int range, RandomSource random) {
        CaveMonster m = type.create(level.getLevel());
        if (m == null) return null;
        BlockPos stand = standing(level, m, spot);
        m.moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, random.nextFloat() * 360f, 0f);
        m.finalizeSpawn(level, level.getCurrentDifficultyAt(stand), range >= 0 ? MobSpawnType.STRUCTURE : MobSpawnType.MOB_SUMMONED, null);
        int lv = captain ? CaveMonster.captainOn(k) - 1 + random.nextInt(3) : CaveMonster.regularOn(k) - 1 + random.nextInt(3);
        m.become(lv, captain);
        if (k >= 0) m.getPersistentData().putInt(BottomlessCave.KEY_FLOOR, k + 1);
        if (range >= 0) {
            m.setPersistenceRequired();
            m.restrictTo(home, range);
            m.getPersistentData().putBoolean(BottomlessCave.KEY_POST, true);
        }
        return m;
    }

    /** Where it can stand at {@code at}'s column: searching from a few blocks over to a few under, ground beneath and room for it; else {@code at}. */
    static BlockPos standing(ServerLevelAccessor level, CaveMonster mob, BlockPos at) {
        BlockPos.MutableBlockPos p = at.mutable();
        for (int dy = 2; dy >= -4; dy--) {
            p.setY(at.getY() + dy);
            if (!level.getBlockState(p.below()).blocksMotion()) continue;
            var box = mob.getType().getDimensions().makeBoundingBox(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (level.noCollision(box) && !level.containsAnyLiquid(box)) return p.immutable();
        }
        return at;
    }
}
