package com.jttw.engine.gametest.transformation;

import com.jttw.engine.gametest.GT;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.network.c2s.TransmuteItemPacket;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.transformation.TransformationAbilityHandler;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.NetworkDirection;
import net.neoforged.neoforge.network.NetworkEvent;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.BiConsumer;

/** Helpers shared by the transformation GameTests. */
final class Tf {

    private Tf() {}

    /**
     * A fresh player standing on the platform floor at relative (2.5, 2, 2.5).
     *
     * <p>Relative y = 0 is the structure block's position; the platform's stone floor is at
     * relative y = 1, so {@link GT#player} (placed at y = 1) stands inside the floor. Tests that
     * care about blocks, projectiles or positions use this instead.
     */
    static ServerPlayer player(GameTestHelper h) {
        ServerPlayer p = GT.player(h);
        p.moveTo(h.absoluteVec(new Vec3(2.5, 2, 2.5)));
        return p;
    }

    /**
     * Gives a fake player a plain vanilla connection so that teleports move it (the Forge fake
     * player's own connection ignores them). Packets sent to it are queued and never delivered.
     */
    static ServerPlayer teleportable(ServerPlayer p) {
        new ServerGamePacketListenerImpl(p.server, new Connection(PacketFlow.SERVERBOUND), p);
        return p;
    }

    /** A fresh player already transformed into {@code type}. */
    static ServerPlayer playerAs(GameTestHelper h, EntityType<?> type) {
        ServerPlayer p = player(h);
        TransformationManager.transform(p, type);
        h.assertTrue(TransformationManager.isTransformed(p), "setup: transform into " + type);
        return p;
    }

    /** Grants the Wukong transform nodes 1..tier (tier 0 grants nothing). */
    static void grantWukongTier(ServerPlayer p, int tier) {
        for (int i = 1; i <= tier; i++) SkillTreeData.grantNode(p, "wu_transform_" + i);
    }

    /** Grants the Erlang Shen transform nodes 1..tier. */
    static void grantErlangTier(ServerPlayer p, int tier) {
        for (int i = 1; i <= tier; i++) SkillTreeData.grantNode(p, "es_transform_" + i);
    }

    /** Right-click with the main hand, as the server sees it. */
    static void rightClick(ServerPlayer p) {
        TransformationAbilityHandler.onRightClickItem(new PlayerInteractEvent.RightClickItem(p, InteractionHand.MAIN_HAND));
    }

    /** Entities of a class within {@code radius} blocks of the player. */
    static <T extends Entity> List<T> near(ServerPlayer p, Class<T> cls, double radius) {
        return p.serverLevel().getEntitiesOfClass(cls, p.getBoundingBox().inflate(radius), e -> e != p);
    }

    static void discard(List<? extends Entity> entities) {
        entities.forEach(Entity::discard);
    }

    /** Asserts the entity has {@code effect} at exactly this amplifier and duration. */
    static void hasEffect(GameTestHelper h, LivingEntity e, MobEffect effect, int amplifier, int duration, String what) {
        MobEffectInstance inst = e.getEffect(effect);
        if (inst == null) {
            h.fail(what + ": no " + effect.getDescriptionId() + " effects=" + e.getActiveEffects()
                    + " " + com.jttw.engine.gametest.GT.whereabouts(h, e));
        }
        h.assertTrue(inst.getAmplifier() == amplifier,
                what + ": amplifier expected " + amplifier + " but was " + inst.getAmplifier());
        h.assertTrue(inst.getDuration() == duration,
                what + ": duration expected " + duration + " but was " + inst.getDuration());
    }

    static Vec3 abs(GameTestHelper h, double x, double y, double z) {
        return h.absoluteVec(new Vec3(x, y, z));
    }

    static BlockPos absPos(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new BlockPos(x, y, z));
    }

    // ── client→server packets, delivered the way Forge delivers them ─────────

    /**
     * A server-side network context whose sender is {@code p}. Forge keeps the constructor
     * package-private; it is reached reflectively so the packet's public handle() runs exactly
     * as it does for a real client message (on the server thread, so the work runs at once).
     */
    public static NetworkEvent.Context serverContext(ServerPlayer p) {
        try {
            Connection connection = new Connection(PacketFlow.SERVERBOUND);
            connection.setListener(p.connection);
            Constructor<NetworkEvent.Context> c = NetworkEvent.Context.class.getDeclaredConstructor(
                    Connection.class, NetworkDirection.class, BiConsumer.class);
            c.setAccessible(true);
            BiConsumer<ResourceLocation, FriendlyByteBuf> sink = (id, buf) -> {};
            return c.newInstance(connection, NetworkDirection.PLAY_TO_SERVER, sink);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot build a NetworkEvent.Context", e);
        }
    }

    static void send(ServerPlayer p, RequestTransformPacket pkt) {
        NetworkEvent.Context ctx = serverContext(p);
        RequestTransformPacket.handle(pkt, () -> ctx);
    }

    static void send(ServerPlayer p, TransmuteItemPacket pkt) {
        NetworkEvent.Context ctx = serverContext(p);
        TransmuteItemPacket.handle(pkt, () -> ctx);
    }

    static RequestTransformPacket request(EntityType<?> type) {
        return new RequestTransformPacket(RequestTransformPacket.Kind.ENTITY, EntityType.getKey(type));
    }

    static RequestTransformPacket requestObject(EntityType<?> type) {
        return new RequestTransformPacket(RequestTransformPacket.Kind.OBJECT_ENTITY, EntityType.getKey(type));
    }
}
