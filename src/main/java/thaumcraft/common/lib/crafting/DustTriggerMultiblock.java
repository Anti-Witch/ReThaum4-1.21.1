package thaumcraft.common.lib.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.api.crafting.Part;
import thaumcraft.common.lib.events.ServerEvents;
import thaumcraft.common.lib.events.ToolEvents;
import thaumcraft.common.lib.utils.BlockUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Multiblock dust trigger that transforms a 3D structure into another.
 * 
 * Example: Using salis mundus on nether bricks to create an infernal furnace.
 * 
 * The blueprint is defined as a 3D array of Parts [Y][X][Z]:
 * - Y: Height layers (0 = bottom)
 * - X: Width
 * - Z: Depth
 * 
 * The trigger will try all 4 horizontal rotations to find a valid placement.
 * 
 * Ported to 1.20.1
 */
public class DustTriggerMultiblock implements IDustTrigger {
    
    private final Part[][][] blueprint;
    private final String research;
    private final int ySize;
    private final int xSize;
    private final int zSize;
    
    /**
     * Create a multiblock dust trigger.
     * 
     * @param research Required research key (null for no requirement)
     * @param blueprint 3D array of parts [Y][X][Z]
     */
    public DustTriggerMultiblock(@Nullable String research, Part[][][] blueprint) {
        this.blueprint = blueprint;
        this.research = research;
        this.ySize = blueprint.length;
        this.xSize = blueprint[0].length;
        this.zSize = blueprint[0][0].length;
    }
    
