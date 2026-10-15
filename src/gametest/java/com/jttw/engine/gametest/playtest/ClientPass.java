package com.jttw.engine.gametest.playtest;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.character.peng.PengAbilityHandler;
import com.jttw.engine.client.StaffScrollHandler;
import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.flight.JTTWFlight;
import com.jttw.engine.item.ErlangAxeItem;
import com.jttw.engine.item.FireTipSpearItem;
import com.jttw.engine.item.YinYangSwordItem;
import com.jttw.engine.progression.level.LevelManager;
import com.jttw.engine.progression.qi.QiData;
import com.jttw.engine.progression.skilltree.SkillTreeData;
import com.jttw.engine.registry.ModItems;
import com.jttw.engine.reincarnation.AlignmentData;
import com.jttw.engine.reincarnation.ReincarnationOutcome;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * An automated play-test in a real client ({@code ./gradlew runPlaytestPass}): what the GameTests
 * cannot see, because the client is what moves the player, aims and draws.
 *
 * <ul>
 *   <li>Flight: real key presses fly the Peng (holding V) and Wukong (Flight III); the speed the
 *       client actually reaches is read off its own movement.</li>
 *   <li>The planted staff: aimed at near the top of a tall staff, then a real attack click.</li>
 *   <li>Tooltips, read back as the client builds them.</li>
 *   <li>First-person pictures of the large axe, the lit spear and the paired swords.</li>
 * </ul>
 * Writes {@code run-playtest/playtest-<time>.txt} and pictures beside it, then quits.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID, value = Dist.CLIENT)
public final class ClientPass {

    private ClientPass() {}

    private static final boolean ENABLED = Boolean.getBoolean("jttw.playtest");
    private static final String RUN = "playtest-" + new java.text.SimpleDateFormat("yyyyMMdd-HHmmss").format(new java.util.Date());

    /** One step: server setup, then the client runs it for some ticks; {@code each} sees each tick, {@code end} reports. */
    private record Step(String name, Consumer<ServerPlayer> setup, int ticks, StepTick each, Consumer<Minecraft> end) {}

    @FunctionalInterface
    private interface StepTick { void tick(Minecraft mc, int t); }

    private static boolean worldRequested;
    private static List<Step> steps;
    private static int index = -1;
    private static int t;
    private static int settle;
    private static final List<String> report = java.util.Collections.synchronizedList(new ArrayList<>()); // the server thread reports too
    private static Vec3 lastPos;
    private static final List<Double> speeds = new ArrayList<>();
    private static PlacedStaffEntity staff;

