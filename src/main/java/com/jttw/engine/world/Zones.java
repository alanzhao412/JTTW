package com.jttw.engine.world;

import com.jttw.engine.JTTWEngine;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;

import java.util.List;

/**
 * The mod's zones: regions of the overworld where one of its biomes replaces vanilla's, each the
 * home of a level band's mobs and boss. No library: the biome is listed among the overworld's at
 * a climate no terrain reaches ({@link #UNREACHABLE}, so it is a known biome with its features
 * and colours, but vanilla never places it), and {@code MultiNoiseBiomeSourceMixin} puts it
 * where a zone covers ground of one of its host biomes.
 *
 * <p>Where zones are follows from the world seed alone: the world is cut into square cells, a
 * share of which hold one zone, a rough circle somewhere inside the cell. Terrain keeps the host
 * biome's shape; the zone changes what grows and lives there.
 */
@EventBusSubscriber(modid = JTTWEngine.MOD_ID)
public final class Zones {

    private Zones() {}

    /** One zone: its biome, the vanilla biomes it may take over, and where its regions lie. */
    public record Zone(ResourceKey<Biome> biome, TagKey<Biome> hosts, ZoneLayout layout) {

        /** Whether this zone covers the block column at (x, z) in a world of this seed. */
        public boolean covers(long seed, int x, int z) {
            return layout.covers(seed, x, z);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(JTTWEngine.MOD_ID, path);
    }

    public static final ResourceKey<Biome> WHITE_BONE_RIDGE = ResourceKey.create(Registries.BIOME, id("white_bone_ridge"));

    /** Levels 21-36: White Bone Ridge, over dry, hilly ground (badlands, savanna, desert). */
    public static final Zone WHITE_BONE = new Zone(WHITE_BONE_RIDGE,
            TagKey.create(Registries.BIOME, id("white_bone_ridge_hosts")), ZoneLayout.WHITE_BONE);

    public static final List<Zone> ALL = List.of(WHITE_BONE);

    /** A climate no terrain reaches: listed here, a biome is known to the overworld but never chosen by it. */
    public static final Climate.ParameterPoint UNREACHABLE = Climate.parameters(10f, 10f, 10f, 10f, 10f, 10f, 0f);

    private static volatile long seed;

    public static long seed() {
        return seed;
    }

    /** For tests; the server sets it as it starts. */
    public static void setSeed(long worldSeed) {
        seed = worldSeed;
    }

    @SubscribeEvent
    public static void onServerAboutToStart(ServerAboutToStartEvent event) {
        seed = event.getServer().getWorldData().worldGenOptions().seed();
    }
}
