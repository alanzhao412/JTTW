package com.jttw.engine.world.spider;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.registry.SpiderCaveRegistry;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.ZoneTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Silk-Web Ridge as it lives once the land is made: which of the Seven Spider Spirits have fallen
 * ({@link Record}, kept for the world), the walls of silk their deaths loosen, the canopy over the
 * Yellow Flower Temple that falls with the last of them (and with it the Demon Lord's shield), and
 * the Purifying Spring, whose warm water washes away the ridge's venom and its binding silk.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class SpiderCave {

    private SpiderCave() {}

    public static final Zones.Zone ZONE = Zones.SPIDER_CAVE;
    /** How often the ridge's keeper looks about: the spring, the canopy. */
    public static final int EVERY = 10;

    /** A region of the ridge: its centre, and its valley's floor (the cave's floor level). */
    public record Region(int cx, int cz, int floor) {
        public CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("X", cx);
            t.putInt("Z", cz);
            t.putInt("Floor", floor);
            return t;
        }

        public static Region load(CompoundTag t) {
            return new Region(t.getInt("X"), t.getInt("Z"), t.getInt("Floor"));
        }

        /** A local spot (from the centre; y over the floor) in the world. */
        public BlockPos at(int lx, int ly, int lz) {
            return new BlockPos(cx + lx, floor + ly, cz + lz);
        }

        long key() {
            return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
        }
    }

    /** A bower of the cave: its region, and which (0 the first in). */
    public record Bower(Region region, int index) {
        public CompoundTag save() {
            CompoundTag t = region.save();
            t.putInt("Bower", index);
            return t;
        }

        public static Bower load(CompoundTag t) {
            return new Bower(Region.load(t), t.getInt("Bower"));
        }
    }

    /** The valley floor of the region centred at (cx, cz) in this level. */
    public static int floor(WorldGenLevel level, int cx, int cz) {
        var shape = ZoneTerrain.broadShape(level);
        int minY = level.getMinBuildHeight(), maxY = ZoneTerrain.landTop(level);
        return SilkRidgeShape.floor(level.getSeed(), cx, cz, (x, z) -> ZoneTerrain.landAt(shape, x, z, minY, maxY),
                level.getSeaLevel() + SilkRidgeShape.ABOVE_SEA, maxY - SilkRidgeShape.UNDER_TOP);
    }

    /** The region of the ridge nearest (x, z) in this level. */
    @Nullable
    public static Region regionNear(WorldGenLevel level, int x, int z) {
        int[] c = ZONE.centre(level.getSeed(), x, z);
        return c == null ? null : new Region(c[0], c[1], floor(level, c[0], c[1]));
    }

    // ── The sisters' fall ───────────────────────────────────────────────

    /** Whether all seven of the region's sisters have fallen (and so the canopy is down). */
    public static boolean sistersAllFallen(ServerLevel level, Region region) {
        return Record.of(level).fallen(region) == (1 << SpiderCavePlan.BOWER_COUNT) - 1;
    }

    /** A sister keeping a bower has fallen: remembered, her bower's silk loosened; and if she was the last, the canopy falls. */
    public static void sisterFell(ServerLevel level, Bower bower) {
        Record record = Record.of(level);
        record.fall(bower.region(), bower.index());
        int loosened = loosenSeal(level, bower);
        Component word = Component.translatable(loosened > 0 ? "message.jttw_engine.spider_cave.seal_loosens" : "message.jttw_engine.spider_cave.sister_falls");
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().distSqr(bower.region().at(SpiderCavePlan.BOWERS.get(bower.index()).x(), 0, SpiderCavePlan.BOWERS.get(bower.index()).z())) < 64 * 64) {
                p.displayClientMessage(word, true);
            }
        }
        if (sistersAllFallen(level, bower.region())) {
            for (ServerPlayer p : level.players()) {
                if (ZONE.depth(level.getSeed(), p.getBlockX(), p.getBlockZ()) > 0) {
                    p.sendSystemMessage(Component.translatable("message.jttw_engine.spider_cave.canopy_falls"));
                }
            }
            if (dissolveCanopy(level, bower.region())) record.clearCanopy(bower.region());
        }
    }

    /** Loosens the silk shutting the way on from {@code bower}: every block of its seal let fall. Returns how many. */
    public static int loosenSeal(ServerLevel level, Bower bower) {
        int tunnel = SpiderCavePlan.sealOf(bower.index());
        if (tunnel < 0) return 0;
        int n = 0;
        for (int[] b : SpiderCavePlan.sealBlocks(tunnel)) {
            BlockPos p = bower.region().at(b[0], b[1], b[2]);
            if (!level.isLoaded(p) || !level.getBlockState(p).is(SpiderCaveRegistry.SILK_SEAL.get())) continue;
            level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            level.sendParticles(ParticleTypes.WHITE_ASH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 10, 0.4, 0.4, 0.4, 0.02);
            n++;
        }
        if (n > 0) {
            int[] first = SpiderCavePlan.sealBlocks(tunnel).get(0);
            BlockPos at = bower.region().at(first[0], first[1], first[2]);
            level.playSound(null, at, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 2f, 0.5f);
        }
        return n;
    }

    /**
     * Lets the canopy over the region's temple fall: each of its blocks in a loaded chunk. Returns
     * whether every chunk it spans was loaded (so it is all down).
     */
    public static boolean dissolveCanopy(ServerLevel level, Region region) {
        boolean all = true;
        int ox = SilkRidgeWorks.TEMPLE_X, oz = SilkRidgeWorks.TEMPLE_Z;
        for (int tx = -SilkRidgeWorks.CANOPY_REACH_X; tx <= SilkRidgeWorks.CANOPY_REACH_X; tx++) {
            for (int tz = -SilkRidgeWorks.CANOPY_REACH_Z; tz <= SilkRidgeWorks.CANOPY_REACH_Z; tz++) {
                BlockPos col = region.at(ox + tx, 0, oz + tz);
                if (!level.isLoaded(col)) {
                    all = false;
                    continue;
                }
                for (int ly = 0; ly <= SilkRidgeWorks.CANOPY_H; ly++) {
                    BlockState silk = SilkRidgeWorks.canopyAt(tx, ly, tz);
                    if (silk == null) continue;
                    BlockPos p = region.at(ox + tx, ly, oz + tz);
                    if (!level.getBlockState(p).is(silk.getBlock())) continue;
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    if ((tx + tz + ly) % 7 == 0) level.sendParticles(ParticleTypes.WHITE_ASH, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.4, 0.4, 0.4, 0.01);
                }
            }
        }
        level.playSound(null, region.at(ox, 6, oz), SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 4f, 0.4f);
        return all;
    }

    // ── The keeper's rounds: the spring, the canopy ─────────────────────

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD)) return;
        if (level.getGameTime() % EVERY != 0) return;
        long seed = level.getSeed();
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || ZONE.depth(seed, p.getBlockX(), p.getBlockZ()) <= 0) continue;
            Region region = regionNear(level, p.getBlockX(), p.getBlockZ());
            if (region == null) continue;
            int lx = p.getBlockX() - region.cx(), lz = p.getBlockZ() - region.cz();
            if (p.isInWater() && SilkRidgeWorks.inPool(lx, lz)) bathe(p);
            if (Math.hypot(lx - SilkRidgeWorks.SPRING_X, lz - SilkRidgeWorks.SPRING_Z) < 48) steam(level, region);
            if (level.getGameTime() % (EVERY * 4) == 0 && Math.hypot(lx - SilkRidgeWorks.TEMPLE_X, lz - SilkRidgeWorks.TEMPLE_Z) < 96) {
                Record record = Record.of(level);
                if (sistersAllFallen(level, region) && !record.canopyCleared(region) && dissolveCanopy(level, region)) record.clearCanopy(region);
            }
        }
    }

    /**
     * A bath in the Purifying Spring: its warm water washes away the ridge's venom, poison and
     * withering, and the silk that binds; and mends a little while one stays in it.
     */
    public static void bathe(net.minecraft.world.entity.LivingEntity e) {
        e.removeEffect(SpiderCaveRegistry.VENOM);
        e.removeEffect(MobEffects.POISON);
        e.removeEffect(MobEffects.WITHER);
        e.removeEffect(ModEffects.PINNED);
        e.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (!e.hasEffect(MobEffects.REGENERATION)) e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, true));
    }

    /** The spring steams: wisps rising off its water. */
    private static void steam(ServerLevel level, Region region) {
        var r = level.random;
        for (int i = 0; i < 6; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble());
            double x = region.cx() + SilkRidgeWorks.SPRING_X + 0.5 + Math.cos(a) * d * SilkRidgeWorks.POOL_RX;
            double z = region.cz() + SilkRidgeWorks.SPRING_Z + 0.5 + Math.sin(a) * d * SilkRidgeWorks.POOL_RZ;
            level.sendParticles(ParticleTypes.CLOUD, x, region.floor() + 0.1, z, 1, 0.2, 0.05, 0.2, 0.01);
            if (i % 2 == 0) level.sendParticles(ParticleTypes.WHITE_ASH, x, region.floor() + 0.6, z, 2, 0.3, 0.3, 0.3, 0.01);
        }
    }

    // ── The record ──────────────────────────────────────────────────────

    /** Which sisters of each region have fallen, and whether its canopy is all down: kept for the world. */
    public static final class Record extends SavedData {

        private static final String NAME = JTTWEngine.MOD_ID + "_spider_cave";
        private final Map<Long, Integer> fallen = new HashMap<>();
        private final Map<Long, Boolean> cleared = new HashMap<>();

        public static Record of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Record::new, Record::load, null), NAME);
        }

        private static Record load(CompoundTag tag, HolderLookup.Provider registries) {
            Record r = new Record();
            CompoundTag f = tag.getCompound("Fallen"), c = tag.getCompound("Cleared");
            for (String k : f.getAllKeys()) r.fallen.put(Long.parseLong(k), f.getInt(k));
            for (String k : c.getAllKeys()) r.cleared.put(Long.parseLong(k), c.getBoolean(k));
            return r;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            CompoundTag f = new CompoundTag(), c = new CompoundTag();
            fallen.forEach((k, v) -> f.putInt(Long.toString(k), v));
            cleared.forEach((k, v) -> c.putBoolean(Long.toString(k), v));
            tag.put("Fallen", f);
            tag.put("Cleared", c);
            return tag;
        }

        /** The region's fallen sisters, by bower, as bits. */
        public int fallen(Region region) {
            return fallen.getOrDefault(region.key(), 0);
        }

        public void fall(Region region, int bower) {
            fallen.merge(region.key(), 1 << bower, (a, b) -> a | b);
            setDirty();
        }

        public boolean canopyCleared(Region region) {
            return cleared.getOrDefault(region.key(), false);
        }

        public void clearCanopy(Region region) {
            cleared.put(region.key(), true);
            setDirty();
        }

        /** For tests: forgets the region. */
        public void forget(Region region) {
            fallen.remove(region.key());
            cleared.remove(region.key());
            setDirty();
        }
    }
}
