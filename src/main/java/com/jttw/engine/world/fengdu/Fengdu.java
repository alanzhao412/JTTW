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
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Fengdu's rules: its Gates of Ghosts, the way down to Diyu for everyone. In the novel only the
 * dead and Sun Wukong (dragged there in his sleep, at level 30 here) go down to the Ten Courts; in
 * folk belief every soul passes through Fengdu, the Ghost City, on its way. So, for any player of
 * any kind or level:
 * <ul>
 *   <li><b>down</b>: stand in the ghost veil of Fengdu's Gate of Ghosts ({@link GhostGateBlock})
 *       for {@value #CROSS_TICKS} ticks and you are taken to Hell's arrival plaza, as Wukong and the
 *       travellers between worlds are ({@code JTTWDimensions.teleportTo}: the plaza's safe ground,
 *       the Book of Life and Death on its dais, the side doors set up if an old Hell lacks them);</li>
 *   <li><b>up</b>: stand so in the veil of one of the side doors of the Gate of Ghosts on Hell's
 *       arrival plaza ({@code HellWorks.RETURN_DOOR_X}) and you are taken back up to Fengdu, before
 *       its Gate of Ghosts ({@link #returnSpot}), facing away from it.</li>
 * </ul>
 * Stepping out of the veil before then, nothing happens; a veil anywhere else (the Nether, Heaven)
 * leads nowhere. The arrival on either side is out of the veils, so no one is sent straight back.
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
        return p.level().getBlockStates(p.getBoundingBox().deflate(0.05)).anyMatch(s -> s.is(ModBlocks.GHOST_GATE.get()));
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p) tick(p);
    }

    /**
     * One tick of a player at the gates: in a veil, the mist gathers round them, and when they have
     * stood there {@value #CROSS_TICKS} ticks it takes them through; out of one, nothing. Returns
     * whether it took them.
     */
    public static boolean tick(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || !inVeil(p)) {
            if (data.contains(KEY_IN_VEIL)) data.remove(KEY_IN_VEIL);
            return false;
        }
        int t = data.getInt(KEY_IN_VEIL) + 1;
        ServerLevel level = p.serverLevel();
        if (t == 1) {
            level.playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1f, 0.5f);
            p.displayClientMessage(Component.literal(HellRules.inHell(level) ? "§3The cold mist gathers round you: the way back to the living..."
                    : "§3The cold mist gathers round you: the way down to Diyu..."), true);
        }
        if (t % 4 == 0) level.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1, p.getZ(), 4, 0.35, 0.6, 0.35, 0.02);
        if (t < CROSS_TICKS) {
            data.putInt(KEY_IN_VEIL, t);
            return false;
        }
        data.remove(KEY_IN_VEIL);
        return cross(p);
    }

    /** Takes {@code p} through the gate they stand at: from the overworld down to Diyu, from Diyu up to Fengdu. Returns whether it led anywhere. */
    public static boolean cross(ServerPlayer p) {
        if (p.level().dimension().equals(Level.OVERWORLD)) {
            down(p);
            return true;
        }
        if (HellRules.inHell(p.level())) {
            up(p);
            return true;
        }
        return false;
    }

    /** Down through Fengdu's Gate of Ghosts to Hell's arrival plaza. */
    public static void down(ServerPlayer p) {
        JTTWDimensions.teleportTo(p, JTTWDimensions.HELL);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.4f);
        p.sendSystemMessage(Component.literal("§8You pass the Gate of Ghosts. The mist closes round you, and parts on Diyu, the "
                + "underworld: the plaza where the dead come in. §7(The side doors of its Gate of Ghosts lead back up to Fengdu.)"));
    }

    /** Up through a side door of Hell's Gate of Ghosts to Fengdu, before its own. */
    public static void up(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        BlockPos at = returnSpot(overworld);
        p.teleportTo(overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0f, 0f);
        overworld.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
        p.sendSystemMessage(Component.literal("§8The mist thins, and you step out before the Gate of Ghosts of Fengdu, the Ghost City, "
                + "among the living again."));
    }

    /** The centre of Fengdu's region, as {x, z}. */
    public static int[] centre(long seed) {
        return ZONE.centres(seed)[0];
    }

    /**
     * Where one comes back up out of Diyu: on Fengdu's terrace, before its Gate of Ghosts (the land
     * there made first, the city with it); if the city is not there, the ground there.
     */
    public static BlockPos returnSpot(ServerLevel overworld) {
        int[] c = centre(overworld.getSeed());
        int x = c[0] + FengduCity.RETURN_X, z = c[1] + FengduCity.RETURN_Z;
        overworld.getChunk(x >> 4, z >> 4);
        BlockPos at = new BlockPos(x, FengduFeature.cityFloor(overworld, c[0], c[1]) + FengduCity.TERRACE_H + 1, z);
        if (standable(overworld, at)) return at;
        return new BlockPos(x, overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
    }

    /** Whether one can stand at {@code at}: solid under it, room for a body over it. */
    public static boolean standable(Level level, BlockPos at) {
        return level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)
                && !level.getBlockState(at).blocksMotion() && !level.getBlockState(at.above()).blocksMotion();
    }
}
