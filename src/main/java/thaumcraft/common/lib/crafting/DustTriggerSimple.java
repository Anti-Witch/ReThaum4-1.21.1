package thaumcraft.common.lib.crafting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import thaumcraft.api.capabilities.ThaumcraftCapabilities;
import thaumcraft.api.crafting.IDustTrigger;
import thaumcraft.common.lib.events.ServerEvents;

import javax.annotation.Nullable;

/**
 * Simple dust trigger that transforms a single block into another.
 * 
 * Example: Using salis mundus on a cauldron to create a crucible.
 * 
 * Ported to 1.20.1
 */
public class DustTriggerSimple implements IDustTrigger {
    
    private final Block target;
    private final ItemStack result;
    private final String research;
    
    /**
     * Create a simple dust trigger.
     * 
     * @param research Required research key (null for no requirement)
     * @param target The block to trigger on
     * @param result The item/block to transform into
     */
    public DustTriggerSimple(@Nullable String research, Block target, ItemStack result) {
        this.target = target;
        this.result = result;
        this.research = research;
    }
    
    @Override
    public Placement getValidFace(Level level, Player player, BlockPos pos, Direction face) {
        BlockState state = level.getBlockState(pos);
        
        // Check if block matches
        if (!state.is(target)) {
            return null;
        }
        
        // Check research requirement
        if (research != null && !ThaumcraftCapabilities.knowsResearch(player, research)) {
            return null;
        }
        
        return new Placement(0, 0, 0, null);
    }
    
    @Override
    public void execute(Level level, Player player, BlockPos pos, Placement placement, Direction side) {
        if (level.isClientSide()) {
            return;
        }
        
        // Fire crafting event for advancements
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, result.copy(), null));
        
        BlockState state = level.getBlockState(pos);
        
        // Schedule the swap with a delay for visual effect
        ServerEvents.addRunnableServer(level, () -> {
            ServerEvents.addSwapper(level, pos, state, result.copy(), false, 0, player, 
                    true, true, -9999, false, false, 0, ServerEvents.DEFAULT_PREDICATE, 0.0f);
        }, 50);
    }
}




