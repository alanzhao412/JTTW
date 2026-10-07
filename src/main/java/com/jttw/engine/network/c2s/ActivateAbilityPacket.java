package com.jttw.engine.network.c2s;

import com.jttw.engine.character.erlang.ErlangShenAbilityHandler;
import com.jttw.engine.character.wukong.WuCloneManager;
import com.jttw.engine.character.nezha.NezhaAbilityHandler;
import com.jttw.engine.character.wukong.WukongAbilityHandler;
import com.jttw.engine.character.erlang.XiaotianQuanManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent client → server to start or stop an ability activation.
 *
 * <p>Slot mapping for Wukong:
 * <ul>
 *   <li>Slot 0 (V key) — 72 Transformations.  The transform menu is opened client-side;
 *       this packet is NOT sent for that slot (no server notification needed on menu-open).</li>
 *   <li>Slot 1 (B key) — Shadow Clone. Tier 1/2: {@code USE} toggle.
 *       Tier 3: {@code START}/{@code STOP} held.</li>
 *   <li>Slot 2 (N key) — Ring of Fire.  Sent with {@code START} on key-down and
 *       {@code STOP} on key-up.</li>
 * </ul>
 *
 * <p>The server routes the packet to the appropriate handler based on the player's
 * subclass and slot mapping.
 */
public class ActivateAbilityPacket {

    public enum Action { USE, START, STOP }

    private final int slot;
    private final Action action;

    public ActivateAbilityPacket(int slot, Action action) {
        this.slot   = slot;
        this.action = action;
    }

    // -----------------------------------------------------------------------
    // Encode / Decode
    // -----------------------------------------------------------------------

    public static void encode(ActivateAbilityPacket pkt, FriendlyByteBuf buf) {
        buf.writeByte(pkt.slot);
        buf.writeByte(pkt.action.ordinal());
    }

    public static ActivateAbilityPacket decode(FriendlyByteBuf buf) {
        int slot    = buf.readByte();
        int ordinal = buf.readByte();
        Action action = Action.values()[Math.min(ordinal, Action.values().length - 1)];
        return new ActivateAbilityPacket(slot, action);
    }

    // -----------------------------------------------------------------------
    // Handle — runs on the server
    // -----------------------------------------------------------------------

