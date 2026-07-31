package com.jttw.engine.network;

import com.jttw.engine.transformation.TransformationData;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet to synchronize transformation state from server to client
 */
public class SyncTransformationPacket {
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
    public static void handle(SyncTransformationPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // Client-side handling
            TransformationManager.handleClientSync(packet.playerId, packet.data);
        });
        context.setPacketHandled(true);
    }
}
