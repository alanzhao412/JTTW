package com.jttw.engine.flight;

import com.jttw.engine.JTTWEngine;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Loads the world ahead of a fast flier, so it is ready when they get there.
 *
 * <p>The server loads chunks around a player only once they are near, and loads the whole square
 * of their view. A flier at top speed crosses several chunks a second, and in a straight line
 * almost all of that square is behind or beside them. So while someone flies fast (or glides on
 * an elytra), the chunks along their path are requested a few seconds ahead: a corridor three
 * chunks wide, nearest first. The requests are short-lived tickets: a turn or a stop lets the old
 * path go within seconds, and what is loaded is loaded, not ticked.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class FlightLookAhead {

    private FlightLookAhead() {}

    /** Below this (blocks per tick, 20 blocks a second) the server's own loading keeps up. */
    public static final double MIN_SPEED = 1.0;
    /** Faster than this is a teleport, not flight (the server itself rejects moves past 10). */
    public static final double MAX_SPEED = 10.0;
    /** How far ahead to reach, in ticks of flight at the current speed. */
    public static final int LEAD_TICKS = 100;
    /** Never further than this, whatever the speed. */
    public static final double MAX_LEAD_BLOCKS = 640;
    /** A request lasts this long unless the path renews it. */
    public static final int TICKET_TICKS = LEAD_TICKS + 60;
    /** Chunks either side of the path. */
    public static final int HALF_WIDTH = 1;

    public static final TicketType<ChunkPos> TICKET =
            TicketType.create("jttw_flight_lookahead", Comparator.comparingLong(ChunkPos::toLong), TICKET_TICKS);

    /** Off for a benchmark's control run. */
    public static boolean enabled = true;

    /** Per flier: where they were last tick, and when each chunk on their path was last requested. */
    private static final class Track {
        Vec3 last;
        ServerLevel level;
        final Map<Long, Long> requestedAt = new HashMap<>();
    }

    private static final Map<UUID, Track> TRACKS = new HashMap<>();

    /**
     * The chunks to load ahead of a flier at {@code pos} moving {@code velocity} blocks a tick,
     * nearest first: empty if they are not moving fast enough to outrun the server.
     */
    public static List<ChunkPos> corridor(Vec3 pos, Vec3 velocity) {
        Vec3 flat = new Vec3(velocity.x, 0, velocity.z);
        double speed = flat.length();
        if (speed < MIN_SPEED || speed > MAX_SPEED) return List.of();
        Vec3 dir = flat.scale(1 / speed);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        double lead = Math.min(MAX_LEAD_BLOCKS, speed * LEAD_TICKS);
        Set<ChunkPos> chunks = new LinkedHashSet<>();
        for (double s = 0; s <= lead; s += 8) {
            Vec3 at = pos.add(dir.scale(s));
            for (int w = -HALF_WIDTH; w <= HALF_WIDTH; w++) {
                chunks.add(new ChunkPos(net.minecraft.core.BlockPos.containing(at.add(side.scale(w * 16)))));
            }
        }
        return new ArrayList<>(chunks);
    }

    /** One tick of looking ahead for this player. Returns how many chunks it newly requested. */
    public static int tick(ServerPlayer player) {
        Track track = TRACKS.computeIfAbsent(player.getUUID(), id -> new Track());
        ServerLevel level = player.serverLevel();
        Vec3 now = player.position();
        if (track.level != level) {
            track.level = level;
            track.last = now;
            track.requestedAt.clear();
            return 0;
        }
        Vec3 velocity = now.subtract(track.last == null ? now : track.last);
        track.last = now;
        boolean flying = player.getAbilities().flying || player.isFallFlying();
        if (!enabled || !flying) return 0;

        long tick = level.getGameTime();
        int requested = 0;
        for (ChunkPos chunk : corridor(now, velocity)) {
            Long at = track.requestedAt.get(chunk.toLong());
            if (at != null && tick - at < TICKET_TICKS / 2) continue; // still fresh
            level.getChunkSource().addRegionTicket(TICKET, chunk, 0, chunk); // loaded in full, not ticked
            track.requestedAt.put(chunk.toLong(), tick);
            requested++;
        }
        // Forget requests that have run out, so the map stays the size of one path
        track.requestedAt.values().removeIf(at -> tick - at > TICKET_TICKS);
        return requested;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) tick(p);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        TRACKS.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        TRACKS.clear(); // each track holds its world
    }
}
