package com.jttw.engine.world.blackwind;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.blackwind.BlackWhirlwindEntity;
import com.jttw.engine.world.Zones;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The black wind over the mountain (黑风): whirls of it wander the slopes round anyone on Black Wind
 * Mountain, under the open sky, a few at a time ({@link BlackWhirlwindEntity}, harmless ones), drifting
 * across the pines and blowing themselves out.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class BlackWind {

    private BlackWind() {}

    /** How often a new whirl may rise near each player, in ticks; how many at most round one; how far off they rise. */
    public static final int EVERY = 80, MOST = 4;
    public static final double FROM = 14, TO = 44;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerLevel level = event.getServer().overworld();
        if (level.getGameTime() % EVERY != 0) return;
        for (ServerPlayer p : level.players()) {
            if (p.isSpectator() || !Zones.BLACK_WIND.isAt(level, p.blockPosition())) continue;
            if (!level.canSeeSky(p.blockPosition().above())) continue;
            if (level.getEntitiesOfClass(BlackWhirlwindEntity.class, new AABB(p.blockPosition()).inflate(TO + 10), w -> !w.isHostile()).size() >= MOST) continue;
            if (level.random.nextFloat() > 0.6f) continue;
            double a = level.random.nextDouble() * Math.PI * 2, d = FROM + level.random.nextDouble() * (TO - FROM);
            double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
            if (!Zones.BLACK_WIND.isAt(level, new net.minecraft.core.BlockPos((int) Math.floor(x), y, (int) Math.floor(z)))) continue;
            // across the player's way, slowly
            Vec3 drift = new Vec3(-Math.sin(a), 0, Math.cos(a)).scale(0.08 + level.random.nextDouble() * 0.06);
            BlackWhirlwindEntity w = BlackWhirlwindEntity.wandering(level, drift, 300 + level.random.nextInt(300), 1.4f + level.random.nextFloat() * 1.2f);
            w.moveTo(x, y, z);
            level.addFreshEntity(w);
        }
    }
}
