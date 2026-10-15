package com.jttw.engine.gametest.bench;

import com.jttw.engine.entity.PlacedStaffEntity;
import com.jttw.engine.entity.QiWaveEntity;
import com.jttw.engine.progression.level.PlayerLevel;
import com.jttw.engine.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The cost of the abilities that break terrain ({@code -Pbench.mode=terrain}). Each runs on fresh,
 * loaded ground, and is measured for 10 seconds from when it starts: the server's slowest tick,
 * its average, and the items left lying about.
 */
final class TerrainBench {

    private TerrainBench() {}

    private static final int LOAD_TICKS = 200;
    private static final int MEASURE_TICKS = 200;

    /** One ability, started on the tick {@code start} runs; {@code each} runs on every measured tick. */
    private record Scenario(String name, Consumer<ServerPlayer> start, Consumer<ServerPlayer> each) {}

    private static final List<String> report = new ArrayList<>();
    private static List<Scenario> scenarios;
    private static int index = -1;
    private static int ticks;
    private static boolean measuring;
    private static long lastNanos;
    private static double sumMs, maxMs, firstMs;
    private static int samples;
    private static double siteX;
    private static PlacedStaffEntity staff;

    /** Where each kind of stray item came from, the first time it appeared (mob loot aside). */
    private static final java.util.Map<String, String> ORIGINS = new java.util.LinkedHashMap<>();

