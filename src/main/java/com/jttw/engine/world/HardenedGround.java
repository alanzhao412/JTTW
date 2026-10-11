package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.dimension.JTTWDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The mod's own places (Heaven, Hell, and its biomes, White Bone Ridge among them) stand up to the
 * destruction its powers bring, unless the one wielding them is very strong. There, ordinary
 * ground takes {@value #TOUGHER} times its blast resistance, and never less than {@value #FLOOR}
 * (obsidian's is 1200), divided by the square root of its breaker's level power: so stone stands as
 * about 600 against a level-20 character (nothing breaks it), about 90 at level 60 (only the
 * mightiest blasts), and as little as 13 to 35 at levels 80 to 100. A creature's blast (a creeper's)
 * is level 1's. The carving powers (the staff's sweep and drill, the Qi Wave, the axe) are weighed
 * as a blast of {@value #CARVE_POWER} at the block. What is soft there (plants, leaves, glass,
 * snow: blast resistance up to {@value #SOFT}) breaks as anywhere. Mining it is as anywhere.
 */
@Mod.EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class HardenedGround {

    private HardenedGround() {}

    public static final float TOUGHER = 50, FLOOR = 1500, SOFT = 0.5f;
    /** The carving powers, weighed as a blast of this power right at the block. */
    public static final float CARVE_POWER = 8;

    /** How far its breaker's strength wears the hardened ground down: the root of their level power, at least 1. */
    public static double strength(@javax.annotation.Nullable net.minecraft.world.entity.Entity by) {
        if (!(by instanceof net.minecraft.world.entity.player.Player p)) return 1;
        return Math.max(1, Math.sqrt(com.jttw.engine.progression.stats.LevelPower.of(p)));
    }

    /** Whether this place is one of the mod's own: its dimensions, or its biomes. */
    public static boolean hardened(Level level, BlockPos pos) {
        var dim = level.dimension();
        if (dim.equals(JTTWDimensions.HEAVEN) || dim.equals(JTTWDimensions.HELL)) return true;
        return level.getBiome(pos).unwrapKey().map(k -> k.location().getNamespace().equals(JTTWEngine.MOD_ID)).orElse(false);
    }

    /** A block's blast resistance where the ground is hardened, against a breaker of this strength. */
    public static float resistance(float vanilla, double strength) {
        return vanilla <= SOFT ? vanilla : (float) (Math.max(vanilla * TOUGHER, FLOOR) / Math.max(1, strength));
    }

    /** Whether the carving powers, in the hands of a breaker of this strength, carve this block. */
    public static boolean carvable(Level level, BlockPos pos, BlockState state, double strength) {
        return !hardened(level, pos) || breaks(CARVE_POWER, 0, resistance(state.getBlock().getExplosionResistance(), strength));
    }

    /**
     * Whether a blast of {@code power} still breaks, at {@code distance} from its heart, a block of
     * {@code resistance}: as vanilla's blast weakens, by about three quarters of a point a block and
     * by (resistance + 0.3) x 0.3 at the block, from up to 1.3 x its power.
     */
    public static boolean breaks(float power, double distance, float resistance) {
        return 1.3 * power - 0.75 * distance - (resistance + 0.3) * 0.3 > 0;
    }

    /** Every blast, the game's and the mod's: in the mod's places, what the hardened ground withstands stays. */
    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;
        Vec3 heart = event.getExplosion().getPosition();
        float power = ((com.jttw.engine.mixin.ExplosionAccessor) event.getExplosion()).jttw_radius();
        double strength = strength(event.getExplosion().getIndirectSourceEntity());
        event.getAffectedBlocks().removeIf(pos -> {
            if (!hardened(level, pos)) return false;
            BlockState state = level.getBlockState(pos);
            float tough = resistance(state.getExplosionResistance(level, pos, event.getExplosion()), strength);
            return !breaks(power, Math.sqrt(pos.getCenter().distanceToSqr(heart)), tough);
        });
    }
}
