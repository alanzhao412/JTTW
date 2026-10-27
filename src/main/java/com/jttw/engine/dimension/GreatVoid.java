package com.jttw.engine.dimension;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.peng.PengCharacter;
import com.jttw.engine.character.peng.PengSkillTree;
import com.jttw.engine.character.wukong.WukongCharacter;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The Great Void (Taixu): an empty realm laid one to one over every other, for the fastest fliers
 * to cross the world in. Nothing is made there (no ground, no caves, no trees), so the world keeps
 * up at any speed. One goes in at where one is and comes out where one has got to, in the realm one
 * came from: x, z and height as they are, the nearest open space if that is inside something.
 *
 * <p>The key (Y) is Sun Wukong's at Somersault Cloud (Cloud Step III) and the Peng's at
 * Ninety-Thousand-Li III: those whose flight builds to the top speed. In the Void one always flies,
 * and never sinks into its floor.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class GreatVoid {
    private GreatVoid() {}

    public static final ResourceKey<Level> KEY =
            ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "great_void"));

    /** The realm a traveller in the Void came from, and goes back to. */
    public static final String ORIGIN_TAG = "jttw_void_origin";

    /**
     * How far the Void is loaded and sent round each traveller, in chunks. There is nothing in it
     * to see, and at its top speed a full view (a square 25 chunks wide) is more than a thousand
     * new chunks a second; a narrow one and the path loaded ahead (FlightLookAhead) keep up.
     */
    public static final int VIEW_DISTANCE = 3;

    /** How far up or down from where one comes out the nearest open space is looked for. */
    public static final int EXIT_SEARCH = 64;

    public static boolean isIn(ServerPlayer player) {
        return player.level().dimension().equals(KEY);
    }

    /** Wukong at Somersault Cloud, the Peng at Ninety-Thousand-Li III. */
    public static boolean canEnter(ServerPlayer player) {
        if (WukongCharacter.is(player)) return SkillTreeData.getFlightTier(player) >= 3;
        if (PengCharacter.is(player)) return PengSkillTree.TREE.highestTier(SkillTreeData.getUnlockedNodes(player), "gp_soar") >= 3;
        return false;
    }

    /** The key: into the Void from anywhere, out of it to where one came from. */
    public static void toggle(ServerPlayer player) {
        if (isIn(player)) leave(player);
        else if (canEnter(player)) enter(player);
    }

    /** Into the Void at the same x, y, z, flying. False if it is not loaded, or one is already there. */
    public static boolean enter(ServerPlayer player) {
        ServerLevel voidLevel = player.server.getLevel(KEY);
        if (voidLevel == null || isIn(player)) return false;
        player.getPersistentData().putString(ORIGIN_TAG, player.level().dimension().location().toString());
        Vec3 v = player.getDeltaMovement();
        double y = Math.max(voidLevel.getMinBuildHeight() + 1, Math.min(voidLevel.getMaxBuildHeight() - 2, player.getY()));
        player.teleportTo(voidLevel, player.getX(), y, player.getZ(), player.getYRot(), player.getXRot());
        fly(player);
        player.setDeltaMovement(v);
        player.displayClientMessage(Component.literal("§7The Great Void"), true);
        return true;
    }

    /** Back to the realm one came from (the Overworld, if it is gone), at the same x, z. False if not in the Void. */
    public static boolean leave(ServerPlayer player) {
        if (!isIn(player)) return false;
        ServerLevel to = origin(player);
        Vec3 v = player.getDeltaMovement();
        Vec3 at = exit(player, to);
        player.getPersistentData().remove(ORIGIN_TAG);
        player.teleportTo(to, at.x, at.y, at.z, player.getYRot(), player.getXRot());
        player.setDeltaMovement(v);
        player.fallDistance = 0;
        return true;
    }

    /** The realm {@code player} entered the Void from: the Overworld if that is not recorded or not loaded. */
    public static ServerLevel origin(ServerPlayer player) {
        CompoundTag tag = player.getPersistentData();
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(ORIGIN_TAG));
        ServerLevel level = id == null ? null : player.server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        return level == null || level.dimension().equals(KEY) ? player.server.overworld() : level;
    }

    /**
     * Where {@code player} comes out in {@code to}: their own x, z and height (kept within the realm's
     * height, under a roof like the Nether's), or the nearest height above or below with room for
     * them; with none within {@link #EXIT_SEARCH}, where a realm leap would set them down.
     */
    public static Vec3 exit(ServerPlayer player, ServerLevel to) {
        double x = player.getX(), z = player.getZ();
        BlockPos column = to.getWorldBorder().clampToBounds(x, player.getY(), z);
        if (column.getX() != Mth.floor(x) || column.getZ() != Mth.floor(z)) {
            x = column.getX() + 0.5;
            z = column.getZ() + 0.5;
        }
        int bottom = to.getMinBuildHeight();
        int top = (to.dimensionType().hasCeiling() ? to.getMinBuildHeight() + to.getLogicalHeight() : to.getMaxBuildHeight()) - 2;
        double y = Math.max(bottom, Math.min(top, player.getY()));
        to.getChunk(column.getX() >> 4, column.getZ() >> 4); // made first, to look inside it
        for (int d = 0; d <= EXIT_SEARCH; d++) {
            for (int sign : d == 0 ? new int[] {1} : new int[] {1, -1}) {
                double at = d == 0 ? y : Math.floor(y) + sign * d;
                if (at < bottom || at > top) continue;
                if (roomAt(player, to, x, at, z)) return new Vec3(x, at, z);
            }
        }
        return RealmArrival.arrival(player, to).pos();
    }

    /** Room for {@code player} with their feet at y in {@code level}: nothing solid, and no lava. */
    private static boolean roomAt(ServerPlayer player, ServerLevel level, double x, double y, double z) {
        AABB box = player.getDimensions(player.getPose()).makeBoundingBox(new Vec3(x, y, z));
        return level.noCollision(player, box) && !level.containsAnyLiquid(box);
    }

    /** One who can fly, flying. */
    private static void fly(ServerPlayer player) {
        if (player.isSpectator()) return;
        if (!player.getAbilities().mayfly || !player.getAbilities().flying) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
            player.onUpdateAbilities();
        }
    }

    /** In the Void one flies always, and its floor holds one up; one who no longer may enter it is sent home. */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isIn(player)) return;
        if (player.isSpectator()) return;
        if (!player.isCreative() && !canEnter(player)) {
            leave(player);
            return;
        }
        fly(player);
        double floor = player.level().getMinBuildHeight();
        if (player.getY() < floor) {
            player.teleportTo(player.getX(), floor, player.getZ());
            Vec3 v = player.getDeltaMovement();
            player.setDeltaMovement(v.x, Math.max(0, v.y), v.z);
        }
    }

    /** Keeps the Void's view narrow: the server's own view setting resets every realm's. Unchanged, it costs nothing. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0) return;
        ServerLevel voidLevel = event.getServer().getLevel(KEY);
        if (voidLevel == null) return;
        voidLevel.getChunkSource().setViewDistance(VIEW_DISTANCE);
        voidLevel.getChunkSource().setSimulationDistance(Math.min(VIEW_DISTANCE, event.getServer().getPlayerList().getSimulationDistance()));
    }
}
