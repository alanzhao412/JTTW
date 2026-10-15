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
 * Client → Server: the owner struck their planted staff. Sent when the click lands on the staff
 * as it is drawn, which the game's own entity hitbox cannot follow along a tall staff.
 */
public class StaffTopplePacket implements CustomPacketPayload {

    public static final Type<StaffTopplePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "staff_topple"));
    public static final StreamCodec<FriendlyByteBuf, StaffTopplePacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), StaffTopplePacket::decode);

    @Override
    public Type<StaffTopplePacket> type() {
        return TYPE;
    }

    private final int entityId;

    public StaffTopplePacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(StaffTopplePacket pkt, FriendlyByteBuf buf) {
        buf.writeInt(pkt.entityId);
    }

    public static StaffTopplePacket decode(FriendlyByteBuf buf) {
        return new StaffTopplePacket(buf.readInt());
    }

    public static void handle(StaffTopplePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            Entity entity = player.serverLevel().getEntity(pkt.entityId);
            if (entity instanceof PlacedStaffEntity staff && staff.withinReachOf(player)) staff.strikeBy(player);
        });
    }
}
