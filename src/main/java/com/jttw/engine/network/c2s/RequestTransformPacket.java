package com.jttw.engine.network.c2s;

import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.transformation.FormCategory;
import com.jttw.engine.transformation.FormClassifier;
import com.jttw.engine.transformation.TransformationAccess;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Sent client → server when the player picks a form in their transformation menu.
 *
 * <p>The server re-checks the choice against the player's transformation art and level
 * ({@link TransformationAccess}); the menu only shows what passes the same check.
 */
public class RequestTransformPacket {

    public enum Kind {
        /** A mob (living entity). */
        ENTITY,
        /** A non-living entity: minecart, boat, arrow... */
        OBJECT_ENTITY,
        /** Another online player's appearance. */
        PLAYER
    }

    private final Kind kind;
    @Nullable private final ResourceLocation entityId;
    @Nullable private final UUID targetPlayer;

    public RequestTransformPacket(Kind kind, ResourceLocation entityId) {
        this(kind, entityId, null);
    }

    private RequestTransformPacket(Kind kind, @Nullable ResourceLocation entityId, @Nullable UUID targetPlayer) {
        this.kind = kind;
        this.entityId = entityId;
        this.targetPlayer = targetPlayer;
    }

    /** A request to take on another player's appearance. */
    public static RequestTransformPacket player(UUID target) {
        return new RequestTransformPacket(Kind.PLAYER, null, target);
    }

    public static void encode(RequestTransformPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.kind.ordinal());
        if (msg.kind == Kind.PLAYER) buf.writeUUID(msg.targetPlayer);
        else buf.writeResourceLocation(msg.entityId);
    }

    public static RequestTransformPacket decode(FriendlyByteBuf buf) {
        int ord = buf.readVarInt();
        Kind kind = (ord >= 0 && ord < Kind.values().length) ? Kind.values()[ord] : Kind.ENTITY;
        if (kind == Kind.PLAYER) return player(buf.readUUID());
        return new RequestTransformPacket(kind, buf.readResourceLocation());
    }

    public static void handle(RequestTransformPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ServerPlayer sender = ctx.getSender();
        if (sender != null) ctx.enqueueWork(() -> handleServer(sender, msg));
        ctx.setPacketHandled(true);
    }

    private static void handleServer(ServerPlayer sender, RequestTransformPacket msg) {
        var unlocked = SkillTreeData.getUnlockedNodes(sender);

        if (msg.kind == Kind.PLAYER) {
            ServerPlayer target = sender.server.getPlayerList().getPlayer(msg.targetPlayer);
            if (target == null || target == sender) return;
            if (!TransformationAccess.mayTake(sender, unlocked, FormCategory.PLAYER)) return;
            TransformationManager.transformIntoPlayer(sender, target.getUUID(), target.getGameProfile().getName());
            return;
        }

        // The registry returns its default (pig) for unknown ids rather than null
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(msg.entityId)) return;
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(msg.entityId);
        if (type == null || type == EntityType.PLAYER) return; // players go through Kind.PLAYER

        Entity created = type.create(sender.serverLevel());
        if (created == null) return;
        FormCategory category = FormClassifier.classify(type, created);
        created.discard();

        boolean isObject = category == FormCategory.OBJECT;
        if ((msg.kind == Kind.OBJECT_ENTITY) != isObject) return; // asked for a mob, got an object, or vice versa
        if (!TransformationAccess.mayTake(sender, unlocked, category)) return;

        TransformationManager.transform(sender, type);
    }
}
