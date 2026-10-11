package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Breaks terrain in bulk without lagging the game: what the staff, the Qi Wave, the axe and the
 * mod's explosions tear out goes through here.
 *
 * <ul>
 *   <li><b>No drops.</b> Broken blocks drop nothing, and neither does what breaks off them (a
 *       flower losing its dirt, a torch its wall): vanilla passes the no-drops flag to the block
 *       itself only, so items spawned while a block is broken are stopped here. A container
 *       still spills what was stored in it: that belongs to a player.</li>
 *   <li><b>A budget per tick.</b> At most {@value #BLOCKS_PER_TICK} blocks a tick, across all
 *       work, oldest first, so a huge blast is carved over a few ticks instead of freezing one.</li>
 *   <li><b>Updates at the edge only.</b> A block whose six neighbours are all being broken too
 *       is removed without telling them; the blocks at the edge of the work tell theirs, so sand
 *       still falls and water still flows in.</li>
 *   <li><b>Representative effects.</b> A few puffs of debris and one sound for each tick's work,
 *       not a particle burst and a sound per block.</li>
 *   <li><b>What loses its hold goes too.</b> Plants, kelp, cane, dripstone next to the hole
 *       that can no longer stand are broken with it (no drops), as vanilla would break them a
 *       tick later, dropping them.</li>
 *   <li><b>Loaded ground only.</b> A position in a chunk that is not loaded is skipped rather
 *       than loading it.</li>
 * </ul>
 * Explosions keep vanilla's blast in full (its shape through hard and soft blocks, damage,
 * knockback, sound and smoke); only the blocks it breaks are taken out this way.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class TerrainBreaker {

    private TerrainBreaker() {}

    /** Blocks broken per tick, per world, across all work. */
    public static final int BLOCKS_PER_TICK = 512;
    /** Positions looked at per tick, broken or not: most of a sphere fired into the sky is air. */
    public static final int LOOKS_PER_TICK = BLOCKS_PER_TICK * 8;
    /** Puffs of debris shown per tick of work. */
    private static final int EFFECT_SAMPLES = 6;

    private static final int EDGE = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS;
    private static final int INTERIOR = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    /** One batch of work: the blocks to break, or something to do once the work before it is done. */
    private static final class Job {
        final ArrayList<BlockPos> positions;
        final java.util.Set<Long> members;
        @Nullable final Explosion explosion;
        @Nullable final Runnable then;
        final boolean effects;
        /** How strong its breaker is, against the mod's hardened ground (see HardenedGround). */
        double strength = 1;
        int cursor;

        Job(List<BlockPos> positions, @Nullable Explosion explosion, boolean effects, @Nullable Runnable then) {
            this.positions = new ArrayList<>(positions);
            this.members = new java.util.HashSet<>(positions.size() * 2);
            for (BlockPos p : positions) members.add(p.asLong());
            this.explosion = explosion;
            this.effects = effects;
            this.then = then;
        }

        boolean done() {
            return cursor >= positions.size();
        }
    }

    private static final Map<ServerLevel, ArrayDeque<Job>> QUEUES = new HashMap<>();

    /** True while a block is being broken here, so whatever it knocks loose drops nothing. */
    private static boolean suppressingDrops;

    /** Breaks these blocks, no drops, over as many ticks as it takes. Air and unbreakable blocks are left. */
    public static void breakBlocks(ServerLevel level, Collection<BlockPos> positions) {
        breakBlocks(level, positions, null);
    }

    /**
     * Breaks these blocks for {@code by}, whose strength is weighed against the mod's hardened
     * ground (see HardenedGround): there, only the very strong carve ordinary ground.
     */
    public static void breakBlocks(ServerLevel level, Collection<BlockPos> positions, @Nullable Entity by) {
        if (positions.isEmpty()) return;
        Job job = new Job(dedupe(positions), null, true, null);
        job.strength = HardenedGround.strength(by);
        enqueue(level, job);
    }

    /** Runs {@code action} once all the breaking asked for in this world so far is done. */
    public static void afterQueued(ServerLevel level, Runnable action) {
        if (pending(level) == 0) action.run();
        else enqueue(level, new Job(List.of(), null, false, action));
    }

    /** Blocks waiting to be broken in this world. */
    public static int pending(ServerLevel level) {
        ArrayDeque<Job> q = QUEUES.get(level);
        if (q == null) return 0;
        int n = 0;
        for (Job j : q) n += j.positions.size() - j.cursor;
        return n;
    }

    /**
     * An explosion that breaks terrain: vanilla's blast now (damage, knockback, sound, smoke, the
     * shape it carves), the blocks it breaks taken out with no drops over the coming ticks. TNT in
     * the blast still goes off. Returns the explosion, or null if something (a claim) stopped it.
     */
    @Nullable
    public static Explosion explode(ServerLevel level, @Nullable Entity source, double x, double y, double z, float power) {
        Explosion explosion = new Explosion(level, source, null, null, x, y, z, power, false,
                Explosion.BlockInteraction.DESTROY);
        if (ForgeEventFactory.onExplosionStart(level, explosion)) return null;
        explosion.explode(); // the blast: what it reaches, and the creatures it hurts and throws
        List<BlockPos> blocks = new ArrayList<>(explosion.getToBlow());
        // Each nearby player sees and hears it, and is thrown by it; the blocks follow from the server
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) < 4096.0) {
                player.connection.send(new ClientboundExplodePacket(x, y, z, power, List.of(),
                        explosion.getHitPlayers().get(player)));
            }
        }
        if (!blocks.isEmpty()) enqueue(level, new Job(dedupe(blocks), explosion, false, null));
        return explosion;
    }

    private static List<BlockPos> dedupe(Collection<BlockPos> positions) {
        java.util.LinkedHashSet<BlockPos> unique = new java.util.LinkedHashSet<>();
        for (BlockPos p : positions) unique.add(p.immutable());
        return new ArrayList<>(unique);
    }

    private static void enqueue(ServerLevel level, Job job) {
        QUEUES.computeIfAbsent(level, l -> new ArrayDeque<>()).add(job);
    }

    /** Breaks up to {@code budget} queued blocks in this world. Returns how many it broke. */
    public static int work(ServerLevel level, int budget) {
        ArrayDeque<Job> queue = QUEUES.get(level);
        if (queue == null) return 0;
        int broken = 0;
        int looked = 0;
        int lookLimit = budget * 8;
        List<BlockPos> shown = new ArrayList<>();
        List<BlockState> shownStates = new ArrayList<>();
        while (!queue.isEmpty() && broken < budget && looked < lookLimit) {
            Job job = queue.peek();
            while (!job.done() && broken < budget && looked < lookLimit) {
                BlockPos pos = job.positions.get(job.cursor++);
                looked++;
                if (!level.isLoaded(pos)) continue; // never load a chunk to break it
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
                // in the mod's own places, ordinary ground stands up to the carving powers (a blast's
                // blocks were already weighed against it, as it detonated)
                if (job.explosion == null && !HardenedGround.carvable(level, pos, state, job.strength)) continue;
                boolean interior = true;
                for (Direction d : Direction.values()) {
                    if (!job.members.contains(pos.relative(d).asLong())) { interior = false; break; }
                }
                // A container's contents spill (they are a player's); nothing else drops
                suppressingDrops = !(level.getBlockEntity(pos) instanceof net.minecraft.world.Container);
                try {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), interior ? INTERIOR : EDGE);
                } finally {
                    suppressingDrops = false;
                }
                if (job.explosion != null) state.getBlock().wasExploded(level, pos, job.explosion); // TNT goes off
                // Next to the hole, what can no longer stand (a plant, kelp, dripstone) goes now:
                // vanilla breaks it on the next tick, dropping it, before a queue could get there
                if (!interior) broken += breakUnsupported(level, pos, job.members);
                if (job.effects && shown.size() < EFFECT_SAMPLES && (broken % 16 == 0)) {
                    shown.add(pos);
                    shownStates.add(state);
                }
                broken++;
            }
            if (job.done()) {
                queue.poll();
                if (job.then != null) job.then.run();
            }
        }
        if (queue.isEmpty()) QUEUES.remove(level);
        for (int i = 0; i < shown.size(); i++) {
            BlockPos p = shown.get(i);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, shownStates.get(i)),
                    p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 12, 0.6, 0.6, 0.6, 0.1);
        }
        if (!shown.isEmpty()) {
            BlockPos p = shown.get(0);
            level.playSound(null, p, shownStates.get(0).getSoundType().getBreakSound(), SoundSource.BLOCKS, 1.2f, 0.8f);
        }
        return broken;
    }

    /** Breaks, with no drops, everything around {@code from} that has lost its hold, and so on outward. */
    private static int breakUnsupported(ServerLevel level, BlockPos from, java.util.Set<Long> seen) {
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        open.add(from);
        int broken = 0;
        while (!open.isEmpty() && broken < BLOCKS_PER_TICK) {
            BlockPos at = open.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = at.relative(d);
                if (!seen.add(n.asLong()) || !level.isLoaded(n)) continue;
                BlockState ns = level.getBlockState(n);
                if (ns.isAir() || ns.getDestroySpeed(level, n) < 0 || ns.canSurvive(level, n)) {
                    seen.remove(n.asLong()); // it stands; a later break beside it may still undo it
                    continue;
                }
                suppressingDrops = !(level.getBlockEntity(n) instanceof net.minecraft.world.Container);
                try {
                    level.setBlock(n, Blocks.AIR.defaultBlockState(), EDGE);
                } finally {
                    suppressingDrops = false;
                }
                broken++;
                open.add(n.immutable());
            }
        }
        return broken;
    }

    @SubscribeEvent
    public static void onItemSpawn(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (suppressingDrops && !event.loadedFromDisk() && event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) work(level, BLOCKS_PER_TICK);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        QUEUES.clear(); // work in flight holds its world
    }
}
