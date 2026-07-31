package com.jttw.engine.network;

import com.jttw.engine.progression.SkillTreeData;
import com.jttw.engine.transformation.TransformTierFilter;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class RequestTransformPacket {

    public enum Kind {
        ENTITY,          // living entities (your current behavior)
        OBJECT_ENTITY    // non-living entities (arrows, minecarts, items, etc.)
    }

    private final Kind kind;
    private final ResourceLocation entityId;

    public RequestTransformPacket(Kind kind, ResourceLocation entityId) {
        this.kind = kind;
        this.entityId = entityId;
    }

    public static void encode(RequestTransformPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.kind.ordinal());
        buf.writeResourceLocation(msg.entityId);
    }

    public static RequestTransformPacket decode(FriendlyByteBuf buf) {
        int ord = buf.readVarInt();
        Kind kind = (ord >= 0 && ord < Kind.values().length) ? Kind.values()[ord] : Kind.ENTITY;
        return new RequestTransformPacket(kind, buf.readResourceLocation());
    }

    public static void handle(RequestTransformPacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        final ServerPlayer sender = ctx.getSender();
        if (sender == null) {
            ctx.setPacketHandled(true);
            return;
        }

        ctx.enqueueWork(() -> {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(msg.entityId);
            if (type == null) return;

            // Never allow player
            if (type == EntityType.PLAYER) return;

            Entity created = type.create(sender.serverLevel());
            if (created == null) return;

            boolean isLiving = (created instanceof LivingEntity);

            // Validate based on requested kind
            if (msg.kind == Kind.ENTITY) {
                if (!isLiving) return;

                // Marshal of the Heavenly Canopy uses a separate filter (36 Transformations)
                String outcomeId = sender.getPersistentData()
                        .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
                boolean isMarshal = "marshal_of_the_heavenly_canopy".equals(outcomeId);

                if (isMarshal) {
                    // Size filter only for 36 Transformations
                    if (TransformTierFilter.isTooSmall(type)) return;
                    if (!TransformTierFilter.isAllowedForMarshal(type)) {
                        return;
                    }
                } else {
                    // Server-side tier validation for entity transforms
                    int transformTier = Math.max(
                            SkillTreeData.getTransformTier(sender),
                            SkillTreeData.getEsTransformTier(sender)
                    );
                    if (!TransformTierFilter.isAllowedAtTier(type, transformTier)) {
                        return;
                    }
                }
            } else if (msg.kind == Kind.OBJECT_ENTITY) {
                if (isLiving) return;
            }

            // NOTE: this next call currently assumes “living morph” in your TransformationManager.
            // We’ll update TransformationManager/TransformationData/PlayerRendererMixin next so non-living works properly.
            TransformationManager.transform(sender, type);
        });

        ctx.setPacketHandled(true);
    }
}