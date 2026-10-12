package com.jttw.engine.network.c2s;

import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server when an ability key is pressed or released.
 *
 * <p>The server hands it to the player's {@link PlayableCharacter}, which decides what each
 * slot does. Menu slots (72 Transformations) open client-side and never send this packet.
 */
public class ActivateAbilityPacket {

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

    public static void handle(ActivateAbilityPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            PlayableCharacter character = Characters.of(player);
            // exhausted after a true form, no ability answers (but the form's own key may end it)
            if (com.jttw.engine.character.divinebeast.TrueForm.exhausted(player)) return;
            if (character != null && pkt.slot >= 0 && pkt.slot < 2 * PlayableCharacter.SLOT_COUNT) {
                character.activate(player, pkt.slot, pkt.action);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
