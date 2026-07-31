package com.jttw.engine.network;

import net.minecraft.client.Minecraft;
import com.jttw.engine.progression.SkillTreeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs a player's full skill tree state (points + all unlocked node IDs) to the client.
 *
 * <p>Sent by {@link LevelManager} on level-up and by {@link SpendSkillPointPacket}
 * after a successful unlock. The client stores the data in
 * {@link com.jttw.engine.client.ClientAbilityData}.
 */
public class SyncSkillTreePacket {

    private final UUID playerId;
    private final int availablePoints;
    private final Set<String> unlockedNodes;

    public SyncSkillTreePacket(UUID playerId, int availablePoints, Set<String> unlockedNodes) {
        this.playerId       = playerId;
        this.availablePoints = availablePoints;
        this.unlockedNodes  = Set.copyOf(unlockedNodes);
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(SyncSkillTreePacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeInt(pkt.availablePoints);
        buf.writeInt(pkt.unlockedNodes.size());
        for (String id : pkt.unlockedNodes) buf.writeUtf(id);
    }

    public static SyncSkillTreePacket decode(FriendlyByteBuf buf) {
        UUID id     = buf.readUUID();
        int points  = buf.readInt();
        int count   = buf.readInt();
        Set<String> nodes = new HashSet<>();
        for (int i = 0; i < count; i++) nodes.add(buf.readUtf());
        return new SyncSkillTreePacket(id, points, nodes);
    }

    // -----------------------------------------------------------------------
    // Handle
    // -----------------------------------------------------------------------

    public static void handle(SyncSkillTreePacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncSkillTreePacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.getUUID().equals(pkt.playerId)) return;
        com.jttw.engine.client.ClientAbilityData.setSkillTree(pkt.availablePoints, pkt.unlockedNodes);
    }

    // -----------------------------------------------------------------------
    // Server-side builder helper
    // -----------------------------------------------------------------------

    public static SyncSkillTreePacket build(net.minecraft.server.level.ServerPlayer player) {
        return new SyncSkillTreePacket(
                player.getUUID(),
                SkillTreeData.getPoints(player),
                SkillTreeData.getUnlockedNodes(player)
        );
    }
}