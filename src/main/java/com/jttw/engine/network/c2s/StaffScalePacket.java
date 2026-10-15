package com.jttw.engine.network.c2s;

import com.jttw.engine.entity.PlacedStaffEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Client → Server: scroll wheel changes placed staff scale.
 */
public class StaffScalePacket implements CustomPacketPayload {

    public static final Type<StaffScalePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "staff_scale"));
    public static final StreamCodec<FriendlyByteBuf, StaffScalePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), StaffScalePacket::decode);

    @Override
    public Type<StaffScalePacket> type() {
        return TYPE;
    }

    private final int entityId;
    private final float delta; // +1 or -1 per scroll tick

    public StaffScalePacket(int entityId, float delta) {
        this.entityId = entityId;
        this.delta = delta;
    }

    public static void encode(StaffScalePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entityId);
        buf.writeFloat(pkt.delta);
    }

    public static StaffScalePacket decode(FriendlyByteBuf buf) {
        return new StaffScalePacket(buf.readInt(), buf.readFloat());
    }

    public static void handle(StaffScalePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;

            Entity entity = player.serverLevel().getEntity(pkt.entityId);
            if (!(entity instanceof PlacedStaffEntity staff)) return;

            // Only the owner, within reach of it
            if (!staff.isOwner(player) || !staff.withinReachOf(player)) return;

            staff.adjustScale(pkt.delta, player);
        });
    }
}
