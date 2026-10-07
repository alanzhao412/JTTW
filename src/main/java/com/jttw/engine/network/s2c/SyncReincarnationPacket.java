package com.jttw.engine.network.s2c;

import com.jttw.engine.reincarnation.AlignmentData;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Server -> client sync for reincarnation identity.
 *
 * <p>Player persistent NBT is not automatically synced to clients. The identity tab
 * needs {@link AlignmentData#KEY_REINCARNATED} and {@link AlignmentData#KEY_REINCARNATION_OUTCOME}
 * to render subclass + description reliably.
 */
public final class SyncReincarnationPacket {

    private final UUID playerId;
    private final boolean reincarnated;
    private final String outcomeId; // empty if not reincarnated

    public SyncReincarnationPacket(UUID playerId, boolean reincarnated, String outcomeId) {
        this.playerId = playerId;
        this.reincarnated = reincarnated;
        this.outcomeId = outcomeId == null ? "" : outcomeId;
    }

    public static void encode(SyncReincarnationPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeBoolean(pkt.reincarnated);
        buf.writeUtf(pkt.outcomeId);
    }

    public static SyncReincarnationPacket decode(FriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        boolean reincarnated = buf.readBoolean();
        String outcomeId = buf.readUtf();
        return new SyncReincarnationPacket(id, reincarnated, outcomeId);
    }

    public static void handle(SyncReincarnationPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncReincarnationPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player target = mc.level.getPlayerByUUID(pkt.playerId);
        if (target == null) return;

        CompoundTag pd = target.getPersistentData();
        pd.putBoolean(AlignmentData.KEY_REINCARNATED, pkt.reincarnated);
        if (pkt.reincarnated) {
            pd.putString(AlignmentData.KEY_REINCARNATION_OUTCOME, pkt.outcomeId);
        } else {
            pd.remove(AlignmentData.KEY_REINCARNATION_OUTCOME);
        }
    }
}