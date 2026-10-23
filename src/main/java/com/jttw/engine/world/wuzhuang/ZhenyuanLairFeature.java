package com.jttw.engine.world.wuzhuang;

import com.jttw.engine.registry.ModEntities;
import com.jttw.engine.world.Zones;
import com.jttw.engine.world.feature.ZoneLairFeature;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The Great Immortal Zhenyuan in his Hall of Heaven and Earth, before the altar, at the heart of
 * each Mount Wuzhuang region: put there once, as the land is made ({@link ZoneLairFeature}). The
 * hall itself, and his household, are laid by {@link MountWuzhuangFeature}, which runs before this.
 */
public class ZhenyuanLairFeature extends ZoneLairFeature {

    public ZhenyuanLairFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    protected Zones.Zone zone() {
        return Zones.MOUNT_WUZHUANG;
    }

    @Override
    protected EntityType<? extends Mob> boss() {
        return ModEntities.ZHENYUAN.get();
    }

    /** The hall's floor: he stands on it, before the altar. */
    @Override
    protected int floorAt(WorldGenLevel level, int x, int z) {
        return IslandShape.TERRACE + Temple.ZHENYUAN_Y - 1;
    }

    @Override
    protected void build(WorldGenLevel level, BlockPos centre, RandomSource random) {
        // the hall is the island's: nothing more to build
    }
}