    @Override
    public Placement getValidFace(Level level, Player player, BlockPos pos, Direction face) {
        // Check research requirement
        if (research != null && !ThaumcraftCapabilities.knowsResearch(player, research)) {
            return null;
        }
        
        // Try all possible offsets to find where the clicked block is in the structure
        for (int yy = -ySize; yy <= 0; ++yy) {
            for (int xx = -xSize; xx <= 0; ++xx) {
                for (int zz = -zSize; zz <= 0; ++zz) {
                    BlockPos p2 = pos.offset(xx, yy, zz);
                    Direction f = fitMultiblock(level, p2);
                    if (f != null) {
                        return new Placement(xx, yy, zz, f);
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Check if the multiblock fits at the given position in any rotation.
     * 
     * @param level The world
     * @param pos Bottom-corner position to test
     * @return The facing direction if valid, null otherwise
     */
    @Nullable
    private Direction fitMultiblock(Level level, BlockPos pos) {
        Direction[] horizontals = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
        
        for (Direction face : horizontals) {
            if (checkRotation(level, pos, face)) {
                return face;
            }
        }
        return null;
    }
    
    /**
     * Check if the multiblock matches at the given position with the given rotation.
     */
    private boolean checkRotation(Level level, BlockPos pos, Direction face) {
        for (int y = 0; y < ySize; ++y) {
            Matrix matrix = new Matrix(blueprint[y]);
            // Rotate based on facing: SOUTH=0, WEST=1, NORTH=2, EAST=3
            // We need to rotate (3 - horizontalIndex) times to align with the facing
            matrix.Rotate90DegRight(getRotationCount(face));
            
            for (int x = 0; x < matrix.getRows(); ++x) {
                for (int z = 0; z < matrix.getCols(); ++z) {
                    Part part = matrix.getMatrix()[x][z];
                    if (part != null) {
                        // Y coordinate: blueprint[0] is at top, so invert
                        BlockPos checkPos = pos.offset(x, -y + (ySize - 1), z);
                        BlockState worldState = level.getBlockState(checkPos);
                        
                        if (!matchesSource(part.getSource(), worldState)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }
    
    /**
     * Check if a world block state matches the Part source.
     */
    private boolean matchesSource(Object source, BlockState worldState) {
        if (source == null) {
            return true; // null source matches anything
        }
        
        if (source instanceof Block block) {
            return worldState.is(block);
        }
        
        if (source instanceof BlockState state) {
            return worldState == state;
        }
        
        if (source instanceof MapColor mapColor) {
            // In 1.20.1, we check the map color of the block
            return worldState.getMapColor(null, null) == mapColor;
        }
        
        if (source instanceof ItemStack stack) {
            Block blockFromItem = Block.byItem(stack.getItem());
            return worldState.is(blockFromItem);
        }
        
        return false;
    }
    
    /**
     * Get the number of 90-degree rotations for a facing direction.
     * SOUTH = 0 rotations, WEST = 1, NORTH = 2, EAST = 3
     */
    private int getRotationCount(Direction face) {
        return switch (face) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }
    
    @Override
    public List<BlockPos> sparkle(Level level, Player player, BlockPos pos, Placement placement) {
        BlockPos p2 = pos.offset(placement.xOffset, placement.yOffset, placement.zOffset);
        ArrayList<BlockPos> list = new ArrayList<>();
        
        for (int y = 0; y < ySize; ++y) {
            Matrix matrix = new Matrix(blueprint[y]);
            matrix.Rotate90DegRight(getRotationCount(placement.facing));
            
            for (int x = 0; x < matrix.getRows(); ++x) {
                for (int z = 0; z < matrix.getCols(); ++z) {
                    Part part = matrix.getMatrix()[x][z];
                    if (part != null) {
                        BlockPos p3 = p2.offset(x, -y + (ySize - 1), z);
                        // Only add exposed blocks for particles
                        if (part.getSource() != null && BlockUtils.isBlockExposed(level, p3)) {
                            list.add(p3);
                        }
                    }
                }
            }
        }
        return list;
    }
    
    @Override
    public void execute(Level level, Player player, BlockPos pos, Placement placement, Direction side) {
        if (level.isClientSide()) {
            return;
        }
        
        // Fire crafting event
        // Note: In 1.12.2 this used infernal furnace as the result, but we don't know
        // what the actual result is here. The event is mainly for statistics/advancements.
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, ItemStack.EMPTY, null));
        
        BlockPos p2 = pos.offset(placement.xOffset, placement.yOffset, placement.zOffset);
        
        for (int y = 0; y < ySize; ++y) {
            Matrix matrix = new Matrix(blueprint[y]);
            matrix.Rotate90DegRight(getRotationCount(placement.facing));
            
            for (int x = 0; x < matrix.getRows(); ++x) {
                for (int z = 0; z < matrix.getCols(); ++z) {
                    Part part = matrix.getMatrix()[x][z];
                    if (part == null || part.getTarget() == null) {
                        continue;
                    }
                    
                    BlockPos p3 = p2.offset(x, -y + (ySize - 1), z);
                    final BlockPos finalPos = p3;
                    
                    // Determine the target item/block
                    ItemStack targetStack = getTargetStack(part, placement.facing, side, player);
                    
                    // Get source for the swapper
                    BlockState sourceState = level.getBlockState(p3);
                    
                    // Block the position from being broken during transformation
                    ToolEvents.addBlockedBlock(level, p3);
                    
                    // Schedule the swap with priority-based delay
                    ServerEvents.addRunnableServer(level, () -> {
                        ServerEvents.addSwapper(level, finalPos, sourceState, targetStack, 
                                false, 0, player, true, false, -9999, 
                                false, false, 0, ServerEvents.DEFAULT_PREDICATE, 0.0f);
                        ToolEvents.clearBlockedBlock(level, finalPos);
                    }, part.getPriority());
                }
            }
        }
    }
    
    /**
     * Create the target ItemStack for a part, handling facing blocks.
     */
    private ItemStack getTargetStack(Part part, Direction placementFacing, Direction clickSide, Player player) {
        Object target = part.getTarget();
        
        if (target instanceof Block block) {
            BlockState targetState = block.defaultBlockState();
            
            // Handle horizontal facing blocks
            if (targetState.hasProperty(HorizontalDirectionalBlock.FACING)) {
                Direction facing;
                
                if (part.getApplyPlayerFacing()) {
                    // Use the side the player clicked (or player facing if not horizontal)
                    facing = clickSide.getAxis().isHorizontal() ? clickSide : player.getDirection().getOpposite();
                } else if (part.isOpp()) {
                    facing = placementFacing.getOpposite();
                } else {
                    facing = placementFacing;
                }
                
                targetState = targetState.setValue(HorizontalDirectionalBlock.FACING, facing);
            }
            
            return new ItemStack(block);
        }
        
        if (target instanceof ItemStack stack) {
            return stack.copy();
        }
        
        if (target instanceof BlockState state) {
            return new ItemStack(state.getBlock());
        }
        
        return ItemStack.EMPTY;
    }
}




