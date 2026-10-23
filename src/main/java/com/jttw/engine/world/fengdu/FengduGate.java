package com.jttw.engine.world.fengdu;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.hell.HellMonster;
import com.jttw.engine.entity.zone.ZoneMonster;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.fengdu.GateRules.Trip;
import com.jttw.engine.world.fengdu.GateRules.Wave;
import com.jttw.engine.world.fengdu.GateRules.WaveKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fengdu's Gate of Ghosts, sealed and fought open ({@link GateRules} has the rules, this keeps them
 * in the world). A gate is known by its city's ground ({@code ground}: the city's local origin, its
 * paving at its centre): the real one is {@link Fengdu#ground}'s; the gallery and the tests lay
 * cities of their own and keep their gates the same way.
 * <ul>
 *   <li><b>the seal</b>: iron bars across the gate's way ({@link FengduCity#sealed}); burning, the
 *       ghost veil down to Diyu ({@link FengduCity#burning}). A city laid before the gate was
 *       sealed (its way open, no side doors) has its gate laid again when someone comes near it,
 *       as an old Hell's side doors are ({@code HellWorks.ensureReturnDoors});</li>
 *   <li><b>the swarm</b>: someone come through an Ancient City's frame ({@link #arriveByCity})
 *       raises it: Yin soldiers, then their captain, then Ox-Head and Horse-Face, rising out of the
 *       terrace in waves, at the level of those come to fight them. They are kept to the terrace
 *       (one that strays past {@value GateRules#LEASH} blocks, or falls off it, is brought back);
 *       they are never left behind (a swarm no one is near for long sinks back into the ground, and
 *       any of it left in an unloaded chunk is turned away when the chunk loads again);</li>
 *   <li><b>the opening</b>: the swarm beaten, the gate ignites by itself, and the pilgrims there may
 *       pass; one come up out of Diyu ({@link #arriveFromHell}) may pass at once, and the gate burns
 *       for them; it goes out again once no one who may pass has been near it for a while.</li>
 * </ul>
 * Every gate with something going on is kept going each half second, and the real gate whenever
 * anyone is near it.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FengduGate {

    private FengduGate() {}

    /** Where a player stands with the gate ({@link Trip}; not kept through death: dying, one leaves the trip). */
    public static final String KEY_TRIP = JTTWEngine.MOD_ID + ":fengdu_trip";
    /** On a soldier of a swarm: the swarm it rose in, and its gate. */
    public static final String KEY_SWARM = JTTWEngine.MOD_ID + ":fengdu_swarm", KEY_GATE = JTTWEngine.MOD_ID + ":fengdu_gate";
    /** How often the gates are kept, in ticks. */
    public static final int EVERY = 10;

    // ── Who may pass ────────────────────────────────────────────────────

    public static Trip trip(Player p) {
        int t = p.getPersistentData().getInt(KEY_TRIP);
        return t >= 0 && t < Trip.values().length ? Trip.values()[t] : Trip.NONE;
    }

    public static void setTrip(Player p, Trip trip) {
        if (trip == Trip.NONE) p.getPersistentData().remove(KEY_TRIP);
        else p.getPersistentData().putInt(KEY_TRIP, trip.ordinal());
    }

    /** Whether {@code p} may pass the Gate of Ghosts down to Diyu. */
    public static boolean mayPass(Player p) {
        return trip(p).mayPass();
    }

    /** A player's trip lapses once they leave Fengdu: out of the world of the living, or far from every gate. */
    public static void lapse(ServerPlayer p) {
        if (trip(p) == Trip.NONE) return;
        boolean overworld = p.level().dimension().equals(Level.OVERWORLD);
        if (GateRules.lapses(overworld, overworld ? distanceToGate(p) : Double.MAX_VALUE)) setTrip(p, Trip.NONE);
    }

    /** How far (horizontally) {@code p} is from the nearest gate there is. */
    public static double distanceToGate(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        double best = distance(p, Fengdu.ground(overworld));
        for (long g : Data.of(overworld).gates.keySet()) best = Math.min(best, distance(p, BlockPos.of(g)));
        return best;
    }

    private static double distance(Entity e, BlockPos ground) {
        return Math.hypot(e.getX() - (ground.getX() + 0.5), e.getZ() - (ground.getZ() + FengduCity.GHOST_Z + 0.5));
    }

    // ── Where things are ────────────────────────────────────────────────

    /** Where one arrives before the gate of the city at {@code ground}, on its terrace. */
    public static BlockPos arrival(BlockPos ground) {
        return ground.offset(FengduCity.RETURN_X, FengduCity.TERRACE_H + 1, FengduCity.RETURN_Z);
    }

    /** The gate's way, as world places: its veil when it burns, its bars when it is sealed. */
    public static List<BlockPos> way(BlockPos ground) {
        List<BlockPos> way = new ArrayList<>();
        for (int x = -FengduCity.VEIL_HALF; x <= FengduCity.VEIL_HALF; x++) {
            for (int h = 1; h <= FengduCity.VEIL_H; h++) way.add(ground.offset(x, FengduCity.TERRACE_H + h, FengduCity.GHOST_Z));
        }
        return way;
    }

    /** The side doors' ways, back to the Ancient City. */
    public static List<BlockPos> sideDoors(BlockPos ground) {
        List<BlockPos> doors = new ArrayList<>();
        for (int side : new int[]{-1, 1}) for (int x = FengduCity.SIDE_DOOR_IN; x <= FengduCity.SIDE_DOOR_OUT; x++) {
            for (int h = 1; h <= FengduCity.SIDE_DOOR_H; h++) doors.add(ground.offset(side * x, FengduCity.TERRACE_H + h, FengduCity.GHOST_Z));
        }
        return doors;
    }

    /** Whether the city's gate stands at {@code ground} (its lintel there): no blocks are touched where it does not. */
    public static boolean gateStands(ServerLevel level, BlockPos ground) {
        BlockPos lintel = ground.offset(0, FengduCity.TERRACE_H + FengduCity.VEIL_H + 1, FengduCity.GHOST_Z);
        return level.isLoaded(lintel) && level.getBlockState(lintel).is(Blocks.GILDED_BLACKSTONE);
    }

    /** Whether the gate at {@code ground} burns. */
    public static boolean burning(ServerLevel level, BlockPos ground) {
        return level.getBlockState(way(ground).get(0)).is(com.jttw.engine.registry.ModBlocks.GHOST_GATE.get());
    }

    // ── Arrivals ────────────────────────────────────────────────────────

    /**
     * {@code p} has come before the gate at {@code ground} through an Ancient City's frame: they must
     * fight, and a swarm rises (the whole of one, or, if one is already up, another vanguard at once);
     * the gate is put out while it stands.
     */
    public static void arriveByCity(ServerPlayer p, BlockPos ground) {
        ServerLevel level = p.server.overworld();
        setTrip(p, GateRules.arriveByCity(trip(p)));
        State s = Data.of(level).state(ground, true);
        long now = level.getGameTime();
        s.lastNear = now;
        if (s.swarm == 0) {
            s.swarm = now ^ level.random.nextLong() | 1;
            s.raised = 0;
            s.waiting.clear();
            s.waiting.addAll(GateRules.SWARM);
        } else {
            s.urgent++;
        }
        if (s.lit) seal(level, ground, s);
        Data.of(level).setDirty();
        p.sendSystemMessage(Component.literal("§8The flagstones of the terrace shudder underfoot. Out of the ground before the Gate of Ghosts "
                + "Hell's soldiers rise to bar it. §7(The gate opens when the last of them falls; the side doors lead back to the Ancient City.)"));
    }

    /** {@code p} has come back up out of Diyu before the gate at {@code ground}: they may go down again, and it burns for them. */
    public static void arriveFromHell(ServerPlayer p, BlockPos ground) {
        ServerLevel level = p.server.overworld();
        setTrip(p, GateRules.arriveFromHell(trip(p)));
        State s = Data.of(level).state(ground, true);
        long now = level.getGameTime();
        s.lastNear = now;
        s.lastAdmittedNear = now;
        s.everAdmitted = true;
        if (s.swarm == 0 && !s.lit && gateStands(level, ground)) ignite(level, ground, s, List.of(p));
        Data.of(level).setDirty();
    }

    // ── Keeping a gate ──────────────────────────────────────────────────

    /**
     * Keeps the gate at {@code ground} one step, {@code near} the players at it: its swarm risen wave
     * by wave and kept to the terrace, beaten or given up; the gate lit or sealed as the rules say,
     * and its blocks made so (an old city's gate laid again first). Nothing is done where the gate's
     * chunk is not loaded.
     */
    public static void step(ServerLevel level, BlockPos ground, List<ServerPlayer> near) {
        if (!level.isLoaded(arrival(ground))) return;
        Data data = Data.of(level);
        State s = data.state(ground, false);
        if (s == null) {
            if (!near.isEmpty()) keepBlocks(level, ground, false);
            return;
        }
        long now = level.getGameTime();
        if (!near.isEmpty()) s.lastNear = now;
        if (near.stream().anyMatch(FengduGate::mayPass)) {
            s.lastAdmittedNear = now;
            s.everAdmitted = true;
        }
        if (s.swarm != 0) {
            if (GateRules.abandoned(now, s.lastNear)) {
                abandon(level, s);
            } else if (!near.isEmpty() && level.isPositionEntityTicking(arrival(ground))) {
                // the swarm rises, and is beaten, only before someone: with no one at the gate it waits
                int standing = standing(level, s);
                for (; s.urgent > 0; s.urgent--) {
                    standing += raise(level, ground, s, WaveKind.VANGUARD, near);
                    s.raised++;
                }
                if (!s.waiting.isEmpty() && GateRules.nextWaveDue(s.raised, standing)) {
                    standing += raise(level, ground, s, s.waiting.poll(), near);
                    s.raised++;
                }
                leash(level, ground, s);
                if (GateRules.beaten(s.waiting.size(), standing)) {
                    s.swarm = 0;
                    s.raised = 0;
                    for (ServerPlayer p : near) {
                        Trip before = trip(p);
                        setTrip(p, GateRules.swarmBroken(before));
                        if (before == Trip.FIGHTING) p.sendSystemMessage(Component.literal(
                                "§3The last of Hell's soldiers falls. Soul fire roars up in the Gate of Ghosts: the way down to Diyu stands open to you."));
                    }
                    if (near.stream().anyMatch(FengduGate::mayPass)) {
                        s.lastAdmittedNear = now;
                        s.everAdmitted = true;
                    }
                }
            }
            data.setDirty();
        }
        boolean burns = GateRules.burns(s.swarm != 0, s.everAdmitted, now, s.lastAdmittedNear);
        if (gateStands(level, ground)) {
            if (burns && !s.lit) ignite(level, ground, s, near);
            else if (!burns && s.lit) seal(level, ground, s);
            keepBlocks(level, ground, s.lit);
        } else {
            s.lit = burns;
        }
        if (s.swarm == 0 && !s.lit && (!s.everAdmitted || now - s.lastAdmittedNear > GateRules.SEAL_AFTER)) data.forget(ground);
        data.setDirty();
    }

    /** The soldiers of the swarm still standing (the fallen forgotten). */
    private static int standing(ServerLevel level, State s) {
        s.soldiers.removeIf(id -> {
            Entity e = level.getEntity(id);
            return e == null || !e.isAlive();
        });
        return s.soldiers.size();
    }

    /** Raises a wave of the swarm round the gate; returns how many rose. */
    private static int raise(ServerLevel level, BlockPos ground, State s, WaveKind kind, List<ServerPlayer> near) {
        int pilgrims = (int) Math.max(1, near.stream().filter(p -> trip(p) == Trip.FIGHTING).count());
        int highest = near.stream().mapToInt(PlayerLevel::getLevel).max().orElse(com.jttw.engine.entity.hell.YinSoldierEntity.LEVEL);
        int lv = GateRules.soldierLevel(highest);
        Wave wave = GateRules.wave(kind, pilgrims);
        List<BlockPos> spots = spawnSpots(level, ground, near);
        List<EntityType<? extends HellMonster>> who = new ArrayList<>();
        if (wave.guards()) {
            who.add(ModEntities.OX_HEAD.get());
            who.add(ModEntities.HORSE_FACE.get());
        }
        for (int i = 0; i < wave.soldiers(); i++) who.add(ModEntities.YIN_SOLDIER.get());
        int risen = 0;
        for (int i = 0; i < who.size(); i++) {
            BlockPos at = spots.get(i % spots.size());
            boolean captain = !wave.guards() && i < wave.captains();
            HellMonster m = ZoneMonster.summon(level, who.get(i), at, captain);
            if (m == null) continue;
            m.become(lv, captain);
            m.setPersistenceRequired();
            m.getPersistentData().putLong(KEY_SWARM, s.swarm);
            m.getPersistentData().putLong(KEY_GATE, ground.asLong());
            ServerPlayer nearest = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer p : near) {
                double d = p.distanceToSqr(m);
                if (d < best && !p.isCreative() && !p.isSpectator()) {
                    best = d;
                    nearest = p;
                }
            }
            if (nearest != null) m.setTarget(nearest);
            s.soldiers.add(m.getUUID());
            level.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 0.4, m.getZ(), 10, 0.3, 0.4, 0.3, 0.03);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, m.getX(), m.getY() + 0.2, m.getZ(), 6, 0.3, 0.1, 0.3, 0.01);
            risen++;
        }
        BlockPos a = arrival(ground);
        level.playSound(null, a, SoundEvents.SOUL_ESCAPE.value(), SoundSource.HOSTILE, 2f, 0.5f);
        level.playSound(null, a, SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, wave.guards() ? 3f : 1.5f, wave.guards() ? 0.6f : 0.8f);
        if (wave.guards()) {
            for (ServerPlayer p : near) p.displayClientMessage(Component.literal("§4Ox-Head and Horse-Face, the Gate's own guards, rise to hold it."), true);
        }
        return risen;
    }

    /**
     * Where soldiers may rise round the gate at {@code ground}: on the terrace before the gate and
     * beside its screen walls, on firm ground with room, not on top of anyone; failing all that (no
     * city there), round where one arrives.
     */
    static List<BlockPos> spawnSpots(ServerLevel level, BlockPos ground, List<ServerPlayer> near) {
        List<BlockPos> spots = new ArrayList<>();
        int y = ground.getY() + FengduCity.TERRACE_H + 1;
        for (int x = -15; x <= 15; x++) {
            for (int z = FengduCity.GHOST_Z - 7; z <= FengduCity.TERRACE_S - 1; z++) {
                boolean before = z > FengduCity.GHOST_Z + 1 && Math.abs(x) >= 2;
                boolean beside = Math.abs(x) >= FengduCity.SCREEN_TO + 1 && z <= FengduCity.GHOST_Z + 1;
                if (!before && !beside || !FengduCity.onTerrace(x, z)) continue;
                BlockPos at = new BlockPos(ground.getX() + x, y, ground.getZ() + z);
                if (!Fengdu.standable(level, at)) continue;
                if (near.stream().anyMatch(p -> p.distanceToSqr(at.getX() + 0.5, at.getY(), at.getZ() + 0.5) < 9)) continue;
                spots.add(at);
            }
        }
        if (spots.isEmpty()) {
            BlockPos a = arrival(ground);
            for (int i = 0; i < 8; i++) {
                double ang = i * Math.PI / 4;
                spots.add(a.offset((int) Math.round(Math.cos(ang) * 6), 0, (int) Math.round(Math.sin(ang) * 6)));
            }
        }
        Collections.shuffle(spots, new java.util.Random(level.random.nextLong()));
        return spots;
    }

    /** Brings back to the terrace any soldier of the swarm strayed past the leash or fallen off it. */
    private static void leash(ServerLevel level, BlockPos ground, State s) {
        BlockPos a = arrival(ground);
        List<BlockPos> spots = null;
        for (UUID id : s.soldiers) {
            if (!(level.getEntity(id) instanceof HellMonster m)) continue;
            boolean strayed = Math.hypot(m.getX() - (a.getX() + 0.5), m.getZ() - (a.getZ() + 0.5)) > GateRules.LEASH || m.getY() < a.getY() - 2.5;
            if (!strayed) continue;
            if (spots == null) spots = spawnSpots(level, ground, List.of());
            BlockPos to = spots.get(level.random.nextInt(spots.size()));
            level.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 1, m.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
            m.getNavigation().stop();
            m.teleportTo(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
            m.setDeltaMovement(0, 0, 0);
            level.sendParticles(ParticleTypes.SOUL, m.getX(), m.getY() + 1, m.getZ(), 8, 0.3, 0.5, 0.3, 0.02);
        }
    }

    /** No one has been near for long: the swarm sinks back into the ground, and is forgotten. */
    private static void abandon(ServerLevel level, State s) {
        for (UUID id : s.soldiers) {
            Entity e = level.getEntity(id);
            if (e == null) continue;
            level.sendParticles(ParticleTypes.SOUL, e.getX(), e.getY() + 0.5, e.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
            e.discard();
        }
        s.soldiers.clear();
        s.waiting.clear();
        s.urgent = 0;
        s.raised = 0;
        s.swarm = 0;
    }

    /** The gate ignites: its way fills with the veil, soul fire roaring up through it. */
    private static void ignite(ServerLevel level, BlockPos ground, State s, List<ServerPlayer> near) {
        s.lit = true;
        BlockState veil = FengduCity.burning();
        for (BlockPos at : way(ground)) {
            level.setBlockAndUpdate(at, veil);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 6, 0.3, 0.4, 0.15, 0.04);
            level.sendParticles(ParticleTypes.SOUL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 2, 0.3, 0.4, 0.15, 0.02);
        }
        BlockPos mid = ground.offset(0, FengduCity.TERRACE_H + 3, FengduCity.GHOST_Z);
        level.playSound(null, mid, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 2f, 0.5f);
        level.playSound(null, mid, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 3f, 0.6f);
        level.playSound(null, mid, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 2f, 0.7f);
    }

    /** The gate goes out: iron bars fall across its way again. */
    private static void seal(ServerLevel level, BlockPos ground, State s) {
        s.lit = false;
        if (!gateStands(level, ground)) return;
        BlockState bars = FengduCity.sealed();
        for (BlockPos at : way(ground)) {
            level.setBlockAndUpdate(at, bars);
            level.sendParticles(ParticleTypes.SMOKE, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 3, 0.3, 0.4, 0.15, 0.01);
        }
        BlockPos mid = ground.offset(0, FengduCity.TERRACE_H + 3, FengduCity.GHOST_Z);
        level.playSound(null, mid, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 2f, 0.5f);
        level.playSound(null, mid, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 2f, 0.6f);
    }

    /**
     * Makes the gate's blocks what they should be: an old city's gate (its way open with the veil,
     * no side doors) laid again from the blueprint; then its way burning or barred, as {@code lit}.
     */
    static void keepBlocks(ServerLevel level, BlockPos ground, boolean lit) {
        if (!gateStands(level, ground)) return;
        var veil = com.jttw.engine.registry.ModBlocks.GHOST_GATE.get();
        boolean old = sideDoors(ground).stream().anyMatch(at -> !level.getBlockState(at).is(veil));
        for (BlockPos at : way(ground)) {
            BlockState here = level.getBlockState(at);
            if (here.is(veil) && here.getValue(GhostGateBlock.WAY) != GhostGateBlock.Way.HELL) old = true;
        }
        if (old) {
            FengduCity.layGate(level, ground);
            JTTWEngine.LOGGER.info("Fengdu: its Gate of Ghosts at {} laid again, sealed, with its side doors", ground.toShortString());
        }
        BlockState want = lit ? FengduCity.burning() : FengduCity.sealed();
        for (BlockPos at : way(ground)) if (!level.getBlockState(at).equals(want)) level.setBlockAndUpdate(at, want);
    }

    // ── Events ──────────────────────────────────────────────────────────

    /** Every half second: the real gate, if anyone is near it or something is going on there, and every other gate with something going on. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !level.dimension().equals(Level.OVERWORLD) || level.getGameTime() % EVERY != 0) return;
        Data data = Data.of(level);
        BlockPos real = Fengdu.ground(level);
        Set<Long> gates = new HashSet<>(data.gates.keySet());
        List<ServerPlayer> atReal = near(level, real);
        if (!atReal.isEmpty() || gates.contains(real.asLong())) step(level, real, atReal);
        gates.remove(real.asLong());
        for (long g : gates) {
            BlockPos ground = BlockPos.of(g);
            step(level, ground, near(level, ground));
        }
    }

    /** The players at the gate of the city at {@code ground}: alive, not watching, within {@value GateRules#RANGE} blocks. */
    public static List<ServerPlayer> near(ServerLevel level, BlockPos ground) {
        List<ServerPlayer> near = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isSpectator() && distance(p, ground) <= GateRules.RANGE) near.add(p);
        }
        return near;
    }

    /** A soldier of a swarm that is no more (left in a chunk that unloaded while it stood), loaded again, is turned away. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.loadedFromDisk() || !(event.getLevel() instanceof ServerLevel level)) return;
        CompoundTag tag = event.getEntity().getPersistentData();
        if (!tag.contains(KEY_SWARM)) return;
        State s = Data.of(level.getServer().overworld()).state(BlockPos.of(tag.getLong(KEY_GATE)), false);
        if (s == null || s.swarm != tag.getLong(KEY_SWARM)) event.setCanceled(true);
    }

    /** The swarm now standing at the gate of the city at {@code ground}: its soldiers' ids (for the tests and the gallery). */
    public static Set<UUID> swarm(ServerLevel level, BlockPos ground) {
        State s = Data.of(level).state(ground, false);
        return s == null ? Set.of() : Set.copyOf(s.soldiers);
    }

    /** Whether a swarm is up at the gate of the city at {@code ground} (risen or still to rise). */
    public static boolean swarmUp(ServerLevel level, BlockPos ground) {
        State s = Data.of(level).state(ground, false);
        return s != null && s.swarm != 0;
    }

    /** Forgets the gate of the city at {@code ground} and its swarm (the tests', when they are done). */
    public static void reset(ServerLevel level, BlockPos ground) {
        State s = Data.of(level).state(ground, false);
        if (s != null) abandon(level, s);
        Data.of(level).forget(ground);
        if (level.isLoaded(arrival(ground))) keepBlocks(level, ground, false);
    }

    // ── What is kept ────────────────────────────────────────────────────

    /** A gate's state: its swarm (its id, its waves still to rise, extra vanguards owed, its soldiers), whether it burns, and when someone was last near. */
    static final class State {
        long swarm;
        final Deque<WaveKind> waiting = new ArrayDeque<>();
        int urgent, raised;
        final Set<UUID> soldiers = new HashSet<>();
        boolean lit, everAdmitted;
        long lastNear, lastAdmittedNear;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putLong("Swarm", swarm);
            t.putIntArray("Waiting", waiting.stream().mapToInt(Enum::ordinal).toArray());
            t.putInt("Urgent", urgent);
            t.putInt("Raised", raised);
            ListTag ids = new ListTag();
            for (UUID id : soldiers) ids.add(net.minecraft.nbt.NbtUtils.createUUID(id));
            t.put("Soldiers", ids);
            t.putBoolean("Lit", lit);
            t.putBoolean("EverAdmitted", everAdmitted);
            t.putLong("LastNear", lastNear);
            t.putLong("LastAdmittedNear", lastAdmittedNear);
            return t;
        }

        static State load(CompoundTag t) {
            State s = new State();
            s.swarm = t.getLong("Swarm");
            for (int k : t.getIntArray("Waiting")) if (k >= 0 && k < WaveKind.values().length) s.waiting.add(WaveKind.values()[k]);
            s.urgent = t.getInt("Urgent");
            s.raised = t.getInt("Raised");
            for (Tag id : t.getList("Soldiers", Tag.TAG_INT_ARRAY)) s.soldiers.add(net.minecraft.nbt.NbtUtils.loadUUID(id));
            s.lit = t.getBoolean("Lit");
            s.everAdmitted = t.getBoolean("EverAdmitted");
            s.lastNear = t.getLong("LastNear");
            s.lastAdmittedNear = t.getLong("LastAdmittedNear");
            return s;
        }
    }

    /** The gates with something going on, by their city's ground. */
    public static final class Data extends SavedData {

        private static final String NAME = JTTWEngine.MOD_ID + "_fengdu_gates";
        final Map<Long, State> gates = new HashMap<>();

        public static Data of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), NAME);
        }

        private static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            for (Tag t : tag.getList("Gates", Tag.TAG_COMPOUND)) {
                CompoundTag c = (CompoundTag) t;
                d.gates.put(c.getLong("Ground"), State.load(c.getCompound("State")));
            }
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            gates.forEach((g, s) -> {
                CompoundTag c = new CompoundTag();
                c.putLong("Ground", g);
                c.put("State", s.save());
                list.add(c);
            });
            tag.put("Gates", list);
            return tag;
        }

        @Nullable
        State state(BlockPos ground, boolean make) {
            return make ? gates.computeIfAbsent(ground.asLong(), g -> new State()) : gates.get(ground.asLong());
        }

        void forget(BlockPos ground) {
            if (gates.remove(ground.asLong()) != null) setDirty();
        }
    }
}
