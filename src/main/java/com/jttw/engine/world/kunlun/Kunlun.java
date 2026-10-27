package com.jttw.engine.world.kunlun;

import com.jttw.engine.JTTWEngine;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.KunlunFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Mount Kunlun (昆仑山), the sacred mountain at the world's western end: in the old books the pillar
 * of heaven and the home of the Queen Mother of the West, whose Jade Pool is on it; and in the novel
 * the Banana-Leaf Fan is a treasure that grew behind it when chaos first parted, the very essence of
 * the Great Yin. One a world, a place and not a zone of a band ({@code Zones.KUNLUN}): a massif
 * raised out of mountain country 5-8 km west of spawn ({@link KunlunShape}), the axis of the world,
 * its summit at y 596 under the taller sky new worlds have ({@code TallOverworld}), lesser peaks,
 * gorges, glaciers and belts of cloud round it; a pilgrim's way of stone winding up it, a gate at its
 * foot and pavilions and shrines along it ({@link KunlunWay}); and on its summit the Queen Mother's
 * palace by the Jade Pool ({@link KunlunPalace}),
 * where the Banana-Leaf Fan lies in the treasure chest before the altar: the only one in the world
 * ({@link #TREASURE}). Nothing fights there. The Earth Gods point the way to anyone who asks sneaking
 * ({@code EarthGod}).
 */
public final class Kunlun {

    private Kunlun() {}

    public static final Zones.Zone ZONE = Zones.KUNLUN;

    /** The treasure chest's loot: the Banana-Leaf Fan, always, and a few modest gifts with it. */
    public static final ResourceKey<LootTable> TREASURE = ResourceKey.create(Registries.LOOT_TABLE, JTTWEngine.id("chests/kunlun_treasure"));

    /** The centre of Mount Kunlun's region (its summit), as {x, z}. */
    public static int[] centre(long seed) {
        return ZONE.centres(seed)[0];
    }

    /** The summit's ground (the palace's local origin) in this world: its centre, at the paving's level (nothing is loaded for it). */
    public static BlockPos summit(WorldGenLevel level) {
        int[] c = centre(level.getSeed());
        return new BlockPos(c[0], KunlunFeature.summit(level, c[0], c[1]), c[1]);
    }

    /** Where the treasure chest stands in this world. */
    public static BlockPos chest(WorldGenLevel level) {
        return summit(level).offset(KunlunPalace.CHEST_X, KunlunPalace.CHEST_Y, KunlunPalace.CHEST_Z);
    }
}
