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
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = JTTWEngine.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    private NetworkHandler() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(SyncTransformationPacket.TYPE, SyncTransformationPacket.STREAM_CODEC, SyncTransformationPacket::handle);
        registrar.playToClient(OpenTransformationMenuPacket.TYPE, OpenTransformationMenuPacket.STREAM_CODEC, OpenTransformationMenuPacket::handle);
        registrar.playToClient(SyncLevelPacket.TYPE, SyncLevelPacket.STREAM_CODEC, SyncLevelPacket::handle);
        registrar.playToClient(SyncAbilitiesPacket.TYPE, SyncAbilitiesPacket.STREAM_CODEC, SyncAbilitiesPacket::handle);
        registrar.playToClient(SyncCooldownPacket.TYPE, SyncCooldownPacket.STREAM_CODEC, SyncCooldownPacket::handle);
        registrar.playToClient(SyncSkillTreePacket.TYPE, SyncSkillTreePacket.STREAM_CODEC, SyncSkillTreePacket::handle);
        registrar.playToClient(SyncQiPacket.TYPE, SyncQiPacket.STREAM_CODEC, SyncQiPacket::handle);
        registrar.playToClient(SyncReincarnationPacket.TYPE, SyncReincarnationPacket.STREAM_CODEC, SyncReincarnationPacket::handle);
        registrar.playToClient(SyncRingStatePacket.TYPE, SyncRingStatePacket.STREAM_CODEC, SyncRingStatePacket::handle);
        registrar.playToClient(SyncEyeOfHeavenPacket.TYPE, SyncEyeOfHeavenPacket.STREAM_CODEC, SyncEyeOfHeavenPacket::handle);
        registrar.playToClient(SyncTransformStatePacket.TYPE, SyncTransformStatePacket.STREAM_CODEC, SyncTransformStatePacket::handle);
        registrar.playToClient(SyncReviveCountPacket.TYPE, SyncReviveCountPacket.STREAM_CODEC, SyncReviveCountPacket::handle);
        registrar.playToClient(SyncXtqStatsPacket.TYPE, SyncXtqStatsPacket.STREAM_CODEC, SyncXtqStatsPacket::handle);
        registrar.playToServer(SpendSkillPointPacket.TYPE, SpendSkillPointPacket.STREAM_CODEC, SpendSkillPointPacket::handle);
        registrar.playToServer(com.jttw.engine.network.c2s.BuyClassAbilityPacket.TYPE, com.jttw.engine.network.c2s.BuyClassAbilityPacket.STREAM_CODEC, com.jttw.engine.network.c2s.BuyClassAbilityPacket::handle);
        registrar.playToServer(ActivateAbilityPacket.TYPE, ActivateAbilityPacket.STREAM_CODEC, ActivateAbilityPacket::handle);
        registrar.playToServer(RequestTransformPacket.TYPE, RequestTransformPacket.STREAM_CODEC, RequestTransformPacket::handle);
        registrar.playToServer(UseFormPacket.TYPE, UseFormPacket.STREAM_CODEC, UseFormPacket::handle);
        registrar.playToServer(SetSignTextPacket.TYPE, SetSignTextPacket.STREAM_CODEC, SetSignTextPacket::handle);
        registrar.playToServer(StaffScalePacket.TYPE, StaffScalePacket.STREAM_CODEC, StaffScalePacket::handle);
        registrar.playToServer(com.jttw.engine.network.c2s.StaffTopplePacket.TYPE, com.jttw.engine.network.c2s.StaffTopplePacket.STREAM_CODEC, com.jttw.engine.network.c2s.StaffTopplePacket::handle);
        registrar.playToServer(DimensionSelectPacket.TYPE, DimensionSelectPacket.STREAM_CODEC, DimensionSelectPacket::handle);
        registrar.playToServer(TransmuteItemPacket.TYPE, TransmuteItemPacket.STREAM_CODEC, TransmuteItemPacket::handle);
        registrar.playToClient(SyncLeadHeadPacket.TYPE, SyncLeadHeadPacket.STREAM_CODEC, SyncLeadHeadPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.SyncStomachPacket.TYPE, com.jttw.engine.network.s2c.SyncStomachPacket.STREAM_CODEC, com.jttw.engine.network.s2c.SyncStomachPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.SyncFlightPacket.TYPE, com.jttw.engine.network.s2c.SyncFlightPacket.STREAM_CODEC, com.jttw.engine.network.s2c.SyncFlightPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.AbilityNotePacket.TYPE, com.jttw.engine.network.s2c.AbilityNotePacket.STREAM_CODEC, com.jttw.engine.network.s2c.AbilityNotePacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.PrivateGlowPacket.TYPE, com.jttw.engine.network.s2c.PrivateGlowPacket.STREAM_CODEC, com.jttw.engine.network.s2c.PrivateGlowPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.TruffleScentPacket.TYPE, com.jttw.engine.network.s2c.TruffleScentPacket.STREAM_CODEC, com.jttw.engine.network.s2c.TruffleScentPacket::handle);
        registrar.playToClient(SyncGoldenEyesPacket.TYPE, SyncGoldenEyesPacket.STREAM_CODEC, SyncGoldenEyesPacket::handle);
        registrar.playToServer(com.jttw.engine.network.c2s.ArmoryPacket.TYPE, com.jttw.engine.network.c2s.ArmoryPacket.STREAM_CODEC, com.jttw.engine.network.c2s.ArmoryPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.FlyingStatePacket.TYPE, com.jttw.engine.network.s2c.FlyingStatePacket.STREAM_CODEC, com.jttw.engine.network.s2c.FlyingStatePacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.SyncKarmaPacket.TYPE, com.jttw.engine.network.s2c.SyncKarmaPacket.STREAM_CODEC, com.jttw.engine.network.s2c.SyncKarmaPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.ArmamentsShownPacket.TYPE, com.jttw.engine.network.s2c.ArmamentsShownPacket.STREAM_CODEC, com.jttw.engine.network.s2c.ArmamentsShownPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.BeastFormPacket.TYPE, com.jttw.engine.network.s2c.BeastFormPacket.STREAM_CODEC, com.jttw.engine.network.s2c.BeastFormPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.QiSeenPacket.TYPE, com.jttw.engine.network.s2c.QiSeenPacket.STREAM_CODEC, com.jttw.engine.network.s2c.QiSeenPacket::handle);
        registrar.playToClient(com.jttw.engine.network.s2c.WaterFxPacket.TYPE, com.jttw.engine.network.s2c.WaterFxPacket.STREAM_CODEC, com.jttw.engine.network.s2c.WaterFxPacket::handle);
    }

    public static void sendToServer(CustomPacketPayload msg) {
        PacketDistributor.sendToServer(msg);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload msg) {
        PacketDistributor.sendToPlayer(player, msg);
    }

    /** To every player within {@code radius} blocks of {@code at} in that world. */
    public static void sendToNear(net.minecraft.server.level.ServerLevel level, net.minecraft.world.phys.Vec3 at, double radius, CustomPacketPayload msg) {
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, radius, msg);
    }

    /** To every player tracking {@code player} (not the player themselves). */
    public static void sendToTrackingPlayers(ServerPlayer player, CustomPacketPayload msg) {
        PacketDistributor.sendToPlayersTrackingEntity(player, msg);
    }
}