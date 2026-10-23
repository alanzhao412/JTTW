package com.jttw.engine.world.fengdu;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * The way in: the great frame at the heart of an Ancient City ({@link AncientFrame}), lit with soul
 * fire. In folk belief the dead go down to Fengdu; here the living go the long way round, through
 * the deep dark, where the old city's builders left a door to nowhere. Soul fire set burning in its
 * way (soul sand or soil in the frame, lit by flint and steel, a fire charge, or soul fire come
 * there any way at all) runs round the frame and fills it with the ghost veil
 * ({@link GhostGateBlock.Way#FENGDU}); a player who stands in it is taken to Fengdu, before its
 * sealed Gate of Ghosts, and the gate's guard rises ({@link FengduGate#arriveByCity}). It works on
 * every Ancient City, in a new world or an old one: nothing of it is laid as the land is made.
 *
 * <p>The way back: the side doors of Fengdu's gate take one to the Ancient City they came from
 * (each player's is remembered, through death too), before its frame, on the side they went in by;
 * one who never came by a city is taken to the lit frame nearest Fengdu, and if no frame has ever
 * been lit, nowhere.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class AncientGate {

    private AncientGate() {}

    /** Kept through death: the frame a player came to Fengdu by ({@link AncientFrame.Frame#pack}), and which side of it they stood. */
    public static final String KEY_CITY = JTTWEngine.MOD_ID + ":ancient_city", KEY_SIDE = JTTWEngine.MOD_ID + ":ancient_city_side";

    /** What a block is to a frame: reinforced deepslate its ring; open air, water, soul fire and its fuel, sculk's veins and the veil its way. */
    public static AncientFrame.Cells cells(Level level) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        return (x, y, z) -> {
            p.set(x, y, z);
            if (!level.isLoaded(p)) return AncientFrame.Cell.OTHER;
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.REINFORCED_DEEPSLATE)) return AncientFrame.Cell.FRAME;
            return open(s) ? AncientFrame.Cell.OPEN : AncientFrame.Cell.OTHER;
        };
    }

    private static boolean open(BlockState s) {
        return s.isAir() || s.is(ModBlocks.GHOST_GATE.get()) || s.is(BlockTags.FIRE) || s.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)
                || s.is(Blocks.SCULK_VEIN) || s.is(Blocks.WATER) || s.canBeReplaced();
    }

    /** Soul fire appearing anywhere in the world of the living (placed, lit, spread): if in a frame's way, the frame is lit. */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!event.getState().is(Blocks.SOUL_FIRE) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (level.dimension().equals(Level.OVERWORLD)) light(level, event.getPos());
    }

    /**
     * Lights the frame whose way holds {@code pos}, if there is one: its way fills with the veil to
     * Fengdu, soul fire running round it. Returns the frame, or null where there is none.
     */
    @Nullable
    public static AncientFrame.Frame light(ServerLevel level, BlockPos pos) {
        AncientFrame.Frame f = AncientFrame.find(cells(level), pos.getX(), pos.getY(), pos.getZ());
        if (f == null) return null;
        BlockState veil = GhostGateBlock.veil(f.alongX() ? Direction.Axis.X : Direction.Axis.Z, GhostGateBlock.Way.FENGDU);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < AncientFrame.WIDTH; i++) for (int j = 0; j < AncientFrame.HEIGHT; j++) {
            int[] c = f.cell(i, j);
            level.setBlock(p.set(c[0], c[1], c[2]), veil, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            if ((i + j) % 2 == 0) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c[0] + 0.5, c[1] + 0.5, c[2] + 0.5, 3, 0.3, 0.3, 0.3, 0.03);
        }
        int[] foot = f.foot();
        BlockPos mid = new BlockPos(foot[0], foot[1] + AncientFrame.HEIGHT / 2, foot[2]);
        level.playSound(null, mid, SoundEvents.SOUL_ESCAPE.value(), SoundSource.BLOCKS, 3f, 0.5f);
        level.playSound(null, mid, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.5f, 0.6f);
        level.playSound(null, mid, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.6f, 0.7f);
        for (ServerPlayer p2 : level.players()) {
            if (p2.distanceToSqr(mid.getX(), mid.getY(), mid.getZ()) < 48 * 48) p2.sendSystemMessage(Component.literal(
                    "§3The soul fire runs round the old frame, and a cold mist fills it. Far off, before the sealed Gate of Ghosts of Fengdu, "
                            + "something stirs."));
        }
        Lit.of(level).add(f);
        return f;
    }

    // ── Through the frame, and back ─────────────────────────────────────

    /** Through the frame whose veil holds {@code veil} to Fengdu, before its Gate of Ghosts, where its guard rises; the city remembered. */
    public static void toFengdu(ServerPlayer p, BlockPos veil) {
        ServerLevel overworld = p.server.overworld();
        AncientFrame.Frame f = AncientFrame.find(cells(overworld), veil.getX(), veil.getY(), veil.getZ());
        if (f != null) {
            CompoundTag kept = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
            kept.putLong(KEY_CITY, f.pack());
            int[] foot = f.foot();
            double across = f.alongX() ? p.getZ() - (foot[2] + 0.5) : p.getX() - (foot[0] + 0.5);
            kept.putInt(KEY_SIDE, across < 0 ? -1 : 1);
            p.getPersistentData().put(Player.PERSISTED_NBT_TAG, kept);
        }
        BlockPos at = Fengdu.returnSpot(overworld);
        p.teleportTo(overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180f, 0f);
        overworld.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.6f);
        p.sendSystemMessage(Component.literal("§8The mist closes round you, cold as the deep dark, and parts on a terrace high over the land: "
                + "Fengdu, the Ghost City, before its Gate of Ghosts, and the gate is barred."));
        FengduGate.arriveByCity(p, Fengdu.ground(overworld));
    }

    /** The frame {@code p} came to Fengdu by, if remembered. */
    @Nullable
    public static AncientFrame.Frame remembered(Player p) {
        CompoundTag kept = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        return kept.contains(KEY_CITY) ? AncientFrame.Frame.unpack(kept.getLong(KEY_CITY)) : null;
    }

    /**
     * Back through a side door of Fengdu's gate to the Ancient City {@code p} came by (else the lit
     * frame nearest Fengdu), before its frame. Returns whether there was anywhere to go.
     */
    public static boolean back(ServerPlayer p) {
        ServerLevel overworld = p.server.overworld();
        AncientFrame.Frame f = remembered(p);
        int side = p.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getInt(KEY_SIDE);
        if (f == null) {
            BlockPos g = Fengdu.ground(overworld);
            f = Lit.of(overworld).nearest(g.getX(), g.getZ());
            side = 1;
        }
        if (f == null) {
            p.sendSystemMessage(Component.literal("§8The mist stirs, but finds no road: no Ancient City's frame burns with soul fire in this world."));
            return false;
        }
        BlockPos at = before(overworld, f, side == 0 ? 1 : side);
        int[] foot = f.foot();
        float yaw = (float) Math.toDegrees(Math.atan2(-(at.getX() + 0.5 - (foot[0] + 0.5)), at.getZ() + 0.5 - (foot[2] + 0.5)));
        p.teleportTo(overworld, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0f);
        FengduGate.setTrip(p, GateRules.Trip.NONE);
        overworld.playSound(null, at, SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.2f, 0.8f);
        p.sendSystemMessage(Component.literal("§8The mist thins, and you stand again in the deep dark, before the Ancient City's frame. "
                + "Tread softly: something down here listens."));
        return true;
    }

    /**
     * Where one comes back to the frame {@code f}: on firm ground before it, on {@code side} of it
     * (or the other side, if there is no room), a block or two off its way; the chunks there made
     * first.
     */
    public static BlockPos before(ServerLevel level, AncientFrame.Frame f, int side) {
        int[] foot = f.foot();
        for (int s : new int[]{side, -side}) {
            for (int d : new int[]{2, 3, 1, 4, 5}) {
                for (int dy : new int[]{0, -1, 1, -2, 2}) {
                    BlockPos at = f.alongX() ? new BlockPos(foot[0], foot[1] + dy, foot[2] + s * d) : new BlockPos(foot[0] + s * d, foot[1] + dy, foot[2]);
                    level.getChunk(at.getX() >> 4, at.getZ() >> 4);
                    if (Fengdu.standable(level, at) && !level.getBlockState(at).is(ModBlocks.GHOST_GATE.get())) return at;
                }
            }
        }
        return f.alongX() ? new BlockPos(foot[0], foot[1], foot[2] + 2 * side) : new BlockPos(foot[0] + 2 * side, foot[1], foot[2]);
    }

    /** The frames lit in this world, so a traveller who never came by one can still be sent to one, and the Earth Gods know of them. */
    public static final class Lit extends SavedData {

        private static final String NAME = JTTWEngine.MOD_ID + "_lit_ancient_frames";
        private final Set<Long> frames = new LinkedHashSet<>();

        public static Lit of(ServerLevel level) {
            return level.getServer().overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(Lit::new, Lit::load, null), NAME);
        }

        private static Lit load(CompoundTag tag, HolderLookup.Provider registries) {
            Lit lit = new Lit();
            for (long f : tag.getLongArray("Frames")) lit.frames.add(f);
            return lit;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putLongArray("Frames", frames.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }

        public void add(AncientFrame.Frame f) {
            if (frames.add(f.pack())) setDirty();
        }

        public void remove(AncientFrame.Frame f) {
            if (frames.remove(f.pack())) setDirty();
        }

        /** The lit frame nearest (x, z), or null if none has been lit. */
        @Nullable
        public AncientFrame.Frame nearest(double x, double z) {
            AncientFrame.Frame best = null;
            double bestD = Double.MAX_VALUE;
            for (long packed : frames) {
                AncientFrame.Frame f = AncientFrame.Frame.unpack(packed);
                int[] c = f.foot();
                double d = Math.hypot(c[0] - x, c[2] - z);
                if (d < bestD) {
                    bestD = d;
                    best = f;
                }
            }
            return best;
        }
    }
}
