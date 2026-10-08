package com.jttw.engine.network.c2s;

import com.jttw.engine.entity.PlacedStaffEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client → Server: the owner struck their planted staff. Sent when the click lands on the staff
 * as it is drawn, which the game's own entity hitbox cannot follow along a tall staff.
 */
public class StaffTopplePacket {

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

    public static void handle(StaffTopplePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            Entity entity = player.serverLevel().getEntity(pkt.entityId);
            if (entity instanceof PlacedStaffEntity staff && staff.withinReachOf(player)) staff.toppleBy(player);
        });
        ctx.setPacketHandled(true);
    }
}
