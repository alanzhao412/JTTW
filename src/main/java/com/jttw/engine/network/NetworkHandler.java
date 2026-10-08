package com.jttw.engine.network;

import com.jttw.engine.network.c2s.ActivateAbilityPacket;
import com.jttw.engine.network.c2s.DimensionSelectPacket;
import com.jttw.engine.network.c2s.RequestTransformPacket;
import com.jttw.engine.network.c2s.SetSignTextPacket;
import com.jttw.engine.network.c2s.UseFormPacket;
import com.jttw.engine.network.c2s.SpendSkillPointPacket;
import com.jttw.engine.network.c2s.StaffScalePacket;
import com.jttw.engine.network.s2c.OpenTransformationMenuPacket;
import com.jttw.engine.network.s2c.SyncAbilitiesPacket;
import com.jttw.engine.network.s2c.SyncCooldownPacket;
import com.jttw.engine.network.s2c.SyncEyeOfHeavenPacket;
import com.jttw.engine.network.s2c.SyncGoldenEyesPacket;
import com.jttw.engine.network.s2c.SyncLeadHeadPacket;
import com.jttw.engine.network.s2c.SyncLevelPacket;
import com.jttw.engine.network.s2c.SyncQiPacket;
import com.jttw.engine.network.s2c.SyncReviveCountPacket;
import com.jttw.engine.network.s2c.SyncRingStatePacket;
import com.jttw.engine.network.s2c.SyncSkillTreePacket;
import com.jttw.engine.network.s2c.SyncTransformStatePacket;
import com.jttw.engine.network.s2c.SyncTransformationPacket;
import com.jttw.engine.network.s2c.SyncXtqStatsPacket;
import com.jttw.engine.JTTWEngine;
import com.jttw.engine.network.s2c.SyncReincarnationPacket;
import com.jttw.engine.network.c2s.TransmuteItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
        .named(ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, "main"))
        .networkProtocolVersion(() -> PROTOCOL_VERSION)
        .clientAcceptedVersions(PROTOCOL_VERSION::equals)
        .serverAcceptedVersions(PROTOCOL_VERSION::equals)
        .simpleChannel();

    private static int id = 0;

    private NetworkHandler() {}

    public static void register() {
        // PLAY_TO_CLIENT
        CHANNEL.messageBuilder(SyncTransformationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTransformationPacket::encode)
                .decoder(SyncTransformationPacket::decode)
                .consumerMainThread(SyncTransformationPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenTransformationMenuPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenTransformationMenuPacket::encode)
                .decoder(OpenTransformationMenuPacket::decode)
                .consumerMainThread(OpenTransformationMenuPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncLevelPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLevelPacket::encode)
                .decoder(SyncLevelPacket::decode)
                .consumerMainThread(SyncLevelPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncAbilitiesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncAbilitiesPacket::encode)
                .decoder(SyncAbilitiesPacket::decode)
                .consumerMainThread(SyncAbilitiesPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncCooldownPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCooldownPacket::encode)
                .decoder(SyncCooldownPacket::decode)
                .consumerMainThread(SyncCooldownPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncSkillTreePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncSkillTreePacket::encode)
                .decoder(SyncSkillTreePacket::decode)
                .consumerMainThread(SyncSkillTreePacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncQiPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncQiPacket::encode)
                .decoder(SyncQiPacket::decode)
                .consumerMainThread(SyncQiPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncReincarnationPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncReincarnationPacket::encode)
                .decoder(SyncReincarnationPacket::decode)
                .consumerMainThread(SyncReincarnationPacket::handle)
                .add();

        // ✅ NEW: ring visual state sync (server → client)
        CHANNEL.messageBuilder(SyncRingStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncRingStatePacket::encode)
                .decoder(SyncRingStatePacket::decode)
                .consumerMainThread(SyncRingStatePacket::handle)
                .add();

        // Eye of Heaven state sync (server → client)
        CHANNEL.messageBuilder(SyncEyeOfHeavenPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncEyeOfHeavenPacket::encode)
                .decoder(SyncEyeOfHeavenPacket::decode)
                .consumerMainThread(SyncEyeOfHeavenPacket::handle)
                .add();

        // Yuan Di transformation state (server → client)
        CHANNEL.messageBuilder(SyncTransformStatePacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncTransformStatePacket::encode)
                .decoder(SyncTransformStatePacket::decode)
                .consumerMainThread(SyncTransformStatePacket::handle)
                .add();

        // Phoenix revive counter (server → client)
        CHANNEL.messageBuilder(SyncReviveCountPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncReviveCountPacket::encode)
                .decoder(SyncReviveCountPacket::decode)
                .consumerMainThread(SyncReviveCountPacket::handle)
                .add();

        // Xiaotian Quan stats (server → client)
        CHANNEL.messageBuilder(SyncXtqStatsPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncXtqStatsPacket::encode)
                .decoder(SyncXtqStatsPacket::decode)
                .consumerMainThread(SyncXtqStatsPacket::handle)
                .add();

        // PLAY_TO_SERVER
        CHANNEL.messageBuilder(SpendSkillPointPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SpendSkillPointPacket::encode)
                .decoder(SpendSkillPointPacket::decode)
                .consumerMainThread(SpendSkillPointPacket::handle)
                .add();

        CHANNEL.messageBuilder(ActivateAbilityPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ActivateAbilityPacket::encode)
                .decoder(ActivateAbilityPacket::decode)
                .consumerMainThread(ActivateAbilityPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestTransformPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestTransformPacket::encode)
                .decoder(RequestTransformPacket::decode)
                .consumerMainThread(RequestTransformPacket::handle)
                .add();

        CHANNEL.messageBuilder(UseFormPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UseFormPacket::encode)
                .decoder(UseFormPacket::decode)
                .consumerMainThread(UseFormPacket::handle)
                .add();

        CHANNEL.messageBuilder(SetSignTextPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetSignTextPacket::encode)
                .decoder(SetSignTextPacket::decode)
                .consumerMainThread(SetSignTextPacket::handle)
                .add();

        CHANNEL.messageBuilder(StaffScalePacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(StaffScalePacket::encode)
                .decoder(StaffScalePacket::decode)
                .consumerMainThread(StaffScalePacket::handle)
                .add();

        CHANNEL.messageBuilder(DimensionSelectPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DimensionSelectPacket::encode)
                .decoder(DimensionSelectPacket::decode)
                .consumerMainThread(DimensionSelectPacket::handle)
                .add();

        // T3 transmutation (client → server)
        CHANNEL.messageBuilder(TransmuteItemPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(TransmuteItemPacket::encode)
                .decoder(TransmuteItemPacket::decode)
                .consumerMainThread(TransmuteItemPacket::handle)
                .add();

        // Fiery Golden Eyes mode (passive / x-ray) for the eye renderer
        CHANNEL.messageBuilder(SyncLeadHeadPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncLeadHeadPacket::encode)
                .decoder(SyncLeadHeadPacket::decode)
                .consumerMainThread(SyncLeadHeadPacket::handle)
                .add();

        CHANNEL.messageBuilder(com.jttw.engine.network.s2c.SyncStomachPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.jttw.engine.network.s2c.SyncStomachPacket::encode)
                .decoder(com.jttw.engine.network.s2c.SyncStomachPacket::decode)
                .consumerMainThread(com.jttw.engine.network.s2c.SyncStomachPacket::handle)
                .add();

        CHANNEL.messageBuilder(com.jttw.engine.network.s2c.TruffleScentPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(com.jttw.engine.network.s2c.TruffleScentPacket::encode)
                .decoder(com.jttw.engine.network.s2c.TruffleScentPacket::decode)
                .consumerMainThread(com.jttw.engine.network.s2c.TruffleScentPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncGoldenEyesPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncGoldenEyesPacket::encode)
                .decoder(SyncGoldenEyesPacket::decode)
                .consumerMainThread(SyncGoldenEyesPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToTrackingPlayers(ServerPlayer player, Object msg) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> player), msg);
    }
}