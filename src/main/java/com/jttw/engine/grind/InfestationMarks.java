package com.jttw.engine.grind;

import com.jttw.engine.village.TangVillagePiece;
import com.jttw.engine.village.raid.RaidedVillage;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * The marks of an infested wild ({@link Infestation}), so none walks in unwarned: along its edge, in
 * about {@value #RIM_PERCENT}% of the chunks it crosses, a skull totem over a heap of bone (as the
 * raided villages' raiders raise), every third with a red sign beside it naming what haunts it and its
 * levels; within, now and then, the bones of what they ate and a skull. Placed in every overworld
 * chunk ({@code neoforge/biome_modifier/infestation_marks.json}); a zone's ground never has one, its
 * biomes being none of the overworld's.
 */
public class InfestationMarks extends Feature<NoneFeatureConfiguration> {

    /** How near its edge (either way) a chunk's middle must be to be marked; the share marked; the share within that has bones. */
    public static final int RIM = 10, RIM_PERCENT = 70, BONES_PERCENT = 25;

    public InfestationMarks(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        try {
            return mark(ctx);
        } catch (RuntimeException e) {
            // never let a fault here kill the chunk's generation (a chunk left unmade is never saved, and the server never stops)
            com.jttw.engine.JTTWEngine.LOGGER.error("Infestation marks failed at {}", ctx.origin(), e);
            return false;
        }
    }

    private boolean mark(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        BlockPos origin = ctx.origin();
        int cx = (origin.getX() >> 4 << 4) + 8, cz = (origin.getZ() >> 4 << 4) + 8;
        Infestation.Ground ground = groundOf(level, server, ctx.chunkGenerator(), server.getChunkSource().randomState().sampler(), server.getSeaLevel() + 8);
        Infestation.Patch patch = Infestation.nearest(ground, cx, cz);
        if (patch == null) return false;
        double d = patch.distance(cx, cz);
        int roll = Math.floorMod((int) Mth.getSeed(cx, 91, cz) ^ (int) level.getSeed(), 100);
        if (Math.abs(d - patch.radius()) <= RIM) {
            if (roll >= RIM_PERCENT) return false;
            int x = (origin.getX() >> 4 << 4) + 4 + Math.floorMod(roll * 7, 8), z = (origin.getZ() >> 4 << 4) + 4 + Math.floorMod(roll * 13, 8);
            BlockPos at = surface(level, x, z);
            if (at == null) return false;
            // facing out of the patch, the way one comes
            float out = (float) Math.toDegrees(Math.atan2(x - patch.x(), patch.z() - z));
            int rot = Math.floorMod(Math.round(out / 22.5f), 16);
            RaidedVillage.totem(level, at, rot);
            if (roll % 3 == 0) {
                BlockPos sign = at.relative(net.minecraft.core.Direction.fromYRot(out + 90));
                if (level.getBlockState(sign).isAir() && level.getBlockState(sign.below()).blocksMotion()) {
                    level.setBlock(sign, Blocks.DARK_OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot), Block.UPDATE_CLIENTS);
                    TangVillagePiece.writeSign(level, sign, DyeColor.ORANGE, signLines(patch));
                }
            }
            return true;
        }
        if (d < patch.radius() - RIM && roll < BONES_PERCENT) {
            BlockPos at = surface(level, (origin.getX() >> 4 << 4) + Math.floorMod(roll * 5, 16), (origin.getZ() >> 4 << 4) + Math.floorMod(roll * 11, 16));
            if (at == null) return false;
            level.setBlock(at.below(), Blocks.BONE_BLOCK.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(at, Blocks.SKELETON_SKULL.defaultBlockState().setValue(SkullBlock.ROTATION, roll % 16), Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }

    /** The lines of a patch's signs: what haunts it, its levels. */
    public static String[] signLines(Infestation.Patch patch) {
        return new String[]{patch.theme().chinese, "Infested:", patch.theme().english, "Lv " + patch.band().lo() + "–" + patch.band().hi()};
    }

    /** The open ground at (x, z): the first air over solid, dry ground; or null (water, nothing to stand on). */
    static BlockPos surface(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos at = new BlockPos(x, y, z);
        if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).blocksMotion() || !level.getFluidState(at.below()).isEmpty()) return null;
        for (int up = 1; up <= 4; up++) if (!level.getBlockState(at.above(up)).isAir() && !level.getBlockState(at.above(up)).is(net.minecraft.tags.BlockTags.LEAVES)) return null;
        return at;
    }

    static Infestation.Ground groundOf(WorldGenLevel level, ServerLevel server, net.minecraft.world.level.chunk.ChunkGenerator gen, Climate.Sampler sampler, int y) {
        BiomeSource source = gen.getBiomeSource();
        long seed = level.getSeed();
        RegistryAccess access = level.registryAccess();
        return new Infestation.Ground() {
            public long seed() { return seed; }
            public Holder<Biome> biomeAt(int x, int z) { return source.getNoiseBiome(x >> 2, y >> 2, z >> 2, sampler); }
            public RegistryAccess registries() { return access; }
            public boolean villageStands(net.minecraft.world.level.levelgen.structure.StructureSet set, net.minecraft.world.level.ChunkPos at) {
                return com.jttw.engine.grind.VillageClearance.stands(server, gen, set, at);
            }
        };
    }
}
