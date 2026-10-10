package com.jttw.engine.network.s2c;

import com.jttw.engine.character.AbilitySlot;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.progression.skilltree.AbilityType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Syncs a player's skill bar (slots V, B, N, M) from server to client, so the HUD and the
 * key handlers know what each slot holds and how it activates.
 *
 * <p>Sent on login and whenever a skill node is unlocked.
 */
public class SyncAbilitiesPacket {

    private final UUID playerId;
    private final List<AbilitySlot> abilities;

    public SyncAbilitiesPacket(UUID playerId, List<AbilitySlot> abilities) {
        this.playerId  = playerId;
        this.abilities = List.copyOf(abilities);
    }

    public static void encode(SyncAbilitiesPacket pkt, FriendlyByteBuf buf) {
        buf.writeUUID(pkt.playerId);
        buf.writeInt(pkt.abilities.size());
        for (AbilitySlot e : pkt.abilities) {
            buf.writeUtf(e.id());
            buf.writeUtf(e.displayName());
            buf.writeByte(e.type().ordinal());
            buf.writeByte(e.tier());
        }
    }

    public static SyncAbilitiesPacket decode(FriendlyByteBuf buf) {
        UUID id    = buf.readUUID();
        int count  = buf.readInt();
        List<AbilitySlot> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String abilityId   = buf.readUtf();
            String displayName = buf.readUtf();
            int typeOrdinal    = buf.readByte();
            int tier           = buf.readByte();
            AbilityType type   = AbilityType.values()[Math.min(typeOrdinal, AbilityType.values().length - 1)];
            list.add(new AbilitySlot(abilityId, displayName, type, tier));
        }
        return new SyncAbilitiesPacket(id, list);
    }

    public static void handle(SyncAbilitiesPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleClient(pkt));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void handleClient(SyncAbilitiesPacket pkt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!mc.player.getUUID().equals(pkt.playerId)) return;
        com.jttw.engine.client.ClientAbilityData.setAbilities(pkt.abilities);
    }

    /** The player's current skill bar, from their character and unlocked nodes. */
    public static SyncAbilitiesPacket build(ServerPlayer player) {
        PlayableCharacter character = Characters.of(player);
        List<AbilitySlot> bar = character == null
                ? PlayableCharacter.emptyBar()
                : character.fullBar(player);
        return new SyncAbilitiesPacket(player.getUUID(), bar);
    }
}
