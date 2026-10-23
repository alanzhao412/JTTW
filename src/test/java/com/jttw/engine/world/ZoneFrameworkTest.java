package com.jttw.engine.world;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The zone framework: several zones together, their heights, keys and bands; and the ridge as designed. */
class ZoneFrameworkTest {

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("jttw_test", name));
    }

    private static Zones.Zone zone(String name, ZoneLayout layout, Zones.Vertical vertical) {
        return Zones.Zone.overworld(biome(name), TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("jttw_test", name + "_hosts")),
                layout, vertical, 40, 50, 100);
    }

    // a crowd of zones, all in the same ring, so they would collide if let
    private static final Zones.Zone A = zone("a", new ZoneLayout(1000, 1600, 260, 70, 0x10L, false), Zones.Vertical.WHOLE_COLUMN);
    private static final Zones.Zone B = zone("b", new ZoneLayout(1000, 1600, 300, 60, 0x1111L, false), Zones.Vertical.WHOLE_COLUMN);
    private static final Zones.Zone CAVE = zone("cave", new ZoneLayout(1000, 1600, 200, 50, 0x2222L, false), Zones.Vertical.below(0));
    private static final Zones.Zone SKY = zone("sky", new ZoneLayout(1000, 1600, 250, 60, 0x3333L, false), Zones.Vertical.above(220));
    private static final Zones.Zone DEEP = zone("deep", new ZoneLayout(1000, 1600, 180, 40, 0x4444L, false), Zones.Vertical.below(-20));
    private static final Zones.Zone SEAS = zone("seas", new ZoneLayout(1000, 1600, 220, 45, 0x5555L, true), Zones.Vertical.WHOLE_COLUMN);
    private static final ZoneMap CROWD = new ZoneMap(List.of(A, B, CAVE, SKY, DEEP, SEAS));

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
        assertEquals(Zones.WHITE_BONE, Zones.MAP.zones().get(0), "first: it takes the first place its ring offers");
        assertEquals(260, ZoneLayout.WHITE_BONE.radius(), "its size as it always was");
        assertEquals(70, ZoneLayout.WHITE_BONE.wobble());
    }

    @Test
    void zonesSharingHeightsNeverShareGround() {
        int stacked = 0;
        List<Zones.Zone> all = CROWD.zones();
        for (long seed = 1; seed <= 40; seed++) {
            ZoneMap.Placement p = CROWD.placement(seed);
            for (int i = 0; i < all.size(); i++) for (int j = i; j < all.size(); j++) {
                Zones.Zone a = all.get(i), b = all.get(j);
                for (int[] ca : p.of(a)) for (int[] cb : p.of(b)) {
                    if (ca == cb) continue;
                    double d = Math.hypot(ca[0] - cb[0], ca[1] - cb[1]);
                    if (a.vertical().overlaps(b.vertical())) {
                        assertTrue(d >= a.layout().margin() + b.layout().margin() + ZoneMap.GAP, "seed " + seed + ": " + a.biome().location() + " and " + b.biome().location());
                    } else if (d < a.layout().margin() + b.layout().margin()) {
                        stacked++;
                    }
                }
            }
            for (int x = -1700; x < 1700; x += 23) for (int z = -1700; z < 1700; z += 29) {
                for (int i = 0; i < all.size(); i++) for (int j = i + 1; j < all.size(); j++) {
                    if (all.get(i).vertical().overlaps(all.get(j).vertical())) {
                        assertFalse(CROWD.depth(all.get(i), seed, x, z) > 0 && CROWD.depth(all.get(j), seed, x, z) > 0, "ground claimed twice");
                    }
                }
            }
        }
        assertTrue(stacked > 0, "a cave or a sky zone may lie under or over another");
    }

    @Test
    void theFirstZoneTakesTheFirstPlaceItsRingOffers() {
        for (long seed = 1; seed <= 20; seed++) {
            assertArrayEquals(A.layout().candidate(seed, 0, 0, 1000, 1600), CROWD.placement(seed).of(A)[0]);
        }
    }

    @Test
    void zoneAtNamesTheZoneAtEachHeight() {
        int found = 0;
        for (long seed = 1; seed <= 10; seed++) {
            int[] c = CROWD.centre(CAVE, seed, 0, 0);
            for (int x = c[0] - 150; x <= c[0] + 150; x += 10) for (int z = c[1] - 150; z <= c[1] + 150; z += 10) {
                if (CROWD.depth(CAVE, seed, x, z) <= 0) continue;
                assertEquals(CAVE, CROWD.zoneAt(seed, x, -10, z), "underground, the cave");
                assertNotEquals(CAVE, CROWD.zoneAt(seed, x, 64, z), "at the surface, not the cave");
                found++;
            }
        }
        assertTrue(found > 0);
    }

    @Test
    void theSeasTakeAQuarterEach() {
        for (long seed = 1; seed <= 20; seed++) {
            int[][] cs = CROWD.centres(SEAS, seed);
            assertEquals(4, cs.length);
            for (int q = 0; q < 4; q++) {
                assertEquals(q, ZoneLayout.quarterOf(cs[q][0], cs[q][1]), "seed " + seed + ": quarter " + q);
                assertEquals(q, SEAS.layout().cellOf(cs[q][0], cs[q][1]), "its key is its quarter");
            }
        }
        assertEquals(0, A.layout().cellOf(123, -456), "one region, one key");
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
    void zonesMustDiffer() {
        assertThrows(IllegalArgumentException.class,
                () -> new ZoneMap(List.of(A, zone("a", new ZoneLayout(500, 900, 100, 10, 0x99L, false), Zones.Vertical.WHOLE_COLUMN))),
                "the same biome twice");
        assertThrows(IllegalArgumentException.class,
                () -> new ZoneMap(List.of(A, zone("z", A.layout(), Zones.Vertical.below(0)))),
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
