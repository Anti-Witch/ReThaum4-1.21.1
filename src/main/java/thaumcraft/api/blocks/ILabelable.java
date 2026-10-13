package thaumcraft.api.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * @author Azanor
 * 
 * Tile entities or blocks that extend this interface can have jar labels applied to them.
 */
public interface ILabelable {

    /**
     * This method is used by the block or tile entity to do whatever needs doing.
     * @param player The player applying the label
     * @param pos The position of the block
     * @param side The side of the block being labeled
     * @param labelStack The label item stack being applied
     * @return true if the label was successfully applied (label will be consumed)
     */
    boolean applyLabel(Player player, BlockPos pos, Direction side, ItemStack labelStack);
}




