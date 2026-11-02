package com.jttw.engine.village.raid;

import com.jttw.engine.grind.VillageClearance;
import com.jttw.engine.registry.ModGrinds;
import com.jttw.engine.village.TangVillage;
import com.jttw.engine.village.TangVillageStructure;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * A raided village (data: {@code worldgen/structure/raided_village_<variant>.json}, placed by
 * {@code worldgen/structure_set/raided_villages.json}): a Tang village the yaoguai have taken. It
 * stands by a Tang village's rules ({@link TangVillageStructure#site}: its variant's biomes, out of
 * every zone and its margin, on near-level dry ground), and keeps {@value #CLEAR_OF_VILLAGES} chunks
 * clear of vanilla's villages (the set's own exclusion zone keeps it as far from the Tang villages').
 */
public class RaidedVillageStructure extends Structure {

    public static final MapCodec<RaidedVillageStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            TangVillage.Variant.CODEC.fieldOf("variant").forGetter(s -> s.variant)
    ).apply(i, RaidedVillageStructure::new));

    /** How many chunks it keeps from any of vanilla's villages. */
    public static final int CLEAR_OF_VILLAGES = 8;

    private final TangVillage.Variant variant;

    public RaidedVillageStructure(StructureSettings settings, TangVillage.Variant variant) {
        super(settings);
        this.variant = variant;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        if (VillageClearance.near(ctx.registryAccess(), VillageClearance.VANILLA_VILLAGES, ctx.seed(), ctx.chunkPos(), CLEAR_OF_VILLAGES, null)) {
            return Optional.empty();
        }
        Optional<BlockPos> site = TangVillageStructure.site(ctx);
        if (site.isEmpty()) return Optional.empty();
        BlockPos c = site.get();
        long layout = ctx.random().nextLong();
        return Optional.of(new GenerationStub(c, b -> b.addPiece(new RaidedVillagePiece(variant, c.getX(), c.getY(), c.getZ(), layout))));
    }

    @Override
    public StructureType<?> type() {
        return ModGrinds.RAIDED_VILLAGE.get();
    }
}
