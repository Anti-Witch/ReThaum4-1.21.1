package thaumcraft.common.tiles.devices;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.tiles.TileThaumcraft;
import thaumcraft.init.ModBlockEntities;

/**
 * TileCandle - block entity for the 16 dyed candle blocks.
 *
 * Carries no data of its own: like TC4's BlockCandleRenderer the layout of the
 * candle cluster is derived from the block position (position seeded random),
 * and the colour comes from the {@link thaumcraft.common.blocks.basic.BlockCandle}
 * instance. The block entity only exists so that the block can be drawn by
 * CandleRenderer (RenderShape.INVISIBLE) instead of the flat block model.
 */
public class TileCandle extends TileThaumcraft {

    public TileCandle(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CANDLE.get(), pos, state);
    }
}
