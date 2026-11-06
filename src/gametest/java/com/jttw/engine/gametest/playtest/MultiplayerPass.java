package com.jttw.engine.gametest.playtest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.client.ClientAbilityData;
import com.jttw.engine.client.ClientInit;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.SpendSkillPointPacket;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.SkillNode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A multiplayer pass in a real client joined to a real dedicated server ({@code ./gradlew runMultiplayerPass},
 * the server started apart, on {@code localhost:25599}, with this client's player an operator): what no
 * GameTest and no single-player pass can see, the mod across the wire. For each character it is made
 * that character by command, buys its whole tree with the packets the tree screen sends, checks what the
 * server synced back to the client (class, level, ability bar), presses each ability key at a zombie,
 * transforms and reverts, and takes a picture. Every message the server sends and any disconnect go in
 * {@code run-mp/mp-pass-<time>.txt}; then it quits.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class MultiplayerPass {

    private MultiplayerPass() {}

    private static final boolean ENABLED = Boolean.getBoolean("jttw.mpPass") && !StressPass.active() && !GeneralStress.active() && !PersistPass.active();   // the stress roles are StressPass's
    /** {@code observer}: a second player who only watches the first, logging what the server shows it of him. */
    private static final boolean OBSERVER = "observer".equals(System.getProperty("jttw.mpPass.role"));
    private static final String RUN = "mp-pass-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());

    private record Action(int at, String name, Runnable run) {}

    private static final List<String> report = Collections.synchronizedList(new ArrayList<>());
    private static List<Action> plan;
    private static int tick, next, settle;
    private static boolean joined, done;

    private static void log(String line) {
        report.add(line);
        JTTWEngine.LOGGER.info("[mp-pass] {}", line);
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent e) {
        if (ENABLED && !done) log("  server says: " + e.getMessage().getString().replace('\n', ' '));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        if (ENABLED && joined && !done) log("DISCONNECTED before the pass finished");
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!ENABLED || done) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DisconnectedScreen d) {
            log("DISCONNECTED: " + d.getTitle().getString());
            finish(mc);
            return;
        }
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        if (!joined) {
            joined = true;
            log("joined " + mc.getConnection().getServerData() + " as " + mc.player.getGameProfile().getName());
        }
        if (OBSERVER) { observe(mc); return; }
        if (settle < 100) { settle++; return; }
        if (plan == null) plan = plan();
        while (next < plan.size() && plan.get(next).at <= tick) {
            Action a = plan.get(next++);
            try {
                a.run.run();
            } catch (Throwable t) {
                log("FAILED " + a.name + ": " + t);
            }
        }
        tick++;
        if (next >= plan.size()) finish(mc);
    }

    private static void cmd(String c) {
        log("> /" + c);
        Minecraft.getInstance().getConnection().sendCommand(c);
    }

    private static void press(KeyMapping key, int holdTicks, List<Action> out, int at) {
        out.add(new Action(at, "press " + key.getName(), () -> {
            closeScreen();
            KeyMapping.click(key.getKey());
            key.setDown(true);
        }));
        out.add(new Action(at + holdTicks, "release " + key.getName(), () -> {
            key.setDown(false);
            closeScreen();   // a menu slot (the 72 Transformations) opens a screen: noted, and shut
        }));
    }

    private static List<Action> plan() {
        List<Action> out = new ArrayList<>();
        int t = 0;
        out.add(new Action(t, "setup", () -> {
            cmd("gamemode survival");
            cmd("time set day");
            cmd("weather clear");
            cmd("effect give @s minecraft:resistance infinite 4 true");
        }));
        t += 20;
        KeyMapping[] slots = { ClientInit.ABILITY_SLOT_0, ClientInit.ABILITY_SLOT_1, ClientInit.ABILITY_SLOT_2, ClientInit.ABILITY_SLOT_3 };
        for (PlayableCharacter c : Characters.all()) {
            String id = c.outcomeId();
            out.add(new Action(t, "become " + id, () -> {
                log("== " + id + " ==");
                cmd("execute at @s run tp Tester2 ^ ^ ^7 facing entity @s eyes");
                cmd("jttw demorph");
                cmd("kill @e[type=!minecraft:player,distance=..40]");
                cmd("effect clear @s");
                cmd("effect give @s minecraft:resistance infinite 4 true");
                cmd("jttw setclass any " + id);
                cmd("jttw_setlevel 60");
                cmd("jttw_addsp 1000");
            }));
            t += 30;
            for (int pass = 0; pass < 4; pass++) {   // a node's prerequisites bought on an earlier pass
                out.add(new Action(t, "buy " + id + " tree", () -> {
                    for (SkillNode n : c.skillTree().allOrdered()) NetworkHandler.sendToServer(new SpendSkillPointPacket(n.id()));
                }));
                t += 15;
            }
            out.add(new Action(t, "check sync " + id, () -> {
                Minecraft mc = Minecraft.getInstance();
                log("  client sees: class " + PlayerLevel.getProgressionClass(mc.player) + ", level " + PlayerLevel.getLevel(mc.player)
                        + ", " + ClientAbilityData.totalCount() + " ability slots");
                if (PlayerLevel.getLevel(mc.player) != 60) log("  MISMATCH: level not synced to 60");
                if (PlayerLevel.getProgressionClass(mc.player) == null) log("  MISMATCH: class not synced");
                if (ClientAbilityData.totalCount() == 0) log("  MISMATCH: no ability bar synced");
                cmd("summon minecraft:zombie ^ ^ ^4");
            }));
            t += 20;
            for (KeyMapping key : slots) {
                press(key, 25, out, t);
                t += 50;
            }
            out.add(new Action(t, "shot " + id, () -> { closeScreen(); shot(id); }));
            t += 10;
            out.add(new Action(t, "transform " + id, () -> cmd("jttw transform minecraft:wolf")));
            t += 40;
            out.add(new Action(t, "demorph " + id, () -> cmd("jttw demorph")));
            t += 30;
        }
        out.add(new Action(t, "end", () -> log("== pass complete ==")));
        return out;
    }

    private static String lastSeen = "";
    private static boolean sawOther;
    private static int alone;

    /** The observer's tick: what it sees of every other player, logged as it changes, and a picture every 10 s. */
    private static void observe(Minecraft mc) {
        closeScreen();
        tick++;
        List<String> seen = new ArrayList<>();
        for (var other : mc.level.players()) {
            if (other == mc.player) continue;
            var form = com.jttw.engine.transformation.TransformationManager.getTransformation(other);
            seen.add(other.getGameProfile().getName() + (form == null ? " (as himself)" : " as " + form.getFormId())
                    + (other.isInvisible() ? ", invisible" : "") + (other.getAbilities().flying ? ", flying" : ""));
        }
        long near = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(16)).size();
        String now = String.join("; ", seen) + " | " + near + " entities within 16";
        if (!now.equals(lastSeen)) {
            log("t" + tick + " sees: " + (seen.isEmpty() ? "no other player" : now));
            lastSeen = now;
        }
        if (!seen.isEmpty()) { sawOther = true; alone = 0; } else alone++;
        if (tick % 200 == 0 && sawOther) shot("observer-" + tick);
        if ((sawOther && alone > 200) || tick > 20 * 60 * 15) {   // he has gone, or a quarter of an hour
            log("== observer done ==");
            finish(mc);
        }
    }

    private static void closeScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen && mc.player != null) {
            log("  DIED; respawning");
            mc.player.respawn();
            mc.setScreen(null);
        } else if (mc.screen != null && !(mc.screen instanceof DisconnectedScreen)) {
            log("  a screen opened: " + mc.screen.getClass().getSimpleName());
            mc.setScreen(null);
        }
    }

    private static void shot(String id) {
        Minecraft mc = Minecraft.getInstance();
        File dir = new File(mc.gameDirectory, "screenshots/" + RUN);
        dir.mkdirs();
        Screenshot.grab(dir, id + ".png", mc.getMainRenderTarget(), msg -> {});
    }

    private static void finish(Minecraft mc) {
        done = true;
        File out = new File(mc.gameDirectory, RUN + ".txt");
        try {
            Files.write(out.toPath(), new ArrayList<>(report));
        } catch (IOException ex) {
            JTTWEngine.LOGGER.error("[mp-pass] could not write the report", ex);
        }
        mc.stop();
    }
}
