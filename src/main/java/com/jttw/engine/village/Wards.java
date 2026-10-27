package com.jttw.engine.village;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * The house-warding talismans (镇宅符) pasted in a level, each where it was pasted until it lapses:
 * for a day ({@value #TICKS} ticks) no monster comes into the world of itself (spawning naturally,
 * in a patrol, or by reinforcement) within {@value #RADIUS} blocks of it. Spawners, eggs and
 * summonings are not stopped. Kept with the level; a pasted ward glints now and then.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Wards extends SavedData {

    public static final int RADIUS = 24;
    public static final long TICKS = 24000;
    private static final String NAME = JTTWEngine.MOD_ID + "_house_wards";

    /** One ward: where, and the game time it lapses at. */
    public record Ward(BlockPos pos, long until) {}

    private final List<Ward> wards = new ArrayList<>();

    public static Wards of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Wards::new, Wards::load, null), NAME);
    }

    private static Wards load(CompoundTag tag, HolderLookup.Provider registries) {
        Wards w = new Wards();
        for (Tag t : tag.getList("wards", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            w.wards.add(new Ward(BlockPos.of(c.getLong("pos")), c.getLong("until")));
        }
        return w;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Ward w : wards) {
            CompoundTag c = new CompoundTag();
            c.putLong("pos", w.pos().asLong());
            c.putLong("until", w.until());
            list.add(c);
        }
        tag.put("wards", list);
        return tag;
    }

    public void add(BlockPos pos, long until) {
        wards.add(new Ward(pos.immutable(), until));
        setDirty();
    }

    /** The wards still standing at {@code gameTime}. */
    public List<Ward> standing(long gameTime) {
        return wards.stream().filter(w -> w.until() > gameTime).toList();
    }

    /** Whether a ward standing at {@code gameTime} keeps monsters from (x, y, z). */
    public boolean warded(double x, double y, double z, long gameTime) {
        for (Ward w : wards) {
            if (w.until() > gameTime && w.pos().distToCenterSqr(x, y, z) <= (double) RADIUS * RADIUS) return true;
        }
        return false;
    }

    private void lapse(long gameTime) {
        if (wards.removeIf(w -> w.until() <= gameTime)) setDirty();
    }

    /** The ways a monster comes of itself, which a ward stops. */
    public static boolean ofItself(MobSpawnType type) {
        return type == MobSpawnType.NATURAL || type == MobSpawnType.PATROL || type == MobSpawnType.REINFORCEMENT || type == MobSpawnType.EVENT;
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (!(event.getEntity() instanceof Enemy) || !ofItself(event.getSpawnType())) return;
        if (!(event.getLevel().getLevel() instanceof ServerLevel level)) return;
        if (of(level).warded(event.getX(), event.getY(), event.getZ(), level.getGameTime())) event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 100 != 0) return;
        Wards w = of(level);
        if (w.wards.isEmpty()) return;
        w.lapse(level.getGameTime());
        for (Ward ward : w.wards) {
            BlockPos p = ward.pos();
            if (level.isLoaded(p) && level.hasNearbyAlivePlayer(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 32)) {
                level.sendParticles(ParticleTypes.ENCHANT, p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.3);
            }
        }
    }
}
