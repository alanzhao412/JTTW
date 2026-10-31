package com.jttw.engine.client;

import com.jttw.engine.world.Zones;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The clouds over the dark zones: tinted to their skies, faded in by weight, night and rain kept. */
class ZoneCloudsTest {

    private static final Vec3 DAY = new Vec3(1, 1, 1);

    @Test
    void noWeightLeavesVanillasCloudsAndFullWeightTintsThem() {
        Vec3 bw = ZoneClouds.TINTS.get(Zones.BLACK_WIND_BIOME);
        assertEquals(DAY, ZoneClouds.blend(DAY, bw, 0));
        assertTrue(bw.distanceTo(ZoneClouds.blend(DAY, bw, 1)) < 1e-9, "wholly in: the tint");
        Vec3 half = ZoneClouds.blend(DAY, bw, 0.5);
        assertTrue(half.x < 1 && half.x > bw.x, "half-way over the border, half-way tinted: " + half);
        Vec3 night = new Vec3(0.1, 0.1, 0.15);
        assertEquals(night.x * bw.x, ZoneClouds.blend(night, bw, 1).x, 1e-9, "night still darkens them");
        assertTrue(bw.distanceTo(ZoneClouds.blend(DAY, bw, 3)) < 1e-9, "weight clamped");
    }

    @Test
    void eachDarkZoneHasItsSkysTint() {
        Vec3 bw = ZoneClouds.TINTS.get(Zones.BLACK_WIND_BIOME), fire = ZoneClouds.TINTS.get(Zones.FIRE_MOUNTAIN_BIOME),
                silk = ZoneClouds.TINTS.get(Zones.SPIDER_CAVE_BIOME);
        assertNotNull(ZoneClouds.TINTS.get(Zones.FENGDU_BIOME), "the Ghost City's clouds too");
        for (Vec3 t : ZoneClouds.TINTS.values()) assertTrue(bw.length() <= t.length(), "Black Wind's ash the darkest");
        assertTrue(bw.x < 0.4 && bw.y < 0.4 && bw.z < 0.4, "Black Wind: dark, ashy");
        assertTrue(fire.x > fire.y && fire.y > fire.z, "the Flaming Mountains: smoky orange");
        assertTrue(silk.z > silk.x && silk.z > silk.y && silk.length() < DAY.length() * 0.8, "Silk-Web: violet-grey");
        assertNull(ZoneClouds.TINTS.get(Zones.MOUNT_WUZHUANG_BIOME), "Mount Wuzhuang's clouds stay white");
        assertNull(ZoneClouds.TINTS.get(Zones.FLOWER_FRUIT_BIOME), "and Flower-Fruit's");
    }
}