    static void onItemJoin(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!measuring || event.loadedFromDisk() || !(event.getEntity() instanceof ItemEntity item)) return;
        String kind = scenarios.get(index).name().substring(0, 8) + ": " + item.getItem().getItem().toString();
        if (ORIGINS.containsKey(kind)) return;
        StackTraceElement[] trace = new Throwable().getStackTrace();
        StringBuilder sb = new StringBuilder();
        boolean loot = false;
        boolean past = false;
        int shown = 0;
        for (StackTraceElement e : trace) {
            String c = e.getClassName();
            if (e.getMethodName().contains("dropAllDeathLoot") || e.getMethodName().contains("dropFromLootTable")) loot = true;
            if (!past) { past = e.getMethodName().equals("addFreshEntity"); continue; } // the spawn itself
            if ((c.startsWith("net.minecraft") || c.startsWith("com.jttw")) && shown < 10) {
                sb.append(" <- ").append(c.substring(c.lastIndexOf('.') + 1)).append('.').append(e.getMethodName());
                shown++;
            }
        }
        if (!loot) ORIGINS.put(kind, sb.toString());
    }

    static List<String> report() {
        ORIGINS.forEach((kind, from) -> report.add("  " + kind + " came from" + from));
        return report;
    }

    private static List<Scenario> scenarios() {
        List<Scenario> all = new ArrayList<>();
        all.add(new Scenario("staff planted and grown to 60x, one scroll a tick", p -> {
            double y = p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) siteX + 6, 0);
            staff = new PlacedStaffEntity(p.level(), siteX + 6.5, y, 0.5, p.getUUID());
            p.level().addFreshEntity(staff);
        }, p -> {
            if (staff.getStaffScale() < 60) staff.adjustScale(1f);
        }));
        all.add(new Scenario("staff toppled at 60x", p -> {
            double y = p.serverLevel().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) siteX + 6, 0);
            staff = new PlacedStaffEntity(p.level(), siteX + 6.5, y, 0.5, p.getUUID());
            staff.setStaffScale(60);
            p.level().addFreshEntity(staff);
            staff.toppleBy(p);
        }, p -> {}));
        all.add(new Scenario("one blast of power 36 (the staff's at 60x, near the tip)", p -> {
            long t = System.nanoTime();
            var ex = com.jttw.engine.world.TerrainBreaker.explode(p.serverLevel(), p, siteX + 10, p.getY(), 0.5, 36f);
            report.add(String.format("  that blast: %.0f ms to compute, %d blocks in it",
                    (System.nanoTime() - t) / 1e6, ex == null ? 0 : ex.getToBlow().size()));
        }, p -> {}));
        all.add(new Scenario("Qi Wave, ATK 40, full charge", p -> {
            QiWaveEntity wave = new QiWaveEntity(p.level(), p, p.getLookAngle(), 40f);
            wave.setChargePower(1f);
            p.level().addFreshEntity(wave);
        }, p -> {}));
        all.add(new Scenario("Erlang axe cleave, level 100, full charge", p -> {
            PlayerLevel.setLevel(p, 100);
            // The cleave is Erlang's alone
            com.jttw.engine.reincarnation.AlignmentData.markReincarnated(p,
                    com.jttw.engine.reincarnation.ReincarnationOutcome.EYE_OF_HEAVEN);
            ItemStack axe = new ItemStack(ModItems.ERLANG_AXE.get());
            p.getPersistentData().remove("jttw_axe_cleave_ready_at");
            axe.releaseUsing(p.level(), p, axe.getUseDuration() - 60);
        }, p -> {}));
        return all;
    }

    /** One server tick of the bench. Returns true when every scenario has run. */
    static boolean tick(ServerPlayer p) {
        if (scenarios == null) scenarios = scenarios();
        ServerLevel level = p.serverLevel();
        long now = System.nanoTime();
        if (measuring && lastNanos != 0) {
            double ms = (now - lastNanos) / 1e6;
            if (samples == 0) firstMs = ms;
            sumMs += ms;
            maxMs = Math.max(maxMs, ms);
            samples++;
        }
        lastNanos = now;
        ticks++;

        if (index < 0 || (measuring && ticks >= MEASURE_TICKS)) {
            if (index >= 0) finish(level);
            index++;
            if (index >= scenarios.size()) return true;
            // Fresh ground for each, east of the last, given time to load
            siteX = 4000 + index * 1000;
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) siteX, 0) + 2;
            p.getAbilities().flying = true;
            p.onUpdateAbilities();
            p.teleportTo(level, siteX, Math.max(y, 80), 0.5, -90f, 0f);
            measuring = false;
            ticks = 0;
            return false;
        }
        Scenario s = scenarios.get(index);
        if (!measuring) {
            // Wait until the ground around is all there: a blast reaching ungenerated land would
            // make the server generate it mid-blast, and that is world generation, not the ability
            if (ticks < LOAD_TICKS || (!aroundLoaded(level) && ticks < LOAD_TICKS * 10)) return false;
            if (!aroundLoaded(level)) report.add("  (site " + (int) siteX + " still not fully loaded)");
            // Stand on the ground, facing east
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) siteX, 0);
            p.teleportTo(level, siteX, y, 0.5, -90f, 0f);
            measuring = true;
            ticks = 0;
            samples = 0;
            sumMs = maxMs = firstMs = 0;
            s.start().accept(p);
            return false;
        }
        s.each().accept(p);
        return false;
    }

    /** Every chunk within 10 chunks of the site is loaded. */
    private static boolean aroundLoaded(ServerLevel level) {
        int cx = ((int) siteX) >> 4;
        for (int x = cx - 10; x <= cx + 10; x++) for (int z = -10; z <= 10; z++) {
            if (level.getChunkSource().getChunkNow(x, z) == null) return false;
        }
        return true;
    }

    private static void finish(ServerLevel level) {
        AABB around = new AABB(siteX - 300, -64, -300, siteX + 300, 320, 300);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, around, e -> true);
        int stacks = items.size();
        java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
        for (ItemEntity i : items) kinds.merge(i.getItem().getItem().toString(), 1, Integer::sum);
        report.add(String.format("%s: start tick %.0f ms; then %.0f ms/tick average, %.0f worst over %d ticks; %d items left lying %s",
                scenarios.get(index).name(), firstMs, sumMs / Math.max(1, samples), maxMs, samples, stacks, kinds));
        items.forEach(ItemEntity::discard); // so the next scenario starts clean
    }
}
