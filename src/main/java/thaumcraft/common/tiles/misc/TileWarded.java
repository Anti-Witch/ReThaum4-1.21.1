package thaumcraft.common.tiles.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.init.ModBlockEntities;

/**
 * TileWarded - Warded blocks (arcane stone, ancient stone variants).
 */
public class TileWarded extends BlockEntity {

    public TileWarded(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WARDED.get(), pos, state);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
    }
}