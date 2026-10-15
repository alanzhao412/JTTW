package com.jttw.engine.network.c2s;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/**
 * Sent client → server when an ability key is pressed or released.
 *
 * <p>The server hands it to the player's {@link PlayableCharacter}, which decides what each
 * slot does. Menu slots (72 Transformations) open client-side and never send this packet.
 */
public class ActivateAbilityPacket implements CustomPacketPayload {

    public static final Type<ActivateAbilityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "activate_ability"));
    public static final StreamCodec<FriendlyByteBuf, ActivateAbilityPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ActivateAbilityPacket::decode);

    @Override
    public Type<ActivateAbilityPacket> type() {
        return TYPE;
    }

    private final int slot;
    private final AbilityAction action;

    public ActivateAbilityPacket(int slot, AbilityAction action) {
        this.slot   = slot;
        this.action = action;
    }

    public static void encode(ActivateAbilityPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.slot);
        buf.writeByte(pkt.action.ordinal());
    }

    public static ActivateAbilityPacket decode(FriendlyByteBuf buf) {
        int slot    = buf.readByte();
        int ordinal = buf.readByte();
        AbilityAction action = AbilityAction.values()[Math.min(ordinal, AbilityAction.values().length - 1)];
        return new ActivateAbilityPacket(slot, action);
    }

    public static void handle(ActivateAbilityPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            if (player == null) return;
            PlayableCharacter character = Characters.of(player);
            if (character != null && pkt.slot >= 0 && pkt.slot < 2 * PlayableCharacter.SLOT_COUNT) {
                character.activate(player, pkt.slot, pkt.action);
            }
        });
    }
}
