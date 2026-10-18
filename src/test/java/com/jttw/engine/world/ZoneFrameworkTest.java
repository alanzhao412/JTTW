package com.jttw.engine.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The zone framework: several zones together, their heights, keys and bands; and the ridge as it always was. */
class ZoneFrameworkTest {

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("jttw_test", name));
    }

    private static Zones.Zone zone(String name, ZoneLayout layout, Zones.Vertical vertical) {
        return Zones.Zone.overworld(biome(name), TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("jttw_test", name + "_hosts")),
                layout, vertical, 40, 50, 100);
    }

    // a crowd of zones, more crowded than the game's, so they would collide if let
    private static final Zones.Zone A = zone("a", ZoneLayout.WHITE_BONE, Zones.Vertical.WHOLE_COLUMN);
    private static final Zones.Zone B = zone("b", new ZoneLayout(1536, 0.8, 300, 60, 0x1111L), Zones.Vertical.WHOLE_COLUMN);
    private static final Zones.Zone CAVE = zone("cave", new ZoneLayout(1024, 0.7, 200, 50, 0x2222L), Zones.Vertical.below(0));
    private static final Zones.Zone SKY = zone("sky", new ZoneLayout(2048, 0.7, 250, 60, 0x3333L), Zones.Vertical.above(220));
    private static final Zones.Zone DEEP = zone("deep", new ZoneLayout(1280, 0.7, 180, 40, 0x4444L), Zones.Vertical.below(-20));
    private static final ZoneMap CROWD = new ZoneMap(List.of(A, B, CAVE, SKY, DEEP));

    @Test
    void theRidgeLiesWhereItAlwaysHas() {
        // fingerprints of the ridge's depth and centres over 80x80 km, taken before the zone framework
        long[][] pinned = {{1L, 0x36338b228ce0863eL, 126058}, {42L, 0xb13cacdabd2daf2L, 131073},
                {987654321L, 0x42d8f854369b4041L, 128478}, {-7L, 0xf67df948e0354448L, 130106}};
        for (long[] pin : pinned) {
            long seed = pin[0], h = 17;
            int in = 0;
            for (int x = -40000; x < 40000; x += 37) for (int z = -40000; z < 40000; z += 41) {
                double d = Zones.WHITE_BONE.depth(seed, x, z);
                if (d > 0) in++;
                h = h * 31 + Double.doubleToLongBits(d);
                int[] c = Zones.WHITE_BONE.centre(seed, x, z);
                h = h * 31 + (c == null ? -1 : c[0] * 65599L + c[1]);
            }
            assertEquals(pin[1], h, "seed " + seed);
            assertEquals(pin[2], in, "seed " + seed);
        }
    }

    @Test
    void theRidgeIsAsItWas() {
        var band = Zones.WHITE_BONE.levelBand();
        assertEquals(Zones.WHITE_BONE_RIDGE.location(), band.biome());
        assertEquals(20, band.minLevel());
        assertEquals(36, band.maxLevel());
        assertEquals(80, band.baseGruntXp());
        assertEquals(Zones.Vertical.WHOLE_COLUMN, Zones.WHITE_BONE.vertical());
        assertEquals(80, Zones.WHITE_BONE.hostY());
        assertSame(ZoneAllies.NONE, Zones.WHITE_BONE.allies(), "allied to no one");
        assertEquals(Zones.WHITE_BONE, Zones.MAP.zones().get(0), "first: it keeps all it rolls");
    }

    @Test
    void zonesSharingHeightsNeverShareGround() {
        int shared = 0, stacked = 0, rawClashes = 0;
        List<Zones.Zone> all = CROWD.zones();
        int[] counts = new int[all.size()];
        for (long seed : new long[]{1L, 42L, 987654321L}) {
            for (int x = -30000; x < 30000; x += 29) for (int z = -30000; z < 30000; z += 53) {
                boolean[] in = new boolean[all.size()];
                for (int i = 0; i < all.size(); i++) {
                    in[i] = CROWD.depth(all.get(i), seed, x, z) > 0;
                    if (in[i]) counts[i]++;
                }
                for (int i = 0; i < all.size(); i++) for (int j = i + 1; j < all.size(); j++) {
                    boolean sameHeights = all.get(i).vertical().overlaps(all.get(j).vertical());
                    if (sameHeights && all.get(i).layout().covers(seed, x, z) && all.get(j).layout().covers(seed, x, z)) rawClashes++;
                    if (!in[i] || !in[j]) continue;
                    if (sameHeights) shared++;
                    else stacked++;
                }
            }
        }
        assertTrue(rawClashes > 0, "left to themselves, these layouts would collide");
        assertEquals(0, shared, "no ground claimed twice at the same heights");
        assertTrue(stacked > 0, "a cave or a sky zone may lie under or over another");
        for (int i = 0; i < counts.length; i++) assertTrue(counts[i] > 0, "every zone keeps some ground: " + all.get(i).biome().location());
    }

    @Test
    void theFirstZoneKeepsAllItRolls() {
        for (int x = -20000; x < 20000; x += 61) for (int z = -20000; z < 20000; z += 67) {
            assertEquals(A.layout().depth(5L, x, z), CROWD.depth(A, 5L, x, z));
        }
    }

    @Test
    void aZoneGivesUpWholeRegionsNeverParts() {
        int given = 0;
        for (int x = -20000; x < 20000; x += 41) for (int z = -20000; z < 20000; z += 43) {
            boolean stands = CROWD.centre(B, 9L, x, z) != null;
            if (CROWD.depth(B, 9L, x, z) > 0) assertTrue(stands, "covered, so its region stands");
            if (B.layout().covers(9L, x, z) && !stands) {
                assertEquals(0, CROWD.depth(B, 9L, x, z), "given up whole");
                given++;
            }
        }
        assertTrue(given > 0, "some given up");
    }

    @Test
    void zoneAtNamesTheZoneAtEachHeight() {
        int found = 0;
        for (int x = -20000; x < 20000; x += 37) for (int z = -20000; z < 20000; z += 41) {
            if (CROWD.depth(CAVE, 3L, x, z) <= 0) continue;
            assertEquals(CAVE, CROWD.zoneAt(3L, x, -10, z), "underground, the cave");
            assertNotEquals(CAVE, CROWD.zoneAt(3L, x, 64, z), "at the surface, not the cave");
            found++;
        }
        assertTrue(found > 0);
    }

    @Test
    void verticalRanges() {
        Zones.Vertical below = Zones.Vertical.below(0), above = Zones.Vertical.above(200);
        assertTrue(below.contains(-1));
        assertFalse(below.contains(0));
        assertTrue(above.contains(200));
        assertFalse(above.contains(199));
        assertFalse(below.overlaps(above));
        assertTrue(Zones.Vertical.WHOLE_COLUMN.overlaps(below));
        assertTrue(Zones.Vertical.WHOLE_COLUMN.overlaps(above));
        assertTrue(Zones.Vertical.below(10).overlaps(Zones.Vertical.below(-30)));
        assertTrue(Zones.Vertical.WHOLE_COLUMN.contains(Integer.MIN_VALUE) && Zones.Vertical.WHOLE_COLUMN.contains(Integer.MAX_VALUE));
    }

    @Test
    void hostKeysAreUniqueAcrossZones() {
        // two zones whose cell numbers xor'd with their salts are equal: the old key ran them together
        Zones.Zone p = zone("p", new ZoneLayout(2048, 0.6, 260, 70, 0x10L), Zones.Vertical.WHOLE_COLUMN);
        Zones.Zone q = zone("q", new ZoneLayout(2048, 0.6, 260, 70, 0x11L), Zones.Vertical.WHOLE_COLUMN);
        long cell0 = p.layout().cellOf(0, 0), cell1 = q.layout().cellOf(2048, 0);
        // cells (0, 0) and (1, 0): their numbers differ in the high word's lowest bit, so pick salts that cancel it
        Zones.Zone r = zone("r", new ZoneLayout(2048, 0.6, 260, 70, cell0 ^ cell1 ^ 0x10L), Zones.Vertical.WHOLE_COLUMN);
        assertEquals(cell0 ^ p.layout().salt(), cell1 ^ r.layout().salt(), "the old keys collide");
        assertNotEquals(p.hostCell(0, 0), r.hostCell(2048, 0), "the new ones do not");
        assertNotEquals(p.hostCell(0, 0), q.hostCell(0, 0), "the same cell, two zones");
        assertEquals(p.hostCell(5, 5), p.hostCell(2000, 2000), "one zone, one cell, one key");
    }

    @Test
    void zonesMustDiffer() {
        assertThrows(IllegalArgumentException.class,
                () -> new ZoneMap(List.of(A, zone("a", new ZoneLayout(512, 0.5, 100, 10, 0x99L), Zones.Vertical.WHOLE_COLUMN))),
                "the same biome twice");
        assertThrows(IllegalArgumentException.class,
                () -> new ZoneMap(List.of(A, zone("z", ZoneLayout.WHITE_BONE, Zones.Vertical.below(0)))),
                "the same salt twice");
        assertDoesNotThrow(() -> new ZoneMap(Zones.ALL), "the game's own");
    }

    @Test
    void aDimensionsZoneHasABandButNoGround() {
        Zones.Zone hell = Zones.Zone.dimension(biome("hell"), 57, 66, 500);
        assertFalse(hell.hasFootprint());
        ZoneMap map = new ZoneMap(List.of(A, hell));
        assertEquals(List.of(A), map.zones(), "not placed in the overworld");
        assertEquals(0, map.depth(hell, 1L, 100, 100));
        assertNull(map.centre(hell, 1L, 100, 100));
        assertEquals(57, hell.levelBand().minLevel());
        assertEquals(66, hell.levelBand().maxLevel());
    }

    @Test
    void anAllianceAndAHostHeightAreTheZones() {
        assertSame(ZoneAllies.PENG, A.alliedTo(ZoneAllies.PENG).allies());
        assertSame(ZoneAllies.NONE, A.allies());
        assertEquals(A.layout(), A.alliedTo(ZoneAllies.PENG).layout());
        assertEquals(-40, CAVE.hostSampledAt(-40).hostY());
    }
}
