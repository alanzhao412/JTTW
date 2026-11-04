package com.jttw.engine.gametest.playtest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.client.ClientInit;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.ActivateAbilityPacket;
import com.jttw.engine.network.c2s.DimensionSelectPacket;
import com.jttw.engine.network.c2s.GreatVoidPacket;
import com.jttw.engine.network.c2s.SpendSkillPointPacket;
import com.jttw.engine.progression.skilltree.SkillNode;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.world.effect.MobEffects;
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
import java.util.Random;

/**
 * A stress pass on a real dedicated server with two real clients ({@code ./gradlew runMultiplayerPass
 * -Pmp.role=stress} and {@code runMultiplayerObserver -Pmp.role2=roamer}; both players operators).
 * <ul>
 *   <li><b>stress</b> (Tester1, the Overworld): the Dragon's Tidal Wave cast at a friend standing in
 *       water (PvP off: he must be neither pinned, slowed nor drowned), then at a horde of 200;
 *       every character's four abilities into the horde; a sprint that makes 3000 blocks of new
 *       world; the horde cleared and the server's recovery.</li>
 *   <li><b>roamer</b> (Tester2): stands in the water for the wave, then goes round every realm by
 *       the Dragon's own Dimension Travel (Nether, End, Hell, Heaven, home) and through the Great
 *       Void, fighting in each, while the other is still at it in the Overworld.</li>
 *   <li><b>probe</b> ({@code runMultiplayerPass -Pmp.role=probe}, alone): a survival Dragon's client
 *       across realms, its frames and what it still knows of him (see {@link #probe}).</li>
 * </ul>
 * The server's tick times ({@code /neoforge tps} per realm, {@code /tick query}) every ten seconds,
 * each client's frames per second and memory every five, go in {@code <run dir>/stress-<role>-<time>.txt}.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class StressPass {

    private StressPass() {}

    static final String ROLE = Boolean.getBoolean("jttw.mpPass") ? System.getProperty("jttw.mpPass.role", "") : "";
    private static final boolean STRESS = ROLE.equals("stress"), ROAMER = ROLE.equals("roamer"), PROBE = ROLE.equals("probe");
    static boolean active() { return STRESS || ROAMER || PROBE; }

    private static final String RUN = "stress-" + ROLE + "-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
    private record Action(int at, String name, Runnable run) {}

    private static final List<String> report = Collections.synchronizedList(new ArrayList<>());
    private static List<Action> plan;
    private static int tick, planTick, planEnd = -1, next, settle, waitedForFriend;
    private static boolean joined, done, startRoaming, stressEnded, roamerDone;
    private static String phase = "joining";
    private static boolean closeScreens = true;
    private static final Random RNG = new Random(7);

    // frames per second, by phase
    private static final java.util.Map<String, int[]> FPS = new java.util.LinkedHashMap<>();   // phase -> {samples, sum, min, max}

    private static void log(String line) {
        report.add("[" + tick + "] " + line);
        JTTWEngine.LOGGER.info("[stress] {}", line);
    }

    private static void phase(String p) {
        phase = p;
        log("#### PHASE " + p);
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent e) {
        if (!active() || done) return;
        String m = e.getMessage().getString().replace('\n', ' ');
        if (m.contains("ROAM_NOW")) startRoaming = true;
        if (m.contains("STRESS_END")) stressEnded = true;
        if (m.contains("ROAMER_DONE")) roamerDone = true;
        // the ability chatter ("[Nezha] ...") is many lines a second: keep the wave's, the travels', and all else
        if (m.startsWith("[") && !m.contains("Tidal Wave") && !m.contains("arrive") && !m.contains("Dimension") && !m.contains("_NOW")
                && !m.contains("_DONE") && !m.contains("_END")) return;
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
            log("joined as " + mc.player.getGameProfile().getName());
        }
        if (mc.screen instanceof DeathScreen) {
            log("DIED in " + mc.level.dimension().location() + " at " + mc.player.blockPosition() + "; respawning");
            mc.player.respawn();
            mc.setScreen(null);
        } else if (mc.screen != null && closeScreens) {
            if (!(mc.screen instanceof net.minecraft.client.gui.screens.ChatScreen)) log("  closed a screen: " + mc.screen.getClass().getSimpleName());
            mc.setScreen(null);
        }
        if (settle < 100) { settle++; return; }
        tick++;
        sample(mc);
        if (PROBE) probe(mc); else if (STRESS) driver(mc); else roamer(mc);
    }

    /** Every 5 s, this client's frames and memory; every 10 s (the driver), the server's tick times. */
    private static void sample(Minecraft mc) {
        if (tick % 100 == 0) {
            int fps = mc.getFps();
            int[] s = FPS.computeIfAbsent(phase, k -> new int[]{0, 0, Integer.MAX_VALUE, 0});
            s[0]++; s[1] += fps; s[2] = Math.min(s[2], fps); s[3] = Math.max(s[3], fps);
            Runtime r = Runtime.getRuntime();
            log("fps " + fps + ", client memory " + (r.totalMemory() - r.freeMemory()) / 1048576 + " MB, in "
                    + mc.level.dimension().location() + " at " + mc.player.blockPosition() + ", " + countEntities(mc) + " entities in view");
        }
        if (STRESS && tick % 200 == 0) {
            cmd("neoforge tps");
            cmd("tick query");
        }
    }

    private static int countEntities(Minecraft mc) {
        int n = 0;
        for (var ignored : mc.level.entitiesForRendering()) n++;
        return n;
    }

    // ── The driver ──────────────────────────────────────────────────────

    private static void driver(Minecraft mc) {
        if (plan == null) {
            if (mc.getConnection().getOnlinePlayers().size() < 2) {
                if (waitedForFriend++ % 200 == 0) log("waiting for Tester2");
                if (waitedForFriend < 20 * 180) return;
            }
            plan = stressPlan();
        }
        while (next < plan.size() && plan.get(next).at <= planTick) {
            Action a = plan.get(next++);
            try {
                a.run.run();
            } catch (Throwable t) {
                log("FAILED " + a.name + ": " + t);
            }
        }
        planTick++;
        if (next >= plan.size()) {
            if (planEnd < 0) { planEnd = tick; phase("waiting-for-the-roamer"); }
            // the server still measured while he goes on in the other realms
            if (roamerDone || tick - planEnd > 20 * 300) finish(mc);
        }
    }

    private static void cmd(String c) {
        Minecraft.getInstance().getConnection().sendCommand(c);
    }

    private static void loud(String c) {
        log("> /" + c);
        cmd(c);
    }

    private static void press(KeyMapping key, int holdTicks, List<Action> out, int at) {
        out.add(new Action(at, "press " + key.getName(), () -> {
            KeyMapping.click(key.getKey());
            key.setDown(true);
        }));
        out.add(new Action(at + holdTicks, "release " + key.getName(), () -> key.setDown(false)));
    }

    /** Become {@code id} at level 100 with the whole tree bought (as the tree screen buys it). */
    private static int become(PlayableCharacter c, List<Action> out, int t) {
        String id = c.outcomeId();
        out.add(new Action(t, "become " + id, () -> {
            log("== " + id + " ==");
            cmd("jttw demorph");
            cmd("jttw setclass any " + id);
            cmd("jttw_setlevel 100");
            cmd("jttw_addsp 1000"); cmd("jttw_addsp 1000");
            hardy();
        }));
        t += 20;
        for (int pass = 0; pass < 4; pass++) {
            out.add(new Action(t, "buy " + id, () -> {
                for (SkillNode n : c.skillTree().allOrdered()) NetworkHandler.sendToServer(new SpendSkillPointPacket(n.id()));
            }));
            t += 10;
        }
        return t;
    }

    private static void hardy() {
        cmd("effect give @s minecraft:resistance infinite 4 true");
        cmd("effect give @s minecraft:regeneration infinite 2 true");
        cmd("effect give @s minecraft:saturation infinite 0 true");
        cmd("effect give @s minecraft:fire_resistance infinite 0 true");
        cmd("effect give @s minecraft:water_breathing infinite 0 true");
    }

    private static final String[] HORDE = {"zombie", "husk", "skeleton", "spider", "vindicator", "pillager", "witch", "drowned", "stray", "creeper"};

    /** {@code n} hostile mobs set in the 24 x 20 blocks ahead (south), ten a tick. */
    private static int horde(int n, List<Action> out, int t) {
        for (int i = 0; i < n; i += 10) {
            int batch = Math.min(10, n - i);
            out.add(new Action(t, "horde", () -> {
                for (int k = 0; k < batch; k++) {
                    String type = HORDE[RNG.nextInt(HORDE.length)];
                    if (type.equals("creeper")) type = "husk";   // creepers would crater the test ground
                    cmd("summon minecraft:" + type + " ~" + (RNG.nextInt(25) - 12) + " ~1 ~" + (5 + RNG.nextInt(20)) + " {PersistenceRequired:1b}");
                }
            }));
            t += 1;
        }
        return t;
    }

    /** The four ability keys, read when the plan is made (they are registered after this class loads). */
    private static KeyMapping[] slots() {
        return new KeyMapping[]{ ClientInit.ABILITY_SLOT_0, ClientInit.ABILITY_SLOT_1, ClientInit.ABILITY_SLOT_2, ClientInit.ABILITY_SLOT_3 };
    }

    private static List<Action> stressPlan() {
        List<Action> out = new ArrayList<>();
        int t = 0;
        out.add(new Action(t, "setup", () -> {
            phase("baseline");
            cmd("gamemode survival");
            cmd("difficulty normal");
            cmd("time set day");
            cmd("weather clear");
            cmd("gamerule doMobLoot false");   // the horde's drops are not what is measured
            hardy();
            // a flat stage at the spawn, ocean-deep water to its south
            cmd("tp @s 0 120 0 0 0");
        }));
        t += 20;
        out.add(new Action(t, "stage", () -> {
            cmd("fill -16 99 -6 16 99 30 minecraft:stone");
            cmd("fill -16 100 -6 16 112 30 minecraft:air");
            cmd("fill -6 100 3 6 101 12 minecraft:water");
            cmd("tp @s 0 100 0 0 0");
        }));
        t += 40;
        PlayableCharacter dragon = Characters.all().stream().filter(c -> c.outcomeId().equals("dragon")).findFirst().orElseThrow();
        t = become(dragon, out, t);
        t += 200;   // a quiet baseline

        // 1. the friend in the water, the wave at him (PvP off)
        out.add(new Action(t, "friend", () -> {
            phase("tidal-at-friend");
            loud("tp Tester2 0 100 6 180 0");
            cmd("tp @s 0 100 0 0 0");
        }));
        t += 60;
        press(ClientInit.ABILITY_SLOT_2, 5, out, t);
        out.add(new Action(t + 1, "note", () -> log("cast Tidal Wave III at Tester2 standing in water 6 blocks south")));
        t += 200;   // the bubble's three seconds, and more
        out.add(new Action(t, "release friend", () -> loud("say ROAM_NOW")));
        t += 100;

        // 2. the horde under the wave, the storm, the lightning
        out.add(new Action(t, "horde", () -> phase("tidal-horde-200")));
        t = horde(200, out, t);
        t += 60;
        press(ClientInit.ABILITY_SLOT_1, 5, out, t);   // the storm
        t += 40;
        for (int w = 0; w < 6; w++) {
            press(ClientInit.ABILITY_SLOT_2, 5, out, t);   // the wave (15 s cooldown)
            for (int l = 0; l < 3; l++) press(ClientInit.ABILITY_SLOT_0, 5, out, t + 60 + l * 80);   // lightning between
            t += 310;
            t = horde(30, out, t);   // keep it near 200
        }
        press(ClientInit.ABILITY_SLOT_1, 5, out, t);   // the storm off
        t += 40;

        // 3. every character's four abilities into the horde
        out.add(new Action(t, "barrage", () -> phase("every-ability-barrage")));
        for (PlayableCharacter c : Characters.all()) {
            t = become(c, out, t);
            out.add(new Action(t, "face", () -> cmd("tp @s 0 100 0 0 0")));
            t = horde(30, out, t + 1);
            for (KeyMapping key : slots()) {
                press(key, 25, out, t);
                t += 45;
            }
            String id = c.outcomeId();
            out.add(new Action(t, "shot", () -> shot("barrage-" + id)));
            t += 10;
        }
        out.add(new Action(t, "count", () -> {
            cmd("jttw demorph");
            loud("execute if entity @e[type=!minecraft:player]");
        }));
        t += 40;

        // 4. the sprint: 3000 blocks of new world, a jump every 2 s
        out.add(new Action(t, "sprint", () -> {
            phase("chunk-sprint-3000");
            cmd("gamemode creative");   // he hangs in the air while the new ground is made: a survival player would be kicked for floating
            cmd("jttw demorph");
        }));
        for (int j = 1; j <= 15; j++) {
            int x = 5000 + j * 200;
            out.add(new Action(t, "jump " + j, () -> cmd("execute in minecraft:overworld run tp @s " + x + " 200 5000")));
            t += 40;
        }
        t += 200;

        // 5. the horde cleared: does the server come back to its baseline?
        out.add(new Action(t, "clear", () -> {
            phase("recovery");
            cmd("tp @s 0 100 0 0 0");
            cmd("gamemode survival");
            loud("kill @e[type=!minecraft:player]");
        }));
        t += 600;
        out.add(new Action(t, "end", () -> {
            loud("say STRESS_END");
            log("== stress pass complete ==");
        }));
        t += 60;
        out.add(new Action(t, "bye", () -> {}));
        return out;
    }

    // ── The roamer ──────────────────────────────────────────────────────

    private static final int LEG = 600;   // 30 s in each realm
    private static int roamAt = -1, legs;
    private static String lastState = "";
    private static float lastHealth = -1;

    private static void roamer(Minecraft mc) {
        if (roamAt < 0) {
            if (tick == 1) {
                phase("standing-for-the-wave");
                cmd("gamemode survival");
                cmd("effect clear @s");
                cmd("effect give @s minecraft:saturation infinite 0 true");
            }
            // he stands in the water: what the wave does to him, as it happens
            boolean pinned = mc.player.hasEffect(com.jttw.engine.registry.ModEffects.PINNED);
            boolean slow = mc.player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN);
            String state = (pinned ? "PINNED " : "") + (slow ? "SLOWED " : "") + (mc.player.isInWater() ? "in water" : "dry")
                    + ", motion " + String.format("%.2f", mc.player.getDeltaMovement().horizontalDistance());
            if (!state.equals(lastState) && (pinned || slow || !state.startsWith(lastState.split(",")[0]))) log("  he is " + state);
            lastState = state;
            if (lastHealth >= 0 && mc.player.getHealth() < lastHealth) log("  HURT: health " + lastHealth + " -> " + mc.player.getHealth());
            lastHealth = mc.player.getHealth();
            if (startRoaming || tick > 20 * 300) {
                roamAt = tick;
                phase("roaming-the-realms");
                cmd("effect give @s minecraft:resistance infinite 4 true");
                cmd("effect give @s minecraft:regeneration infinite 2 true");
                cmd("effect give @s minecraft:fire_resistance infinite 0 true");
                cmd("effect give @s minecraft:water_breathing infinite 0 true");
                cmd("effect give @s minecraft:slow_falling infinite 0 true");
                cmd("jttw setclass any dragon");
                cmd("jttw_setlevel 100");
                cmd("jttw_addsp 1000"); cmd("jttw_addsp 1000");
            }
            return;
        }
        int r = tick - roamAt;
        if (r >= 20 && r < 60 && r % 10 == 0) {
            PlayableCharacter dragon = Characters.all().stream().filter(c -> c.outcomeId().equals("dragon")).findFirst().orElseThrow();
            for (SkillNode n : dragon.skillTree().allOrdered()) NetworkHandler.sendToServer(new SpendSkillPointPacket(n.id()));
        }
        // a leg in each realm: 50 s. Travel; fight there; fly about to make its ground.
        if (r < 80) return;
        int leg = (r - 80) / LEG, at = (r - 80) % LEG;
        if (leg >= 11) {
            if (at == 0) {
                log("== roamer done (" + legs + " legs) ==");
                cmd("say ROAMER_DONE");
            }
            if (at == 40) finish(mc);
            return;
        }
        String where = mc.level.dimension().location().toString();
        if (at == 0) {
            legs++;
            if (leg == 5) {   // the sixth: the Great Void, from the Overworld
                log("-> into the Great Void from " + where);
                NetworkHandler.sendToServer(GreatVoidPacket.INSTANCE);
            } else if (leg == 6) {
                log("-> out of the Great Void from " + where + " at " + mc.player.blockPosition());
                NetworkHandler.sendToServer(GreatVoidPacket.INSTANCE);
            } else {
                log("-> Dimension Travel from " + where);
                NetworkHandler.sendToServer(new DimensionSelectPacket(1));
                NetworkHandler.sendToServer(new ActivateAbilityPacket(PlayableCharacter.SLOT_COUNT, AbilityAction.USE));
            }
        }
        if (at == 60) {
            phase("realm-" + where);
            log("  arrived: " + where + " at " + mc.player.blockPosition());
            shot("roam-" + leg + "-" + where.replace(':', '_'));
            for (int k = 0; k < 40; k++) {
                String type = HORDE[RNG.nextInt(HORDE.length)];
                if (type.equals("creeper")) type = "zombie";
                cmd("summon minecraft:" + type + " ~" + (RNG.nextInt(17) - 8) + " ~ ~" + (RNG.nextInt(17) - 8) + " {PersistenceRequired:1b}");
            }
        }
        if (at >= 100 && at <= 400 && at % 75 == 25) {   // the wave, lightning, the storm (and off), the wave again
            int[] order = {2, 0, 1, 1, 2};
            NetworkHandler.sendToServer(new ActivateAbilityPacket(order[(at - 100) / 75], AbilityAction.USE));
        }
        if (at >= 420 && at < 560 && at % 20 == 0) cmd("tp @s ~48 ~ ~");   // fly on: new ground in this realm (or the Void's)
        if (at == 580) cmd("kill @e[type=!minecraft:player,distance=..64]");
    }

    // ── The probe: a survival player's client across realms ──

    /**
     * {@code -Pmp.role=probe}, alone: a level-100 Dragon in survival crosses realms twice by
     * Dimension Travel, logging his frames and what his client still knows of him every second. A
     * change of realm once left the client without its mirrored level and reincarnation mark (no JTTW
     * health bar, vanilla's hearts back, a wall of them at that health: 0 to 1 frames a second).
     */
    private static void probe(Minecraft mc) {
        if (tick % 20 == 0 && tick > 0) {
            boolean knows = mc.player.getPersistentData().getBoolean("jttw_reincarnated")
                    && com.jttw.engine.progression.level.PlayerLevel.getLevel(mc.player) == 100;
            log("  fps " + mc.getFps() + " in " + mc.level.dimension().location()
                    + (knows ? ", client knows him (level 100, reincarnated)" : ", CLIENT HAS LOST HIS LEVEL OR REINCARNATION"));
        }
        PlayableCharacter dragon = Characters.all().stream().filter(c -> c.outcomeId().equals("dragon")).findFirst().orElseThrow();
        switch (tick) {
            case 1 -> { phase("probe-overworld"); cmd("execute in minecraft:overworld run tp @s 0 120 0"); cmd("gamemode survival"); hardy();
                cmd("jttw setclass any dragon"); cmd("jttw_setlevel 100"); cmd("jttw_addsp 1000"); cmd("jttw_addsp 1000"); }
            case 40, 50, 60, 70 -> { for (SkillNode n : dragon.skillTree().allOrdered()) NetworkHandler.sendToServer(new SpendSkillPointPacket(n.id())); }
            case 200 -> { phase("probe-next-realm-by-dimension-travel"); travel(1); }
            case 400 -> { phase("probe-another-realm-by-dimension-travel"); travel(2); }
            case 600 -> { log("== probe done =="); finish(mc); }
            default -> {}
        }
    }

    /** The Dragon's Dimension Travel, {@code steps} on round the wheel from where it points. */
    private static void travel(int steps) {
        for (int i = 0; i < steps; i++) NetworkHandler.sendToServer(new DimensionSelectPacket(1));
        NetworkHandler.sendToServer(new ActivateAbilityPacket(PlayableCharacter.SLOT_COUNT, AbilityAction.USE));
    }

    // ── Both ────────────────────────────────────────────────────────────

    private static void shot(String id) {
        Minecraft mc = Minecraft.getInstance();
        // grab puts it under <dir>/screenshots/ itself
        Screenshot.grab(mc.gameDirectory, RUN + "/" + id + ".png", mc.getMainRenderTarget(), msg -> {});
    }

    private static void finish(Minecraft mc) {
        done = true;
        report.add("");
        report.add("frames per second by phase (samples, mean, min, max):");
        FPS.forEach((p, s) -> report.add("  " + p + ": " + s[0] + ", " + (s[0] == 0 ? 0 : s[1] / s[0]) + ", " + s[2] + ", " + s[3]));
        File out = new File(mc.gameDirectory, RUN + ".txt");
        try {
            Files.write(out.toPath(), new ArrayList<>(report));
        } catch (IOException ex) {
            JTTWEngine.LOGGER.error("[stress] could not write the report", ex);
        }
        mc.stop();
    }
}
