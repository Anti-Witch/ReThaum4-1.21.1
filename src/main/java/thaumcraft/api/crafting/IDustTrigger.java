package thaumcraft.api.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Interface for salis mundus dust triggers.
 * When a player uses salis mundus on a block, all registered triggers are checked
 * to see if they match. If a match is found, the trigger is executed.
 * 
 * Used for:
 * - Creating multiblock structures (infernal furnace, arcane bore, etc.)
 * - Simple block transformations
 * 
 * Ported to 1.20.1
 */
public interface IDustTrigger {
    
    /**
     * Check if using dust on the given location and face will result in a valid operation.
     * Called on both client and server.
     * 
     * @param level The world
     * @param player The player using the dust
     * @param pos The clicked block position
     * @param face The clicked face
     * @return A Placement if valid, null if not valid
     */
    Placement getValidFace(Level level, Player player, BlockPos pos, Direction face);
    
    /**
     * Execute the trigger operation.
     * Called on both client and server after validation.
     * 
     * @param level The world
     * @param player The player
     * @param pos The clicked block position
     * @param placement The placement info from getValidFace
     * @param side The clicked face
     */
    void execute(Level level, Player player, BlockPos pos, Placement placement, Direction side);
    
    /**
     * Get the list of block positions that should display sparkle effects.
     * By default returns just the clicked block.
     * 
     * @param level The world
     * @param player The player
     * @param pos The clicked block position
     * @param placement The placement info
     * @return List of positions to sparkle
     */
    default List<BlockPos> sparkle(Level level, Player player, BlockPos pos, Placement placement) {
        return Collections.singletonList(pos);
    }
    
    /**
     * Placement information returned by getValidFace.
     * Contains offset from clicked position and facing direction.
     */
    class Placement {
        public int xOffset, yOffset, zOffset;
        public Direction facing;
        
        public Placement(int xOffset, int yOffset, int zOffset, Direction facing) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.zOffset = zOffset;
            this.facing = facing;
        }
    }
    
    // === Static Registry ===
    
    /** List of all registered dust triggers */
    ArrayList<IDustTrigger> triggers = new ArrayList<>();
    
    /**
     * Register a dust trigger.
     * @param trigger The trigger to register
     */
    static void registerDustTrigger(IDustTrigger trigger) {
        triggers.add(trigger);
    }
    
    /**
     * Get all registered triggers.
     */
    static List<IDustTrigger> getTriggers() {
        return triggers;
    }
    
    /**
     * Clear all registered triggers (for reloading).
     */
    static void clearTriggers() {
        triggers.clear();
    }
}




