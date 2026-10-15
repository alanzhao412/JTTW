package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.entity.ridge.RidgeMonster;
import com.jttw.engine.registry.ModEffects;
import com.jttw.engine.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

/**
 * The Curse of the Dead, laid on whoever kills Lady White Bone: for ten days, at every nightfall,
 * Bone Servants and Wraiths rise round the cursed wherever they are, in any biome or world. It
 * outlasts death and milk; only the ten days end it.
 *
 * <p>Kept as the game time it ends at (so changing the time of day neither shortens nor stretches
 * it) and the night it last struck (counted by the overworld's day, so it strikes once a night).
 * The effect icon shows it and is put back if anything takes it off.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class CurseOfTheDead {

    private CurseOfTheDead() {}

    /** Ten Minecraft days. */
    public static final long LASTS = 10 * 24000L;
    /** Night, for the curse: from sunset until dawn (day time within a day). */
    public static final long DUSK = 12000, DAWN = 23000;
    /** Each nightfall's rising: so many of each. */
    public static final int SERVANTS = 2, WRAITHS = 2;
    /** How far from the cursed the dead rise. */
    public static final int NEAREST = 8, FARTHEST = 14;

    public static final String KEY_UNTIL = "jttw_curse_dead_until", KEY_LAST_NIGHT = "jttw_curse_dead_night";

    /** Lays the curse (or lays it afresh, for ten days from now). */
    public static void lay(ServerPlayer p) {
        p.getPersistentData().putLong(KEY_UNTIL, p.serverLevel().getGameTime() + LASTS);
        // the night she fell does not count: the dead first come at the next nightfall
        p.getPersistentData().putLong(KEY_LAST_NIGHT, night(overworldDayTime(p)));
        showIcon(p);
        p.sendSystemMessage(Component.literal("§5The Curse of the Dead is upon you. §7For ten days, the dead will find you every night."));
        p.level().playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 0.6f, 0.6f);
    }

    public static boolean isCursed(ServerPlayer p) {
        return p.getPersistentData().getLong(KEY_UNTIL) > p.serverLevel().getGameTime();
    }

    /** Ticks of curse left. */
    public static long remaining(ServerPlayer p) {
        return Math.max(0, p.getPersistentData().getLong(KEY_UNTIL) - p.serverLevel().getGameTime());
    }

    /** Which night a day time falls in: the night that begins on day n is night n (a time before dawn belongs to the one before). */
    public static long night(long dayTime) {
        return Math.floorDiv(dayTime - DUSK, 24000L);
    }

    public static boolean isNight(long dayTime) {
        long t = Math.floorMod(dayTime, 24000L);
        return t >= DUSK && t < DAWN;
    }

    private static long overworldDayTime(ServerPlayer p) {
        return p.server.overworld().getDayTime();
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 20 != 0) return;
        if (!p.getPersistentData().contains(KEY_UNTIL)) return;
        check(p, overworldDayTime(p));
    }

    /**
     * One look at a cursed player, at this day time: the curse ends if its days are up, else the
     * icon is kept and, the first time in a night, the dead rise. Returns those that rose.
     */
    public static List<RidgeMonster> check(ServerPlayer p, long dayTime) {
        if (!isCursed(p)) {
            p.getPersistentData().remove(KEY_UNTIL);
            p.getPersistentData().remove(KEY_LAST_NIGHT);
            p.removeEffect(ModEffects.CURSE_OF_THE_DEAD);
            p.sendSystemMessage(Component.literal("§7The Curse of the Dead lifts. The dead rest."));
            return List.of();
        }
        showIcon(p);
        if (!isNight(dayTime) || night(dayTime) == p.getPersistentData().getLong(KEY_LAST_NIGHT)) return List.of();
        p.getPersistentData().putLong(KEY_LAST_NIGHT, night(dayTime));
        return rise(p);
    }

    /** The dead rise round the player, and come for them. */
    public static List<RidgeMonster> rise(ServerPlayer p) {
        ServerLevel level = p.serverLevel();
        List<RidgeMonster> risen = new ArrayList<>();
        for (int i = 0; i < SERVANTS + WRAITHS; i++) {
            EntityType<? extends RidgeMonster> type = i < SERVANTS ? ModEntities.BONE_SERVANT.get() : ModEntities.WRAITH.get();
            BlockPos at = groundNear(level, p);
            RidgeMonster m = RidgeMonster.summon(level, type, at, level.random.nextFloat() < RidgeMonster.CAPTAIN_CHANCE);
            if (m == null) continue;
            m.setTarget(p);
            risen.add(m);
            if (m.isCaptain()) m.bringEscorts(level);
        }
        p.sendSystemMessage(Component.literal("§5The dead have followed you."));
        level.playSound(null, p.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.HOSTILE, 1f, 0.5f);
        return risen;
    }

    private static BlockPos groundNear(ServerLevel level, ServerPlayer p) {
        BlockPos best = p.blockPosition();
        for (int attempt = 0; attempt < 12; attempt++) {
            double a = level.random.nextDouble() * Math.PI * 2;
            double d = NEAREST + level.random.nextDouble() * (FARTHEST - NEAREST);
            int x = (int) Math.floor(p.getX() + Math.cos(a) * d), z = (int) Math.floor(p.getZ() + Math.sin(a) * d);
            // near the player's own height: not on a roof far above them or the bottom of a ravine
            BlockPos at = new BlockPos(x, (int) Math.floor(p.getY()), z);
            BlockPos.MutableBlockPos q = at.mutable();
            for (int dy = 3; dy >= -6; dy--) {
                q.setY(at.getY() + dy);
                if (level.getBlockState(q.below()).blocksMotion() && level.getBlockState(q).getCollisionShape(level, q).isEmpty()
                        && level.getBlockState(q.above()).getCollisionShape(level, q.above()).isEmpty()) return q.immutable();
            }
            best = at;
        }
        return best;
    }

    private static void showIcon(ServerPlayer p) {
        int left = (int) Math.min(Integer.MAX_VALUE, remaining(p));
        MobEffectInstance on = p.getEffect(ModEffects.CURSE_OF_THE_DEAD);
        if (on == null || Math.abs(on.getDuration() - left) > 40) {
            p.removeEffect(ModEffects.CURSE_OF_THE_DEAD);
            p.addEffect(new MobEffectInstance(ModEffects.CURSE_OF_THE_DEAD, left, 0, false, false, true));
        }
    }

    /** The curse outlasts death. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        var from = event.getOriginal().getPersistentData();
        var to = event.getEntity().getPersistentData();
        for (String key : new String[]{KEY_UNTIL, KEY_LAST_NIGHT}) if (from.contains(key)) to.putLong(key, from.getLong(key));
    }
}
