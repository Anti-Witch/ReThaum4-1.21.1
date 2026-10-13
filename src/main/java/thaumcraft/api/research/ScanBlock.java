package thaumcraft.api.research;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A scan thing that checks for specific blocks in the world.
 */
@SuppressWarnings("deprecation")
public class ScanBlock implements IScanThing {
    
    private final String research;
    private final Block[] blocks;
    
    /**
     * Create a new block scanner with auto-generated research key.
     * @param block The block to scan for
     */
    public ScanBlock(Block block) {
        this("!" + block.builtInRegistryHolder().key().location().toString(), block);
    }
    
    /**
     * Create a new block scanner.
     * @param research The research key to unlock
     * @param blocks The blocks to scan for
     */
    public ScanBlock(String research, Block... blocks) {
        this.research = research;
        this.blocks = blocks;
        
        // Also register item forms of these blocks
        for (Block block : blocks) {
            ItemStack stack = new ItemStack(block);
            if (!stack.isEmpty()) {
                ScanningManager.addScannableThing(new ScanItem(research, stack));
            }
        }
    }
    
    @Override
    public boolean checkThing(Player player, Object obj) {
        if (obj instanceof BlockPos pos) {
            Block worldBlock = player.level().getBlockState(pos).getBlock();
            for (Block block : blocks) {
                if (worldBlock == block) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public String getResearchKey(Player player, Object object) {
        return research;
    }
}




