package com.jttw.engine.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.HashMap;
import java.util.Map;

/**
 * The creatures that glow for this player alone (see world.PrivateGlow), until when; the game's
 * own glowing outline is drawn round them (see mixin.MinecraftMixin).
 */
@OnlyIn(Dist.CLIENT)
public final class PrivateGlowClient {

    private PrivateGlowClient() {}

    private static final Map<Integer, Long> UNTIL = new HashMap<>();

    public static void show(int ticks, int[] entityIds) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long until = mc.level.getGameTime() + ticks;
        for (int id : entityIds) UNTIL.put(id, until);
    }

    public static boolean glows(Entity e) {
        if (UNTIL.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            UNTIL.clear();
            return false;
        }
        Long until = UNTIL.get(e.getId());
        if (until == null) return false;
        if (mc.level.getGameTime() < until) return true;
        UNTIL.remove(e.getId());
        return false;
    }
}
