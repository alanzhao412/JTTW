package com.jttw.engine.gametest.playtest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.AbilityAction;
import com.jttw.engine.character.Characters;
import com.jttw.engine.character.PlayableCharacter;
import com.jttw.engine.network.NetworkHandler;
import com.jttw.engine.network.c2s.ActivateAbilityPacket;
import com.jttw.engine.network.c2s.GreatVoidPacket;
import com.jttw.engine.network.c2s.SpendSkillPointPacket;
import com.jttw.engine.progression.skilltree.SkillNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The general stress pass, two real clients on a real dedicated server: everything heavy the mod
 * adds, not one ability ({@code runMultiplayerPass -Pmp.role=general} with
 * {@code runMultiplayerObserver -Pmp.role2=partner}; both operators).
 * <ol>
 *   <li><b>census</b>: every JTTW creature, kind by kind, twenty of it (three if it is a big one)
 *       set on both players: the server's tick time and the client's frames with each, so a costly
 *       mind or a costly model shows by name;</li>
 *   <li><b>boss brawl</b>: every big one at once about both players, abilities going;</li>
 *   <li><b>zones</b>: each player sent to a different zone at once (by {@code /locate biome}), the
 *       mod's heaviest ground made in two places together;</li>
 *   <li><b>the Great Void</b>: the partner as Wukong, 2000 blocks through it and out onto new ground;</li>
 *   <li><b>soak</b>: some forty minutes of rounds (hordes of the mod's creatures and vanilla's, a
 *       new character each round, forms taken, realms crossed) for what grows over a long session.</li>
 * </ol>
 * The partner does what the driver tells it in chat ({@code PARTNER_...}). Reports go in
 * {@code <run dir>/general-<role>-<time>.txt}.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class GeneralStress {

    private GeneralStress() {}

    static final String ROLE = Boolean.getBoolean("jttw.mpPass") ? System.getProperty("jttw.mpPass.role", "") : "";
    private static final boolean DRIVER = ROLE.equals("general"), PARTNER = ROLE.equals("partner");
    static boolean active() { return DRIVER || PARTNER; }

    /** Soak rounds: three minutes each. */
    private static final int SOAK_ROUNDS = Integer.getInteger("jttw.general.soakRounds", 13);

    private static final String RUN = "general-" + ROLE + "-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());
    private record Action(int at, String name, Runnable run) {}

    private static final List<String> report = Collections.synchronizedList(new ArrayList<>());
    private static List<Action> plan;
    private static int tick, planTick, next, settle, waited;
    private static boolean joined, done, ended;
    private static String phase = "joining";
    private static final Random RNG = new Random(11);
    private static final java.util.Map<String, int[]> FPS = new java.util.LinkedHashMap<>();
    /** A {@code /locate biome} answer waited for: its coordinates. */
    private static volatile int[] located;

    private static void log(String line) {
        report.add("[" + tick + "] " + line);
        JTTWEngine.LOGGER.info("[general] {}", line);
    }

    private static void phase(String p) {
        phase = p;
        log("#### PHASE " + p);
    }

    private static final Pattern LOCATED = Pattern.compile("is at \\[(-?\\d+), [~\\-\\d]+, (-?\\d+)\\]");

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent e) {
        if (!active() || done) return;
        String m = e.getMessage().getString().replace('\n', ' ');
        Matcher loc = LOCATED.matcher(m);
        if (loc.find()) located = new int[]{Integer.parseInt(loc.group(1)), Integer.parseInt(loc.group(2))};
        int at = m.indexOf("PARTNER_");
        if (PARTNER && at >= 0) partnerOrder(m.substring(at + "PARTNER_".length()).trim());
        if (m.contains("GENERAL_END")) ended = true;
        // keep what measures, warns, fails or travels; the ability chatter ("[Nezha] ...") and summons go
        if (m.contains("Summoned new") || m.contains("Killed ") || m.contains("Applied effect")) return;
        if (m.startsWith("[") && !m.contains("PARTNER_") && !m.contains("_END") && !m.contains("arrive")) return;
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
        if (!joined) { joined = true; log("joined as " + mc.player.getGameProfile().getName()); }
        if (mc.screen instanceof DeathScreen) {
            log("DIED in " + mc.level.dimension().location() + " at " + mc.player.blockPosition() + "; respawning");
            mc.player.respawn();
            mc.setScreen(null);
        } else if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.ReceivingLevelScreen)) {
            mc.setScreen(null);
        }
        if (settle < 100) { settle++; return; }
        tick++;
        if (tick % 100 == 0) {
            int fps = mc.getFps();
            int[] s = FPS.computeIfAbsent(phase, k -> new int[]{0, 0, Integer.MAX_VALUE, 0});
            s[0]++; s[1] += fps; s[2] = Math.min(s[2], fps); s[3] = Math.max(s[3], fps);
            Runtime r = Runtime.getRuntime();
            boolean knows = mc.player.getPersistentData().getBoolean("jttw_reincarnated");
            log("fps " + fps + ", client memory " + (r.totalMemory() - r.freeMemory()) / 1048576 + " MB, in "
                    + mc.level.dimension().location() + " at " + mc.player.blockPosition() + ", "
                    + mc.level.getEntityCount() + " entities known" + (knows ? "" : ", CLIENT NOT REINCARNATED"));
        }
        if (PARTNER) { partner(mc); return; }
        if (tick % 200 == 0) { cmd("tick query"); cmd("neoforge tps"); }
        if (plan == null) {
            if (mc.getConnection().getOnlinePlayers().size() < 2 && waited++ < 20 * 180) return;
            plan = plan();
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
        if (next >= plan.size()) finish(mc);
    }

    private static void cmd(String c) {
        Minecraft.getInstance().getConnection().sendCommand(c);
    }

    private static void loud(String c) {
        log("> /" + c);
        cmd(c);
    }

    private static void hardy() {
        cmd("effect give @s minecraft:resistance infinite 4 true");
        cmd("effect give @s minecraft:regeneration infinite 2 true");
        cmd("effect give @s minecraft:saturation infinite 0 true");
        cmd("effect give @s minecraft:fire_resistance infinite 0 true");
        cmd("effect give @s minecraft:water_breathing infinite 0 true");
    }

    /** Become {@code id}, level 100, the whole tree bought (as the tree screen buys it), over 60 ticks from {@code t}. */
    private static void become(String id, List<Action> out, int t) {
        out.add(new Action(t, "become " + id, () -> becomeNow(id)));
        for (int pass = 1; pass <= 4; pass++) out.add(new Action(t + 15 * pass, "buy " + id, () -> buyTree(id)));
    }

    private static void becomeNow(String id) {
        cmd("jttw demorph");
        cmd("jttw setclass any " + id);
        cmd("jttw_setlevel 100");
        cmd("jttw_addsp 1000");
        cmd("jttw_addsp 1000");
        hardy();
    }

    private static void buyTree(String id) {
        var c = Characters.all().stream().filter(p -> p.outcomeId().equals(id)).findFirst();
        if (c.isEmpty()) { log("FAILED: no character " + id); return; }
        for (SkillNode n : c.get().skillTree().allOrdered()) NetworkHandler.sendToServer(new SpendSkillPointPacket(n.id()));
    }

    /** Every JTTW creature (not a projectile, a clone's stand-in or a thing laid down: those are MISC). */
    private static List<EntityType<?>> creatures() {
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
            var key = BuiltInRegistries.ENTITY_TYPE.getKey(t);
            if (key.getNamespace().equals(JTTWEngine.MOD_ID) && t.getCategory() != MobCategory.MISC) out.add(t);
        }
        return out;
    }

    private static String id(EntityType<?> t) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(t).toString();
    }

    private static final java.util.Map<EntityType<?>, Boolean> BIG = new java.util.HashMap<>();

    /** A big one, met one at a time in play: large, or a boss's health (100 or more), or a zone's boss. */
    private static boolean big(EntityType<?> t) {
        return BIG.computeIfAbsent(t, k -> {
            if (k.getDimensions().width() >= 1.4f || k.getDimensions().height() >= 3.5f) return true;
            var e = k.create(Minecraft.getInstance().level);
            boolean boss = e instanceof net.minecraft.world.entity.LivingEntity l && l.getMaxHealth() >= 100
                    || e instanceof com.jttw.engine.entity.zone.ZoneBoss;
            if (e != null) e.discard();
            return boss;
        });
    }

    private static void summonAround(String type, int n, int radius) {
        for (int k = 0; k < n; k++) {
            double a = RNG.nextDouble() * Math.PI * 2, r = 4 + RNG.nextDouble() * radius;
            cmd("summon " + type + " " + (int) (Math.cos(a) * r) + " 101 " + (int) (Math.sin(a) * r) + " {PersistenceRequired:1b}");
        }
    }

    private static final String[] CAST = {"celestial_primate", "dragon", "third_lotus_prince", "eye_of_heaven", "grand_saint_of_nine_spirits", "bull_demon_king", "gold_winged_peng"};
    private static final String[] VANILLA = {"zombie", "husk", "skeleton", "spider", "vindicator", "pillager", "witch", "stray"};

    private static List<Action> plan() {
        List<Action> out = new ArrayList<>();
        List<EntityType<?>> kinds = creatures();
        List<String> castable = new ArrayList<>();
        for (String c : CAST) if (Characters.all().stream().anyMatch(p -> p.outcomeId().equals(c))) castable.add(c);
        int t = 0;
        out.add(new Action(t, "setup", () -> {
            phase("setup");
            log("creatures in the census: " + kinds.size());
            cmd("execute in minecraft:overworld run tp @s 0 120 0");
            cmd("gamemode survival");
            cmd("difficulty normal");
            cmd("time set day");
            cmd("weather clear");
            cmd("gamerule doMobLoot false");
            cmd("gamerule doDaylightCycle false");
            hardy();
        }));
        t += 20;
        out.add(new Action(t, "stage", () -> {
            cmd("fill -24 99 -24 24 99 24 minecraft:stone");
            cmd("fill -24 100 -24 24 115 24 minecraft:air");
            cmd("tp @s 0 100 0 0 0");
            cmd("tp Tester2 3 100 0");
            cmd("execute as Tester2 run effect give @s minecraft:resistance infinite 4 true");
            cmd("execute as Tester2 run effect give @s minecraft:regeneration infinite 2 true");
            cmd("execute as Tester2 run effect give @s minecraft:saturation infinite 0 true");
            cmd("execute as Tester2 run effect give @s minecraft:fire_resistance infinite 0 true");
            cmd("execute as Tester2 run gamemode survival Tester2");
        }));
        t += 40;
        become("third_lotus_prince", out, t);
        out.add(new Action(t, "partner", () -> loud("say PARTNER_AS celestial_primate")));
        t += 120;
        out.add(new Action(t, "baseline", () -> phase("census-baseline")));
        t += 210;

        // 1. the census
        for (EntityType<?> k : kinds) {
            String id = id(k);
            int n = big(k) ? 3 : 20;
            out.add(new Action(t, "census " + id, () -> {
                phase("census " + id + " x" + n);
                cmd("tp @s 0 100 0");
                cmd("tp Tester2 3 100 0");
                summonAround(id, n, 10);
            }));
            t += 200;   // the tick query at 200 ticks covers the 100 before it, all of them about
            out.add(new Action(t - 1, "census count " + id, () -> loud("execute if entity @e[type=" + id + "]")));
            out.add(new Action(t, "census end " + id, () -> cmd("kill @e[type=!minecraft:player,distance=..80]")));
            t += 20;
        }

        // 2. the boss brawl: every big one at once
        out.add(new Action(t, "brawl", () -> {
            phase("boss-brawl");
            cmd("tp @s 0 100 0");
            cmd("tp Tester2 3 100 0");
            cmd("say PARTNER_ATTACK on");
            for (EntityType<?> k : kinds) if (big(k)) summonAround(id(k), 1, 16);
        }));
        for (int i = 0; i < 30; i++) out.add(new Action(t + 20 + i * 40, "brawl ability", () ->
                NetworkHandler.sendToServer(new ActivateAbilityPacket(RNG.nextInt(4), AbilityAction.USE))));
        t += 1300;
        out.add(new Action(t, "brawl count", () -> loud("execute if entity @e[type=!minecraft:player]")));
        out.add(new Action(t + 1, "brawl end", () -> { cmd("kill @e[type=!minecraft:player]"); cmd("say PARTNER_ATTACK off"); }));
        t += 100;

        // 3. the zones, two at once
        String[] zones = {"kunlun", "fengdu", "fire_mountain", "flower_fruit_mountain", "black_wind_mountain", "white_bone_ridge",
                "lion_camel_ridge", "silk_web_ridge", "bottomless_cave", "mystical_waters", "mount_wuzhuang"};
        out.add(new Action(t, "zones", () -> { phase("zones"); cmd("gamemode creative"); cmd("execute as Tester2 run gamemode creative Tester2"); }));
        t += 20;
        for (int z = 0; z < zones.length; z++) {
            String mine = zones[z], his = zones[(z + 5) % zones.length];
            out.add(new Action(t, "locate " + mine, () -> { located = null; cmd("locate biome jttw_engine:" + mine); }));
            out.add(new Action(t + 60, "go " + mine, () -> {
                int[] at = located;
                if (at == null) { log("  could not find " + mine); return; }
                phase("zone " + mine + " (and the partner in " + his + ")");
                loud("tp @s " + at[0] + " 180 " + at[1]);
            }));
            out.add(new Action(t + 70, "locate " + his, () -> { located = null; cmd("execute as Tester2 at Tester2 run locate biome jttw_engine:" + his); }));
            out.add(new Action(t + 130, "send " + his, () -> {
                int[] at = located;
                if (at == null) { log("  could not find " + his + " for the partner"); return; }
                loud("tp Tester2 " + at[0] + " 180 " + at[1]);
            }));
            out.add(new Action(t + 400, "shot " + mine, () -> shot("zone-" + mine)));
            t += 420;
        }

        // 4. the Great Void, the partner as Wukong
        out.add(new Action(t, "void", () -> {
            phase("great-void");
            loud("tp Tester2 0 150 0");
            cmd("tp @s 4 150 0");
            cmd("say PARTNER_VOID_IN");
        }));
        out.add(new Action(t + 60, "void fly", () -> cmd("say PARTNER_FLY 2000")));
        out.add(new Action(t + 700, "void out", () -> cmd("say PARTNER_VOID_OUT")));
        t += 1000;

        // 5. the soak
        out.add(new Action(t, "soak", () -> {
            phase("soak");
            cmd("gamemode survival");
            cmd("execute as Tester2 run gamemode survival Tester2");
            cmd("say PARTNER_ATTACK on");
        }));
        t += 20;
        for (int round = 0; round < SOAK_ROUNDS; round++) {
            String me = castable.get(round % castable.size()), him = castable.get((round + 3) % castable.size());
            int r = round;
            out.add(new Action(t, "round " + r, () -> {
                phase("soak round " + r + " (" + me + " and " + him + ")");
                cmd("execute in minecraft:overworld run tp @s 0 100 0 0 0");
                cmd("execute in minecraft:overworld run tp Tester2 3 100 0");
                cmd("say PARTNER_AS " + him);
            }));
            become(me, out, t + 2);
            out.add(new Action(t + 80, "horde " + r, () -> {
                for (int k = 0; k < 30; k++) summonAround("minecraft:" + VANILLA[RNG.nextInt(VANILLA.length)], 1, 14);
                for (int k = 0; k < 20; k++) {
                    EntityType<?> kind = kinds.get(RNG.nextInt(kinds.size()));
                    if (!big(kind)) summonAround(id(kind), 1, 14);
                }
            }));
            for (int i = 0; i < 20; i++) out.add(new Action(t + 100 + i * 50, "soak ability", () ->
                    NetworkHandler.sendToServer(new ActivateAbilityPacket(RNG.nextInt(4), AbilityAction.USE))));
            out.add(new Action(t + 1200, "form " + r, () -> cmd("jttw transform minecraft:" + (r % 2 == 0 ? "wolf" : "horse"))));
            out.add(new Action(t + 1400, "demorph " + r, () -> cmd("jttw demorph")));
            // a realm crossed each round, there and back (the partner by command; the driver every other round)
            out.add(new Action(t + 1500, "realms " + r, () -> {
                cmd("execute in jttw_engine:" + (r % 2 == 0 ? "hell" : "heaven") + " run tp Tester2 0 120 0");
                if (r % 2 == 1) cmd("execute in minecraft:the_nether run tp @s 0 70 0");
            }));
            out.add(new Action(t + 1700, "home " + r, () -> {
                cmd("execute in minecraft:overworld run tp Tester2 3 100 0");
                cmd("execute in minecraft:overworld run tp @s 0 100 0");
            }));
            out.add(new Action(t + 1780, "count " + r, () -> loud("execute if entity @e[type=!minecraft:player]")));
            out.add(new Action(t + 1790, "clear " + r, () -> cmd("kill @e[type=!minecraft:player]")));
            t += 3600;   // three minutes a round, the last ninety seconds of it quiet
        }
        out.add(new Action(t, "end", () -> {
            phase("end");
            loud("say GENERAL_END");
        }));
        out.add(new Action(t + 100, "bye", () -> log("== general pass complete ==")));
        return out;
    }

    // ── The partner ─────────────────────────────────────────────────────

    private static boolean attacking;
    private static int flyLeft;
    private static String partnerAs;
    private static int partnerAsAt = -1;

    private static void partnerOrder(String order) {
        log("  order: " + order);
        if (order.startsWith("AS ")) { partnerAs = order.substring(3).trim(); partnerAsAt = tick; becomeNow(partnerAs); }
        else if (order.startsWith("ATTACK")) attacking = order.endsWith("on");
        else if (order.equals("VOID_IN") || order.equals("VOID_OUT")) {
            NetworkHandler.sendToServer(GreatVoidPacket.INSTANCE);
            log("  -> Great Void key");
        } else if (order.startsWith("FLY ")) flyLeft = Integer.parseInt(order.substring(4).trim());
    }

    private static void partner(Minecraft mc) {
        if (partnerAs != null && partnerAsAt >= 0) {
            int since = tick - partnerAsAt;
            if (since > 0 && since <= 60 && since % 15 == 0) buyTree(partnerAs);
        }
        if (attacking && tick % 60 == 30) NetworkHandler.sendToServer(new ActivateAbilityPacket(RNG.nextInt(4), AbilityAction.USE));
        if (flyLeft > 0 && tick % 10 == 0) {
            cmd("tp @s ~50 ~ ~");
            flyLeft -= 50;
            if (flyLeft <= 0) log("  flew; now in " + mc.level.dimension().location() + " at " + mc.player.blockPosition());
        }
        if (tick % 200 == 0 && mc.level.dimension().location().getPath().equals("great_void")) log("  in the Great Void at " + mc.player.blockPosition());
        if (ended || tick > 20 * 60 * 120) {
            log("== partner done ==");
            finish(mc);
        }
    }

    // ── Both ────────────────────────────────────────────────────────────

    private static void shot(String id) {
        Minecraft mc = Minecraft.getInstance();
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
            JTTWEngine.LOGGER.error("[general] could not write the report", ex);
        }
        mc.stop();
    }
}
