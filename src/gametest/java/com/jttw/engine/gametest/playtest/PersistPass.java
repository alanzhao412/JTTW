package com.jttw.engine.gametest.playtest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.client.ClientInit;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.GreatVoidPacket;
import com.jttw.engine.network.c2s.SpendSkillPointPacket;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.progression.skilltree.SkillNode;
import com.jttw.engine.transformation.TransformationManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Does a player come back as they left, through a logout in an odd state and a server restart?
 * Two sessions on a real dedicated server, two players each ({@code -Pmp.role=persist-a1} and
 * {@code -Pmp.role2=persist-b1}, then the server restarted, then {@code persist-a2} and {@code persist-b2}):
 * <ul>
 *   <li><b>a</b> (Tester1): Wukong, level 80, his tree bought, his clones out and in a wolf's form,
 *       when the server is stopped under him ({@code /stop}, which saves);</li>
 *   <li><b>b</b> (Tester2): the Peng, in a form in Hell, then into the Great Void from there, and
 *       logs out in it before the restart.</li>
 * </ul>
 * Before and after, each takes down what the server holds of them ({@code /data get entity}: the
 * mod's data, the inventory, the position and realm) and what their client shows; the server's
 * count of the mod's things left in the world (clones, summons) is taken too. Everything goes in
 * {@code <run dir>/persist-<role>.txt}; the sessions' snapshots are compared afterwards.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class PersistPass {

    private PersistPass() {}

    static final String ROLE = Boolean.getBoolean("jttw.mpPass") ? System.getProperty("jttw.mpPass.role", "") : "";
    static boolean active() { return ROLE.startsWith("persist-"); }
    private static final boolean A = ROLE.startsWith("persist-a"), SECOND = ROLE.endsWith("2");

    private static final List<String> report = Collections.synchronizedList(new ArrayList<>());
    private static int tick, settle;
    private static boolean joined, done, partnerDone;

    private static void log(String line) {
        report.add("[" + tick + "] " + line);
        JTTWEngine.LOGGER.info("[persist] {}", line);
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent e) {
        if (!active() || done) return;
        String m = e.getMessage().getString().replace('\n', ' ');
        if (m.contains("B_LOGGING_OUT")) partnerDone = true;
        if (m.contains("Summoned new") || m.contains("Applied effect")) return;
        log("  server: " + m);
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!active() || done) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DisconnectedScreen d) {
            log("DISCONNECTED: " + d.getTitle().getString());
            finish(mc);
            return;
        }
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;
        if (!joined) {
            joined = true;
            log("joined as " + mc.player.getGameProfile().getName() + " in " + mc.level.dimension().location() + " at " + mc.player.blockPosition());
        }
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)) mc.setScreen(null);
        if (settle < 100) { settle++; return; }
        tick++;
        if (A && !SECOND) a1(mc);
        else if (A) a2(mc);
        else if (!SECOND) b1(mc);
        else b2(mc);
    }

    private static void cmd(String c) {
        log("> /" + c);
        Minecraft.getInstance().getConnection().sendCommand(c);
    }

    private static void buy(String id) {
        var c = Characters.all().stream().filter(p -> p.outcomeId().equals(id)).findFirst();
        if (c.isEmpty()) { log("FAILED: no character " + id); return; }
        for (SkillNode n : c.get().skillTree().allOrdered()) NetworkHandler.sendToServer(new SpendSkillPointPacket(n.id()));
    }

    /** What the server holds of {@code who}, and (for oneself) what the client shows. */
    private static void snapshot(Minecraft mc, String who, String label) {
        log("#### SNAPSHOT " + label + " " + who);
        cmd("data get entity " + who + " NeoForgeData");
        cmd("data get entity " + who + " Inventory");
        cmd("data get entity " + who + " Dimension");
        cmd("data get entity " + who + " Pos");
        if (who.equals(mc.player.getGameProfile().getName())) {
            var form = TransformationManager.getTransformation(mc.player);
            log("  client shows: level " + PlayerLevel.getLevel(mc.player) + ", class " + PlayerLevel.getProgressionClass(mc.player)
                    + ", " + com.jttw.engine.client.ClientAbilityData.totalCount() + " ability slots, form " + (form == null ? "none" : form.getFormId())
                    + ", reincarnated " + mc.player.getPersistentData().getBoolean("jttw_reincarnated")
                    + ", in " + mc.level.dimension().location() + " at " + mc.player.blockPosition());
        }
    }

    private static void counts() {
        log("#### COUNTS");
        for (String t : new String[]{"jttw_engine:wu_clone", "jttw_engine:xiaotian_quan", "jttw_engine:placed_staff", "jttw_engine:bone_servant"}) {
            cmd("execute if entity @e[type=" + t + "]");
        }
    }

    private static final KeyMapping[] SLOTS() {
        return new KeyMapping[]{ClientInit.ABILITY_SLOT_0, ClientInit.ABILITY_SLOT_1, ClientInit.ABILITY_SLOT_2, ClientInit.ABILITY_SLOT_3};
    }

    private static KeyMapping held;

    private static void press(int slot) {
        KeyMapping k = SLOTS()[slot];
        KeyMapping.click(k.getKey());
        k.setDown(true);
        held = k;
    }

    // ── a: Wukong, stopped under ────────────────────────────────────────

    private static void a1(Minecraft mc) {
        if (held != null && tick % 20 == 10) { held.setDown(false); held = null; }
        switch (tick) {
            case 1 -> {
                cmd("execute in minecraft:overworld run tp @s 0 120 0");
                cmd("gamemode survival");
                cmd("effect give @s minecraft:resistance infinite 4 true");
                cmd("jttw setclass any celestial_primate");
                cmd("jttw_setlevel 80");
                cmd("jttw_addsp 1000");
                cmd("give @s minecraft:diamond 7");
                cmd("give @s minecraft:golden_apple 3");
            }
            case 20, 30, 40, 50 -> buy("celestial_primate");
            // each of his keys once, a second apart (one is his clones; the menu of forms is shut by the tick)
            case 100 -> press(0);
            case 120 -> press(1);
            case 140 -> press(2);
            case 160 -> press(3);
            case 240 -> cmd("jttw transform minecraft:wolf");
            case 300 -> { counts(); snapshot(mc, "Tester1", "before-stop"); }
            default -> {}
        }
        if (tick > 340 && (partnerDone || tick > 20 * 240) && tick % 20 == 0 && !stopping) {
            stopping = true;
            log("the partner has logged out; once more, then the server is stopped under him");
            counts();
            snapshot(mc, "Tester1", "at-stop");
        }
        if (stopping && ++stopWait == 60) cmd("stop");
    }

    private static boolean stopping;
    private static int stopWait;

    private static void a2(Minecraft mc) {
        switch (tick) {
            case 100 -> { counts(); snapshot(mc, "Tester1", "after-restart"); }
            case 200 -> {
                if (mc.getConnection().getOnlinePlayers().size() < 2) log("  (Tester2 not on yet)");
                snapshot(mc, "Tester2", "after-restart (seen by Tester1)");
            }
            case 260 -> { cmd("jttw demorph"); press(0); }
            case 300 -> { held.setDown(false); held = null; counts(); snapshot(mc, "Tester1", "after-demorph-and-a-key"); }
            case 400 -> { cmd("say A2_DONE"); log("== a2 done =="); finish(mc); }
            default -> {}
        }
    }

    // ── b: the Peng, out in the Great Void ──────────────────────────────

    private static void b1(Minecraft mc) {
        switch (tick) {
            case 1 -> {
                cmd("gamemode survival");
                cmd("effect give @s minecraft:resistance infinite 4 true");
                cmd("jttw setclass any gold_winged_peng");
                cmd("jttw_setlevel 90");
                cmd("jttw_addsp 1000");
                cmd("give @s minecraft:emerald 11");
            }
            case 20, 30, 40, 50 -> buy("gold_winged_peng");
            case 100 -> cmd("execute in jttw_engine:hell run tp @s 0 60 0");
            case 200 -> cmd("jttw transform minecraft:spider");
            case 260 -> { log("-> the Great Void key, from Hell"); NetworkHandler.sendToServer(GreatVoidPacket.INSTANCE); }
            case 320 -> cmd("tp @s ~300 ~ ~");
            case 380 -> snapshot(mc, "Tester2", "logging-out");
            case 440 -> { cmd("say B_LOGGING_OUT"); }
            case 460 -> { log("== b1 logs out =="); finish(mc); }
            default -> {}
        }
    }

    private static void b2(Minecraft mc) {
        switch (tick) {
            case 100 -> snapshot(mc, "Tester2", "after-restart");
            case 160 -> { log("-> the Great Void key, to leave it"); NetworkHandler.sendToServer(GreatVoidPacket.INSTANCE); }
            case 260 -> snapshot(mc, "Tester2", "after-leaving-the-void");
            case 300 -> { cmd("jttw demorph"); }
            case 360 -> snapshot(mc, "Tester2", "after-demorph");
            case 420 -> { log("== b2 done =="); finish(mc); }
            default -> {}
        }
    }

    private static void finish(Minecraft mc) {
        done = true;
        File out = new File(mc.gameDirectory, "persist-" + ROLE.substring("persist-".length()) + ".txt");
        try {
            Files.write(out.toPath(), new ArrayList<>(report));
        } catch (IOException ex) {
            JTTWEngine.LOGGER.error("[persist] could not write the report", ex);
        }
        mc.stop();
    }
}
