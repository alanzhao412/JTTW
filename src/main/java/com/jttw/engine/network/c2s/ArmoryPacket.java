package com.jttw.engine.network.c2s;

import com.jttw.engine.item.Armory;
import com.jttw.engine.item.ArmoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import com.jttw.engine.JTTWEngine;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Supplier;

/** Client → server: open the armory, or cycle the next weapon from it into hand. */
public class ArmoryPacket implements CustomPacketPayload {

    public static final Type<ArmoryPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "armory"));
    public static final StreamCodec<FriendlyByteBuf, ArmoryPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), ArmoryPacket::decode);

    @Override
    public Type<ArmoryPacket> type() {
        return TYPE;
    }

    public enum Action { OPEN, CYCLE }

    private final Action action;

    public ArmoryPacket(Action action) {
        this.action = action;
    }

    public static void encode(ArmoryPacket pkt, FriendlyByteBuf buf) {
        buf.writeEnum(pkt.action);
    }

    public static ArmoryPacket decode(FriendlyByteBuf buf) {
        return new ArmoryPacket(buf.readEnum(Action.class));
    }

    public static void handle(ArmoryPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer p = (ServerPlayer) ctx.player();
            if (p == null) return;
            if (pkt.action == Action.CYCLE) {
                Armory.cycle(p);
                return;
            }
            if (Armory.slots(p).isEmpty()) {
                p.displayClientMessage(Component.literal("§7You have no armaments."), true);
                return;
            }
            open(p);
        });
    }

    /** Opens the player's armory to them. */
    public static void open(ServerPlayer p) {
        String who = p.getPersistentData().getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);
        p.openMenu(new SimpleMenuProvider((id, inventory, player) -> new ArmoryMenu(id, inventory, Armory.of(p)),
                Component.literal("Armory")), buf -> buf.writeUtf(who));
    }
}
