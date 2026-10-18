package com.jttw.engine.network.c2s;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.progression.skilltree.SkillPurchase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent client → server when the player buys a class ability on the character screen's Skills tab.
 *
 * <p>The server hands it to {@link SkillPurchase#purchaseClassAbility}, which validates it.
 */
public class BuyClassAbilityPacket implements CustomPacketPayload {

    public static final Type<BuyClassAbilityPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "buy_class_ability"));
    public static final StreamCodec<FriendlyByteBuf, BuyClassAbilityPacket> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), BuyClassAbilityPacket::decode);

    @Override
    public Type<BuyClassAbilityPacket> type() {
        return TYPE;
    }

    private final String abilityId;

    public BuyClassAbilityPacket(String abilityId) {
        this.abilityId = abilityId;
    }

    public static void encode(BuyClassAbilityPacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.abilityId);
    }

    public static BuyClassAbilityPacket decode(FriendlyByteBuf buf) {
        return new BuyClassAbilityPacket(buf.readUtf());
    }

    public static void handle(BuyClassAbilityPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) SkillPurchase.purchaseClassAbility(player, pkt.abilityId);
        });
    }
}
