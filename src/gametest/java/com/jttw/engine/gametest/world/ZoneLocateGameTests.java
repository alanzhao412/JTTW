package com.jttw.engine.gametest.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.ZoneLocate;
import com.jttw.engine.world.Zones;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static com.jttw.engine.gametest.GT.*;

/**
 * {@code /locate biome} for a zone answers from where the zones were placed, at once: vanilla's search
 * (the server's own thread, sampling outward from the asker) held the server past its watchdog's minute
 * for a zone thousands of blocks out, and the watchdog brought it down.
 */
@GameTestHolder(JTTWEngine.MOD_ID)
@PrefixGameTestTemplate(false)
public class ZoneLocateGameTests {

    /** An operator's command source at the test's corner, everything it is told kept. */
    private static CommandSourceStack asker(GameTestHelper h, List<String> heard) {
        CommandSource ear = new CommandSource() {
            @Override public void sendSystemMessage(Component c) { heard.add(c.getString()); }
            @Override public boolean acceptsSuccess() { return true; }
            @Override public boolean acceptsFailure() { return true; }
            @Override public boolean shouldInformAdmins() { return false; }
        };
        var at = h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        return new CommandSourceStack(ear, Vec3.atCenterOf(at), Vec2.ZERO, h.getLevel(), 4, "jttw_locator",
                Component.literal("jttw_locator"), h.getLevel().getServer(), null);
    }

    @GameTest(template = PLATFORM)
    public static void locatingAZoneAnswersAtOnceFromItsPlacement(GameTestHelper h) {
        var server = h.getLevel().getServer();
        List<String> heard = new ArrayList<>();
        var src = asker(h, heard);
        var at = h.absolutePos(net.minecraft.core.BlockPos.ZERO);
        for (Zones.Zone zone : new Zones.Zone[]{Zones.KUNLUN, Zones.FENGDU, Zones.MYSTICAL_WATERS}) {
            heard.clear();
            long t0 = System.nanoTime();
            server.getCommands().performPrefixedCommand(src, "locate biome " + zone.id());
            long ms = (System.nanoTime() - t0) / 1_000_000;
            check(h, ms < 500, zone.id() + ": answered in " + ms + " ms");
            int[] c = ZoneLocate.nearest(zone, Zones.seed(), at.getX(), at.getZ());
            check(h, c != null, zone.id() + " has a place in the Overworld");
            check(h, heard.size() == 1 && heard.get(0).contains(zone.id()) && heard.get(0).contains(c[0] + ", ~, " + c[1]),
                    zone.id() + ": told the nearest centre " + c[0] + ", " + c[1] + "; heard " + heard);
        }
        heard.clear();
        server.getCommands().performPrefixedCommand(src, "execute run locate biome jttw_engine:kunlun");
        check(h, heard.size() == 1 && heard.get(0).contains("jttw_engine:kunlun"), "through execute ... run too; heard " + heard);
        // a vanilla biome is vanilla's to look for: handed the parsed command, the zones' answer lets it by
        for (String vanilla : new String[]{"locate biome minecraft:desert", "locate biome desert", "locate structure minecraft:village_plains"}) {
            var event = new net.neoforged.neoforge.event.CommandEvent(server.getCommands().getDispatcher().parse(vanilla, src));
            ZoneLocate.onCommand(event);
            check(h, !event.isCanceled(), "\"" + vanilla + "\" left to vanilla");
        }
        h.succeed();
    }
}