    private static ServerPlayer server(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    private static void become(ServerPlayer p, ReincarnationOutcome who, String... nodes) {
        AlignmentData.markReincarnated(p, who);
        LevelManager.initClass(p, who);
        for (String n : nodes) SkillTreeData.grantNode(p, n);
        QiData.setMax(p, 100000f);
        QiData.setCurrent(p, 100000f);
    }

    /** Up in the air over the flat world, facing east, flying, in survival so the mod's flight steers. */
    private static void aloft(ServerPlayer p) {
        p.setGameMode(GameType.SURVIVAL);
        p.teleportTo(p.serverLevel(), 0.5, 120, 0.5, -90f, 0f);
        p.getAbilities().mayfly = true;
        p.getAbilities().flying = true;
        p.onUpdateAbilities();
    }

    private static void reset(ServerPlayer p) {
        PengAbilityHandler.stopSoaring(p);
        JTTWFlight.setBoost(p, 1, false);
        JTTWFlight.setUniversalFlight(p, false);
        p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        p.removeAllEffects();
        p.clearFire();
        p.setTicksFrozen(0);
        p.setGameMode(GameType.CREATIVE);
        p.getAbilities().flying = false;
        p.onUpdateAbilities();
    }

    /** Horizontal speed the client moved the player this tick, in blocks a tick. */
    private static void sampleSpeed(Minecraft mc) {
        Vec3 now = mc.player.position();
        if (lastPos != null) speeds.add(Math.hypot(now.x - lastPos.x, now.z - lastPos.z));
        lastPos = now;
    }

    private static String speedLine(double expectStart, double expectTop) {
        StringBuilder sb = new StringBuilder();
        for (int s : new int[]{1, 3, 5, 8, 10, 12}) {
            int i = Math.min(speeds.size() - 1, s * 20);
            if (i >= 0) sb.append(String.format(" %ds:%.2f", s, speeds.get(i)));
        }
        double max = speeds.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        return String.format("blocks/tick at%s; top %.2f (expected about %.2f rising to %.2f)", sb, max, expectStart, expectTop);
    }

    private static void shoot(Minecraft mc, String name) {
        new File(mc.gameDirectory, "screenshots/" + RUN).mkdirs(); // the save fails, silently, into a missing folder
        Screenshot.grab(mc.gameDirectory, RUN + "/" + name + ".png", mc.getMainRenderTarget(),
                msg -> JTTWEngine.LOGGER.info("ClientPass: {}", msg.getString()));
        report.add("  picture: " + name + ".png");
    }

    private static List<Step> steps() {
        List<Step> all = new ArrayList<>();
        // ── Flight, with real keys ──
        all.add(new Step("Peng holds V (Ninety-Thousand-Li III) for 12 s", p -> {
            become(p, ReincarnationOutcome.GOLD_WINGED_PENG, "gp_soar_1", "gp_soar_2", "gp_soar_3");
            aloft(p);
            JTTWFlight.setUniversalFlight(p, true);
            JTTWFlight.setFlightSpeedTier(p, 2);
        }, 260, (mc, tick) -> {
            if (tick == 15) mc.getSingleplayerServer().execute(() -> PengAbilityHandler.startSoaring(server(mc)));
            if (tick >= 15) sampleSpeed(mc);
        }, mc -> report.add("  " + speedLine(1.55, 5.58))));
        all.add(new Step("Wukong flies forward (Flight III) for 12 s", p -> {
            become(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
            aloft(p);
            JTTWFlight.setUniversalFlight(p, true);
            JTTWFlight.setFlightSpeedTier(p, 3);
        }, 260, (mc, tick) -> {
            KeyMapping.set(mc.options.keyUp.getKey(), tick >= 15);
            if (tick >= 15) sampleSpeed(mc);
        }, mc -> {
            KeyMapping.set(mc.options.keyUp.getKey(), false);
            report.add("  " + speedLine(2.8, 5.58));
        }));
        // ── Teleported straight out of fast flight, he arrives where he was sent ──
        all.add(new Step("Wukong flies fast, stops, and is teleported 200 blocks", p -> {
            become(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
            aloft(p);
            JTTWFlight.setUniversalFlight(p, true);
            JTTWFlight.setFlightSpeedTier(p, 3);
        }, 140, (mc, tick) -> {
            KeyMapping.set(mc.options.keyUp.getKey(), tick >= 5 && tick < 80);
            if (tick == 80) mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = server(mc);
                sp.getAbilities().flying = false;
                sp.onUpdateAbilities();
                sp.teleportTo(sp.serverLevel(), -200.5, 120, 0.5, -90f, 0f);
            });
        }, mc -> report.add(String.format("  sent to x -200.5; the client is at x %.1f (it %s)", mc.player.getX(),
                Math.abs(mc.player.getX() + 200.5) < 3 ? "stayed there" : "DRIFTED"))));
        // ── The planted staff, aimed at near its top, a real click ──
        all.add(new Step("Wukong clicks a 30x staff near its top, 4 blocks off", p -> {
            become(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
            p.setGameMode(GameType.SURVIVAL);
            double ground = p.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 40, 0);
            staff = new PlacedStaffEntity(p.level(), 40.5, ground, 0.5, p.getUUID());
            staff.setStaffScale(30);
            p.level().addFreshEntity(staff);
            double side = staff.standingBox().maxX;
            JTTWFlight.setUniversalFlight(p, true); // so he stays up there
            p.getAbilities().mayfly = true;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(p.serverLevel(), side + 4, ground + 30, 0.5, 90f, 0f); // facing west, at it
        }, 60, (mc, tick) -> {
            if (tick == 30) {
                // What the client has: where it stands, and the staff as it knows it
                report.add(String.format("  client: player at %.1f %.1f %.1f, flying %s", mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        mc.player.getAbilities().flying));
                for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
                    if (e instanceof PlacedStaffEntity s) {
                        report.add(String.format("  client: staff at %.1f %.1f %.1f, scale %.0f, owner %s, standing %s, box %s, aim %.2f",
                                s.getX(), s.getY(), s.getZ(), s.getStaffScale(), s.isOwner(mc.player), s.isStanding(), s.standingBox(),
                                s.aimDistance(mc.player.getEyePosition(), mc.player.getLookAngle())));
                    }
                }
                PlacedStaffEntity aimed = StaffScrollHandler.aimedStaff(mc);
                report.add("  the client aims at the staff (scroll and click target): " + (aimed != null ? "yes" : "NO"));
                KeyMapping.click(mc.options.keyAttack.getKey());
            }
        }, mc -> mc.getSingleplayerServer().execute(() ->
                report.add("  after the click the staff is falling: " + (staff != null && staff.isFalling() ? "yes" : "NO")))));
        // ── Tooltips, as the client builds them ──
        all.add(new Step("tooltips for a Heavenly at level 1", p -> {
            become(p, ReincarnationOutcome.MARSHAL_HEAVENLY_CANOPY);
        }, 20, (mc, tick) -> {}, mc -> {
            report.add("  client: outcome '" + mc.player.getPersistentData().getString(AlignmentData.KEY_REINCARNATION_OUTCOME)
                    + "', class " + com.jttw.engine.progression.level.PlayerLevel.getProgressionClass(mc.player)
                    + ", level " + com.jttw.engine.progression.level.PlayerLevel.getLevel(mc.player)
                    + ", STR " + com.jttw.engine.item.WieldRules.strength(mc.player));
            for (ItemStack s : List.of(new ItemStack(ModItems.RUYI_JINGU_BANG.get()), new ItemStack(ModItems.ERLANG_AXE.get()),
                    new ItemStack(ModItems.FIRE_TIP_SPEAR.get()), new ItemStack(ModItems.QIANKUN_RING.get()),
                    YinYangSwordItem.createYin(), new ItemStack(ModItems.HUNTIAN_CLOTH.get()))) {
                List<String> lines = s.getTooltipLines(mc.player, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
                report.add("  " + lines.get(0) + ": " + String.join(" | ", lines.subList(1, lines.size())));
            }
        }));
        // ── First-person pictures ──
        all.add(new Step("Erlang's axe, enlarged", p -> {
            become(p, ReincarnationOutcome.EYE_OF_HEAVEN);
            ItemStack axe = new ItemStack(ModItems.ERLANG_AXE.get());
            ErlangAxeItem.setLarge(axe, true);
            p.setItemInHand(InteractionHand.MAIN_HAND, axe);
            p.teleportTo(p.serverLevel(), 0.5, p.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0), 0.5, -90f, 0f);
        }, 40, (mc, tick) -> {}, mc -> shoot(mc, "erlang_axe_large")));
        all.add(new Step("Erlang's axe, regular", p -> {
            become(p, ReincarnationOutcome.EYE_OF_HEAVEN);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ERLANG_AXE.get()));
        }, 40, (mc, tick) -> {}, mc -> shoot(mc, "erlang_axe_regular")));
        all.add(new Step("Nezha's spear, lit", p -> {
            become(p, ReincarnationOutcome.THIRD_LOTUS_PRINCE);
            ItemStack spear = new ItemStack(ModItems.FIRE_TIP_SPEAR.get());
            FireTipSpearItem.setAflame(spear, true);
            p.setItemInHand(InteractionHand.MAIN_HAND, spear);
        }, 40, (mc, tick) -> {}, mc -> shoot(mc, "fire_tip_spear_lit")));
        all.add(new Step("the swords, one in hand, its partner in the inventory (Wukong: not his)", p -> {
            become(p, ReincarnationOutcome.CELESTIAL_PRIMATE);
            p.setGameMode(GameType.SURVIVAL);
            p.getInventory().selected = 0;
            p.getInventory().items.set(0, YinYangSwordItem.createYin());
            p.getInventory().items.set(6, YinYangSwordItem.createYang());
        }, 40, (mc, tick) -> {}, mc -> {
            mc.getSingleplayerServer().execute(() -> {
                ServerPlayer sp = server(mc);
                report.add("  server: selected " + sp.getInventory().selected + ", main " + sp.getMainHandItem()
                        + " " + sp.getMainHandItem().getTag() + ", off " + sp.getOffhandItem() + ", game mode " + sp.gameMode.getGameModeForPlayer()
                        + ", frozen " + sp.getTicksFrozen() + ", fire " + sp.getRemainingFireTicks());
            });
            report.add("  client: selected " + mc.player.getInventory().selected + ", main " + mc.player.getMainHandItem());
            ItemStack off = mc.player.getOffhandItem();
            report.add("  the client sees the partner drawn into the off hand: " + (YinYangSwordItem.isYang(off) ? "yes" : "NO"));
            report.add("  frostbitten and burning: " + (mc.player.isFullyFrozen() && mc.player.isOnFire() ? "yes" : "NO"));
            shoot(mc, "yin_yang_swords_paired");
        }));
        return all;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ENABLED) return;
        Minecraft mc = Minecraft.getInstance();
        if (!worldRequested) {
            if (mc.level == null && mc.screen != null && mc.getOverlay() == null) {
                worldRequested = true;
                mc.options.onboardAccessibility = false;
                GameRules rules = new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
                LevelSettings settings = new LevelSettings("jttw_playtest_" + System.currentTimeMillis(), GameType.CREATIVE,
                        false, Difficulty.PEACEFUL, true, rules, WorldDataConfiguration.DEFAULT);
                mc.createWorldOpenFlows().createFreshLevel(settings.levelName(), settings, new WorldOptions(7L, false, false),
                        access -> access.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT)
                                .value().createWorldDimensions());
            }
            return;
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen != null) mc.setScreen(null);
        if (mc.player.tickCount < 60) return;
        if (steps == null) steps = steps();

        if (index < 0 || (settle == 0 && t >= steps.get(index).ticks())) {
            if (index >= 0) {
                steps.get(index).end().accept(mc);
                mc.getSingleplayerServer().execute(() -> reset(server(mc)));
            }
            index++;
            if (index >= steps.size()) {
                finish(mc);
                return;
            }
            settle = 20; // every step, the first too, starts from a settled, reset player
            return;
        }
        if (settle > 0) {
            if (--settle == 0) {
                Step s = steps.get(index);
                report.add(s.name() + ":");
                speeds.clear();
                lastPos = null;
                t = 0;
                mc.getSingleplayerServer().execute(() -> s.setup().accept(server(mc)));
            }
            return;
        }
        steps.get(index).each().tick(mc, t++);
    }

    private static void finish(Minecraft mc) {
        index = Integer.MAX_VALUE / 2;
        // Give the server a moment to report the last step, then write and quit
        mc.getSingleplayerServer().execute(() -> mc.execute(() -> {
            File out = new File(mc.gameDirectory, RUN + ".txt");
            try {
                synchronized (report) { Files.write(out.toPath(), report); }
            } catch (IOException e) {
                JTTWEngine.LOGGER.error("ClientPass: could not write the report", e);
            }
            synchronized (report) { report.forEach(line -> JTTWEngine.LOGGER.info("ClientPass: {}", line)); }
            mc.stop();
        }));
    }
}
