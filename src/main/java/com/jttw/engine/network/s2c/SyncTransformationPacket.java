package com.jttw.engine.network.s2c;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.FriendlyByteBuf;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to synchronize transformation state from server to client
 */
public class SyncTransformationPacket implements CustomPacketPayload {

    public static final Type<SyncTransformationPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "sync_transformation"));
    public static final StreamCodec<FriendlyByteBuf, SyncTransformationPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), SyncTransformationPacket::decode);

    @Override
    public Type<SyncTransformationPacket> type() {
        return TYPE;
    }
    private final UUID playerId;
    @Nullable
    private final TransformationData data;

    public SyncTransformationPacket(UUID playerId, @Nullable TransformationData data) {
        this.playerId = playerId;
        this.data = data;
    }

    /**
     * Encode packet to buffer
     */
    public static void encode(SyncTransformationPacket packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerId);
        buf.writeBoolean(packet.data != null);
        
        if (packet.data != null) {
            packet.data.toNetwork(buf);
        }
    }

    /**
     * Decode packet from buffer
     */
    public static SyncTransformationPacket decode(FriendlyByteBuf buf) {
        UUID playerId = buf.readUUID();
        boolean hasData = buf.readBoolean();
        
        TransformationData data = null;
        if (hasData) {
            data = TransformationData.fromNetwork(buf);
        }
        
        return new SyncTransformationPacket(playerId, data);
    }

    /**
     * Handle packet on client
     */
    public static void handle(SyncTransformationPacket packet, IPayloadContext ctx) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Client-side handling
            TransformationManager.handleClientSync(packet.playerId, packet.data);
        });
    }
}
