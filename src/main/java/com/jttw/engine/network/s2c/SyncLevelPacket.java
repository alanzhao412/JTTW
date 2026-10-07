package com.jttw.engine.network.s2c;

import net.minecraft.client.Minecraft;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.stats.ProgressionClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs a player's level, XP, class, and form stage from server to client.
 *
 * <p>Sent by {@link LevelManager} on every level-up and on player login.
 * The client stores the synced data in the target player's persistent data
 * so HUD rendering systems can read it the same way as server-side code.
 *
 * <p><b>Register in {@link com.jttw.engine.network.NetworkHandler#register()}:</b>
 * <pre>{@code
 * CHANNEL.messageBuilder(SyncLevelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
 *     .encoder(SyncLevelPacket::encode)
 *     .decoder(SyncLevelPacket::decode)
 *     .consumerMainThread(SyncLevelPacket::handle)
 *     .add();
 * }</pre>
 */
public class SyncLevelPacket {

    private final UUID playerId;
    private final int level;
    private final long xp;
    @Nullable
    private final String className;   // null = class not yet assigned
    private final int formStage;

    public SyncLevelPacket(UUID playerId, int level, long xp,
                           @Nullable String className, int formStage) {
        this.playerId  = playerId;
        this.level     = level;
        this.xp        = xp;
        this.className = className;
        this.formStage = formStage;
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(SyncLevelPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeInt(pkt.level);
        buf.writeLong(pkt.xp);
        buf.writeBoolean(pkt.className != null);
        if (pkt.className != null) buf.writeUtf(pkt.className);
        buf.writeInt(pkt.formStage);
    }

    public static SyncLevelPacket decode(FriendlyByteBuf buf) {
        UUID id       = buf.readUUID();
        int level     = buf.readInt();
        long xp       = buf.readLong();
        boolean hasCls = buf.readBoolean();
        String cls    = hasCls ? buf.readUtf() : null;
        int form      = buf.readInt();
        return new SyncLevelPacket(id, level, xp, cls, form);
    }

    // -----------------------------------------------------------------------
    // Client-side handling
    // -----------------------------------------------------------------------

    public static void handle(SyncLevelPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncLevelPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player target = mc.level.getPlayerByUUID(pkt.playerId);
        if (target == null) return;

        // Mirror the server NBT so client-side code can call PlayerLevel.getLevel() normally
        PlayerLevel.setLevel(target, pkt.level);
        PlayerLevel.setXp(target, pkt.xp);
        if (pkt.className != null) {
            ProgressionClass cls = ProgressionClass.parse(pkt.className);
            if (cls != null) PlayerLevel.setProgressionClass(target, cls);
        }
        PlayerLevel.setFormStage(target, pkt.formStage);
    }
}