package com.jttw.engine.world.fengdu;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import com.jttw.engine.registry.ModBlocks;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.FengduFeature;
import com.jttw.engine.world.hell.HellRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import javax.annotation.Nullable;

/**
 * Fengdu's rules: the ghost veils, and where each leads. In the novel only the dead and Sun Wukong
 * (dragged there in his sleep, at level 30 here) go down to the Ten Courts; in folk belief every
 * soul passes through Fengdu, the Ghost City, on its way. Its Gate of Ghosts is sealed to the
 * living who walk there; they come to it the long way, through the deep dark. So, for any player
 * of any kind or level, standing in a veil ({@link GhostGateBlock}) for {@value #CROSS_TICKS} ticks:
 * <ul>
 *   <li><b>an Ancient City's frame</b>, lit with soul fire ({@link AncientGate}), takes them to
 *       Fengdu, onto its terrace before its Gate of Ghosts ({@link #returnSpot}), where Hell's
 *       soldiers rise to bar it ({@link FengduGate});</li>
 *   <li><b>Fengdu's Gate of Ghosts</b>, once the swarm is beaten and it burns, takes those who broke
 *       it (or have come back up from Diyu) down to Hell's arrival plaza, as Wukong and the
 *       travellers between worlds are taken ({@code JTTWDimensions.teleportTo}: the plaza's safe
 *       ground, the Book of Life and Death on its dais, the side doors set up if an old Hell lacks
 *       them); anyone else it does not let through ({@link GateRules});</li>
 *   <li><b>the side doors of Hell's Gate of Ghosts</b> ({@code HellWorks.RETURN_DOOR_X}) take them
 *       back up to Fengdu, before its gate, facing away from it, and it burns for them;</li>
 *   <li><b>the side doors of Fengdu's gate</b> take them back to the Ancient City they came by.</li>
 * </ul>
 * Stepping out of the veil before then, nothing happens; a veil in the Nether or Heaven leads
 * nowhere. The arrival on every side is out of the veils, so no one is sent straight back.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Fengdu {

    private Fengdu() {}

    public static final Zones.Zone ZONE = Zones.FENGDU;

    /** How long a player stands in a ghost veil before it takes them through, in ticks. */
    public static final int CROSS_TICKS = 20;
    /** How long they have stood in one so far (gone once they step out). */
    private static final String KEY_IN_VEIL = JTTWEngine.MOD_ID + ":in_ghost_veil";

    /** Whether {@code p} stands in a ghost veil. */
    public static boolean inVeil(Player p) {
        return veilAt(p) != null;
    }

    /** The place of the ghost veil {@code p} stands in, or null. */
    @Nullable
    public static BlockPos veilAt(Player p) {
        var box = p.getBoundingBox().deflate(0.05);
        for (BlockPos at : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (p.level().getBlockState(at).is(ModBlocks.GHOST_GATE.get())) return at.immutable();
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) tick(p);
    }

    /**
     * One tick of a player at the gates: their trip lapsing if they have left Fengdu; in a veil, the
     * mist gathers round them, and when they have stood there {@value #CROSS_TICKS} ticks it takes
     * them through, if it will; out of one, nothing. Returns whether it took them.
     */
    public static boolean tick(ServerPlayer p) {
        if (p.tickCount % 20 == 0) FengduGate.lapse(p);
        CompoundTag data = p.getPersistentData();
        BlockPos veil = !p.isAlive() || p.isSpectator() || p.isPassenger() ? null : veilAt(p);
        if (veil == null) {
            if (data.contains(KEY_IN_VEIL)) data.remove(KEY_IN_VEIL);
            return false;
        }
        int t = data.getInt(KEY_IN_VEIL) + 1;
        ServerLevel level = p.serverLevel();
        GhostGateBlock.Way way = HellRules.inHell(level) ? null : level.getBlockState(veil).getValue(GhostGateBlock.WAY);
        boolean barred = way == GhostGateBlock.Way.HELL && !FengduGate.mayPass(p);
        if (t == 1) {
            level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1f, 0.5f);
            p.displayClientMessage(Component.literal(way == null ? "§3The cold mist gathers round you: the way back to the living..."
                    : barred ? "§8The mist is cold and will not part for you: the Gate of Ghosts lets through only those who broke its guard."
                    : way == GhostGateBlock.Way.HELL ? "§3The cold mist gathers round you: the way down to Diyu..."
                    : way == GhostGateBlock.Way.FENGDU ? "§3The cold mist gathers round you: the road to Fengdu, the Ghost City..."
                    : "§3The cold mist gathers round you: the way back to the Ancient City..."), true);
        }
        if (t % 4 == 0 && !barred) level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 4, 0.35, 0.6, 0.35, 0.02);
        if (t < CROSS_TICKS || barred) {
            data.putInt(KEY_IN_VEIL, Math.min(t, CROSS_TICKS));
            return false;
        }
        data.remove(KEY_IN_VEIL);
        boolean took = cross(p, veil);
        if (!took) data.putInt(KEY_IN_VEIL, CROSS_TICKS); // nowhere to go: not asked again until they step out
        return took;
    }

    /** Takes {@code p} through the veil at {@code veil}, where it leads. Returns whether it led anywhere. */
    public static boolean cross(ServerPlayer p, BlockPos veil) {
        if (HellRules.inHell(p.level())) {
            up(p);
            return true;
        }
        if (!p.level().dimension().equals(Level.OVERWORLD)) return false;
        BlockState s = p.level().getBlockState(veil);
        if (!s.is(ModBlocks.GHOST_GATE.get())) return false;
        return switch (s.getValue(GhostGateBlock.WAY)) {
            case HELL -> {
                if (!FengduGate.mayPass(p)) yield false;
                down(p);
                yield true;
            }
            case FENGDU -> {
                AncientGate.toFengdu(p, veil);
                yield true;
            }
            case CITY -> AncientGate.back(p);
        };
    }

    /** Down through Fengdu's Gate of Ghosts to Hell's arrival plaza. */
    public static void down(ServerPlayer p) {
        FengduGate.setTrip(p, GateRules.Trip.NONE);
        JTTWDimensions.teleportTo(p, JTTWDimensions.HELL);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.4f);
        p.sendSystemMessage(Component.literal("§8You pass the Gate of Ghosts. The mist closes round you, and parts on Diyu, the "
                + "underworld: the plaza where the dead come in. §7(The side doors of its Gate of Ghosts lead back up to Fengdu.)"));
    }

    /** Up through a side door of Hell's Gate of Ghosts to Fengdu, before its own, which burns for them. */
    public static void up(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        BlockPos at = returnSpot(overworld);
        p.teleportTo(overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        overworld.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
        p.sendSystemMessage(Component.literal("§8The mist thins, and you step out before the Gate of Ghosts of Fengdu, the Ghost City, "
                + "among the living again. §7(It burns for you: step back through it to go down again; its side doors lead to the Ancient City.)"));
        FengduGate.arriveFromHell(p, ground(overworld));
    }

    /** The centre of Fengdu's region, as {x, z}. */
    public static int[] centre(long seed) {
        return ZONE.centres(seed)[0];
    }

    private static long groundSeed;
    @Nullable
    private static BlockPos groundAt;

    /** The city's ground (its local origin: the paving's level at the region's centre), worked out once a world (nothing is loaded for it). */
    public static synchronized BlockPos ground(ServerLevel overworld) {
        if (groundAt == null || groundSeed != overworld.getSeed()) {
            int[] c = centre(overworld.getSeed());
            groundAt = new BlockPos(c[0], FengduFeature.cityFloor(overworld, c[0], c[1]), c[1]);
            groundSeed = overworld.getSeed();
        }
        return groundAt;
    }

    /**
     * Where one comes to Fengdu, by the Ancient City's frame or up out of Diyu: on its terrace,
     * before its Gate of Ghosts (the land there made first, the city with it); if the city is not
     * there, the ground there.
     */
    public static BlockPos returnSpot(ServerLevel overworld) {
        BlockPos at = FengduGate.arrival(ground(overworld));
        overworld.getChunk(at.getX() >> 4, at.getZ() >> 4);
        if (standable(overworld, at)) return at;
        return new BlockPos(at.getX(), overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
    }

    /** Whether one can stand at {@code at}: solid under it, room for a body over it. */
    public static boolean standable(Level level, BlockPos at) {
        return level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                && !level.getBlockState(at).blocksMotion() && !level.getBlockState(at.above()).blocksMotion();
    }
}
