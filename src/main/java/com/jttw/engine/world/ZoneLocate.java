package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code /locate biome} for a zone, answered from where the zones were placed ({@link Zones#MAP}), at once.
 * Vanilla's search samples the world's biomes outward from the asker on the server's own thread: the
 * zones lie thousands of blocks out (Fengdu and Kunlun beyond its 6400), so it held the server still
 * past its watchdog's minute, and the watchdog brought the server down.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class ZoneLocate {

    private ZoneLocate() {}

    /** {@code locate biome <zone>}, alone or at the end of an {@code execute ... run}. */
    private static final Pattern LOCATE = Pattern.compile("(?:^|\\brun\\s+)locate\\s+biome\\s+(?:" + JTTWEngine.MOD_ID + ":)?([a-z0-9_]+)\\s*$");

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        String input = event.getParseResults().getReader().getString().trim();
        if (input.startsWith("/")) input = input.substring(1);
        Matcher m = LOCATE.matcher(input);
        if (!m.find()) return;
        // a bare name ("locate biome kunlun") is taken for a zone's too; vanilla's own ("desert") is no zone
        Zones.Zone zone = Zones.byId(JTTWEngine.MOD_ID + ":" + m.group(1));
        CommandSourceStack source = event.getParseResults().getContext().getSource();
        if (source.getLevel().dimension() != Level.OVERWORLD) return;
        if (zone == null || !source.hasPermission(2)) return;
        BlockPos from = BlockPos.containing(source.getPosition());
        int[] best = nearest(zone, Zones.seed(), from.getX(), from.getZ());
        if (best == null) return;   // a dimension's zone (Hell, Heaven): vanilla's search, in its own realm
        event.setCanceled(true);
        source.sendSuccess(() -> answer(zone, best, from), false);
    }

    /** The nearest of the zone's centres to x, z ({x, z}), or null if it has none in the Overworld. */
    @Nullable
    public static int[] nearest(Zones.Zone zone, long seed, int x, int z) {
        int[] best = null;
        long bestD = Long.MAX_VALUE;
        for (int[] c : Zones.MAP.placement(seed).of(zone)) {
            long d = (long) (c[0] - x) * (c[0] - x) + (long) (c[1] - z) * (c[1] - z);
            if (d < bestD) { bestD = d; best = c; }
        }
        return best;
    }

    /** Vanilla's answer: "The nearest ... is at [x, ~, z] (n blocks away)", the coordinates a click to go there. */
    static Component answer(Zones.Zone zone, int[] at, BlockPos from) {
        int away = (int) Math.round(Math.hypot(at[0] - from.getX(), at[1] - from.getZ()));
        Component coords = ComponentUtils.wrapInSquareBrackets(Component.translatable("chat.coordinates", at[0], "~", at[1]))
                .withStyle(s -> s.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + at[0] + " ~ " + at[1]))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("chat.coordinates.tooltip"))));
        return Component.translatable("commands.locate.biome.success", zone.biome().location().toString(), coords, away);
    }
}