    public static void handle(ActivateAbilityPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            routeToHandler(player, pkt.slot, pkt.action);
        });
        ctx.get().setPacketHandled(true);
    }

    private static void routeToHandler(ServerPlayer player, int slot, Action action) {
        // Primary: route by reincarnation outcome (set by the full reincarnation flow)
        String outcomeId = player.getPersistentData()
                .getString(com.jttw.engine.reincarnation.AlignmentData.KEY_REINCARNATION_OUTCOME);

        if ("celestial_primate".equals(outcomeId)) {
            handleWukong(player, slot, action);
            return;
        }

        if ("yuan_di".equals(outcomeId)) {
            handleYuanDi(player, slot, action);
            return;
        }

        if ("gold_winged_peng".equals(outcomeId)) {
            // TODO: handlePeng — no abilities yet
            return;
        }

        // Marshal of the Heavenly Canopy — 36 Transformations
        // Slot 0 is MENU type (transform GUI opened client-side, no server packet needed).
        // No other ability slots for Marshal yet.
        if ("marshal_of_the_heavenly_canopy".equals(outcomeId)) {
            handleMarshal(player, slot, action);
            return;
        }

        if ("third_lotus_prince".equals(outcomeId)) {
            handleNezha(player, slot, action);
            return;
        }

        // Fallback: route by ProgressionClass for players who set their class
        // via debug command without going through reincarnation (e.g. in testing)
        com.jttw.engine.progression.stats.ProgressionClass cls =
                com.jttw.engine.progression.level.PlayerLevel.getProgressionClass(player);
        if (cls == com.jttw.engine.progression.stats.ProgressionClass.PRIMORDIAL) {
            handleWukong(player, slot, action);
            return;
        }

        // Erlang Shen (Eye of Heaven outcome)
        if ("eye_of_heaven".equals(outcomeId)) {
            handleErlangShen(player, slot, action);
            return;
        }

        // Fallback: HEAVENLY class via debug command
        if (cls == com.jttw.engine.progression.stats.ProgressionClass.HEAVENLY) {
            handleErlangShen(player, slot, action);
            return;
        }

        // Dragon line (WATER Divine Beast)
        if ("dragon".equals(outcomeId)) {
            handleDragon(player, slot, action);
            return;
        }

        // Byakko line (BEAST Divine Beast)
        if ("white_tiger".equals(outcomeId)) {
            handleByakko(player, slot, action);
            return;
        }

        // Phoenix line (FIRE Divine Beast)
        if ("phoenix".equals(outcomeId)) {
            handlePhoenix(player, slot, action);
            return;
        }

        // Turtle line (EARTH Divine Beast)
        if ("turtle".equals(outcomeId)) {
            handleTurtle(player, slot, action);
            return;
        }

        // Fallback: DIVINE_BEAST class — route by beast line
        if (cls == com.jttw.engine.progression.stats.ProgressionClass.DIVINE_BEAST) {
            com.jttw.engine.progression.stats.DivineBeastLine line =
                    com.jttw.engine.progression.level.PlayerLevel.getBeastLine(player);
            if (line == com.jttw.engine.progression.stats.DivineBeastLine.WATER) {
                handleDragon(player, slot, action);
                return;
            }
            if (line == com.jttw.engine.progression.stats.DivineBeastLine.EARTH) {
                handleTurtle(player, slot, action);
                return;
            }
            if (line == com.jttw.engine.progression.stats.DivineBeastLine.BEAST) {
                handleByakko(player, slot, action);
                return;
            }
            if (line == com.jttw.engine.progression.stats.DivineBeastLine.FIRE) {
                handlePhoenix(player, slot, action);
                return;
            }
        }

        // TODO: add routing for YAOGUAI subclasses
    }

    private static void handleErlangShen(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 1 -> {
                // Xiaotian Quan — pure TOGGLE
                if (action == Action.USE) {
                    XiaotianQuanManager.toggle(player);
                }
            }
            case 2 -> {
                // Eye of Heaven — TOGGLE
                if (action == Action.USE) {
                    ErlangShenAbilityHandler.toggleEye(player);
                }
            }
        }
    }

    private static void handleTurtle(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 0 -> {
                // Shell Guard — HELD
                if (action == Action.START) {
                    com.jttw.engine.character.turtle.TurtleAbilityHandler.startGuard(player);
                } else if (action == Action.STOP) {
                    com.jttw.engine.character.turtle.TurtleAbilityHandler.stopGuard(player);
                }
            }
            case 1 -> {
                // Unstoppable Force — USE
                if (action == Action.USE) {
                    com.jttw.engine.character.turtle.TurtleAbilityHandler.castUnstoppableForce(player);
                }
            }
            case 2 -> {
                // Tectonic Slam — USE
                if (action == Action.USE) {
                    com.jttw.engine.character.turtle.TurtleAbilityHandler.castTectonicSlam(player);
                }
            }
            case 3 -> {
                // Gravity Well — TOGGLE
                if (action == Action.USE) {
                    com.jttw.engine.character.turtle.TurtleAbilityHandler.toggleGravityWell(player);
                }
            }
        }
    }

    private static void handleByakko(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 0 -> {
                // Predator's Leap — USE
                if (action == Action.USE) {
                    com.jttw.engine.character.byakko.ByakkoAbilityHandler.castLeap(player);
                }
            }
            case 1 -> {
                // Territorial Roar — USE
                if (action == Action.USE) {
                    com.jttw.engine.character.byakko.ByakkoAbilityHandler.castRoar(player);
                }
            }
        }
    }

    private static void handleDragon(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 0 -> {
                // Lightning Strike — USE
                if (action == Action.USE) {
                    com.jttw.engine.character.dragon.DragonAbilityHandler.castLightning(player);
                }
            }
            case 1 -> {
                // Weather Control — TOGGLE
                if (action == Action.USE) {
                    com.jttw.engine.character.dragon.DragonAbilityHandler.toggleWeather(player);
                }
            }
            case 2 -> {
                // Tidal Wave — USE (dr_wave tier >= 1), fallback to Dimension Travel
                if (action == Action.USE) {
                    int waveTier = com.jttw.engine.progression.skilltree.SkillTreeData.getDrWaveTier(player);
                    if (waveTier >= 1) {
                        com.jttw.engine.character.dragon.DragonAbilityHandler.castTidalWave(player);
                    } else {
                        com.jttw.engine.character.dragon.DragonAbilityHandler.travelDimension(player);
                    }
                }
            }
        }
    }

    private static void handlePhoenix(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 0 -> {
                // Flamethrower — HELD
                if (action == Action.START) {
                    com.jttw.engine.character.phoenix.PhoenixAbilityHandler.startFlame(player);
                } else if (action == Action.STOP) {
                    com.jttw.engine.character.phoenix.PhoenixAbilityHandler.stopFlame(player);
                }
            }
            case 1 -> {
                // Solar Radiance — TOGGLE
                if (action == Action.USE) {
                    com.jttw.engine.character.phoenix.PhoenixAbilityHandler.toggleRadiance(player);
                }
            }
        }
    }

    private static void handleYuanDi(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 0 -> {
                // Primordial Transformation — TOGGLE (T2/T3; T1 is auto)
                // Sovereign's Command fires innately on activation
                if (action == Action.USE) {
                    com.jttw.engine.character.yuandi.YuanDiAbilityHandler.toggleTransform(player);
                }
            }
            case 1 -> {
                // Demon's Breath — HELD
                if (action == Action.START) {
                    com.jttw.engine.character.yuandi.YuanDiAbilityHandler.startBreath(player);
                } else if (action == Action.STOP) {
                    com.jttw.engine.character.yuandi.YuanDiAbilityHandler.stopBreath(player);
                }
            }
            case 2 -> {
                // Primordial Smite — USE
                if (action == Action.USE) {
                    com.jttw.engine.character.yuandi.YuanDiAbilityHandler.usePrimordialSmite(player);
                }
            }
            case 3 -> {
                // Abyssal Gravity — USE
                if (action == Action.USE) {
                    com.jttw.engine.character.yuandi.YuanDiAbilityHandler.useAbyssalGravity(player);
                }
            }
        }
    }

    private static void handleMarshal(ServerPlayer player, int slot, Action action) {
        // Slot 0 — 36 Transformations: MENU type, opened client-side. No server action needed.
    }

    private static void handleNezha(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 0 -> {
                if (action == Action.USE) NezhaAbilityHandler.cycleWeapon(player);
            }
            case 1 -> {
                if (action == Action.USE) NezhaAbilityHandler.castSamadhiFire(player);
            }
            case 2 -> {
                if (action == Action.START) NezhaAbilityHandler.startDetonationCharge(player);
                if (action == Action.STOP) NezhaAbilityHandler.stopDetonationCharge(player);
            }
            case 3 -> {
                if (action == Action.USE) NezhaAbilityHandler.toggleThreeHeadsSixArms(player);
            }
        }
    }

    private static void handleWukong(ServerPlayer player, int slot, Action action) {
        switch (slot) {
            case 1 -> {
                // Shadow Clone — each press summons one more clone
                if (action == Action.USE) {
                    WuCloneManager.summon(player);
                }
            }
            case 2 -> {
                // Ring of Fire
                if (action == Action.START) {
                    WukongAbilityHandler.startRing(player);
                } else if (action == Action.STOP) {
                    WukongAbilityHandler.stopRing(player);
                }
            }
            case 3 -> {
                // Fiery Golden Eyes (slot 3 / M key)
                if (action == Action.USE) {
                    // Check if player is sneaking: shift = toggle xray, no shift = eye laser
                    if (player.isShiftKeyDown()) {
                        WukongAbilityHandler.toggleGoldenEyesXray(player);
                    } else {
                        WukongAbilityHandler.fireEyeLaser(player);
                    }
                }
            }
        }
    }
}